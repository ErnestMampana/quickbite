package za.co.ernest.quickbite.cart.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import za.co.ernest.quickbite.cart.client.OrderClient;
import za.co.ernest.quickbite.cart.client.OrderRequest;
import za.co.ernest.quickbite.cart.client.OrderResponse;
import za.co.ernest.quickbite.cart.dto.AddToCartRequest;
import za.co.ernest.quickbite.cart.dto.CartResponse;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private OrderClient orderClient;

    @InjectMocks
    private CartService cartService;

    private final Long customerId = 101L;
    private final Long restaurantId = 505L;

    @BeforeEach
    void setUp() {
        cartService.clearCartCache(customerId);
    }

    @Test
    @DisplayName("getCart: Should create and return a new empty cart when customer has no existing cart")
    void getCart_WhenCartDoesNotExist_CreatesNewCart() {
        CartResponse response = cartService.getCart(customerId);

        assertNotNull(response);
        assertEquals(customerId, response.customerId());
        assertNull(response.restaurantId());
        assertTrue(response.items().isEmpty());
        assertEquals(BigDecimal.ZERO, response.totalAmount());
    }

    @Test
    @DisplayName("addProductToCart: Should add item to cart and return updated cart response")
    void addProductToCart_ValidRequest_AddsItemAndReturnsCart() {
        AddToCartRequest request = new AddToCartRequest(
                restaurantId,
                1L,
                "Beef Burger",
                new BigDecimal("85.00"),
                2
        );

        CartResponse response = cartService.addProductToCart(customerId, request);

        assertNotNull(response);
        assertEquals(customerId, response.customerId());
        assertEquals(restaurantId, response.restaurantId());
        assertEquals(1, response.items().size());

        var addedItem = response.items().get(0);
        assertEquals(1L, addedItem.productId());
        assertEquals("Beef Burger", addedItem.productName());
        assertEquals(2, addedItem.quantity());
        assertEquals(new BigDecimal("170.00"), addedItem.subTotal());
        assertEquals(new BigDecimal("170.00"), response.totalAmount());
    }

    @Test
    @DisplayName("addProductToCart: Should increment quantity when adding an existing product from the same restaurant")
    void addProductToCart_DuplicateProduct_IncrementsQuantity() {
        AddToCartRequest firstRequest = new AddToCartRequest(
                restaurantId, 1L, "Beef Burger", new BigDecimal("85.00"), 1
        );
        AddToCartRequest secondRequest = new AddToCartRequest(
                restaurantId, 1L, "Beef Burger", new BigDecimal("85.00"), 3
        );

        cartService.addProductToCart(customerId, firstRequest);
        CartResponse response = cartService.addProductToCart(customerId, secondRequest);

        assertEquals(1, response.items().size());
        assertEquals(4, response.items().get(0).quantity());
        assertEquals(new BigDecimal("340.00"), response.totalAmount());
    }

    @Test
    @DisplayName("addProductToCart: Should throw IllegalArgumentException when adding item from a different restaurant")
    void addProductToCart_DifferentRestaurant_ThrowsIllegalArgumentException() {
        AddToCartRequest firstRequest = new AddToCartRequest(
                restaurantId, 1L, "Beef Burger", new BigDecimal("85.00"), 1
        );
        AddToCartRequest secondRequest = new AddToCartRequest(
                999L, 2L, "Chicken Wrap", new BigDecimal("65.00"), 1
        );

        cartService.addProductToCart(customerId, firstRequest);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.addProductToCart(customerId, secondRequest)
        );

        assertEquals("Cannot mix menu items from different restaurants. Clear cart first.", exception.getMessage());
    }

    @Test
    @DisplayName("clearCartCache: Should remove customer cart from cache")
    void clearCartCache_ExistingCart_RemovesCart() {
        AddToCartRequest request = new AddToCartRequest(
                restaurantId, 1L, "Chips", new BigDecimal("35.00"), 1
        );
        cartService.addProductToCart(customerId, request);

        cartService.clearCartCache(customerId);

        CartResponse newCart = cartService.getCart(customerId);
        assertTrue(newCart.items().isEmpty());
        assertNull(newCart.restaurantId());
    }

    @Test
    @DisplayName("checkout: Should place order, clear cart cache, and return OrderResponse on success")
    void checkout_ValidCart_PlacesOrderAndClearsCache() {
        AddToCartRequest request = new AddToCartRequest(
                restaurantId, 10L, "Pizza", new BigDecimal("120.00"), 1
        );
        cartService.addProductToCart(customerId, request);

        OrderResponse mockOrderResponse = mock(OrderResponse.class);
        when(orderClient.placeOrder(eq(customerId), any(OrderRequest.class)))
                .thenReturn(mockOrderResponse);

        OrderResponse actualResponse = cartService.checkout(customerId);

        assertSame(mockOrderResponse, actualResponse);

        ArgumentCaptor<OrderRequest> orderRequestCaptor = ArgumentCaptor.forClass(OrderRequest.class);
        verify(orderClient, times(1)).placeOrder(eq(customerId), orderRequestCaptor.capture());

        OrderRequest capturedRequest = orderRequestCaptor.getValue();
        assertEquals(restaurantId, capturedRequest.restaurantId());
        assertEquals(new BigDecimal("120.00"), capturedRequest.totalAmount());
        assertEquals(1, capturedRequest.items().size());
        assertEquals(10L, capturedRequest.items().get(0).productId());

        CartResponse cartAfterCheckout = cartService.getCart(customerId);
        assertTrue(cartAfterCheckout.items().isEmpty());
    }

    @Test
    @DisplayName("checkout: Should throw IllegalStateException when cart does not exist")
    void checkout_NonExistentCart_ThrowsIllegalStateException() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> cartService.checkout(customerId)
        );

        assertEquals("Cannot checkout an empty or non-existent cart for customer: " + customerId, exception.getMessage());
        verifyNoInteractions(orderClient);
    }

    @Test
    @DisplayName("checkout: Should throw IllegalStateException when cart exists but has no items")
    void checkout_EmptyCart_ThrowsIllegalStateException() {
        cartService.getCart(customerId);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> cartService.checkout(customerId)
        );

        assertEquals("Cannot checkout an empty or non-existent cart for customer: " + customerId, exception.getMessage());
        verifyNoInteractions(orderClient);
    }

    @Test
    @DisplayName("checkout: Should throw RuntimeException and retain cart in cache when OrderClient fails")
    void checkout_OrderClientThrowsException_ThrowsRuntimeExceptionAndRetainsCart() {
        AddToCartRequest request = new AddToCartRequest(
                restaurantId, 10L, "Sushi", new BigDecimal("200.00"), 1
        );
        cartService.addProductToCart(customerId, request);

        when(orderClient.placeOrder(eq(customerId), any(OrderRequest.class)))
                .thenThrow(new RuntimeException("Service Unavailable"));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> cartService.checkout(customerId)
        );

        assertEquals("Checkout failed. Please try again later.", exception.getMessage());
        assertNotNull(exception.getCause());

        CartResponse existingCart = cartService.getCart(customerId);
        assertEquals(1, existingCart.items().size());
    }
}


