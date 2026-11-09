package za.co.ernest.quickbite.restaurant_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ernest.quickbite.restaurant_service.entity.Restaurant;

import java.util.List;


public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    List<Restaurant> findByIsOpenTrue();
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByAddressIgnoreCase(String address);
}
