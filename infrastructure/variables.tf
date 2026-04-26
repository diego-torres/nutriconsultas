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
# SSH: restrict to your home or office IP (CIDR) — never use 0.0.0.0/0 for 22
# -----------------------------------------------------------------------------

variable "admin_ssh_cidr" {
  type        = string
  description = "IPv4 CIDR allowed to reach SSH (port 22) on the app instance. Example: 203.0.113.10/32. Database instance has no SSH from the internet; use SSM Session Manager if needed."
  default     = "127.0.0.1/32" # must be set for real use
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
# Key pair: create one in the EC2 console in this region and set ec2_key_name, or
# set create_key_pair = true to generate a key in Terraform (private key in output).
# You can also omit both and use only SSM Session Manager to reach the app host.
# -----------------------------------------------------------------------------

variable "ec2_key_name" {
  type        = string
  description = "Name of an existing EC2 key pair in this region (app instance only)."
  default     = null
}

variable "create_key_pair" {
  type        = bool
  description = "If true, create an EC2 key pair; private key in outputs (sensitive). Ignored if ec2_key_name is set."
  default     = true
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
