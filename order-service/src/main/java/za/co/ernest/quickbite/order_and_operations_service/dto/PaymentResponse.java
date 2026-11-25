package za.co.ernest.quickbite.order_and_operations_service.dto;

public record PaymentResponse(
        String reference,
        String status,
        String authorizationUrl
) {}
