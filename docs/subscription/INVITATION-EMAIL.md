# Invitation email delivery (#209)

Runbook for **nutritionist invitation** emails after platform admin creates an invite (#184). Production uses **Amazon SES** (Terraform + EC2 IAM role). Local dev uses **console mode** — no AWS credentials required.

---

## Modes

| Mode | Config | When |
|------|--------|------|
| `console` | `nutriconsultas.subscription.invitation.email.mode=console` (default) | Local dev, laptops without SES |
| `ses` | `mode=ses` + verified domain + `MAIL_FROM` | Staging/production EC2 |

| Property / env | Description |
|----------------|-------------|
| `nutriconsultas.subscription.invitation.email.mode` / `INVITATION_EMAIL_MODE` | `console` or `ses` |
| `nutriconsultas.subscription.invitation.email.from-address` / `MAIL_FROM` | Required in `ses` mode (e.g. `invites@minutriporcion.com`) |
| `nutriconsultas.subscription.invitation.email.ses-region` / `AWS_SES_REGION` | SES API region (default `us-east-1`) |
| `nutriconsultas.subscription.invitation.base-url` / `APP_BASE_URL` | Base URL for redeem links in email body |

Template: `src/main/resources/templates/email/nutritionist-invitation.html`

---

## Local dev (console mode)

```properties
# .env or application-dev.properties
nutriconsultas.subscription.invitation.email.mode=console
nutriconsultas.subscription.invitation.base-url=http://localhost:3000
```

1. Platform admin creates invitation at `/admin/platform/invitations/new`
2. App logs `INVITATION_LINK=http://localhost:3000/invitation/nutritionist/redeem?token=...` (grep-friendly)
3. Copy link from logs **or** admin UI flash `inviteUrl`
4. Redeem flow unchanged (#184)

No SMTP, no AWS credentials on laptop.

---

## Production (SES)

### Terraform

When `enable_ses = true` (default) and `route53_domain` or `public_site_domain` is set:

- `aws_ses_domain_identity` + DKIM for the mail domain
- Route 53 TXT/CNAME records (when `route53_domain` creates a zone)
- IAM policy on app EC2 role: `ses:SendEmail`, `ses:SendRawEmail`
- New app instances: `INVITATION_EMAIL_MODE=ses`, `MAIL_FROM`, `AWS_SES_REGION` in `app.env`

Variables: `enable_ses`, `ses_mail_domain`, `invitation_mail_from` — see [`infrastructure/README.md`](../../infrastructure/README.md).

### SES sandbox

New AWS accounts start in **SES sandbox**: you can only send to verified recipient addresses until production access is approved in the SES console. Request production access before go-live.

### Brownfield EC2

```bash
export AWS_PROFILE=minutriporcion AWS_DEFAULT_REGION=us-east-1
export INVITATION_EMAIL_MODE=ses MAIL_FROM='invites@minutriporcion.com'
bash infrastructure/scripts/ssm-update-invitation-email.sh
```

Ensure the domain is verified in SES and DKIM DNS records are published.

---

## Logging

- Console mode: logs `INVITATION_LINK=...` and redacted recipient (`email[n***@domain.com]`)
- SES mode: logs send success with redacted recipient only
- Never log full recipient email, card data, or patient PHI

---

## References

- [`SUBSCRIPTION-ENFORCEMENT-PLAN.md`](SUBSCRIPTION-ENFORCEMENT-PLAN.md) — invitation → checkout flow
- [Amazon SES domain verification + DKIM](https://docs.aws.amazon.com/ses/latest/dg/creating-identities.html)
- [`STRIPE-OPS.md`](STRIPE-OPS.md) — paid invitation checkout (#208)
