package za.co.ernest.quickbite.order_and_operations_service.state;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class OrderStateConverter implements AttributeConverter<OrderState, String> {

    @Override
    public String convertToDatabaseColumn(OrderState attribute) {
        return attribute == null ? null : attribute.getStatusName();
    }

    @Override
    public OrderState convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;

        return switch (dbData.toUpperCase()) {
            case "CREATED" -> new CreatedOrderState();
            case "PAID" -> new PaidOrderState();
            case "PREPARING" -> new PreparingOrderState();
            case "DISPATCHED" -> new DispatchedOrderState();
            case "DELIVERED" -> new DeliveredOrderState();
            case "CANCELLED" -> new CancelledOrderState();
            default -> throw new IllegalArgumentException("Unknown order status identifier string: " + dbData);
        };
    }
}
