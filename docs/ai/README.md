# AI Assistant docs

Documentation for the **AI Nutrition Assistant** track.

| Doc | Purpose |
|-----|---------|
| [`TOOL-CONTRACT.md`](TOOL-CONTRACT.md) | Tool names, JSON schemas, read vs draft (#363) |
| [`DATA-ACCESS-RULES.md`](DATA-ACCESS-RULES.md) | PHI, scoping, OpenAI allow/deny lists, logging (#362) |
| [`FUNCTIONAL-SCOPE.md`](FUNCTIONAL-SCOPE.md) | Supported workflows, prompts, confirmation rules (#361) |
| `ai/system-prompt-base.txt` + `AiSystemPromptService` | Server system prompt (#367) |
| [`PROMPT-SECURITY.md`](PROMPT-SECURITY.md) | Input guardrails, delimiter wrap, injection block list (#439–#441) |
| [`BULK-SCOPE-GOLDEN-PROMPTS.md`](BULK-SCOPE-GOLDEN-PROMPTS.md) | Golden prompt evaluation for bulk scope (#450) |
| [`SECURITY-GOLDEN-PROMPTS.md`](SECURITY-GOLDEN-PROMPTS.md) | Golden prompt evaluation for defense-in-depth (#441) |
| [`NUTRITION-GOLDEN-PROMPTS.md`](NUTRITION-GOLDEN-PROMPTS.md) | Golden prompt evaluation for nutrition workflows (#401) |
| [`DRAFT-SCHEMA-VALIDATION.md`](DRAFT-SCHEMA-VALIDATION.md) | JSON Schema validation for draft tool arguments (#402) |
| [`E2E-DRAFT-FLOW-TEST.md`](E2E-DRAFT-FLOW-TEST.md) | End-to-end draft flow integration test (#403) |
| [`LOCAL-AI-SETUP.md`](LOCAL-AI-SETUP.md) | Local dev: `.env`, `AI_ENABLED`, OpenAI keys (#405) |
| [`PRODUCTION-AI-SETUP.md`](PRODUCTION-AI-SETUP.md) | Production EC2 `app.env`, SSM, rollback (#406) |
| [`NUTRITIONIST-USER-GUIDANCE.md`](NUTRITIONIST-USER-GUIDANCE.md) | Nutritionist-facing guidance — drafts, prompts, limits (#407) |
| [`AI-ASSISTANT-PLAN.md`](AI-ASSISTANT-PLAN.md) | Architecture, security, tools, milestones, definition of done |
| [`../../ISSUE-AI-ASSISTANT.md`](../../ISSUE-AI-ASSISTANT.md) | GitHub issue registry (#360–#408) |
| [`../../AI-ASSISTANT-WORKFLOW.md`](../../AI-ASSISTANT-WORKFLOW.md) | Agent implementation workflow |
