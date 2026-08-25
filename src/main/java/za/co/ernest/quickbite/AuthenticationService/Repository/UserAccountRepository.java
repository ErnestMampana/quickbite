package za.co.ernest.quickbite.AuthenticationService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ernest.quickbite.AuthenticationService.Entity.User;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);
}
