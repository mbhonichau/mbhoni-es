# Tenant security remediation — 2026-09-26

This is a partial remediation ledger, not a declaration that tenant isolation or lifecycle enforcement is complete. No database migration or production-data change has been performed.

## Triage and reproduction

Reproductions were traced through current controllers and services before editing. A and B denote distinct tenants. Session writes require CSRF. Regression tests exercise the real tenant-access service with mocked persistence; they are not live-database or browser penetration tests.

| ID | Severity | Original finding / reproduction | Root cause |
| --- | --- | --- | --- |
| C1 | Critical | A requests `/organization?tenantId=B` with ORG entitlement/view permission | Service trusts requested tenant |
| C2 | Critical | A reads `/api/user/profile/{B-user}` or writes its `/process-toggles` with USER_VIEW/USER_EDIT | Resource lookup lacks tenant authorization |
| C3 | Critical | A GETs/PUTs `/api/employees/{B-employee}/field-values` with EMPLOYEE entitlement and permission | Employee ID trusted before querying values |
| C4 | Critical | A POSTs `/api/employees?tenantId=A` with B's employee ID in the body, or uses `/employees/save` | Create routes enter an unguarded update branch |
| C5 | Critical | A selects B's category in `/master-data?categoryId=...` or `/master-data/codes/save` | Category/code identifiers bypass tenant checks |
| C6 | Critical | OAuth/tenantless principal with audit authority requests `/audit-logs` | Unresolved tenant treated as platform-wide query |
| C7 | Critical | Previously unseen OAuth email signs in | First tenant and fallback first role assigned without authorization |
| C8 | Critical | Ordinary tenant member PUTs `/api/admin/tenant/{own-id}` | Tenant membership confused with edit/activation authority |
| C9 | Critical | Active user/key accesses inactive, suspended or cancelled tenant | No central lifecycle access policy |
| H1 | High | Field schema update/deactivate supplies another tenant's field ID | Requested tenant checked, actual resource owner unchecked |
| H2 | High | Quota form accepts foreign tenant; BILLING_EDIT invokes platform-wide overdue processing | Missing target-tenant/platform authorization |
| H3 | High | Foreign parent organization, employee organization or manager ID supplied | Relationship tenant consistency not enforced |
| H4 | High | Null-to-findAll and unguarded service lookups | Inconsistent manual shared-database isolation |
| H5 | High | Tenant saved before separate role/module/settings/subscription setup | No atomic onboarding completion invariant |
| H6 | High | Arbitrary subscription assignments and CANCELLED-to-SUSPENDED overdue processing | No state transition validation |
| M1 | Medium | Anonymous signup reaches global-admin-only listing or requires tenant context | No public tenant provisioning flow |
| M2 | Medium | Org hierarchy has free-form types and no access-boundary semantics | Missing membership/inheritance policy implementation |
| M3 | Medium | Tenant activation, status, subscription and expiry drift | No reconciliation process |

No Low-priority finding or cleanup is included.

## Additional Critical reproductions confirmed during remediation

| ID | Reproduction | Root cause |
| --- | --- | --- |
| C10 | Start the original service with seeded usernames/default credentials, or an existing account using a seeded username | Startup assigned fixed passwords and promoted users by mutable username |
| C11 | Submit a foreign invoice/quotation/expense ID with the caller's tenant in a save payload | Stored ownership not checked before reassignment |
| C12 | Read an employee/org response containing nested user, manager, parent or children | Entity serialization crossed authorization boundaries and exposed user fields |
| C13 | POST `/api/contracts?tenantId=A` with B's contract ID, or post B's milestone ID into A's contract | Raw entity merge and unchecked child ownership |
| C14 | GET `/employees/payslips/{B-id}/print`, or submit B's payslip ID to generate | Payslip lookup/save bypassed tenant and employee visibility checks |
| C15 | Tenant SERVICE_EDIT, or the view's TENANT_VIEW fallback, writes `/api/services` or `/services/save` | Shared catalog writes lacked a platform-admin check |
| C16 | Anonymous/foreign caller reads `/api/public/branding`, modules or onboarding-schema | Public exposure plus missing service guards; user confirmed same-tenant authentication is required |
| C17 | Delete an account, reuse its username for another identity, and reuse the old session | Session refresh loaded by mutable username rather than immutable user ID |
| C18 | Purge a tenant whose rows have an inconsistent foreign-tenant child under ON DELETE CASCADE | Scope-limited deletes alone cannot constrain database cascades |

