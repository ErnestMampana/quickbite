package za.co.ernest.quickbite.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ernest.quickbite.entity.User;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);
}
