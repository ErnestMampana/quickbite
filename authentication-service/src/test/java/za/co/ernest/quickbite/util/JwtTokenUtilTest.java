package za.co.ernest.quickbite.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.ernest.quickbite.entity.User;
import za.co.ernest.quickbite.enums.AccountStatus;
import za.co.ernest.quickbite.enums.UserRole;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenUtilTest {

    private static final String TEST_SECRET = "SecureSigningKeyLongEnoughToMeetHMACMinBits256RequirementString";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hour

    private JwtTokenUtil jwtTokenUtil;
    private SecretKey verificationKey;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        jwtTokenUtil = new JwtTokenUtil(TEST_SECRET, EXPIRATION_MS);
        verificationKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

        sampleUser = User.builder()
                .id(42L)
                .username("ernestm")
                .email("ernest@quickbite.co.za")
                .firstName("Ernest")
                .lastName("Mohlala")
                .role(UserRole.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void mintAccessToken_ShouldEmbedExpectedSubjectClaimsAndExpiration() {
        String token = jwtTokenUtil.mintAccessToken(sampleUser);

        assertThat(token).isNotBlank();

        Claims claims = Jwts.parser()
                .verifyWith(verificationKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("ernest@quickbite.co.za");
        assertThat(claims.get("role", String.class)).isEqualTo("CUSTOMER");
        assertThat(claims.get("status", String.class)).isEqualTo("ACTIVE");

        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        assertThat(issuedAt).isNotNull();
        assertThat(expiration).isNotNull();
        assertThat(expiration.getTime() - issuedAt.getTime()).isEqualTo(EXPIRATION_MS);
    }

    @Test
    void mintAccessToken_ShouldGenerateUniqueTokens_WhenCalledInSameMillisecond() {
        String firstToken = jwtTokenUtil.mintAccessToken(sampleUser);
        String secondToken = jwtTokenUtil.mintAccessToken(sampleUser);

        assertThat(firstToken).isNotEqualTo(secondToken);
    }

    @Test
    void mintAccessToken_ShouldFailVerification_WhenParsedWithWrongSigningKey() {
        String token = jwtTokenUtil.mintAccessToken(sampleUser);
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "CompletelyDifferentSigningKeyThatIsLongEnoughForHmac256".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> Jwts.parser()
                .verifyWith(wrongKey)
                .build()
                .parseSignedClaims(token))
                .isInstanceOf(SignatureException.class);
    }
}
