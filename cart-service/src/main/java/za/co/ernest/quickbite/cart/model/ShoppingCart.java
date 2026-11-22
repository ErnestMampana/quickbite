package za.co.ernest.quickbite.cart.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class ShoppingCart {

    private final Long customerId;
    private Long restaurantId;
    private final Map<Long, CartItem> items = new ConcurrentHashMap<>();

    public ShoppingCart(Long customerId) {
        this.customerId = customerId;
    }

    public void addItem(Long targetRestaurantId, CartItem newItem) {
        if (this.restaurantId != null && !this.restaurantId.equals(targetRestaurantId) && !this.items.isEmpty()) {
            throw new IllegalArgumentException("Cannot mix menu items from different restaurants. Clear cart first.");
        }

        this.restaurantId = targetRestaurantId;

        if (items.containsKey(newItem.getProductId())) {
            items.get(newItem.getProductId()).incrementQuantity(newItem.getQuantity());
        } else {
            items.put(newItem.getProductId(), newItem);
        }
    }

    public void removeItem(Long productId) {
        this.items.remove(productId);
        if (this.items.isEmpty()) {
            this.restaurantId = null;
        }
    }

    public BigDecimal calculateTotalCartValuation() {
        return this.items.values().stream()
                .map(CartItem::calculateSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
