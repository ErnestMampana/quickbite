package za.co.ernest.quickbite.order_and_operations_service.dto;

import java.io.Serializable;

public record OrderStatusChangedEvent(Long orderId, String oldStatus, String newStatus, Long customerId) implements Serializable {}
