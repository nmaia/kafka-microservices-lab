package dev.sandbox.orderservice.application;

import dev.sandbox.orderservice.domain.DomainEvent;
import dev.sandbox.orderservice.domain.DomainEventPublisher;
import dev.sandbox.orderservice.domain.Order;
import dev.sandbox.orderservice.domain.OrderRepository;
import org.springframework.stereotype.Service;

/**
 * Use case - orchestrates domain + ports for order creation. Depends only on port interfaces
 * (OrderRepository, DomainEventPublisher), never on their concrete adapters, so OrderController
 * stays decoupled from persistence/Kafka entirely.
 */
@Service
public class CreateOrderUseCase {

  private final OrderRepository orderRepository;
  private final DomainEventPublisher domainEventPublisher;

  public CreateOrderUseCase(
      OrderRepository orderRepository, DomainEventPublisher domainEventPublisher) {
    this.orderRepository = orderRepository;
    this.domainEventPublisher = domainEventPublisher;
  }

  public Order execute(CreateOrderCommand command) {
    Order order = Order.create(command.customerId(), command.items(), command.currency());

    orderRepository.save(order);

    for (DomainEvent event : order.pullDomainEvents()) {
      domainEventPublisher.publish(event);
    }

    return order;
  }
}
