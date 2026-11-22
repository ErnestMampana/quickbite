package za.co.ernest.payment_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import za.co.ernest.payment_service.dto.PaymentEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventPublisher {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @Value("${app.kafka.topics.payment-events:payment-events}")
    private String paymentEventsTopic;

    public void publishPaymentEvent(PaymentEvent event) {
        log.info("Publishing PaymentEvent [status={}] for order [{}] reference [{}]",
                event.status(), event.orderId(), event.reference());
        kafkaTemplate.send(paymentEventsTopic, event.orderId().toString(), event);
    }
}
