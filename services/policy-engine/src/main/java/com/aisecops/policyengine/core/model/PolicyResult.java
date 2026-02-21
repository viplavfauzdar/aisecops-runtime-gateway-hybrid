package com.aisecops.policyengine.core.model;

public record PolicyResult(String decision, String redactedMessage, Integer riskScore) {}
