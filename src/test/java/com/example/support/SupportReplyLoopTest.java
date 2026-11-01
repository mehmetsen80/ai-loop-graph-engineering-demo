package com.example.support;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SupportReplyLoopTest {

  static final class MemAudit implements AuditLog {
    final List<String> decisions = new ArrayList<>();
    public void node(String ticketId, String node, String detail) {}
    public void tool(String ticketId, String tool, String ref) {}
    public void decision(String ticketId, String decision, String actorId) {
      decisions.add(decision);
    }
  }

  static final class StubLlm implements LlmClient {
    private final AtomicInteger calls = new AtomicInteger();
    private final List<String> scripts;
    StubLlm(String... scripts) { this.scripts = List.of(scripts); }
    public String complete(String systemPrompt, String userPrompt) {
      int i = Math.min(calls.getAndIncrement(), scripts.size() - 1);
      return scripts.get(i);
    }
    int callCount() { return calls.get(); }
  }

  static OrderLookup orders() {
    return ticketId -> new Order("ord-1", "PRO", 12_500);
  }

  static PolicySearch policies() {
    return (intent, planCode) -> new Policy("POL-REFUND-PRO", "Refunds under $100 auto.");
  }

  static DeskConsole acceptConsole() {
    return new DeskConsole() {
      public String actorId() { return "agent-9"; }
      public boolean confirmSend(String ticketId, String body) { return true; }
    };
  }

  static CrmSend recordingCrm(List<String> sent) {
    return (ticketId, body, actorId) -> sent.add(ticketId + "|" + actorId);
  }

  @Test
  void retriesThenApprovesAndSends() {
    // First draft misses policy citation; second cites it and avoids refund promise language path
    // amount is 12500 from triage stub — so promisesRefund true would fail finance check.
    // Second draft: cites policy, does not promise refund.
    StubLlm llm = new StubLlm(
        "We will process a refund immediately.",
        "We can arrange a replacement. See POL-REFUND-PRO.");
    MemAudit audit = new MemAudit();
    List<String> sent = new ArrayList<>();

    SupportReplyLoop loop = SupportApp.build(
        llm, orders(), policies(), recordingCrm(sent), acceptConsole(), audit);

    Reply reply = loop.run(
        new Ticket("48291", "Order arrived damaged", "cust-1"),
        new Context("agent-9", "email"));

    assertEquals("48291", reply.ticketId());
    assertTrue(reply.body().contains("POL-REFUND-PRO"));
    assertEquals("agent-9", reply.sentBy());
    assertEquals(1, sent.size());
    assertTrue(audit.decisions.contains("RETRY"));
    assertTrue(audit.decisions.contains("APPROVED"));
    assertTrue(audit.decisions.contains("SENT"));
    assertEquals(2, llm.callCount()); // triage unused LLM + 2 drafts? triage stubs without LLM call
  }

  @Test
  void exhaustsRetriesAndNeedsHuman() {
    StubLlm llm = new StubLlm(
        "We will refund you now with no policy id.",
        "Still a refund promise without citing POL-REFUND-PRO.",
        "Again a refund without the policy id.");
    MemAudit audit = new MemAudit();

    SupportReplyLoop loop = SupportApp.build(
        llm, orders(), policies(), recordingCrm(new ArrayList<>()), acceptConsole(), audit);

    NeedsHumanException ex = assertThrows(
        NeedsHumanException.class,
        () -> loop.run(new Ticket("48291", "damaged", "c1"), new Context("a", "email")));

    assertEquals("48291", ex.ticketId());
    assertNotNull(ex.draft());
    assertFalse(ex.critique().isBlank());
  }

  @Test
  void humanRejectDoesNotCallCrm() {
    StubLlm llm = new StubLlm("Quiet apology. See POL-REFUND-PRO.");
    MemAudit audit = new MemAudit();
    List<String> sent = new ArrayList<>();
    DeskConsole reject = new DeskConsole() {
      public String actorId() { return "agent-9"; }
      public boolean confirmSend(String ticketId, String body) { return false; }
    };

    // amount from triage is 12500; draft must not promise refund
    SupportReplyLoop loop = SupportApp.build(
        llm, orders(), policies(), recordingCrm(sent), reject, audit);

    assertThrows(
        NeedsHumanException.class,
        () -> loop.run(new Ticket("48291", "damaged", "c1"), new Context("a", "email")));
    assertTrue(sent.isEmpty());
    assertTrue(audit.decisions.contains("REJECTED"));
  }
}
