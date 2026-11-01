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
| **Loop** | The *time* cycle: Act → Observe → retry until a gate says stop | **`SupportReplyLoop.run`** — `for (attempt ≤ 3)` re-enters **Act** (`gather` + `write`) with critique notes |
| **Graph** | The *structure*: named nodes and allowed edges (who may call whom) | **`SupportGraph`** — `GraphNode` + `GraphEdge` list from `SupportGraph.wire(...)`. Drafter has **no** `CrmSend` edge — only `HumanGate` does. `SupportApp.graph` / `.build` are the composition root |

A loop without a graph is one model thrashing on the ticket.  
A graph without a loop never improves the draft. This demo builds **both**.

### Loop — concrete code

| Loop piece | File | What to look for |
| :--- | :--- | :--- |
| Orchestrator / cycle | `SupportReplyLoop.java` | `MAX_TRIES = 3`; on critic failure, `critique = result.notes()` then next iteration |
| Feedback edge into Act | `SupportReplyLoop.java` loop body | Retry re-runs **both** `researcher.gather` and `drafter.write(..., critique)` — Act ≠ Drafter alone |
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

The Graph is a **first-class type**, not a README metaphor over DI.

| Graph piece | File | Role |
| :--- | :--- | :--- |
| Node ids | `GraphNode.java` | Enum of agent / tool / human nodes |
| Edges | `GraphEdge.java` | `from → to (port)` — the allowed calls |
| Graph | `SupportGraph.java` | Wires instances + declares `edges()`; **forbids** `DRAFTER → CRM_SEND` at wire time |
| Composition root | `SupportApp.java` | `graph(...)` → `SupportGraph`; `build(...)` → `graph(...).loop()` |
| Tests | `SupportGraphTest.java` | Asserts edges exist and Drafter has no CrmSend edge |

| Graph **edge** (allowed call) | From → To | Declared in |
| :--- | :--- | :--- |
| Triage → LLM | `TRIAGE → LLM` | `SupportGraph.wire` |
| Researcher → tools | `RESEARCHER → ORDER_LOOKUP / POLICY_SEARCH` | same |
| Drafter → LLM | `DRAFTER → LLM` | same — **no** `DRAFTER → CRM_SEND` |
| Critic → ToneCheck | `CRITIC → TONE_CHECK` | same |
| Human → CRM | `HUMAN_GATE → CRM_SEND` | same — sole CrmSend owner |

Assembly:

```text
SupportGraph.wire(...)
  → nodes: Triage / Researcher / Drafter / Critic / HumanGate
  → edges: List<GraphEdge>   // inspectable Graph structure
  → forbid DRAFTER → CRM_SEND
  → .loop() → SupportReplyLoop(...)   // Loop over this Graph
```

### One sentence each

- **Loop** = *when* work repeats (`SupportReplyLoop`’s retry until approve / escalate).
- **Graph** = *who* may talk to *whom* (`SupportGraph` + `GraphEdge`; especially Drafter ↛ CRM).

## What you get

