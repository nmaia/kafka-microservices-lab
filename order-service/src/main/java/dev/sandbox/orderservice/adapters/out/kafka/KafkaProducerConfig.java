package dev.sandbox.orderservice.adapters.out.kafka;

import dev.sandbox.avro.OrderCreated;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Adapter configuration - builds a KafkaTemplate typed specifically to the OrderCreated Avro value
 * (String key), instead of relying on Spring Boot's untyped KafkaTemplate<Object, Object>
 * autoconfiguration, so the compiler enforces the correct value type at every injection site.
 */
@Configuration
public class KafkaProducerConfig {

  @Bean
  public ProducerFactory<String, OrderCreated> orderCreatedProducerFactory(
      KafkaProperties kafkaProperties) {
    // buildProducerProperties() is deprecated in 3.3.4; the SslBundles-taking
    // overload is the supported path. null is the documented no-op here since
    // this sandbox's Kafka listener is PLAINTEXT - no SSL bundle to supply.
    return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties(null));
  }

  @Bean
  public KafkaTemplate<String, OrderCreated> orderCreatedKafkaTemplate(
      ProducerFactory<String, OrderCreated> orderCreatedProducerFactory) {
    return new KafkaTemplate<>(orderCreatedProducerFactory);
  }
}
