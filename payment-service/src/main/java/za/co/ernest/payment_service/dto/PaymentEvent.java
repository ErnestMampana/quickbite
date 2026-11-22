package za.co.ernest.payment_service.dto;

import za.co.ernest.payment_service.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentEvent(
        String reference,
        Long orderId,
        String customerEmail,
        BigDecimal amountZar,
        PaymentStatus status,
        String paymentChannel,
        Instant occurredAt
) {}
