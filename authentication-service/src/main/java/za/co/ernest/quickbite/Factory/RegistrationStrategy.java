package za.co.ernest.quickbite.Factory;

import za.co.ernest.quickbite.dto.UserDTO;
import za.co.ernest.quickbite.entity.User;
import za.co.ernest.quickbite.enums.UserRole;

public interface RegistrationStrategy {
    User register(UserDTO dto);
    void validate(UserDTO email);

    UserRole getSupportedRole();
}
