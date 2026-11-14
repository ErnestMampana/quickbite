package za.co.ernest.quickbite.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import za.co.ernest.quickbite.kafka.event.OtpNotificationEvent;
import za.co.ernest.quickbite.kafka.port.NotificationPublisher;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationPublisher implements NotificationPublisher {

    private final KafkaTemplate<String, OtpNotificationEvent> kafkaTemplate;

    @Value("${app.kafka.topics.otp-notification:quickbite.notifications.otp}")
    private String otpTopic;

    @Override
    public void sendOtpEvent(OtpNotificationEvent event) {
        log.info("Publishing [{}] OTP event for [{}] to Kafka topic [{}]", event.purpose(), event.email(), otpTopic);
        kafkaTemplate.send(otpTopic, event.email(), event);
    }
}
