package za.co.ernest.quickbite.util;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class OtpGenerator {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public String generateSixDigitOtp() {
        int code = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(code);
    }
}
