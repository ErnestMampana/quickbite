package za.co.ernest.quickbite.restaurant_service.dto;

import java.util.List;

public record RestaurantResponse(
        Long id,
        String name,
        String address,
        String phoneNumber,
        boolean isOpen,
        List<MenuItemResponse> menu
) {}
