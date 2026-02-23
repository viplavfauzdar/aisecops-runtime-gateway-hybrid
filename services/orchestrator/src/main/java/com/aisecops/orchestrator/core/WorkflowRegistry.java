package com.aisecops.orchestrator.core;

import com.aisecops.orchestrator.workflows.IncidentToJiraWorkflow;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class WorkflowRegistry {

  private final IncidentToJiraWorkflow incidentToJiraWorkflow = new IncidentToJiraWorkflow();

  public Object resolve(String workflowId) {
    if (workflowId == null || workflowId.isBlank()) {
      throw new IllegalArgumentException("workflowId is required");
    }

    String key = workflowId.trim().toUpperCase(Locale.ROOT).replace('-', '_');

    if (key.equals("INCIDENT_TO_JIRA")) {
      return incidentToJiraWorkflow;
    }

    throw new IllegalArgumentException("Unknown workflowId: " + workflowId);
  }
}
