# Retention maintenance — revoked nutritionist data purge (#220)

Platform admins can purge clinical tenant data for nutritionists whose access was **revoked** (`access.revoke` audit) more than **90 days** ago (configurable via `MAINTENANCE_RETENTION_DAYS`).

## Eligibility

| Criterion | Source |
|-----------|--------|
| Subscription `CANCELLED` | Admin revoke (#210), not payment-only suspend |
| Revoke timestamp ≤ now − retention days | `subscription_audit_event.details` contains `action=access.revoke`; `created_at` is authoritative |
| Not already purged | `subscription.tenant_purged_at IS NULL` |

Director clinic members and payment-grace `SUSPENDED` accounts without admin revoke are **not** eligible.

## Admin UI

- **Route:** `/admin/platform/maintenance` (platform admin allowlist only)
- **Manual trigger:** “Ejecutar limpieza” with SweetAlert confirmation
- **History:** paginated table of `maintenance_run` rows with status, counts, S3 key

## Backup format (S3)

- **Path:** `{MAINTENANCE_S3_PREFIX}/{runId}/backup.json.gz` (default prefix `maintenance/revoked-nutritionist-backups`)
- **Compression:** gzip
- **Content:** JSON document

```json
{
  "schemaVersion": 1,
  "runId": "uuid",
  "exportedAt": "2026-06-22T12:00:00Z",
  "tenants": [
    {
      "userId": "auth0|…",
      "subscriptionId": 42,
      "revokedAt": "2026-01-01T00:00:00Z",
      "patientCount": 3,
      "patients": [{ "id": 1 }, { "id": 2 }],
      "dietaIds": [10, 11],
      "platilloIds": [20],
      "profile": { "id": 5, "publicBookingId": "…", "logoExtension": "png" },
      "clinicId": null
    }
  ]
}
```

Restore from backup is **out of scope for v1**; the schema is versioned for a future restore issue.

## Purge scope (per eligible nutritionist)

Deleted:

- All `Paciente` rows for `userId` and related clinical history (via `PacienteDeletionService`)
- Owned `Dieta` and `Platillo` templates
- Booking availability settings, working hours, blocks
- `NutritionistProfile`
- `Clinic` row when sole director with no active members; otherwise `ClinicMember` row only

**Not** deleted:

- `subscription`, `nutritionist_invitation`, `subscription_audit_event`
- `maintenance_run` metadata

After successful purge: `subscription.tenant_purged_at` is set.

## Configuration

| Property | Env | Default |
|----------|-----|---------|
| `nutriconsultas.subscription.maintenance.retention-days` | `MAINTENANCE_RETENTION_DAYS` | `90` |
| `nutriconsultas.subscription.maintenance.s3-prefix` | `MAINTENANCE_S3_PREFIX` | `maintenance/revoked-nutritionist-backups` |
| `nutriconsultas.subscription.maintenance.presigned-url-minutes` | `MAINTENANCE_PRESIGNED_URL_MINUTES` | `15` |

Uses existing `AWS_BUCKET`, `AWS_KEY`, `AWS_SECRET`.

## Audit actions

| Action | `details` prefix |
|--------|------------------|
| Cleanup run | `action=retention.purge,runId=…` |
| Backup delete | `action=retention.backup.delete,runId=…` |

No PHI in logs or audit details — user IDs and counts only.
