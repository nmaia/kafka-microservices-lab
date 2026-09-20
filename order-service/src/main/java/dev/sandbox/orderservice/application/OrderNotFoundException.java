package dev.sandbox.orderservice.application;

/**
 * Application exception - signals that no Order exists for a given id. Thrown from
 * OrderController's GET lookup (no dedicated query use case for a plain, unconditional repository
 * read); mapped to 404 by the REST exception advice.
 */
public class OrderNotFoundException extends RuntimeException {

  public OrderNotFoundException(String orderId) {
    super("No order found with id " + orderId);
  }
}
