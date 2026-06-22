# Auth0 role sync for subscription plans

Nutritionist plan tiers map to Auth0 **Roles** (RBAC). The database `subscription.plan_tier` is authoritative; Auth0 roles are synced for JWT/session claims.

**Issue:** [#182](https://github.com/diego-torres/nutriconsultas/issues/182)  
**Service:** `NutritionistRoleService.assignRole(adminPrincipal, targetUserId, planTier)`  
**Client:** `Auth0RoleSyncClient` (Management API)

---

## Tenant setup (extends #108)

1. **Enable RBAC** on the Auth0 API used for the nutritionist web app (if not already).
2. **Create four Roles** in Auth0 Dashboard → User Management → Roles (names must match exactly):

   | Role name | Plan tier |
   |-----------|-----------|
   | `nutriologo-basico` | Básico |
   | `nutriologo-profesional` | Profesional |
   | `nutriologo-plus` | Plus |
   | `director-consultorio` | Consultorio |

3. **Machine-to-Machine app** — same `AUTH0_MGMT_*` credentials as patient email lookup (#109). Grant scopes:
   - `read:roles`
   - `read:users`
   - `update:users`
   - `create:role_members` (assign roles)
   - `delete:role_members` (revoke roles on plan change)
   - `read:connections` and `update:connections` (disable public signup via `infrastructure/scripts/auth0-disable-public-signup.sh`)

4. **Environment variables** (see `.env.example`):

   ```properties
   AUTH0_MGMT_CLIENT_ID=...
   AUTH0_MGMT_CLIENT_SECRET=...
   AUTH0_MGMT_DOMAIN=https://YOUR_TENANT.auth0.com/
   ```

   When `AUTH0_MGMT_CLIENT_ID` is empty, `NoOpAuth0RoleSyncClient` is used and admin role assignment throws `Auth0ManagementNotConfiguredException`. Invitation redeem (#184) provisions subscription/clinic in PostgreSQL even if Auth0 sync fails (warn log only).

---

## Runtime behavior

1. Platform admin calls `NutritionistRoleService.assignRole` (future admin UI in #184).
2. `PlatformAdminService.requirePlatformAdmin` enforces the allowlist (#183).
3. Subscription is resolved for the target user (clinic director or active member).
4. `subscription.plan_tier` is updated in PostgreSQL.
5. `Auth0RoleSyncClient` removes all four plan roles from the user, then assigns the role matching the new tier.
6. `SubscriptionAuditEvent` with type `ROLE_ASSIGNED` records actor and target user IDs only.

---

## Related

- [`SUBSCRIPTION-ENFORCEMENT-PLAN.md`](SUBSCRIPTION-ENFORCEMENT-PLAN.md) — roles & identity model
- [`ISSUE-SUBSCRIPTION.md`](../../ISSUE-SUBSCRIPTION.md) — #182 registry
- Mobile API #108 — Auth0 API audience and M2M credentials
