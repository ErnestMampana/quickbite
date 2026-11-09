package za.co.ernest.quickbite.restaurant_service.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import za.co.ernest.quickbite.restaurant_service.dto.CreateRestaurantRequest;
import za.co.ernest.quickbite.restaurant_service.dto.RestaurantResponse;
import za.co.ernest.quickbite.restaurant_service.entity.Restaurant;
import za.co.ernest.quickbite.restaurant_service.enums.RestaurantStatus;
import za.co.ernest.quickbite.restaurant_service.exception.ResourceConflictException;
import za.co.ernest.quickbite.restaurant_service.repository.RestaurantRepository;

import java.util.Collections;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;

    @Transactional
    public RestaurantResponse registerRestaurant(Long authenticatedOwnerId, CreateRestaurantRequest request) {

        if (restaurantRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new ResourceConflictException("Registration Failed: Phone number is already registered to a different profile.");
        }

        if (restaurantRepository.existsByAddressIgnoreCase(request.address())) {
            throw new ResourceConflictException("Registration Failed: A restaurant is already operating at this address.");
        }

        Restaurant restaurant = Restaurant.builder()
                .name(request.name())
                .address(request.address())
                .phoneNumber(request.phoneNumber())
                .ownerId(authenticatedOwnerId)
                .build();

        Restaurant savedRestaurant = restaurantRepository.save(restaurant);
        return mapToRestaurantResponse(savedRestaurant);
    }

    public boolean isRestaurantActiveAndValid(Long restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .map(restaurant -> restaurant.getStatus() != RestaurantStatus.SUSPENDED)
                .orElse(false);
    }

    private RestaurantResponse mapToRestaurantResponse(Restaurant restaurant) {

        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getAddress(),
                restaurant.getPhoneNumber(),
                restaurant.isOpen(),
                Collections.emptyList()
        );
    }


}
