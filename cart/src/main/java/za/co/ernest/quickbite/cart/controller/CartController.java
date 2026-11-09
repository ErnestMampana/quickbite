package za.co.ernest.quickbite.cart.controller;


import lombok.RequiredArgsConstructor;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.ernest.quickbite.cart.client.OrderResponse;
import za.co.ernest.quickbite.cart.dto.AddToCartRequest;
import za.co.ernest.quickbite.cart.dto.CartResponse;
import za.co.ernest.quickbite.cart.service.CartService;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")

public class CartController {

    private final CartService cartService;


    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @RequestHeader("X-Authenticated-User-Id") Long authenticatedCustomerId) {
        return ResponseEntity.ok(cartService.getCart(authenticatedCustomerId));
    }


    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItemToCart(
            @RequestHeader("X-Authenticated-User-Id") Long authenticatedCustomerId,
            @Valid @RequestBody AddToCartRequest request) {
        return ResponseEntity.ok(cartService.addProductToCart(authenticatedCustomerId, request));
    }

    @PostMapping("/checkout/{customerId}")
    public ResponseEntity<OrderResponse> checkout(@PathVariable("customerId") Long customerId) {
        OrderResponse orderResponse = cartService.checkout(customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
    }


    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @RequestHeader("X-Authenticated-User-Id") Long authenticatedCustomerId) {
        cartService.clearCartCache(authenticatedCustomerId);
        return ResponseEntity.noContent().build();
    }
}
