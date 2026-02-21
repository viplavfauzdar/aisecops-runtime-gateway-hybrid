package com.aisecops.gatewayapi.api;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/v1")
public class ChatController {

  private final RestClient restClient;
  private final String policyUrl;

  public ChatController(@Value("${POLICY_ENGINE_URL:http://localhost:8081}") String policyUrl) {
    this.policyUrl = policyUrl;
    this.restClient = RestClient.create();
  }

  public record ChatRequest(String message) {}
  public record PolicyDecision(String decision, String redactedMessage, Integer riskScore) {}

  @PostMapping(value="/chat", consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
  public Map<String, Object> chat(@RequestBody ChatRequest req,
                                 @RequestHeader(value="X-Correlation-Id", required=false) String corr) {
    String correlationId = (corr == null || corr.isBlank()) ? UUID.randomUUID().toString().replace("-", "") : corr;

    // Call Policy Engine PDP
    PolicyDecision pd = restClient.post()
        .uri(policyUrl + "/api/v1/policy/eval")
        .header("X-Correlation-Id", correlationId)
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("message", req.message()))
        .retrieve()
        .body(PolicyDecision.class);

    // V1: Return policy decision only. Model/tool execution comes later.
    return Map.of(
        "correlationId", correlationId,
        "decision", pd.decision(),
        "riskScore", pd.riskScore(),
        "message", pd.redactedMessage()
    );
  }
}
