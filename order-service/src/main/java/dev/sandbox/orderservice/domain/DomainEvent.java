package dev.sandbox.orderservice.domain;

/**
 * Marker interface for domain events raised by aggregates in this bounded
 * context, accumulated during a command and retrieved via
 * {@code pullDomainEvents()} once the command completes.
 */
public interface DomainEvent {}
