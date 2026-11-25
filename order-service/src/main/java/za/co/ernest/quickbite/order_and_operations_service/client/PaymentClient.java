package za.co.ernest.quickbite.order_and_operations_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import za.co.ernest.quickbite.order_and_operations_service.dto.PaymentRequest;
import za.co.ernest.quickbite.order_and_operations_service.dto.PaymentResponse;

@FeignClient(
        name = "payment-service",
        url = "${app.services.payment-url}",
        contextId = "paymentServiceClient"
)
public interface PaymentClient {

    @PostMapping("/api/v1/payments/initialize")
    PaymentResponse initializePayment(@RequestBody PaymentRequest request);

}

