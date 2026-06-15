# Mobile API contract docs

Canonical cross-repo contracts for the `[Mobile API]` track, vendored into this repo so [`../../AGENT-WORKFLOW.md`](../../AGENT-WORKFLOW.md) and [`../../ISSUE.md`](../../ISSUE.md) are self-contained.

| File | What it is |
|------|-----------|
| `ALIGNMENT-SPEC.md` | Source-of-truth contract for all agents. §F8 = backend↔mobile field/enum map. Phase 0 + endpoints **#91–#99 done** on `main`. **OpenAPI #112 done** — `docs/api/openapi-mobile.yaml` ([PR #164](https://github.com/diego-torres/nutriconsultas/pull/164)). **PHI audit #115** — `docs/mobile-api/PHI-LOGGING-AUDIT.md`. **NEXT:** #116. Invitation **#132–#141** deferred (see §F8.6). |
| `mobile-api-roadmap-v2.md` | Per-endpoint (#91–#99) request/response JSON and field mappings. All endpoints **done**; OpenAPI **#112 done**; PHI audit **#115**; **NEXT:** #116. |
| `PHI-LOGGING-AUDIT.md` | Completed PHI logging audit checklist for `/rest/mobile/**` (#115). |

**Last synced:** 2026-06-15 (#115 PHI audit done, PR #168; **NEXT:** #116).

**Provenance / drift:** these are synced copies of the workspace-root originals
(`/Users/joelmartinez/Documents/Work/ALIGNMENT-SPEC.md` and `mobile-api-roadmap-v2.md`),
which the mobile repo also reads. If the originals change, re-copy them here.
The mobile consumer registry lives at `Escanor4323/nutriconsultas-mobile` → `ISSUE.md`.
