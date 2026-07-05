# End-to-end AI draft flow test (#403)

**Issue:** [#403](https://github.com/diego-torres/nutriconsultas/issues/403) · Epic [#400](https://github.com/diego-torres/nutriconsultas/issues/400)  
**Related:** [`NUTRITION-GOLDEN-PROMPTS.md`](NUTRITION-GOLDEN-PROMPTS.md) (#401) · [`DRAFT-SCHEMA-VALIDATION.md`](DRAFT-SCHEMA-VALIDATION.md) (#402)

Integration test that exercises the full draft pipeline with **real persistence and tool dispatch**; only OpenAI HTTP is mocked.

---

## Coverage

| Scenario | Assertion |
|----------|-----------|
| Chat message | User + assistant + tool audit rows persisted |
| `create_dish_draft` tool | `ai_generated_draft` row with `DRAFT` status |
| No auto-save | `platillo` count unchanged until accept |
| Discard | Status → `DISCARDED` |
| Accept | Materializes `platillo` for owning nutritionist |
| IDOR | Other nutritionist gets 404 / “Borrador no encontrado” |

---

## Test class

`AiDraftFlowIntegrationTest` — `@SpringBootTest` with H2, `nutriconsultas.ai.enabled=true`, scope classifier disabled, catalog `Alimento` seeded per test.

Run:

```bash
mvn test -Dtest=AiDraftFlowIntegrationTest
```

---

## Mocking strategy

OpenAI returns a two-step completion:

1. `tool_calls` → `create_dish_draft` with valid JSON (real catalog `alimentoId`)
2. Final assistant message in Spanish

All other layers (`AiOrchestrationService`, `AiOrchestrationToolDispatcher`, schema validation, `CreateDishDraftToolService`, `AiDraftLifecycleService`) run without mocks.
