package za.co.ernest.quickbite.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import za.co.ernest.quickbite.dto.*;
import za.co.ernest.quickbite.entity.User;
import za.co.ernest.quickbite.enums.AccountStatus;
import za.co.ernest.quickbite.enums.UserRole;
import za.co.ernest.quickbite.kafka.port.NotificationPublisher;
import za.co.ernest.quickbite.repository.TokenRepository;
import za.co.ernest.quickbite.repository.UserAccountRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private TokenRepository tokenRepository;

    @MockitoBean
    private NotificationPublisher notificationPublisher;

    @BeforeEach
    void cleanDatabase() {
        tokenRepository.deleteAll();
        userAccountRepository.deleteAll();
    }

    @Test
    void fullRegistrationConfirmationAndLoginFlow_ShouldSucceedEndToEnd() throws Exception {
        UserDTO registerRequest = new UserDTO(
                "ernestm", "Pass@123", "ernest@quickbite.co.za", "Ernest", "Mohlala", UserRole.CUSTOMER
        );

        // 1. Register -> 201 Created
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("ernest@quickbite.co.za"))
                .andExpect(jsonPath("$.status").value("AWAITING_CONFIRMATION"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        verify(notificationPublisher).sendOtpEvent(any());

        // 2. Login before confirmation -> 403 Forbidden (AccountNotReadyException)
        LoginRequest loginRequest = new LoginRequest("ernest@quickbite.co.za", "Pass@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Login blocked: Your account is not confirmed."));

        // 3. Confirm email using persisted OTP -> 200 OK
        User savedUser = userAccountRepository.findByEmail("ernest@quickbite.co.za").orElseThrow();
        ConfirmEmailRequest confirmRequest = new ConfirmEmailRequest("ernest@quickbite.co.za", savedUser.getOtpCode());

        mockMvc.perform(post("/api/v1/auth/confirm-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        // 4. Login after confirmation -> 200 OK
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void passwordResetAndRefreshOtpFlow_ShouldUpdatePasswordAndAllowLogin() throws Exception {
        // Seed and confirm a customer
        UserDTO registerRequest = new UserDTO(
                "ernestm", "Pass@123", "ernest@quickbite.co.za", "Ernest", "Mohlala", UserRole.CUSTOMER
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        User user = userAccountRepository.findByEmail("ernest@quickbite.co.za").orElseThrow();
        mockMvc.perform(post("/api/v1/auth/confirm-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ConfirmEmailRequest("ernest@quickbite.co.za", user.getOtpCode()))));

        // 1. Request password reset -> POST /api/v1/auth/password-reset/request
        PasswordResetRequest resetTrigger = new PasswordResetRequest("ernest@quickbite.co.za");
        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetTrigger)))
                .andExpect(status().isOk())
                .andExpect(content().string("Password reset OTP dispatched. Token expires in 5 minutes."));

        // 2. Refresh OTP -> PUT /api/v1/auth/refresh-otp
        mockMvc.perform(put("/api/v1/auth/refresh-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetTrigger)))
                .andExpect(status().isOk())
                .andExpect(content().string("New OTP generated and dispatched. Valid for 5 minutes."));

        // 3. Complete password reset with refreshed OTP -> PUT /api/v1/auth/reset-password
        User userWithRefreshedOtp = userAccountRepository.findByEmail("ernest@quickbite.co.za").orElseThrow();
        ResetPasswordRequest resetPayload = new ResetPasswordRequest(
                "ernest@quickbite.co.za", userWithRefreshedOtp.getOtpCode(), "NewPass@9"
        );

        mockMvc.perform(put("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        // 4. Verify login works with new password
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ernest@quickbite.co.za", "NewPass@9"))))
                .andExpect(status().isOk());

        verify(notificationPublisher, times(3)).sendOtpEvent(any());
        assertThat(userAccountRepository.findByEmail("ernest@quickbite.co.za").orElseThrow().getStatus())
                .isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should confirm email and return 400 when otp is invalid.")
    void confirmEmail_ShouldReturn400BadRequest_WhenOtpIsInvalid() throws Exception {
        UserDTO registerRequest = new UserDTO(
                "ernestm", "Pass@123", "ernest@quickbite.co.za", "Ernest", "Mohlala", UserRole.CUSTOMER
        );
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        ConfirmEmailRequest wrongOtp = new ConfirmEmailRequest("ernest@quickbite.co.za", "000000");

        mockMvc.perform(post("/api/v1/auth/confirm-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongOtp)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid OTP code provided."));
    }

    @Test
    void register_ShouldReturn409Conflict_WhenEmailAlreadyExists() throws Exception {
        UserDTO request = new UserDTO(
                "ernestm", "Pass@123", "duplicate@quickbite.co.za", "Ernest", "Mohlala", UserRole.CUSTOMER
        );

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}

