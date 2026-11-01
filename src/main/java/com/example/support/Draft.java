package com.example.support;

public record Draft(String text, String policyId, boolean promisesRefund) {
  public boolean citesPolicy(String expectedPolicyId) {
    return policyId != null
        && policyId.equals(expectedPolicyId)
        && text != null
        && text.contains(expectedPolicyId);
  }
}

/** Final shipped reply after human Send. */
