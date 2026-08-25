package za.co.ernest.quickbite.AuthenticationService.Factory;

import za.co.ernest.quickbite.AuthenticationService.DTO.UserDTO;

public interface RegistrationStrategy {
    void register(UserDTO dto);
    void validate(UserDTO email);
}
