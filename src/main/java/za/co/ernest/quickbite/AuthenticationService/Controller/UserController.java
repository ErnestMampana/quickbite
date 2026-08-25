package za.co.ernest.quickbite.AuthenticationService.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.ernest.quickbite.AuthenticationService.Service.UserService;
import za.co.ernest.quickbite.AuthenticationService.DTO.UserDTO;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/create")
    public ResponseEntity<String> getUser(@Valid @RequestBody UserDTO userDTO) {
        return new ResponseEntity<>(userService.createUser(userDTO),HttpStatus. CREATED);
    }
}
