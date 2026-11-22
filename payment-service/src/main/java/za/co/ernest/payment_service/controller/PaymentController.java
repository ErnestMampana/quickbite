package za.co.ernest.payment_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.ernest.payment_service.dto.InitializePaymentRequest;
import za.co.ernest.payment_service.dto.PaymentResponse;
import za.co.ernest.payment_service.service.PaymentService;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initialize")
    public ResponseEntity<PaymentResponse> initializePayment(@Valid @RequestBody InitializePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.initializePayment(request));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handlePaystackWebhook(
            @RequestBody String rawPayload,
            @RequestHeader(value = "x-paystack-signature", required = false) String signature) {
        paymentService.processWebhook(rawPayload, signature);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId));
    }

    @GetMapping("/verify/{reference}")
    public ResponseEntity<PaymentResponse> verifyPayment(@PathVariable String reference) {
        return ResponseEntity.ok(paymentService.verifyPayment(reference));
    }

}

