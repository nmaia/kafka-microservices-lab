package dev.sandbox.orderservice.domain;

import java.util.Optional;

/**
 * Port - persistence boundary for Order, per Hexagonal architecture. Implemented by an adapter
 * (in-memory for M2, JPA from M3 onward) with zero change to this interface.
 */
public interface OrderRepository {

  void save(Order order);

  Optional<Order> findById(String orderId);
}
