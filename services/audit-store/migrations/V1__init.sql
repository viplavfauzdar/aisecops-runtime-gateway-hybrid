-- Minimal audit schema for V1
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
CREATE INDEX IF NOT EXISTS idx_audit_corr ON audit_event(correlation_id);
CREATE INDEX IF NOT EXISTS idx_audit_ts ON audit_event(ts);
