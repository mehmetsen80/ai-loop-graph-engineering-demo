package com.example.support;

import java.util.List;
import java.util.Objects;

/**
 * The Graph for one support-reply job: wired nodes plus the allowed edges (who may call whom).
 *
 * <p>This is Graph Engineering in code — not a metaphor over plain DI. {@link #edges()} is the
 * structure; {@link #loop()} runs the Plan → Act → Observe cycle over that structure. Forbidden
 * edges (especially Drafter → CrmSend) fail at wire time.
 */
public final class SupportGraph {

  private final TriageAgent triage;
  private final ResearcherAgent researcher;
  private final DrafterAgent drafter;
  private final ReplyCritic critic;
  private final HumanGate humanGate;
  private final AuditLog audit;
  private final List<GraphEdge> edges;

  private SupportGraph(
      TriageAgent triage,
      ResearcherAgent researcher,
      DrafterAgent drafter,
      ReplyCritic critic,
      HumanGate humanGate,
      AuditLog audit,
      List<GraphEdge> edges) {
    this.triage = Objects.requireNonNull(triage);
    this.researcher = Objects.requireNonNull(researcher);
    this.drafter = Objects.requireNonNull(drafter);
    this.critic = Objects.requireNonNull(critic);
    this.humanGate = Objects.requireNonNull(humanGate);
    this.audit = Objects.requireNonNull(audit);
    this.edges = List.copyOf(edges);
    forbid(GraphNode.DRAFTER, GraphNode.CRM_SEND);
  }

  /**
   * Wire nodes and declare every allowed edge. Construction rejects Drafter → CrmSend even if a
   * future edit accidentally injects {@link CrmSend} into {@link DrafterAgent}.
   */
  public static SupportGraph wire(
      LlmClient llm,
      OrderLookup orders,
      PolicySearch policies,
      CrmSend crm,
      DeskConsole console,
      AuditLog audit) {

    TriageAgent triage = new TriageAgent(llm);
    ResearcherAgent researcher = new ResearcherAgent(orders, policies, audit);
    DrafterAgent drafter = new DrafterAgent(llm);
    ReplyCritic critic = new ReplyCritic(new ToneCheck());
    HumanGate humanGate = new HumanGate(crm, audit, console);

    List<GraphEdge> edges = List.of(
        new GraphEdge(GraphNode.TRIAGE, GraphNode.LLM, "LlmClient"),
        new GraphEdge(GraphNode.RESEARCHER, GraphNode.ORDER_LOOKUP, "OrderLookup"),
        new GraphEdge(GraphNode.RESEARCHER, GraphNode.POLICY_SEARCH, "PolicySearch"),
        new GraphEdge(GraphNode.RESEARCHER, GraphNode.AUDIT, "AuditLog"),
        new GraphEdge(GraphNode.DRAFTER, GraphNode.LLM, "LlmClient"),
        // TODO(llm-critic): also declare CRITIC → LLM when ReplyCritic gains an optional LlmClient.
        new GraphEdge(GraphNode.CRITIC, GraphNode.TONE_CHECK, "ToneCheck"),
        new GraphEdge(GraphNode.HUMAN_GATE, GraphNode.CRM_SEND, "CrmSend"),
        new GraphEdge(GraphNode.HUMAN_GATE, GraphNode.AUDIT, "AuditLog"),
        new GraphEdge(GraphNode.HUMAN_GATE, GraphNode.DESK_CONSOLE, "DeskConsole"));

    return new SupportGraph(triage, researcher, drafter, critic, humanGate, audit, edges);
  }

  /** Allowed edges — the Graph structure. */
  public List<GraphEdge> edges() {
    return edges;
  }

  public boolean allows(GraphNode from, GraphNode to) {
    return edges.stream().anyMatch(e -> e.from() == from && e.to() == to);
  }

  /** Run the Loop over this Graph. */
  public SupportReplyLoop loop() {
    return new SupportReplyLoop(triage, researcher, drafter, critic, humanGate, audit);
  }

  TriageAgent triage() {
    return triage;
  }

  ResearcherAgent researcher() {
    return researcher;
  }

  DrafterAgent drafter() {
    return drafter;
  }

  ReplyCritic critic() {
    return critic;
  }

  HumanGate humanGate() {
    return humanGate;
  }

  AuditLog audit() {
    return audit;
  }

  private void forbid(GraphNode from, GraphNode to) {
    if (allows(from, to)) {
      throw new IllegalStateException(
          "Forbidden graph edge: " + from + " → " + to
              + ". Only HUMAN_GATE may own CrmSend.");
    }
  }
}
