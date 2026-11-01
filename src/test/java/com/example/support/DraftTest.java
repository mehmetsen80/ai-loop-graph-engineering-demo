package com.example.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DraftTest {

  @Test
  void citesPolicyRequiresIdInBody() {
    Draft ok = new Draft("See POL-1 here", "POL-1", false);
    assertTrue(ok.citesPolicy("POL-1"));
    Draft miss = new Draft("No id here", "POL-1", false);
    assertFalse(miss.citesPolicy("POL-1"));
  }
}
