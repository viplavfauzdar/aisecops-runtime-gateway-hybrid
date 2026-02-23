package com.aisecops.orchestrator.api;

import com.aisecops.orchestrator.core.WorkflowRunner;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orchestrator")
public class OrchestratorController {

  private final WorkflowRunner runner;

  public OrchestratorController(WorkflowRunner runner) {
    this.runner = runner;
  }

  @PostMapping(
      value = "/run",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public OrchestratorRunResponse run(@RequestBody OrchestratorRunRequest req) {
    return runner.run(req);
  }

  public record OrchestratorRunRequest(
      String workflowId,
      Map<String, Object> inputs,
      String actor,
      String correlationId) {
  }

  public record OrchestratorRunResponse(
      String status,
      Object result,
      String correlationId) {
  }
}
