package com.example.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SupportGraphTest {

  private static SupportGraph sample() {
    return SupportGraph.wire(
        (system, user) -> "ok",
        ticketId -> new Order("ord-1", "PRO", 12_500),
        (intent, planCode) -> new Policy("POL-REFUND-PRO", "text"),
        (ticketId, body, actorId) -> {},
        new DeskConsole() {
          public String actorId() { return "desk"; }
          public boolean confirmSend(String ticketId, String body) { return true; }
        },
        new AuditLog() {
          public void node(String ticketId, String node, String detail) {}
          public void tool(String ticketId, String tool, String ref) {}
          public void decision(String ticketId, String decision, String actorId) {}
        });
  }

  @Test
  void declaresConstructorEdgesAsGraphStructure() {
    SupportGraph graph = sample();
    List<GraphEdge> edges = graph.edges();

    assertFalse(edges.isEmpty(), "Graph must declare edges — not only wire constructors");
    assertTrue(graph.allows(GraphNode.DRAFTER, GraphNode.LLM));
    assertTrue(graph.allows(GraphNode.RESEARCHER, GraphNode.ORDER_LOOKUP));
    assertTrue(graph.allows(GraphNode.HUMAN_GATE, GraphNode.CRM_SEND));
  }

  @Test
  void forbidsDrafterToCrmSend() {
    SupportGraph graph = sample();

    assertFalse(
        graph.allows(GraphNode.DRAFTER, GraphNode.CRM_SEND),
        "Drafter must not have a CrmSend edge — that is the Graph rule");
    assertTrue(
        edgesContainOnlyHumanGateCrm(graph.edges()),
        "CrmSend edge must belong to HUMAN_GATE only");
  }

  @Test
  void loopRunsOverTheSameGraph() {
    assertNotNull(sample().loop());
  }

  private static boolean edgesContainOnlyHumanGateCrm(List<GraphEdge> edges) {
    return edges.stream()
        .filter(e -> e.to() == GraphNode.CRM_SEND)
        .allMatch(e -> e.from() == GraphNode.HUMAN_GATE);
  }
}
