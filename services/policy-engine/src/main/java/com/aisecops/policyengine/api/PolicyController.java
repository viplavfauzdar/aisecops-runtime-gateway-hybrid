package com.aisecops.policyengine.api;

import com.aisecops.policyengine.core.PolicyService;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/policy")
public class PolicyController {

  private final PolicyService policyService;

  public PolicyController(PolicyService policyService) {
    this.policyService = policyService;
  }

  public record EvalRequest(@NotBlank String message) {}
  public record EvalResponse(String decision, String redactedMessage, Integer riskScore) {}

  @PostMapping(value="/eval", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
  public EvalResponse eval(@RequestBody EvalRequest req,
                           @RequestHeader(value="X-Correlation-Id", required=false) String corr) {
    String correlationId = (corr == null || corr.isBlank()) ? UUID.randomUUID().toString().replace("-", "") : corr;
    var result = policyService.evaluate(req.message(), correlationId, "policy-engine");
    return new EvalResponse(result.decision(), result.redactedMessage(), result.riskScore());
  }

  @GetMapping("/active")
  public Map<String, Object> active() {
    return Map.of("policyFile", policyService.getPolicyFile(), "loaded", true);
  }
}
