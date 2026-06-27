#!/bin/bash

# Strict PHI logging audit for the patient mobile API package (#115) and patient
# invitation package (#141). Scans mobile + paciente.invitation for unsafe log patterns.

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

VIOLATIONS=0

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MOBILE_SRC="$PROJECT_ROOT/src/main/java/com/nutriconsultas/mobile"
INVITATION_SRC="$PROJECT_ROOT/src/main/java/com/nutriconsultas/paciente/invitation"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Mobile API PHI Logging Audit (#115 / #141)${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

scan_directory() {
  local label="$1"
  local src_dir="$2"

  if [ ! -d "$src_dir" ]; then
    echo -e "${RED}✗ ${label} source directory not found: $src_dir${NC}"
    exit 1
  fi

  local java_files
  java_files=$(find "$src_dir" -name "*.java" -type f | sort)
  local file_count
  file_count=$(echo "$java_files" | wc -l | tr -d ' ')
  echo -e "${YELLOW}Scanning ${file_count} ${label} Java file(s)...${NC}"
  echo ""

  while IFS= read -r file; do
    if [[ "$file" == *"PhiLogTurboFilter.java" ]] || [[ "$file" == *"ConsolePatientInvitationEmailSender.java" ]]; then
      continue
    fi

    line_number=0
    while IFS= read -r line; do
      line_number=$((line_number + 1))

      if [[ "$line" =~ ^[[:space:]]*// ]] || [[ "$line" =~ ^[[:space:]]*\* ]]; then
        continue
      fi

      if echo "$line" | grep -qE 'log\.(info|debug|warn|error|trace)\('; then
        if echo "$line" | grep -qE '\b(body|getBody\(\)|request\.body\(\))\b' \
            && ! echo "$line" | grep -q 'LogRedaction'; then
          report_violation "$file" "$line_number" "$line" "Message body must never appear in log statements"
        fi

        if echo "$line" | grep -qE 'log\.(info|warn|error)\(' \
            && echo "$line" | grep -qE '\b(getEmail|getNombre|getTelefono|getDireccion|getFechaNacimiento)\b' \
            && ! echo "$line" | grep -q 'LogRedaction'; then
          report_violation "$file" "$line_number" "$line" "PHI accessor must not be logged at INFO+ without LogRedaction"
        fi

        if echo "$line" | grep -qE 'log\.(info|warn|error)\(' \
            && echo "$line" | grep -qE '\bpatientAuthSub\b' \
            && ! echo "$line" | grep -q 'LogRedaction\.redactUserId'; then
          report_violation "$file" "$line_number" "$line" "Auth0 sub must use LogRedaction.redactUserId at INFO+"
        fi

        if echo "$line" | grep -qE 'log\.(info|debug|warn|error|trace)\([^)]*\{[^}]*\b(paciente|Paciente)\b[^}]*\}' \
            && ! echo "$line" | grep -q 'LogRedaction'; then
          report_violation "$file" "$line_number" "$line" "Paciente entity must be logged via LogRedaction"
        fi

        if echo "$line" | grep -qE 'log\.(info|debug|warn|error|trace)\([^)]*\{[^}]*\b(PatientMessage|CalendarEvent|ClinicalExam|AnthropometricMeasurement|PacienteDieta)\b[^}]*\}' \
            && ! echo "$line" | grep -q 'LogRedaction'; then
          report_violation "$file" "$line_number" "$line" "Patient-related entity must be logged via LogRedaction"
        fi

        if echo "$line" | grep -qE 'log\.(info|warn|error)\(' \
            && echo "$line" | grep -qE '\b(urlToken|rawUrlToken|humanCode|inviteUrl)\b'; then
          report_violation "$file" "$line_number" "$line" "Raw invitation token material must never be logged (#141)"
        fi
      fi
    done < "$file"
  done <<< "$java_files"
}

report_violation() {
  local file="$1"
  local line="$2"
  local content="$3"
  local reason="$4"
  echo -e "${RED}✗ VIOLATION in ${file}:${NC}"
  echo -e "  ${RED}${reason}${NC}"
  echo -e "    ${YELLOW}Line ${line}:${NC} ${content}"
  echo ""
  VIOLATIONS=$((VIOLATIONS + 1))
}

scan_directory "mobile" "$MOBILE_SRC"
scan_directory "patient invitation" "$INVITATION_SRC"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Audit Summary${NC}"
echo -e "${BLUE}========================================${NC}"

if [ "$VIOLATIONS" -eq 0 ]; then
  echo -e "${GREEN}✓ No mobile/invitation PHI logging violations found${NC}"
  exit 0
fi

echo -e "${RED}✗ ${VIOLATIONS} violation(s) found${NC}"
echo -e "${YELLOW}Use com.nutriconsultas.util.LogRedaction for all patient-related log output.${NC}"
exit 1
