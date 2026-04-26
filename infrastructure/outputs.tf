output "app_public_ip" {
  description = "Elastic IP of the app instance (use for testing before DNS is live)."
  value       = aws_eip.app.public_ip
}

output "app_instance_id" {
  description = "App EC2 instance id (e.g. for SSM: aws ssm start-session --target <id>)."
  value       = aws_instance.app.id
}

output "db_instance_id" {
  description = "Database EC2 instance id (SSM: no public SSH; use Session Manager after the agent is online)."
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

output "ec2_key_pair_private_pem" {
  description = "PEM of the generated key (only if create_key_pair = true and ec2_key_name is not set). Save to a file and chmod 600; never commit."
  value       = try(tls_private_key.this[0].private_key_pem, null)
  sensitive   = true
}

output "route53_name_servers" {
  description = "If route53_domain is set, add these as nameservers in your registrar (see domain-setup.md). Empty when Route 53 is not created."
  value       = length(aws_route53_zone.app) > 0 ? aws_route53_zone.app[0].name_servers : []
}

output "route53_domain" {
  value       = var.route53_domain
  description = "The zone name if created; otherwise null."
}
