# Phase 2 workforce delivery report

## Outcome and validation

Phase 2 adds five read-only workforce endpoints with tenant-only numeric IDs, explicit DTOs, scope checks, sensitive-field exclusions, and isolated-organization exclusions. No entities, existing controllers, existing tests, dependencies or migrations were modified by this phase. The only approved legacy code edits are the API-key filter restriction and key-creation permission allowlist extension. Existing unrelated working-tree changes were preserved.

- Shared matcher commit: `31c4358 refactor(api): share API path matcher`.
- `mvn clean verify`: 242 tests, zero failures, zero errors, two opt-in skips.
- Separate disposable MySQL 8.0.46 run: V8FlywayMigrationTest and V12FlywayMigrationTest both passed, zero skips.
- The existing gates are V8_MYSQL_INTEGRATION and V12_MYSQL_INTEGRATION respectively. Both were enabled for the separate run; neither existing test was edited to rename its gate.
- New tests: ApiPathsParityTest (2), WorkforceFieldPolicyTest (2), WorkforcePermissionTest (4), WorkforceIntegrationTest (11): 19 total.
- Workforce SQL query counts: page of 1 employee = 5; page of 50 = 5. CountingDataSource counts prepared statements through the actual JDBC repository, with authentication and tenant/lifecycle repositories mocked. Their overhead is not included in those numbers. Queries are organization projection, employee count, employee page, effective definitions, and page-wide custom values. There are no ORM entity loads in the workforce repository. Correlated SQL subqueries are executed within the page/count statements, not separate round trips.
- Read-only verification compares complete before/after table contents (stronger than row counts) and confirms read-only transactions for all five endpoints. The existing real-JPA credential test separately checks authentication does not persist lastUsedAt.
- Legacy-route tests check new-key 403s, mixed-key response parity, unchanged authority sets including empty and unknown grants, and existing scope mappings. The original Phase 1 suite still checks CSRF, Stripe signatures, docs routing and no-session behavior.
- Build logs: admin-service/api-phase2-verify.log and admin-service/api-phase2-mysql.log (local verification artifacts).

## Phase 1 closeout

The exact shouldNotFilter additions to ApiKeyAuthenticationFilter and TenantContextFilter remain in `filter-exclusions.patch`. Both are:

```diff
+    @Override
+    protected boolean shouldNotFilter(HttpServletRequest request) {
+        return com.mbhoni_creative.api.security.ApiPaths.matches(request);
+    }
+
```

Before the refactor, ApiPaths and the chain used separate hand-written path rules. Now ApiPaths.MATCHER is the single RequestMatcher used by the chain and by both filters through ApiPaths.matches. No second filter was changed during Phase 2. `shared-path-matcher.patch` records the source diff.

ApiPathsParityTest verifies context-path handling, trailing slashes, URI fallback, and case sensitivity for both actual filters and the shared matcher. StrictHttpFirewall rejects ;jsessionid, // and /../ forms before chain selection. Legacy /api/employees is not skipped. Accepted lowercase /api/v1 paths select the new chain; differently cased paths do not match the case-sensitive API mappings. Existing integration tests exercise real security chains.

JPA auditing generates server-local LocalDateTime values; there is no UTC DateTimeProvider. The MySQL serverTimezone=UTC setting does not establish UTC semantics for application-generated LocalDateTime. README.md documents this, including conversion of offset inputs to the server zone. Phase 1 module timestamps use the latest across assignment, catalog, subscription, plan and plan-module mapping; that behavior is unchanged.

| Existing permission | Phase 1 scopes |
| --- | --- |
| USER_VIEW | users:read, access:check |
| TENANT_VIEW | tenant:read, quotas:read |
| CUSTOMIZATION_VIEW | branding:read |
| MODULE_VIEW | modules:read |
| SUBSCRIPTION_VIEW | entitlements:read |
| Anything else without an explicit mapping | None |

Phase 1 new files, relative to admin-service:

- docs/api/README.md, filter-exclusions.patch, phase1-existing-files.patch
- src/main/resources/api-openapi.properties
- src/main/java/com/mbhoni_creative/api/config/ApiOpenApiConfig.java, ApiSecurityConfig.java
- api/controller/ApiController.java, ApiDocsController.java, ApiNotFoundController.java
- api/dto/ApiDtos.java, ApiEnvelope.java
- api/error/ApiExceptionHandler.java, ApiProblems.java
- api/mapper/ApiMapper.java, ApiTimestamp.java
- api/security/ApiCredentialService.java, ApiKeyFilter.java, ApiPaths.java, ApiScopes.java
- api/service/ApiReadService.java
- src/test/java/com/mbhoni_creative/api/ApiCredentialReadOnlyTest.java, ApiReadServiceTest.java, ApiSecurityIntegrationTest.java

## Exploration findings

