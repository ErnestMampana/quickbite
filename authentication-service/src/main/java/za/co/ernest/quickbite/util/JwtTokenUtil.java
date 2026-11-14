package za.co.ernest.quickbite.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import za.co.ernest.quickbite.entity.User;
import za.co.ernest.quickbite.kafka.port.TokenGenerator;

import java.nio.charset.StandardCharsets;


import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenUtil implements TokenGenerator {

    private final SecretKey signingKey;
    private final long expirationDurationMs;

    public JwtTokenUtil(
            @Value("${app.security.jwt.secret:SecureSigningKeyLongEnoughToMeetHMACMinBits256RequirementString}") String secret,
            @Value("${app.security.jwt.expiration-ms:86400000}") long expiration) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationDurationMs = expiration;
    }

    @Override
    public String mintAccessToken(User user) {
        Date generationTime = new Date();
        Date expirationTime = new Date(generationTime.getTime() + expirationDurationMs);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claims(Map.of(
                        "email", user.getEmail(),
                        "role", user.getRole().name(),
                        "status", user.getStatus().name()
                ))
                .issuedAt(generationTime)
                .expiration(expirationTime)
                .signWith(signingKey)
                .compact();
    }
}
