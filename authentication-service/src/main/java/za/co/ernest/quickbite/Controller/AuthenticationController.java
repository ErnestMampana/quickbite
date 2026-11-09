package za.co.ernest.quickbite.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.ernest.quickbite.dto.AuthResponse;
import za.co.ernest.quickbite.dto.LoginRequest;
import za.co.ernest.quickbite.dto.UserDTO;
import za.co.ernest.quickbite.dto.UserResponseDTO;
import za.co.ernest.quickbite.service.AuthenticationService;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Slf4j
public class AuthenticationController {

    private final AuthenticationService authenticationService;


    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> registerAccount(@Valid @RequestBody UserDTO registrationPayload) {
        log.info("Intercepted inbound REST request parameter mapping for user account registration.");
        UserResponseDTO profileResponse = authenticationService.createUserAccount(registrationPayload);
        return ResponseEntity.status(HttpStatus.CREATED).body(profileResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginAccount(@Valid @RequestBody LoginRequest loginPayload) {
        log.info("Intercepted inbound REST request context validation tracking for user login execution.");
        AuthResponse tokenPayload = authenticationService.authenticateCredentials(loginPayload);
        return ResponseEntity.ok(tokenPayload);
    }
//
//    @PostMapping("/login")
//    public ResponseEntity<UserResponseDTO> authenticate(@RequestBody CredentialsDTO request) {
//        return ResponseEntity.ok(authenticationService.authenticate(request));
//    }
//
//    @PostMapping("/confirm-email")
//    public ResponseEntity<UserResponseDTO> confirmEmail(@RequestBody ConfirmEmailRequest request) {
//        return ResponseEntity.ok(authenticationService.confirmEmail(request));
//    }
//
//    @PostMapping("/password-reset/request")
//    public ResponseEntity<String> requestPasswordReset(@RequestBody PasswordResetRequest request) {
//        return ResponseEntity.ok(authenticationService.requestPasswordReset(request.getEmail()));
//    }
//
//    @PutMapping("/reset-password")
//    public ResponseEntity<UserResponseDTO> resetPassword(@RequestBody ResetPasswordRequest request) {
//        return ResponseEntity.ok(authenticationService.resetPassword(request));
//    }
//
//    @PutMapping("/refresh-otp")
//    public ResponseEntity<String> refreshOtp(@RequestBody PasswordResetRequest request) {
//        return ResponseEntity.ok(authenticationService.refreshOtp(request.getEmail()));
//    }
}
