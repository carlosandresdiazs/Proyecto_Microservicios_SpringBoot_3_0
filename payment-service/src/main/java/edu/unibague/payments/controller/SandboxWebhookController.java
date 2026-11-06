package edu.unibague.payments.controller;
import tools.jackson.databind.json.JsonMapper;
import edu.unibague.payments.dto.SandboxEvent;
import edu.unibague.payments.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
@RestController @RequestMapping("/api/payments/webhooks/sandbox") public class SandboxWebhookController {
 private final PaymentService payments; private final JsonMapper mapper; private final byte[] secret;
 public SandboxWebhookController(PaymentService payments,JsonMapper mapper,@Value("${app.webhook.secret}") String secret){
  this.payments=payments;this.mapper=mapper;this.secret=secret.getBytes(StandardCharsets.UTF_8);
  if(this.secret.length<32) throw new IllegalStateException("WEBHOOK_SECRET debe tener al menos 32 bytes");
 }
 @PostMapping public ResponseEntity<Void> accept(@RequestBody byte[] raw,@RequestHeader(value="X-Sandbox-Signature",required=false) String signature){
  if(raw.length>8192 || signature==null || !signature.matches("[0-9a-fA-F]{64}")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  try {
   Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));
   if(!MessageDigest.isEqual(mac.doFinal(raw),HexFormat.of().parseHex(signature))) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
   SandboxEvent event=mapper.readValue(raw,SandboxEvent.class);
   payments.receive(event);return ResponseEntity.ok().build();
  }catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Evento JSON inválido");}
 }
}