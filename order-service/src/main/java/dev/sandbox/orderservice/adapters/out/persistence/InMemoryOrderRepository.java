package dev.sandbox.orderservice.adapters.out.persistence;

import dev.sandbox.orderservice.domain.Order;
import dev.sandbox.orderservice.domain.OrderRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * Adapter - in-memory implementation of the OrderRepository port. Temporary stand-in for M2 only;
 * replaced by a real JpaOrderRepository in M3 with no change to the port itself.
 */
@Repository
public class InMemoryOrderRepository implements OrderRepository {

  private final Map<String, Order> orders = new ConcurrentHashMap<>();

  @Override
  public void save(Order order) {
    orders.put(order.orderId(), order);
  }

  @Override
  public Optional<Order> findById(String orderId) {
    return Optional.ofNullable(orders.get(orderId));
  }
}
