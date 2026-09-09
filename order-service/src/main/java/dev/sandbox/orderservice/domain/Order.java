package dev.sandbox.orderservice.domain;

import com.github.f4b6a3.uuid.UuidCreator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root - the transaction/consistency boundary for order creation.
 * Owns its own invariants; {@link #create} is the only way to produce a
 * valid instance.
 */
public final class Order {

    private final String orderId;
    private final String customerId;
    private final List<OrderLine> items;
    private final BigDecimal totalAmount;
    private final String currency;
    private final Instant createdAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private Order(String orderId, String customerId, List<OrderLine> items,
                  BigDecimal totalAmount, String currency, Instant createdAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.items = items;
        this.totalAmount = totalAmount;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    public static Order create(String customerId, List<OrderLine> items, String currency) {
        validateCustomerId(customerId);
        validateItems(items);
        validateCurrency(currency);

        Order order = new Order(
                UuidCreator.getTimeOrderedEpoch().toString(),
                customerId,
                List.copyOf(items),
                calculateTotalAmount(items),
                currency,
                Instant.now()
        );

        order.domainEvents.add(new OrderCreatedEvent(
                order.orderId, order.customerId, order.items,
                order.totalAmount, order.currency, order.createdAt
        ));

        return order;
    }

    // O(n) over the line items - unavoidable, single pass. Each OrderLine already
    // guarantees quantity > 0 and unitPrice > 0 at construction, and validateItems
    // below guarantees the list is non-empty, so the sum here is always strictly
    // positive - no separate "totalAmount > 0" check is needed, it's a structural
    // consequence of construction rather than something to re-verify.
    private static BigDecimal calculateTotalAmount(List<OrderLine> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderLine item : items) {
            total = total.add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }
        return total;
    }

    private static void validateCustomerId(String customerId) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("customerId must not be null or blank");
        }
    }

    private static void validateItems(List<OrderLine> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("An order must have at least one item");
        }
    }

    private static void validateCurrency(String currency) {
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("currency must be a valid ISO 4217 code", e);
        }
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public String orderId() {
        return orderId;
    }

    public String customerId() {
        return customerId;
    }

    public List<OrderLine> items() {
        return items;
    }

    public BigDecimal totalAmount() {
        return totalAmount;
    }

    public String currency() {
        return currency;
    }

    public Instant createdAt() {
        return createdAt;
    }

    // Entity identity equality (DDD): two Orders are equal if they're the same
    // order (same orderId), regardless of any other field's value - not full
    // value equality like OrderLine.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order other)) return false;
        return orderId.equals(other.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(orderId);
    }
}