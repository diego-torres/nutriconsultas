# -----------------------------------------------------------------------------
# General
# -----------------------------------------------------------------------------

variable "aws_region" {
  type        = string
  description = "AWS region (e.g. us-east-1 — often the lowest-cost option for many services)."
  default     = "us-east-1"
}

variable "project" {
  type        = string
  description = "Name prefix for resource names and common tags (cost/ownership tracking)."
  default     = "nutriconsultas"
}

# -----------------------------------------------------------------------------
# Instance sizing (free tier / low cost)
# - New accounts: 750 hours/month of t2/t3.micro Linux for 12 months; two instances
#   running 24/7 typically exceeds that combined allowance — expect partial charges.
# -----------------------------------------------------------------------------

variable "db_instance_type" {
  type        = string
  description = "EC2 for PostgreSQL. t3.micro is a common free-tier class for new accounts."
  default     = "t3.micro"
}

variable "app_instance_type" {
  type        = string
  description = "EC2 for the Java application. t3.micro is a common free-tier class for new accounts."
  default     = "t3.small"
}

variable "ebs_root_volume_size_gb" {
  type        = number
  description = "Root EBS size (GB) for both instances. 30 is within typical free-tier storage (new accounts) when eligible."
  default     = 30
}

# -----------------------------------------------------------------------------
# PostgreSQL bootstrap
# -----------------------------------------------------------------------------

variable "db_name" {
  type        = string
  description = "Name of the PostgreSQL database to create (non-sensitive)."
  default     = "nutriconsultas"
}

variable "db_app_username" {
  type        = string
  description = "Application database user to create in PostgreSQL."
  default     = "nutriapp"
}

variable "db_app_password" {
  type        = string
  description = "Password for db_app_username. Set via TF_VAR_db_app_password or a local (gitignored) tfvars file. Never commit."
  sensitive   = true
}

# -----------------------------------------------------------------------------
# Application secrets (written to /opt/nutriconsultas/app.env on first boot)
# - Values are base64-encoded into user_data by Terraform and decoded on the host
#   (safe for special characters). Changing these replaces the app instance when
#   user_data_replace_on_change is true.
# - EC2 user_data is visible to principals with ec2:DescribeInstances; prefer
#   AWS Secrets Manager or SSM Parameter Store for stricter production controls.
# -----------------------------------------------------------------------------

variable "auth_client" {
  type        = string
  description = "Auth0 application Client ID (spring.security.oauth2.client.registration.auth0.client-id)."
  sensitive   = true
}

variable "auth_secret" {
  type        = string
  description = "Auth0 application Client Secret."
  sensitive   = true
}

variable "auth_issuer" {
  type        = string
  description = "Auth0 issuer URI, e.g. https://YOUR_TENANT.auth0.com/ (include trailing slash if your Auth0 docs show it)."
  sensitive   = true
}

variable "auth_audience" {
  type        = string
  description = "Auth0 API identifier for patient mobile JWT validation (app.security.jwt.audience / AUTH_AUDIENCE). Must match mobile AUTH0_AUDIENCE."
  sensitive   = false
}

variable "auth0_mgmt_client_id" {
  type        = string
  description = "Auth0 M2M Client ID for Management API (AUTH0_MGMT_CLIENT_ID). Enables patient mobile linkage by email."
  default     = ""
  sensitive   = true
}

variable "auth0_mgmt_client_secret" {
  type        = string
  description = "Auth0 M2M Client Secret for Management API (AUTH0_MGMT_CLIENT_SECRET)."
  default     = ""
  sensitive   = true
}

variable "auth0_mgmt_domain" {
  type        = string
  description = "Auth0 Management API domain (AUTH0_MGMT_DOMAIN). Defaults to auth_issuer when empty."
  default     = ""
  sensitive   = false
}

variable "auth0_mobile_native_client_id" {
  type        = string
  description = "Auth0 Native app client ID for patient mobile signup (/dbconnections/signup). AUTH0_MOBILE_NATIVE_CLIENT_ID."
  default     = ""
  sensitive   = true
}

variable "auth0_patient_broker_client_id" {
  type        = string
  description = "Confidential Auth0 app client ID for server-side password-realm login. AUTH0_PATIENT_BROKER_CLIENT_ID."
  default     = ""
  sensitive   = true
}

variable "auth0_patient_broker_client_secret" {
  type        = string
  description = "Confidential Auth0 app client secret for patient auth broker. AUTH0_PATIENT_BROKER_CLIENT_SECRET."
  default     = ""
  sensitive   = true
}

variable "auth0_patient_broker_domain" {
  type        = string
  description = "Auth0 tenant domain for patient broker (AUTH0_PATIENT_BROKER_DOMAIN). Defaults to auth_issuer when empty."
  default     = ""
  sensitive   = false
}

variable "auth0_patient_broker_connection" {
  type        = string
  description = "Auth0 database connection name for patient signup/login. AUTH0_PATIENT_BROKER_CONNECTION."
  default     = "Username-Password-Authentication"
  sensitive   = false
}

