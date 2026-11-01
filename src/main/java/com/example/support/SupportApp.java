package com.example.support;

/**
 * Composition root. Builds the {@link SupportGraph} (Graph Engineering), then a
 * {@link SupportReplyLoop} that runs over it (Loop Engineering).
 */
public final class SupportApp {

  private SupportApp() {}

  /** Wire the Graph, then return a Loop that runs on those nodes/edges. */
  public static SupportReplyLoop build(
      LlmClient llm,
      OrderLookup orders,
      PolicySearch policies,
      CrmSend crm,
      DeskConsole console,
      AuditLog audit) {
    return graph(llm, orders, policies, crm, console, audit).loop();
  }

  /** Graph only — inspect {@link SupportGraph#edges()} without running the Loop. */
  public static SupportGraph graph(
      LlmClient llm,
      OrderLookup orders,
      PolicySearch policies,
      CrmSend crm,
      DeskConsole console,
      AuditLog audit) {
    return SupportGraph.wire(llm, orders, policies, crm, console, audit);
  }
}