| Piece | Role |
| :--- | :--- |
| `SupportGraph` | Graph Engineering: `GraphNode` + `GraphEdge` + wire-time forbid Drafter → CrmSend |
| `SupportReplyLoop` | Loop Engineering: triage once, then Act (tools + draft) ↔ Observe up to 3 times, then human Send |
| Agents | Named graph nodes with scoped ports — **not** “must call an LLM” (see below) |
| Ports | `LlmClient`, `OrderLookup`, `PolicySearch`, `CrmSend`, `DeskConsole`, `AuditLog` |
| Gate | `HumanGate` — only place that may call `CrmSend` |
| Demo entry | `SupportMain` — prints graph edges, then runs the loop; scripted LLM stubs |
| Tests | JUnit 5 — critic rules, loop retry/send/reject, **graph edge rules** |

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
=== Support reply Graph ===
[graph] TRIAGE → LLM (LlmClient)
[graph] RESEARCHER → ORDER_LOOKUP (OrderLookup)
[graph] RESEARCHER → POLICY_SEARCH (PolicySearch)
...
[graph] HUMAN_GATE → CRM_SEND (CrmSend)
=== Support reply Loop ===
ticket=48291 ...
[audit] node  TRIAGE — refund
[audit] tool  orderLookup → ord-48291
[audit] tool  policySearch → POL-REFUND-PRO
[llm] draft #1 → We will refund you immediately ...
[audit] decide RETRY (critic)          ← Observe failed; next Act re-gathers + rewrites
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
| **Act** | `ResearcherAgent` + `DrafterAgent` | Tools evidence + LLM draft. On **Retry**, both run again; critique notes feed `drafter.write` (Researcher still gathers from the same `Plan`) |
| **Observe** | `ReplyCritic` + `ToneCheck` | Deterministic checks — not another free-form LLM call. **TODO(llm-critic):** optional `LlmClient` behind the same `CritiqueResult` contract; keep hard gates in code; add `CRITIC → LLM` edge |
| **Decide** | loop + `HumanGate` | `RETRY` / `APPROVED` → desk confirm → `SENT` or `REJECTED` / escalate |

### Agent ≠ “has an LLM”

In this demo an **agent** is a named graph node with one job and restricted ports. LLM use is optional per node:

| Node | LLM? | Ports |
| :--- | :--- | :--- |
| `TriageAgent` | optional (`LlmClient`; stub skips the call) | Plan only |
| `ResearcherAgent` | **no** | `OrderLookup`, `PolicySearch`, `AuditLog` |
| `DrafterAgent` | **yes** | `LlmClient` only — **no** `CrmSend` |
| `ReplyCritic` | **no** (TODO optional) | `ToneCheck` (+ hard policy/finance rules in code) |

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
├── SupportMain          # prints SupportGraph.edges(), then runs the loop
├── SupportApp           # composition root → SupportGraph / .loop()
├── SupportGraph         # Graph Engineering: wire + edges() + forbid Drafter→CrmSend
├── GraphNode            # named nodes (agents / tools / human)
├── GraphEdge            # from → to (port)
├── SupportReplyLoop     # Plan → Act → Observe → Decide
├── TriageAgent          # Plan (optional LLM)
├── ResearcherAgent      # Act · tools (no LLM)
├── DrafterAgent         # Act · LLM text
├── ReplyCritic          # Observe (deterministic; TODO optional LLM)
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
| `SupportGraphTest` | edges declared; Drafter ↛ CrmSend; `.loop()` builds |

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

Keep `ReplyCritic` deterministic by default. **TODO(llm-critic):** an optional LLM Observe path is fine if it still returns `CritiqueResult` and hard policy/finance gates stay in code — then declare `CRITIC → LLM` in `SupportGraph.wire`.

**Fail the desk path** — in `SupportMain`, change `confirmSend` to return `false` and you should see `REJECTED` + `NeedsHumanException` with no CRM send.

**Force escalation** — feed three bad drafts from the scripted LLM; after `MAX_TRIES = 3` the loop throws `NeedsHumanException`.

---

## Design takeaways

1. **Loop lives in `SupportReplyLoop`** — the retry `for` re-enters Act (tools + draft) with critique notes.
2. **Graph lives in `SupportGraph`** — `GraphNode` + `GraphEdge`; `SupportApp` is only the composition root.
3. **Plan / Act / Observe / Decide** are loop phases; Act = Researcher + Drafter, not Drafter alone.
4. **Agent ≠ LLM** — Researcher (and today’s Critic) are agents/nodes without model calls.
5. **Observe can be code** — policy, finance, tone in `ReplyCritic` (optional LLM is a TODO).
6. **Audit every node** — `AuditLog` (`RETRY`, `APPROVED`, `SENT`, `REJECTED`).
7. **Bound the loop** — then `NeedsHumanException` with last draft + critique.

---

## License / status

Teaching / scratch demo. Not a production support desk. Safe to delete or fork.
