package za.co.ernest.payment_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InitializePaymentRequest(
        @NotNull(message = "Order ID is required")
        Long orderId,

        @NotNull(message = "Customer email is required")
        @Email(message = "Valid email is required")
        String email,

        @NotNull(message = "Amount in ZAR is required")
        @DecimalMin(value = "1.00", message = "Minimum payment amount is R1.00")
        BigDecimal amountZar
) {}
