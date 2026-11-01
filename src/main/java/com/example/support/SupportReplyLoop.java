package com.example.support;

public final class SupportReplyLoop {

  private static final int MAX_TRIES = 3;

  private final TriageAgent triage;
  private final ResearcherAgent researcher;
  private final DrafterAgent drafter;
  private final ReplyCritic critic;
  private final HumanGate humanGate;
  private final AuditLog audit;

  public SupportReplyLoop(
      TriageAgent triage,
      ResearcherAgent researcher,
      DrafterAgent drafter,
      ReplyCritic critic,
      HumanGate humanGate,
      AuditLog audit) {
    this.triage = triage;
    this.researcher = researcher;
    this.drafter = drafter;
    this.critic = critic;
    this.humanGate = humanGate;
    this.audit = audit;
  }

  public Reply run(Ticket ticket, Context ctx) {
    Plan plan = triage.plan(ticket);
    audit.node(ticket.id(), "TRIAGE", plan.intent());

    String critique = "";
    Draft draft = null;

    for (int attempt = 1; attempt <= MAX_TRIES; attempt++) {
      Evidence evidence = researcher.gather(plan, ctx);
      draft = drafter.write(plan, evidence, critique);
      audit.node(ticket.id(), "DRAFT", "attempt=" + attempt);

      CritiqueResult result = critic.review(draft, plan);
      if (result.isApproved()) {
        audit.decision(ticket.id(), "APPROVED", "critic");
        return humanGate.requireSend(draft, ticket);
      }
      critique = result.notes();
      audit.decision(ticket.id(), "RETRY", "critic");
    }

    throw new NeedsHumanException(ticket.id(), draft, critique);
  }
}
