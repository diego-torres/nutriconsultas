# Logging Security Protection Tools

This project includes multiple layers of protection to prevent patient information from being exposed in logs.

## Quick Start

### Run Full Audit
```bash
./scripts/audit-logging.sh
```

### Check Staged Files (Pre-commit)
```bash
./scripts/check-logging-security.sh
```

## Available Tools

### 1. **LogRedaction Utility** ✅
- **Location:** `src/main/java/com/nutriconsultas/util/LogRedaction.java`
- **Purpose:** Safely redact sensitive information before logging
- **Usage:** Always use when logging patient-related entities

### 2. **Pre-commit Hook** ✅
- **Location:** `.git/hooks/pre-commit` (auto-installed)
- **Purpose:** Blocks commits with unsafe logging patterns
- **Behavior:** Automatically runs before each commit

### 3. **Audit Script** ✅
- **Location:** `scripts/audit-logging.sh`
- **Purpose:** Comprehensive scan of all Java files
- **Usage:** Run manually or in CI/CD

### 4. **CI/CD Integration** ✅
- **Location:** `.github/workflows/maven.yml`
- **Purpose:** Automated checks in pull requests
- **Behavior:** Runs logging security check in CI pipeline

## Protection Summary

| Layer | When It Runs | What It Checks | Blocks Commits |
|-------|-------------|----------------|-----------------|
| LogRedaction Utility | Development | Manual usage | No (guidance) |
| Pre-commit Hook | Before commit | Staged files | Yes |
| Audit Script | Manual/CI | All Java files | No (reports) |
| CI/CD Check | PR/CI | All Java files | Yes (CI fails) |

## Usage Examples

### ✅ Safe Logging
```java
log.info("Paciente found: {}", LogRedaction.redactPaciente(paciente));
log.debug("Event: {}", LogRedaction.redactCalendarEvent(event));
log.info("Paciente ID: {}", paciente.getId());
```

### ❌ Unsafe Logging (Will Be Detected)
```java
log.info("Paciente: {}", paciente);  // ❌ Detected by pre-commit hook
log.debug("Event: {}", calendarEvent);  // ❌ Detected by audit script
```

## Documentation

For detailed information, see:
- **Full Guide:** `docs/LOGGING_SECURITY.md`
- **Developer Guidelines:** `AGENTS.md` (Logging Sensitive Information section)
- **Project Rules:** `.cursorrules`

## Testing the Tools

### Test Pre-commit Hook
```bash
# Make a test change with unsafe logging
echo 'log.info("Test: {}", paciente);' >> src/test/TestFile.java
git add src/test/TestFile.java
git commit -m "Test"  # Should be blocked
```

### Test Audit Script
```bash
./scripts/audit-logging.sh
# Should report: ✓ No violations found
```

## Support

If you have questions:
1. Check `docs/LOGGING_SECURITY.md` for detailed documentation
2. Review the audit script output for specific guidance
3. Check LogRedaction utility JavaDoc

