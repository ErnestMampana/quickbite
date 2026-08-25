package za.co.ernest.quickbite.AuthenticationService.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ernest.quickbite.AuthenticationService.Entity.DriverDetails;

import java.util.Optional;

public interface DriverDetailsRepository extends JpaRepository<DriverDetails, Integer> {

    Optional<DriverDetails> findByLicensePlate(String email);
}
