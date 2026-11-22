package za.co.ernest.quickbite.cart.model;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CartItem {

    private final Long productId;
    private final String productName;
    private final BigDecimal unitPrice;
    private int quantity;

    public void incrementQuantity(int amount) {
        this.quantity += amount;
    }

    public BigDecimal calculateSubTotal() {
        return this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
    }
}