variable "aws_bucket" {
  type        = string
  description = "S3 bucket name for application uploads (amazon.s3.bucket). Terraform creates this bucket (see s3-app-uploads.tf). If it already exists, run: terraform import aws_s3_bucket.app_uploads NAME"
  sensitive   = false
}

variable "aws_key" {
  type        = string
  description = "AWS access key id for S3 (amazon.s3.key)."
  sensitive   = true
}

variable "aws_secret" {
  type        = string
  description = "AWS secret access key for S3 (amazon.s3.secret)."
  sensitive   = true
}

variable "maps_key" {
  type        = string
  description = "Google Maps API key (maps.api.key). Optional: use empty string if unused."
  default     = ""
  sensitive   = true
}

variable "recaptcha_secret_key" {
  type        = string
  description = "reCAPTCHA secret (recaptcha.secret-key). Optional: use empty string if unused."
  default     = ""
  sensitive   = true
}

variable "recaptcha_site_key" {
  type        = string
  description = "reCAPTCHA v2 site key (recaptcha.site-key / RECAPTCHA_SITE_KEY). Public widget key paired with recaptcha_secret_key."
  default     = ""
  sensitive   = false
}

variable "platform_admin_emails" {
  type        = list(string)
  description = "OAuth login emails granted platform admin (contact inquiries inbox). Written to app.env as PLATFORM_ADMIN_EMAILS (comma-separated)."
  default     = []
  sensitive   = true
}

variable "payment_provider" {
  type        = string
  description = "Subscription payment provider id (PAYMENT_PROVIDER). Default stripe (#207)."
  default     = "stripe"
  sensitive   = false
}

variable "stripe_secret_key" {
  type        = string
  description = "Stripe secret API key (STRIPE_SECRET_KEY). Never commit."
  default     = ""
  sensitive   = true
}

variable "stripe_webhook_secret" {
  type        = string
  description = "Stripe webhook signing secret (STRIPE_WEBHOOK_SECRET). Optional until Dashboard webhook is configured (#208)."
  default     = ""
  sensitive   = true
}

# -----------------------------------------------------------------------------
# HTTPS (Let's Encrypt Certbot on the app EC2; DNS must point apex/aliases to the app EIP first)
# -----------------------------------------------------------------------------

variable "public_site_domain" {
  type        = string
  description = "Primary hostname for nginx server_name and Certbot (e.g. minutriporcion.com). Empty keeps server_name _ and skips Certbot."
  default     = ""
}

variable "public_site_domain_aliases" {
  type        = list(string)
  description = "Extra hostnames for nginx and Certbot (e.g. [\"www.minutriporcion.com\"]). DNS A/AAAA or CNAME for each must reach the app EIP."
  default     = []
}

variable "certbot_admin_email" {
  type        = string
  description = "Let's Encrypt registration email (ACME). Required to run Certbot when public_site_domain is set. Empty skips Certbot (run manually later via SSM)."
  default     = ""
  sensitive   = true
}

# -----------------------------------------------------------------------------
# Optional Route 53: create hosted zone + records (see domain-setup.md for GoDaddy)
# -----------------------------------------------------------------------------

variable "route53_domain" {
  type        = string
  description = "If set (e.g. minutriporcion.com), create a Route 53 public zone and A records for @ and (optionally) www. If null, no DNS. Cost: hosted zone is about $0.50/mo; free tier does not include Route 53."
  default     = null
}

variable "route53_www_record" {
  type        = bool
  description = "If route53_domain is set, add an A record for www to the app EIP. (Same as apex; for HTTP www→root redirect, configure the app or reverse proxy later.)"
  default     = true
}

# -----------------------------------------------------------------------------
# Amazon SES — nutritionist invitation email (#209)
# -----------------------------------------------------------------------------

variable "enable_ses" {
  type        = bool
  description = "Create SES domain identity, DKIM DNS (when Route 53 zone exists), and IAM send policy on the app EC2 role."
  default     = true
}

variable "ses_mail_domain" {
  type        = string
  description = "Domain for SES identity and DKIM. Defaults to route53_domain or public_site_domain when empty."
  default     = ""
}

variable "invitation_mail_from" {
  type        = string
  description = "From address for nutritionist invitation emails (MAIL_FROM / app env)."
  default     = "invites@minutriporcion.com"
}

# -----------------------------------------------------------------------------
# CodePipeline: GitHub source (CodeStar Connection) + CodeBuild. Leave empty to skip.
# -----------------------------------------------------------------------------

variable "github_repository" {
  type        = string
  description = "GitHub repo in org/name form (e.g. diego-torres/nutriconsultas) for CodeStar Connection and CodePipeline source. Empty = no CodePipeline, connection, or CodeBuild project."
  default     = ""
}

variable "codepipeline_branch" {
  type        = string
  description = "Branch that triggers the pipeline on push (CodePipeline V2 Git trigger `includes`) and is checked out in the Source stage."
  default     = "main"
}
