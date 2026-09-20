package dev.sandbox.orderservice.adapters.in.rest.dto;

import java.math.BigDecimal;

/**
 * DTO - outbound REST representation of a single order line, decoupled from the domain OrderLine so
 * the API contract can evolve independently. Anemic by design.
 */
public record OrderLineResponse(String sku, int quantity, BigDecimal unitPrice) {}
