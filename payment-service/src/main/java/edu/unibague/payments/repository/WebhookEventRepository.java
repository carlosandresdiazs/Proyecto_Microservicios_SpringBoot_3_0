package edu.unibague.payments.repository;
import edu.unibague.payments.model.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
public interface WebhookEventRepository extends JpaRepository<WebhookEvent,String>{}