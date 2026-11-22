package za.co.ernest.payment_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.ernest.payment_service.entity.Payment;
import za.co.ernest.payment_service.enums.PaymentStatus;

import java.util.Collection;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReference(String reference);
    Optional<Payment> findByOrderId(Long orderId);
    Optional<Payment> findFirstByOrderIdAndStatusIn(Long orderId, Collection<PaymentStatus> statuses);
}
