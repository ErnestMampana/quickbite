package za.co.ernest.quickbite.resturantService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRestaurantRequest(
        @NotBlank(message = "Restaurant name cannot be blank")
        @Size(min = 2, max = 100, message = "Restaurant name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Operational address is required")
        @Size(max = 255, message = "Address cannot exceed 255 characters")
        String address,

        @NotBlank(message = "Contact phone number is required")
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Invalid contact phone number format")
        String phoneNumber
) {}
