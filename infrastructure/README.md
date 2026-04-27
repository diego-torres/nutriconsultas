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

## IAM: policies for an AWS user (or access key) that runs this Terraform

This project **creates and mutates** infrastructure in the account, including **IAM roles**, **instance profiles**, and **inline/attachment policies** for EC2, CodeBuild, and CodePipeline. The identity used for `terraform apply` must be able to do that. **Checking** CodeConnection status from the CLI (`ListConnections` / `GetConnection`) is included once that identity can manage those APIs.

**Option A (simple, typical for a single account owner or solo dev):**

- Attach the AWS managed policy **[`AdministratorAccess`](https://docs.aws.amazon.com/aws-managed-policy/latest/reference/AdministratorAccess.html)**.  
  This covers all services used below, `iam:*` (required for the roles Terraform creates), **CodeConnections** / developer tools, and the console. Rotate keys and do not share them.

**Option B (no full admin; two managed policies, often enough):**

1. **[`PowerUserAccess`](https://docs.aws.amazon.com/aws-managed-policy/latest/reference/PowerUserAccess.html)** – broad support for **EC2**, **S3**, **SSM**, **CodePipeline**, **CodeBuild**, **CodeStar Connections** / **CodeConnections**, **Route 53**, **CloudWatch Logs**, and most services this stack uses, while excluding a few global/security actions (e.g. IAM user and group *account* admin tasks PowerUser is designed to avoid).
2. **[`IAMFullAccess`](https://docs.aws.amazon.com/aws-managed-policy/latest/reference/IAMFullAccess.html)** – **required** for Terraform in this repo because it creates **roles**, **instance profiles**, and **inline policies** (e.g. SSM role for EC2, CodeBuild/CodePipeline service roles) and may create **service-linked role** requests when a service is used for the first time.  
  **Note:** `PowerUserAccess` alone is **not** enough (Terraform will fail on `aws_iam_role` and similar resources).

**Option C (least privilege):** have an administrator craft one or more **customer managed** policies that allow the exact `Create`/`Update`/`Delete`/`Read` / `Tagging` / `iam:PassRole` actions and resource ARNs for: **EC2**, **EBS**, **S3** (buckets, encryption, public access), **SSM** ( parameters ), **Route 53** (if `route53_domain` is set), **EIP**, **STS** ( `GetCallerIdentity` — usually allowed for any user ), **data-only** `Describe` on default VPC/AMI/subnets, **CodeStar Connections** / `codeconnections:*` and **`codestar-connections:`** (AWS still maps some APIs; watch `terraform apply` errors), **CodePipeline**, **CodeBuild**, **IAM** ( roles, instance profiles, policies, PassRole to `ec2`, `codebuild`, `codepipeline` ). This is more work to maintain; Option A or B is normal for a dedicated “infra” IAM user in one account.

**If you use a [remote S3 (and optional DynamoDB lock) state backend](https://developer.hashicorp.com/terraform/language/settings/backends/s3):** grant the same user **at least** read/write to that **state bucket and key prefix** and, if used, the **DynamoDB** lock table (in addition to the above). A separate tiny policy is common.

**This Terraform stack touches (for your own custom policy designs):** EC2, EBS, VPC **Describe** (default subnet/VPC/AMI), Security Groups, Elastic IP, S3 ( app artifacts + CodePipeline artifacts buckets ), SSM **Parameter** Store, optional Route 53 ( zone + records ), **CodeStar Connections** ( `aws_codestarconnections_connection` ), **CodeBuild** (two projects), **CodePipeline** (one pipeline), **CloudWatch Logs** (via CodeBuild and pipeline behavior ), **STS** (caller identity in Terraform data source).

**Not replaced by IAM:** completing the **CodeStar / GitHub connection** in the browser (OAuth) for the pipeline; policies only let APIs run.

## Configure and apply

1. **Copy the example variable file and edit (do not commit secrets):**
   ```bash
   cp terraform.tfvars.example terraform.tfvars
   # Edit: db password, region, optional domain, and (for CodePipeline) `github_repository`.
   ```
   For **CodePipeline** to be created, `github_repository` must be set to a non-empty `owner/repo` string in `terraform.tfvars`. If it is empty or missing, Terraform creates **no** pipeline; `aws codepipeline list-pipelines` will be empty.
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

#### Check connection status: AWS CLI and `gh` (optional)

**AWS CLI** (authoritative for the pipeline: same account and **Region** as the connection).

List GitHub connections and their status (replace the region with yours):

```bash
aws codestar-connections list-connections --region us-east-1 --provider-type-filter GitHub \
  --query "Connections[].[ConnectionName,ConnectionStatus,ConnectionArn]" --output table
```

Detail for the connection created by this stack (from the `infrastructure/` directory, after `apply` with `github_repository` set):

```bash
cd infrastructure
aws codestar-connections get-connection --region us-east-1 \
  --connection-arn "$(terraform output -raw codestar_connection_arn)"
```

If that command errors because the output is empty, set `github_repository` in Terraform and re-apply, or use an ARN from `list-connections` above.

`ConnectionStatus` is usually **`PENDING`** until you finish the GitHub authorization, then **`AVAILABLE`**. (Other values, e.g. **`ERROR`** or **`EXPIRED`**, mean the console flow or a reconnect is required.)

**GitHub CLI** (`gh`) does **not** read AWS. It is only useful to confirm you are logged into GitHub and, optionally, that a relevant **GitHub App** installation exists:

- `gh auth status` – confirms `gh` is logged into the right user/org account.
- Optional, user-level app installs: `gh api /user/installations` (or pipe through `jq` to inspect `app` names and `repository_selection`).
- If the repository is under a **GitHub Organization**, the **AWS Connector for GitHub** install is often on the **org**; an org admin may use `gh api /orgs/ORG_NAME/installations` (requires sufficient token scopes) or check **GitHub: Org settings → Installed GitHub Apps**.

**Summary:** Use **AWS** `get-connection` / `list-connections` to know if CodePipeline is allowed to pull. Use **`gh`** only to debug GitHub login or app install visibility on the GitHub side.

**If the CLI returns `AccessDeniedException` for `codeconnections:ListConnections`:** the IAM user or role your AWS CLI is using (for example a narrow user like `registry-user` used only for ECR) is missing `codeconnections:ListConnections` and `codeconnections:GetConnection`. Granting that **requires an account administrator** (or another identity with `iam:CreatePolicy` and `iam:AttachUserPolicy`, or the console equivalent). A restricted user **cannot** attach a policy to itself.

- **Policy body:** [iam/codeconnections-read.json](iam/codeconnections-read.json) in this repository.
- **File path and `file://`:** `aws` resolves `file://` relative to the shell’s **current working directory**, not the repo. Either `cd` to the **git repository root** (where the `infrastructure` folder lives), or use an **absolute** path, for example:
  - `file:///Users/yourname/Documents/GitHub/nutriconsultas/infrastructure/iam/codeconnections-read.json` (macOS/Linux), or
  - `file://$(pwd)/infrastructure/iam/codeconnections-read.json` after `cd` to the repo root.
- **Create and attach the policy (admin only):** use a profile with IAM admin rights, **not** the same credentials as the limited user, for example:
  - `export AWS_PROFILE=...`  # or `--profile` on each command  
  - `cd` to the repo root, then:  
    `aws iam create-policy --policy-name CodeConnectionsListRead --policy-document file://infrastructure/iam/codeconnections-read.json`  
  - `aws iam attach-user-policy --user-name registry-user --policy-arn <arn-from-create-policy-output>`

  If the CLI says **`AccessDenied` for `iam:CreatePolicy` or `iam:AttachUserPolicy`**, the AWS CLI is **still** using a non-admin identity (for example `registry-user`). You **cannot** get past that with the same user: have someone with IAM rights create/attach the policy, or use the **Console** (below). **Path note:** the JSON is at `infrastructure/iam/codeconnections-read.json` (from repo root). If you `cd infrastructure`, use `file://iam/codeconnections-read.json` instead.
- **Then** switch back to `registry-user` (or re-export credentials) to run `aws codestar-connections list-connections`.
- **No IAM CLI access?** In the **AWS web console** (as an account admin or IAM user that can manage policies), open **IAM → Policies → Create policy → JSON**, paste the contents of [iam/codeconnections-read.json](iam/codeconnections-read.json), name it (for example `CodeConnectionsListRead`), then **Users → `registry-user` → Add permissions → Add policies** and attach that policy. No `aws iam create-policy` is required.
- **Alternative:** an admin can create the same policy in the **IAM** console and attach it to `registry-user`, or you can use the **Developer tools -> Connections** console with a console-capable admin principal.

## What is *not* automated (by design, for simplicity and cost)

- **TLS/HTTPS**: optional **Certbot** on the app instance when `public_site_domain` and `certbot_admin_email` are set in tfvars (DNS must point to the EIP before apply). See `terraform.tfvars.example`.
- **Backups, observability, and high availability**: a single-EC2 database is a starting point, not a production backup strategy.

## Files

- `main.tf` – Security groups (no 22 on app), EC2, SSM instance profile, EIP, optional Route 53
- `cicd.tf` – S3 JAR bucket, SSM parameters for the deploy script
- `codepipeline.tf` – CodeStar Connection, S3 for pipeline artifacts, CodeBuild, CodePipeline (if `github_repository` is set)
- `iam/codeconnections-read.json` – optional IAM policy for `aws codestar-connections list/get-connection` (not required for the pipeline service roles)
- `buildspecs/app-build.yml`, `buildspecs/app-deploy.yml` – Build and deploy for CodeBuild
- `variables.tf` / `outputs.tf` – Config and post-apply values
- `templates/*.sh` – User data for database and app hosts; `templates/nutriconsultas-nginx.conf.tpl` – nginx `server` block (Terraform `templatefile`, Certbot-friendly `server_name`)
- `scripts/deploy-jar-to-ec2-ssm.sh` – Upload JAR to S3 and SSM on the app instance (used by CodeBuild deploy and by hand)
