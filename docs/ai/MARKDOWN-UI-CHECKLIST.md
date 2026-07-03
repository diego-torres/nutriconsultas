# AI chat Markdown UI — manual checklist (#434)

Use after changes to `ai-markdown.js`, vendored `marked` / `DOMPurify`, or chat/widget templates.

## Full-page chat (`/admin/ai`)

1. Send a message that returns Markdown (lists, **bold**, `code`, fenced block).
2. Confirm assistant bubble renders formatting; user bubble stays plain text (no raw HTML).
3. Confirm Spanish punctuation (¿? ¡! — «») displays correctly.
4. Paste `<script>alert(1)</script>` in a **user** message — must show escaped text only.
5. If assistant returns a link, `https://` opens in new tab; `javascript:` links must not execute.

## Floating widget (patient / dieta / platillo page)

Repeat steps 1–5 on a page with the green robot widget.

## Fallback

1. Block `marked.min.js` in devtools → reload → assistant messages show plain text with line breaks (no script errors).

## Libraries (vendored)

| File | Purpose |
|------|---------|
| `static/sbadmin/vendor/marked/marked.min.js` | Markdown parse |
| `static/sbadmin/vendor/dompurify/purify.min.js` | XSS sanitize |
| `static/sbadmin/js/ai-markdown.js` | Shared wrapper + link hardening |
