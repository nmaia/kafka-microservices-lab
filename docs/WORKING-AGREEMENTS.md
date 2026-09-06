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
3. Commit both together, e.g. `git commit -m "docs: M<n> summary + checklist update"`