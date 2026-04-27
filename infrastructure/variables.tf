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
  default     = "t3.micro"
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
# CodePipeline: GitHub source (CodeStar Connection) + CodeBuild. Leave empty to skip.
# -----------------------------------------------------------------------------

variable "github_repository" {
  type        = string
  description = "GitHub repo in org/name form (e.g. diego-torres/nutriconsultas) for CodeStar Connection and CodePipeline source. Empty = no CodePipeline, connection, or CodeBuild project."
  default     = ""
}

variable "codepipeline_branch" {
  type        = string
  description = "Branch that triggers the pipeline (pushes) and is checked out in the build stage."
  default     = "main"
}
