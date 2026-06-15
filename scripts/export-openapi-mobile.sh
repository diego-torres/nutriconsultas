#!/usr/bin/env bash
# Export OpenAPI 3 YAML for /rest/mobile/patient/** (#112).
# Writes docs/api/openapi-mobile.yaml using the test profile (no running server required).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

mvn -B -Dtest=MobileOpenApiIntegrationTest#mobileOpenApiYamlIsAvailableForExport -Dopenapi.export=true test

echo "Wrote docs/api/openapi-mobile.yaml"
