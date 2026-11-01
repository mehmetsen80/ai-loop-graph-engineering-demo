package com.example.support;

public final class HumanGate {

  private final CrmSend crm;
  private final AuditLog audit;
  private final DeskConsole console;

  public HumanGate(CrmSend crm, AuditLog audit, DeskConsole console) {
    this.crm = crm;
    this.audit = audit;
    this.console = console;
  }

  public Reply requireSend(Draft draft, Ticket ticket) {
    boolean ok = console.confirmSend(ticket.id(), draft.text());
    if (!ok) {
      audit.decision(ticket.id(), "REJECTED", console.actorId());
      throw new NeedsHumanException(
          ticket.id(), draft, "Desk agent rejected Send");
    }
    crm.sendReply(ticket.id(), draft.text(), console.actorId());
    audit.decision(ticket.id(), "SENT", console.actorId());
    return new Reply(ticket.id(), draft.text(), console.actorId());
  }
}

/** UI / CLI the desk agent uses to approve Send. */
