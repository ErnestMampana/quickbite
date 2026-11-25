package za.co.ernest.notification_service.notification_service.notification;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.ernest.notification_service.notification_service.event.OtpNotificationEvent;
import za.co.ernest.notification_service.notification_service.service.EmailService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpNotificationConsumerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OtpNotificationConsumer consumer;

    private final String email = "ernest@example.co.za";
    private final String firstName = "Ernest";
    private final String otpCode = "123456";

    @Test
    @DisplayName("handleOtpNotification: Should send password reset email when purpose is PASSWORD_RESET")
    void handleOtpNotification_PasswordResetPurpose_SendsPasswordResetEmail() {
        OtpNotificationEvent event = new OtpNotificationEvent(
                email, firstName, otpCode, "PASSWORD_RESET"
        );

        consumer.handleOtpNotification(event);

        verify(emailService, times(1)).sendPasswordResetEmail(email, firstName, otpCode);
        verify(emailService, never()).sendAccountVerificationEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("handleOtpNotification: Should send account verification email when purpose is ACCOUNT_VERIFICATION")
    void handleOtpNotification_AccountVerificationPurpose_SendsAccountVerificationEmail() {
        OtpNotificationEvent event = new OtpNotificationEvent(
                email, firstName, otpCode, "ACCOUNT_VERIFICATION"
        );

        consumer.handleOtpNotification(event);

        verify(emailService, times(1)).sendAccountVerificationEmail(email, firstName, otpCode);
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("handleOtpNotification: Should default to account verification email when purpose is null or unrecognized")
    void handleOtpNotification_NullPurpose_DefaultsToAccountVerificationEmail() {
        OtpNotificationEvent event = new OtpNotificationEvent(
                email, firstName, otpCode, null
        );

        consumer.handleOtpNotification(event);

        verify(emailService, times(1)).sendAccountVerificationEmail(email, firstName, otpCode);
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("handleOtpNotification: Should propagate exception when EmailService fails so Kafka can retry/DLQ")
    void handleOtpNotification_EmailServiceThrowsException_PropagatesException() {
        OtpNotificationEvent event = new OtpNotificationEvent(
                email, firstName, otpCode, "PASSWORD_RESET"
        );

        doThrow(new IllegalStateException("Email composition failed"))
                .when(emailService).sendPasswordResetEmail(email, firstName, otpCode);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> consumer.handleOtpNotification(event)
        );

        assertEquals("Email composition failed", exception.getMessage());
    }
}

