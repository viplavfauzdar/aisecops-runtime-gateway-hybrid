# AISecOps Runtime Gateway (Hybrid: Spring Boot + Python Risk Service)

**Generated:** 2026-02-20 America/New_York

This repository is a **starter skeleton** for an enterprise-grade AI runtime gateway:
- **Spring Boot control plane**: auth boundary, policy decision point, orchestration, audit, admin backend
- **Python risk-intel microservice**: PII/sensitivity classification, output risk scoring (model-assisted later)
---

## 🔗 Related Projects

This repository was originally built as part of the **AISecOps proof‑of‑concept stack** together with the OpenClaw integration plugin:

Plugin repository:
https://github.com/viplavfauzdar/aisecops-openclaw-plugin

In the original architecture the flow looked like:

```
OpenClaw Agent
      ↓
aisecops-openclaw-plugin
      ↓
AISecOps Runtime Gateway
      ↓
Policy Engine + Risk Service
      ↓
Tool Runner / Sandbox
```

The gateway acts as the **control plane backend** responsible for:

- policy evaluation
- risk scoring and classification
- human‑in‑the‑loop approvals
- audit logging
- tool execution mediation

### Evolution of the architecture

The current direction of the project is moving toward a **framework‑agnostic AISecOps Interceptor** which can integrate with multiple agent frameworks (OpenClaw, LangGraph, CrewAI, etc.).

In the newer model the runtime interception layer sits closer to the agent runtime:

```
Agent / Framework
      ↓
AISecOps Interceptor
      ↓
Policy Engine / Risk / Approval
      ↓
Tool Execution
```

In that architecture this **Runtime Gateway can serve as the backend control plane**, while the interceptor acts as the portable runtime enforcement layer.

## Services (V1)
- `services/gateway-api` (Spring Boot): entrypoint, auth, routing, correlation ids
- `services/policy-engine` (Spring Boot): policy evaluation (YAML), redaction + HITL decisions, calls risk-intel
- `services/orchestrator` (Spring Boot): OpenClaw integration layer (placeholder hooks)
- `services/tool-runner` (Spring Boot): sandboxed tool execution (Docker runner hooks)
- `services/risk-intel` (Python/FastAPI): classify prompt/output sensitivity + risk score
- `infra/postgres`: audit/event store

## Quick start (dev)
1. Copy env:
   ```bash
   cp .env.example .env
   ```
2. Start everything:
   ```bash
   docker compose up --build
   ```
3. Try a request (gateway):
   ```bash
   curl -s http://localhost:8080/api/v1/chat -H "Content-Type: application/json" -d '{"message":"hello world"}' | jq
   ```

## What’s implemented vs stubbed
✅ Running service skeletons + health endpoints + basic request flow + risk service stub  
✅ YAML policy loading + basic allow/deny/redact decisions (simple defaults)  
✅ Audit event table + writes (minimal)  
⬜ OpenClaw orchestration wiring (placeholder)  
⬜ Real tool sandbox + allowlists (placeholder)  
⬜ Real OIDC integration (dev stub auth)

## Next steps
- Replace dev auth with OIDC (Okta/Azure AD)
- Implement allowlisted tools + sandbox runner
- Add model adapters (Ollama/OpenAI/Anthropic)
- Upgrade risk-intel from regex to lightweight classifier

## Architecture Documentation

Full architecture documentation (C4 model, security boundaries, and deployment view):

- 📐 System Context & Container diagrams: `docs/diagrams/`
- 🧠 Full architecture doc (C4 + Security + Deployment): `docs/architecture.md`

Open locally:

```bash
code docs/architecture.md
```

Or preview with Markdown + Mermaid support enabled in VS Code.
