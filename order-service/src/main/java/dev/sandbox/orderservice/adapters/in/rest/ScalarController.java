package dev.sandbox.orderservice.adapters.in.rest;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Inbound REST adapter - serves Scalar's API reference UI over order-service's generated OpenAPI
 * spec. Scalar has no first-party Java/Spring package, so this embeds its standard CDN-script
 * integration directly instead of a static resource file (which wouldn't respect the gating below).
 * Conditional on the whole bean, not just its content, so /scalar disappears entirely (404) rather
 * than rendering a page pointed at a disabled spec endpoint - see
 * docs/milestones/M2-order-service.md.
 */
@Controller
@ConditionalOnProperty(name = "springdoc.api-docs.enabled", matchIfMissing = true)
public class ScalarController {

  private static final String SCALAR_HTML =
      """
            <!doctype html>
            <html>
              <head>
                <title>order-service API Reference</title>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1" />
              </head>
              <body>
                <script id="api-reference" data-url="/v3/api-docs"></script>
                <script src="https://cdn.jsdelivr.net/npm/@scalar/api-reference"></script>
              </body>
            </html>
            """;

  @GetMapping(value = "/scalar", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> scalarUi() {
    return ResponseEntity.ok(SCALAR_HTML);
  }
}
