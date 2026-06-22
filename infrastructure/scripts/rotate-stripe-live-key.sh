#!/usr/bin/env bash
# Apply a newly rolled Stripe LIVE secret key (+ optional webhook secret) to EC2.
# Use after rolling the key in Stripe Dashboard (Developers → API keys → Roll key).
#
# Usage:
#   export AWS_PROFILE=minutriporcion AWS_DEFAULT_REGION=us-east-1
#   export STRIPE_SECRET_KEY='sk_live_...'   # paste once in terminal only — never commit
#   export STRIPE_WEBHOOK_SECRET='whsec_...' # optional; omit to keep current EC2 webhook secret
#   bash infrastructure/scripts/rotate-stripe-live-key.sh
#
# Updates gitignored infrastructure/terraform.tfvars stripe_secret_key when present.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
: "${STRIPE_SECRET_KEY:?Set STRIPE_SECRET_KEY to the new sk_live_... from Stripe Dashboard}"

if [[ "$STRIPE_SECRET_KEY" != sk_live_* ]]; then
  echo "Error: expected a live secret key (sk_live_...)." >&2
  exit 1
fi

export PAYMENT_STUB_SIMULATE_CHECKOUT=false
export STRIPE_SUCCESS_URL="${STRIPE_SUCCESS_URL:-https://minutriporcion.com/admin}"
export STRIPE_CANCEL_URL="${STRIPE_CANCEL_URL:-https://minutriporcion.com/admin}"

bash "$ROOT/infrastructure/scripts/ssm-update-stripe-keys.sh"

TFVARS="$ROOT/infrastructure/terraform.tfvars"
if [ -f "$TFVARS" ]; then
  python3 - <<PY
from pathlib import Path
import re
p = Path("$TFVARS")
text = p.read_text()
sk = """$STRIPE_SECRET_KEY"""
text = re.sub(r'stripe_secret_key\s*=\s*".*"', f'stripe_secret_key     = "{sk}"', text)
p.write_text(text)
print("Updated stripe_secret_key in terraform.tfvars (gitignored).")
PY
fi

echo "Done. Old exposed key: revoke or wait for Stripe roll grace period to expire."
