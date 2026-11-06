package edu.unibague.payments.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record CreatePaymentRequest(@NotBlank @Size(max=100) String orderId,
 @NotNull @DecimalMin("0.01") @Digits(integer=13,fraction=2) BigDecimal amount,
 @NotBlank @Pattern(regexp="[A-Z]{3}") String currency){}