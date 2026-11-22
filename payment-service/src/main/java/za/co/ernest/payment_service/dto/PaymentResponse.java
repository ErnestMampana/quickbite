package za.co.ernest.payment_service.dto;

import za.co.ernest.payment_service.enums.PaymentStatus;

import java.math.BigDecimal;

public record PaymentResponse(
        String reference,
        Long orderId,
        BigDecimal amountZar,
        String currency,
        PaymentStatus status,
        String authorizationUrl
) {}
