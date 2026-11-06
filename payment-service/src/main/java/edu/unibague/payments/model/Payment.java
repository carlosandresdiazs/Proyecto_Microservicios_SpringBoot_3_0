package edu.unibague.payments.model;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="payments") public class Payment {
 @Id public UUID id;
 @Column(name="user_id",nullable=false,length=150) public String userId;
 @Column(name="order_id",nullable=false,length=100) public String orderId;
 @Column(nullable=false,precision=15,scale=2) public BigDecimal amount;
 @Column(nullable=false,length=3) public String currency;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) public PaymentStatus status;
 @Column(name="idempotency_key",nullable=false,length=120) public String idempotencyKey;
 @Column(name="provider_reference",length=140) public String providerReference;
 @Column(name="created_at",nullable=false) public Instant createdAt;
 @Column(name="updated_at",nullable=false) public Instant updatedAt;
 @PrePersist void init(){createdAt=Instant.now();updatedAt=createdAt;}
 @PreUpdate void touch(){updatedAt=Instant.now();}
}