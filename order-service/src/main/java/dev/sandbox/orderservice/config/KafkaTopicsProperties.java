package dev.sandbox.orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed binding for app.kafka.topics.* - topic names order-service publishes to. Kept as its own
 * type (rather than @Value per property) so a new topic (e.g. M5's DLQ topic) is one more record
 * component, not a new injection point scattered across adapters.
 */
@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicsProperties(String orderEvents) {}
