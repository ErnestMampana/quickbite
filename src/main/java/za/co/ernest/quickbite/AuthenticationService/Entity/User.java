package za.co.ernest.quickbite.AuthenticationService.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import za.co.ernest.quickbite.AuthenticationService.Enum.AccountStatus;
import za.co.ernest.quickbite.AuthenticationService.Enum.UserRole;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String username;

    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!$*\\-.=?@_]).{8,10}$",
            message = "Password must be 8-10 characters long and include at least one lowercase letter, one uppercase letter, one number, and one special character (!$*-.=?@_)")
    private String password;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;
    private AccountStatus accountStatus;


    public static User createUser(String username, String password, String email, String firstName, String lastName, UserRole role) {

        User user = new User();
        user.username = username;
        user.password = password;
        user.email = email;
        user.firstName = firstName;
        user.lastName = lastName;
        user.role = role;
        user.accountStatus = AccountStatus.AWAITING_CONFIRMATION;


        return user;
    }


}
