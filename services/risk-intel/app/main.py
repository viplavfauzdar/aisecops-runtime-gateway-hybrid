from fastapi import FastAPI
from pydantic import BaseModel
import re

app = FastAPI(title="AISecOps Risk Intel", version="0.1.0")

EMAIL_RE = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}")
PHONE_RE = re.compile(r"\b(?:\+1[-. ]?)?\(?\d{3}\)?[-. ]?\d{3}[-. ]?\d{4}\b")
SSN_RE = re.compile(r"\b\d{3}-\d{2}-\d{4}\b")

class ScoreRequest(BaseModel):
    text: str

class ScoreResponse(BaseModel):
    containsPii: bool
    riskScore: int

@app.get("/health")
def health():
    return {"status": "ok", "service": "risk-intel"}

def heuristic_risk(text: str) -> tuple[bool, int]:
    contains = False
    score = 5

    if EMAIL_RE.search(text):
        contains = True
        score += 35
    if PHONE_RE.search(text):
        contains = True
        score += 25
    if SSN_RE.search(text):
        contains = True
        score += 60

    # crude “dangerous instruction” heuristic (V1 placeholder)
    danger_terms = ["exfiltrate", "steal", "bypass", "credentials", "password", "token", "disable security"]
    if any(t in text.lower() for t in danger_terms):
        score += 30

    return contains, min(score, 100)

@app.post("/api/v1/risk/score", response_model=ScoreResponse)
def score(req: ScoreRequest):
    contains, score = heuristic_risk(req.text)
    return ScoreResponse(containsPii=contains, riskScore=score)
