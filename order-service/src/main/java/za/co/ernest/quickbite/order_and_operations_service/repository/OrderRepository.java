package za.co.ernest.quickbite.order_and_operations_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ernest.quickbite.order_and_operations_service.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
