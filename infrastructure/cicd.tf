# -----------------------------------------------------------------------------
# CI: S3 JAR bucket, SSM parameters, CodePipeline in codepipeline.tf
# -----------------------------------------------------------------------------

data "aws_caller_identity" "current" {}

# Globally-unique, non-public bucket; CodeBuild deploy uploads, EC2 copies via SSM
resource "aws_s3_bucket" "app_artifacts" {
  bucket = "${var.project}-app-jar-${data.aws_caller_identity.current.account_id}"

  tags = merge(
    local.common_tags,
    { Name = "${var.project}-app-jar" }
  )
}

resource "aws_s3_bucket_versioning" "app_artifacts" {
  bucket = aws_s3_bucket.app_artifacts.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "app_artifacts" {
  bucket = aws_s3_bucket.app_artifacts.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "app_artifacts" {
  bucket                  = aws_s3_bucket.app_artifacts.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

locals {
  deploy_artifact_s3_key = "releases/nutriconsultas-web.jar"
  ssm_prefix             = "/${var.project}/deploy"
  ssm_param_path_arn     = "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter/${trimprefix(local.ssm_prefix, "/")}/*"
}

# EC2: pull the published JAR from S3 during SSM deploy (instance profile, no long-lived keys)
data "aws_iam_policy_document" "ec2_s3_deploy" {
  statement {
    effect = "Allow"
    actions = [
      "s3:GetObject",
    ]
    resources = ["${aws_s3_bucket.app_artifacts.arn}/${local.deploy_artifact_s3_key}"]
  }
  statement {
    effect = "Allow"
    actions = [
      "s3:ListBucket",
    ]
    resources = [aws_s3_bucket.app_artifacts.arn]
    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["releases/"]
    }
  }
}

resource "aws_iam_role_policy" "ssm_s3_artifacts" {
  name   = "${var.project}-ssm-s3-jar"
  role   = aws_iam_role.ssm.id
  policy = data.aws_iam_policy_document.ec2_s3_deploy.json
}

resource "aws_ssm_parameter" "s3_bucket" {
  name  = "${local.ssm_prefix}/s3_bucket"
  type  = "String"
  value = aws_s3_bucket.app_artifacts.bucket
  tags  = local.common_tags
}

resource "aws_ssm_parameter" "s3_key" {
  name  = "${local.ssm_prefix}/s3_key"
  type  = "String"
  value = local.deploy_artifact_s3_key
  tags  = local.common_tags
}

resource "aws_ssm_parameter" "app_instance_id" {
  name  = "${local.ssm_prefix}/app_instance_id"
  type  = "String"
  value = aws_instance.app.id
  tags  = local.common_tags
}

resource "aws_ssm_parameter" "aws_region" {
  name  = "${local.ssm_prefix}/aws_region"
  type  = "String"
  value = var.aws_region
  tags  = local.common_tags
}
