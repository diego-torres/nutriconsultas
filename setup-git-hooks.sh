#!/bin/bash

# Script to install git hooks for the project
# This copies the pre-commit hook to .git/hooks/

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HOOKS_DIR="$SCRIPT_DIR/.git/hooks"
HOOK_FILE="$HOOKS_DIR/pre-commit"

# Check if we're in a git repository
if [ ! -d "$SCRIPT_DIR/.git" ]; then
    echo -e "${RED}✗ Error: Not a git repository${NC}"
    exit 1
fi

# Create hooks directory if it doesn't exist
mkdir -p "$HOOKS_DIR"

# Check if hook template exists (for new installations)
HOOK_TEMPLATE="$SCRIPT_DIR/git-hooks/pre-commit"
if [ -f "$HOOK_TEMPLATE" ]; then
    echo -e "${YELLOW}Installing pre-commit hook from template...${NC}"
    cp "$HOOK_TEMPLATE" "$HOOK_FILE"
    chmod +x "$HOOK_FILE"
    echo -e "${GREEN}✓ Pre-commit hook installed successfully${NC}"
elif [ -f "$HOOK_FILE" ]; then
    echo -e "${GREEN}✓ Pre-commit hook already exists${NC}"
    echo -e "${YELLOW}To reinstall, remove .git/hooks/pre-commit and run this script again${NC}"
else
    echo -e "${YELLOW}⚠ No hook template found. Creating default pre-commit hook...${NC}"
    # Create a basic hook if template doesn't exist
    cat > "$HOOK_FILE" << 'EOF'
#!/bin/bash
# Pre-commit hook for formatting validation
# This is a placeholder. Run: mvn spring-javaformat:apply before committing.
exit 0
EOF
    chmod +x "$HOOK_FILE"
    echo -e "${GREEN}✓ Basic pre-commit hook created${NC}"
    echo -e "${YELLOW}Note: For full functionality, ensure the pre-commit hook is properly configured${NC}"
fi

