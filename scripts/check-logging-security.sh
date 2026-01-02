#!/bin/bash

# Quick check script for logging security (lightweight version for pre-commit)
# This is a faster version that only checks staged files

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get list of staged Java files
STAGED_JAVA_FILES=$(git diff --cached --name-only --diff-filter=ACM | grep '\.java$' || true)

if [ -z "$STAGED_JAVA_FILES" ]; then
    exit 0
fi

# Track violations
VIOLATIONS=0

# Check each staged file
while IFS= read -r file; do
    # Skip if file doesn't exist (might be deleted)
    if [ ! -f "$file" ]; then
        continue
    fi
    
    # Skip LogRedaction.java itself
    if [[ "$file" == *"LogRedaction.java" ]]; then
        continue
    fi
    
    # Check for unsafe logging patterns
    # Pattern: Logging paciente/Paciente without LogRedaction
    if grep -nE 'log\.(info|debug|warn|error|trace)\([^)]*\{[^}]*\b(paciente|Paciente|CalendarEvent|ClinicalExam|AnthropometricMeasurement|PacienteDieta)\b[^}]*\}[^)]*\)' "$file" | grep -v "LogRedaction" > /dev/null 2>&1; then
        echo -e "${RED}✗ Unsafe logging detected in $file:${NC}"
        grep -nE 'log\.(info|debug|warn|error|trace)\([^)]*\{[^}]*\b(paciente|Paciente|CalendarEvent|ClinicalExam|AnthropometricMeasurement|PacienteDieta)\b[^}]*\}[^)]*\)' "$file" | grep -v "LogRedaction" | head -3 | while IFS=: read -r line content; do
            echo -e "  ${YELLOW}Line $line:${NC} ${content}"
        done
        echo ""
        ((VIOLATIONS++))
    fi
done <<< "$STAGED_JAVA_FILES"

if [ $VIOLATIONS -gt 0 ]; then
    echo -e "${RED}✗ $VIOLATIONS file(s) with unsafe logging patterns${NC}"
    echo -e "${YELLOW}Please use LogRedaction utility for patient-related entities.${NC}"
    echo -e "${YELLOW}Example: log.info(\"Paciente: {}\", LogRedaction.redactPaciente(paciente))${NC}"
    exit 1
fi

exit 0

