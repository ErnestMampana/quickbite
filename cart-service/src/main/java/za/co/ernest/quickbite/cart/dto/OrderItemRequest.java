package za.co.ernest.quickbite.cart.dto;

import java.math.BigDecimal;

public record OrderItemRequest(
        Long productId,
        Integer quantity,
        BigDecimal unitPrice
) {}
