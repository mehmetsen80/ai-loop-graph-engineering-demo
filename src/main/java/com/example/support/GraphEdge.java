package com.example.support;

/**
 * One allowed edge in the agent graph: {@code from} may call {@code to} through {@code port}.
 *
 * <p>Edges are the Graph Engineering artifact — not comments on constructors.
 */
public record GraphEdge(GraphNode from, GraphNode to, String port) {

  public GraphEdge {
    if (from == null || to == null) {
      throw new IllegalArgumentException("from and to are required");
    }
    if (port == null || port.isBlank()) {
      throw new IllegalArgumentException("port is required");
    }
  }

  @Override
  public String toString() {
    return from + " → " + to + " (" + port + ")";
  }
}
