package za.co.ernest.quickbite.restaurant_service.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import za.co.ernest.quickbite.restaurant_service.dto.CreateMenuItemRequest;
import za.co.ernest.quickbite.restaurant_service.dto.MenuItemResponse;
import za.co.ernest.quickbite.restaurant_service.dto.UpdateProductRequest;
import za.co.ernest.quickbite.restaurant_service.service.ProductService;
import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;


    @PostMapping("/{restaurantId}/add")
    public ResponseEntity<MenuItemResponse> addProduct(
            @PathVariable Long restaurantId,
            @RequestHeader("X-Authenticated-User-Id") Long authenticatedOwnerId,
            @Valid @RequestBody CreateMenuItemRequest request) {

        MenuItemResponse response = productService.uploadProduct(restaurantId, authenticatedOwnerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{restaurantId}/products/{itemId}")
    public ResponseEntity<MenuItemResponse> updateProductDetails(
            @PathVariable Long restaurantId,
            @PathVariable Long itemId,
            @RequestHeader("X-Authenticated-User-Id") Long authenticatedOwnerId,
            @Valid @RequestBody UpdateProductRequest request) {

        MenuItemResponse response = productService.updateProductDetails(restaurantId, itemId, authenticatedOwnerId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{restaurantId}/products/{itemId}")
    public ResponseEntity<MenuItemResponse> getProductDetails(
            @PathVariable Long restaurantId,
            @PathVariable Long itemId,
            @RequestHeader("X-Authenticated-User-Id") Long authenticatedOwnerId) {

        MenuItemResponse response = productService.getProductDetails(restaurantId, itemId, authenticatedOwnerId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{restaurantId}/products/by-name/{productName}")
    public ResponseEntity<MenuItemResponse> getProductDetailsByName(
            @PathVariable Long restaurantId,
            @PathVariable String productName,
            @RequestHeader("X-Authenticated-User-Id") Long authenticatedOwnerId) {

        MenuItemResponse response = productService.getProductDetailsByName(restaurantId, productName, authenticatedOwnerId);
        return ResponseEntity.ok(response);
    }
}
