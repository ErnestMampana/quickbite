package za.co.ernest.quickbite.restaurant.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import za.co.ernest.quickbite.AuthenticationService.Exception.ResourceNotFoundException;
import za.co.ernest.quickbite.restaurant.dto.CreateMenuItemRequest;
import za.co.ernest.quickbite.restaurant.dto.MenuItemResponse;
import za.co.ernest.quickbite.restaurant.entity.MenuItem;
import za.co.ernest.quickbite.restaurant.entity.Restaurant;
import za.co.ernest.quickbite.restaurant.enums.ProductCategory;
import za.co.ernest.quickbite.restaurant.repository.RestaurantRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantService {
    //private final DriverDetailsRepository driverDetailsRepository;

    private final RestaurantRepository restaurantRepository;


    @Transactional
    public MenuItemResponse uploadProduct(Long restaurantId, Long authenticatedOwnerId, CreateMenuItemRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with ID: " + restaurantId));

//        if (!restaurant.isOwnedBy(authenticatedOwnerId)) {
//            throw new UnauthorizedOwnershipException("Access Denied: Conflicting tenant ownership identifiers.");
//        }

        // 1. Safe parsing of incoming string to Rich Domain Enum
        ProductCategory targetCategory = ProductCategory.valueOf(request.category().toUpperCase());

        // 2. Delegate strategy execution directly to the enum instance
        targetCategory.validateConstraints(request);

        // 3. Construct entity with precise typing
        MenuItem menuItem = MenuItem.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .category(targetCategory)
                .build();

        restaurant.addMenuItem(menuItem);
        restaurantRepository.save(restaurant);

        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getName(),
                menuItem.getDescription(),
                menuItem.getPrice(),
                menuItem.isAvailable(),
                menuItem.getCategory().name()
        );
    }
}
