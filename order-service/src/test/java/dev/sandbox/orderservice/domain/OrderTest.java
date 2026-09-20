package dev.sandbox.orderservice.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class OrderTest {

  @Nested
  @DisplayName("create()")
  class Create {

    @Test
    @DisplayName("builds an Order with the given customerId, items, and currency")
    void createsOrderWithGivenFields() {
      OrderLine line = new OrderLine("SKU-1", 2, new BigDecimal("19.99"));

      Order order = Order.create("cust-123", List.of(line), "USD");

      assertThat(order.customerId()).isEqualTo("cust-123");
      assertThat(order.items()).containsExactly(line);
      assertThat(order.currency()).isEqualTo("USD");
      assertThat(order.orderId()).isNotBlank();
      assertThat(order.createdAt()).isNotNull();
    }

    @Test
    @DisplayName("calculates totalAmount as the sum of quantity * unitPrice across all items")
    void calculatesTotalAmountAcrossItems() {
      OrderLine line1 = new OrderLine("SKU-1", 2, new BigDecimal("19.99"));
      OrderLine line2 = new OrderLine("SKU-2", 1, new BigDecimal("5.00"));

      Order order = Order.create("cust-123", List.of(line1, line2), "USD");

      assertThat(order.totalAmount()).isEqualByComparingTo("44.98");
    }

    @Test
    @DisplayName("raises a matching OrderCreatedEvent")
    void raisesOrderCreatedEvent() {
      OrderLine line = new OrderLine("SKU-1", 2, new BigDecimal("19.99"));

      Order order = Order.create("cust-123", List.of(line), "USD");
      List<DomainEvent> events = order.pullDomainEvents();

      assertThat(events).hasSize(1);
      assertThat(events.getFirst())
          .isInstanceOfSatisfying(
              OrderCreatedEvent.class,
              event -> {
                assertThat(event.orderId()).isEqualTo(order.orderId());
                assertThat(event.customerId()).isEqualTo("cust-123");
                assertThat(event.totalAmount()).isEqualByComparingTo(order.totalAmount());
              });
    }
  }

  @Nested
  @DisplayName("create() validation")
  class Validation {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("rejects a null or blank customerId")
    void rejectsBlankCustomerId(String customerId) {
      OrderLine line = new OrderLine("SKU-1", 1, new BigDecimal("10.00"));

      assertThatIllegalArgumentException()
          .isThrownBy(() -> Order.create(customerId, List.of(line), "USD"))
          .withMessage("customerId must not be null or blank");
    }

    @Test
    @DisplayName("rejects a null items list")
    void rejectsNullItems() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> Order.create("cust-123", null, "USD"))
          .withMessage("An order must have at least one item");
    }

    @Test
    @DisplayName("rejects an empty items list")
    void rejectsEmptyItems() {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> Order.create("cust-123", List.of(), "USD"))
          .withMessage("An order must have at least one item");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "XX", "INVALID"})
    @DisplayName("rejects a null or malformed currency code")
    void rejectsInvalidCurrency(String currency) {
      OrderLine line = new OrderLine("SKU-1", 1, new BigDecimal("10.00"));

      assertThatIllegalArgumentException()
          .isThrownBy(() -> Order.create("cust-123", List.of(line), currency))
          .withMessage("currency must be a valid ISO 4217 code");
    }
  }

  @Nested
  @DisplayName("equals() / hashCode()")
  class EqualsAndHashCode {

    @Test
    @DisplayName("is equal to itself")
    void equalToItself() {
      Order order =
          Order.create(
              "cust-123", List.of(new OrderLine("SKU-1", 1, new BigDecimal("10.00"))), "USD");

      assertThat(order).isEqualTo(order);
    }

    @Test
    @DisplayName("is not equal to null")
    void notEqualToNull() {
      Order order =
          Order.create(
              "cust-123", List.of(new OrderLine("SKU-1", 1, new BigDecimal("10.00"))), "USD");

      assertThat(order).isNotEqualTo(null);
    }

    @Test
    @DisplayName("is not equal to an instance of a different type")
    void notEqualToDifferentType() {
      Order order =
          Order.create(
              "cust-123", List.of(new OrderLine("SKU-1", 1, new BigDecimal("10.00"))), "USD");

      assertThat(order).isNotEqualTo("not an order");
    }

    @Test
    @DisplayName("two independently created orders are never equal")
    void differentOrdersAreNeverEqual() {
      OrderLine line = new OrderLine("SKU-1", 1, new BigDecimal("10.00"));

      Order first = Order.create("cust-123", List.of(line), "USD");
      Order second = Order.create("cust-123", List.of(line), "USD");

      assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("two instances sharing an orderId are equal, regardless of other fields")
    void sameOrderIdIsEqualRegardlessOfOtherFields() throws ReflectiveOperationException {
      Order first = withOrderId("shared-id", "cust-1", "USD");
      Order second = withOrderId("shared-id", "cust-2", "EUR");

      assertThat(first).isEqualTo(second);
      assertThat(first).hasSameHashCodeAs(second);
    }

    // Order's constructor is private - create() always assigns a fresh orderId, so this is
    // the only way to get two instances sharing one, which is what the DDD identity contract
    // being tested above actually requires.
    private Order withOrderId(String orderId, String customerId, String currency)
        throws ReflectiveOperationException {
      Constructor<Order> constructor =
          Order.class.getDeclaredConstructor(
              String.class,
              String.class,
              List.class,
              BigDecimal.class,
              String.class,
              Instant.class);
      constructor.setAccessible(true);
      OrderLine line = new OrderLine("SKU-1", 1, new BigDecimal("10.00"));
      return constructor.newInstance(
          orderId, customerId, List.of(line), BigDecimal.TEN, currency, Instant.now());
    }
  }
}
