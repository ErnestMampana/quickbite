package za.co.ernest.quickbite.cart.client;

import za.co.ernest.quickbite.cart.dto.OrderItemRequest;

import java.math.BigDecimal;
import java.util.List;

public record OrderRequest(
       // Long customerId,
       Long restaurantId,
       BigDecimal totalAmount,
       List<OrderItemRequest> items

) {}
