package dev.sandbox.orderservice.domain;

/**
 * Marker interface for domain events raised by aggregates in this bounded context, accumulated
 * during a command and retrieved via {@code pullDomainEvents()} once the command completes. Sealed
 * so KafkaOrderEventPublisher's dispatch switch is verified exhaustive by the compiler as new event
 * types are added.
 */
public sealed interface DomainEvent permits OrderCreatedEvent {}
