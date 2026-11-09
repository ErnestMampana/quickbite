package za.co.ernest.quickbite.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long customerId,
        Long restaurantId,
        List<CartItemResponse> items,
        BigDecimal totalAmount
) {}
