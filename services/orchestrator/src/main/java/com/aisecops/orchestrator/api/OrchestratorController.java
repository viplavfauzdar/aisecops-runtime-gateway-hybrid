package com.aisecops.orchestrator.api;

import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orchestrator")
public class OrchestratorController {

  @PostMapping(value="/run", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> run(@RequestBody Map<String, Object> req) {
    // Placeholder: integrate OpenClaw workflows here
    return Map.of("status", "stub", "received", req);
  }
}
