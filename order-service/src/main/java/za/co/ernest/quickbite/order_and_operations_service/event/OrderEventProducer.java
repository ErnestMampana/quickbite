package za.co.ernest.quickbite.order_and_operations_service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import za.co.ernest.quickbite.order_and_operations_service.dto.OrderPlacedEvent;
import za.co.ernest.quickbite.order_and_operations_service.dto.OrderStatusChangedEvent;

import java.math.BigDecimal;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String ORDER_TOPIC = "quickbite-order-events";
    private static final String TOPIC_STATE_CHANGES = "quickbite-order-status-events";


    public void publishOrderPlaced(Long orderId, Long customerId, Long restaurantId, BigDecimal amount, String status) {
        OrderPlacedEvent event = new OrderPlacedEvent(orderId, customerId, restaurantId, amount, status);

        kafkaTemplate.send(ORDER_TOPIC, orderId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Asynchronously pushed order event to MQS topic [{}] for Order ID [{}]", ORDER_TOPIC, orderId);
                    } else {
                        log.error("MQS critical publish failure for Order ID [{}]", orderId, ex);
                    }
                });
    }



    public void publishStatusChange(Long orderId, String oldStatus, String newStatus, Long customerId) {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(orderId, oldStatus, newStatus, customerId);

        kafkaTemplate.send(TOPIC_STATE_CHANGES, orderId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("MQS Stream: Dispatched status transition event [{}] -> [{}] for Order ID [{}]", oldStatus, newStatus, orderId);
                    } else {
                        log.error("MQS critical failure broadcasting state update for Order ID [{}]", orderId, ex);
                    }
                });
    }
}
