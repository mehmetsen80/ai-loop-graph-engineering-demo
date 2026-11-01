package com.example.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CritiqueResultTest {

  @Test
  void approvedFactorySetsFlagAndEmptyNotes() {
    CritiqueResult r = CritiqueResult.approved();
    assertTrue(r.isApproved());
    assertEquals("", r.notes());
  }

  @Test
  void retryFactoryKeepsNotes() {
    CritiqueResult r = CritiqueResult.retry("Cite policy POL-1");
    assertFalse(r.isApproved());
    assertEquals("Cite policy POL-1", r.notes());
  }

  @Test
  void retryRejectsBlankNotes() {
    assertThrows(IllegalArgumentException.class, () -> CritiqueResult.retry("  "));
    assertThrows(IllegalArgumentException.class, () -> CritiqueResult.retry(null));
  }
}
