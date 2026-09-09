# Working Agreements

How this project gets built, session to session — not what's been built (see
`CONTEXT-CHECKLIST.md` for that).

## Pairing style
- Work step-by-step: one concept or one step explained at a time.
- The developer types and runs every command themselves, in their
  own terminal/IDE — not handed finished files to drop in, except for M0's
  initial scaffold (a deliberate one-time exception, agreed before M1 started).
- Move to the next step only once the current one is confirmed working
  (paste output/screenshots), not before.
- When there's a genuine design decision (not just a mechanical step), it's
  explained and discussed before being implemented — the developer weighs in,
  not just approves.

## Complexity analysis & rationale comments
When implementing business logic (starting M2), analyze and comment Big O
complexity **selectively, not universally**:

- **Do it** for any method with a genuine algorithmic choice — loops over
  collections, nested iteration, recursion, or a choice between data
  structures with different lookup/insert complexity. State the actual
  complexity and *why*, reasoning as if this were a real production
  scenario even though current sandbox volumes are tiny — that's the
  point: build the habit before scale forces it. If a worse complexity is
  being deliberately accepted (e.g., simplicity over performance at this
  stage), say so explicitly in the comment.
- **Skip it** for trivial code — field access, simple conditionals,
  delegating to a well-known library call, straightforward DTO
  construction. A Big O comment on something O(1) and obviously so is
  noise, not documentation.
- **Cyclomatic complexity is a separate concern from Big O — assess both
  while implementing, don't conflate them.** Big O measures how
  runtime/space scales with input size (loops, recursion, data structure
  choice). Cyclomatic complexity measures how many independent branches
  exist through a method's control flow (`if`/`else`/`switch`/`&&`/`||`/
  loops) — it's about testability and cognitive load, not scale. As a
  method is being written, if it's accumulating several independent
  branches, say so as part of the same design discussion used for Big O —
  roughly how many paths through it, and whether that's reasonable for
  what the method does. A method can be O(1) and still hard to test
  exhaustively if it has many branches; a method can have almost no
  branching and still be O(n²) — call out both where relevant, not just
  one. When branching is high enough that reasoning about every path (or
  writing a test per path) becomes genuinely hard, that's a signal to
  refactor (extract methods, replace conditionals with
  polymorphism/a lookup table) — independent of whatever its Big O
  already is.
- This is discussed as part of the normal step-by-step pairing process
  (per the pairing style above), not bolted on after the fact — complexity
  is part of the design decision, not a lint pass at the end.

## Documentation, after every milestone
Once a milestone's Definition of Done (per `implementation-roadmap.md`) is
verified, before moving to the next milestone:

1. Create `docs/milestones/M<n>-<short-name>.md` containing:
    - **Goal** — one line, from the roadmap
    - **What was built** — concrete artifacts (files, services, configs)
    - **Decisions & trade-offs** — real alternatives considered and why one
      was chosen, not just what was done
    - **Verification** — how the Definition of Done was actually confirmed
    - **Commands reference** — every command run, in order, runnable as a
      reference later
    - **Troubleshooting log** — every real error hit and how it was
      diagnosed/fixed, not just the happy path
2. Update `docs/CONTEXT-CHECKLIST.md`:
    - Mark the milestone done in the status table
    - Update the "Next up" line
    - Add any newly-discovered gotchas to the "Known gotchas" section
3. Commit both together, e.g. `git commit -m "docs: M<n> summary + checklist update"`.

**If the current milestone changes something from an earlier one** (a port's shape, a domain contract, 
behavior documented in a prior milestone doc) — this is allowed, sometimes necessary, and should never be silent. 
Discuss why before changing it (per the pairing style above), then update that **earlier** milestone's `docs/milestones/M<n>-*.md` 
to reflect what changed and why, rather than only documenting it in the current milestone's doc. See `implementation-roadmap.md`'s 
opening note for what's expected to stay stable (ports/contracts) versus what's expected to evolve (implementations behind them).

## Using Claude Code (or any AI assistant) on this repo
- Claude Code reads project files directly, but doesn't automatically
  prioritize `docs/` over raw code state when reasoning about status — it can
  infer "what milestone are we on" from code artifacts alone and get it
  wrong (e.g., missing a verification step that's recorded in a milestone
  doc but not evident from the code itself).
- **Start a new Claude Code session by asking it to read
  `docs/CONTEXT-CHECKLIST.md` first**, rather than assuming it'll find its
  way there. Once it has read the relevant docs, it reasons from them
  correctly — the gap is in reaching for them unprompted, not in
  understanding them.

## Claude Code permission mode
Claude Code's "Auto" mode (default as of August 2026 on Pro/Max/Team) lets
it run commands and edit files without asking first — this conflicts with
the pairing style above. Always use **Manual mode** (`Shift+Tab` to cycle,
or set `defaultMode: "default"` in `~/.claude/settings.json` so new
sessions start there automatically).