## Updated findings table — 2026-09-29

Paths below are relative to `admin-service/src/main/java/com/mbhoni_creative`; tests are relative to `admin-service/src/test/java/com/mbhoni_creative`. Rows marked partial are not represented as resolved. Changes remain uncommitted and have not been deployed.

| Original Finding | Fix Applied | File(s) Changed | Test Added (Y/N + location) | Breaking Change (Y/N) | Remaining Follow-up |
| --- | --- | --- | --- | --- | --- |
| C1 Organization tenant reads | Resolve caller scope and authorize stored unit; filter organization visibility | adminservice/impl/OrganizationUnitServiceImpl.java | Y — adminservice/impl/OrganizationIsolationTest.java | Y — hierarchy JSON | MySQL rollout of V9 |
| C2 Profiles/security toggles | Stored-user tenant checks; Owner protection | adminservice/impl/UserProfileServiceImpl.java; config/OwnerProtection.java | Y — adminservice/impl/UserProfileIsolationTest.java; config/OwnerProtectionTest.java | N | Existing Owner assignments required |
| C3 Employee field values | Authorize employee and organization before reads/writes | adminservice/impl/EmployeeFieldValueServiceImpl.java | Y — adminservice/impl/EmployeeFieldIsolationTest.java | N | Broader hierarchy integration test in staging |
| C4 Employee payload IDOR | Check stored owner, resolve org/manager/user references, tenant lock for manager changes | adminservice/impl/EmployeeServiceImpl.java | Y — adminservice/impl/EmployeeIsolationTest.java | N | Inventory legacy foreign relationships |
| C5 Master-data IDOR | Resource-owner checks, immutable keys, scoped writes and COW overrides/tombstones | adminservice/impl/MasterDataServiceImpl.java; adminrepository/LookupCategoryRepository.java; adminrepository/LookupCodeRepository.java | Y — adminservice/impl/MasterDataIsolationTest.java | Y — scoped uniqueness V8 | Additional lookup-specific COW integration coverage |
| C6 Audit null-context leak | Only verified platform admins may query without tenant scope | adminservice/impl/AuditLogServiceImpl.java | Y — adminservice/impl/AuditIsolationTest.java | N | None specific to this reproduction |
| C7 Arbitrary OAuth membership | Verified Google subject/email, no email-only local-account link, atomic new-tenant signup; partial overall | config/CustomOAuth2UserService.java; config/TenantOAuthPrincipal.java; onboarding/TenantSignupService.java | Y — config/OAuthMembershipTest.java; onboarding/TenantSignupServiceTest.java | Y — signup/membership contract | F1 invitations/domain/request approval; explicit OAuth linking |
| C8 Activation bypass | TENANT_EDIT required; platform-only activation override with reason and actor audit | adminservice/impl/TenantServiceImpl.java; admindto/TenantDto.java | Y — adminservice/impl/TenantActivationAuthorizationTest.java | Y — changeReason required | Reconcile verified legacy status drift |
| C9 Lifecycle access | Server-side request gate; Owner-only billing/bookkeeping; suspended recovery and retained export | billing/TenantLifecycleFilter.java; billing/BillingRecoveryController.java; billing/TenantExportController.java; config/SecurityConfig.java | Y — billing/TenantLifecycleFilterTest.java; billing/TenantExportScopeTest.java | Y — access/API contracts | Stripe Checkout and deployment verification |
| C10 Seed credentials/startup promotion | Explicit strong bootstrap secrets; never promote/reset existing identity by username | config/BootstrapAccounts.java; config/DataInitializer.java | Y — config/BootstrapAccountsTest.java | Y — first-install configuration | F2 rotate legacy deployed passwords; enforce rotation workflow separately |
| C11 Invoice/quotation/expense ownership | Check stored IDs before reassignment; Owner-only services and billing routes | adminservice/impl/TenantInvoicingServiceImpl.java; adminservice/impl/BillingServiceImpl.java; admincontroller/BillingController.java | Y — adminservice/impl/TenantBookkeepingIsolationTest.java | Y — Owner-only billing | External payment/portal integration verification |
| C12 Nested serialization exposure | Remove user/manager/org graph expansion; expose scalar reference IDs | adminentity/Employee.java; adminentity/OrganizationUnit.java | Y — config/OrganizationAccessServiceTest.java | Y — JSON shapes | Client migration; broader response graph review remains H4 |
| C13 Contracts/milestones | Stored ownership checks, scoped lists, scalar copying and child ownership validation | adminservice/impl/ContractServiceImpl.java | Y — adminservice/impl/ContractIsolationTest.java | N | No reproduction-specific follow-up |
| C14 Payslip IDOR | Authorize stored tenant and employee on print/save; filter isolated records from lists | admincontroller/EmployeeViewController.java | Y — adminservice/impl/PayslipIsolationTest.java | N | Payroll role policy remains existing EMPLOYEE permissions |
| C15 Global catalog writes | Platform-only shared catalog, module and plan-module writes; tenant module defaults require Owner/top Admin | adminservice/impl/ServiceCatalogServiceImpl.java; ModuleServiceImpl.java; PlanModuleServiceImpl.java | Y — adminservice/impl/SharedCatalogAuthorizationTest.java | Y — tightened write authorization | Audit remaining shared industry-template mutation services |
| C16 Public metadata exposure | Require authentication and same-tenant service checks, including domain-resolved branding | config/SecurityConfig.java; adminservice/impl/TenantCustomizationServiceImpl.java; ModuleServiceImpl.java; TenantOnboardingFieldServiceImpl.java | Y — adminservice/impl/TenantMetadataIsolationTest.java | Y — formerly anonymous APIs | Update API consumers; full security-chain staging verification |
| C17 Session identity substitution | Refresh by immutable user ID; revoke deleted/inactive identity sessions | config/TenantContextFilter.java | Y — config/TenantContextFilterTest.java | N | No reproduction-specific follow-up |
| C18 Cascading purge escape | Preflight every FK for foreign dependents before mutation; refuse inconsistent graphs; transactional rollback | billing/TenantDataScope.java; billing/TenantPurgeService.java | Y — billing/TenantPurgeServiceTest.java | N | MySQL integration test with actual schema required |
| H1 Field/default ownership | Field ID ownership; platform-only global writes; Owner/top Admin tenant COW overrides and inactive tombstones | adminservice/impl/FieldDefinitionServiceImpl.java; adminrepository/FieldDefinitionRepository.java; config/TenantGovernance.java | Y — adminservice/impl/DefaultsGovernanceTest.java | Y — V8 scope uniqueness | F3 legacy onboarding-field materialization still differs from COW |
| H2 Quotas/overdue | Platform-only reasoned quota overrides and overdue processing; tenant-locked seat cap | billing/TenantQuotaService.java; adminservice/impl/BillingServiceImpl.java; UserServiceImpl.java | Y — billing/BillingOverrideAuthorizationTest.java; adminservice/UserServiceTest.java | Y — override reason/Owner billing | API/storage metering is not implemented |
| H3 Relationship integrity | Same-tenant org/manager/user validation, cycles checked, tenant locks | adminservice/impl/OrganizationUnitServiceImpl.java; EmployeeServiceImpl.java | Y — isolation tests above; config/OrganizationAccessServiceTest.java | N | Reviewed legacy data repair and concurrency integration tests |
| H4 Consistent isolation | Shared DB with tenant_id on owned roots and parent-join scoping on children; multiple service paths hardened; PARTIAL | Services above; billing/TenantDataScope.java | Y — isolation and metadata tests above | Mixed | Full repository-query/controller inventory is not complete; no universal ORM/DB isolation guarantee claimed |
| H5 Atomic provisioning | Self-serve local/Google creation is transactional across tenant/Owner/roles/settings/modules/subscription/billing account | onboarding/TenantSignupService.java; TrialPlanInitializer.java | Y — onboarding/TenantSignupServiceTest.java includes failure rollback | Y — V7 Owner identity | F4 platform create still creates a bare tenant; requires initial-Owner contract |
| H6 Arbitrary lifecycle/Stripe plan drift | Transition matrix, timestamped deadlines, signature verification, idempotency, current Stripe state, paid price mapping | billing/SubscriptionLifecycle.java; StripeWebhookService.java; StripeSignatureVerifier.java; StripePlanResolver.java; StripeLinkService.java | Y — billing/SubscriptionLifecycleTest.java; StripeSignatureVerifierTest.java; StripeWebhookServiceTest.java | Y — V6/V10 and assignment reason | Stripe test-mode E2E; Checkout; API-version pinning |
| M1 Broken public signup | Organization-based atomic 14-day TRIAL on five-seat TRIAL_STARTER, no card to start | admincontroller/AdminController.java; onboarding/TenantSignupService.java; templates/auth/register.html | Y — onboarding/TenantSignupServiceTest.java | Y — signup payload | F1 remaining membership paths |
| M2 Missing org inheritance | Explicit boundaries and memberships; Owner bypass, Admin membership, restricted parent inheritance; audited changes | config/OrganizationAccessService.java; onboarding/OrganizationBoundaryService.java; adminentity/OrganizationMembership.java | Y — config/OrganizationAccessServiceTest.java | Y — V9 and boundary APIs | Existing hierarchy review before enabling isolation |
| M3 Orphans/deadlines/retention | Scheduled and request-time deadline evaluation; day-30 local purge including billing/audit; manual deletion shares retention gate | billing/LifecycleScheduler.java; TenantPurgeService.java; adminservice/impl/TenantServiceImpl.java; Database/subscription_lifecycle_inventory.sql | Y — billing/TenantPurgeServiceTest.java; SubscriptionLifecycleTest.java; TenantActivationAuthorizationTest.java | Y — deletion behavior | F5 reviewed backfill and MySQL rollout; F6 external retention |

