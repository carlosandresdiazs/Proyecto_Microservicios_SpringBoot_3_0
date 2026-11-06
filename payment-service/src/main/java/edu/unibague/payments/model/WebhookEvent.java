package edu.unibague.payments.model;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="webhook_events") public class WebhookEvent {
 @Id @Column(name="event_id",length=140) public String eventId;
 @Column(name="received_at",nullable=false) public Instant receivedAt=Instant.now();
}