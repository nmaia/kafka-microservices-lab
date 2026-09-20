package dev.sandbox.orderservice.adapters.in.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO - outbound REST representation of a created/fetched Order, decoupled from the domain
 * aggregate so the API contract can evolve independently. Anemic by design.
 */
public record OrderResponse(
    String orderId,
    String customerId,
    List<OrderLineResponse> items,
    BigDecimal totalAmount,
    String currency,
    Instant createdAt) {}
