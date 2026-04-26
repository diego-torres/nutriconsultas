# Pointing **minutriporcion.com** at the application

You can either **delegate the whole domain to Route 53** (useful if you want all DNS in AWS) or **keep GoDaddy as the DNS host** and only add **A/AAAA** records. Both are valid; the Terraform stack supports **full delegation** via the optional `route53_domain` variable and creates the hosted zone and apex/`www` **A** records to the app’s **Elastic IP**.

> **No secrets in DNS.** Never put API keys or database passwords in DNS. Only **names, IPs, and mail-related records** belong in public DNS.

## Option A — Delegate the domain to Route 53 (Terraform creates the zone)

1. In Terraform, set:
   ```hcl
   route53_domain   = "minutriporcion.com"
   route53_www_record = true
   ```
   Then `terraform apply`. Note the `route53_name_servers` output: four **NS** values like `ns-***.awsdns-**.` etc.

2. In **GoDaddy** (Domains → your domain → **Nameservers**):
   - Select **I’ll use my own nameservers** (or **Change** → **Custom**).
   - Enter the **four** name servers from `terraform output route53_name_servers` exactly as printed (trailing dot optional in GoDaddy, depending on the UI).
   - Save. Propagation can take a few hours (sometimes up to 48 hours); often much faster.

3. After delegation propagates, **all** DNS for `minutriporcion.com` is defined in the Route 53 **hosted zone** in AWS. The Terraform in this repo already includes:
   - An **A** record for the apex `minutriporcion.com` → app Elastic IP.
   - If enabled, an **A** for `www.minutriporcion.com` → the same IP.

4. If you use **email** (MX) or other records at GoDaddy today, you must **recreate those same records in Route 53** (same priorities and targets) or your mail and services will break after you switch nameservers.

5. On the app EC2, terminate TLS (HTTPS) in **nginx** or a reverse proxy, or in front of the instance, using a certificate (e.g. [Let’s Encrypt](https://letsencrypt.org/)) for `minutriporcion.com` and `www.minutriporcion.com` if you serve both. Until then, the site is HTTP on port 80; nginx already proxies to the app on 3000.

6. **Cost note:** a Route 53 **hosted zone** is billed (see [Route 53 pricing](https://aws.amazon.com/route53/pricing/)); the **free tier** in AWS is separate from that charge.

---

## Option B — Keep nameservers on GoDaddy (no Route 53 zone in Terraform)

1. `terraform apply` **without** `route53_domain` (or `null`).

2. After apply, read `app_public_ip` (Elastic IP). It **must stay** the same: do not release the EIP, or the IP in DNS will be wrong.

3. In **GoDaddy** DNS (DNS **Management** for the domain), add or update:
   - **Type** `A`, **Name** `@` (or `minutriporcion.com` depending on the form), **Value** = the Elastic **IPv4** from `terraform output app_public_ip`, TTL e.g. 300–600s.
   - If you use **www**: **Type** `A`, **Name** `www`, **Value** = same IP (or a **CNAME** to the apex, if GoDaddy allows it; many UIs use two **A** records, which is fine here).

4. This avoids the Route 53 monthly hosted zone fee, but you manage A/AAAA and future records entirely in **GoDaddy** until you migrate.

---

## If you *already* created a Route 53 zone manually

- Do **not** set `route53_domain` in Terraform in a way that would **duplicate** an existing zone for the same name in the same account, or you will get a conflict. Either import the existing zone into state or `terraform` only the **records** against that zone. When in doubt, use a single source of truth for the zone.

---

## Verification (either option)

- From a laptop: `dig +short minutriporcion.com` (or an online “DNS checker”) should return the app’s **public** IPv4 after TTL expires.
- From a browser: `http://minutriporcion.com` should hit **nginx** on 80, which reverse-proxies to the Spring Boot port **3000** on the same instance.

**IPv6** is not configured in the Terraform here; you can add AAAA to an Elastic IP that supports it or use a dual-stack design later. For a minimal first deployment, **A** to the IPv4 EIP is enough.
