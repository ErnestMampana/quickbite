package za.co.ernest.quickbite.cart.client;

import java.math.BigDecimal;

public record OrderResponse(
        Long id,
        Long customerId,
        Long restaurantId,
        BigDecimal totalAmount,
        String status
) {}
