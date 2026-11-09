package za.co.ernest.quickbite.cart.client;


import org.springframework.cloud.openfeign.FeignClient;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "order-service",
        contextId = "orderServiceClient",
        url = "${app.services.order-url:http://localhost:8084}"
)
public interface OrderClient {
    @PostMapping("/api/v1/orders")
    OrderResponse placeOrder(
            @RequestHeader("X-Authenticated-User-Id") Long authenticatedCustomerId,
            @RequestBody OrderRequest request
    );
}




