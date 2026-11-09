package za.co.ernest.quickbite.dto;

public record AuthResponse(
        String token,
        String tokenType,
        String email,
        String role
) {}
