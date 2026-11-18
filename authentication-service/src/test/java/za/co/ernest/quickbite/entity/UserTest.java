package za.co.ernest.quickbite.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import za.co.ernest.quickbite.Exception.AccountNotReadyException;
import za.co.ernest.quickbite.Exception.AccountStateConflictException;
import za.co.ernest.quickbite.Exception.ExpiredOtpException;
import za.co.ernest.quickbite.Exception.InvalidOtpException;
import za.co.ernest.quickbite.enums.AccountStatus;
import za.co.ernest.quickbite.enums.UserRole;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserTest {

    private User pendingCustomer;
    private User activeCustomer;

    @BeforeEach
    void setUp() {
        pendingCustomer = User.createPendingUser(
                "ernestm", "hashedPass", "ernest@quickbite.co.za", "Ernest", "Mohlala", UserRole.CUSTOMER
        );
        pendingCustomer.assignOtp("123456", 5);

        activeCustomer = User.builder()
                .username("activeuser")
                .password("hashedPass")
                .email("active@quickbite.co.za")
                .firstName("Active")
                .lastName("User")
                .role(UserRole.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void createPendingUser_ShouldSetAwaitingConfirmation_ForNonAdminRoles() {
        assertThat(pendingCustomer.getStatus()).isEqualTo(AccountStatus.AWAITING_CONFIRMATION);
    }

    @Test
    void createPendingUser_ShouldSetActiveImmediately_ForAdminRole() {
        User admin = User.createPendingUser(
                "admin1", "hashedPass", "admin@quickbite.co.za", "Admin", "User", UserRole.ADMIN
        );
        assertThat(admin.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    void confirmEmail_ShouldActivateAccountAndClearOtp_WhenOtpIsValid() {
        pendingCustomer.confirmEmail("123456");

        assertThat(pendingCustomer.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(pendingCustomer.getOtpCode()).isNull();
        assertThat(pendingCustomer.getOtpExpiresAt()).isNull();
    }

    @Test
    void confirmEmail_ShouldThrowAccountStateConflictException_WhenAlreadyActive() {
        assertThatThrownBy(() -> activeCustomer.confirmEmail("123456"))
                .isInstanceOf(AccountStateConflictException.class)
                .hasMessage("Account is already confirmed and active.");
    }

    @Test
    void confirmEmail_ShouldThrowInvalidOtpException_WhenOtpDoesNotMatch() {
        assertThatThrownBy(() -> pendingCustomer.confirmEmail("999999"))
                .isInstanceOf(InvalidOtpException.class)
                .hasMessage("Invalid OTP code provided.");
    }

    @Test
    void confirmEmail_ShouldThrowExpiredOtpException_WhenOtpIsExpired() {
        pendingCustomer.setOtpExpiresAt(LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> pendingCustomer.confirmEmail("123456"))
                .isInstanceOf(ExpiredOtpException.class)
                .hasMessage("OTP has expired. Please request a new token.");
    }

    @Test
    void requestPasswordReset_ShouldSetAwaitingPwdResetAndAssignOtp() {
        activeCustomer.requestPasswordReset("654321", 5);

        assertThat(activeCustomer.getStatus()).isEqualTo(AccountStatus.AWAITING_PWD_RESET);
        assertThat(activeCustomer.getOtpCode()).isEqualTo("654321");
        assertThat(activeCustomer.getOtpExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void resetPassword_ShouldUpdatePasswordActivateAccountAndClearOtp_WhenValid() {
        activeCustomer.requestPasswordReset("654321", 5);

        activeCustomer.resetPassword("654321", "newEncodedPass");

        assertThat(activeCustomer.getPassword()).isEqualTo("newEncodedPass");
        assertThat(activeCustomer.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(activeCustomer.getOtpCode()).isNull();
        assertThat(activeCustomer.getOtpExpiresAt()).isNull();
    }

    @Test
    void resetPassword_ShouldThrowAccountStateConflictException_WhenNoResetWasRequested() {
        assertThatThrownBy(() -> activeCustomer.resetPassword("654321", "newEncodedPass"))
                .isInstanceOf(AccountStateConflictException.class)
                .hasMessage("No active password reset request found for this account.");
    }

    @Test
    void refreshOtp_ShouldUpdateOtp_WhenAccountIsPending() {
        pendingCustomer.refreshOtp("111222", 5);

        assertThat(pendingCustomer.getOtpCode()).isEqualTo("111222");
        assertThat(pendingCustomer.getOtpExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void refreshOtp_ShouldThrowAccountStateConflictException_WhenAccountIsActive() {
        assertThatThrownBy(() -> activeCustomer.refreshOtp("111222", 5))
                .isInstanceOf(AccountStateConflictException.class)
                .hasMessage("Active accounts do not require an OTP refresh.");
    }

    @Test
    void ensureIsHealthy_ShouldPass_WhenAccountIsActive() {
        assertThatCode(() -> activeCustomer.ensureIsHealthy()).doesNotThrowAnyException();
    }

    @Test
    void ensureIsHealthy_ShouldThrowAccountNotReadyException_WhenAwaitingConfirmation() {
        assertThatThrownBy(() -> pendingCustomer.ensureIsHealthy())
                .isInstanceOf(AccountNotReadyException.class)
                .hasMessage("Login blocked: Your account is not confirmed.");
    }

    @Test
    void ensureIsHealthy_ShouldThrowAccountNotReadyException_WhenAwaitingPasswordReset() {
        activeCustomer.requestPasswordReset("654321", 5);

        assertThatThrownBy(() -> activeCustomer.ensureIsHealthy())
                .isInstanceOf(AccountNotReadyException.class)
                .hasMessage("Login blocked: Password reset is pending. Complete the reset flow first.");
    }
}
