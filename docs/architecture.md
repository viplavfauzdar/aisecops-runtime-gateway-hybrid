# AISecOps Runtime Gateway Architecture (C4 Model)

This project documents architecture using the **C4 model**:

- Level 1 → Context
- Level 2 → Container
- Level 3 → Component

Additionally, we include:
- Security / Trust Boundary diagram (CISO-friendly)
- Deployment diagram (Platform/SRE-friendly)

---

## Level 1 — Context Diagram

File: `docs/diagrams/system.mmd`

Shows:
- External users (Employees / Apps)
- AISecOps Runtime Gateway (system)
- External LLM providers (OpenAI, Anthropic, Ollama)
- Enterprise SaaS systems (Jira, GitHub, Cloud APIs)

---

## Level 2 — Container Diagram

File: `docs/diagrams/system1.mmd`

Shows deployable units inside the system boundary:
- Gateway API (Spring Boot)
- Policy Engine / PDP (Spring Boot)
- Risk-Intel Service (Python / FastAPI)
- Orchestrator (Spring Boot + OpenClaw)
- Tool Runner (Spring Boot)
- Audit Log Store (PostgreSQL)

---

## Level 3 — Component Diagrams

Below are component-level diagrams for the most critical containers.

---

### Gateway API — Component Diagram

```mermaid
C4Component
title Gateway API – Component Diagram

Container_Boundary(gw, "Gateway API (Spring Boot)") {
  Component(chatController, "ChatController", "Spring MVC", "Receives /api/v1/chat requests")
  Component(auth, "Auth Layer (V1 stub)", "Filter/Interceptor", "Extracts actor, enforces authN/authZ (OIDC in V2)")
  Component(policyClient, "PolicyEngineClient", "HTTP Client", "Calls Policy Engine for PDP decisions")
  Component(orchestratorClient, "OrchestratorClient", "HTTP Client", "Triggers workflows when allowed (V1 stub)")
  Component(toolRunnerClient, "ToolRunnerClient", "HTTP Client", "Invokes tools when allowed (V1 stub)")
  Component(auditWriter, "AuditWriter", "JDBC", "Writes gateway audit events to Postgres")
  ComponentDb(auditdb, "Audit Log Store", "PostgreSQL", "Stores audit_event records")
}

System_Ext(policy, "Policy Engine (PDP)", "Spring Boot")
System_Ext(orchestrator, "Orchestrator", "Spring Boot + OpenClaw")
System_Ext(toolrunner, "Tool Runner", "Spring Boot")

Rel(chatController, auth, "Passes request through")
Rel(chatController, policyClient, "Requests policy decision")
Rel(policyClient, policy, "POST /api/v1/policy/eval")
Rel(chatController, orchestratorClient, "Calls when allowed (future)")
Rel(orchestratorClient, orchestrator, "POST /api/v1/orchestrator/run")
Rel(chatController, toolRunnerClient, "Calls when needed (future)")
Rel(toolRunnerClient, toolrunner, "POST /api/v1/tools/invoke")
Rel(chatController, auditWriter, "Writes audit event")
Rel(auditWriter, auditdb, "INSERT audit_event")
```

---

### Policy Engine (PDP) — Component Diagram

```mermaid
C4Component
title Policy Engine (PDP) – Component Diagram

Container_Boundary(policy, "Policy Engine (Spring Boot)") {
  Component(policyController, "PolicyController", "Spring MVC", "Handles /policy/eval endpoint")
  Component(policyService, "PolicyService", "Java Service", "Loads policies, evaluates decisions, applies redaction")
  Component(policyLoader, "PolicyLoader", "SnakeYAML", "Parses default.yaml policy config")
  Component(riskClient, "RiskIntelClient", "HTTP Client", "Calls Risk-Intel for risk scoring")
  Component(redactor, "Redactor", "Regex Rules", "Redacts PII in V1")
  Component(auditWriter, "AuditWriter", "JDBC", "Persists audit events to Postgres")
  ComponentDb(auditdb, "Audit Log Store", "PostgreSQL", "Stores audit_event records")
}

System_Ext(risk, "Risk-Intel Service", "Python / FastAPI")

Rel(policyController, policyService, "Delegates evaluation")
Rel(policyService, policyLoader, "Loads policy config")
Rel(policyService, riskClient, "Requests risk score")
Rel(riskClient, risk, "POST /api/v1/risk/score")
Rel(policyService, redactor, "Redacts if needed")
Rel(policyService, auditWriter, "Writes audit event")
Rel(auditWriter, auditdb, "INSERT audit_event")
```

