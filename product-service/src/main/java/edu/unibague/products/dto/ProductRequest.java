package edu.unibague.products.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductRequest(
 @NotBlank @Size(max=120) String name,
 @NotBlank @Size(max=500) String description,
 @NotNull @DecimalMin("0.00") BigDecimal price,
 @NotNull @Min(0) Integer stock
) {}
