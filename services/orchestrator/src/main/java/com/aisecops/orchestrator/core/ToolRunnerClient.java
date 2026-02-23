package com.aisecops.orchestrator.core;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class ToolRunnerClient {

  private final WebClient webClient;

  public ToolRunnerClient(
      WebClient.Builder builder,
      @Value("${toolrunner.base-url:http://tool-runner:8083}") String baseUrl) {
    this.webClient = builder.baseUrl(baseUrl).build();
  }

  public Map<String, Object> invoke(
      String toolName,
      Map<String, Object> args,
      String actor,
      String correlationId) {

    return webClient.post()
        .uri("/api/v1/tools/invoke")
        .bodyValue(Map.of(
            "toolName", toolName,
            "args", args,
            "actor", actor,
            "correlationId", correlationId
        ))
        .retrieve()
        .bodyToMono(Map.class)
        .block();
  }
}