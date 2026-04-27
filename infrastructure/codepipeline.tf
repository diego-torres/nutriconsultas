# CodePipeline: GitHub (CodeStar Connection) -> CodeBuild (Maven) -> CodeBuild (S3 + SSM to EC2).
# Set github_repository = "org/repo" in tfvars. Leave empty to skip the pipeline, connection, and build projects.

locals {
  codepipeline_enabled      = trimspace(var.github_repository) != ""
  pipeline_artifacts_bucket = "${var.project}-cp-artifacts-${data.aws_caller_identity.current.account_id}"
}

# -----------------------------------------------------------------------------
# S3: pipeline default artifact store
# -----------------------------------------------------------------------------

resource "aws_s3_bucket" "codepipeline" {
  count  = local.codepipeline_enabled ? 1 : 0
  bucket = local.pipeline_artifacts_bucket
  tags   = merge(local.common_tags, { Name = "${var.project}-codepipeline" })
}

resource "aws_s3_bucket_public_access_block" "codepipeline" {
  count  = local.codepipeline_enabled ? 1 : 0
  bucket = aws_s3_bucket.codepipeline[0].id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "codepipeline" {
  count  = local.codepipeline_enabled ? 1 : 0
  bucket = aws_s3_bucket.codepipeline[0].id
  rule {
    apply_server_side_encryption_by_default { sse_algorithm = "AES256" }
  }
}

# -----------------------------------------------------------------------------
# CodeStar Connection (GitHub): complete in AWS Console (Developer tools > Connections)
# -----------------------------------------------------------------------------

resource "aws_codestarconnections_connection" "github" {
  count         = local.codepipeline_enabled ? 1 : 0
  name          = "${var.project}-github"
  provider_type = "GitHub"
  tags          = merge(local.common_tags, { Name = "${var.project}-github" })
}

# -----------------------------------------------------------------------------
# IAM: CodeBuild (deploy) - S3 JAR + SSM (same surface as the old GHA role)
# -----------------------------------------------------------------------------

data "aws_iam_policy_document" "codebuild_app_deploy" {
  count = local.codepipeline_enabled ? 1 : 0
  statement {
    effect    = "Allow"
    actions   = ["s3:PutObject", "s3:AbortMultipartUpload"]
    resources = ["${aws_s3_bucket.app_artifacts.arn}/*"]
  }
  statement {
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.app_artifacts.arn]
    condition {
      test     = "StringLike"
      variable = "s3:prefix"
      values   = ["releases/"]
    }
  }
  statement {
    effect    = "Allow"
    actions   = ["ssm:SendCommand", "ssm:ListCommandInvocations"]
    resources = ["*"]
  }
  statement {
    effect = "Allow"
    actions = [
      "ssm:GetCommandInvocation",
    ]
    resources = ["arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:*"]
  }
  statement {
    effect    = "Allow"
    actions   = ["ssm:GetParameter", "ssm:GetParameters"]
    resources = [local.ssm_param_path_arn]
  }
}

data "aws_iam_policy_document" "codebuild_assume" {
  count = local.codepipeline_enabled ? 1 : 0
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["codebuild.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "codebuild_build_artifacts" {
  count = local.codepipeline_enabled ? 1 : 0
  statement {
    effect = "Allow"
    actions = [
      "s3:PutObject",
      "s3:AbortMultipartUpload",
      "s3:GetObject",
      "s3:GetObjectVersion",
    ]
    resources = [
      "${aws_s3_bucket.codepipeline[0].arn}",
      "${aws_s3_bucket.codepipeline[0].arn}/*",
    ]
  }
  statement {
    effect    = "Allow"
    actions   = ["s3:ListBucket"]
    resources = [aws_s3_bucket.codepipeline[0].arn]
  }
  statement {
    effect = "Allow"
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
    ]
    resources = ["arn:aws:logs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:*"]
  }
}

data "aws_iam_policy_document" "codebuild_deploy_cwl" {
  count = local.codepipeline_enabled ? 1 : 0
  statement {
    effect = "Allow"
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents",
    ]
    resources = ["arn:aws:logs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:*"]
  }
}

resource "aws_iam_role" "codebuild_build" {
  count              = local.codepipeline_enabled ? 1 : 0
  name               = "${var.project}-codebuild-build"
  assume_role_policy = data.aws_iam_policy_document.codebuild_assume[0].json
  tags               = merge(local.common_tags, { Name = "${var.project}-codebuild-build" })
}

resource "aws_iam_role_policy" "codebuild_build" {
  count  = local.codepipeline_enabled ? 1 : 0
  name   = "pipeline"
  role   = aws_iam_role.codebuild_build[0].id
  policy = data.aws_iam_policy_document.codebuild_build_artifacts[0].json
}

resource "aws_iam_role" "codebuild_deploy" {
  count              = local.codepipeline_enabled ? 1 : 0
  name               = "${var.project}-codebuild-deploy"
  assume_role_policy = data.aws_iam_policy_document.codebuild_assume[0].json
  tags               = merge(local.common_tags, { Name = "${var.project}-codebuild-deploy" })
}

resource "aws_iam_role_policy" "codebuild_deploy" {
  count  = local.codepipeline_enabled ? 1 : 0
  name   = "deploy"
  role   = aws_iam_role.codebuild_deploy[0].id
  policy = data.aws_iam_policy_document.codebuild_app_deploy[0].json
}

resource "aws_iam_role_policy" "codebuild_deploy_cwl" {
  count  = local.codepipeline_enabled ? 1 : 0
  name   = "cwl"
  role   = aws_iam_role.codebuild_deploy[0].id
  policy = data.aws_iam_policy_document.codebuild_deploy_cwl[0].json
}