| Area | Existing storage and decision |
| --- | --- |
| Sensitive core identity | Employee.nationalId, taxNumber, licenseNumber/category/expiryDate excluded. No dedicated passport or birth-date field on Employee. |
| Contact and address | Employee.email is ambiguous and omitted; workPhone, residentialAddress and all emergencyContact fields excluded. |
| Screening and employment details | Employee.backgroundCheckStatus, complianceEmploymentStatus, serviceProvider and terminationDate excluded. Probation/notice and clearance/permit dynamic fields excluded. |
| Bank, salary, tax, medical, disciplinary, birth and documents | No dedicated bank/salary/medical/disciplinary/birth/attachment attribute on Employee. Arbitrary contents can occur in extendedAttributesJson or dynamic field values. Raw JSON is entirely omitted; dynamic values are filtered by the central policy. TaxNumber is a core field. FILE values are excluded. |
| Dynamic fields | field_definitions describes section, key, label, type, options, required/active flags and order. There is no sensitivity/visibility/restricted flag. tenant_id NULL means global default. |
| Dynamic values | EAV rows in employee_field_values link employee_id and field_definition_id and store a text value. Tenant scope comes from the employee and definition; there is no value-level tenant_id. Employee has a separate raw extendedAttributesJson storage mechanism, excluded in full. TenantOnboardingField and industry field templates configure onboarding requirements; they are not the EAV schema exposed here. |
| N+1 risk | Existing values repository reads by one employee at a time; field-definition and employee tenant/org/manager eager relations can load graphs. The new JDBC projection repository avoids those graphs, loads custom values using one IN query for page IDs, and performs no per-row repository calls. |
| Employee timestamps | BaseEntity.updatedAt is audited. Custom values have recordedAt, refreshed by the existing save service, not an audited updatedAt. Both contribute to inclusive employee filtering and returned timestamps. |
| Definition timestamps | BaseEntity.updatedAt exists, but definition changes do not touch employee timestamps. Excluded from employee incremental semantics by decision; nightly full resync recommended. |
| Org and lookup timestamps | Org units, categories and codes inherit audited updatedAt. Org filtering uses it. Reference data is merged across records and offers no updatedSince in this phase. |
| Org structure | Tenant-owned self-referencing parent and child collection; no depth or status column. isolated/restrictParentVisibility exist. A single tenant query and cycle-aware parent traversal excludes isolated subtrees, invalid parents and cycles. Flat output only. |
| Reference types | Category codes are database records, not a closed enum. No seeded category catalogue was found in migrations or DataInitializer, and the application database was not queried. Thus no claim is made to enumerate live tenant-created category codes. The explicit API allowlist is COUNTRY, CURRENCY, EMPLOYMENT_TYPE and EMPLOYEE_STATUS, all mixed global/tenant through their category. Non-allowlisted types and metadata are excluded. |

## New scope mappings and legacy-key boundary

| New permission | Scope |
| --- | --- |
| API_EMPLOYEE_READ | employees:read |
| API_ORG_READ | orgunits:read |
| API_REFERENCE_READ | reference:read |

The legacy employee route accepts EMPLOYEE_VIEW OR API_KEY. Existing API keys automatically receive API_KEY. Dedicated names therefore do not alone restrict legacy access. The approved filter guard rejects a non-empty API-only key on the legacy chain with 403 before controller execution; the automatic authority grant also has the requested condition. The guard is required because /api/user/profile only checks isAuthenticated(). No controller annotations changed.

Permission.name is a VARCHAR(100) with a unique constraint, not an enum. WorkforcePermissionInitializer atomically inserts the three names at startup, without assigning them to any key or role. This is additive permission catalogue data, not a schema migration or backfill. Existing keys' stored grants are untouched. The initializer is idempotent and does not replace existing descriptions.

The UI uses permission checkboxes (`templates/api-keys/create.html`) and the service resolves a set of permission IDs. Mixed API-only and legacy grants are supported and intentionally unrestricted. A key that also holds legacy permissions retains legacy access; create v1 integration keys with API-only permissions. Empty keys are not classified v1-only, preserving baseline behavior.

## Seeded definition disclosure decisions

Every V5 seeded definition is listed below and checked against the migration by a regression test. Only explicitly safe seeds are exposed. The rule also applies to tenant overrides of seeded keys; section/type changes cannot bypass it.

| Key | Section | Type | Decision |
| --- | --- | --- | --- |
| qualification_type | QUALIFICATION | SELECT | Exposed |
| license_number | QUALIFICATION | TEXT | Excluded |
| issuing_body | QUALIFICATION | TEXT | Excluded |
| license_expiry_date | QUALIFICATION | DATE | Excluded |
| clearance_reference | BACKGROUND_CHECK | TEXT | Excluded |
| vetting_provider | BACKGROUND_CHECK | TEXT | Excluded |
| clearance_expiry_date | BACKGROUND_CHECK | DATE | Excluded |
| probation_end_date | EMPLOYMENT_STATUS | DATE | Excluded |
| notice_period_days | EMPLOYMENT_STATUS | NUMBER | Excluded |
| work_permit_number | EMPLOYMENT_STATUS | TEXT | Excluded |
| site_location_code | FIELD_ASSIGNMENT | TEXT | Exposed |
| gps_checkin_radius | FIELD_ASSIGNMENT | NUMBER | Excluded |
| assigned_equipment_id | FIELD_ASSIGNMENT | TEXT | Exposed |

