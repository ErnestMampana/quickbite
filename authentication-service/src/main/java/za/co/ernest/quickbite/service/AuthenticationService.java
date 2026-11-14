package za.co.ernest.quickbite.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.ernest.quickbite.Factory.RegistrationFactory;
import za.co.ernest.quickbite.Factory.RegistrationStrategy;
import za.co.ernest.quickbite.dto.*;
import za.co.ernest.quickbite.entity.Token;
import za.co.ernest.quickbite.entity.User;
import za.co.ernest.quickbite.enums.AccountStatus;
import za.co.ernest.quickbite.kafka.event.OtpNotificationEvent;
import za.co.ernest.quickbite.kafka.port.NotificationPublisher;
import za.co.ernest.quickbite.kafka.port.TokenGenerator;
import za.co.ernest.quickbite.repository.TokenRepository;
import za.co.ernest.quickbite.repository.UserAccountRepository;
import za.co.ernest.quickbite.util.OtpGenerator;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private static final int OTP_EXPIRY_MINUTES = 5;

    private final RegistrationFactory registrationFactory;
    private final UserAccountRepository userAccountRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenGenerator tokenGenerator;
    private final OtpGenerator otpGenerator;
    private final NotificationPublisher notificationPublisher;

    @Transactional
    public AuthResponse createUserAccount(UserDTO request) {
        log.info("Requesting account creation for role: [{}]", request.role());

        RegistrationStrategy strategy = registrationFactory.getStrategy(request.role());
        User newUser = strategy.register(request);

        if (newUser.getStatus() == AccountStatus.AWAITING_CONFIRMATION) {
            String otp = otpGenerator.generateSixDigitOtp();
            newUser.assignOtp(otp, OTP_EXPIRY_MINUTES);

            log.info("DEV ONLY - Verification OTP for [{}]: {}", newUser.getEmail(), otp);
            notificationPublisher.sendOtpEvent(new OtpNotificationEvent(
                    newUser.getEmail(), newUser.getFirstName(), otp, "ACCOUNT_VERIFICATION"
            ));
        }

        userAccountRepository.save(newUser);

        String jwtToken = tokenGenerator.mintAccessToken(newUser);
        saveUserToken(newUser, jwtToken);

        log.info("Account created and initial session token persisted for [{}]", newUser.getEmail());
        return buildUserResponse(newUser, jwtToken, "Account created. Please confirm the OTP sent to your email within 5 minutes.");
    }

    @Transactional
    public AuthResponse authenticateCredentials(LoginRequest request) {
        log.info("Processing login request for [{}]", request.email());

        User user = getUserByEmail(request.email());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        user.ensureIsHealthy();

        String jwtToken = tokenGenerator.mintAccessToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, jwtToken);

        log.info("User logged in successfully: [{}]", user.getEmail());
        String message = (user.getStatus() == AccountStatus.AWAITING_CONFIRMATION)
                ? "Login successful. Please verify your email using the OTP sent to you."
                : "Login successful.";

        return buildUserResponse(user, jwtToken, message);
    }

    @Transactional
    public AuthResponse confirmEmail(ConfirmEmailRequest request) {
        User user = getUserByEmail(request.email());
        user.confirmEmail(request.otp());
        userAccountRepository.save(user);

        // Rotate token so the JWT claims reflect the updated ACTIVE status
        String freshToken = tokenGenerator.mintAccessToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, freshToken);

        log.info("Email confirmed and account activated for [{}]", user.getEmail());
        return buildUserResponse(user, freshToken, "Email confirmed successfully. Your account is now active.");
    }

    @Transactional
    public String requestPasswordReset(String email) {
        User user = getUserByEmail(email);
        String otp = otpGenerator.generateSixDigitOtp();

        user.requestPasswordReset(otp, OTP_EXPIRY_MINUTES);
        userAccountRepository.save(user);
        revokeAllUserTokens(user);

        log.info("DEV ONLY - Password Reset OTP for [{}]: {}", user.getEmail(), otp);
        notificationPublisher.sendOtpEvent(new OtpNotificationEvent(
                user.getEmail(), user.getFirstName(), otp, "PASSWORD_RESET"
        ));

        return "Password reset OTP dispatched. Token expires in 5 minutes.";
    }

    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        User user = getUserByEmail(request.email());
        user.resetPassword(request.otp(), passwordEncoder.encode(request.newPassword()));
        userAccountRepository.save(user);

        String freshToken = tokenGenerator.mintAccessToken(user);
        revokeAllUserTokens(user);
        saveUserToken(user, freshToken);

        log.info("Password reset completed and new session issued for [{}]", user.getEmail());
        return buildUserResponse(user, freshToken, "Password updated successfully. Your account is active.");
    }

    @Transactional
    public String refreshOtp(String email) {
        User user = getUserByEmail(email);
        String newOtp = otpGenerator.generateSixDigitOtp();

        user.refreshOtp(newOtp, OTP_EXPIRY_MINUTES);
        userAccountRepository.save(user);

        String purpose = (user.getStatus() == AccountStatus.AWAITING_PWD_RESET) ? "PASSWORD_RESET" : "ACCOUNT_VERIFICATION";
        log.info("DEV ONLY - Refreshed OTP [{}] for [{}]: {}", purpose, user.getEmail(), newOtp);

        notificationPublisher.sendOtpEvent(new OtpNotificationEvent(
                user.getEmail(), user.getFirstName(), newOtp, purpose
        ));

        return "New OTP generated and dispatched. Valid for 5 minutes.";
    }

    private User getUserByEmail(String email) {
        return userAccountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User account not found for email: " + email));
    }

    private void saveUserToken(User user, String jwtToken) {
        Token token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType("BEARER")
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
    }

    private void revokeAllUserTokens(User user) {
        List<Token> validTokens = tokenRepository.findAllValidTokensByUser(user.getId());
        if (validTokens.isEmpty()) {
            return;
        }
        validTokens.forEach(t -> {
            t.setExpired(true);
            t.setRevoked(true);
        });
        tokenRepository.saveAll(validTokens);
    }

    private AuthResponse buildUserResponse(User user, String token, String message) {
        return new AuthResponse(
                user.getUsername(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getStatus(),
                token,
                message,
                Instant.now().toString()
        );
    }
}
