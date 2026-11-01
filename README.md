# AI Loop & Graph Engineering Demo

A small, runnable Java teaching project that shows how to engineer an **AI agent loop** as an explicit graph — not a single prompt.

It models a **customer-support reply** workflow:

**Plan → Act → Observe → Decide**

with a hard **human gate** before anything is sent to the customer.

The same patterns appear in the Deqra Document  
`.deqra/ai_loop_graph_engineering.deqra` (classic support-ticket example, not Deqra product code).

---

## Where is the **Loop**? Where is the **Graph**?

These are two different design layers. Both show up in this repo — in different files.

| Concept | Meaning here | **Where in this project** |
| :--- | :--- | :--- |
| **Loop** | The *time* cycle: draft → critique → revise until a gate says stop | **`SupportReplyLoop.run`** — the `for (attempt ≤ 3)` that feeds `critique` back into `drafter.write(...)` |
| **Graph** | The *structure*: named nodes (agents/tools/human) and allowed edges (who may call whom) | **`SupportApp.build`** wires the nodes; **constructor args + ports** are the edges. Drafter has **no** `CrmSend` edge — only `HumanGate` does |

A loop without a graph is one model thrashing on the ticket.  
A graph without a loop never improves the draft. This demo builds **both**.

### Loop — concrete code

| Loop piece | File | What to look for |
| :--- | :--- | :--- |
| Orchestrator / cycle | `SupportReplyLoop.java` | `MAX_TRIES = 3`; on critic failure, `critique = result.notes()` then next iteration |
| Feedback edge into Act | `SupportReplyLoop.java` → `drafter.write(plan, evidence, critique)` | Prior Observe notes become the next Act input — that *is* the loop |
| Stop conditions | same file | `result.isApproved()` → leave loop to HumanGate; else after 3 tries → `NeedsHumanException` |
| Runnable proof | `SupportMain.java` | Scripted drafts: attempt 1 `RETRY`, attempt 2 `APPROVED` (watch `[audit] decide`) |
| Tests | `SupportReplyLoopTest.java` | `retriesThenApprovesAndSends`, `exhaustsRetriesAndNeedsHuman` |

Plan → Act → Observe → Decide as **loop phases** inside one `run()`:

| Phase | Called from the loop | Class |
| :--- | :--- | :--- |
| Plan (once) | `triage.plan(ticket)` | `TriageAgent` |
| Act | `researcher.gather(...)` then `drafter.write(...)` | `ResearcherAgent`, `DrafterAgent` |
| Observe | `critic.review(draft, plan)` | `ReplyCritic` (+ `ToneCheck`) |
| Decide | `isApproved` → retry / `humanGate.requireSend` / escalate | `SupportReplyLoop` + `HumanGate` |

### Graph — concrete code

The graph is not a library framework. It is the **wiring** of one job per node and **restricted edges**.

| Graph piece | File | Role as a **node** |
| :--- | :--- | :--- |
| Triage | `TriageAgent.java` | Node: ticket → `Plan` |
| Researcher | `ResearcherAgent.java` | Node: plan → `Evidence` via tools |
| Drafter | `DrafterAgent.java` | Node: plan + evidence + critique → `Draft` (LLM text only) |
| Critic | `ReplyCritic.java` | Node: draft + plan → `CritiqueResult` |
| Human | `HumanGate.java` + `DeskConsole` | Node: approve Send |
| Tools | `OrderLookup`, `PolicySearch`, `CrmSend`, `LlmClient` | Tool nodes (ports) |
| Audit | `AuditLog.java` | Observability on node/tool/decision events |

| Graph **edge** (allowed call) | From → To | Enforced how |
| :--- | :--- | :--- |
| triage → plan | `SupportReplyLoop` → `TriageAgent` | only called once at start of `run` |
| researcher → order / policy tools | `ResearcherAgent` → `OrderLookup`, `PolicySearch` | constructor injection |
| drafter → LLM | `DrafterAgent` → `LlmClient` | constructor; **no** `CrmSend` in ctor |
| loop → critic | `SupportReplyLoop` → `ReplyCritic` | after every draft |
| critic → retry notes | Observe → next Act | `critique` string passed into `write` |
| human → CRM send | `HumanGate` → `CrmSend` | **only** `HumanGate` receives `CrmSend` in `SupportApp.build` |

Composition root (graph assembly):

