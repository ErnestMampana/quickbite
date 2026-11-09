package za.co.ernest.quickbite.order_and_operations_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.ernest.quickbite.order_and_operations_service.entity.OrderItem;
import za.co.ernest.quickbite.order_and_operations_service.repository.OrderRepository;
import za.co.ernest.quickbite.order_and_operations_service.client.RestaurantClient;
import za.co.ernest.quickbite.order_and_operations_service.dto.CreateOrderRequest;
import za.co.ernest.quickbite.order_and_operations_service.dto.OrderResponse;
import za.co.ernest.quickbite.order_and_operations_service.entity.Order;
import za.co.ernest.quickbite.order_and_operations_service.event.OrderEventProducer;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestaurantClient restaurantClient;
    private final OrderEventProducer orderEventProducer;



    @Transactional
    public OrderResponse placeOrder(Long authenticatedCustomerId, CreateOrderRequest request) {
        log.info("Processing checkout for customer ID [{}]", authenticatedCustomerId);

        Boolean isAvailable = restaurantClient.isRestaurantActive(request.restaurantId());
        if (Boolean.FALSE.equals(isAvailable) || isAvailable == null) {
            throw new za.co.ernest.quickbite.order_and_operations_service.exception.UnauthorizedOwnershipException(
                    "Order Denied: The target establishment is currently closed or suspended.");
        }

        Order order = Order.builder()
                .customerId(authenticatedCustomerId)
                .restaurantId(request.restaurantId())
                .totalAmount(request.totalAmount())
                .build();

        List<OrderItem> orderItems = request.items().stream()
                .map(itemReq -> OrderItem.builder()
                        .productId(itemReq.productId())
                        .quantity(itemReq.quantity())
                        .unitPrice(itemReq.unitPrice())
                        .order(order)
                        .build())
                .toList();

        order.setItems(orderItems);

        Order savedOrder = orderRepository.save(order);

        orderEventProducer.publishOrderPlaced(
                savedOrder.getId(),
                savedOrder.getCustomerId(),
                savedOrder.getRestaurantId(),
                savedOrder.getTotalAmount(),
                savedOrder.getLifecycleState().getStatusName()
        );

        return mapToResponse(savedOrder);
    }
//    @Transactional
//    public OrderResponse placeOrder(Long authenticatedCustomerId, CreateOrderRequest request) {
//        log.info("Processing checkout for customer ID [{}]", authenticatedCustomerId);
//
//        Boolean isAvailable = restaurantClient.isRestaurantActive(request.restaurantId());
//
//        if (Boolean.FALSE.equals(isAvailable) || isAvailable == null) {
//            throw new za.co.ernest.quickbite.order_and_operations_service.exception.UnauthorizedOwnershipException(
//                    "Order Denied: The target establishment is currently closed or suspended.");
//        }
//
//        Order order = Order.builder()
//                .customerId(authenticatedCustomerId)
//                .restaurantId(request.restaurantId())
//                .totalAmount(request.totalAmount())
//                .build();
//
//        Order savedOrder = orderRepository.save(order);
//
//        orderEventProducer.publishOrderPlaced(
//                savedOrder.getId(),
//                savedOrder.getCustomerId(),
//                savedOrder.getRestaurantId(),
//                savedOrder.getTotalAmount(),
//                savedOrder.getLifecycleState().getStatusName()
//        );
//
//        return mapToResponse(savedOrder);
//    }


    @Transactional
    public OrderResponse advanceOrderToPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order resolution mismatch for ID: " + orderId));

        order.processPaymentSuccess();

        return mapToResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse handlePaymentSuccess(Long orderId) {
        Order order = fetchOrderOrThrow(orderId);
        String previousStatus = order.getLifecycleState().getStatusName();

        order.processPaymentSuccess();
        Order savedOrder = orderRepository.save(order);


        orderEventProducer.publishStatusChange(
                savedOrder.getId(),
                previousStatus,
                savedOrder.getLifecycleState().getStatusName(),
                savedOrder.getCustomerId()
        );

        return mapToResponse(savedOrder);
    }


    @Transactional
    public OrderResponse handleKitchenAcceptance(Long orderId) {
        Order order = fetchOrderOrThrow(orderId);
        String previousStatus = order.getLifecycleState().getStatusName();

        order.acceptToKitchen();
        Order savedOrder = orderRepository.save(order);

        orderEventProducer.publishStatusChange(
                savedOrder.getId(),
                previousStatus,
                savedOrder.getLifecycleState().getStatusName(),
                savedOrder.getCustomerId()
        );

        return mapToResponse(savedOrder);
    }



    private Order fetchOrderOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Target order not found for ID: " + orderId));
    }


    private OrderResponse mapToResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getRestaurantId(),
                order.getTotalAmount(),
                order.getLifecycleState().getStatusName()
        );
    }
}
