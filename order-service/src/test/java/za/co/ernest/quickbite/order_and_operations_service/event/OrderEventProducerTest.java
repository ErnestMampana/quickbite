package za.co.ernest.quickbite.order_and_operations_service.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import za.co.ernest.quickbite.order_and_operations_service.dto.OrderStatusChangedEvent;
import za.co.ernest.quickbite.order_and_operations_service.event.producer.OrderEventProducer;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private OrderEventProducer producer;

    private static final String ORDER_TOPIC = "quickbite-order-events";
    private static final String TOPIC_STATE_CHANGES = "quickbite-order-status-events";

    private final Long orderId = 9001L;
    private final Long customerId = 101L;
    private final Long restaurantId = 505L;

    @Test
    @DisplayName("publishOrderPlaced: Should send OrderPlacedEvent to quickbite-order-events with orderId key")
    @SuppressWarnings("unchecked")
    void publishOrderPlaced_Success_SendsEventToKafka() {
        BigDecimal amount = new BigDecimal("185.50");
        String status = "CREATED";

        SendResult<String, Object> mockSendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mockSendResult);

        when(kafkaTemplate.send(eq(ORDER_TOPIC), eq(orderId.toString()), any(OrderPlacedEvent.class)))
                .thenReturn(future);

        assertDoesNotThrow(() ->
                producer.publishOrderPlaced(orderId, customerId, restaurantId, amount, status)
        );

        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(kafkaTemplate, times(1)).send(eq(ORDER_TOPIC), eq("9001"), eventCaptor.capture());

        OrderPlacedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(orderId, capturedEvent.orderId());
        assertEquals(customerId, capturedEvent.customerId());
        assertEquals(restaurantId, capturedEvent.restaurantId());
        assertEquals(amount, capturedEvent.amount());
        assertEquals(status, capturedEvent.status());
    }

    @Test
    @DisplayName("publishOrderPlaced: Should handle async broker failure gracefully in whenComplete callback")
    void publishOrderPlaced_BrokerFailure_HandlesExceptionGracefully() {
        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka broker unreachable"));

        when(kafkaTemplate.send(eq(ORDER_TOPIC), eq(orderId.toString()), any(OrderPlacedEvent.class)))
                .thenReturn(failedFuture);

        assertDoesNotThrow(() ->
                producer.publishOrderPlaced(orderId, customerId, restaurantId, new BigDecimal("100.00"), "CREATED")
        );

        verify(kafkaTemplate, times(1)).send(eq(ORDER_TOPIC), eq("9001"), any(OrderPlacedEvent.class));
    }

    @Test
    @DisplayName("publishStatusChange: Should send OrderStatusChangedEvent to quickbite-order-status-events with orderId key")
    @SuppressWarnings("unchecked")
    void publishStatusChange_Success_SendsEventToKafka() {
        String oldStatus = "CREATED";
        String newStatus = "PAID";

        SendResult<String, Object> mockSendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(mockSendResult);

        when(kafkaTemplate.send(eq(TOPIC_STATE_CHANGES), eq(orderId.toString()), any(OrderStatusChangedEvent.class)))
                .thenReturn(future);

        assertDoesNotThrow(() ->
                producer.publishStatusChange(orderId, oldStatus, newStatus, customerId)
        );

        ArgumentCaptor<OrderStatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
        verify(kafkaTemplate, times(1)).send(eq(TOPIC_STATE_CHANGES), eq("9001"), eventCaptor.capture());

        OrderStatusChangedEvent capturedEvent = eventCaptor.getValue();
        assertEquals(orderId, capturedEvent.orderId());
        assertEquals(oldStatus, capturedEvent.oldStatus());
        assertEquals(newStatus, capturedEvent.newStatus());
        assertEquals(customerId, capturedEvent.customerId());
    }

    @Test
    @DisplayName("publishStatusChange: Should handle async broker failure gracefully in whenComplete callback")
    void publishStatusChange_BrokerFailure_HandlesExceptionGracefully() {
        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka timeout"));

        when(kafkaTemplate.send(eq(TOPIC_STATE_CHANGES), eq(orderId.toString()), any(OrderStatusChangedEvent.class)))
                .thenReturn(failedFuture);

        assertDoesNotThrow(() ->
                producer.publishStatusChange(orderId, "PAID", "PREPARING", customerId)
        );

        verify(kafkaTemplate, times(1)).send(eq(TOPIC_STATE_CHANGES), eq("9001"), any(OrderStatusChangedEvent.class));
    }
}

