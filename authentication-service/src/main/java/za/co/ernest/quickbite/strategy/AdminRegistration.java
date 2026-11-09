package za.co.ernest.quickbite.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import za.co.ernest.quickbite.Factory.RegistrationStrategy;
import za.co.ernest.quickbite.dto.UserDTO;
import za.co.ernest.quickbite.entity.User;
import za.co.ernest.quickbite.enums.UserRole;
import za.co.ernest.quickbite.repository.UserAccountRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminRegistration implements RegistrationStrategy {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(UserDTO dto) {
        validate(dto);
        log.info("Elevating platform administrative account clearance: [{}]", dto.email());

        User user = User.createUser(
                dto.username(),
                passwordEncoder.encode(dto.password()),
                dto.email(),
                dto.firstName(),
                dto.lastName(),
                getSupportedRole()
        );
        return userAccountRepository.save(user);
    }

    @Override
    public void validate(UserDTO dto) {
        if (userAccountRepository.findByEmail(dto.email()).isPresent()) {
            throw new IllegalArgumentException("Administrative system assignment denied: Record conflict.");
        }
    }

    @Override
    public UserRole getSupportedRole() {
        return UserRole.ADMIN;
    }
}
