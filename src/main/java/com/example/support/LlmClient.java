package com.example.support;

public interface LlmClient {
  String complete(String systemPrompt, String userPrompt);
}
