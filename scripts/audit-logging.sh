#!/bin/bash

# Script to audit logging statements for potential patient information exposure
# This script checks for unsafe logging patterns that might expose sensitive data

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Track violations
VIOLATIONS=0
WARNINGS=0

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Logging Security Audit${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Get project root
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

# Find all Java files in src/main/java
JAVA_FILES=$(find src/main/java -name "*.java" -type f)

if [ -z "$JAVA_FILES" ]; then
    echo -e "${YELLOW}⚠ No Java files found${NC}"
    exit 0
fi

echo -e "${YELLOW}Scanning $(echo "$JAVA_FILES" | wc -l | tr -d ' ') Java file(s)...${NC}"
echo ""

# Patterns to detect unsafe logging
# Pattern 1: Logging entire patient objects without LogRedaction
PATTERN1='log\.(info|debug|warn|error|trace)\([^)]*\{[^}]*paciente[^}]*\}[^)]*\)'
PATTERN1_DESC="Logging paciente object without LogRedaction"

# Pattern 2: Logging patient objects with variable name (case insensitive)
PATTERN2='log\.(info|debug|warn|error|trace)\([^)]*\{[^}]*\b(paciente|Paciente)\b[^}]*\}[^)]*\)'
PATTERN2_DESC="Logging paciente variable without LogRedaction"

# Pattern 3: Logging common sensitive fields
PATTERN3='log\.(info|debug|warn|error|trace)\([^)]*\b(email|phone|telefono|nombre|name|address|direccion|medical|medico|record|historial)\b[^)]*\)'
PATTERN3_DESC="Logging potentially sensitive patient fields"

# Pattern 4: Logging CalendarEvent, ClinicalExam, etc. without LogRedaction
PATTERN4='log\.(info|debug|warn|error|trace)\([^)]*\{[^}]*\b(CalendarEvent|ClinicalExam|AnthropometricMeasurement|PacienteDieta)\b[^}]*\}[^)]*\)'
PATTERN4_DESC="Logging patient-related entities without LogRedaction"

# Check each file
while IFS= read -r file; do
    # Skip LogRedaction.java itself (it's the solution, not a violation)
    if [[ "$file" == *"LogRedaction.java" ]]; then
        continue
    fi
    
    # Check for Pattern 1: Direct paciente object logging
    if grep -nE "$PATTERN1" "$file" > /dev/null 2>&1; then
        echo -e "${RED}✗ VIOLATION in $file:${NC}"
        echo -e "  ${RED}$PATTERN1_DESC${NC}"
        grep -nE "$PATTERN1" "$file" | head -5 | while IFS=: read -r line content; do
            echo -e "    ${YELLOW}Line $line:${NC} ${content}"
        done
        echo ""
        ((VIOLATIONS++))
    fi
    
    # Check for Pattern 2: Paciente variable logging
    if grep -nE "$PATTERN2" "$file" > /dev/null 2>&1; then
        # Check if LogRedaction is used in the same line
        if ! grep -nE "$PATTERN2" "$file" | grep -q "LogRedaction"; then
            echo -e "${RED}✗ VIOLATION in $file:${NC}"
            echo -e "  ${RED}$PATTERN2_DESC${NC}"
            grep -nE "$PATTERN2" "$file" | grep -v "LogRedaction" | head -5 | while IFS=: read -r line content; do
                echo -e "    ${YELLOW}Line $line:${NC} ${content}"
            done
            echo ""
            ((VIOLATIONS++))
        fi
    fi
    
    # Check for Pattern 3: Sensitive fields
    if grep -nE "$PATTERN3" "$file" > /dev/null 2>&1; then
        # Check if it's in a comment or string literal (false positive)
        if grep -nE "$PATTERN3" "$file" | grep -vE "(//|/\*|\*|LogRedaction)" > /dev/null 2>&1; then
            echo -e "${YELLOW}⚠ WARNING in $file:${NC}"
            echo -e "  ${YELLOW}$PATTERN3_DESC${NC}"
            grep -nE "$PATTERN3" "$file" | grep -vE "(//|/\*|\*|LogRedaction)" | head -3 | while IFS=: read -r line content; do
                echo -e "    ${YELLOW}Line $line:${NC} ${content}"
            done
            echo ""
            ((WARNINGS++))
        fi
    fi
    
    # Check for Pattern 4: Patient-related entities without LogRedaction
    if grep -nE "$PATTERN4" "$file" > /dev/null 2>&1; then
        # Check if LogRedaction is used
        if ! grep -nE "$PATTERN4" "$file" | grep -q "LogRedaction"; then
            echo -e "${RED}✗ VIOLATION in $file:${NC}"
            echo -e "  ${RED}$PATTERN4_DESC${NC}"
            grep -nE "$PATTERN4" "$file" | grep -v "LogRedaction" | head -5 | while IFS=: read -r line content; do
                echo -e "    ${YELLOW}Line $line:${NC} ${content}"
            done
            echo ""
            ((VIOLATIONS++))
        fi
    fi
    
done <<< "$JAVA_FILES"

# Summary
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Audit Summary${NC}"
echo -e "${BLUE}========================================${NC}"

if [ $VIOLATIONS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo -e "${GREEN}✓ No violations or warnings found${NC}"
    echo -e "${GREEN}All logging statements appear safe${NC}"
    exit 0
elif [ $VIOLATIONS -eq 0 ]; then
    echo -e "${GREEN}✓ No violations found${NC}"
    echo -e "${YELLOW}⚠ $WARNINGS warning(s) found (review recommended)${NC}"
    exit 0
else
    echo -e "${RED}✗ $VIOLATIONS violation(s) found${NC}"
    if [ $WARNINGS -gt 0 ]; then
        echo -e "${YELLOW}⚠ $WARNINGS warning(s) found${NC}"
    fi
    echo ""
    echo -e "${YELLOW}Please fix violations before committing.${NC}"
    echo -e "${YELLOW}Use LogRedaction utility for all patient-related entities.${NC}"
    echo -e "${YELLOW}Example: log.info(\"Paciente found: {}\", LogRedaction.redactPaciente(paciente))${NC}"
    exit 1
fi

