#!/usr/bin/env bash
# Update Stripe / payment env vars in /opt/nutriconsultas/app.env on the app EC2, then restart.
#
# Usage:
#   export AWS_PROFILE=minutriporcion AWS_DEFAULT_REGION=us-east-1
#   export STRIPE_SECRET_KEY='sk_test_...' STRIPE_WEBHOOK_SECRET='whsec_...'
#   ./ssm-update-stripe-keys.sh [project]
#
# Optional:
#   PAYMENT_PROVIDER=stripe (default)
#   PAYMENT_STUB_SIMULATE_CHECKOUT=false (default for prod)
#   PAYMENT_CURRENCY=MXN (default)
#   STRIPE_SUCCESS_URL / STRIPE_CANCEL_URL (default https://minutriporcion.com/admin)
#   STRIPE_PRICE_BASICO / _PROFESIONAL / _PLUS / _CONSULTORIO (optional Price IDs)
#
# See docs/subscription/STRIPE-OPS.md (#208).
set -euo pipefail

PROJECT="${1:-nutriconsultas}"
P="/${PROJECT}/deploy"
: "${AWS_REGION:-${AWS_DEFAULT_REGION:?Set AWS_DEFAULT_REGION (or use configure)}}"
: "${STRIPE_SECRET_KEY:?Set STRIPE_SECRET_KEY (Stripe secret API key)}"

PAYMENT_PROVIDER="${PAYMENT_PROVIDER:-stripe}"
PAYMENT_STUB_SIMULATE_CHECKOUT="${PAYMENT_STUB_SIMULATE_CHECKOUT:-false}"
PAYMENT_CURRENCY="${PAYMENT_CURRENCY:-MXN}"
STRIPE_WEBHOOK_SECRET="${STRIPE_WEBHOOK_SECRET:-}"
STRIPE_SUCCESS_URL="${STRIPE_SUCCESS_URL:-https://minutriporcion.com/admin}"
STRIPE_CANCEL_URL="${STRIPE_CANCEL_URL:-https://minutriporcion.com/admin}"
STRIPE_PRICE_BASICO="${STRIPE_PRICE_BASICO:-}"
STRIPE_PRICE_PROFESIONAL="${STRIPE_PRICE_PROFESIONAL:-}"
STRIPE_PRICE_PLUS="${STRIPE_PRICE_PLUS:-}"
STRIPE_PRICE_CONSULTORIO="${STRIPE_PRICE_CONSULTORIO:-}"

if [[ "$STRIPE_SECRET_KEY" != sk_* ]]; then
  echo "Error: STRIPE_SECRET_KEY must start with sk_test_ or sk_live_." >&2
  exit 1
fi

if [ -n "$STRIPE_WEBHOOK_SECRET" ] && [[ "$STRIPE_WEBHOOK_SECRET" != whsec_* ]]; then
  echo "Error: STRIPE_WEBHOOK_SECRET must start with whsec_ when set." >&2
  exit 1
fi

INSTANCE_ID="$(aws ssm get-parameter --name "$P/app_instance_id" --query "Parameter.Value" --output text)"

b64() { printf '%s' "$1" | base64 | tr -d '\n'; }

PARAMS="$(jq -n \
  --arg pp "$(b64 "$PAYMENT_PROVIDER")" \
  --arg sk "$(b64 "$STRIPE_SECRET_KEY")" \
  --arg wh "$(b64 "$STRIPE_WEBHOOK_SECRET")" \
  --arg stub "$(b64 "$PAYMENT_STUB_SIMULATE_CHECKOUT")" \
  --arg cur "$(b64 "$PAYMENT_CURRENCY")" \
  --arg suc "$(b64 "$STRIPE_SUCCESS_URL")" \
  --arg can "$(b64 "$STRIPE_CANCEL_URL")" \
  --arg pb "$(b64 "$STRIPE_PRICE_BASICO")" \
  --arg ppr "$(b64 "$STRIPE_PRICE_PROFESIONAL")" \
  --arg pl "$(b64 "$STRIPE_PRICE_PLUS")" \
  --arg pc "$(b64 "$STRIPE_PRICE_CONSULTORIO")" \
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
   upsert("PAYMENT_PROVIDER"; $pp)
   + upsert("STRIPE_SECRET_KEY"; $sk)
   + upsert("STRIPE_WEBHOOK_SECRET"; $wh)
   + upsert("PAYMENT_STUB_SIMULATE_CHECKOUT"; $stub)
   + upsert("PAYMENT_CURRENCY"; $cur)
   + upsert("STRIPE_SUCCESS_URL"; $suc)
   + upsert("STRIPE_CANCEL_URL"; $can)
   + upsert("STRIPE_PRICE_BASICO"; $pb)
   + upsert("STRIPE_PRICE_PROFESIONAL"; $ppr)
   + upsert("STRIPE_PRICE_PLUS"; $pl)
   + upsert("STRIPE_PRICE_CONSULTORIO"; $pc)
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
echo "Stripe payment env updated and nutriconsultas restarted."
