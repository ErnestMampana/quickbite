package za.co.ernest.quickbite.order_and_operations_service.event.consumer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentEvent(
        Long orderId,
        Long customerId,
        String reference,
        BigDecimal amount,
        String status,
        Instant occurredAt
) {}
