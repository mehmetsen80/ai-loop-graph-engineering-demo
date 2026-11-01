package com.example.support;

import java.util.ArrayList;
import java.util.List;

/**
 * Observe step: policy / finance / tone gates → {@link CritiqueResult}.
 *
 * <p>This teaching build is <strong>deterministic</strong> (rules + {@link ToneCheck}) so the Loop
 * latch stays cheap and unit-testable.
 *
 * <p>TODO(llm-critic): optional LLM critic path — inject {@link LlmClient}, keep the same
 * {@code CritiqueResult.approved()} / {@code retry(notes)} contract, and declare a
 * {@code CRITIC → LLM} {@link GraphEdge} in {@link SupportGraph#wire}. Hard gates (policy cite,
 * finance threshold) should still run in code so an LLM cannot approve a forbidden Send.
 */
public final class ReplyCritic {

  private static final int FINANCE_THRESHOLD_CENTS = 10_000;

  private final ToneCheck toneCheck;

  public ReplyCritic(ToneCheck toneCheck) {
    this.toneCheck = toneCheck;
  }

  public CritiqueResult review(Draft draft, Plan plan) {
    List<String> notes = new ArrayList<>();

    if (!draft.citesPolicy(plan.policyId())) {
      notes.add("Cite policy " + plan.policyId());
    }
    if (draft.promisesRefund()
        && plan.amountCents() > FINANCE_THRESHOLD_CENTS) {
      notes.add(
          "Refund over $100 needs finance; do not promise Send.");
    }
    if (toneCheck.isHostile(draft.text())) {
      notes.add("Soften tone; no blame on the customer.");
    }

    if (notes.isEmpty()) {
      return CritiqueResult.approved();
    }
    return CritiqueResult.retry(String.join("\n", notes));
  }
}
