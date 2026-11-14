package za.co.ernest.quickbite.kafka.port;

import za.co.ernest.quickbite.kafka.event.OtpNotificationEvent;

public interface NotificationPublisher {
    void sendOtpEvent(OtpNotificationEvent event);
}
