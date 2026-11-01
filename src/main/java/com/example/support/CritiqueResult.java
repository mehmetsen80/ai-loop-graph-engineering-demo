package com.example.support;

public final class CritiqueResult {

  private final boolean approved;
  private final String notes;

  private CritiqueResult(boolean approved, String notes) {
    this.approved = approved;
    this.notes = notes == null ? "" : notes;
  }

  public static CritiqueResult approved() {
    return new CritiqueResult(true, "");
  }

  /** Build a retry with free-text notes for the Drafter. */
  public static CritiqueResult retry(String notes) {
    if (notes == null || notes.isBlank()) {
      throw new IllegalArgumentException("retry requires notes");
    }
    return new CritiqueResult(false, notes);
  }

  public boolean isApproved() {
    return approved;
  }

  public String notes() {
    return notes;
  }
}
