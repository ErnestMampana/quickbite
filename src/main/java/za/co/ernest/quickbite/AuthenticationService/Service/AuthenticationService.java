package za.co.ernest.quickbite.AuthenticationService.Service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import za.co.ernest.quickbite.AuthenticationService.DTO.UserDTO;
import za.co.ernest.quickbite.AuthenticationService.Factory.RegistrationFactory;
import za.co.ernest.quickbite.AuthenticationService.Factory.RegistrationStrategy;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final RegistrationFactory registrationFactory;

    public String createUser(UserDTO userDTO) {

        log.info("Creating user with role {}", userDTO.role());

        RegistrationStrategy strategy = registrationFactory.getStrategy(userDTO.role());
        strategy.validate(userDTO);
        strategy.register(userDTO);

        return "User created";
    }
}



