package za.co.ernest.quickbite.order_and_operations_service.dto;

import java.math.BigDecimal;

public record OrderResponse(
        Long id,
        Long customerId,
        Long restaurantId,
        BigDecimal totalAmount,
        String status
) {}
