package za.co.ernest.payment_service.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import za.co.ernest.payment_service.dto.InitializePaymentRequest;
import za.co.ernest.payment_service.dto.PaymentResponse;
import za.co.ernest.payment_service.dto.PaystackInitResponse;
import za.co.ernest.payment_service.entity.Payment;
import za.co.ernest.payment_service.enums.PaymentStatus;
import za.co.ernest.payment_service.dto.PaymentEvent;
import za.co.ernest.payment_service.kafka.PaymentEventPublisher;
import za.co.ernest.payment_service.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher eventPublisher;
    private final PaystackSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String callbackUrl;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentEventPublisher eventPublisher,
            PaystackSignatureVerifier signatureVerifier,
            ObjectMapper objectMapper,
            @Value("${paystack.api.base-url}") String baseUrl,
            @Value("${paystack.api.secret-key}") String secretKey,
            @Value("${paystack.api.callback-url}") String callbackUrl) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
        this.signatureVerifier = signatureVerifier;
        this.objectMapper = objectMapper;
        this.callbackUrl = callbackUrl;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + secretKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "QuickBite-PaymentService/1.0")
                .build();
    }

    @Transactional
    public PaymentResponse initializePayment(InitializePaymentRequest request) {
        Optional<Payment> existingActivePayment = paymentRepository.findFirstByOrderIdAndStatusIn(
                request.orderId(),
                List.of(PaymentStatus.SUCCESS, PaymentStatus.PENDING)
        );

        if (existingActivePayment.isPresent()) {
            Payment existing = existingActivePayment.get();
            log.info("Returning existing [{}] payment [ref={}] for order [{}]",
                    existing.getStatus(), existing.getReference(), request.orderId());
            return toResponse(existing);
        }
        String reference = "QB-" + request.orderId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long amountInCents = request.amountZar().multiply(BigDecimal.valueOf(100)).longValueExact();

        Map<String, Object> paystackPayload = Map.of(
                "email", request.email(),
                "amount", amountInCents,
                "currency", "ZAR",
                "reference", reference,
                "callback_url", callbackUrl
        );

        PaystackInitResponse response = restClient.post()
                .uri("/transaction/initialize")
                .body(paystackPayload)
                .retrieve()
                .body(PaystackInitResponse.class);

        if (response == null || !response.status() || response.data() == null) {
            throw new IllegalStateException("Failed to initialize Paystack transaction.");
        }

        Payment payment = Payment.builder()
                .reference(reference)
                .orderId(request.orderId())
                .customerEmail(request.email())
                .amountZar(request.amountZar())
                .currency("ZAR")
                .status(PaymentStatus.PENDING)
                .authorizationUrl(response.data().authorizationUrl())
                .accessCode(response.data().accessCode())
                .build();

        paymentRepository.save(payment);
        log.info("Initialized Paystack payment [ref={}] for order [{}]", reference, request.orderId());

        return toResponse(payment);
    }

    @Transactional
    public void processWebhook(String rawPayload, String signatureHeader) {
        if (!signatureVerifier.isValidSignature(rawPayload, signatureHeader)) {
            log.warn("Rejected Paystack webhook with invalid HMAC signature.");
            throw new SecurityException("Invalid Paystack webhook signature.");
        }

        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String eventType = root.path("event").asText();
            JsonNode data = root.path("data");
            String reference = data.path("reference").asText();
            String channel = data.path("channel").asText("unknown");

            Payment payment = paymentRepository.findByReference(reference)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown payment reference: " + reference));

            // Idempotency guard: ignore duplicate webhooks if already finalized
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                log.info("Ignoring duplicate webhook for already completed payment [{}]", reference);
                return;
            }

            if ("charge.success".equals(eventType)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaymentChannel(channel);
                paymentRepository.save(payment);

                eventPublisher.publishPaymentEvent(new PaymentEvent(
                        payment.getReference(),
                        payment.getOrderId(),
                        payment.getCustomerEmail(),
                        payment.getAmountZar(),
                        PaymentStatus.SUCCESS,
                        channel,
                        Instant.now()
                ));
            } else if ("charge.failed".equals(eventType)) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setPaymentChannel(channel);
                paymentRepository.save(payment);

                eventPublisher.publishPaymentEvent(new PaymentEvent(
                        payment.getReference(),
                        payment.getOrderId(),
                        payment.getCustomerEmail(),
                        payment.getAmountZar(),
                        PaymentStatus.FAILED,
                        channel,
                        Instant.now()
                ));
            }
        } catch (Exception e) {
            log.error("Failed to parse Paystack webhook payload: {}", e.getMessage(), e);
            throw new IllegalStateException("Webhook processing failed", e);
        }
    }

    @Transactional
    public PaymentResponse verifyPayment(String reference) {
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Unknown payment reference: " + reference));

        // Idempotency guard: if already SUCCESS, return immediately without re-publishing Kafka event
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return toResponse(payment);
        }

        String rawResponse = restClient.get()
                .uri("/transaction/verify/{reference}", reference)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode data = root.path("data");
            String gatewayStatus = data.path("status").asText(); // "success", "failed", "abandoned"
            String channel = data.path("channel").asText("unknown");

            if ("success".equalsIgnoreCase(gatewayStatus)) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaymentChannel(channel);
                paymentRepository.save(payment);

                eventPublisher.publishPaymentEvent(new PaymentEvent(
                        payment.getReference(),
                        payment.getOrderId(),
                        payment.getCustomerEmail(),
                        payment.getAmountZar(),
                        PaymentStatus.SUCCESS,
                        channel,
                        Instant.now()
                ));
                log.info("Verified and marked payment [{}] as SUCCESS via channel [{}]", reference, channel);
            } else if ("failed".equalsIgnoreCase(gatewayStatus)) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setPaymentChannel(channel);
                paymentRepository.save(payment);
            }

            return toResponse(payment);
        } catch (Exception e) {
            log.error("Failed to verify Paystack transaction [{}]: {}", reference, e.getMessage(), e);
            throw new IllegalStateException("Payment verification failed", e);
        }
    }


    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("No payment found for order ID: " + orderId));
        return toResponse(payment);
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getReference(),
                payment.getOrderId(),
                payment.getAmountZar(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getAuthorizationUrl()
        );
    }
}

