#!/usr/bin/env bash
# Update invitation email env vars in /opt/nutriconsultas/app.env on the app EC2, then restart.
#
# Usage:
#   export AWS_PROFILE=minutriporcion AWS_DEFAULT_REGION=us-east-1
#   export INVITATION_EMAIL_MODE=ses MAIL_FROM='invites@minutriporcion.com'
#   bash infrastructure/scripts/ssm-update-invitation-email.sh
#
# Optional: AWS_SES_REGION=us-east-1 (default us-east-1)
#
# See docs/subscription/INVITATION-EMAIL.md (#209).
set -euo pipefail

PROJECT="${1:-nutriconsultas}"
P="/${PROJECT}/deploy"
: "${AWS_REGION:-${AWS_DEFAULT_REGION:?Set AWS_DEFAULT_REGION (or use configure)}}"
: "${INVITATION_EMAIL_MODE:?Set INVITATION_EMAIL_MODE (console or ses)}"
: "${MAIL_FROM:?Set MAIL_FROM (e.g. invites@minutriporcion.com)}"

AWS_SES_REGION="${AWS_SES_REGION:-us-east-1}"

if [[ "$INVITATION_EMAIL_MODE" != "console" && "$INVITATION_EMAIL_MODE" != "ses" ]]; then
  echo "Error: INVITATION_EMAIL_MODE must be console or ses." >&2
  exit 1
fi

INSTANCE_ID="$(aws ssm get-parameter --name "$P/app_instance_id" --query "Parameter.Value" --output text)"

b64() { printf '%s' "$1" | base64 | tr -d '\n'; }

PARAMS="$(jq -n \
  --arg mode "$(b64 "$INVITATION_EMAIL_MODE")" \
  --arg from "$(b64 "$MAIL_FROM")" \
  --arg region "$(b64 "$AWS_SES_REGION")" \
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
   upsert("INVITATION_EMAIL_MODE"; $mode)
   + upsert("MAIL_FROM"; $from)
   + upsert("AWS_SES_REGION"; $region)
   + [
       "chmod 640 \"$ENV_FILE\"",
       "chown root:nutri \"$ENV_FILE\"",
       "systemctl restart nutriconsultas",
       "systemctl is-active --quiet nutriconsultas"
     ] | { commands: . }')"

COMMAND_ID="$(aws ssm send-command \
  --instance-ids "$INSTANCE_ID" \
  --document-name "AWS-RunShellScript" \
  --timeout-seconds 120 \
  --parameters "$PARAMS" \
  --query 'Command.CommandId' \
  --output text)"

echo "SSM command: $COMMAND_ID (instance $INSTANCE_ID)"
aws ssm wait command-executed --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID"
STATUS="$(aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" --query Status --output text)"
if [ "$STATUS" != "Success" ]; then
  aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" \
    --query '[Status,StandardErrorContent]' --output text >&2
  exit 1
fi
echo "Invitation email env updated and nutriconsultas restarted."
