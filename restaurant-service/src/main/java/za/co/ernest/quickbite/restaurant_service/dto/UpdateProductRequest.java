package za.co.ernest.quickbite.restaurant_service.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank(message = "Product name cannot be blank")
        @Size(min = 3, max = 100, message = "Product name must be between 3 and 100 characters")
        String name,

        @NotBlank(message = "Description of ingredients is required")
        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be a positive value greater than zero")
        BigDecimal price,

        @NotNull(message = "Availability status must be specified")
        Boolean isAvailable,

        @NotBlank(message = "Product category classification is required")
        @Pattern(regexp = "(?i)^(KOTA|MEAL|DRINK)$", message = "Category must match either KOTA, MEAL, or DRINK")
        String category
) {}