## Tracked open work

- **F1 — membership/account model (blocked on user decision):** one login with multiple tenant memberships versus separate accounts. Invitation acceptance, verified-domain opt-in and request approval must not silently move existing users. Seven-day invitation expiry is confirmed, not yet implemented.
- **F2 — installed credentials:** rotate legacy seeded passwords and revoke sessions. Code prevents future default creation/promotion; it cannot certify deployed credentials were changed.
- **F3 — legacy defaults:** `tenant_onboarding_fields` still copies defaults. A safe conversion must distinguish intentionally customized old rows from copied rows; do not overwrite tenant choices or claim all defaults are COW.
- **F4 — platform onboarding:** platform create needs an explicit initial Owner/provisioning contract. Public self-serve transaction coverage does not fix that older route.
- **F5 — deployment data:** inventory missing Owners/deadlines, mismatched statuses and foreign relationships. No production rows have been inspected or repaired. Backfill only reviewed inputs, separately from schema, with before-image export and reversible updates.
- **F6 — deletion scope:** day-30 purge is local database only. Stripe records, backups, external logs/files need their own retention implementation before an end-to-end deletion claim.
- **F7 — integration/completeness:** full repository query audit, MySQL/Flyway integration, real security-chain HTTP tests and Stripe test-mode E2E remain. H2/unit tests do not prove those environments.
- **F8 — paid onboarding:** existing recovery uses Stripe portal and platform linking; self-serve Checkout and explicit local-to-OAuth linking remain open. Trial hard cap is seats, not unimplemented API/storage metering.

## Confirmed rules

Stripe Billing; configurable 14-day trial, seven-day payment and suspended grace, 30-day cancelled export window with billing/audit deletion at day 30. Cancellation during PAST_DUE preserves both grace periods. Owner-only billing/bookkeeping. Owner bypasses isolated orgs; top Admin needs membership. Global defaults platform-only; tenant overrides Owner/top Admin. Public tenant metadata requires same-tenant authentication.

## Validation and rollout

Latest full run on 2026-09-29: `mvn "-Dmaven.repo.local=C:\Users\mbuso\.m2\repository" test` — **182 tests, 0 failures, 0 errors, 0 skipped**. Regression tests retain the original assertions and include actual H2 transaction rollback for provisioning and purge. Compiler/Mockito warnings remain visible. No migration or live Stripe action was executed.

See [tenant-remediation-rollout.md](tenant-remediation-rollout.md) for breaking contracts, configuration, schema-first rollout, separate reviewed data repairs, rollback prerequisites and limitations. There is no claim that every query/table is now isolated or that the entire audit is complete.