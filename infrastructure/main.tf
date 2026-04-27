# -----------------------------------------------------------------------------
# Data: default VPC (no NAT/IGW custom charges — use account default network)
# -----------------------------------------------------------------------------

data "aws_vpc" "this" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.this.id]
  }
}

data "aws_ami" "al2023" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-kernel-*-x86_64"]
  }

  filter {
    name   = "state"
    values = ["available"]
  }
}

locals {
  # Avoid committing secrets: override tfvars; escape for SQL single-quoted strings
  db_password_escaped = replace(
    replace(var.db_app_password, "\\", "\\\\"),
    "'",
    "''"
  )

  common_tags = {
    Project   = var.project
    ManagedBy = "terraform"
  }

  subnet_id = data.aws_subnets.default.ids[0]
}

# -----------------------------------------------------------------------------
# Security groups: no inbound port 22 — admin only via SSM Session Manager. DB: 5432 from app only.
# -----------------------------------------------------------------------------

resource "aws_security_group" "app" {
  name_prefix = "${var.project}-app-"
  description = "App EC2: HTTP, HTTPS, admin via SSM only; can reach DB on 5432"
  vpc_id      = data.aws_vpc.this.id

  # Public web
  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(
    local.common_tags,
    { Name = "${var.project}-app" }
  )

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_security_group" "db" {
  name_prefix = "${var.project}-db-"
  description = "PostgreSQL: only the app may connect on 5432"
  vpc_id      = data.aws_vpc.this.id

  # Least privilege: only from the app’s security group
  ingress {
    description     = "PostgreSQL from app only"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(
    local.common_tags,
    { Name = "${var.project}-db" }
  )

  lifecycle {
    create_before_destroy = true
  }
}

# -----------------------------------------------------------------------------
# SSM: required for app and DB (no public SSH; security group does not open port 22)
# -----------------------------------------------------------------------------

data "aws_iam_policy_document" "ec2_trust" {
  statement {
    effect = "Allow"
    actions = [
      "sts:AssumeRole"
    ]
    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "ssm" {
  name               = "${var.project}-ssm"
  assume_role_policy = data.aws_iam_policy_document.ec2_trust.json
  tags               = local.common_tags
}

resource "aws_iam_role_policy_attachment" "ssm" {
  role       = aws_iam_role.ssm.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ssm" {
  name = "${var.project}-ssm"
  role = aws_iam_role.ssm.name
  tags = local.common_tags
}

# -----------------------------------------------------------------------------
# Database EC2 (no public IP)
# -----------------------------------------------------------------------------

resource "aws_instance" "db" {
  ami                    = data.aws_ami.al2023.id
  instance_type          = var.db_instance_type
  subnet_id              = local.subnet_id
  vpc_security_group_ids = [aws_security_group.db.id]
  key_name               = null
  # Public address so the instance can reach the internet for updates and SSM, without
  # a NAT gateway. Security group allows only 5432 from the app, not the open internet.
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.ssm.id

  root_block_device {
    volume_size           = var.ebs_root_volume_size_gb
    volume_type           = "gp3"
    delete_on_termination = true
    encrypted             = true
  }

  user_data = base64encode(
    templatefile(
      "${path.module}/templates/postgresql.user_data.sh",
      {
        db_name     = var.db_name
        db_user     = var.db_app_username
        db_password = local.db_password_escaped
        vpc_cidr    = data.aws_vpc.this.cidr_block
      }
    )
  )

  metadata_options {
    http_tokens = "required" # IMDSv2
  }

  tags = merge(
    local.common_tags,
    { Name = "${var.project}-postgresql" }
  )
}

# -----------------------------------------------------------------------------
# App EC2 + Elastic IP (stable address for A record and JDBC from app to DB)
# -----------------------------------------------------------------------------

resource "aws_instance" "app" {
  depends_on = [aws_instance.db]

  ami                    = data.aws_ami.al2023.id
  instance_type          = var.app_instance_type
  subnet_id              = local.subnet_id
  vpc_security_group_ids = [aws_security_group.app.id]
  # No key pair: shell access is only via SSM (Session Manager). Port 22 is not in the app SG.
  key_name                    = null
  associate_public_ip_address = true
  iam_instance_profile        = aws_iam_instance_profile.ssm.id

  root_block_device {
    volume_size           = var.ebs_root_volume_size_gb
    volume_type           = "gp3"
    delete_on_termination = true
    encrypted             = true
  }

  user_data = base64encode(
    templatefile(
      "${path.module}/templates/app.user_data.sh",
      {
        # JDBC: DB private IP, same default VPC; password via base64 in user_data (treat as secret)
        db_private_ip   = aws_instance.db.private_ip
        db_name         = var.db_name
        db_user         = var.db_app_username
        db_password_b64 = base64encode(var.db_app_password)
      }
    )
  )

  metadata_options {
    http_tokens = "required"
  }

  tags = merge(
    local.common_tags,
    { Name = "${var.project}-app" }
  )
}

resource "aws_eip" "app" {
  domain   = "vpc"
  instance = aws_instance.app.id
  tags = merge(
    local.common_tags,
    { Name = "${var.project}-app-eip" }
  )
}

# -----------------------------------------------------------------------------
# Optional: Route 53
# -----------------------------------------------------------------------------

resource "aws_route53_zone" "app" {
  count = var.route53_domain == null ? 0 : 1
  name  = var.route53_domain
  tags  = local.common_tags
}

resource "aws_route53_record" "apex" {
  count   = var.route53_domain == null ? 0 : 1
  zone_id = aws_route53_zone.app[0].zone_id
  name    = var.route53_domain
  type    = "A"
  ttl     = 300
  records = [aws_eip.app.public_ip]
}

resource "aws_route53_record" "www" {
  count   = (var.route53_domain == null || !var.route53_www_record) ? 0 : 1
  zone_id = aws_route53_zone.app[0].zone_id
  name    = "www.${var.route53_domain}"
  type    = "A"
  ttl     = 300
  records = [aws_eip.app.public_ip]
}
