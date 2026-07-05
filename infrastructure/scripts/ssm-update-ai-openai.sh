#!/usr/bin/env bash
# Update AI assistant OpenAI env vars in /opt/nutriconsultas/app.env on the app EC2, then restart.
#
# Usage (enable):
#   export AWS_PROFILE=minutriporcion AWS_DEFAULT_REGION=us-east-1
#   export AI_ENABLED=true
#   export OPENAI_API_KEY='sk-proj-...'
#   export OPENAI_MODEL='gpt-5-mini'
#   ./ssm-update-ai-openai.sh [project]
#
# Usage (disable — key/model may stay in app.env):
#   export AI_ENABLED=false
#   ./ssm-update-ai-openai.sh
#
# Optional: OPENAI_STORE, AI_CHAT_MESSAGE_RATE_LIMIT, AI_CHAT_MESSAGE_RATE_WINDOW, AI_MAX_TOOL_CALLS
#
# See docs/ai/PRODUCTION-AI-SETUP.md (#406).
set -euo pipefail

PROJECT="${1:-nutriconsultas}"
P="/${PROJECT}/deploy"
: "${AWS_REGION:-${AWS_DEFAULT_REGION:?Set AWS_DEFAULT_REGION (or use configure)}}"
: "${AI_ENABLED:?Set AI_ENABLED (true or false)}"

if [ "$AI_ENABLED" = "true" ]; then
  : "${OPENAI_API_KEY:?Set OPENAI_API_KEY when AI_ENABLED=true}"
  : "${OPENAI_MODEL:?Set OPENAI_MODEL when AI_ENABLED=true}"
fi

INSTANCE_ID="$(aws ssm get-parameter --name "$P/app_instance_id" --query "Parameter.Value" --output text)"

b64() { printf '%s' "$1" | base64 | tr -d '\n'; }

KEY_B64=""
if [ -n "${OPENAI_API_KEY:-}" ]; then
  KEY_B64="$(b64 "$OPENAI_API_KEY")"
fi

PARAMS="$(jq -n \
  --arg enabled "$AI_ENABLED" \
  --arg model "${OPENAI_MODEL:-}" \
  --arg keyB64 "$KEY_B64" \
  --arg store "${OPENAI_STORE:-}" \
  --arg rateLimit "${AI_CHAT_MESSAGE_RATE_LIMIT:-}" \
  --arg rateWindow "${AI_CHAT_MESSAGE_RATE_WINDOW:-}" \
  --arg maxTools "${AI_MAX_TOOL_CALLS:-}" \
  'def upsertPlain($key; $val):
     [
       ("grep -v \"^" + $key + "=\" \"$ENV_FILE\" > \"${ENV_FILE}.tmp\" || true"),
       ("echo " + $key + "=" + $val + " >> \"${ENV_FILE}.tmp\""),
       "mv \"${ENV_FILE}.tmp\" \"$ENV_FILE\""
     ];
   def upsertSecret($key; $valB64):
     if ($valB64 | length) == 0 then [] else
     [
       ("grep -v \"^" + $key + "=\" \"$ENV_FILE\" > \"${ENV_FILE}.tmp\" || true"),
       ("echo -n " + $key + "= >> \"${ENV_FILE}.tmp\""),
       ("printf %s \"" + $valB64 + "\" | base64 -d >> \"${ENV_FILE}.tmp\""),
       "echo >> \"${ENV_FILE}.tmp\"",
       "mv \"${ENV_FILE}.tmp\" \"$ENV_FILE\""
     ] end;
   def upsertOptional($key; $val):
     if ($val | length) == 0 then [] else upsertPlain($key; $val) end;
   [
     "set -euo pipefail",
     "ENV_FILE=/opt/nutriconsultas/app.env",
     "touch \"$ENV_FILE\""
   ]
   + upsertPlain("AI_ENABLED"; $enabled)
   + upsertSecret("OPENAI_API_KEY"; $keyB64)
   + upsertOptional("OPENAI_MODEL"; $model)
   + upsertOptional("OPENAI_STORE"; $store)
   + upsertOptional("AI_CHAT_MESSAGE_RATE_LIMIT"; $rateLimit)
   + upsertOptional("AI_CHAT_MESSAGE_RATE_WINDOW"; $rateWindow)
   + upsertOptional("AI_MAX_TOOL_CALLS"; $maxTools)
   + [
       "chmod 640 \"$ENV_FILE\"",
       "chown root:nutri \"$ENV_FILE\"",
       "systemctl restart nutriconsultas",
       "for i in $(seq 1 60); do",
       "  if curl -sf http://127.0.0.1:3000/actuator/health >/dev/null 2>&1; then break; fi",
       "  sleep 2",
       "done",
       "curl -sf http://127.0.0.1:3000/actuator/health >/dev/null",
       "systemctl is-active --quiet nutriconsultas",
       "echo \"=== AI env audit ===\"",
       "grep -E \"^AI_ENABLED=\" \"$ENV_FILE\" || echo \"AI_ENABLED=MISSING\"",
       "v=$(grep -E \"^OPENAI_API_KEY=\" \"$ENV_FILE\" | cut -d= -f2- || true)",
       "if [ -z \"$v\" ]; then echo \"OPENAI_API_KEY=EMPTY\"; else echo \"OPENAI_API_KEY=SET(len=${#v})\"; fi",
       "grep -E \"^OPENAI_MODEL=\" \"$ENV_FILE\" || echo \"OPENAI_MODEL=MISSING\"",
       "grep -E \"^OPENAI_STORE=\" \"$ENV_FILE\" || true",
       "grep -E \"^AI_CHAT_MESSAGE_RATE_LIMIT=\" \"$ENV_FILE\" || true",
       "grep -E \"^AI_CHAT_MESSAGE_RATE_WINDOW=\" \"$ENV_FILE\" || true",
       "journalctl -u nutriconsultas -n 30 --no-pager | grep -i \"AI assistant\" || true"
     ] | { commands: . }')"

COMMAND_ID="$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name "AWS-RunShellScript" \
  --timeout-seconds 180 \
  --parameters "$PARAMS" \
  --query 'Command.CommandId' \
  --output text)"

echo "SSM command: $COMMAND_ID (instance $INSTANCE_ID)"
aws ssm wait command-executed --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID"
STATUS="$(aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" --query Status --output text)"
STDOUT="$(aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" --query StandardOutputContent --output text)"
if [ "$STATUS" != "Success" ]; then
  aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" \
    --query '[Status,StandardErrorContent]' --output text >&2
  exit 1
fi
echo "$STDOUT"
echo "AI OpenAI env updated and nutriconsultas restarted."
