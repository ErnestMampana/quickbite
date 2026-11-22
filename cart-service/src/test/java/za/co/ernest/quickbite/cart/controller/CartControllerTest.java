package za.co.ernest.quickbite.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.ernest.quickbite.cart.client.OrderResponse;
import za.co.ernest.quickbite.cart.dto.AddToCartRequest;
import za.co.ernest.quickbite.cart.dto.CartItemResponse;
import za.co.ernest.quickbite.cart.dto.CartResponse;
import za.co.ernest.quickbite.cart.service.CartService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

   private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private CartService cartService;

    private static final String USER_HEADER = "X-Authenticated-User-Id";
    private final Long customerId = 101L;
    private final Long restaurantId = 505L;

    @Test
    @DisplayName("GET /api/v1/cart: Should return 200 OK with CartResponse")
    void getCart_ValidHeader_ReturnsOkWithCart() throws Exception {
        CartResponse mockResponse = new CartResponse(
                customerId,
                restaurantId,
                Collections.emptyList(),
                BigDecimal.ZERO
        );

        when(cartService.getCart(customerId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/cart")
                        .header(USER_HEADER, customerId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.restaurantId").value(restaurantId))
                .andExpect(jsonPath("$.items").isEmpty());

        verify(cartService, times(1)).getCart(customerId);
    }

    @Test
    @DisplayName("GET /api/v1/cart: Should return 400 Bad Request when X-Authenticated-User-Id header is missing")
    void getCart_MissingHeader_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /api/v1/cart/items: Should add item and return 200 OK with updated CartResponse")
    void addItemToCart_ValidRequest_ReturnsOkWithUpdatedCart() throws Exception {
        AddToCartRequest request = new AddToCartRequest(
                restaurantId,
                1L,
                "Beef Burger",
                new BigDecimal("85.00"),
                2
        );

        CartItemResponse itemResponse = new CartItemResponse(
                1L, "Beef Burger", new BigDecimal("85.00"), 2, new BigDecimal("170.00")
        );
        CartResponse mockResponse = new CartResponse(
                customerId,
                restaurantId,
                List.of(itemResponse),
                new BigDecimal("170.00")
        );

        when(cartService.addProductToCart(eq(customerId), any(AddToCartRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header(USER_HEADER, customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.items[0].productName").value("Beef Burger"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        verify(cartService, times(1)).addProductToCart(eq(customerId), any(AddToCartRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/cart/items: Should return 400 Bad Request when request body fails @Valid constraints")
    void addItemToCart_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Invalid payload (e.g., null fields / negative quantity) to trigger @Valid
        AddToCartRequest invalidRequest = new AddToCartRequest(
                null,
                null,
                "",
                new BigDecimal("-10.00"),
                0
        );

        mockMvc.perform(post("/api/v1/cart/items")
                        .header(USER_HEADER, customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST /api/v1/cart/checkout/{customerId}: Should return 201 Created with OrderResponse")
    void checkout_ValidCustomerId_ReturnsCreatedWithOrderResponse() throws Exception {
        OrderResponse mockOrderResponse = mock(OrderResponse.class);
        when(cartService.checkout(customerId)).thenReturn(mockOrderResponse);

        mockMvc.perform(post("/api/v1/cart/checkout/{customerId}", customerId))
                .andExpect(status().isCreated());

        verify(cartService, times(1)).checkout(customerId);
    }

    @Test
    @DisplayName("DELETE /api/v1/cart: Should clear cart and return 204 No Content")
    void clearCart_ValidHeader_ReturnsNoContent() throws Exception {
        doNothing().when(cartService).clearCartCache(customerId);

        mockMvc.perform(delete("/api/v1/cart")
                        .header(USER_HEADER, customerId))
                .andExpect(status().isNoContent());

        verify(cartService, times(1)).clearCartCache(customerId);
    }
}

