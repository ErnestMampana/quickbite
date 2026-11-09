package za.co.ernest.quickbite.order_and_operations_service.dto;

import java.math.BigDecimal;

public record OrderItemRequest(
        Long productId,
        Integer quantity,
        BigDecimal unitPrice
) {}
