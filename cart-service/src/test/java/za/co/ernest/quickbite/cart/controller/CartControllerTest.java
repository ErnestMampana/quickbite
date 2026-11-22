package za.co.ernest.quickbite.cart.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean; // Boot 3.4+: use @MockitoBean (org.springframework.test.context.bean.override.mockito)
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import za.co.ernest.quickbite.cart.client.OrderResponse;
import za.co.ernest.quickbite.cart.dto.AddToCartRequest;
import za.co.ernest.quickbite.cart.dto.CartResponse;
import za.co.ernest.quickbite.cart.service.CartService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false) // skip Spring Security filters/CSRF if security is on the classpath
class CartControllerTest {

    private static final String BASE_URL = "/api/v1/cart";
    private static final String USER_HEADER = "X-Authenticated-User-Id";
    private static final Long CUSTOMER_ID = 42L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    // ------------------------------------------------------------------
    // GET /api/v1/cart
    // ------------------------------------------------------------------

    @Test
    @DisplayName("GET cart - returns 200 and the cart for the authenticated user")
    void getCart_returnsCart() throws Exception {
        when(cartService.getCart(CUSTOMER_ID)).thenReturn(cartResponse());

        mockMvc.perform(get(BASE_URL).header(USER_HEADER, CUSTOMER_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID));

        verify(cartService).getCart(CUSTOMER_ID);
    }

    @Test
    @DisplayName("GET cart - returns 400 when the X-Authenticated-User-Id header is missing")
    void getCart_missingHeader_returns400() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("GET cart - returns 400 when the header is not a number")
    void getCart_nonNumericHeader_returns400() throws Exception {
        mockMvc.perform(get(BASE_URL).header(USER_HEADER, "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("GET cart - service exception is propagated when no exception handler is registered")
    void getCart_serviceThrows_propagates() {
        when(cartService.getCart(CUSTOMER_ID)).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> mockMvc.perform(get(BASE_URL).header(USER_HEADER, CUSTOMER_ID)))
                .isInstanceOf(ServletException.class)
                .hasRootCauseMessage("boom");
        // If you have a @ControllerAdvice, add @Import(YourAdvice.class) and assert the mapped status instead.
    }

    // ------------------------------------------------------------------
    // POST /api/v1/cart/items
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST items - returns 200 and the updated cart for a valid request")
    void addItemToCart_validRequest_returnsCart() throws Exception {
        when(cartService.addProductToCart(eq(CUSTOMER_ID), any(AddToCartRequest.class)))
                .thenReturn(cartResponse());

        mockMvc.perform(post(BASE_URL + "/items")
                        .header(USER_HEADER, CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddToCartJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID));

        verify(cartService).addProductToCart(eq(CUSTOMER_ID), any(AddToCartRequest.class));
    }

    @Test
    @DisplayName("POST items - returns 400 when the header is missing")
    void addItemToCart_missingHeader_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAddToCartJson()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST items - returns 400 when the body is missing")
    void addItemToCart_missingBody_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/items")
                        .header(USER_HEADER, CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST items - returns 400 for malformed JSON")
    void addItemToCart_malformedJson_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/items")
                        .header(USER_HEADER, CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST items - returns 400 when the request body fails validation (empty object)")
    void addItemToCart_emptyBody_failsValidation() throws Exception {
        // Assumes AddToCartRequest has constraints such as @NotNull on productId.
        mockMvc.perform(post(BASE_URL + "/items")
                        .header(USER_HEADER, CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST items - returns 400 when quantity is zero or negative")
    void addItemToCart_invalidQuantity_failsValidation() throws Exception {
        // Assumes @Min(1) / @Positive on quantity. Remove if your DTO does not constrain it.
        mockMvc.perform(post(BASE_URL + "/items")
                        .header(USER_HEADER, CUSTOMER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\": 1, \"quantity\": -5}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST items - returns 415 for an unsupported content type")
    void addItemToCart_wrongContentType_returns415() throws Exception {
        mockMvc.perform(post(BASE_URL + "/items")
                        .header(USER_HEADER, CUSTOMER_ID)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(cartService);
    }

    // ------------------------------------------------------------------
    // POST /api/v1/cart/checkout/{customerId}
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST checkout - returns 201 and the created order")
    void checkout_returnsCreated() throws Exception {
        when(cartService.checkout(CUSTOMER_ID)).thenReturn(orderResponse());

        mockMvc.perform(post(BASE_URL + "/checkout/{customerId}", CUSTOMER_ID))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(cartService).checkout(CUSTOMER_ID);
    }

    @Test
    @DisplayName("POST checkout - returns 400 when customerId is not a number")
    void checkout_nonNumericCustomerId_returns400() throws Exception {
        mockMvc.perform(post(BASE_URL + "/checkout/{customerId}", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("POST checkout - service exception is propagated when no exception handler is registered")
    void checkout_serviceThrows_propagates() {
        when(cartService.checkout(CUSTOMER_ID)).thenThrow(new IllegalStateException("Cart is empty"));

        assertThatThrownBy(() -> mockMvc.perform(post(BASE_URL + "/checkout/{customerId}", CUSTOMER_ID)))
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class);
    }

    // ------------------------------------------------------------------
    // DELETE /api/v1/cart
    // ------------------------------------------------------------------

    @Test
    @DisplayName("DELETE cart - returns 204 and clears the cache for the authenticated user")
    void clearCart_returnsNoContent() throws Exception {
        doNothing().when(cartService).clearCartCache(CUSTOMER_ID);

        mockMvc.perform(delete(BASE_URL).header(USER_HEADER, CUSTOMER_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(cartService).clearCartCache(CUSTOMER_ID);
    }

    @Test
    @DisplayName("DELETE cart - returns 400 when the header is missing")
    void clearCart_missingHeader_returns400() throws Exception {
        mockMvc.perform(delete(BASE_URL))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(cartService);
    }

    // ------------------------------------------------------------------
    // Routing
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Unsupported HTTP method on /items returns 405")
    void items_unsupportedMethod_returns405() throws Exception {
        mockMvc.perform(put(BASE_URL + "/items").header(USER_HEADER, CUSTOMER_ID))
                .andExpect(status().isMethodNotAllowed());
    }

    // ------------------------------------------------------------------
    // Helpers - ADJUST these to match your real DTOs
    // ------------------------------------------------------------------

    private String validAddToCartJson() {
        // Assumed fields: productId, quantity. Change to match AddToCartRequest.
        return "{\"productId\": 1, \"quantity\": 2}";
    }

    private CartResponse cartResponse() {
        // Assumes CartResponse has a no-args constructor and a customerId setter.
        // If it uses @Builder / an all-args constructor / a record, build it that way instead.
        CartResponse response = new CartResponse();
        response.setCustomerId(CUSTOMER_ID);
        return response;
    }

    private OrderResponse orderResponse() {
        // Same assumption as above for OrderResponse.
        return new OrderResponse();
    }
}
