package za.co.ernest.quickbite.resturantService.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ernest.quickbite.resturantService.entity.Restaurant;


public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

}
