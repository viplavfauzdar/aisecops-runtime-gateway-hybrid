package com.aisecops.toolrunner.api;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tools")
public class ToolRunnerController {

  @PostMapping(value="/invoke", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> invoke(@RequestBody Map<String, Object> req) {
    // Placeholder: implement allowlist + sandbox execution
    return Map.of("status", "stub", "result", Map.of("message", "Tool execution not implemented yet"), "request", req);
  }
}
