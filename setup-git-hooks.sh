#!/bin/bash

# Install git hooks for the project (copies templates from git-hooks/ into .git/hooks/).

set -euo pipefail

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HOOKS_DIR="$SCRIPT_DIR/.git/hooks"
HOOK_TEMPLATE="$SCRIPT_DIR/git-hooks/pre-commit"
HOOK_FILE="$HOOKS_DIR/pre-commit"

if [ ! -d "$SCRIPT_DIR/.git" ]; then
	echo -e "${RED}✗ Error: Not a git repository${NC}"
	exit 1
fi

mkdir -p "$HOOKS_DIR"

if [ ! -f "$HOOK_TEMPLATE" ]; then
	echo -e "${RED}✗ Missing template: git-hooks/pre-commit${NC}"
	exit 1
fi

cp "$HOOK_TEMPLATE" "$HOOK_FILE"
chmod +x "$HOOK_FILE"
echo -e "${GREEN}✓ Pre-commit hook installed from git-hooks/pre-commit${NC}"

if ! command -v gitleaks >/dev/null 2>&1; then
	echo -e "${YELLOW}⚠ gitleaks not found on PATH — pre-commit will fail until it is installed${NC}"
	echo -e "${YELLOW}  brew install gitleaks${NC}"
else
	echo -e "${GREEN}✓ gitleaks available: $(gitleaks version 2>/dev/null | head -1)${NC}"
fi

echo -e "${YELLOW}Tip: commits always run a staged secret scan; Java commits also run checkstyle.${NC}"
