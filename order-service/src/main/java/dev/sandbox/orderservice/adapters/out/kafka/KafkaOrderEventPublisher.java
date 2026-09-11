package dev.sandbox.orderservice.adapters.out.kafka;

import dev.sandbox.avro.OrderCreated;
import dev.sandbox.orderservice.config.KafkaTopicsProperties;
import dev.sandbox.orderservice.domain.DomainEvent;
import dev.sandbox.orderservice.domain.DomainEventPublisher;
import dev.sandbox.orderservice.domain.OrderCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Adapter - Kafka implementation of the DomainEventPublisher port. Dispatches on the concrete
 * DomainEvent type via a pattern-matching switch; DomainEvent being sealed means the compiler
 * rejects this class if a new event type is added without a matching case here - no default/throw
 * branch needed.
 */
@Component
public class KafkaOrderEventPublisher implements DomainEventPublisher {

  private final KafkaTemplate<String, OrderCreated> kafkaTemplate;
  private final OrderEventMapper orderEventMapper;
  private final KafkaTopicsProperties kafkaTopicsProperties;

  public KafkaOrderEventPublisher(
      KafkaTemplate<String, OrderCreated> kafkaTemplate,
      OrderEventMapper orderEventMapper,
      KafkaTopicsProperties kafkaTopicsProperties) {
    this.kafkaTemplate = kafkaTemplate;
    this.orderEventMapper = orderEventMapper;
    this.kafkaTopicsProperties = kafkaTopicsProperties;
  }

  @Override
  public void publish(DomainEvent event) {
    switch (event) {
      case OrderCreatedEvent e -> sendOrderCreated(e);
    }
  }

  private void sendOrderCreated(OrderCreatedEvent event) {
    OrderCreated avroEvent = orderEventMapper.toAvro(event);
    kafkaTemplate.send(kafkaTopicsProperties.orderEvents(), event.orderId(), avroEvent);
  }
}
