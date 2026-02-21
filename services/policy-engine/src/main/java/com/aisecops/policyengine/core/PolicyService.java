package com.aisecops.policyengine.core;

import com.aisecops.policyengine.core.model.PolicyConfig;
import com.aisecops.policyengine.core.model.PolicyResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.yaml.snakeyaml.Yaml;

@Service
public class PolicyService {

  private final JdbcTemplate jdbc;
  private final RestClient restClient;
  private final ObjectMapper mapper;

  private final String riskIntelUrl;
  private final String policyFile;

  private volatile PolicyConfig config;

  public PolicyService(JdbcTemplate jdbc,
                       ObjectMapper mapper,
                       @Value("${RISK_INTEL_URL:http://localhost:8090}") String riskIntelUrl,
                       @Value("${POLICY_FILE:src/main/resources/policies/default.yaml}") String policyFile) {
    this.jdbc = jdbc;
    this.mapper = mapper;
    this.riskIntelUrl = riskIntelUrl;
    this.policyFile = policyFile;
    this.restClient = RestClient.create();
    loadPolicies();
    ensureSchema();
  }

  public String getPolicyFile() { return policyFile; }

  private void ensureSchema() {
    // minimal bootstrap (works even if migrations not applied yet)
    jdbc.execute("""
      CREATE TABLE IF NOT EXISTS audit_event (
        id BIGSERIAL PRIMARY KEY,
        ts TIMESTAMPTZ NOT NULL DEFAULT now(),
        correlation_id VARCHAR(64) NOT NULL,
        actor VARCHAR(128),
        service VARCHAR(64) NOT NULL,
        event_type VARCHAR(64) NOT NULL,
        risk_score INTEGER,
        payload JSONB
      );
    """);
  }

  public synchronized void loadPolicies() {
    try {
      var yamlStr = Files.readString(Path.of(policyFile));
      var yaml = new Yaml();
      var parsed = yaml.loadAs(yamlStr, PolicyConfig.class);
      this.config = parsed == null ? PolicyConfig.defaultConfig() : parsed;
    } catch (Exception e) {
      this.config = PolicyConfig.defaultConfig();
    }
  }

  public PolicyResult evaluate(String message, String correlationId, String serviceName) {
    int riskScore = callRiskIntel(message).riskScore();
    boolean containsPii = callRiskIntel(message).containsPii();

    // Simple V1 logic:
    // - If PII and policy says local-only => REDACT
    // - If riskScore >= deny_threshold => DENY
    // - Else ALLOW (possibly redacted)
    var denyThreshold = config.getDenyThreshold();
    var redact = containsPii && config.isRedactPii();

    String decision;
    String redactedMessage = message;

    if (riskScore >= denyThreshold) {
      decision = "DENY";
    } else if (redact) {
      decision = "REDACT";
      redactedMessage = redactBasic(message);
    } else {
      decision = "ALLOW";
    }

    writeAudit(correlationId, serviceName, "POLICY_DECISION", riskScore,
        Map.of("decision", decision, "containsPii", containsPii, "messageHash", Integer.toHexString(message.hashCode())));

    return new PolicyResult(decision, redactedMessage, riskScore);
  }

  private record RiskIntelResponse(boolean containsPii, int riskScore) {}

  private RiskIntelResponse callRiskIntel(String text) {
    try {
      var resp = restClient.post()
          .uri(riskIntelUrl + "/api/v1/risk/score")
          .body(Map.of("text", text))
          .retrieve()
          .body(RiskIntelResponse.class);
      return resp == null ? new RiskIntelResponse(false, 0) : resp;
    } catch (Exception e) {
      // fail-safe: be conservative (moderate risk) if the service is down
      return new RiskIntelResponse(false, 25);
    }
  }

  private String redactBasic(String s) {
    // Very basic patterns: emails + US phone
    return s
        .replaceAll("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}", "[REDACTED_EMAIL]")
        .replaceAll("\\b(?:\\+1[-. ]?)?\\(?\\d{3}\\)?[-. ]?\\d{3}[-. ]?\\d{4}\\b", "[REDACTED_PHONE]");
  }

  private void writeAudit(String correlationId, String service, String eventType, Integer riskScore, Map<String, Object> payload) {
    try {
      var json = mapper.writeValueAsString(payload);
      jdbc.update("""
        INSERT INTO audit_event(correlation_id, actor, service, event_type, risk_score, payload)
        VALUES (?, ?, ?, ?, ?, ?::jsonb)
      """, correlationId, null, service, eventType, riskScore, json);
    } catch (Exception ignored) {}
  }
}