# -----------------------------------------------------------------------------
# CodeBuild projects
# -----------------------------------------------------------------------------

resource "aws_codebuild_project" "app_build" {
  count         = local.codepipeline_enabled ? 1 : 0
  name          = "${var.project}-app-build"
  description   = "Maven build for nutriconsultas (CodePipeline stage)"
  build_timeout = 40
  service_role  = aws_iam_role.codebuild_build[0].arn
  tags          = local.common_tags

  artifacts { type = "CODEPIPELINE" }
  source {
    type      = "CODEPIPELINE"
    buildspec = "infrastructure/buildspecs/app-build.yml"
  }
  environment {
    compute_type                = "BUILD_GENERAL1_SMALL"
    image                       = "aws/codebuild/standard:7.0"
    type                        = "LINUX_CONTAINER"
    image_pull_credentials_type = "CODEBUILD"
    privileged_mode             = false
  }
}

resource "aws_codebuild_project" "app_deploy" {
  count         = local.codepipeline_enabled ? 1 : 0
  name          = "${var.project}-app-deploy"
  description   = "S3 + SSM deploy to app EC2"
  build_timeout = 30
  service_role  = aws_iam_role.codebuild_deploy[0].arn
  tags          = local.common_tags

  artifacts { type = "CODEPIPELINE" }
  source {
    type      = "CODEPIPELINE"
    buildspec = file("${path.module}/buildspecs/app-deploy.yml")
  }
  environment {
    compute_type                = "BUILD_GENERAL1_SMALL"
    image                       = "aws/codebuild/standard:7.0"
    type                        = "LINUX_CONTAINER"
    image_pull_credentials_type = "CODEBUILD"
    privileged_mode             = false
    environment_variable {
      name  = "NUTR_PROJECT"
      value = var.project
    }
  }
}

# -----------------------------------------------------------------------------
# CodePipeline: assume role, pipeline
# -----------------------------------------------------------------------------

data "aws_iam_policy_document" "codepipeline_assume" {
  count = local.codepipeline_enabled ? 1 : 0
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["codepipeline.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "codepipeline_policy" {
  count = local.codepipeline_enabled ? 1 : 0
  statement {
    effect = "Allow"
    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:PutObjectAcl",
    ]
    resources = [
      aws_s3_bucket.codepipeline[0].arn,
      "${aws_s3_bucket.codepipeline[0].arn}/*"
    ]
  }
  statement {
    effect    = "Allow"
    actions   = ["s3:ListBucket", "s3:GetBucketVersioning", "s3:GetBucketLocation"]
    resources = [aws_s3_bucket.codepipeline[0].arn]
  }
  statement {
    effect = "Allow"
    actions = [
      "codebuild:StartBuild",
      "codebuild:StopBuild",
      "codebuild:BatchGetBuilds",
      "codebuild:BatchGetProjects",
    ]
    resources = [
      aws_codebuild_project.app_build[0].arn,
      aws_codebuild_project.app_deploy[0].arn,
    ]
  }
  statement {
    effect    = "Allow"
    actions   = ["iam:PassRole"]
    resources = [aws_iam_role.codebuild_build[0].arn, aws_iam_role.codebuild_deploy[0].arn]
  }
  statement {
    effect    = "Allow"
    actions   = ["codestar-connections:UseConnection"]
    resources = [aws_codestarconnections_connection.github[0].arn]
  }
}

resource "aws_iam_role" "codepipeline" {
  count              = local.codepipeline_enabled ? 1 : 0
  name               = "${var.project}-codepipeline"
  assume_role_policy = data.aws_iam_policy_document.codepipeline_assume[0].json
  tags               = merge(local.common_tags, { Name = "${var.project}-codepipeline" })
}

resource "aws_iam_role_policy" "codepipeline" {
  count  = local.codepipeline_enabled ? 1 : 0
  name   = "pipeline"
  role   = aws_iam_role.codepipeline[0].id
  policy = data.aws_iam_policy_document.codepipeline_policy[0].json
}

resource "aws_codepipeline" "app" {
  count    = local.codepipeline_enabled ? 1 : 0
  name     = "${var.project}-app"
  role_arn = aws_iam_role.codepipeline[0].arn
  tags     = local.common_tags

  artifact_store {
    location = aws_s3_bucket.codepipeline[0].id
    type     = "S3"
  }

  stage {
    name = "Source"
    action {
      name             = "GitHub"
      category         = "Source"
      owner            = "AWS"
      version          = "1"
      provider         = "CodeStarSourceConnection"
      output_artifacts = ["source_output"]
      run_order        = 1
      configuration = {
        ConnectionArn        = aws_codestarconnections_connection.github[0].arn
        FullRepositoryId     = var.github_repository
        BranchName           = var.codepipeline_branch
        OutputArtifactFormat = "CODE_ZIP"
      }
    }
  }

  stage {
    name = "Build"
    action {
      name             = "Maven"
      category         = "Build"
      owner            = "AWS"
      version          = "1"
      provider         = "CodeBuild"
      input_artifacts  = ["source_output"]
      output_artifacts = ["build_output"]
      run_order        = 1
      configuration    = { ProjectName = aws_codebuild_project.app_build[0].name }
    }
  }

  stage {
    name = "Deploy"
    action {
      name            = "Ssm"
      category        = "Build"
      owner           = "AWS"
      version         = "1"
      provider        = "CodeBuild"
      input_artifacts = ["build_output"]
      run_order       = 1
      configuration   = { ProjectName = aws_codebuild_project.app_deploy[0].name }
    }
  }
}
