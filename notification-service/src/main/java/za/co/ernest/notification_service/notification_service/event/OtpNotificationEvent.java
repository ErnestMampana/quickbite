package za.co.ernest.notification_service.notification_service.event;

public record OtpNotificationEvent(
        String email,
        String firstName,
        String otpCode,
        String purpose
) {}