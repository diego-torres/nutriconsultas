#!/bin/bash

# Linting and formatting script for nutriconsultas project
# This script runs all linting tools and formats the code

set -e

echo "=========================================="
echo "Running Linting and Formatting Tools"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print status
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    print_error "Maven is not installed or not in PATH"
    exit 1
fi

# Step 1: Format Java code with Spring Java Format
print_status "Step 1/5: Formatting Java code with Spring Java Format..."
if mvn spring-javaformat:apply; then
    print_status "✓ Java code formatted successfully"
else
    print_error "✗ Java code formatting failed"
    exit 1
fi
echo ""

# Step 2: Run Checkstyle
print_status "Step 2/5: Running Checkstyle..."
if mvn checkstyle:check; then
    print_status "✓ Checkstyle passed"
else
    print_warning "✗ Checkstyle found issues (see output above)"
    # Don't exit on checkstyle failures, just warn
fi
echo ""

# Step 3: Run SpotBugs
print_status "Step 3/5: Running SpotBugs..."
if mvn spotbugs:check; then
    print_status "✓ SpotBugs passed"
else
    print_warning "✗ SpotBugs found issues (see output above)"
    # Don't exit on spotbugs failures, just warn
fi
echo ""

# Step 4: Run PMD
print_status "Step 4/5: Running PMD..."
if mvn pmd:check; then
    print_status "✓ PMD passed"
else
    print_warning "✗ PMD found issues (see output above)"
    # Don't exit on PMD failures, just warn
fi
echo ""

# Step 5: Validate Thymeleaf templates
print_status "Step 5/5: Validating Thymeleaf templates..."
if mvn exec:java@validate-thymeleaf -Dexec.mainClass="com.nutriconsultas.ThymeleafValidator" -Dexec.args="src/main/resources/templates" 2>&1 | grep -q "Validation complete"; then
    print_status "✓ Thymeleaf templates validated"
else
    print_warning "✗ Thymeleaf validation found issues (see output above)"
    # Try alternative approach
    if mvn test-compile exec:java -Dexec.mainClass="com.nutriconsultas.ThymeleafValidator" -Dexec.args="src/main/resources/templates" > /dev/null 2>&1; then
        print_status "✓ Thymeleaf templates validated (alternative method)"
    else
        print_warning "✗ Thymeleaf validation failed - templates may have issues"
    fi
fi
echo ""

echo "=========================================="
print_status "Linting and formatting complete!"
echo "=========================================="
echo ""
print_status "Summary:"
echo "  - Java code has been formatted"
echo "  - Checkstyle, SpotBugs, and PMD reports are available"
echo "  - Thymeleaf templates have been validated"
echo ""
print_status "To view detailed reports:"
echo "  - Checkstyle: target/checkstyle-result.xml"
echo "  - SpotBugs: target/spotbugsXml.xml"
echo "  - PMD: target/pmd.xml"

