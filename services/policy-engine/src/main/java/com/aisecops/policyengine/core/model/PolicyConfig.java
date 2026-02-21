package com.aisecops.policyengine.core.model;

public class PolicyConfig {
  private int denyThreshold = 80;
  private boolean redactPii = true;

  public int getDenyThreshold() { return denyThreshold; }
  public void setDenyThreshold(int denyThreshold) { this.denyThreshold = denyThreshold; }

  public boolean isRedactPii() { return redactPii; }
  public void setRedactPii(boolean redactPii) { this.redactPii = redactPii; }

  public static PolicyConfig defaultConfig() {
    return new PolicyConfig();
  }
}
