package dev.sandbox.orderservice.adapters.in.rest;

import dev.sandbox.orderservice.adapters.in.rest.dto.CreateOrderRequest;
import dev.sandbox.orderservice.adapters.in.rest.dto.OrderLineRequest;
import dev.sandbox.orderservice.adapters.in.rest.dto.OrderLineResponse;
import dev.sandbox.orderservice.adapters.in.rest.dto.OrderResponse;
import dev.sandbox.orderservice.application.CreateOrderCommand;
import dev.sandbox.orderservice.domain.Order;
import dev.sandbox.orderservice.domain.OrderLine;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adapter mapper - translates between the REST boundary's DTOs and the application/domain shapes
 * (CreateOrderRequest -> CreateOrderCommand, Order -> OrderResponse). Manual rather than
 * MapStruct-generated, same reasoning as OrderEventMapper: only one mapping pair so far, not enough
 * shapes/directions yet to justify the annotation-processor setup.
 */
@Component
public class OrderRestMapper {

  public CreateOrderCommand toCommand(CreateOrderRequest request) {
    return new CreateOrderCommand(
        request.customerId(), toOrderLines(request.items()), request.currency());
  }

  public OrderResponse toResponse(Order order) {
    return new OrderResponse(
        order.orderId(),
        order.customerId(),
        toLineResponses(order.items()),
        order.totalAmount(),
        order.currency(),
        order.createdAt());
  }

  private List<OrderLine> toOrderLines(List<OrderLineRequest> items) {
    return items.stream()
        .map(item -> new OrderLine(item.sku(), item.quantity(), item.unitPrice()))
        .toList();
  }

  private List<OrderLineResponse> toLineResponses(List<OrderLine> items) {
    return items.stream()
        .map(item -> new OrderLineResponse(item.sku(), item.quantity(), item.unitPrice()))
        .toList();
  }
}
