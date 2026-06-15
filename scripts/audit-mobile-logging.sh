#!/bin/bash

# Strict PHI logging audit for the patient mobile API package (#115).
# Scans only src/main/java/com/nutriconsultas/mobile for unsafe log patterns.

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

VIOLATIONS=0

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MOBILE_SRC="$PROJECT_ROOT/src/main/java/com/nutriconsultas/mobile"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Mobile API PHI Logging Audit (#115)${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

if [ ! -d "$MOBILE_SRC" ]; then
  echo -e "${RED}✗ Mobile source directory not found: $MOBILE_SRC${NC}"
  exit 1
fi

JAVA_FILES=$(find "$MOBILE_SRC" -name "*.java" -type f | sort)
FILE_COUNT=$(echo "$JAVA_FILES" | wc -l | tr -d ' ')
echo -e "${YELLOW}Scanning ${FILE_COUNT} mobile Java file(s)...${NC}"
echo ""

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

while IFS= read -r file; do
  if [[ "$file" == *"PhiLogTurboFilter.java" ]]; then
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
    fi
  done < "$file"
done <<< "$JAVA_FILES"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Audit Summary${NC}"
echo -e "${BLUE}========================================${NC}"

if [ "$VIOLATIONS" -eq 0 ]; then
  echo -e "${GREEN}✓ No mobile PHI logging violations found${NC}"
  exit 0
fi

echo -e "${RED}✗ ${VIOLATIONS} violation(s) found${NC}"
echo -e "${YELLOW}Use com.nutriconsultas.util.LogRedaction for all patient-related log output.${NC}"
exit 1
