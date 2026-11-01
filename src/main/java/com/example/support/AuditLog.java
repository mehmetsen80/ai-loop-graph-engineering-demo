package com.example.support;

public interface AuditLog {
  void node(String ticketId, String node, String detail);
  void tool(String ticketId, String tool, String ref);
  /** decision: APPROVED | RETRY | SENT | REJECTED */
  void decision(String ticketId, String decision, String actorId);
}
