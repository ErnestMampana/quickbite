package za.co.ernest.quickbite.restaurant_service.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.ernest.quickbite.restaurant_service.dto.CreateRestaurantRequest;
import za.co.ernest.quickbite.restaurant_service.dto.RestaurantResponse;
import za.co.ernest.quickbite.restaurant_service.service.RestaurantService;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/restaurant")
public class RestaurantController {

    private final RestaurantService restaurantService;

    @PostMapping("/register")
    public ResponseEntity<RestaurantResponse> register(
            @RequestHeader("X-Authenticated-User-Id") Long authenticatedOwnerId,
            @Valid @RequestBody CreateRestaurantRequest request) {

        RestaurantResponse response = restaurantService.registerRestaurant(authenticatedOwnerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/validate/{id}")
    public ResponseEntity<Boolean> validateRestaurantForOrder(@PathVariable Long id) {
        boolean isValid = restaurantService.isRestaurantActiveAndValid(id);
        return ResponseEntity.ok(isValid);
    }


//    @PatchMapping("/{id}/suspend")
//    public ResponseEntity<Void> suspendRestaurant(
//            @RequestHeader("X-Authenticated-User-Role") String role, // Extracted securely upstream
//            @PathVariable Long id) {
//
//        if (!"ADMIN".equalsIgnoreCase(role)) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
//        }
//
//        restaurantService.suspendRestaurant(id);
//        return ResponseEntity.noContent().build();
//    }

}
