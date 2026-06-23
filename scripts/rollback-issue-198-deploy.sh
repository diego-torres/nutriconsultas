#!/usr/bin/env bash
# Rollback helper for issue #198 production deploy.
# Restores the pre-deploy PostgreSQL snapshot and optionally redeploys a prior JAR.
#
# Prerequisites: AWS CLI profile minutriporcion, SSM access to app + DB instances,
# local backup files under backups/ (or S3 copy).
#
# Usage:
#   ./scripts/rollback-issue-198-deploy.sh [--jar /path/to/previous.jar]
#
set -euo pipefail

PROFILE="${AWS_PROFILE:-minutriporcion}"
REGION="${AWS_DEFAULT_REGION:-us-east-1}"
BACKUP_SQL_GZ="${ROLLBACK_SQL_GZ:-backups/nutriconsultas-prod-20260623.sql.gz}"
S3_BACKUP="s3://nutriconsultas-app-jar-982361522543/db-backups/nutriconsultas-prod-20260623.sql.gz"
JAR_PATH=""

while [ $# -gt 0 ]; do
  case "$1" in
    --jar)
      JAR_PATH="${2:?}"
      shift 2
      ;;
    *)
      echo "Unknown argument: $1" >&2
      exit 1
      ;;
  esac
done

if [ ! -f "$BACKUP_SQL_GZ" ]; then
  echo "Downloading backup from S3: $S3_BACKUP"
  aws s3 cp "$S3_BACKUP" "$BACKUP_SQL_GZ" --profile "$PROFILE" --region "$REGION"
fi

echo "=== Rollback database to pre-#198 snapshot ==="
echo "Backup file: $BACKUP_SQL_GZ"
echo ""
echo "1. Open SSM port-forward to prod DB (via app instance), e.g.:"
echo "   aws ssm start-session --profile $PROFILE --region $REGION \\"
echo "     --target <app-instance-id> \\"
echo "     --document-name AWS-StartPortForwardingSessionToRemoteHost \\"
echo "     --parameters host=<db-private-ip>,portNumber=5432,localPortNumber=15432"
echo ""
echo "2. Restore (DESTRUCTIVE — replaces current prod data):"
echo "   gunzip -c $BACKUP_SQL_GZ | psql -h localhost -p 15432 -U nutriconsultas -d nutriconsultas"
echo ""
echo "3. Verify row counts:"
echo "   psql -h localhost -p 15432 -U nutriconsultas -d nutriconsultas -c \\"
echo "     \"SELECT COUNT(*) FROM dieta WHERE user_id='system:template-dietas';\""
echo "   (expect 20 template dietas before #198)"
echo ""

if [ -n "$JAR_PATH" ]; then
  echo "=== Redeploy previous application JAR ==="
  SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
  AWS_PROFILE="$PROFILE" AWS_DEFAULT_REGION="$REGION" \
    "$SCRIPT_DIR/../infrastructure/scripts/deploy-jar-to-ec2-ssm.sh" "$JAR_PATH"
else
  echo "=== Application rollback (optional) ==="
  echo "Save current JAR before deploy:"
  echo "  aws s3 cp s3://nutriconsultas-app-jar-982361522543/releases/nutriconsultas-web.jar \\"
  echo "    backups/nutriconsultas-web-pre-198.jar --profile $PROFILE"
  echo ""
  echo "Redeploy saved JAR:"
  echo "  ./scripts/rollback-issue-198-deploy.sh --jar backups/nutriconsultas-web-pre-198.jar"
fi
