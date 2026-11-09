package za.co.ernest.quickbite.restaurant_service.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import za.co.ernest.quickbite.restaurant_service.dto.UpdateProductRequest;
import za.co.ernest.quickbite.restaurant_service.exception.ResourceNotFoundException;
import za.co.ernest.quickbite.restaurant_service.dto.CreateMenuItemRequest;
import za.co.ernest.quickbite.restaurant_service.dto.MenuItemResponse;
import za.co.ernest.quickbite.restaurant_service.entity.MenuItem;
import za.co.ernest.quickbite.restaurant_service.entity.Restaurant;
import za.co.ernest.quickbite.restaurant_service.enums.ProductCategory;
import za.co.ernest.quickbite.restaurant_service.exception.UnauthorizedOwnershipException;
import za.co.ernest.quickbite.restaurant_service.repository.RestaurantRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {
    //private final DriverDetailsRepository driverDetailsRepository;

    private final RestaurantRepository restaurantRepository;


    @Transactional
    public MenuItemResponse uploadProduct(Long restaurantId, Long authenticatedOwnerId, CreateMenuItemRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with ID: " + restaurantId));

        if (!restaurant.isOwnedBy(authenticatedOwnerId)) {
            throw new UnauthorizedOwnershipException("Access Denied: Conflicting tenant ownership identifiers.");
        }

        ProductCategory targetCategory = ProductCategory.valueOf(request.category().toUpperCase());

        targetCategory.validateConstraints(request);

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

    @Transactional
    public MenuItemResponse updateProductDetails(Long restaurantId, Long itemId, Long authenticatedOwnerId, UpdateProductRequest request) {

        Restaurant restaurant = validateOwnership(restaurantId, authenticatedOwnerId);

        MenuItem menuItem = restaurant.findNestedItemOrThrow(itemId);

        ProductCategory targetCategory = ProductCategory.valueOf(request.category().toUpperCase());

        CreateMenuItemRequest validationWrapper = new CreateMenuItemRequest(
                request.name(), request.description(), request.price(), request.category()
        );
        targetCategory.validateConstraints(validationWrapper);

        menuItem.updateDetails(
                request.name(),
                request.description(),
                request.price(),
                request.isAvailable(),
                targetCategory
        );

        restaurantRepository.save(restaurant);
        log.info("Successfully updated whole profile specs for Item ID [{}] under Restaurant ID [{}]", itemId, restaurantId);

        return mapToResponse(menuItem);
    }

    public MenuItemResponse getProductDetails(Long restaurantId, Long itemId, Long authenticatedOwnerId) {
        Restaurant restaurant = validateOwnership(restaurantId, authenticatedOwnerId);
        MenuItem menuItem = restaurant.findNestedItemOrThrow(itemId);

        log.debug("Frontend retrieved immutable product specs for Item ID [{}] under Restaurant ID [{}]", itemId, restaurantId);
        return mapToResponse(menuItem);
    }

    public MenuItemResponse getProductDetailsByName(Long restaurantId, String productName, Long authenticatedOwnerId) {
        Restaurant restaurant = validateOwnership(restaurantId, authenticatedOwnerId);
        MenuItem menuItem = restaurant.findNestedItemByNameOrThrow(productName);
        return mapToResponse(menuItem);
    }

    private MenuItemResponse mapToResponse(MenuItem menuItem) {
        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getName(),
                menuItem.getDescription(),
                menuItem.getPrice(),
                menuItem.isAvailable(),
                menuItem.getCategory().name());
    }

    private Restaurant validateOwnership(Long restaurantId, Long authenticatedOwnerId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant profile not found with ID: " + restaurantId));

        if (!restaurant.isOwnedBy(authenticatedOwnerId)) {
            throw new UnauthorizedOwnershipException("Access Denied: You do not have permission to view this restaurant's catalog details.");
        }

        return restaurant;
    }
}
