# Logging Security Guide

This document describes the tools and protections in place to prevent patient information from being exposed in logs.

## Overview

Patient privacy is critical. The application implements multiple layers of protection to ensure that patient-identifiable information (names, emails, phone numbers, medical records) is never logged.

## Protection Layers

### 1. LogRedaction Utility

The `LogRedaction` utility class (`com.nutriconsultas.util.LogRedaction`) provides safe methods to redact sensitive information before logging.

**Available Methods:**
- `redactPaciente(paciente)` - Redacts patient objects
- `redactCalendarEvent(event)` - Redacts calendar events
- `redactClinicalExam(exam)` - Redacts clinical exams
- `redactPacienteDieta(pacienteDieta)` - Redacts diet assignments
- `redactAnthropometricMeasurement(measurement)` - Redacts measurements

**Usage:**
```java
// ❌ BAD - Exposes patient data
log.info("Paciente found: {}", paciente);

// ✅ GOOD - Uses LogRedaction
log.info("Paciente found: {}", LogRedaction.redactPaciente(paciente));
```

### 2. Pre-commit Hook

The pre-commit hook automatically checks staged files for unsafe logging patterns before allowing commits.

**What it checks:**
- Logging of `paciente`/`Paciente` objects without `LogRedaction`
- Logging of patient-related entities (`CalendarEvent`, `ClinicalExam`, etc.) without `LogRedaction`
- Direct logging of sensitive field names

**Location:** `.git/hooks/pre-commit` (installed via `./setup-git-hooks.sh`)

**Behavior:**
- Blocks commits if unsafe patterns are detected
- Provides specific line numbers and examples
- Suggests using `LogRedaction` utility

### 3. Audit Script

A comprehensive audit script scans all Java files for potential logging security issues.

**Usage:**
```bash
./scripts/audit-logging.sh
```

**What it does:**
- Scans all Java files in `src/main/java`
- Detects multiple unsafe logging patterns
- Reports violations and warnings
- Provides fix suggestions

**Output:**
- **Violations**: Must be fixed (blocks commits)
- **Warnings**: Should be reviewed (may be false positives)

### 4. CI/CD Integration

The logging security check can be integrated into CI/CD pipelines.

**GitHub Actions Example:**
```yaml
- name: Check Logging Security
  run: ./scripts/audit-logging.sh
```

**Maven Integration:**
Add to `pom.xml` to run during build:
```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>exec-maven-plugin</artifactId>
  <executions>
    <execution>
      <phase>validate</phase>
      <goals>
        <goal>exec</goal>
      </goals>
      <configuration>
        <executable>bash</executable>
        <commandlineArgs>scripts/audit-logging.sh</commandlineArgs>
      </configuration>
    </execution>
  </executions>
</plugin>
```

## Detection Patterns

The tools detect the following unsafe patterns:

### Pattern 1: Direct Object Logging
```java
// ❌ Detected
log.info("Paciente: {}", paciente);
log.debug("Event: {}", calendarEvent);
```

### Pattern 2: Variable Name Logging
```java
// ❌ Detected
log.info("Found {}", paciente);
log.debug("Processing {}", clinicalExam);
```

### Pattern 3: Sensitive Field Logging
```java
// ⚠ Warning (may be false positive)
log.info("Email: {}", email);
log.debug("Phone: {}", phone);
```

### Pattern 4: Entity Logging Without Redaction
```java
// ❌ Detected
log.info("Measurement: {}", anthropometricMeasurement);
log.debug("Dieta: {}", pacienteDieta);
```

## Safe Patterns

These patterns are **safe** and will not trigger violations:

```java
// ✅ Safe - Uses LogRedaction
log.info("Paciente found: {}", LogRedaction.redactPaciente(paciente));
log.debug("Event: {}", LogRedaction.redactCalendarEvent(event));

// ✅ Safe - Logging only IDs
log.info("Paciente ID: {}", paciente.getId());
log.debug("Event ID: {}", event.getId());

// ✅ Safe - Logging system information
log.info("Processing request for user: {}", userId);
log.debug("Operation completed in {} ms", duration);
```

## Best Practices

1. **Always use LogRedaction** when logging patient-related entities
2. **Log only IDs** when you need to reference entities
3. **Never log** patient names, emails, phone numbers, or medical records
4. **Review warnings** from the audit script (may indicate areas to improve)
5. **Test logging** in development to ensure no sensitive data appears

## Manual Testing

To verify logging is safe:

1. **Run the audit script:**
   ```bash
   ./scripts/audit-logging.sh
   ```

2. **Check log output:**
   - Enable DEBUG logging
   - Perform typical operations (create patient, view calendar, etc.)
   - Review log files for any patient-identifiable information
   - Ensure only IDs and redacted information appear

3. **Review CI/CD logs:**
   - Check that audit script passes in CI
   - Review any warnings or violations reported

## Troubleshooting

### False Positives

If the audit script reports false positives:

1. **Check if LogRedaction is used:**
   - The pattern might match but LogRedaction is actually used
   - Review the specific line reported

2. **Check if it's a comment:**
   - Comments containing logging examples won't trigger violations
   - The script filters out comments

3. **Check if it's a string literal:**
   - String literals mentioning sensitive fields are safe
   - The script attempts to filter these out

### Pre-commit Hook Not Running

If the pre-commit hook isn't running:

1. **Check if it's installed:**
   ```bash
   ls -la .git/hooks/pre-commit
   ```

2. **Reinstall the hook:**
   ```bash
   ./setup-git-hooks.sh
   ```

3. **Check file permissions:**
   ```bash
   chmod +x .git/hooks/pre-commit
   ```

### Audit Script Not Finding Issues

If the audit script doesn't find issues but you suspect there are problems:

1. **Run with verbose output:**
   - Check the script output for any skipped files
   - Verify it's scanning the correct directory

2. **Check file patterns:**
   - Ensure Java files are in `src/main/java`
   - Check that files aren't excluded by `.gitignore`

3. **Manual review:**
   - Search for `log.` statements manually
   - Review each logging statement for patient data

## Additional Resources

- **LogRedaction Utility:** `src/main/java/com/nutriconsultas/util/LogRedaction.java`
- **Developer Guidelines:** `AGENTS.md` (Logging Sensitive Information section)
- **Project Rules:** `.cursorrules` (Logging best practices)

## Support

If you have questions or find issues with the logging security tools:

1. Review this documentation
2. Check the audit script output for specific guidance
3. Review the LogRedaction utility JavaDoc
4. Consult the project's coding guidelines

