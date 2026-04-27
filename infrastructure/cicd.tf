# -----------------------------------------------------------------------------
# CI: S3 for JAR artifacts, GitHub Actions OIDC role, SSM-based deploy, discover config
# -----------------------------------------------------------------------------

data "aws_caller_identity" "current" {}

# Globally-unique, non-public bucket; GitHub Action uploads, EC2 copies via SSM
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
  github_cicd_enabled    = trimspace(var.github_repository) != ""
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

# GitHub OIDC: if your apply errors with EntityAlreadyExists, the provider may already
# exist — import: terraform import 'aws_iam_openid_connect_provider.github[0]' <arn>
resource "aws_iam_openid_connect_provider" "github" {
  count           = local.github_cicd_enabled ? 1 : 0
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = var.github_actions_thumbprints
  tags = merge(
    local.common_tags,
    { Name = "${var.project}-github-oidc" }
  )
}

# GitHub → AWS (no static keys). Set github_repository = "" to skip the OIDC role and provider.
data "aws_iam_policy_document" "github_oidc" {
  count = local.github_cicd_enabled ? 1 : 0
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]
    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github[0].arn]
    }
    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }
    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = ["repo:${var.github_repository}:ref:refs/heads/main"]
    }
  }
}

data "aws_iam_policy_document" "github_actions_deploy" {
  count = local.github_cicd_enabled ? 1 : 0
  statement {
    effect = "Allow"
    actions = [
      "s3:PutObject",
      "s3:AbortMultipartUpload",
    ]
    resources = [
      "${aws_s3_bucket.app_artifacts.arn}/*",
    ]
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
  statement {
    effect = "Allow"
    actions = [
      "ssm:SendCommand",
      "ssm:ListCommandInvocations",
    ]
    resources = ["*"]
  }
  statement {
    effect = "Allow"
    actions = [
      "ssm:GetCommandInvocation",
    ]
    resources = [
      "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:*"
    ]
  }
  statement {
    effect = "Allow"
    actions = [
      "ssm:GetParameter",
      "ssm:GetParameters",
    ]
    resources = [local.ssm_param_path_arn]
  }
}

resource "aws_iam_role" "github_actions_deploy" {
  count              = local.github_cicd_enabled ? 1 : 0
  name               = "${var.project}-github-actions-deploy"
  assume_role_policy = data.aws_iam_policy_document.github_oidc[0].json
  tags = merge(
    local.common_tags,
    { Name = "${var.project}-github-actions-deploy" }
  )
}

resource "aws_iam_role_policy" "github_actions_deploy" {
  count  = local.github_cicd_enabled ? 1 : 0
  name   = "deploy"
  role   = aws_iam_role.github_actions_deploy[0].id
  policy = data.aws_iam_policy_document.github_actions_deploy[0].json
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
