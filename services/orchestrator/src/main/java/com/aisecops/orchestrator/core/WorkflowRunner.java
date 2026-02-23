package com.aisecops.orchestrator.core;

import com.aisecops.orchestrator.api.OrchestratorController.OrchestratorRunRequest;
import com.aisecops.orchestrator.api.OrchestratorController.OrchestratorRunResponse;
import com.aisecops.orchestrator.workflows.IncidentToJiraWorkflow;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WorkflowRunner {

    private final WorkflowRegistry registry;
    private final ToolRunnerClient toolRunnerClient;

    public WorkflowRunner(WorkflowRegistry registry,
            ToolRunnerClient toolRunnerClient) {
        this.registry = registry;
        this.toolRunnerClient = toolRunnerClient;
    }

    public OrchestratorRunResponse run(OrchestratorRunRequest req) {
        String workflowId = req.workflowId();
        String actor = req.actor();
        String correlationId = req.correlationId();
        Map<String, Object> inputs = (req.inputs() == null) ? Map.of() : req.inputs();

        Object wf = registry.resolve(workflowId);

        Object result;
        if (wf instanceof IncidentToJiraWorkflow incident) {
            var draft = incident.run(inputs, actor, correlationId);

            Map<String, Object> toolResult = toolRunnerClient.invoke(
                    "jira.create_issue",
                    Map.of(
                            "title", draft.title(),
                            "description", draft.description(),
                            "severity", draft.severity(),
                            "metadata", draft.metadata()),
                    actor,
                    correlationId);

            result = Map.of(
                    "ticketDraft", draft,
                    "toolResponse", toolResult);
        } else {
            throw new IllegalStateException(
                    "Unsupported workflow type: " + wf.getClass().getName());
        }

        return new OrchestratorRunResponse("OK", result, correlationId);
    }
}
