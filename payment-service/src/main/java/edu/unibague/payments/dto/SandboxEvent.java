package edu.unibague.payments.dto;
import java.util.UUID;
public record SandboxEvent(String eventId,UUID paymentId,String status,String providerReference){}