# Invitation security audit (#141)

Cross-cutting acceptance audit for patient invitation onboarding security controls. Canonical error codes: [`ALIGNMENT-SPEC.md`](ALIGNMENT-SPEC.md) §F8.6.3–4.

**Audit date:** 2026-06-22  
**Scope:** #134 create, #135 preview, #136 redeem, #139 revoke, #140 Auth0 Post-Login gate

---

## Control matrix

| Control | Requirement | Status | Evidence |
|---------|-------------|--------|----------|
| Rate limit preview | Resilience4j, IP-scoped (#135) | ✅ | `PatientInvitationPreviewRateLimiter`, `application.properties` |
| Rate limit redeem | Resilience4j, sub-scoped (#136) | ✅ | `PatientInvitationRedeemRateLimiter` |
| Enumeration protection | Generic 404 for absent/expired/revoked | ✅ | `PatientInvitationUnavailableException`, `PatientInvitationRevokeNotFoundException` |
| Malformed token | 400, distinct message | ✅ | `PatientInvitationInvalidTokenException` |
| TTL server-side | `expiresAt` checked in service layer | ✅ | `isRedeemablePending`, preview service |
| No raw token logging | App + invitation package | ✅ | `PhiLogTurboFilter`, `InvitationPackageLoggingAuditTest` |
| Auth0 Action logging | No token in Action code | ✅ | `patient-invitation-gate.js` (no `console.log`) |
| JWT + redeem gate | Data access requires linked `sub` + redeem | ✅ | #136 redeem, #137 `PatientLinkageFilter` |

---

## F8.6.3 error-code compliance

| HTTP | Condition | Implementation | Test |
|------|-----------|----------------|------|
| 400 | Malformed token | `PatientInvitationInvalidTokenException` | `MobileInvitationIntegrationTest` preview malformed; `PatientInvitationRedeemServiceTest` |
| 404 | Absent / expired / revoked (generic) | `PatientInvitationUnavailableException` | Preview unknown vs expired; redeem unknown vs expired; revoke IDOR |
| 409 | Redeem by different `sub` | `PatientInvitationRedeemConflictException` | `MobileInvitationIntegrationTest` |
| 422 | Patient not `INVITED` at redeem | `PatientInvitationPatientStatusException` | `PatientInvitationRedeemServiceTest` |
| 429 | Rate limit preview/redeem | `RequestNotPermitted` → handler | Preview + redeem integration tests |

---

## Rate limiting

| Endpoint | Limiter key | Prod limit | Test limit |
|----------|-------------|------------|------------|
| `GET …/preview` | Client IP (`ClientIpResolver`) | 10/min | 2/min |
| `POST …/redeem` | `patientAuthSub` | 10/min | 2/min |

Configuration: `resilience4j.ratelimiter.instances.patientInvitationPreview` / `patientInvitationRedeem` in `application.properties` and `application-test.properties`.

429 response: localized `error.rate.limit.exceeded`, `Retry-After: 60` (`MobileApiExceptionHandler`).

---

## Enumeration protection

**Rule:** Never distinguish whether a token exists, expired, or was revoked — single generic 404 message (`error.invitation.invalid_or_expired`).

| Flow | Service | Notes |
|------|---------|-------|
| Preview | `PatientInvitationPreviewServiceImpl` | Revoked/expired/unknown → same exception |
| Redeem | `PatientInvitationRedeemServiceImpl` | Absent hash → `Unavailable`; non-pending → `Unavailable` |
| Revoke IDOR | `PatientInvitationRevokeServiceImpl` | Wrong nutritionist → `RevokeNotFound` (same 404 message) |

**Intentional 409:** Second `sub` redeeming an already-redeemed invite reveals prior redemption (F8.6.3).

---

## Token logging defense

### Runtime (`PhiLogTurboFilter`)

Protects loggers under `com.nutriconsultas.mobile` and `com.nutriconsultas.paciente.invitation` at INFO+:

- Email addresses
- Raw Auth0 `sub` (unless `REDACTED`)
- Human codes (`NUTRI-XXXX-XXXX`)
- Invite URL path tokens (`/i/{token}`)
- Standalone 43-char base64url URL tokens (#141)

### Static audits

| Gate | Command / test |
|------|----------------|
| Mobile package | `bash scripts/audit-mobile-logging.sh` |
| Invitation package | Same script (extended #141) |
| JUnit | `MobilePackageLoggingAuditTest`, `InvitationPackageLoggingAuditTest` |

### Service logging

Controllers and invitation services log only non-sensitive IDs (`invitationId`, `pacienteId`) at INFO.

---

## Auth0 Post-Login gate (#140)

- Action script does not log `invitation_token`.
- Preview fallback sends token in URL path to backend — **platform access logs** may capture URLs; operational teams must restrict Auth0 log retention/access.
- Documented in [`docs/auth0/PATIENT-POST-LOGIN-GATE.md`](../auth0/PATIENT-POST-LOGIN-GATE.md).

---

## Residual risks

| Risk | Mitigation |
|------|------------|
| Preview rate limit keyed on `X-Forwarded-For` | Trust proxy chain on production load balancer |
| 409 on redeem conflict | Acceptable per F8.6.3; does not leak token validity |
| Offline JWS cannot see DB revocation at Auth0 login | Preview path validates live state; redeem is authoritative |
| Standalone 43-char TurboFilter pattern | Low false-positive risk; only invitation package + mobile loggers |

---

## Sign-off criteria (#141)

- [x] Rate limits on preview and redeem with integration tests
- [x] Enumeration tests for preview and redeem (unknown vs expired)
- [x] IDOR tests for revoke (#139)
- [x] PHI/token logging audits (TurboFilter + static)
- [x] Auth0 Action no-token policy documented
- [x] This audit doc published

**Phase 2 invitation onboarding track:** complete after #141 merges.
