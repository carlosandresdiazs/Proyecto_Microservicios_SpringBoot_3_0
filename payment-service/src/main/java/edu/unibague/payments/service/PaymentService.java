package edu.unibague.payments.service;
import edu.unibague.payments.dto.*;
import edu.unibague.payments.model.*;
import edu.unibague.payments.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
@Service public class PaymentService {
 private final PaymentRepository payments;
 private final WebhookEventRepository events;
 private final org.springframework.jdbc.core.JdbcTemplate jdbc;
 public PaymentService(PaymentRepository payments,WebhookEventRepository events,org.springframework.jdbc.core.JdbcTemplate jdbc){this.payments=payments;this.events=events;this.jdbc=jdbc;}
 @Transactional public PaymentResponse create(String user,String key,CreatePaymentRequest req){
  if(key==null||!key.matches("[a-zA-Z0-9._:-]{8,120}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Idempotency-Key inválida");
  var old=payments.findByUserIdAndIdempotencyKey(user,key);
  if(old.isPresent()) return sameOrConflict(old.get(),req);
  // UPSERT atómico; ante concurrencia prevalece la primera solicitud.
  jdbc.update("INSERT INTO payments(id,user_id,order_id,amount,currency,status,idempotency_key,created_at,updated_at) " +
   "VALUES (?,?,?,?,?,'PENDING',?,now(),now()) ON CONFLICT(user_id,idempotency_key) DO NOTHING",
   UUID.randomUUID(),user,req.orderId(),req.amount(),req.currency(),key);
  Payment stored=payments.findByUserIdAndIdempotencyKey(user,key)
   .orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Solicitud concurrente; reintentar"));
  return sameOrConflict(stored,req);
 }
 private PaymentResponse sameOrConflict(Payment p,CreatePaymentRequest r){
  if(!p.orderId.equals(r.orderId())||p.amount.compareTo(r.amount())!=0||!p.currency.equals(r.currency()))
   throw new ResponseStatusException(HttpStatus.CONFLICT,"Clave reutilizada con datos distintos");
  return PaymentResponse.of(p);
 }
 @Transactional(readOnly=true) public PaymentResponse find(UUID id,String user,boolean admin){
  Payment p=payments.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND));
  if(!admin&&!p.userId.equals(user))throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  return PaymentResponse.of(p);
 }
 @Transactional(readOnly=true) public List<PaymentResponse> mine(String user){return payments.findByUserIdOrderByCreatedAtDesc(user).stream().map(PaymentResponse::of).toList();}
 @Transactional public void receive(SandboxEvent evt){
  if(evt.eventId()==null||!evt.eventId().matches("[a-zA-Z0-9._:-]{8,140}")||
     evt.paymentId()==null||evt.providerReference()==null||evt.providerReference().isBlank()||evt.providerReference().length()>140)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Evento inválido");
  PaymentStatus next;
  try { next=PaymentStatus.valueOf(evt.status()); }
  catch(Exception ex){ throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Estado inválido"); }
  if(next==PaymentStatus.PENDING) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Estado final obligatorio");
  // Registrar id de evento primero. PostgreSQL garantiza un solo ganador, incluso entre replicas.
  int inserted=jdbc.update("INSERT INTO webhook_events(event_id,received_at) VALUES (?,now()) ON CONFLICT(event_id) DO NOTHING",evt.eventId());
  if(inserted==0) return;
  // Actualizar solamente PENDING, sin sobreescribir confirmaciones concurrentes.
  int updated=jdbc.update("UPDATE payments SET status=?, provider_reference=?, updated_at=now() " +
   "WHERE id=? AND status='PENDING'",next.name(),evt.providerReference(),evt.paymentId());
  if(updated==0){
   Payment p=payments.findById(evt.paymentId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Pago inexistente"));
   if(p.status!=next || !java.util.Objects.equals(p.providerReference,evt.providerReference()))
    throw new ResponseStatusException(HttpStatus.CONFLICT,"Transición incompatible");
  }
 }
}
