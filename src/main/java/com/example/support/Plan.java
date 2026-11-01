package com.example.support;

public record Plan(
    String ticketId,
    String intent,       // e.g. "refund"
    String policyId,     // e.g. "POL-REFUND-PRO"
    int amountCents
) {}
