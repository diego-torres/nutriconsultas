#!/usr/bin/env bash
# Update Apple Sign-In webhook env vars in /opt/nutriconsultas/app.env on the app EC2, then restart.
#
# Usage (enable webhook — observe-only):
#   export AWS_PROFILE=minutriporcion AWS_DEFAULT_REGION=us-east-1
#   export APPLE_SIGNIN_WEBHOOK_ENABLED=true
#   export APPLE_SIGNIN_EXPECTED_AUDIENCE='com.minutriporcion.app'   # Apple Services ID / client identifier
#   ./ssm-update-apple-signin.sh [project]
#
# Usage (disable webhook):
#   export APPLE_SIGNIN_WEBHOOK_ENABLED=false
#   ./ssm-update-apple-signin.sh
#
# Optional: APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS, APPLE_SIGNIN_VERIFICATION_FAILURE_ALERT_THRESHOLD
#
# See docs/auth/apple-signin-setup.md (#510).
set -euo pipefail

PROJECT="${1:-nutriconsultas}"
P="/${PROJECT}/deploy"
: "${AWS_REGION:-${AWS_DEFAULT_REGION:?Set AWS_DEFAULT_REGION (or use configure)}}"
: "${APPLE_SIGNIN_WEBHOOK_ENABLED:?Set APPLE_SIGNIN_WEBHOOK_ENABLED (true or false)}"

if [ "$APPLE_SIGNIN_WEBHOOK_ENABLED" = "true" ]; then
  : "${APPLE_SIGNIN_EXPECTED_AUDIENCE:?Set APPLE_SIGNIN_EXPECTED_AUDIENCE when APPLE_SIGNIN_WEBHOOK_ENABLED=true}"
fi

INSTANCE_ID="$(aws ssm get-parameter --name "$P/app_instance_id" --query "Parameter.Value" --output text)"

PARAMS="$(jq -n \
  --arg enabled "$APPLE_SIGNIN_WEBHOOK_ENABLED" \
  --arg audience "${APPLE_SIGNIN_EXPECTED_AUDIENCE:-}" \
  --arg autoDestructive "${APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS:-}" \
  --arg alertThreshold "${APPLE_SIGNIN_VERIFICATION_FAILURE_ALERT_THRESHOLD:-}" \
  'def upsertPlain($key; $val):
     [
       ("grep -v \"^" + $key + "=\" \"$ENV_FILE\" > \"${ENV_FILE}.tmp\" || true"),
       ("echo " + $key + "=" + $val + " >> \"${ENV_FILE}.tmp\""),
       "mv \"${ENV_FILE}.tmp\" \"$ENV_FILE\""
     ];
   def upsertOptional($key; $val):
     if ($val | length) == 0 then [] else upsertPlain($key; $val) end;
   [
     "set -euo pipefail",
     "ENV_FILE=/opt/nutriconsultas/app.env",
     "touch \"$ENV_FILE\""
   ]
   + upsertPlain("APPLE_SIGNIN_WEBHOOK_ENABLED"; $enabled)
   + upsertOptional("APPLE_SIGNIN_EXPECTED_AUDIENCE"; $audience)
   + upsertOptional("APPLE_SIGNIN_AUTO_PROCESS_DESTRUCTIVE_EVENTS"; $autoDestructive)
   + upsertOptional("APPLE_SIGNIN_VERIFICATION_FAILURE_ALERT_THRESHOLD"; $alertThreshold)
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
       "echo \"=== Apple Sign-In env audit ===\"",
       "grep -E \"^APPLE_SIGNIN_\" \"$ENV_FILE\" || echo \"APPLE_SIGNIN_*=MISSING\"",
       "journalctl -u nutriconsultas -n 30 --no-pager | grep -i \"Apple Sign-In\" || true"
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
echo "Apple Sign-In env updated and nutriconsultas restarted."
