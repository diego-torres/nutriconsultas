#!/usr/bin/env bash
# Auth0 + prod mobile E2E setup for patient app testing.
#
# Prerequisites:
#   - auth0 CLI logged in: auth0 login  (tenant dev-imd1udg26uvzvfto.us.auth0.com)
#   - AWS_PROFILE=minutriporcion (or set AWS_REGION / credentials)
#
# Usage:
#   ./scripts/mobile-auth0-e2e-setup.sh audit
#   ./scripts/mobile-auth0-e2e-setup.sh link --paciente-id 1 --sub 'google-oauth2|…' [--email user@example.com]
#   ./scripts/mobile-auth0-e2e-setup.sh link-by-email --paciente-id 1 --email user@example.com
#   ./scripts/mobile-auth0-e2e-setup.sh verify-token <access_token>
#
set -euo pipefail

TENANT_DOMAIN="${AUTH0_TENANT_DOMAIN:-dev-imd1udg26uvzvfto.us.auth0.com}"
NATIVE_CLIENT_ID="${AUTH0_NATIVE_CLIENT_ID:-CmZdUaMZ3Oqs4JSbUBdYZYmKoqDUiLBk}"
API_IDENTIFIER="${AUTH0_API_IDENTIFIER:-https://api.nutriconsultas.minutriporcion.com}"
AWS_REGION="${AWS_REGION:-us-east-1}"
AWS_PROFILE="${AWS_PROFILE:-minutriporcion}"
PROD_BASE_URL="${PROD_BASE_URL:-https://minutriporcion.com}"

require_auth0() {
	if ! auth0 tenants list >/dev/null 2>&1; then
		echo "Auth0 CLI is not authenticated. Run: auth0 login" >&2
		exit 1
	fi
}

ssm_instance_id() {
	aws ssm get-parameter --region "${AWS_REGION}" --profile "${AWS_PROFILE}" \
		--name /nutriconsultas/deploy/app_instance_id --query Parameter.Value --output text
}

run_prod_sql() {
	local sql="$1"
	local instance_id b64_sql tmp_json cmd_id
	instance_id="$(ssm_instance_id)"
	b64_sql="$(printf '%s' "${sql}" | base64 | tr -d '\n')"
	tmp_json="$(mktemp)"
	jq -n --arg b64 "${b64_sql}" \
		'{commands: ["set -eu", "set -a; source /opt/nutriconsultas/app.env; set +a", ("echo " + ($b64 | @sh) + " | base64 -d | PGPASSWORD=$JDBC_DATABASE_PASSWORD psql -h 172.31.14.201 -U $JDBC_DATABASE_USERNAME -d nutriconsultas -v ON_ERROR_STOP=1 -f -")]}' \
		>"${tmp_json}"
	cmd_id="$(aws ssm send-command --region "${AWS_REGION}" --profile "${AWS_PROFILE}" \
		--instance-ids "${instance_id}" \
		--document-name "AWS-RunShellScript" \
		--parameters "file://${tmp_json}" \
		--query Command.CommandId --output text)"
	rm -f "${tmp_json}"
	sleep 10
	aws ssm get-command-invocation --region "${AWS_REGION}" --profile "${AWS_PROFILE}" \
		--command-id "${cmd_id}" --instance-id "${instance_id}" \
		--query '{Status:Status,Stdout:StandardOutputContent,Stderr:StandardErrorContent}' --output json
}

cmd_audit() {
	require_auth0
	echo "=== Auth0 tenant audit (${TENANT_DOMAIN}) ==="
	echo
	echo "-- API resource (issue #108) --"
	auth0 apis list --json 2>/dev/null | python3 -c "
import json, sys
target = '${API_IDENTIFIER}'
apis = json.load(sys.stdin)
found = [a for a in apis if a.get('identifier') == target]
if found:
    a = found[0]
    print('OK  API identifier:', a.get('identifier'))
    print('    name:', a.get('name'))
    print('    signing_alg:', a.get('signing_alg'))
else:
    print('MISSING  API with identifier', target)
    print('Create: auth0 apis create --name \"Nutriconsultas Mobile API\" --identifier \"' + target + '\"')
"
	echo
	echo "-- Native app (${NATIVE_CLIENT_ID}) --"
	auth0 apps show "${NATIVE_CLIENT_ID}" --json 2>/dev/null | python3 -c "
import json, sys
a = json.load(sys.stdin)
print('name:', a.get('name'))
print('app_type:', a.get('app_type'))
print('callbacks:', len(a.get('callbacks') or []), 'configured')
"
	echo
	echo "-- Native app connections (Google / Apple / DB) --"
	auth0 api get "clients/${NATIVE_CLIENT_ID}/connections" 2>/dev/null | python3 -c "
import json, sys
raw = json.load(sys.stdin)
conns = raw.get('connections', raw if isinstance(raw, list) else [])
names = sorted(c.get('name', c.get('connection_id', '?')) for c in conns)
for n in names:
    print(' -', n)
if not any('apple' in str(n).lower() for n in names):
    print('WARN  Apple (A1) not enabled on minutriporcion-native — enable in Auth0 Dashboard')
"
	echo
	echo "-- Prod backend (no Bearer → expect 401) --"
	for path in visits diet-plans messages progress; do
		code="$(curl -s -o /dev/null -w '%{http_code}' "${PROD_BASE_URL}/rest/mobile/patient/${path}")"
		echo "GET /rest/mobile/patient/${path} → ${code}"
	done
	echo
	echo "-- Prod app.env Auth0 keys --"
	instance_id="$(ssm_instance_id)"
	cmd_id="$(aws ssm send-command --region "${AWS_REGION}" --profile "${AWS_PROFILE}" \
		--instance-ids "${instance_id}" \
		--document-name "AWS-RunShellScript" \
		--parameters 'commands=["grep -E \"^(AUTH_ISSUER|AUTH_AUDIENCE|AUTH0_MGMT_)\" /opt/nutriconsultas/app.env | sed \"s/=.*SECRET.*/=***REDACTED***/; s/CLIENT_SECRET=.*/CLIENT_SECRET=***REDACTED***/\""]' \
		--query Command.CommandId --output text)"
	sleep 8
	aws ssm get-command-invocation --region "${AWS_REGION}" --profile "${AWS_PROFILE}" \
		--command-id "${cmd_id}" --instance-id "${instance_id}" \
		--query StandardOutputContent --output text
	echo
	echo "-- Paciente linkage (prod DB) --"
	run_prod_sql "SELECT id, name, COALESCE(email,''), COALESCE(patient_auth_sub,'(unlinked)') FROM paciente ORDER BY id;"
}

