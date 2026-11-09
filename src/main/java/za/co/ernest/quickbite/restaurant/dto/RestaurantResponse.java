package za.co.ernest.quickbite.resturantService.dto;

import java.util.List;

public record RestaurantResponse(
        Long id,
        String name,
        String address,
        String phoneNumber,
        boolean isOpen,
        List<MenuItemResponse> menu
) {}
