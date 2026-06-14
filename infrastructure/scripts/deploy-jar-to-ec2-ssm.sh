#!/usr/bin/env bash
# Upload a JAR to the S3 key created by infrastructure/cicd.tf, then SSM: copy on host + restart.
# Usage: ./deploy-jar-to-ec2-ssm.sh <path-to-jar> [project]
#   project  Terraform `var.project` (default: nutriconsultas) → SSM /{project}/deploy/*
set -euo pipefail
JAR_FILE="${1:?path to JAR required}"
PROJECT="${2:-nutriconsultas}"
P="/${PROJECT}/deploy"
: "${AWS_REGION:-${AWS_DEFAULT_REGION:?Set AWS_DEFAULT_REGION (or use configure)}}"

if [ ! -f "$JAR_FILE" ]; then
  echo "JAR not found: $JAR_FILE" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
UNIT_FILE="$SCRIPT_DIR/nutriconsultas-app.service"
if [ ! -f "$UNIT_FILE" ]; then
  echo "Missing systemd unit template: $UNIT_FILE" >&2
  exit 1
fi
UNIT_B64=$(base64 < "$UNIT_FILE" | tr -d '\n')

BUCKET="$(aws ssm get-parameter --name "$P/s3_bucket" --query "Parameter.Value" --output text)"
KEY="$(aws ssm get-parameter --name "$P/s3_key" --query "Parameter.Value" --output text)"
INSTANCE_ID="$(aws ssm get-parameter --name "$P/app_instance_id" --query "Parameter.Value" --output text)"
DEPLOY_REGION="$(aws ssm get-parameter --name "$P/aws_region" --query "Parameter.Value" --output text)"
S3URI="s3://$BUCKET/$KEY"
aws s3 cp "$JAR_FILE" "$S3URI" --no-progress

JSON="$(jq -n \
  --arg s3 "$S3URI" \
  --arg ub "$UNIT_B64" \
  --arg iid "$INSTANCE_ID" \
  --arg rg "$DEPLOY_REGION" \
  '{
     DocumentName: "AWS-RunShellScript",
     InstanceIds: [$iid],
     TimeoutSeconds: 1800,
     Parameters: {
       commands: [
         "set -euo pipefail",
         ("export AWS_DEFAULT_REGION=" + ($rg | @sh)),
         "[ -x /usr/bin/aws ] || timeout 300 sudo dnf -y -q install awscli",
         ("printf %s " + ($ub | @sh) + " | base64 -d | sudo tee /etc/systemd/system/nutriconsultas.service > /dev/null"),
         "sudo systemctl daemon-reload",
         ("timeout 600 sudo /usr/bin/aws s3 cp " + $s3 + " /opt/nutriconsultas/app.new.jar --no-progress"),
         "sudo chown nutri:nutri /opt/nutriconsultas/app.new.jar",
         "sudo mv -f /opt/nutriconsultas/app.new.jar /opt/nutriconsultas/app.jar",
         "sudo systemctl daemon-reload",
         "sudo systemctl enable nutriconsultas 2>/dev/null || true",
         "sudo systemctl restart nutriconsultas",
         "systemctl is-active --quiet nutriconsultas"
       ]
     },
     Comment: "nutriconsultas deploy"
   }')"

CMD_ID="$(aws ssm send-command --cli-input-json "$JSON" --query "Command.CommandId" --output text)"
# Poll must outlive SendCommand TimeoutSeconds (1800s): 420 * 5s = 35m + margin vs stuck InProgress.
POLL_MAX=420
echo "SSM CommandId=$CMD_ID instance=$INSTANCE_ID region=$DEPLOY_REGION (waiting up to ~$((POLL_MAX * 5 / 60))m for completion)"
poll=0
while [ "$poll" -lt "$POLL_MAX" ]; do
  poll=$((poll + 1))
  ST=$(aws ssm get-command-invocation --command-id "$CMD_ID" --instance-id "$INSTANCE_ID" --query "Status" --output text 2>/dev/null) || true
  ST="${ST:-InProgress}"
  case "$ST" in
    Success)
      echo "Deploy complete (SSM $CMD_ID)."
      exit 0
      ;;
    Failed|Cancelled|Cancelling|TimedOut|CANCELLED|FAILED)
      echo "SSM command failed: $ST" >&2
      aws ssm get-command-invocation --command-id "$CMD_ID" --instance-id "$INSTANCE_ID" || true
      exit 1
      ;;
    *)
      if [ $((poll % 12)) -eq 0 ]; then
        echo "SSM still $ST after $((poll * 5 / 60))m (command $CMD_ID)…"
      fi
      sleep 5
      ;;
  esac
done
echo "Timeout waiting for SSM (still not Success after ~$((POLL_MAX * 5 / 60))m)." >&2
aws ssm get-command-invocation --command-id "$CMD_ID" --instance-id "$INSTANCE_ID" || true
exit 1
