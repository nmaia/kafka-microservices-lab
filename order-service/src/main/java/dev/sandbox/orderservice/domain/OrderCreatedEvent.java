package dev.sandbox.orderservice.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Domain event - records that an Order was created. Distinct from the
 * Avro-serialized OrderCreated wire event; mapping between the two happens
 * at the adapter boundary, per the Hexagonal layering.
 */
public record OrderCreatedEvent(
    String orderId,
    String customerId,
    List<OrderLine> items,
    BigDecimal totalAmount,
    String currency,
    Instant createdAt)
    implements DomainEvent {}
