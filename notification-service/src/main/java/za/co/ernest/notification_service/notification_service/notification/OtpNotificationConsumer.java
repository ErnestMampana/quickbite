package za.co.ernest.notification_service.notification_service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import za.co.ernest.notification_service.notification_service.event.OtpNotificationEvent;
import za.co.ernest.notification_service.notification_service.service.EmailService;


@Component
@RequiredArgsConstructor
@Slf4j
public class OtpNotificationConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = "${app.kafka.topics.otp-notifications:quickbite.notifications.otp}", groupId = "notification-service-group")
    public void handleOtpNotification(OtpNotificationEvent event) {
        log.info("Consumed OtpNotificationEvent for [{}] with purpose [{}]", event.email(), event.purpose());

        if ("PASSWORD_RESET".equals(event.purpose())) {
            emailService.sendPasswordResetEmail(event.email(), event.firstName(), event.otpCode());
        } else {
            emailService.sendAccountVerificationEmail(event.email(), event.firstName(), event.otpCode());
        }
    }
}
