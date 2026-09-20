package dev.sandbox.orderservice.adapters.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * DTO - inbound REST request body for POST /orders, decoupled from CreateOrderCommand so the API
 * contract and the use case's input can evolve independently. Anemic by design.
 */
public record CreateOrderRequest(
    @NotBlank String customerId,
    @NotEmpty @Valid List<OrderLineRequest> items,
    @NotBlank String currency) {}
