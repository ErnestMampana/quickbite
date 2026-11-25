package za.co.ernest.quickbite.order_and_operations_service.event;

import java.math.BigDecimal;

public record PaymentSucceededEvent(
        Long orderId,
        String paymentReference,
        BigDecimal amountPaid
) {}
