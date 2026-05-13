#!/usr/bin/env bash
# Install JAR from S3 (SSM parameters), fix nginx proxy headers, run Certbot for HTTPS.
# Usage: AWS_PROFILE=minutriporcion bash infrastructure/scripts/ssm-remediate-app-host.sh [instance-id]
# Omit instance-id to use SSM parameter /nutriconsultas/deploy/app_instance_id (us-east-1).
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REGION="${AWS_REGION:-us-east-1}"
INSTANCE_ID="${1:-}"
if [[ -z "${INSTANCE_ID}" ]]; then
	INSTANCE_ID="$(aws ssm get-parameter --region "${REGION}" \
		--name /nutriconsultas/deploy/app_instance_id --query Parameter.Value --output text)"
fi

B64_SCRIPT="$(base64 <"${SCRIPT_DIR}/remediate-app-host-remote.sh" | tr -d '\n')"
TMP_JSON="$(mktemp)"
jq -n --arg b64 "${B64_SCRIPT}" \
	'{commands: ["set -eu;" + ("echo " + ($b64 | @sh) + "|base64 -d|bash")] }' \
	>"${TMP_JSON}"

CMD_ID="$(aws ssm send-command --region "${REGION}" \
	--instance-ids "${INSTANCE_ID}" \
	--document-name "AWS-RunShellScript" \
	--parameters "file://${TMP_JSON}" \
	--timeout-seconds 600 \
	--output text --query Command.CommandId)"
rm -f "${TMP_JSON}"

echo "SSM CommandId: ${CMD_ID} (instance ${INSTANCE_ID})"
for _ in $(seq 1 120); do
	STATUS="$(aws ssm get-command-invocation --region "${REGION}" \
		--command-id "${CMD_ID}" --instance-id "${INSTANCE_ID}" \
		--query Status --output text 2>/dev/null || echo Pending)"
	if [[ "${STATUS}" == Success ]] || [[ "${STATUS}" == Failed ]] ||
		[[ "${STATUS}" == Cancelled ]] || [[ "${STATUS}" == TimedOut ]]; then
		break
	fi
	sleep 5
done

aws ssm get-command-invocation --region "${REGION}" \
	--command-id "${CMD_ID}" --instance-id "${INSTANCE_ID}" \
	--query '{Status:Status,Stdout:StandardOutputContent,Stderr:StandardErrorContent}' \
	--output json | jq .

if [[ "${STATUS}" != Success ]]; then
	echo "Remediation failed with status=${STATUS}" >&2
	exit 1
fi
