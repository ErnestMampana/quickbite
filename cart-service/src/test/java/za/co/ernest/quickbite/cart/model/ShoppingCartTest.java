package za.co.ernest.quickbite.cart.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ShoppingCartTest {

    private ShoppingCart cart;
    private final Long customerId = 101L;
    private final Long restaurantA = 1L;
    private final Long restaurantB = 2L;

    @BeforeEach
    void setUp() {
        cart = new ShoppingCart(customerId);
    }

    @Test
    @DisplayName("addItem: Should set restaurantId and add new item")
    void addItem_NewItem_AddsToCart() {
        CartItem burger = new CartItem(10L, "Burger", new BigDecimal("80.00"), 1);

        cart.addItem(restaurantA, burger);

        assertEquals(restaurantA, cart.getRestaurantId());
        assertEquals(1, cart.getItems().size());
        assertEquals(new BigDecimal("80.00"), cart.calculateTotalCartValuation());
    }

    @Test
    @DisplayName("addItem: Should increment quantity when same productId is added")
    void addItem_ExistingProduct_IncrementsQuantity() {
        CartItem burger1 = new CartItem(10L, "Burger", new BigDecimal("80.00"), 1);
        CartItem burger2 = new CartItem(10L, "Burger", new BigDecimal("80.00"), 2);

        cart.addItem(restaurantA, burger1);
        cart.addItem(restaurantA, burger2);

        assertEquals(1, cart.getItems().size());
        assertEquals(3, cart.getItems().get(10L).getQuantity());
        assertEquals(new BigDecimal("240.00"), cart.calculateTotalCartValuation());
    }

    @Test
    @DisplayName("addItem: Should throw IllegalArgumentException when adding item from different restaurant")
    void addItem_DifferentRestaurant_ThrowsException() {
        CartItem burger = new CartItem(10L, "Burger", new BigDecimal("80.00"), 1);
        CartItem pizza = new CartItem(20L, "Pizza", new BigDecimal("120.00"), 1);

        cart.addItem(restaurantA, burger);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cart.addItem(restaurantB, pizza)
        );

        assertEquals("Cannot mix menu items from different restaurants. Clear cart first.", ex.getMessage());
    }

    @Test
    @DisplayName("removeItem: Should reset restaurantId to null when the last item is removed")
    void removeItem_LastItemRemoved_ResetsRestaurantId() {
        CartItem burger = new CartItem(10L, "Burger", new BigDecimal("80.00"), 1);
        cart.addItem(restaurantA, burger);

        cart.removeItem(10L);

        assertTrue(cart.getItems().isEmpty());
        assertNull(cart.getRestaurantId());
        assertEquals(BigDecimal.ZERO, cart.calculateTotalCartValuation());

        CartItem pizza = new CartItem(20L, "Pizza", new BigDecimal("120.00"), 1);
        assertDoesNotThrow(() -> cart.addItem(restaurantB, pizza));
        assertEquals(restaurantB, cart.getRestaurantId());
    }

    @Test
    @DisplayName("calculateTotalCartValuation: Should sum subtotals of multiple items accurately")
    void calculateTotalCartValuation_MultipleItems_ReturnsCorrectTotal() {
        cart.addItem(restaurantA, new CartItem(10L, "Burger", new BigDecimal("85.50"), 2)); // 171.00
        cart.addItem(restaurantA, new CartItem(11L, "Soda", new BigDecimal("22.25"), 1));   // 22.25

        assertEquals(new BigDecimal("193.25"), cart.calculateTotalCartValuation());
    }
}

