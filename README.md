# AISecOps Runtime Gateway (Hybrid: Spring Boot + Python Risk Service)

**Generated:** 2026-02-20 America/New_York

This repository is a **starter skeleton** for an enterprise-grade AI runtime gateway:
- **Spring Boot control plane**: auth boundary, policy decision point, orchestration, audit, admin backend
- **Python risk-intel microservice**: PII/sensitivity classification, output risk scoring (model-assisted later)

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
