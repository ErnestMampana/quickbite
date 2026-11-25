package za.co.ernest.quickbite.order_and_operations_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(
        name = "restaurant-service",
        contextId = "restaurantServiceClient",
        url = "${app.services.restaurant-url:http://localhost:8585}"
)
public interface RestaurantClient {

    @GetMapping("/api/v1/restaurants/validate/{id}")
    Boolean isRestaurantActive(@PathVariable("id") Long id);
}