```text
SupportApp.build(...)
  TriageAgent(llm)
  ResearcherAgent(orders, policies, audit)   // tools, not CRM
  DrafterAgent(llm)                          // text only — no CrmSend
  ReplyCritic(ToneCheck)
  HumanGate(crm, audit, console)             // sole CrmSend owner
  → SupportReplyLoop(triage, researcher, drafter, critic, humanGate, audit)
```

That last line is “run the **loop** over this **graph**.”

### One sentence each

- **Loop** = *when* work repeats (`SupportReplyLoop`’s retry until approve / escalate).
- **Graph** = *who* may talk to *whom* (`SupportApp` + agent constructors; especially Drafter ↛ CRM).

## What you get

| Piece | Role |
| :--- | :--- |
| `SupportReplyLoop` | Orchestrator: triage once, then draft ↔ critic up to 3 times, then human Send |
| Agents | `TriageAgent`, `ResearcherAgent`, `DrafterAgent`, `ReplyCritic` |
| Ports | `LlmClient`, `OrderLookup`, `PolicySearch`, `CrmSend`, `DeskConsole`, `AuditLog` |
| Gate | `HumanGate` — only place that may call `CrmSend` |
| Demo entry | `SupportMain` — scripted LLM stubs; audit trail via Lombok `@Slf4j` / SLF4J |
| Tests | 15 JUnit 5 tests covering critic rules, loop retry/send/reject |

No external LLM API key is required. The demo uses a **scripted `LlmClient`** so the path is deterministic.

---

## Requirements

- **JDK 21+** (`java -version`)
- **Maven 3.9+** (IntelliJ’s bundled Maven is fine)

```bash
java -version   # 21+
mvn -version
```

---

## Logging

Demo output uses **SLF4J** with Lombok’s `@Slf4j` annotation (not `System.out`).

- API: `org.slf4j:slf4j-api`
- Binding: Logback (`src/main/resources/logback.xml`)
- Annotation: `lombok.extern.slf4j.Slf4j` → static `log` field

Tune levels in `logback.xml` (`com.example.support` defaults to `INFO`).

## Quick start

```bash
cd /Users/mehmetsen/IdeaProjects/ai-loop-graph-engineering-demo

# Compile + unit tests
mvn test

# Run the end-to-end demo (scripted drafts)
mvn -q exec:java
```

### Expected demo output (shape)

```text
=== Support reply loop ===
ticket=48291 ...
[audit] node  TRIAGE — refund
[audit] tool  orderLookup → ord-48291
[audit] tool  policySearch → POL-REFUND-PRO
[llm] draft #1 → We will refund you immediately ...
[audit] decide RETRY (critic)          ← Observe failed
...
[llm] draft #2 → ... See POL-REFUND-PRO.
[audit] decide APPROVED (critic)
[desk] confirm Send for 48291? yes
[crm] send ticket=48291 ...
[audit] decide SENT (agent-desk-1)
=== SENT ===
```

Attempt 1 is rejected (promises a refund over $100 and does not cite policy).  
Attempt 2 is approved (cites `POL-REFUND-PRO`, offers replacement, no refund promise).  
Desk confirms → CRM send.

### IntelliJ

1. **File → Open** → this folder (Maven project).
2. Wait for indexing / JDK 21 project SDK.
3. Run `com.example.support.SupportMain`.
4. Or right-click `pom.xml` → Maven → `test` / `exec:java`.

---

## Combined picture (loop *over* the graph)

```text
                    ┌──────────────┐
 Ticket + Context → │ TriageAgent  │ → Plan (intent, policyId, amount)
                    └──────┬───────┘
                           │
              ┌────────────▼────────────┐
              │   for attempt ≤ 3       │
              │                         │
              │  ResearcherAgent        │ → Evidence (order, policy)
              │  DrafterAgent (+ notes) │ → Draft
              │  ReplyCritic            │ → CritiqueResult
              │                         │
              │  approved? ──yes──► HumanGate.requireSend → Reply
              │      │ no                               │
              │      └── notes → next attempt            │ desk rejects
              └────────────┬─────────────────────────────┘
                           │ exhausted
                           ▼
                  NeedsHumanException
```

