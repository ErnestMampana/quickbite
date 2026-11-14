package za.co.ernest.quickbite.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import za.co.ernest.quickbite.dto.*;
import za.co.ernest.quickbite.service.AuthenticationService;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;


    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerAccount(@Valid @RequestBody UserDTO registrationPayload) {
        log.info("Intercepted inbound REST request parameter mapping for user account registration.");
        AuthResponse profileResponse = authenticationService.createUserAccount(registrationPayload);
        return ResponseEntity.status(HttpStatus.CREATED).body(profileResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginAccount(@Valid @RequestBody LoginRequest loginPayload) {
        log.info("Intercepted inbound REST request context validation tracking for user login execution.");
        AuthResponse tokenPayload = authenticationService.authenticateCredentials(loginPayload);
        return ResponseEntity.ok(tokenPayload);
    }
    @PostMapping("/confirm-email")
    public ResponseEntity<AuthResponse> confirmEmail(@Valid @RequestBody ConfirmEmailRequest request) {
        return ResponseEntity.ok(authenticationService.confirmEmail(request));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<String> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(authenticationService.requestPasswordReset(request.email()));
    }

    @PutMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authenticationService.resetPassword(request));
    }

    @PutMapping("/refresh-otp")
    public ResponseEntity<String> refreshOtp(@Valid @RequestBody PasswordResetRequest request) {
        return ResponseEntity.ok(authenticationService.refreshOtp(request.email()));
    }
}
