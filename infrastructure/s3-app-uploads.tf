# -----------------------------------------------------------------------------
# Application uploads bucket (Spring amazon.s3.bucket = var.aws_bucket)
# If this bucket already exists in the account, import before first apply:
#   terraform import aws_s3_bucket.app_uploads YOUR_BUCKET_NAME
# -----------------------------------------------------------------------------

resource "aws_s3_bucket" "app_uploads" {
  bucket = var.aws_bucket

  tags = merge(
    local.common_tags,
    { Name = "${var.project}-app-uploads" }
  )
}

resource "aws_s3_bucket_public_access_block" "app_uploads" {
  bucket = aws_s3_bucket.app_uploads.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "app_uploads" {
  bucket = aws_s3_bucket.app_uploads.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}
