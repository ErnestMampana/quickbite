package za.co.ernest.quickbite.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import za.co.ernest.quickbite.kafka.event.OtpNotificationEvent;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventProducer {

    private final KafkaTemplate<String, OtpNotificationEvent> kafkaTemplate;

    @Value("${app.kafka.topics.otp-notification}")
    private String otpTopic;

    public void sendOtpEvent(OtpNotificationEvent event) {
        log.info("Publishing OTP event [{}] for user [{}] to Kafka topic [{}]", event.purpose(), event.email(), otpTopic);
        kafkaTemplate.send(otpTopic, event.email(), event);
    }
}
