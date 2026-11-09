package za.co.ernest.quickbite.dto;

import za.co.ernest.quickbite.enums.UserRole;

public record UserResponseDTO(
        String username,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        String message,
        String timestamp
) {}
