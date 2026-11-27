package za.co.ernest.quickbite.restaurant_service.dto;

import java.math.BigDecimal;
import java.util.List;

public record MenuItemResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        boolean isAvailable,
        String category,
        List<String> imageUrls
) {}
