package za.co.ernest.quickbite.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import za.co.ernest.quickbite.enums.UserRole;

@Builder
public record UserDTO(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password,
        @Email(message = "Invalid email format") @NotBlank(message = "Email is required") String email,
        @NotBlank(message = "First name is required") String firstName,
        @NotBlank(message = "Last name is required") String lastName,
        @NotNull(message = "User role is required") UserRole role
) {}