| Step | Class | Job |
| :--- | :--- | :--- |
| **Plan** | `TriageAgent` | Turn the ticket into a `Plan` (teaching stub returns refund / `POL-REFUND-PRO` / $125) |
| **Act** | `ResearcherAgent` + `DrafterAgent` | Gather tools evidence; write reply text (LLM). Critic notes are fed back on retry |
| **Observe** | `ReplyCritic` + `ToneCheck` | Deterministic checks — not another free-form LLM call |
| **Decide** | loop + `HumanGate` | `RETRY` / `APPROVED` → desk confirm → `SENT` or `REJECTED` / escalate |

### Critic rules (`ReplyCritic`)

1. Draft body must **cite** `plan.policyId()`.
2. If the draft **promises a refund** and `amountCents > 10_000` ($100) → finance hold (do not promise Send).
3. Hostile tone (`your fault`, `you are wrong`, `deal with it`, blank) → soften.

`CritiqueResult.approved()` / `CritiqueResult.retry(notes)` / `isApproved()` — never mutate the `Draft`.

### Tool ownership (important)

- **Drafter never receives `CrmSend`.** It only produces text.
- **`HumanGate` is the only owner of `CrmSend`.** That is the graph edge that keeps “send” behind a human.

Refund detection strips the policy id from the draft before looking for the word `refund`, so citing `POL-REFUND-PRO` alone does not count as promising a refund.

---

## Package map

```text
com.example.support
├── SupportMain          # runnable demo
├── SupportApp           # composition root (wire ports → loop)
├── SupportReplyLoop     # Plan → Act → Observe → Decide
├── TriageAgent          # Plan
├── ResearcherAgent      # Act (tools)
├── DrafterAgent         # Act (LLM text)
├── ReplyCritic          # Observe
├── ToneCheck
├── HumanGate            # Decide / send
├── CritiqueResult       # approved | retry(notes)
├── NeedsHumanException  # max retries or desk reject
├── records              # Ticket, Context, Plan, Order, Policy,
│                        # Evidence, Draft, Reply
└── ports                # LlmClient, OrderLookup, PolicySearch,
                         # CrmSend, DeskConsole, AuditLog
```

---

## Running tests

```bash
mvn test
# or a single class:
mvn -Dtest=SupportReplyLoopTest test
```

| Test class | Covers |
| :--- | :--- |
| `CritiqueResultTest` | factory + blank retry rejected |
| `ToneCheckTest` | hostile / neutral / blank |
| `DraftTest` | policy citation |
| `ReplyCriticTest` | approve, missing cite, finance, tone |
| `HumanGateTest` | desk accept → CRM + `SENT` |
| `SupportReplyLoopTest` | retry→send, exhaust→human, desk reject (no CRM) |

All tests use in-memory stubs (no network).

---

## Maven coordinates

| | |
| :--- | :--- |
| Group / artifact | `com.example:ai-loop-graph-engineering-demo` |
| Version | `1.0.0-SNAPSHOT` |
| Java | 21 (`maven.compiler.release`) |
| Logging | SLF4J 2 + Logback + Lombok `@Slf4j` |
| Test | JUnit Jupiter 5.11 |
| Main class | `com.example.support.SupportMain` (`exec-maven-plugin`) |

Useful goals:

```bash
mvn clean test
mvn -q exec:java
mvn -q -DskipTests package
```

---

## Extending the demo

**Swap in a real LLM** — implement `LlmClient`:

```java
LlmClient llm = (system, user) -> yourVendor.chat(system, user);
```

Keep `ReplyCritic` deterministic; do not replace Observe with another open-ended model call unless you also keep hard policy checks.

**Fail the desk path** — in `SupportMain`, change `confirmSend` to return `false` and you should see `REJECTED` + `NeedsHumanException` with no CRM send.

**Force escalation** — feed three bad drafts from the scripted LLM; after `MAX_TRIES = 3` the loop throws `NeedsHumanException`.

---

## Design takeaways

1. **Loop lives in `SupportReplyLoop`** — the retry `for` + critique feedback is the cycle.
2. **Graph lives in `SupportApp` + constructors** — nodes and forbidden edges (Drafter ↛ CRM).
3. **Plan / Act / Observe / Decide** are phases of the loop, implemented as graph nodes.
4. **Observe can be code** — policy, finance, tone checks in `ReplyCritic`.
5. **Audit every node** — `AuditLog` (`RETRY`, `APPROVED`, `SENT`, `REJECTED`).
6. **Bound the loop** — then `NeedsHumanException` with last draft + critique.

---

## License / status

Teaching / scratch demo. Not a production support desk. Safe to delete or fork.
