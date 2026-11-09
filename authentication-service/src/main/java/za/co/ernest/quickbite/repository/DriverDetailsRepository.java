package za.co.ernest.quickbite.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ernest.quickbite.entity.DriverDetails;

import java.util.Optional;

public interface DriverDetailsRepository extends JpaRepository<DriverDetails, Integer> {

    Optional<DriverDetails> findByLicensePlate(String email);
}
