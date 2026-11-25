package za.co.ernest.quickbite.order_and_operations_service.event.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import za.co.ernest.quickbite.order_and_operations_service.event.PaymentSucceededEvent;
import za.co.ernest.quickbite.order_and_operations_service.service.OrderService;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${app.kafka.topics.payment-events:payment-events}",
            groupId = "${spring.kafka.consumer.group-id:order-service-group}"
    )
    public void consumePaymentEvent(PaymentEvent event) {
        log.info("Received PaymentEvent [status={}] for order [{}], reference [{}]",
                event.status(), event.orderId(), event.reference());

        if ("SUCCESS".equalsIgnoreCase(event.status())) {
            orderService.handlePaymentSuccess(event.orderId());
        } else {
            log.warn("Payment not successful for order [{}]: status={}",
                    event.orderId(), event.status());
        }
    }
}


