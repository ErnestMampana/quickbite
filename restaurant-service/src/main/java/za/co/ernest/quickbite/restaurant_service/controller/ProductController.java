package za.co.ernest.quickbite.restaurant_service.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import za.co.ernest.quickbite.restaurant_service.dto.CreateMenuItemRequest;
import za.co.ernest.quickbite.restaurant_service.dto.MenuItemResponse;
import za.co.ernest.quickbite.restaurant_service.dto.UpdateProductRequest;
import za.co.ernest.quickbite.restaurant_service.service.ProductService;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;


    @PostMapping(path = "/{restaurantId}/add",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MenuItemResponse> createMenuItem(
            @PathVariable Long restaurantId,
            @RequestHeader("X-Authenticated-User-Id") Long authenticatedOwnerId,
            @RequestPart("data") @Valid CreateMenuItemRequest request,
            @RequestPart("images") List<MultipartFile> images) {

        MenuItemResponse response = productService.uploadProduct(restaurantId, authenticatedOwnerId, request,images);
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
