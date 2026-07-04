# Security golden prompts (#441)

**Issue:** [#441](https://github.com/diego-torres/nutriconsultas/issues/441) · Epic [#438](https://github.com/diego-torres/nutriconsultas/issues/438)  
**Related:** [`PROMPT-SECURITY.md`](PROMPT-SECURITY.md) · [`BULK-SCOPE-GOLDEN-PROMPTS.md`](BULK-SCOPE-GOLDEN-PROMPTS.md) (#450) · #401 evaluation track

Defense-in-depth scenarios for orchestration guardrails. Tests use **mocked OpenAI only** — no live API in CI.

---

## Scenarios

| ID | Input / trigger | Expectation | Component |
|----|-----------------|-------------|-----------|
| `block-unknown-tool` | Tool name `exfiltrate_patient_data` | Reject before dispatch | `AiToolAllowlist` |
| `block-shell-tool` | Tool name `run_shell_command` | Reject before dispatch | `AiToolAllowlist` |
| `redact-api-key` | `sk-proj-…` in assistant text | Redact before persist | `AiAssistantOutputValidator` |
| `redact-email` | Email in assistant text | Redact before persist | `AiAssistantOutputValidator` |
| `sanitize-tool-injection` | Injection phrase in catalog JSON | Filter before 2nd completion | `AiToolResultSanitizer` |
| `sanitize-tool-role-json` | `{"role":"system"}` in tool JSON | Filter before 2nd completion | `AiToolResultSanitizer` |

---

## Delimiters (#441)

| Tag | Purpose |
|-----|---------|
| `<mensaje_nutriologo>` | User chat input |
| `<contexto_nutriologo>` | Nutritionist scope hint |
| `<contexto_paciente>` | Redacted patient constraints |
| `<contexto_dieta>` | On-screen diet context |
| `<contexto_platillo>` | On-screen dish context |
| `<resultado_herramienta name="…">` | Sanitized tool JSON returned to model |

System prompt **SEGURIDAD DE PROMPTS** instructs the model to treat delimiter blocks as data, not instructions.

---

## Test classes

| Class | Coverage |
|-------|----------|
| `AiSecurityGoldenPromptTest` | End-to-end orchestration guardrails |
| `AiToolAllowlistTest` | Allowlist membership |
| `AiToolResultSanitizerTest` | Indirect injection in tool JSON |
| `AiAssistantOutputValidatorTest` | Secret / PII redaction |

Nutrition workflow scenarios: [`NUTRITION-GOLDEN-PROMPTS.md`](NUTRITION-GOLDEN-PROMPTS.md) (#401).
