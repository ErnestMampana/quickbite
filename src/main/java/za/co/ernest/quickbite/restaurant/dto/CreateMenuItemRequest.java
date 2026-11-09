package za.co.ernest.quickbite.resturantService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateMenuItemRequest(
        @NotBlank(message = "Product title cannot be blank")
        @Size(min = 3, max = 100, message = "Product title must be between 3 and 100 characters")
        String name,

        @NotBlank(message = "Description of ingredients is required")
        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @NotNull(message = "Base price is required")
        @Positive(message = "Price must be a positive value greater than zero")
        BigDecimal price,

        @NotBlank(message = "Product category classification is required")
        @Pattern(regexp = "(?i)^(KOTA|MEAL|DRINK)$", message = "Category must match either KOTA, MEAL, or DRINK")
        String category
) {}
