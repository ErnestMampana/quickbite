package za.co.ernest.quickbite.order_and_operations_service.client;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "authentication-service",
        url = "${app.services.authentication-url}",
        contextId = "authenticationServiceClient"
)
public interface CustomerClient {

    @GetMapping("/api/v1/auth/get-user/{userId}")
    String getUserEmail(@PathVariable int userId);
}
