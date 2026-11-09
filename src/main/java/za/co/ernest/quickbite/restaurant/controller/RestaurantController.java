package za.co.ernest.quickbite.restaurant.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.ernest.quickbite.restaurant.dto.RestaurantDTO;
import za.co.ernest.quickbite.restaurant.service.ProductService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/restaurant")
public class controller {

    private final ProductService productService;

    public ResponseEntity<RestaurantDTO> register(@Valid @RequestBody RestaurantDTO restaurantDTO){
        return new ResponseEntity<>(productService.createRestaurant(restaurantDTO), HttpStatus. CREATED);
    }
}
