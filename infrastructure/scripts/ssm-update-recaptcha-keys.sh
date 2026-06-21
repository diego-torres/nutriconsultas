#!/usr/bin/env bash
# Update RECAPTCHA_SITE_KEY and RECAPTCHA_SECRET_KEY in /opt/nutriconsultas/app.env on the app EC2, then restart.
#
# Usage:
#   export AWS_PROFILE=minutriporcion AWS_DEFAULT_REGION=us-east-1
#   export RECAPTCHA_SITE_KEY='6L...' RECAPTCHA_SECRET_KEY='6L...'
#   ./ssm-update-recaptcha-keys.sh [project]
#
# Keys must be Google reCAPTCHA v2 (checkbox) site/secret pair from https://www.google.com/recaptcha/admin
set -euo pipefail

PROJECT="${1:-nutriconsultas}"
P="/${PROJECT}/deploy"
: "${AWS_REGION:-${AWS_DEFAULT_REGION:?Set AWS_DEFAULT_REGION (or use configure)}}"
: "${RECAPTCHA_SITE_KEY:?Set RECAPTCHA_SITE_KEY (Google reCAPTCHA v2 site key)}"
: "${RECAPTCHA_SECRET_KEY:?Set RECAPTCHA_SECRET_KEY (Google reCAPTCHA v2 secret key)}"

if [[ "$RECAPTCHA_SITE_KEY" != 6L* ]] || [[ "$RECAPTCHA_SECRET_KEY" != 6L* ]]; then
  echo "Error: RECAPTCHA_* values must be Google reCAPTCHA keys (typically starting with 6L)." >&2
  echo "Got site prefix: ${RECAPTCHA_SITE_KEY:0:8}... secret prefix: ${RECAPTCHA_SECRET_KEY:0:8}..." >&2
  exit 1
fi

INSTANCE_ID="$(aws ssm get-parameter --name "$P/app_instance_id" --query "Parameter.Value" --output text)"

SITE_B64="$(printf '%s' "$RECAPTCHA_SITE_KEY" | base64 | tr -d '\n')"
SECRET_B64="$(printf '%s' "$RECAPTCHA_SECRET_KEY" | base64 | tr -d '\n')"

PARAMS="$(jq -n \
  --arg site "$SITE_B64" \
  --arg secret "$SECRET_B64" \
  '{
     commands: [
       "set -euo pipefail",
       "ENV_FILE=/opt/nutriconsultas/app.env",
       "touch \"$ENV_FILE\"",
       "grep -v \"^RECAPTCHA_SITE_KEY=\" \"$ENV_FILE\" > \"${ENV_FILE}.tmp\" || true",
       "grep -v \"^RECAPTCHA_SECRET_KEY=\" \"${ENV_FILE}.tmp\" > \"${ENV_FILE}.new\" || true",
       "rm -f \"${ENV_FILE}.tmp\"",
       ("echo -n RECAPTCHA_SITE_KEY= >> \"${ENV_FILE}.new\""),
       ("printf %s \"" + $site + "\" | base64 -d >> \"${ENV_FILE}.new\""),
       "echo >> \"${ENV_FILE}.new\"",
       ("echo -n RECAPTCHA_SECRET_KEY= >> \"${ENV_FILE}.new\""),
       ("printf %s \"" + $secret + "\" | base64 -d >> \"${ENV_FILE}.new\""),
       "echo >> \"${ENV_FILE}.new\"",
       "mv \"${ENV_FILE}.new\" \"$ENV_FILE\"",
       "chmod 640 \"$ENV_FILE\"",
       "chown root:nutri \"$ENV_FILE\"",
       "systemctl restart nutriconsultas",
       "systemctl is-active --quiet nutriconsultas"
     ]
   }')"

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
  aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" --query '[Status,StandardErrorContent]' --output text >&2
  exit 1
fi
echo "reCAPTCHA keys updated and nutriconsultas restarted."