---

### Risk-Intel Service — Component Diagram

```mermaid
C4Component
title Risk-Intel (Python/FastAPI) – Component Diagram

Container_Boundary(risk, "Risk-Intel Service") {
  Component(api, "FastAPI App", "FastAPI", "Exposes /health and /api/v1/risk/score")
  Component(scoring, "Heuristic Scoring", "Python", "Computes riskScore based on PII + danger terms")
  Component(patterns, "PII Pattern Rules", "Regex", "Email/Phone/SSN detection")
}

Rel(api, scoring, "Invokes scoring logic")
Rel(scoring, patterns, "Evaluates regex rules")
```

---

## Security / Trust Boundary Diagram

This diagram is designed for security reviews and threat modeling.

```mermaid
flowchart LR
  subgraph Z0["Untrusted Zone (User Devices)"]
    U["Employees / Apps"]
  end

  subgraph Z1["Enterprise Trust Zone (Corp Network / VPN)"]
    SSO["Enterprise SSO (OIDC/OAuth2)"]
    GW["Gateway API"]
    PDP["Policy Engine (PDP)"]
    RI["Risk-Intel"]
    ORCH["Orchestrator (OpenClaw)"]
    TOOL["Tool Runner (Sandbox Exec)"]
    AUD["Audit DB (Postgres)"]
  end

  subgraph Z2["External / Third-Party Zone"]
    OAI["OpenAI"]
    ANT["Anthropic"]
    EXT["Enterprise SaaS (Jira/GitHub/Cloud APIs)"]
  end

  subgraph Z3["On-Prem / Local Model Zone"]
    OLL["Ollama (Local LLMs)"]
  end

  U -->|HTTPS| SSO;
  SSO -->|JWT| GW;
  GW -->|PDP call| PDP;
  PDP -->|Risk score| RI;

  GW -->|Audit write| AUD;
  PDP -->|Audit write| AUD;
  ORCH -->|Audit write| AUD;
  TOOL -->|Audit write| AUD;

  GW -->|Allowed exec| ORCH;
  ORCH --> TOOL;
  TOOL -->|Allowlisted outbound| EXT;

  ORCH -->|Model routing| OLL;
  ORCH -->|Model routing| OAI;
  ORCH -->|Model routing| ANT;
```

---

## Deployment Diagram (Kubernetes-oriented)

This is the production deployment view (can be adapted to ECS/VMs).

```mermaid
flowchart TB
  subgraph K8S["Kubernetes Cluster"]
    subgraph NS["Namespace: aisecops"]
      IN["Ingress / API Gateway"]
      GW["gateway-api Deployment"]
      PDP["policy-engine Deployment"]
      RI["risk-intel Deployment"]
      ORCH["orchestrator Deployment"]
      TOOL["tool-runner Deployment"]
      DB["Postgres (StatefulSet or Managed)"]
      OBS["Observability (Prometheus / OTel Collector)"]
    end
  end

  USERS["Employees / Apps"] -->|HTTPS| IN;
  IN --> GW;

  GW --> PDP;
  PDP --> RI;

  GW --> DB;
  PDP --> DB;
  ORCH --> DB;
  TOOL --> DB;

  GW --> OBS;
  PDP --> OBS;
  RI --> OBS;
  ORCH --> OBS;
  TOOL --> OBS;

  ORCH -->|Policy-controlled egress| LLMEXT["OpenAI / Anthropic"];
  ORCH -->|In-cluster| OLL["Ollama (optional in-cluster or on-prem)"];
  TOOL -->|Egress allowlist| SAAS["Jira / GitHub / Cloud APIs"];
```

---

## What to add next (once V2 features land)

- Gateway API component diagram update for real OIDC (Okta/Azure AD)
- Orchestrator components: workflow engine, tool registry, agent executor
- Tool Runner components: allowlist, Docker runner, network policies
- Model Router components: provider adapters + cost/risk routing
- A dedicated Threat Model doc (`docs/threat-model.md`) with STRIDE/LINDDUN