output "app_public_ip" {
  description = "Elastic IP of the app instance (use for testing before DNS is live)."
  value       = aws_eip.app.public_ip
}

output "app_instance_id" {
  description = "App EC2 instance id (e.g. for SSM: aws ssm start-session --target <id>)."
  value       = aws_instance.app.id
}

output "db_instance_id" {
  description = "PostgreSQL host EC2 instance id (SSM Session Manager: aws ssm start-session --target <id>)."
  value       = aws_instance.db.id
}

output "db_private_ip" {
  description = "Private IPv4 of the database (5432 is open only to the app security group)."
  value       = aws_instance.db.private_ip
}

output "jdbc_example" {
  description = "Example JDBC URL for application.properties (password is the value you set for db_app_password; do not commit secrets)."
  value       = "jdbc:postgresql://${aws_instance.db.private_ip}:5432/${var.db_name}"
}

output "route53_name_servers" {
  description = "If route53_domain is set, add these as nameservers in your registrar (see domain-setup.md). Empty when Route 53 is not created."
  value       = length(aws_route53_zone.app) > 0 ? aws_route53_zone.app[0].name_servers : []
}

output "route53_domain" {
  value       = var.route53_domain
  description = "The zone name if created; otherwise null."
}

# -----------------------------------------------------------------------------
# CI/CD: copy into GitHub repository **Variables** (not secrets) after apply
# -----------------------------------------------------------------------------

output "s3_app_artifact_bucket" {
  description = "S3 bucket for the JAR. Keys also in SSM (project/deploy s3_bucket and s3_key parameters)."
  value       = aws_s3_bucket.app_artifacts.bucket
}

output "s3_app_artifact_key" {
  description = "S3 key for the JAR; also in SSM."
  value       = local.deploy_artifact_s3_key
}

output "github_actions_oidc_role_arn" {
  description = "Set as GitHub repository variable GITHUB_AWS_OIDC_DEPLOY_ROLE_ARN when github_repository is set. Empty if OIDC was not created."
  value       = try(aws_iam_role.github_actions_deploy[0].arn, "")
}
