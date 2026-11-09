package za.co.ernest.quickbite.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record AddToCartRequest(
        @NotNull(message = "Restaurant context is mandatory") Long restaurantId,
        @NotNull(message = "Product identity is mandatory") Long productId,
        @NotBlank(message = "Product display title is required") String productName,
        @NotNull(message = "Unit price valuation is required") @Positive BigDecimal unitPrice,
        @Min(value = 1, message = "Quantity increment must be at least 1") int quantity
) {}
