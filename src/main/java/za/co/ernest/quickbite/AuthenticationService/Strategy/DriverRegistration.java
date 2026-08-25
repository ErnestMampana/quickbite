package za.co.ernest.quickbite.AuthenticationService.Strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import za.co.ernest.quickbite.AuthenticationService.DTO.UserDTO;
import za.co.ernest.quickbite.AuthenticationService.Entity.DriverDetails;
import za.co.ernest.quickbite.AuthenticationService.Entity.User;
import za.co.ernest.quickbite.AuthenticationService.Factory.RegistrationStrategy;
import za.co.ernest.quickbite.AuthenticationService.Repository.DriverDetailsRepository;
import za.co.ernest.quickbite.AuthenticationService.Repository.UserAccountRepository;
import za.co.ernest.quickbite.AuthenticationService.util.PasswordGenerator;

@Component
@RequiredArgsConstructor
public class DriverRegistration implements RegistrationStrategy {
    private final UserAccountRepository userRepository;
    private final DriverDetailsRepository driverDetailsRepository;
    @Override
    public void register(UserDTO dto) {

        String password = PasswordGenerator.generateSecurePassword(8);

        validate(dto);

        User user = User.createUser(dto.username(), password, dto.email(), dto.firstName(), dto.lastName(), dto.role());
        User savedUser = userRepository.save(user);

        DriverDetails details = new DriverDetails(savedUser, dto.licensePlate(), dto.vehicleType());
        driverDetailsRepository.save(details);
    }

    @Override
    public void validate(UserDTO dto) {
        if(driverDetailsRepository.findByLicensePlate(dto.licensePlate()).isPresent())
            throw new IllegalArgumentException("User already exists");
    }
}