cmd_link() {
	local paciente_id="" patient_sub="" patient_email=""
	while [[ $# -gt 0 ]]; do
		case "$1" in
			--paciente-id) paciente_id="$2"; shift 2 ;;
			--sub) patient_sub="$2"; shift 2 ;;
			--email) patient_email="$2"; shift 2 ;;
			*) echo "Unknown arg: $1" >&2; exit 1 ;;
		esac
	done
	if [[ -z "${paciente_id}" || -z "${patient_sub}" ]]; then
		echo "Usage: $0 link --paciente-id ID --sub 'auth0|…' [--email user@example.com]" >&2
		exit 1
	fi
	local sql="UPDATE paciente SET patient_auth_sub = '${patient_sub}'"
	if [[ -n "${patient_email}" ]]; then
		sql="${sql}, email = '${patient_email}'"
	fi
	sql="${sql} WHERE id = ${paciente_id} RETURNING id, name, email, patient_auth_sub;"
	echo "Linking paciente ${paciente_id} → sub ${patient_sub}"
	run_prod_sql "${sql}"
}

cmd_link_by_email() {
	require_auth0
	local paciente_id="" email=""
	while [[ $# -gt 0 ]]; do
		case "$1" in
			--paciente-id) paciente_id="$2"; shift 2 ;;
			--email) email="$2"; shift 2 ;;
			*) echo "Unknown arg: $1" >&2; exit 1 ;;
		esac
	done
	if [[ -z "${paciente_id}" || -z "${email}" ]]; then
		echo "Usage: $0 link-by-email --paciente-id ID --email user@example.com" >&2
		exit 1
	fi
	local sub
	sub="$(auth0 users search-by-email "${email}" --json 2>/dev/null | python3 -c "
import json, sys
users = json.load(sys.stdin)
if not users:
    sys.exit('No Auth0 user for email')
print(users[0].get('user_id',''))
")"
	cmd_link --paciente-id "${paciente_id}" --sub "${sub}" --email "${email}"
}

cmd_verify_token() {
	local token="${1:-}"
	if [[ -z "${token}" ]]; then
		echo "Usage: $0 verify-token <access_token>" >&2
		exit 1
	fi
	echo "Testing mobile endpoints with Bearer token..."
	for path in visits diet-plans messages progress; do
		code="$(curl -s -o /tmp/mobile-e2e-body.json -w '%{http_code}' \
			-H "Authorization: Bearer ${token}" \
			"${PROD_BASE_URL}/rest/mobile/patient/${path}")"
		echo "GET /rest/mobile/patient/${path} → ${code}"
		if [[ "${code}" == "403" ]]; then
			echo "  (403 = patient_not_linked — run link or link-by-email)"
		elif [[ "${code}" == "401" ]]; then
			echo "  (401 = invalid JWT or wrong aud — check AUTH0_AUDIENCE on mobile)"
		elif [[ "${code}" == "200" ]]; then
			head -c 120 /tmp/mobile-e2e-body.json; echo
		fi
	done
	rm -f /tmp/mobile-e2e-body.json
}

main() {
	local cmd="${1:-audit}"
	shift || true
	case "${cmd}" in
		audit) cmd_audit ;;
		link) cmd_link "$@" ;;
		link-by-email) cmd_link_by_email "$@" ;;
		verify-token) cmd_verify_token "$@" ;;
		*) echo "Unknown command: ${cmd}" >&2; exit 1 ;;
	esac
}

main "$@"
