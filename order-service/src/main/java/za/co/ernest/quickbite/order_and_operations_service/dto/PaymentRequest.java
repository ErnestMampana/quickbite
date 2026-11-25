package za.co.ernest.quickbite.order_and_operations_service.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PaymentRequest(
        Long orderId,
        String email,
        BigDecimal amountZar
) {}
