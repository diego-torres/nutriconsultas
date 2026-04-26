# AWS infrastructure (Terraform)

Provisions a **small, cost-oriented** two-instance layout: one EC2 for **PostgreSQL** and one for the **Spring Boot** app, in the **default VPC** of the target region. Security groups follow least privilege: **port 5432** on the database is reachable **only** from the application’s security group.

This matches [issue #69](https://github.com/diego-torres/nutriconsultas/issues/69) (repository layout, automation, and documentation). **No secrets** belong in this repository: use a local `terraform.tfvars` (ignored by git) and/or `TF_VAR_*` (see [variables](variables.tf)).

## What you will pay (ballpark; check current AWS pricing and your free-tier eligibility)

| Item | Notes |
|------|--------|
| **EC2** | `t3.micro` is a common *free tier for eligible new accounts* (750 Linux hours per month, first 12 months). **Two** instances each running 24/7 often **exceeds** 750 hours combined, so you may be billed for the extra time. `t2.micro` is the older free-tier class in some materials; use the same sizing rule of thumb. |
| **EBS** | A small `gp3` root volume; new accounts may get 30 GiB of General Purpose free tier. |
| **Elastic IP** | Charged if **not** attached to a running instance, or in some disassociation cases. Keep the EIP on the app instance. |
| **Data transfer** | Varies with traffic. |
| **Route 53** (optional) | A hosted public zone is about **US$0.50/month** per zone; it is **not** covered by the classic “12-month free” EC2/EBS bundle. |

**NAT gateways and Application Load Balancers** are *not* created here, to keep monthly cost low. The app is exposed on the instance (nginx on 80, Spring Boot on 3000). For HTTPS, add **Let’s Encrypt** (e.g. `certbot`) on the app host, or a managed load balancer and certificates later if you outgrow a single host.

## Prerequisites

- [Terraform](https://developer.hashicorp.com/terraform) `>= 1.5.0` and an AWS account with a **default VPC** in the target region.
- [AWS credentials](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-configure.html) in the environment or shared config files (`aws configure`).
- A strong `db_app_password` (alphanumeric and symbols; avoid a raw single quote in the password, or the bootstrap SQL on the database instance can fail).

## Configure and apply

1. **Copy the example variable file and edit (do not commit secrets):**
   ```bash
   cp terraform.tfvars.example terraform.tfvars
   # Edit: admin_ssh_cidr, db_app_password, region, and optional domain.
   ```
2. **Set the database password (required; this variable has no default):**
   ```bash
   export TF_VAR_db_app_password='your-long-random-secret'
   ```
3. **Initialize and apply**
   ```bash
   cd infrastructure
   terraform init
   terraform plan
   terraform apply
   ```

4. **SSH key (optional):** If `create_key_pair = true` and you did not set `ec2_key_name`, the **private** PEM is in the sensitive output `ec2_key_pair_private_pem` (not printed by default; use `terraform output -raw ec2_key_pair_private_pem` and save to a file, `chmod 400`). The database instance has **no** key pair: use **SSM Session Manager** (`aws ssm start-session --target <db_instance_id>`) once the SSM agent is online.

5. **Deploy the application JAR** to the app instance, then start the service:
   ```bash
   # Example: copy from your laptop (replace key path and user ubuntu vs ec2-user — Amazon Linux uses ec2-user)
   scp -i your-key.pem ../target/nutriconsultas-*.jar ec2-user@<app_public_ip>:/tmp/app.jar
   # On the app host
   sudo mv /tmp/app.jar /opt/nutriconsultas/app.jar
   sudo chown nutri:nutri /opt/nutriconsultas/app.jar
   sudo systemctl enable --now nutriconsultas
   ```

6. **OAuth2 / environment:** Set `AUTH_*`, `AWS_*`, and any other required variables for the app. Options include editing `/opt/nutriconsultas/app.env` and extending the `systemd` `EnvironmentFile`, or a separate drop-in. Never commit real secrets to git.

7. **Destroy (when you are sure):**
   ```bash
   terraform destroy
   ```

## Outputs

- `app_public_ip` – Elastic IP (HTTP on port 80, nginx to Tomcat/embedded 3000).
- `db_private_ip` / `jdbc_example` – connection details; password is the value you set for the database user, not the Terraform state password field alone (treat state as sensitive).
- `route53_name_servers` – if you set `route53_domain`, add these in your registrar. See [domain-setup.md](domain-setup.md).
- `ec2_key_pair_private_pem` – when Terraform created the key pair; save and protect.

## What is *not* automated (by design, for simplicity and cost)

- **TLS/HTTPS** on a public host name: use certbot, or a load balancer, after DNS points to the EIP.
- **Backups, observability, and high availability**: a single-EC2 database is a starting point, not a production backup strategy.
- **Bastion host**: SSM is preferred over opening SSH to the data tier.

## Files

- `main.tf` – Security groups, EC2, EIAM SSM, EIP, optional Route 53
- `variables.tf` / `outputs.tf` – Config and post-apply values
- `templates/*.sh` – User data for database and app hosts
