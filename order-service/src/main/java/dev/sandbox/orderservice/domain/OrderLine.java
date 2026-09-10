package dev.sandbox.orderservice.domain;

import java.math.BigDecimal;

/**
 * Value object - immutable, identity-less line item. Equal by value (record semantics), and cannot
 * exist in an invalid state - invariants are enforced in the compact constructor rather than
 * trusted from the caller.
 */
public record OrderLine(String sku, int quantity, BigDecimal unitPrice) {

  public OrderLine {
    if (sku == null || sku.isBlank()) {
      throw new IllegalArgumentException("sku must not be null or blank");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
    if (unitPrice == null || unitPrice.signum() <= 0) {
      throw new IllegalArgumentException("unitPrice must be positive");
    }
  }
}
