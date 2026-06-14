# Mobile API contract docs

Canonical cross-repo contracts for the `[Mobile API]` track, vendored into this repo so [`../../AGENT-WORKFLOW.md`](../../AGENT-WORKFLOW.md) and [`../../ISSUE.md`](../../ISSUE.md) are self-contained.

| File | What it is |
|------|-----------|
| `ALIGNMENT-SPEC.md` | Source-of-truth contract for all agents. §F8 = backend↔mobile field/enum map. Phase 0 + endpoints #91–#98, #111, #113 **done on `main`** (PRs #117, #142–#151). **#99 in progress.** |
| `mobile-api-roadmap-v2.md` | Per-endpoint (#91–#99) request/response JSON and field mappings. **#99 measurements** in progress. |

**Provenance / drift:** these are synced copies of the workspace-root originals
(`/Users/joelmartinez/Documents/Work/ALIGNMENT-SPEC.md` and `mobile-api-roadmap-v2.md`),
which the mobile repo also reads. If the originals change, re-copy them here.
The mobile consumer registry lives at `Escanor4323/nutriconsultas-mobile` → `ISSUE.md`.
