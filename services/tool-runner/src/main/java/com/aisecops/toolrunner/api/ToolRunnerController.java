package com.aisecops.toolrunner.api;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/tools")
public class ToolRunnerController {

  private static final Logger log = LoggerFactory.getLogger(ToolRunnerController.class);

  @PostMapping(value = "/invoke", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public ToolInvokeResponse invoke(@RequestBody(required = false) ToolInvokeRequest req) {

    // Log raw request object
    log.info("AISecOps ToolRunner received request: {}", req);

    // Normalize inputs (avoid NPE + Map.of null issues)
    final String toolName =
        (req == null || req.toolName() == null || req.toolName().isBlank())
            ? "unknown"
            : req.toolName();

    final String correlationId =
        (req == null || req.correlationId() == null || req.correlationId().isBlank())
            ? "cid-" + System.currentTimeMillis()
            : req.correlationId();

    final Map<String, Object> args =
        (req == null || req.args() == null) ? Map.of() : req.args();

    // Build safe response object (never use Map.of with nullable values)
    final var result = new java.util.LinkedHashMap<String, Object>();
    result.put("message", "Tool execution not implemented yet");
    result.put("toolName", toolName);
    result.put("args", args);

    return new ToolInvokeResponse("STUB", result, correlationId);
  }

  public record ToolInvokeRequest(
      String toolName,
      Map<String, Object> args,
      String actor,
      String correlationId) {
  }

  public record ToolInvokeResponse(
      String status,
      Object result,
      String correlationId) {
  }
}
