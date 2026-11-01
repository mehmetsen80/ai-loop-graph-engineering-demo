package com.example.support;

public final class ToneCheck {

  public boolean isHostile(String text) {
    if (text == null || text.isBlank()) {
      return true;
    }
    String lower = text.toLowerCase();
    return lower.contains("your fault")
        || lower.contains("you are wrong")
        || lower.contains("deal with it");
  }
}
