package edu.unibague.payments.dto;
import edu.unibague.payments.model.Payment;
import java.math.BigDecimal;
import java.util.UUID;
public record PaymentResponse(UUID id,String orderId,BigDecimal amount,String currency,String status){
 public static PaymentResponse of(Payment p){return new PaymentResponse(p.id,p.orderId,p.amount,p.currency,p.status.name());}
}