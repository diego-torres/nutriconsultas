#!/usr/bin/env bash
# Update patient mobile Auth0 broker env vars in /opt/nutriconsultas/app.env, then restart.
#
# Usage:
#   export AWS_PROFILE=minutriporcion AWS_DEFAULT_REGION=us-east-1
#   export AUTH0_MOBILE_NATIVE_CLIENT_ID='CmZd...'
#   export AUTH0_PATIENT_BROKER_CLIENT_ID='...'
#   export AUTH0_PATIENT_BROKER_CLIENT_SECRET='...'
#   export AUTH0_PATIENT_BROKER_DOMAIN='https://your-tenant.auth0.com/'   # optional
#   export AUTH0_PATIENT_BROKER_CONNECTION='Username-Password-Authentication'  # optional
#   ./ssm-update-patient-broker.sh [project]
#
# Or source repo root `.env`:
#   set -a && source ../.env && set +a && ./ssm-update-patient-broker.sh
#
# See docs/auth0-mobile-setup.md (mobile repo) and .env.example (broker section).
set -euo pipefail

PROJECT="${1:-nutriconsultas}"
P="/${PROJECT}/deploy"
: "${AWS_REGION:-${AWS_DEFAULT_REGION:?Set AWS_DEFAULT_REGION (or use configure)}}"
: "${AUTH0_MOBILE_NATIVE_CLIENT_ID:?Set AUTH0_MOBILE_NATIVE_CLIENT_ID}"
: "${AUTH0_PATIENT_BROKER_CLIENT_ID:?Set AUTH0_PATIENT_BROKER_CLIENT_ID}"
: "${AUTH0_PATIENT_BROKER_CLIENT_SECRET:?Set AUTH0_PATIENT_BROKER_CLIENT_SECRET}"

AUTH0_PATIENT_BROKER_DOMAIN="${AUTH0_PATIENT_BROKER_DOMAIN:-${AUTH_ISSUER:-}}"
AUTH0_PATIENT_BROKER_CONNECTION="${AUTH0_PATIENT_BROKER_CONNECTION:-Username-Password-Authentication}"

if [ -z "$AUTH0_PATIENT_BROKER_DOMAIN" ]; then
  echo "Error: set AUTH0_PATIENT_BROKER_DOMAIN or AUTH_ISSUER." >&2
  exit 1
fi

INSTANCE_ID="$(aws ssm get-parameter --name "$P/app_instance_id" --query "Parameter.Value" --output text)"

b64() { printf '%s' "$1" | base64 | tr -d '\n'; }

PARAMS="$(jq -n \
  --arg native "$(b64 "$AUTH0_MOBILE_NATIVE_CLIENT_ID")" \
  --arg brokerId "$(b64 "$AUTH0_PATIENT_BROKER_CLIENT_ID")" \
  --arg brokerSecret "$(b64 "$AUTH0_PATIENT_BROKER_CLIENT_SECRET")" \
  --arg brokerDomain "$(b64 "$AUTH0_PATIENT_BROKER_DOMAIN")" \
  --arg brokerConn "$(b64 "$AUTH0_PATIENT_BROKER_CONNECTION")" \
  'def upsert($key; $valB64):
     [
       "set -euo pipefail",
       "ENV_FILE=/opt/nutriconsultas/app.env",
       "touch \"$ENV_FILE\"",
       ("grep -v \"^" + $key + "=\" \"$ENV_FILE\" > \"${ENV_FILE}.tmp\" || true"),
       ("echo -n " + $key + "= >> \"${ENV_FILE}.tmp\""),
       ("printf %s \"" + $valB64 + "\" | base64 -d >> \"${ENV_FILE}.tmp\""),
       "echo >> \"${ENV_FILE}.tmp\"",
       "mv \"${ENV_FILE}.tmp\" \"$ENV_FILE\""
     ];
   upsert("AUTH0_MOBILE_NATIVE_CLIENT_ID"; $native)
   + upsert("AUTH0_PATIENT_BROKER_CLIENT_ID"; $brokerId)
   + upsert("AUTH0_PATIENT_BROKER_CLIENT_SECRET"; $brokerSecret)
   + upsert("AUTH0_PATIENT_BROKER_DOMAIN"; $brokerDomain)
   + upsert("AUTH0_PATIENT_BROKER_CONNECTION"; $brokerConn)
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
       "echo \"=== broker env audit ===\"",
       "for k in AUTH0_MOBILE_NATIVE_CLIENT_ID AUTH0_PATIENT_BROKER_CLIENT_ID AUTH0_PATIENT_BROKER_CLIENT_SECRET AUTH0_PATIENT_BROKER_DOMAIN AUTH0_PATIENT_BROKER_CONNECTION; do",
       "  v=$(grep -E \"^${k}=\" \"$ENV_FILE\" | cut -d= -f2- || true)",
       "  if [ -z \"$v\" ]; then echo \"${k}=EMPTY\"; else echo \"${k}=SET(len=${#v})\"; fi",
       "done",
       "echo \"=== signup probe ===\"",
       "curl -sS -o /tmp/signup-probe.json -w \"HTTP %{http_code}\\n\" -X POST http://127.0.0.1:3000/rest/mobile/auth/signup -H \"Content-Type: application/json\" -d \"{\\\"email\\\":\\\"broker-probe@example.com\\\",\\\"password\\\":\\\"TestPass123!\\\",\\\"displayName\\\":\\\"Probe\\\"}\"",
       "head -c 200 /tmp/signup-probe.json; echo"
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
echo "Patient auth broker env updated and nutriconsultas restarted."
