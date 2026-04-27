# AWS infrastructure (Terraform)

Provisions a **small, cost-oriented** two-instance layout: one EC2 for **PostgreSQL** and one for the **Spring Boot** app, in the **default VPC** of your target region. The app security group **does not open port 22**; shell access is **SSM Session Manager** only. Security groups: **port 5432** on the database is reachable **only** from the application’s security group.

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
   # Edit: db password, region, and optional domain.
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
4. **If you are upgrading** from a version that set `admin_ssh_cidr` or an EC2 key pair, remove those from `terraform.tfvars` and run `terraform plan` to drop the key pair and remove port 22 from the app security group.

5. **Destroy (when you are sure):** `cd infrastructure && terraform destroy`

## Connect to the EC2 instances (SSM Session Manager only)

**There is no public SSH (port 22) on these instances** — the app security group does not allow it, the instances have **no** EC2 key pair, and the **sshd** service is stopped and **masked** on first boot in user data (defense in depth). Use **SSM Session Manager** for a shell, file inspection, and troubleshooting. The SSM agent (Amazon Linux 2023) uses the instance IAM profile (`AmazonSSMManagedInstanceCore`); the instance can reach the SSM endpoints over the internet (public subnet + public IP, default VPC route to the internet).

### Your IAM user or role (who runs the CLI or console)

Must allow, at minimum, starting a session and usually describing instances, for example:

- `ssm:StartSession`, `ssm:DescribeSessions`, `ssm:TerminateSession` (or use the `AmazonSSMFullAccess` managed policy for a lab account; tighten for production)
- `ssm:SendCommand` if you use the deploy script or SSM for automation
- `ec2:DescribeInstances` (or scoped) to find instance IDs, unless you use IDs from `terraform output`

[Session Manager plugin for the AWS CLI](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html) is **required** for `aws ssm start-session` from a laptop.

### Instance IDs

- `terraform output -raw app_instance_id`
- `terraform output -raw db_instance_id`

### AWS Console

1. Open **Systems Manager** → **Session Manager** → **Start session**.
2. Filter by the instance name tag (e.g. `Name = nutriconsultas-app`) and select the instance, then **Start session**.

If **Start session** is disabled, check that the instance is **online** in the AWS console, that the **SSM agent** is running on the host, and that the instance profile is attached. After first boot, wait 1–2 minutes for the agent to register with SSM.

### AWS CLI

Replace `i-…` and the region with your values:

```bash
aws ssm start-session --target i-0abc1234 --region us-east-1
```

**Port forwarding (optional,** to reach the app on the instance at `localhost:3000` from your machine):

```bash
aws ssm start-session --target i-0abc1234 --region us-east-1 \
  --document-name AWS-StartPortForwardingSession \
  --parameters "portNumber=3000,localPortNumber=13000"
```

Then open `http://127.0.0.1:13000`. See [Session Manager port forwarding](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-sessions.html).

### First-time application deploy (manual, without GitHub)

Preferred: the GitHub Action on `main` (see below) or [scripts/deploy-jar-to-ec2-ssm.sh](scripts/deploy-jar-to-ec2-ssm.sh) from a machine with AWS credentials that can `PutObject` the JAR and `SendCommand` (same pattern as the CI role).

**Option A — SSM from your laptop (after a session is open):** in the interactive shell, you cannot `scp` from the laptop through classical SSH, because port 22 is closed. **Upload the JAR to the S3 artifact bucket** (see `s3_app_artifact_bucket` output) at the key `releases/nutriconsultas-web.jar` using `aws s3 cp`, then in the SSM session run the same `aws s3 cp` / `mv` / `systemctl restart` steps the deploy script uses, or run the full script with credentials that have permission to target the instance.

**Option B — One-shot:** run [scripts/deploy-jar-to-ec2-ssm.sh](scripts/deploy-jar-to-ec2-ssm.sh) (requires `aws` CLI, `jq`, and IAM for S3 + SSM).

### OAuth2 and app secrets on the server

Set `AUTH_*`, `AWS_*`, and any other required environment variables. Edit `/opt/nutriconsultas/app.env` in an SSM session (or a systemd `EnvironmentFile` drop-in). Never commit real secrets to git.

## Outputs

- `app_instance_id` / `db_instance_id` — for SSM `--target` (and outputs below).
- `app_public_ip` – Elastic IP (HTTP on port 80, nginx to the app on 3000).
- `db_private_ip` / `jdbc_example` – connection details; password is the value you set for the database user (treat state as sensitive).
- `route53_name_servers` – if you set `route53_domain`, add these in your registrar. See [domain-setup.md](domain-setup.md).
- `s3_app_artifact_bucket` / `s3_app_artifact_key` – S3 JAR key used by the deploy step (and SSM).
- `codepipeline_name` / `codepipeline_arn` – set when `github_repository` is non-empty; open the pipeline in the **CodePipeline** console to see runs and failures.
- `codestar_connection_arn` – you must **authorize the GitHub connection** once in the console (see below) before the pipeline can fetch the repo.

## AWS CodePipeline: deploy to EC2 (replaces a GitHub Actions deploy)

GitHub is still the **source of truth** for the code, but the **orchestration** to build and deploy runs in **AWS** on push to the branch you set (default `main` via `codepipeline_branch`).

1. Set in Terraform, for example:
   - `github_repository  = "your-github-org/nutriconsultas"`
   - `codepipeline_branch  = "main"`
   - Run `terraform apply`.
2. In the **AWS account**, open **Developer tools** (or **Settings**), then **Connections** (or search for **CodeStar connections**). Select the new connection, choose **Update pending connection**, and sign in to **GitHub** to authorize the app. Wait until the status is **Available** (not *Pending*).
3. Pushes to `codepipeline_branch` in that GitHub repository trigger: **Source** (zip from GitHub) -> **Build** (CodeBuild runs Maven with [buildspecs/app-build.yml](buildspecs/app-build.yml)) -> **Deploy** (CodeBuild runs [buildspecs/app-deploy.yml](buildspecs/app-deploy.yml), which calls [scripts/deploy-jar-to-ec2-ssm.sh](scripts/deploy-jar-to-ec2-ssm.sh): S3 `PutObject` for the JAR, then SSM to the app EC2).
4. The **.github/workflows/maven.yml** job only runs **lint and tests** (no deploy to AWS). If the pipeline fails, use **CodePipeline** / **CodeBuild** log groups in CloudWatch for the failing stage.
5. **Manual** deploy (same as before) from a laptop with `aws` + `jq` and the right IAM: `bash infrastructure/scripts/deploy-jar-to-ec2-ssm.sh path/to/nutriconsultas-web-*.jar [project]`

## What is *not* automated (by design, for simplicity and cost)

- **TLS/HTTPS** on a public host name: use certbot, or a load balancer, after DNS points to the EIP.
- **Backups, observability, and high availability**: a single-EC2 database is a starting point, not a production backup strategy.

## Files

- `main.tf` – Security groups (no 22 on app), EC2, SSM instance profile, EIP, optional Route 53
- `cicd.tf` – S3 JAR bucket, SSM parameters for the deploy script
- `codepipeline.tf` – CodeStar Connection, S3 for pipeline artifacts, CodeBuild, CodePipeline (if `github_repository` is set)
- `buildspecs/app-build.yml`, `buildspecs/app-deploy.yml` – Build and deploy for CodeBuild
- `variables.tf` / `outputs.tf` – Config and post-apply values
- `templates/*.sh` – User data for database and app hosts
- `scripts/deploy-jar-to-ec2-ssm.sh` – Upload JAR to S3 and SSM on the app instance (used by CodeBuild deploy and by hand)
