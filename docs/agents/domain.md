# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the codebase.

## Before exploring, read these

- **`GLOSSARY.md`** at the repo root
- **`docs/adr/`**: read ADRs that touch the area you're about to work in

If either doesn't exist, **proceed silently**. Don't flag their absence; don't suggest creating them upfront. The `/skill:domain-modeling` skill (reached via `/skill:grill-with-docs` and `/skill:improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

## File structure

This repo uses the single-context layout:

```
/
├── GLOSSARY.md
├── docs/adr/
│   ├── 0001-event-sourced-orders.md
│   └── 0002-postgres-for-write-model.md
└── src/
```

If this repo later grows genuinely separate contexts, add a root `GLOSSARY-MAP.md` pointing at one `GLOSSARY.md` per context, and check `src/<context>/docs/adr/` for context-scoped decisions alongside the system-wide `docs/adr/`.

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a test name), use the term as defined in `GLOSSARY.md`. Don't drift to synonyms the glossary explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal: either you're inventing language the project doesn't use (reconsider) or there's a real gap (note it for `/skill:domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0007 (event-sourced orders), but worth reopening because…_
