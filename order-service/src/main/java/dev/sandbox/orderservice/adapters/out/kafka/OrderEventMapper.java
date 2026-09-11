package dev.sandbox.orderservice.adapters.out.kafka;

import dev.sandbox.avro.OrderCreated;
import dev.sandbox.avro.OrderItem;
import dev.sandbox.orderservice.domain.OrderCreatedEvent;
import dev.sandbox.orderservice.domain.OrderLine;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adapter mapper - translates the domain OrderCreatedEvent into the Avro OrderCreated wire event.
 * Manual rather than MapStruct-generated for now: Avro's generated builder pattern doesn't map
 * cleanly onto MapStruct's constructor/setter inference, and there's only one mapping so far to
 * justify the annotation-processor setup - revisit once a second exists.
 */
@Component
public class OrderEventMapper {

  public OrderCreated toAvro(OrderCreatedEvent event) {
    return OrderCreated.newBuilder()
        .setOrderId(event.orderId())
        .setCustomerId(event.customerId())
        .setTotalAmount(event.totalAmount())
        .setCurrency(event.currency())
        .setItems(toAvroItems(event.items()))
        .setCreatedAt(event.createdAt())
        .build();
  }

  private List<OrderItem> toAvroItems(List<OrderLine> items) {
    return items.stream()
        .map(
            line ->
                OrderItem.newBuilder()
                    .setSku(line.sku())
                    .setQuantity(line.quantity())
                    .setUnitPrice(line.unitPrice())
                    .build())
        .toList();
  }
}
