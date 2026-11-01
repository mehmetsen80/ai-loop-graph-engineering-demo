package com.example.support;

public record Ticket(String id, String body, String customerId) {}

/** Runtime context (identity, locale, channel). */
