package za.co.ernest.quickbite.dto;

import za.co.ernest.quickbite.enums.AccountStatus;
import za.co.ernest.quickbite.enums.UserRole;

public record AuthResponse(
        String username,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        AccountStatus status,
        String token,
        String message,
        String timestamp
) {}
