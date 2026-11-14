package za.co.ernest.quickbite.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PasswordResetRequest(
        @Email(message = "Invalid email format") @NotBlank(message = "Email is required") String email
) {}
