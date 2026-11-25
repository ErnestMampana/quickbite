package za.co.ernest.quickbite.order_and_operations_service.service;

import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.ernest.quickbite.order_and_operations_service.client.RestaurantClient;
import za.co.ernest.quickbite.order_and_operations_service.dto.CreateOrderRequest;
import za.co.ernest.quickbite.order_and_operations_service.dto.OrderItemRequest;
import za.co.ernest.quickbite.order_and_operations_service.dto.OrderResponse;
import za.co.ernest.quickbite.order_and_operations_service.entity.Order;
import za.co.ernest.quickbite.order_and_operations_service.event.producer.OrderEventProducer;
import za.co.ernest.quickbite.order_and_operations_service.exception.UnauthorizedOwnershipException;
import za.co.ernest.quickbite.order_and_operations_service.repository.OrderRepository;
import za.co.ernest.quickbite.order_and_operations_service.state.CreatedOrderState;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestaurantClient restaurantClient;

    @Mock
    private OrderEventProducer orderEventProducer;

    @InjectMocks
    private OrderService orderService;

    private final Long customerId = 101L;
    private final Long restaurantId = 505L;
    private final Long orderId = 9001L;

    private Order buildPersistedOrder() {
        Order order = Order.builder()
                .customerId(customerId)
                .restaurantId(restaurantId)
                .totalAmount(new BigDecimal("150.00"))
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);
        return order;
    }

    @Test
    @DisplayName("placeOrder: Should save order in CreatedOrderState, publish OrderPlaced event, and return OrderResponse")
    void placeOrder_RestaurantActive_SavesOrderAndPublishesEvent() {

        OrderItemRequest itemReq = new OrderItemRequest(10L, 2, new BigDecimal("75.00"));
        CreateOrderRequest request = new CreateOrderRequest(
                restaurantId,
                new BigDecimal("150.00"),
                List.of(itemReq)
        );

        String expectedInitialStatus = new CreatedOrderState().getStatusName();
        when(restaurantClient.isRestaurantActive(restaurantId)).thenReturn(true);

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order orderToSave = invocation.getArgument(0);
            ReflectionTestUtils.setField(orderToSave, "id", orderId);
            return orderToSave;
        });

        OrderResponse response = orderService.placeOrder(customerId, request);

        assertNotNull(response);
        assertEquals(orderId, response.id());
        assertEquals(customerId, response.customerId());
        assertEquals(restaurantId, response.restaurantId());
        assertEquals(new BigDecimal("150.00"), response.totalAmount());
        assertEquals(expectedInitialStatus, response.status());

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertInstanceOf(CreatedOrderState.class, savedOrder.getLifecycleState());
        assertEquals(1, savedOrder.getItems().size());
        assertEquals(10L, savedOrder.getItems().get(0).getProductId());
        assertSame(savedOrder, savedOrder.getItems().get(0).getOrder());

        verify(orderEventProducer, times(1)).publishOrderPlaced(
                eq(orderId),
                eq(customerId),
                eq(restaurantId),
                eq(new BigDecimal("150.00")),
                eq(expectedInitialStatus)
        );
    }

    @Test
    @DisplayName("placeOrder: Should throw UnauthorizedOwnershipException when restaurant returns false")
    void placeOrder_RestaurantInactive_ThrowsUnauthorizedOwnershipException() {
        CreateOrderRequest request = new CreateOrderRequest(
                restaurantId, new BigDecimal("150.00"), List.of()
        );

        when(restaurantClient.isRestaurantActive(restaurantId)).thenReturn(false);

        UnauthorizedOwnershipException exception = assertThrows(
                UnauthorizedOwnershipException.class,
                () -> orderService.placeOrder(customerId, request)
        );

        assertEquals("Order Denied: The target establishment is currently closed or suspended.", exception.getMessage());
        verifyNoInteractions(orderRepository, orderEventProducer);
    }

    @Test
    @DisplayName("placeOrder: Should throw UnauthorizedOwnershipException when restaurant availability is null")
    void placeOrder_RestaurantAvailabilityNull_ThrowsUnauthorizedOwnershipException() {
        CreateOrderRequest request = new CreateOrderRequest(
                restaurantId, new BigDecimal("150.00"), List.of()
        );

        when(restaurantClient.isRestaurantActive(restaurantId)).thenReturn(null);

        assertThrows(
                UnauthorizedOwnershipException.class,
                () -> orderService.placeOrder(customerId, request)
        );

        verifyNoInteractions(orderRepository, orderEventProducer);
    }

    @Test
    @DisplayName("advanceOrderToPaid: Should transition CreatedOrderState to Paid and save without publishing event")
    void advanceOrderToPaid_ExistingOrder_AdvancesAndSaves() {
        Order order = buildPersistedOrder();
        String initialStatus = order.getLifecycleState().getStatusName();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.advanceOrderToPaid(orderId);

        assertNotNull(response);
        assertEquals(orderId, response.id());
        assertNotEquals(initialStatus, response.status());
        assertEquals(order.getLifecycleState().getStatusName(), response.status());

        verify(orderRepository, times(1)).save(order);
        verifyNoInteractions(orderEventProducer);
    }

    @Test
    @DisplayName("advanceOrderToPaid: Should throw RuntimeException when order is not found")
    void advanceOrderToPaid_OrderNotFound_ThrowsRuntimeException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.advanceOrderToPaid(orderId)
        );

        assertEquals("Order resolution mismatch for ID: " + orderId, exception.getMessage());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("handlePaymentSuccess: Should transition to Paid, save order, and publish status change event")
    void handlePaymentSuccess_ExistingOrder_UpdatesStateAndPublishesStatusChange() {
        Order order = buildPersistedOrder();
        String initialStatus = order.getLifecycleState().getStatusName();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.handlePaymentSuccess(orderId);

        String updatedStatus = order.getLifecycleState().getStatusName();
        assertEquals(updatedStatus, response.status());
        assertNotEquals(initialStatus, updatedStatus);

        verify(orderRepository, times(1)).save(order);
        verify(orderEventProducer, times(1)).publishStatusChange(
                orderId,
                initialStatus,
                updatedStatus,
                customerId
        );
    }

    @Test
    @DisplayName("handlePaymentSuccess: Should throw ResourceNotFoundException when order does not exist")
    void handlePaymentSuccess_OrderNotFound_ThrowsResourceNotFoundException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.handlePaymentSuccess(orderId)
        );

        assertEquals("Target order not found for ID: " + orderId, exception.getMessage());
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderEventProducer);
    }

    @Test
    @DisplayName("handleKitchenAcceptance: Should transition Paid order to Preparing, save, and publish status change")
    void handleKitchenAcceptance_PaidOrder_AcceptsAndPublishesStatusChange() {
        Order order = buildPersistedOrder();

        order.processPaymentSuccess();
        String paidStatus = order.getLifecycleState().getStatusName();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderService.handleKitchenAcceptance(orderId);
        String preparingStatus = order.getLifecycleState().getStatusName();
        assertEquals(preparingStatus, response.status());
        assertNotEquals(paidStatus, preparingStatus);
        verify(orderRepository, times(1)).save(order);
        verify(orderEventProducer, times(1)).publishStatusChange(
                orderId,
                paidStatus,
                preparingStatus,
                customerId
        );
    }
    @Test
    @DisplayName("handleKitchenAcceptance: Should throw IllegalStateException when attempting to accept an unpaid order")
    void handleKitchenAcceptance_UnpaidOrder_ThrowsIllegalStateException() {

        Order unpaidOrder = buildPersistedOrder();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(unpaidOrder));
        assertThrows(
                IllegalStateException.class,
                () -> orderService.handleKitchenAcceptance(orderId)
        );
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderEventProducer);
    }
    @Test
    @DisplayName("handleKitchenAcceptance: Should throw ResourceNotFoundException when order does not exist")
    void handleKitchenAcceptance_OrderNotFound_ThrowsResourceNotFoundException() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.handleKitchenAcceptance(orderId)
        );
        assertEquals("Target order not found for ID: " + orderId, exception.getMessage());
        verifyNoInteractions(orderEventProducer);
    }
}