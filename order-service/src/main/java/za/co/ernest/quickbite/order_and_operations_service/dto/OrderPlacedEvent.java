package za.co.ernest.quickbite.order_and_operations_service.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public record OrderPlacedEvent(Long orderId, Long customerId, Long restaurantId, BigDecimal amount, String status) implements Serializable {}
