package za.co.ernest.quickbite.restaurant_service.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import za.co.ernest.quickbite.restaurant_service.dto.CreateRestaurantRequest;
import za.co.ernest.quickbite.restaurant_service.dto.RestaurantResponse;
import za.co.ernest.quickbite.restaurant_service.entity.Restaurant;
import za.co.ernest.quickbite.restaurant_service.enums.RestaurantStatus;
import za.co.ernest.quickbite.restaurant_service.exception.ResourceConflictException;
import za.co.ernest.quickbite.restaurant_service.repository.RestaurantRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @InjectMocks
    private RestaurantService restaurantService;

    @Test
    @DisplayName("registerRestaurant - should save new restaurant and return mapped response when phone and address are unique")
    void registerRestaurant_ValidUniqueInputs_SavesAndReturnsResponse() {
        Long ownerId = 42L;
        CreateRestaurantRequest request = new CreateRestaurantRequest(
                "Krugersdorp Flame Grill",
                "45 Commissioner Street, Krugersdorp",
                "0119531234"
        );

        when(restaurantRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(false);
        when(restaurantRepository.existsByAddressIgnoreCase(request.address())).thenReturn(false);
        when(restaurantRepository.save(any(Restaurant.class))).thenAnswer(invocation -> {
            Restaurant entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 1L);
            return entity;
        });

        RestaurantResponse response = restaurantService.registerRestaurant(ownerId, request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Krugersdorp Flame Grill", response.name());
        assertEquals("45 Commissioner Street, Krugersdorp", response.address());
        assertEquals("0119531234", response.phoneNumber());
        assertFalse(response.isOpen());
        assertTrue(response.menu().isEmpty());

        ArgumentCaptor<Restaurant> restaurantCaptor = ArgumentCaptor.forClass(Restaurant.class);
        verify(restaurantRepository).save(restaurantCaptor.capture());
        assertEquals(ownerId, restaurantCaptor.getValue().getOwnerId());
    }

    @Test
    @DisplayName("registerRestaurant - should throw ResourceConflictException when phone number already exists")
    void registerRestaurant_DuplicatePhoneNumber_ThrowsConflictException() {
        CreateRestaurantRequest request = new CreateRestaurantRequest(
                "Duplicate Phone Diner",
                "10 Market Street, Krugersdorp",
                "0119531234"
        );

        when(restaurantRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(true);

        ResourceConflictException ex = assertThrows(
                ResourceConflictException.class,
                () -> restaurantService.registerRestaurant(42L, request)
        );
        assertTrue(ex.getMessage().contains("Phone number is already registered"));

        verify(restaurantRepository, never()).existsByAddressIgnoreCase(any());
        verify(restaurantRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerRestaurant - should throw ResourceConflictException when address already exists (case-insensitive)")
    void registerRestaurant_DuplicateAddress_ThrowsConflictException() {
        CreateRestaurantRequest request = new CreateRestaurantRequest(
                "Duplicate Address Cafe",
                "45 COMMISSIONER STREET, KRUGERSDORP",
                "0119539999"
        );

        when(restaurantRepository.existsByPhoneNumber(request.phoneNumber())).thenReturn(false);
        when(restaurantRepository.existsByAddressIgnoreCase(request.address())).thenReturn(true);

        ResourceConflictException ex = assertThrows(
                ResourceConflictException.class,
                () -> restaurantService.registerRestaurant(42L, request)
        );
        assertTrue(ex.getMessage().contains("already operating at this address"));

        verify(restaurantRepository, never()).save(any());
    }

    @Test
    @DisplayName("isRestaurantActiveAndValid - should return true when restaurant exists and is ACTIVE")
    void isRestaurantActiveAndValid_ActiveRestaurant_ReturnsTrue() {
        Restaurant activeRestaurant = Restaurant.builder()
                .name("Active Bistro")
                .address("12 Main Rd")
                .phoneNumber("0115550001")
                .ownerId(10L)
                .build();

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(activeRestaurant));

        assertTrue(restaurantService.isRestaurantActiveAndValid(1L));
    }

    @Test
    @DisplayName("isRestaurantActiveAndValid - should return false when restaurant is SUSPENDED")
    void isRestaurantActiveAndValid_SuspendedRestaurant_ReturnsFalse() {
        Restaurant suspendedRestaurant = Restaurant.builder()
                .name("Suspended Bistro")
                .address("14 Main Rd")
                .phoneNumber("0115550002")
                .ownerId(10L)
                .build();
        ReflectionTestUtils.setField(suspendedRestaurant, "status", RestaurantStatus.SUSPENDED);

        when(restaurantRepository.findById(2L)).thenReturn(Optional.of(suspendedRestaurant));

        assertFalse(restaurantService.isRestaurantActiveAndValid(2L));
    }

    @Test
    @DisplayName("isRestaurantActiveAndValid - should return false when restaurant ID does not exist")
    void isRestaurantActiveAndValid_NonExistentRestaurant_ReturnsFalse() {
        when(restaurantRepository.findById(999L)).thenReturn(Optional.empty());

        assertFalse(restaurantService.isRestaurantActiveAndValid(999L));
    }
}

