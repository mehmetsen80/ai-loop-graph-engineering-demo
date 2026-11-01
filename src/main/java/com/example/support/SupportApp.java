package com.example.support;

public final class SupportApp {

  public static SupportReplyLoop build(
      LlmClient llm,
      OrderLookup orders,
      PolicySearch policies,
      CrmSend crm,
      DeskConsole console,
      AuditLog audit) {

    TriageAgent triage = new TriageAgent(llm);
    ResearcherAgent researcher =
        new ResearcherAgent(orders, policies, audit);
    DrafterAgent drafter = new DrafterAgent(llm);
    ReplyCritic critic = new ReplyCritic(new ToneCheck());
    HumanGate humanGate = new HumanGate(crm, audit, console);

    // Drafter never receives crm. HumanGate is the only CrmSend owner.
    return new SupportReplyLoop(
        triage, researcher, drafter, critic, humanGate, audit);
  }
}
