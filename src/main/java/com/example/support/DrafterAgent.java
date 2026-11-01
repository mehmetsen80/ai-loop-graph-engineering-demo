package com.example.support;

public final class DrafterAgent {

  private final LlmClient llm;

  public DrafterAgent(LlmClient llm) {
    this.llm = llm;
  }

  public Draft write(Plan plan, Evidence evidence, String critique) {
    String system = """
        You write customer-support replies.
        Cite policy id %s in the body.
        Do not promise Send for refunds over $100.
        """.formatted(plan.policyId());

    String user = """
        Intent: %s
        Amount cents: %d
        Order: %s
        Policy: %s
        Prior critique (fix if empty):
        %s
        """.formatted(
            plan.intent(),
            plan.amountCents(),
            evidence.order().id(),
            evidence.policy().text(),
            critique == null ? "" : critique);

    String text = llm.complete(system, user);
    // Ignore policy id tokens (e.g. POL-REFUND-PRO) when detecting refund promises.
    String probe = text.replace(plan.policyId(), "");
    boolean promisesRefund = probe.toLowerCase().contains("refund");
    return new Draft(text, plan.policyId(), promisesRefund);
  }
}
