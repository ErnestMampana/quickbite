package za.co.ernest.quickbite.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import za.co.ernest.quickbite.entity.User;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtTokenUtil {

    private final Key signingKey;
    private final long expirationDurationMs;

    public JwtTokenUtil(
            @Value("${app.security.jwt.secret:SecureSigningKeyLongEnoughToMeetHMACMinBits256RequirementString}") String secret,
            @Value("${app.security.jwt.expiration-ms:86400000}") long expiration) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationDurationMs = expiration;
    }

    public String mintAccessToken(User user) {
        Date generationTime = new Date();
        Date expirationTime = new Date(generationTime.getTime() + expirationDurationMs);

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .setClaims(Map.of(
                        "email", user.getEmail(),
                        "role", user.getRole().name(),
                        "firstName", user.getFirstName(),
                        "lastName", user.getLastName()
                ))
                .setIssuedAt(generationTime)
                .setExpiration(expirationTime)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