No live custom-key inventory was read from the application database. The tests cover sensitive tenant keys and labels, section/type exclusions, foreign definitions, global fallback and tenant deactivation. Unknown global definitions fail closed. Tenant-created definitions use the approved keyword/type/section policy; arbitrary innocuous labels can still conceal sensitive content. The proposed sensitivity metadata below remains important.

## Existing legacy employee exposure reported without changes

Every existing authenticated key still receives API_KEY and can invoke the legacy employee reads when the tenant's employee module is enabled, subject to the existing tenant and organization service checks. These routes return Employee entities rather than the v1 allowlist DTOs.

Jackson-visible Employee properties are: id, tenant, employeeNumber, userId, firstName, lastName, email, workPhone, jobTitle, organizationUnit, managerId, costCenter, employmentType, status, complianceEmploymentStatus, backgroundCheckStatus, serviceProvider, hireDate, terminationDate, nationalId, taxNumber, licenseNumber, licenseCategory, licenseExpiryDate, emergencyContactName, emergencyContactPhone, emergencyContactRelation, residentialAddress, extendedAttributesJson; plus inherited createdAt, updatedAt, createdBy and updatedBy. The raw user and manager associations are JsonIgnore, but their computed IDs are exposed. tenant, organizationUnit and serviceProvider are entity associations, so their serializable nested properties may also be exposed depending on lazy loading and Jackson configuration. This report does not assert a finite safe nested DTO contract for that legacy route. Tightening existing-key behavior is a separate follow-up, not implemented here.

## Recommended migrations and limits

No migrations were applied or added. Recommendations requiring separate approval:

1. V13 (or next available version): add nullable sensitivity classification to field_definitions, initially treating unclassified fields as restricted. Add schema separately from any reviewed classification backfill; use a reversible backfill and explicit rollback plan. Do not infer safety automatically from labels.
2. Employees indexes on (tenant_id, updated_at, id) and (tenant_id, organization_unit_id, id); org units on (tenant_id, updated_at, id). These compound indexes were not found in the repository migrations; inspect live SHOW INDEX before applying.
3. employee_field_values(employee_id, recorded_at). V5 has the employee-only index and (employee_id, field_definition_id) uniqueness, but no employee/recorded_at compound index. Consider the update-filter execution plan before finalizing.

The org graph and effective field/reference configuration are loaded tenant-wide then paginated in memory; employee rows and totals are filtered/paged in SQL. This keeps SQL round trips bounded, but very large org/config/reference catalogues require memory and execution-plan review. Invalid parent graphs fail closed. Offset paging is not snapshot isolation across HTTP requests. Field deletion, definition changes, org visibility changes and missing historic timestamps require periodic full reconciliation. Pattern checks are not a substitute for explicit sensitivity classification.

Before Phase 3 billing: separately review and authorize billing API scope, Stripe authority, monetary/currency contracts, metering and idempotency, payment-failure gating and reconciliation. This phase does not add billing, payslip, webhook or provisioning endpoints, and makes no new claim about financial rounding/currency coverage.

## Files and exact diffs

New runtime files under src/main/java/com/mbhoni_creative/api:
- config/WorkforcePermissionInitializer.java
- controller/WorkforceController.java
- dto/WorkforceDtos.java
- error/WorkforceProblem.java and WorkforceExceptionHandler.java
- repository/WorkforceRepository.java
- security/WorkforcePermissions.java and WorkforceFieldPolicy.java
- service/WorkforceService.java

New tests under src/test/java/com/mbhoni_creative/api:
- ApiPathsParityTest.java
- WorkforceFieldPolicyTest.java
- WorkforcePermissionTest.java
- WorkforceIntegrationTest.java

Modified existing source files: api/security/ApiPaths.java and api/config/ApiSecurityConfig.java (one shared matcher); api/security/ApiScopes.java (three explicit scopes); config/ApiKeyAuthenticationFilter.java (approved legacy restriction); adminservice/impl/ApiKeyServiceImpl.java (allow only the three new read permissions in addition to its existing set). README.md adds endpoint contracts, examples and limitations. No existing test was edited.

Exact patches: shared-path-matcher.patch (matcher refactor); phase2-filter.patch (legacy filter alone); phase2-existing-files.patch (other existing-file changes, including documentation). The original Phase 1 filter additions are separately retained in filter-exclusions.patch. This report and these patches are new documentation files.
