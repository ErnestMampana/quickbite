package za.co.ernest.quickbite.order_and_operations_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.ernest.quickbite.order_and_operations_service.dto.CreateOrderRequest;
import za.co.ernest.quickbite.order_and_operations_service.dto.OrderResponse;
import za.co.ernest.quickbite.order_and_operations_service.service.OrderService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(
            @RequestHeader("X-Authenticated-User-Id") Long authenticatedCustomerId,
            @Valid @RequestBody CreateOrderRequest request) {

        OrderResponse response = orderService.placeOrder(authenticatedCustomerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}/pay")
    public ResponseEntity<OrderResponse> payOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.handlePaymentSuccess(id));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<OrderResponse> acceptOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.handleKitchenAcceptance(id));
    }

//    @PatchMapping("/{id}/dispatch")
//    public ResponseEntity<OrderResponse> dispatchOrder(@PathVariable Long id) {
//        return ResponseEntity.ok(orderService.handleDriverDispatch(id));
//    }
//
//    @PatchMapping("/{id}/complete")
//    public ResponseEntity<OrderResponse> completeOrder(@PathVariable Long id) {
//        return ResponseEntity.ok(orderService.handleOrderDeliveryComplete(id));
//    }
}
