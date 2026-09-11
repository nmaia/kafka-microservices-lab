package dev.sandbox.orderservice.application;

import dev.sandbox.orderservice.domain.OrderLine;
import java.util.List;

/**
 * Input for {@link CreateOrderUseCase} - decouples the use case's contract from both
 * OrderController's REST request shape and Order.create()'s own signature, so each can evolve
 * independently.
 */
public record CreateOrderCommand(String customerId, List<OrderLine> items, String currency) {}
