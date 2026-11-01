package com.example.support;

public final class TriageAgent {

  private final LlmClient llm;

  public TriageAgent(LlmClient llm) {
    this.llm = llm;
  }

  public Plan plan(Ticket ticket) {
    // In production: structured LLM output. Teaching stub:
    return new Plan(
        ticket.id(),
        "refund",
        "POL-REFUND-PRO",
        12_500
    );
  }
}

/** Thin LLM port — Drafter and Triage share this, not CRM tools. */
