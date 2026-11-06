package edu.unibague.payments.controller;
import edu.unibague.payments.dto.*;
import edu.unibague.payments.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/payments") public class PaymentController {
 private final PaymentService service;
 public PaymentController(PaymentService service){this.service=service;}
 @PostMapping public ResponseEntity<PaymentResponse> create(@AuthenticationPrincipal Jwt jwt,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody CreatePaymentRequest req){
  return ResponseEntity.status(HttpStatus.CREATED).body(service.create(jwt.getSubject(),key,req));
 }
 @GetMapping public List<PaymentResponse> mine(@AuthenticationPrincipal Jwt jwt){return service.mine(jwt.getSubject());}
 @GetMapping("/{id}") public PaymentResponse get(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt){
  return service.find(id,jwt.getSubject(),isAdmin(jwt));
 }
 private boolean isAdmin(Jwt jwt) {
  Object realmAccess=jwt.getClaim("realm_access");
  if(realmAccess instanceof java.util.Map<?,?> map && map.get("roles") instanceof java.util.Collection<?> roles)
   return roles.contains("ADMIN");
  return false;
 }
}