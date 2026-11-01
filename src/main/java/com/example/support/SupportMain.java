package com.example.support;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;

/**
 * Runnable demo of Plan → Act → Observe → Decide for one support ticket.
 *
 * <pre>
 *   mvn -q exec:java
 * </pre>
 */
@Slf4j
public final class SupportMain {

  private SupportMain() {}

  public static void main(String[] args) {
    AtomicInteger llmCalls = new AtomicInteger();
    List<String> scripts = List.of(
        // Attempt 1 — critic rejects (refund promise, no policy cite)
        "We will refund you immediately for the damaged order.",
        // Attempt 2 — critic approves (cites policy, no refund promise)
        "Sorry about the damage. We can ship a replacement. See POL-REFUND-PRO.");

    LlmClient llm = (system, user) -> {
      int i = Math.min(llmCalls.getAndIncrement(), scripts.size() - 1);
      String out = scripts.get(i);
      log.info("[llm] draft #{} → {}", i + 1, out);
      return out;
    };

    OrderLookup orders = ticketId -> new Order("ord-48291", "PRO", 12_500);
    PolicySearch policies = (intent, planCode) ->
        new Policy("POL-REFUND-PRO", "Refunds over $100 need finance; offer replacement.");

    CrmSend crm = (ticketId, body, actorId) ->
        log.info("[crm] send ticket={} by={} body={}", ticketId, actorId, body);

    DeskConsole console = new DeskConsole() {
      @Override
      public String actorId() {
        return "agent-desk-1";
      }

      @Override
      public boolean confirmSend(String ticketId, String body) {
        log.info("[desk] confirm Send for {}? yes", ticketId);
        return true;
      }
    };

    AuditLog audit = new AuditLog() {
      @Override
      public void node(String ticketId, String node, String detail) {
        log.info("[audit] node  {} — {}", node, detail);
      }

      @Override
      public void tool(String ticketId, String tool, String ref) {
        log.info("[audit] tool  {} → {}", tool, ref);
      }

      @Override
      public void decision(String ticketId, String decision, String actorId) {
        log.info("[audit] decide {} ({})", decision, actorId);
      }
    };

    SupportGraph graph =
        SupportApp.graph(llm, orders, policies, crm, console, audit);
    SupportReplyLoop loop = graph.loop();

    Ticket ticket = new Ticket(
        "48291",
        "My order arrived damaged — please fix this.",
        "cust-77");
    Context ctx = new Context("agent-desk-1", "email");

    log.info("=== Support reply Graph ===");
    graph.edges().forEach(e -> log.info("[graph] {}", e));
    log.info("=== Support reply Loop ===");
    log.info("ticket={} body={}", ticket.id(), ticket.body());
    try {
      Reply reply = loop.run(ticket, ctx);
      log.info("=== SENT ===");
      log.info("by={}", reply.sentBy());
      log.info("body={}", reply.body());
    } catch (NeedsHumanException e) {
      log.warn("=== NEEDS HUMAN ===");
      log.warn("ticket={}", e.ticketId());
      log.warn("critique={}", e.critique());
      if (e.draft() != null) {
        log.warn("lastDraft={}", e.draft().text());
      }
      System.exit(1);
    }
  }
}
