package za.co.ernest.quickbite.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import za.co.ernest.quickbite.enums.AccountStatus;
import za.co.ernest.quickbite.enums.UserRole;

import java.time.LocalDateTime;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, name = "password")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!$*\\-.=?@_]).{8,10}$")
    private String password;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, name = "first_name", length = 50)
    private String firstName;

    @Column(nullable = false, name = "last_name", length = 50)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AccountStatus status = AccountStatus.AWAITING_CONFIRMATION;

    @Column(name = "otp_code", length = 6)
    private String otpCode;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    public static User createPendingUser(String username, String passwordHash, String email, String firstName, String lastName, UserRole role) {
        AccountStatus initialStatus = (role == UserRole.ADMIN)
                ? AccountStatus.ACTIVE
                : AccountStatus.AWAITING_CONFIRMATION;

        return User.builder()
                .username(username)
                .password(passwordHash)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .status(initialStatus)
                .build();
    }

    public void assignOtp(String otp, int expiryMinutes) {
        this.otpCode = otp;
        this.otpExpiresAt = LocalDateTime.now().plusMinutes(expiryMinutes);
    }

    public void confirmEmail(String inputOtp) {
        if (this.status == AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is already confirmed and active.");
        }
        validateOtp(inputOtp);
        this.status = AccountStatus.ACTIVE;
        clearOtp();
    }

    public void requestPasswordReset(String otp, int expiryMinutes) {
        this.status = AccountStatus.AWAITING_PWD_RESET;
        assignOtp(otp, expiryMinutes);
    }

    public void resetPassword(String inputOtp, String newEncodedPassword) {
        if (this.status != AccountStatus.AWAITING_PWD_RESET) {
            throw new IllegalStateException("No active password reset request found for this account.");
        }
        validateOtp(inputOtp);
        this.password = newEncodedPassword;
        this.status = AccountStatus.ACTIVE;
        clearOtp();
    }

    public void refreshOtp(String newOtp, int expiryMinutes) {
        if (this.status == AccountStatus.ACTIVE) {
            throw new IllegalStateException("Active accounts do not require an OTP refresh.");
        }
        assignOtp(newOtp, expiryMinutes);
    }

    public void ensureIsHealthy() {
        if (this.status == AccountStatus.AWAITING_PWD_RESET) {
            throw new IllegalStateException("Login blocked: Password reset is pending. Complete the reset flow first.");
        }
    }

    private void validateOtp(String inputOtp) {
        if (this.otpCode == null || !this.otpCode.equals(inputOtp)) {
            throw new IllegalArgumentException("Invalid OTP code provided.");
        }
        if (this.otpExpiresAt == null || LocalDateTime.now().isAfter(this.otpExpiresAt)) {
            throw new IllegalArgumentException("OTP has expired. Please request a new token.");
        }
    }

    private void clearOtp() {
        this.otpCode = null;
        this.otpExpiresAt = null;
    }

}
