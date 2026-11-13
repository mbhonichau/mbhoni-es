# Tenant remediation rollout

Status: source changes only. No live database migration, Stripe mutation, credential rotation, or production purge has been executed.

## Breaking changes

- Tenant metadata under `/api/public/**` now requires authentication. Branding, modules and onboarding schemas enforce same-tenant access in services. Update callers to send their tenant session/API key. Verified platform administration retains its explicit exception.
- Employee JSON returns `userId` and `managerId` instead of expanding those objects. Organization JSON returns `parentUnitId` and does not expand parent/children. Update clients to use authorized resource endpoints.
- Self-serve signup requires an organization name and provisions a new tenant; a client-supplied tenant ID cannot grant membership. Google sign-in does not link an existing local account solely by matching email.
- Owner identity is recorded separately from roles. All billing/bookkeeping requires Owner; ordinary billing permissions alone no longer suffice. Protected ownership changes use `/api/tenants/{tenantId}/ownership` with userId and reason.
- Platform activation, subscription assignments, quota overrides and Stripe linking require justification. Manual tenant deletion now requires CANCELLED with expired export retention.
- V6–V10 add lifecycle, Owner, default-scope uniqueness and organization-membership structures. Review SQL before rollout; do not rely on Hibernate update as a migration plan.

## Deployment sequence

1. Stop application writes and scheduled jobs during the migration/data-repair window. Back up the database and verify restoration in a disposable MySQL instance.
2. Inventory duplicate global field/category keys before V8. Resolve each collision from reviewed business records, preserving an export of changed rows. V8 intentionally fails rather than choosing an arbitrary winner.
3. Apply schema-only V6–V10 through Flyway in staging first. V10 accommodates installations where Hibernate already created `trial_ends_at`. Record whether that column pre-existed. Rollback scripts are in `Database/rollback`; they require exports and explicit operator review, and are not automatic production rollback.
4. Run `Database/subscription_lifecycle_inventory.sql` after schema migration. Existing missing Owners and dates are not guessed. Assign each reviewed Owner through the audited ownership endpoint. Backfill only verified lifecycle dates in a separate transaction with a before-image export and conditional rollback; the repository does not contain guessed tenant-specific repair values.
5. Rotate any legacy seeded credentials through the existing credential-management process and revoke affected sessions. Removing startup defaults does not rotate passwords already stored in a deployed database. The `passwordChangeRequired` marker alone is not proof of an enforced password-rotation workflow.
6. Configure Stripe and bootstrap settings below. Exercise signed webhook replay, out-of-order events, paid plan changes, failed payments and cancellation in Stripe test mode against staging. No live Stripe verification has been performed here.
7. Verify MySQL export/purge with representative tenant data and actual foreign keys. H2 tests cover rollback and tenant boundaries but do not certify MySQL metadata/SQL behavior. The purge refuses inconsistent foreign dependents before deletion, including cascading FKs.
8. Resume the application only after the migration inventory and smoke tests are reviewed. Monitor reconciliation/purge error logs: failures are surfaced and retried, not recorded as successful purges.

## Configuration

`application.yml` maps these environment variables:

- TENANT_TRIAL_DAYS=14
- TENANT_PAYMENT_GRACE_DAYS=7
- TENANT_SUSPENDED_GRACE_DAYS=7
- TENANT_EXPORT_RETENTION_DAYS=30
- TENANT_RECONCILE_MS=60000
- STRIPE_SECRET_KEY, STRIPE_WEBHOOK_SECRET
- STRIPE_PORTAL_RETURN_URL (HTTPS), STRIPE_PORTAL_CONFIGURATION
- BOOTSTRAP_ADMIN_USERNAME, BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD (new installations only; at least 16 characters, at most 72 UTF-8 bytes)

Supply case-sensitive `app.stripe.price-plans.price_...: PLAN_CODE` entries in deployment YAML. A mapped active non-trial plan and exactly one base Stripe subscription item are required. Unknown prices fail reconciliation without acknowledging success. Configure the Stripe portal to expose only approved price changes.

The webhook is POST `/api/webhooks/stripe`. Supported events: invoice.paid, invoice.payment_failed, customer.subscription.updated and customer.subscription.deleted. Raw-body signatures, timestamp tolerance, explicit customer/subscription mapping, receipt idempotency and current-provider-state reconciliation protect state changes. Paid conversion currently uses the audited platform Stripe-link endpoint; self-serve Checkout remains a tracked follow-up.

## Retention boundary

At the cancellation deadline plus 30 days, the local database purge includes tenant billing and audit records with no additional retention. Export excludes authentication secrets, API keys and platform-wide configuration. This implementation does not delete provider-side Stripe records, backups, external files or external log sinks; their retention needs an explicit operational implementation before claiming end-to-end deletion.

## Open implementation decisions and follow-ups

- Membership: existing accounts currently have one tenant. Clarify whether invitations can create another tenant membership for the same login or require a separate account before implementing acceptance/domain joining. Do not silently transfer accounts.
- Invitation expiry default is confirmed at seven days; invitation, verified-domain opt-in and request approval flows are not yet implemented.
- Existing-owner assignments and missing lifecycle deadlines need reviewed deployment data, not inferred first users or inferred paid states.
- Legacy `tenant_onboarding_fields` still materializes defaults; complete copy-on-write conversion separately with an explicit treatment of existing customized versus copied rows.
- The platform tenant-create form still creates a bare tenant and needs an explicit initial Owner/provisioning contract. Do not represent that path as atomic self-serve onboarding.
- API/storage metering enforcement, full Stripe Checkout, explicit local-to-OAuth account linking, provider API-version pinning and provider-side retention are not completed by this patch.
