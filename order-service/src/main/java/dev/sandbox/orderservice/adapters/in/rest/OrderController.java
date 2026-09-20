package dev.sandbox.orderservice.adapters.in.rest;

import dev.sandbox.orderservice.adapters.in.rest.dto.CreateOrderRequest;
import dev.sandbox.orderservice.adapters.in.rest.dto.OrderResponse;
import dev.sandbox.orderservice.application.CreateOrderUseCase;
import dev.sandbox.orderservice.application.OrderNotFoundException;
import dev.sandbox.orderservice.domain.Order;
import dev.sandbox.orderservice.domain.OrderRepository;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Adapter - inbound REST entry point for order creation and lookup. POST delegates to
 * CreateOrderUseCase; GET reads directly from the OrderRepository port, since a plain,
 * unconditional lookup needs no use-case orchestration of its own.
 */
@RestController
@RequestMapping("/orders")
public class OrderController {

  private final CreateOrderUseCase createOrderUseCase;
  private final OrderRepository orderRepository;
  private final OrderRestMapper orderRestMapper;

  public OrderController(
      CreateOrderUseCase createOrderUseCase,
      OrderRepository orderRepository,
      OrderRestMapper orderRestMapper) {
    this.createOrderUseCase = createOrderUseCase;
    this.orderRepository = orderRepository;
    this.orderRestMapper = orderRestMapper;
  }

  @PostMapping
  public ResponseEntity<OrderResponse> createOrder(
      @Valid @RequestBody CreateOrderRequest request, UriComponentsBuilder uriBuilder) {
    Order order = createOrderUseCase.execute(orderRestMapper.toCommand(request));

    URI location = uriBuilder.path("/orders/{orderId}").buildAndExpand(order.orderId()).toUri();

    return ResponseEntity.created(location).body(orderRestMapper.toResponse(order));
  }

  @GetMapping("/{orderId}")
  public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
    Order order =
        orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

    return ResponseEntity.ok(orderRestMapper.toResponse(order));
  }
}
