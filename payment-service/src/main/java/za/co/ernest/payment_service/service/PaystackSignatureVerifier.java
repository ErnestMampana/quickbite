package za.co.ernest.payment_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
public class PaystackSignatureVerifier {

    private static final String HMAC_SHA512 = "HmacSHA512";
    private final String secretKey;

    public PaystackSignatureVerifier(@Value("${paystack.api.secret-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    public boolean isValidSignature(String rawPayload, String paystackSignatureHeader) {
        if (paystackSignatureHeader == null || paystackSignatureHeader.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA512);
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            String computedHex = HexFormat.of().formatHex(rawHmac);

            // Constant-time comparison prevents timing attacks
            return MessageDigest.isEqual(
                    computedHex.getBytes(StandardCharsets.UTF_8),
                    paystackSignatureHeader.toLowerCase().getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }
}

