package za.co.ernest.quickbite.UserService.Service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.ernest.quickbite.UserService.Entity.User;
import za.co.ernest.quickbite.UserService.Repository.UserRepository;
import za.co.ernest.quickbite.UserService.DTO.UserDTO;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public String createUser(UserDTO userDTO) {
        User user = User.createUser(userDTO.username(), userDTO.password(),
                userDTO.email(), userDTO.firstName(), userDTO.lastName());
        userRepository.save(user);

        return "User created";
    }


}
