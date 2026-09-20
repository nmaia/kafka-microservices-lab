package dev.sandbox.orderservice.adapters.in.rest;

import dev.sandbox.orderservice.application.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Adapter - centralizes REST error mapping so OrderController stays free of try/catch. Domain
 * validation failures (IllegalArgumentException from Order.create()/OrderLine) become 400s; a
 * missing order becomes a 404 - both as RFC 7807 ProblemDetail, matching Spring Boot's own built-in
 * exception handling once spring.mvc.problemdetails.enabled is on.
 */
@RestControllerAdvice
public class OrderRestExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleInvalidOrder(IllegalArgumentException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(OrderNotFoundException.class)
  public ProblemDetail handleOrderNotFound(OrderNotFoundException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }
}
