package za.co.ernest.quickbite.order_and_operations_service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotNull(message = "Restaurant identity context is required.")
        Long restaurantId,

        @NotNull(message = "Total checkout valuation is required.")
        @Positive(message = "Total order amount must be a positive value greater than zero.")
        BigDecimal totalAmount,

        @NotNull(message = "Cannot process empty order.")
        List<OrderItemRequest> items
) {}

