package com.example.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToneCheckTest {

  private final ToneCheck tone = new ToneCheck();

  @Test
  void flagsHostilePhrases() {
    assertTrue(tone.isHostile("This is your fault entirely."));
    assertTrue(tone.isHostile("You are wrong about the order."));
  }

  @Test
  void allowsNeutralCopy() {
    assertFalse(tone.isHostile("Sorry about the damage — here is a replacement."));
  }

  @Test
  void blankIsHostile() {
    assertTrue(tone.isHostile(""));
    assertTrue(tone.isHostile(null));
  }
}
