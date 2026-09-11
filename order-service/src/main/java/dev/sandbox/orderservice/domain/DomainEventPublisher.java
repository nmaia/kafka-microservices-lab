package dev.sandbox.orderservice.domain;

/**
 * Port - outbound boundary for publishing domain events raised by aggregates in this bounded
 * context. Implemented by adapters/out/kafka/KafkaOrderEventPublisher; kept generic on DomainEvent
 * so future event types (e.g. M3's OrderConfirmedEvent) reuse it without a new port method.
 */
public interface DomainEventPublisher {

  void publish(DomainEvent event);
}
