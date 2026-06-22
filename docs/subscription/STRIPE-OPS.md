# Stripe operational setup (#208)

Runbook for **test and production** Stripe billing on Minutriporcion. Code lives in #207 (`StripePaymentProvider`); this doc covers Dashboard, webhooks, and EC2 secrets.

**Webhook URL (production):** `https://minutriporcion.com/rest/subscription/payment/webhook`

**Monthly prices (MXN, inline checkout when Price IDs omitted):** Básico $100 · Profesional $200 · Plus $600 · Consultorio $900.

---

## Checklist

### 1. Stripe account

- [ ] Account active with **MXN** enabled
- [ ] **Test** secret key (`sk_test_…`) for validation
- [ ] **Live** secret key (`sk_live_…`) for go-live (separate webhook endpoint)

Optional: create recurring **Products + Prices** in [Stripe Dashboard → Products](https://dashboard.stripe.com/products) and set `STRIPE_PRICE_*` env vars. If omitted, checkout uses inline `price_data` at the amounts above.

### 2. Webhook endpoint

Create in [Dashboard → Webhooks](https://dashboard.stripe.com/webhooks) **or** Stripe CLI:

```bash
stripe webhook_endpoints create \
  --url "https://minutriporcion.com/rest/subscription/payment/webhook" \
  --enabled-events checkout.session.completed,customer.subscription.updated,customer.subscription.deleted,invoice.payment_failed
```

Copy the **signing secret** (`whsec_…`) into `STRIPE_WEBHOOK_SECRET`.

| Event | App action |
|-------|------------|
| `checkout.session.completed` | Activate subscription (`ACTIVE`), set Stripe subscription + customer ids |
| `customer.subscription.updated` | Sync status / period |
| `customer.subscription.deleted` | Cancel subscription |
| `invoice.payment_failed` | Grace / past-due handling |

Invalid signature → HTTP **400**. Duplicate events are idempotent via `PaymentWebhookService`.

### 3. EC2 environment (`app.env`)

Brownfield hosts (already running) do **not** re-run Terraform user-data. Push secrets with SSM:

```bash
export AWS_PROFILE=minutriporcion AWS_DEFAULT_REGION=us-east-1
export STRIPE_SECRET_KEY='sk_test_...'    # or sk_live_ for production
export STRIPE_WEBHOOK_SECRET='whsec_...'
export PAYMENT_STUB_SIMULATE_CHECKOUT=false
bash infrastructure/scripts/ssm-update-stripe-keys.sh
```

**New instances:** set `stripe_secret_key` / `stripe_webhook_secret` in gitignored `terraform.tfvars` (or `TF_VAR_*`) before `terraform apply`. User-data writes `PAYMENT_PROVIDER`, `STRIPE_*`, `PAYMENT_STUB_SIMULATE_CHECKOUT=false`, and success/cancel URLs.

| Variable | Required | Notes |
|----------|----------|--------|
| `PAYMENT_PROVIDER` | yes | `stripe` |
| `STRIPE_SECRET_KEY` | yes | `sk_test_` or `sk_live_` |
| `STRIPE_WEBHOOK_SECRET` | yes (prod) | From webhook endpoint |
| `PAYMENT_STUB_SIMULATE_CHECKOUT` | yes | **`false`** on EC2 |
| `PAYMENT_CURRENCY` | no | Default `MXN` |
| `STRIPE_SUCCESS_URL` / `STRIPE_CANCEL_URL` | no | Default `https://minutriporcion.com/admin` |
| `STRIPE_PRICE_*` | no | Dashboard Price IDs per plan tier |

Never commit keys. Rotate via SSM script or Terraform + instance replace.

### 4. Test mode validation

1. Configure EC2 with **test** keys + test webhook signing secret.
2. Platform admin: create a **paid** nutritionist invitation (not payment-exempt).
3. Redeem link → Stripe Checkout → pay with [test card](https://docs.stripe.com/testing) `4242 4242 4242 4242`.
4. Confirm webhook delivery in Stripe Dashboard (HTTP 2xx).
5. Confirm subscription **ACTIVE** in `/admin/platform/subscriptions`.
6. Logs must not contain card numbers or patient PHI.

Local dev: use `stripe listen --forward-to localhost:3000/rest/subscription/payment/webhook` and the CLI `whsec_` secret in `.env`.

### 5. Production go-live

1. Create a **live mode** webhook endpoint (same URL, live signing secret).
2. Run `ssm-update-stripe-keys.sh` with `sk_live_…` and live `whsec_…`.
3. Smoke test with a real low-value plan or agreed business test.
4. Monitor Stripe Dashboard → Webhooks for failures.

---

## Troubleshooting

| Symptom | Likely cause |
|---------|----------------|
| 503 “El pago en línea no está configurado” | Missing `STRIPE_SECRET_KEY` or `PAYMENT_PROVIDER` not `stripe` |
| Checkout works, subscription stays `PENDING_PAYMENT` | Webhook secret mismatch or endpoint not reachable |
| 400 on webhook | Wrong `STRIPE_WEBHOOK_SECRET` or body modified by proxy |
| Stub checkout on prod | `PAYMENT_STUB_SIMULATE_CHECKOUT=true` (must be `false`) |

**Verify env on host (SSM session):**

```bash
grep -E '^(PAYMENT_|STRIPE_)' /opt/nutriconsultas/app.env | sed 's/=sk_.*/=***/; s/=whsec_.*/=***/'
systemctl status nutriconsultas
curl -s -o /dev/null -w '%{http_code}\n' -X POST https://minutriporcion.com/rest/subscription/payment/webhook
# Expect 400 without Stripe-Signature header
```

---

## References

- [Stripe Checkout subscriptions](https://docs.stripe.com/billing/subscriptions/checkout)
- [Stripe webhooks](https://docs.stripe.com/webhooks)
- [`infrastructure/README.md`](../../infrastructure/README.md) — Terraform variables
- [`SUBSCRIPTION-ENFORCEMENT-PLAN.md`](SUBSCRIPTION-ENFORCEMENT-PLAN.md) — invitation → checkout flow
