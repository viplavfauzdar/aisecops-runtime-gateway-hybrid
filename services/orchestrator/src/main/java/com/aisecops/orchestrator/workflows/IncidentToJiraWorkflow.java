
package com.aisecops.orchestrator.workflows;

import java.time.OffsetDateTime;
import java.util.Map;

public class IncidentToJiraWorkflow {

  public record Result(
      String title,
      String severity,
      String description,
      Map<String, Object> metadata
  ) {}

  /**
   * V1: deterministic workflow that transforms an incoming alert payload into a Jira ticket draft.
   * Step 2 will actually call Tool Runner to create the ticket.
   */
  public Result run(Map<String, Object> inputs, String actor, String correlationId) {
    Map<String, Object> safeInputs = (inputs == null) ? Map.of() : inputs;

    String alert = asString(safeInputs.get("alert"));
    String service = asString(safeInputs.getOrDefault("service", "unknown-service"));
    String env = asString(safeInputs.getOrDefault("env", "prod"));

    String severity = deriveSeverity(alert);
    String title = "[INCIDENT][" + env + "][" + severity + "] " + summarize(alert);

    String description = ""
        + "Actor: " + nullToUnknown(actor) + "\n"
        + "CorrelationId: " + nullToUnknown(correlationId) + "\n"
        + "Timestamp: " + OffsetDateTime.now() + "\n\n"
        + "Alert:\n"
        + (alert.isBlank() ? "No alert details" : alert) + "\n\n"
        + "Service: " + service + "\n"
        + "Environment: " + env + "\n";

    return new Result(
        title,
        severity,
        description,
        Map.of(
            "workflow", "incident_to_jira",
            "service", service,
            "env", env
        )
    );
  }

  private static String asString(Object o) {
    return o == null ? "" : String.valueOf(o).trim();
  }

  private static String nullToUnknown(String s) {
    return (s == null || s.isBlank()) ? "unknown" : s;
  }

  private static String summarize(String alert) {
    if (alert == null) return "No alert details";
    String a = alert.replaceAll("\\s+", " ").trim();
    if (a.isEmpty()) return "No alert details";
    return a.length() <= 120 ? a : a.substring(0, 117) + "...";
  }

  private static String deriveSeverity(String alert) {
    if (alert == null) return "MEDIUM";
    String a = alert.toLowerCase();
    if (a.contains("sev0") || a.contains("sev-0") || a.contains("p0") || a.contains("outage") || a.contains("down")) {
      return "CRITICAL";
    }
    if (a.contains("sev1") || a.contains("sev-1") || a.contains("p1") || a.contains("degraded") || a.contains("error rate")) {
      return "HIGH";
    }
    if (a.contains("warn") || a.contains("warning") || a.contains("latency")) {
      return "MEDIUM";
    }
    return "LOW";
  }
}
