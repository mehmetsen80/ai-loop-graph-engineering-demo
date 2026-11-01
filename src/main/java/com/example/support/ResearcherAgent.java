package com.example.support;

public final class ResearcherAgent {

  private final OrderLookup orders;
  private final PolicySearch policies;
  private final AuditLog audit;

  public ResearcherAgent(
      OrderLookup orders,
      PolicySearch policies,
      AuditLog audit) {
    this.orders = orders;
    this.policies = policies;
    this.audit = audit;
  }

  public Evidence gather(Plan plan) {
    return gather(plan, null);
  }

  public Evidence gather(Plan plan, Context ctx) {
    Order order = orders.byTicket(plan.ticketId());
    Policy policy = policies.find(plan.intent(), order.planCode());
    audit.tool(plan.ticketId(), "orderLookup", order.id());
    audit.tool(plan.ticketId(), "policySearch", policy.id());
    return new Evidence(order, policy);
  }
}
