package dev.sandbox.orderservice.adapters.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * DTO - inbound REST representation of a single order line, decoupled from the domain OrderLine.
 * Anemic by design (adapter-boundary DTO); validated structurally here, business invariants
 * re-checked by OrderLine's own compact constructor regardless.
 */
public record OrderLineRequest(
    @NotBlank String sku, @Positive int quantity, @NotNull @Positive BigDecimal unitPrice) {}
