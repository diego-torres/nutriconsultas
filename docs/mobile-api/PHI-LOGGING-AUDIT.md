# Mobile API PHI logging audit (#115)

Audit completed **2026-06-15** on branch `mobile-api/115-phi-audit`.

## Policy

- No patient names, emails, phone numbers, DOB, measurements, message bodies, or raw Auth0 `sub` values in unstructured logs at **INFO** or above.
- Use `com.nutriconsultas.util.LogRedaction` for all patient-related identifiers in log statements.
- Defense in depth: `PhiLogTurboFilter` (configured in `logback-spring.xml`) denies INFO+ mobile log lines that match email or unredacted Auth0 sub patterns.

## Checklist

| Area | Issue | Status | Notes |
|------|-------|--------|-------|
| Visits | #91, #92 | ✓ | `MobilePatientVisitController` / `MobilePatientVisitService` — patient ID via `LogRedaction.redactPaciente`, visit ID via `LogRedaction.redactCalendarEvent` |
| Diet plans | #93, #94, #95 | ✓ | `MobilePatientDietPlanController` / `MobilePatientDietPlanService` — assignment ID via `LogRedaction.redactPacienteDieta`; no plan content or notes logged |
| Messages | #96, #97 | ✓ | `MobilePatientMessageController` / `MobilePatientMessageService` — only `PatientMessage[id=…]` at INFO; message body never logged |
| Progress | #98, #99 | ✓ | `MobilePatientProgressController` / `MobilePatientProgressService` — patient ID only; no raw BMI/weight values in logs |
| Auth resolver | #107 | ✓ | `PatientAuthService`, `PatientLinkageFilter`, `PatientAuthLinkageService` — `sub` redacted via `LogRedaction.redactUserId` |
| Exception handler | — | ✓ | `MobileApiExceptionHandler` — generic messages only, no request payloads |

## CI gates

| Gate | Command |
|------|---------|
| Repo-wide logging audit | `bash scripts/audit-logging.sh` |
| Mobile package audit (#115) | `bash scripts/audit-mobile-logging.sh` |
| Automated tests | `MobilePackageLoggingAuditTest`, `MobilePhiLoggingIntegrationTest`, `PhiLogTurboFilterTest` |

## Safe logging examples

```java
log.debug("Mobile list visits for patient {}", LogRedaction.redactPaciente(paciente.getId()));
log.info("Patient sent message: {}", LogRedaction.redactPatientMessage(saved.getId()));
log.info("Linked mobile Auth0 account for patient {} sub={}",
    LogRedaction.redactPaciente(saved.getId()), LogRedaction.redactUserId(patientAuthSub));
```

## Related

- `docs/LOGGING_SECURITY.md` — project-wide logging security guide
- `README_LOGGING_SECURITY.md` — quick reference
- Issue [#141](https://github.com/diego-torres/nutriconsultas/issues/141) — invitation token logging (future; never log raw tokens)
