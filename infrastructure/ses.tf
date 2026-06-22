# -----------------------------------------------------------------------------
# Amazon SES — nutritionist invitation email (#209)
# -----------------------------------------------------------------------------

locals {
  ses_mail_domain = trimspace(var.ses_mail_domain) != "" ? trimspace(var.ses_mail_domain) : (
    var.route53_domain != null ? var.route53_domain : trimspace(var.public_site_domain)
  )
  ses_enabled = var.enable_ses && local.ses_mail_domain != ""
}

resource "aws_ses_domain_identity" "invitations" {
  count  = local.ses_enabled ? 1 : 0
  domain = local.ses_mail_domain
}

resource "aws_ses_domain_dkim" "invitations" {
  count  = local.ses_enabled ? 1 : 0
  domain = aws_ses_domain_identity.invitations[0].domain
}

resource "aws_route53_record" "ses_verification" {
  count   = local.ses_enabled && length(aws_route53_zone.app) > 0 ? 1 : 0
  zone_id = aws_route53_zone.app[0].zone_id
  name    = "_amazonses.${local.ses_mail_domain}"
  type    = "TXT"
  ttl     = 600
  records = [aws_ses_domain_identity.invitations[0].verification_token]
}

resource "aws_route53_record" "ses_dkim" {
  count   = local.ses_enabled && length(aws_route53_zone.app) > 0 ? 3 : 0
  zone_id = aws_route53_zone.app[0].zone_id
  name    = "${aws_ses_domain_dkim.invitations[0].dkim_tokens[count.index]}._domainkey"
  type    = "CNAME"
  ttl     = 600
  records = ["${aws_ses_domain_dkim.invitations[0].dkim_tokens[count.index]}.dkim.amazonses.com"]
}

data "aws_iam_policy_document" "app_ses_send" {
  count = local.ses_enabled ? 1 : 0

  statement {
    sid    = "SendInvitationEmail"
    effect = "Allow"
    actions = [
      "ses:SendEmail",
      "ses:SendRawEmail",
    ]
    resources = [
      aws_ses_domain_identity.invitations[0].arn,
    ]
  }
}


resource "aws_iam_role_policy" "app_ses_send" {
  count  = local.ses_enabled ? 1 : 0
  name   = "${var.project}-app-ses-send"
  role   = aws_iam_role.ssm.id
  policy = data.aws_iam_policy_document.app_ses_send[0].json
}
