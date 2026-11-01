package com.example.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReplyCriticTest {

  private final ReplyCritic critic = new ReplyCritic(new ToneCheck());

  @Test
  void approvesCleanDraftUnderThreshold() {
    Plan plan = new Plan("t1", "refund", "POL-REFUND-PRO", 5_000);
    Draft draft = new Draft(
        "We can help. See POL-REFUND-PRO for details.",
        "POL-REFUND-PRO",
        true);
    CritiqueResult r = critic.review(draft, plan);
    assertTrue(r.isApproved());
  }

  @Test
  void retriesWhenPolicyNotCited() {
    Plan plan = new Plan("t1", "refund", "POL-REFUND-PRO", 5_000);
    Draft draft = new Draft("We can help with a refund.", "POL-REFUND-PRO", true);
    CritiqueResult r = critic.review(draft, plan);
    assertFalse(r.isApproved());
    assertTrue(r.notes().contains("Cite policy POL-REFUND-PRO"));
  }

  @Test
  void retriesLargeRefundPromise() {
    Plan plan = new Plan("t1", "refund", "POL-REFUND-PRO", 12_500);
    Draft draft = new Draft(
        "We will refund you today. POL-REFUND-PRO.",
        "POL-REFUND-PRO",
        true);
    CritiqueResult r = critic.review(draft, plan);
    assertFalse(r.isApproved());
    assertTrue(r.notes().contains("finance"));
  }

  @Test
  void retriesHostileTone() {
    Plan plan = new Plan("t1", "refund", "POL-REFUND-PRO", 5_000);
    Draft draft = new Draft(
        "This is your fault. See POL-REFUND-PRO.",
        "POL-REFUND-PRO",
        false);
    CritiqueResult r = critic.review(draft, plan);
    assertFalse(r.isApproved());
    assertTrue(r.notes().toLowerCase().contains("tone"));
  }
}
