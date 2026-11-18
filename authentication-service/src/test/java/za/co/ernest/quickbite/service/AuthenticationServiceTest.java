package za.co.ernest.quickbite.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import za.co.ernest.quickbite.Exception.AccountNotReadyException;
import za.co.ernest.quickbite.Exception.InvalidOtpException;
import za.co.ernest.quickbite.Factory.RegistrationFactory;
import za.co.ernest.quickbite.Factory.RegistrationStrategy;
import za.co.ernest.quickbite.dto.*;
import za.co.ernest.quickbite.entity.Token;
import za.co.ernest.quickbite.entity.User;
import za.co.ernest.quickbite.enums.AccountStatus;
import za.co.ernest.quickbite.enums.UserRole;
import za.co.ernest.quickbite.kafka.event.OtpNotificationEvent;
import za.co.ernest.quickbite.kafka.port.NotificationPublisher;
import za.co.ernest.quickbite.kafka.port.TokenGenerator;
import za.co.ernest.quickbite.repository.TokenRepository;
import za.co.ernest.quickbite.repository.UserAccountRepository;
import za.co.ernest.quickbite.util.OtpGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private RegistrationFactory registrationFactory;
    @Mock private RegistrationStrategy registrationStrategy;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private TokenRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenGenerator tokenGenerator;
    @Mock private OtpGenerator otpGenerator;
    @Mock private NotificationPublisher notificationPublisher;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User pendingCustomer;
    private User activeCustomer;

    @BeforeEach
    void setUp() {
        pendingCustomer = User.builder()
                .id(1L)
                .username("ernestm")
                .email("ernest@quickbite.co.za")
                .password("encodedPass")
                .firstName("Ernest")
                .lastName("Mohlala")
                .role(UserRole.CUSTOMER)
                .status(AccountStatus.AWAITING_CONFIRMATION)
                .otpCode("123456")
                .otpExpiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        activeCustomer = User.builder()
                .id(2L)
                .username("activeuser")
                .email("active@quickbite.co.za")
                .password("encodedPass")
                .firstName("Active")
                .lastName("User")
                .role(UserRole.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void createUserAccount_ShouldGenerateOtpPublishKafkaEventAndMintToken_WhenCustomerRegisters() {
        UserDTO request = new UserDTO("ernestm", "Pass@123", "ernest@quickbite.co.za", "Ernest", "Mohlala", UserRole.CUSTOMER);
        User unconfirmedUser = User.createPendingUser("ernestm", "encodedPass", "ernest@quickbite.co.za", "Ernest", "Mohlala", UserRole.CUSTOMER);

        when(registrationFactory.getStrategy(UserRole.CUSTOMER)).thenReturn(registrationStrategy);
        when(registrationStrategy.register(request)).thenReturn(unconfirmedUser);
        when(otpGenerator.generateSixDigitOtp()).thenReturn("654321");
        when(tokenGenerator.mintAccessToken(unconfirmedUser)).thenReturn("mock-jwt-token");

        AuthResponse response = authenticationService.createUserAccount(request);

        assertThat(response.token()).isEqualTo("mock-jwt-token");
        assertThat(response.status()).isEqualTo(AccountStatus.AWAITING_CONFIRMATION);
        assertThat(unconfirmedUser.getOtpCode()).isEqualTo("654321");

        verify(userAccountRepository).save(unconfirmedUser);
        verify(tokenRepository).save(any(Token.class));

        ArgumentCaptor<OtpNotificationEvent> eventCaptor = ArgumentCaptor.forClass(OtpNotificationEvent.class);
        verify(notificationPublisher).sendOtpEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().otpCode()).isEqualTo("654321");
    }

    @Test
    void createUserAccount_ShouldSkipOtpAndKafkaEvent_WhenAdminRegisters() {
        UserDTO adminRequest = new UserDTO("admin1", "Pass@123", "admin@quickbite.co.za", "Admin", "One", UserRole.ADMIN);
        User adminUser = User.createPendingUser("admin1", "encodedPass", "admin@quickbite.co.za", "Admin", "One", UserRole.ADMIN);

        when(registrationFactory.getStrategy(UserRole.ADMIN)).thenReturn(registrationStrategy);
        when(registrationStrategy.register(adminRequest)).thenReturn(adminUser);
        when(tokenGenerator.mintAccessToken(adminUser)).thenReturn("admin-jwt");

        AuthResponse response = authenticationService.createUserAccount(adminRequest);

        assertThat(response.status()).isEqualTo(AccountStatus.ACTIVE);
        verifyNoInteractions(otpGenerator, notificationPublisher);
    }

    @Test
    void authenticateCredentials_ShouldRevokeOldTokensAndIssueNewToken_WhenCredentialsAreValidAndAccountIsActive() {
        LoginRequest loginRequest = new LoginRequest("active@quickbite.co.za", "Pass@123");
        Token oldToken = Token.builder().id(10L).token("old-jwt").expired(false).revoked(false).build();

        when(userAccountRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(activeCustomer));
        when(passwordEncoder.matches("Pass@123", "encodedPass")).thenReturn(true);
        when(tokenGenerator.mintAccessToken(activeCustomer)).thenReturn("new-jwt");
        when(tokenRepository.findAllValidTokensByUser(2L)).thenReturn(List.of(oldToken));

        AuthResponse response = authenticationService.authenticateCredentials(loginRequest);

        assertThat(response.token()).isEqualTo("new-jwt");
        assertThat(oldToken.isExpired()).isTrue();
        assertThat(oldToken.isRevoked()).isTrue();
        verify(tokenRepository).saveAll(List.of(oldToken));
        verify(tokenRepository).save(any(Token.class));
    }

    @Test
    void authenticateCredentials_ShouldThrowException_WhenPasswordDoesNotMatch() {
        LoginRequest loginRequest = new LoginRequest("active@quickbite.co.za", "WrongPass");

        when(userAccountRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(activeCustomer));
        when(passwordEncoder.matches("WrongPass", "encodedPass")).thenReturn(false);

        assertThatThrownBy(() -> authenticationService.authenticateCredentials(loginRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid email or password.");

        verifyNoInteractions(tokenGenerator, tokenRepository);
    }

    @Test
    void authenticateCredentials_ShouldThrowAccountNotReadyException_WhenAccountIsUnconfirmed() {
        LoginRequest loginRequest = new LoginRequest("ernest@quickbite.co.za", "Pass@123");

        when(userAccountRepository.findByEmail(loginRequest.email())).thenReturn(Optional.of(pendingCustomer));
        when(passwordEncoder.matches("Pass@123", "encodedPass")).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.authenticateCredentials(loginRequest))
                .isInstanceOf(AccountNotReadyException.class)
                .hasMessageContaining("Your account is not confirmed");
    }

    @Test
    void confirmEmail_ShouldActivateUserAndRotateToken_WhenOtpIsValid() {
        ConfirmEmailRequest request = new ConfirmEmailRequest("ernest@quickbite.co.za", "123456");

        when(userAccountRepository.findByEmail(request.email())).thenReturn(Optional.of(pendingCustomer));
        when(tokenGenerator.mintAccessToken(pendingCustomer)).thenReturn("active-jwt");
        when(tokenRepository.findAllValidTokensByUser(1L)).thenReturn(List.of());

        AuthResponse response = authenticationService.confirmEmail(request);

        assertThat(pendingCustomer.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(pendingCustomer.getOtpCode()).isNull();
        assertThat(response.token()).isEqualTo("active-jwt");
        verify(userAccountRepository).save(pendingCustomer);
    }

    @Test
    void confirmEmail_ShouldThrowInvalidOtpException_WhenOtpIsIncorrect() {
        ConfirmEmailRequest request = new ConfirmEmailRequest("ernest@quickbite.co.za", "999999");

        when(userAccountRepository.findByEmail(request.email())).thenReturn(Optional.of(pendingCustomer));

        assertThatThrownBy(() -> authenticationService.confirmEmail(request))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessage("Invalid OTP code provided.");
    }
}
