package com.example.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HumanGateTest {

  @Test
  void requireSendWritesCrmAndAudit() {
    List<String> sent = new ArrayList<>();
    List<String> decisions = new ArrayList<>();
    AuditLog audit = new AuditLog() {
      public void node(String t, String n, String d) {}
      public void tool(String t, String tool, String ref) {}
      public void decision(String t, String decision, String actorId) { decisions.add(decision); }
    };
    HumanGate gate = new HumanGate(
        (ticketId, body, actorId) -> sent.add(body),
        audit,
        new DeskConsole() {
          public String actorId() { return "desk-1"; }
          public boolean confirmSend(String ticketId, String body) { return true; }
        });

    Draft draft = new Draft("Hello POL-REFUND-PRO", "POL-REFUND-PRO", false);
    Reply reply = gate.requireSend(draft, new Ticket("t1", "x", "c"));
    assertEquals("Hello POL-REFUND-PRO", reply.body());
    assertEquals(1, sent.size());
    assertEquals(List.of("SENT"), decisions);
  }
}
