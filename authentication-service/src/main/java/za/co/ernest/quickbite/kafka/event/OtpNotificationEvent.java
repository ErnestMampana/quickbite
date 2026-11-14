package za.co.ernest.quickbite.kafka.event;

public record OtpNotificationEvent(String email,
                                   String firstName,
                                   String otpCode,
                                   String purpose) {
}
