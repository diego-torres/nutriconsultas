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
# GitHub Actions: OIDC for deploy job (S3 put + SSM to app). Leave empty to skip the role.
# -----------------------------------------------------------------------------

variable "github_repository" {
  type        = string
  description = "Set to the GitHub repo in org/repo form (e.g. diego-torres/nutriconsultas) so the deploy workflow on main can assume a role. Empty string = do not create OIDC provider/role (S3 and SSM parameters are still created)."
  default     = ""
}

variable "github_actions_thumbprints" {
  type        = list(string)
  description = "Root CA thumbprints for https://token.actions.githubusercontent.com. Add entries if GitHub publishes new certificates."
  default     = ["6938fd4e98bab03faadb97b34396831e3780aea1"]
}
