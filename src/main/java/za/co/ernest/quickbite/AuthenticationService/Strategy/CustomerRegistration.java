package za.co.ernest.quickbite.AuthenticationService.Strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.ernest.quickbite.AuthenticationService.DTO.UserDTO;
import za.co.ernest.quickbite.AuthenticationService.Entity.User;
import za.co.ernest.quickbite.AuthenticationService.Factory.RegistrationStrategy;
import za.co.ernest.quickbite.AuthenticationService.Repository.UserAccountRepository;

@Component
@RequiredArgsConstructor
public class CustomerRegistration implements RegistrationStrategy {
    private final UserAccountRepository userAccountRepository;

    @Override
    public void register(UserDTO dto) {

        validate(dto);

        User user = User.createUser(dto.username(), dto.password(), dto.email(), dto.firstName(), dto.lastName(), dto.role());
        userAccountRepository.save(user);
        //TODO: Send welcome message using kafka notifications
    }

    @Override
    public void validate(UserDTO dto) {
        if(userAccountRepository.findByEmail(dto.email()).isPresent())
            throw new IllegalArgumentException("User already exists");
    }
}
