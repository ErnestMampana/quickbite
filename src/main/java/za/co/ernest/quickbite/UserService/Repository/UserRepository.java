package za.co.ernest.quickbite.UserService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ernest.quickbite.UserService.Entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {
}
