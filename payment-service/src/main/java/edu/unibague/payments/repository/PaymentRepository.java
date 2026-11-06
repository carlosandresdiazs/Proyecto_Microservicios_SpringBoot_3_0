package edu.unibague.payments.repository;
import edu.unibague.payments.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface PaymentRepository extends JpaRepository<Payment,UUID> {
 Optional<Payment> findByUserIdAndIdempotencyKey(String userId,String key);
 List<Payment> findByUserIdOrderByCreatedAtDesc(String userId);
}