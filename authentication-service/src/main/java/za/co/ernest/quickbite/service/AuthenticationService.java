package za.co.ernest.quickbite.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.ernest.quickbite.dto.AuthResponse;
import za.co.ernest.quickbite.dto.LoginRequest;
import za.co.ernest.quickbite.dto.UserDTO;
import za.co.ernest.quickbite.dto.UserResponseDTO;
import za.co.ernest.quickbite.entity.User;
import za.co.ernest.quickbite.enums.UserRole;
import za.co.ernest.quickbite.Factory.RegistrationFactory;
import za.co.ernest.quickbite.Factory.RegistrationStrategy;
import za.co.ernest.quickbite.util.JwtTokenUtil;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final RegistrationFactory registrationFactory;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    @Transactional
    public UserResponseDTO createUserAccount(UserDTO userDTO) {
        log.info("Routing account sign-up sequence via factory engine for role: [{}]", userDTO.role());

        RegistrationStrategy strategy = registrationFactory.getStrategy(userDTO.role());
        User savedUser = strategy.register(userDTO);

        return new UserResponseDTO(
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getRole(),
                "Identity account registered successfully in system boundaries.",
                Instant.now().toString()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse authenticateCredentials(LoginRequest request) {
        log.info("Processing authentication request credentials for address: [{}]", request.email());

        User user = userAccountRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Authentication failed: Access credentials invalid."));

        if (!user.isActive()) {
            throw new IllegalStateException("Authentication failed: Target user profile status is suspended.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Authentication failed: Access credentials invalid.");
        }

        String jsonWebToken = jwtTokenUtil.mintAccessToken(user);
        log.info("Successful validation execution context. Token issued for key index: [{}]", user.getId());

        return new AuthResponse(jsonWebToken, "Bearer", user.getEmail(), user.getRole().name());
    }
}



