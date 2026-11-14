# Admin JSON API — Phase 1

All application endpoints are under `/api/v1`. This is an additive, read-only API for tenant credentials. It does not replace the legacy `/api/employees`, `/api/user`, `/api/integrations`, `/api/tenants/...` or Stripe webhook endpoints.

## Authentication and tenant boundaries

Send `X-API-Key` over HTTPS. Keys are already SHA-256 hashed by the existing `ApiKeyService`; no new key store or schema migration is introduced. Use the existing key administration UI to create a replacement key, distribute it, then revoke the old key. Raw keys, hashes, reset tokens, passwords and audit actor fields are never response fields.

The API reuses `ApiKeyService.authenticate` inside a read-only transaction. Its existing `lastUsedAt` assignment is not persisted by this API. This behavior is verified against H2 with the real service and repository. Authentication and resource GETs do not create default settings/branding/quota rows. Legacy key authentication is unchanged.

The tenant is taken exclusively from the authenticated key. `tenantId` query parameters and `X-Tenant-ID` cannot select a different tenant. Context is cleared before and after each request, including exceptions. Foreign or missing user IDs return the same 404; access checks return `allowed: false` for either. Inactive users have no allowed permissions. Requests from inactive or lifecycle-restricted tenants return 403; the existing lifecycle service determines grace-period access.

API requests do not use UI sessions, create sessions, set session cookies, redirect to login, or require CSRF tokens. A UI session alone cannot authenticate to v1. There is no JWT implementation or bearer-token fallback.

## Scope mapping (deny by default)

| Existing key permission | API scopes |
| --- | --- |
| `USER_VIEW` | `users:read`, `access:check` |
| `TENANT_VIEW` | `tenant:read`, `quotas:read` |
| `CUSTOMIZATION_VIEW` | `branding:read` |
| `MODULE_VIEW` | `modules:read` |
| `SUBSCRIPTION_VIEW` | `entitlements:read` |

Any permission absent from this table grants nothing on v1. In particular, a write-only legacy permission does not imply read access. An authenticated key needs at least one mapped scope to use `/users/me` or retrieve the OpenAPI specification. Key identities have no user roles; their `roles` array is empty. Existing permissions and the key-creation flow are unchanged.

`POST /access/check` checks a user's effective permission using the existing `CustomUserPrincipal` authority derivation, including Owner rules. It is a permission check, not a promise that every resource-specific operation will succeed: organization boundaries, module eligibility and operation-specific policies still apply at the destination service.

## Endpoints

| Method and path | Required scope | Response data |
| --- | --- | --- |
| `GET /api/v1/users/me` | Any mapped scope | Key identity, tenant, permissions, scopes; no credentials |
| `GET /api/v1/users/{id}` | `users:read` | Safe user profile, roles and permissions |
| `POST /api/v1/access/check` | `access:check` | `{ "allowed": true/false }` |
| `GET /api/v1/tenant` | `tenant:read` | Profile and selected operational settings |
| `GET /api/v1/tenant/branding` | `branding:read` | Display name, logo URL, colors |
| `GET /api/v1/tenant/quotas` | `quotas:read` | Limits and active-user count |
| `GET /api/v1/tenant/modules` | `modules:read` | Paginated modules from tenant assignments/current plan |
| `GET /api/v1/tenant/entitlements/{module}` | `entitlements:read` | Module, enabled, plan code, valid-until |

Access-check body:

```json
{ "userId": 123, "permission": "EMPLOYEE_VIEW" }
```

Single responses use `{ "data": ... }`. Lists use:

```json
{ "data": [], "page": { "number": 0, "size": 25, "totalElements": 0 } }
```

Module list parameters: `page` (zero-based, default 0), `size` (1–100, default 25), `updatedSince` (ISO-8601). Invalid bounds return 400; out-of-range pages return an empty list with the filtered total. Sorting is stable by module code. Pagination is applied after timestamp filtering.

Branding defaults to the tenant name, no logo, primary `#2563eb` and secondary `#16a34a`. Missing settings use the existing entity's in-memory operational defaults. Missing quota overrides fall back to the plan's seat limit. Unknown storage/API usage is `null`, not zero. No storage or API usage meter is implied by this response. `validUntil` is the provider period end, trial deadline, or payment-grace deadline when known; otherwise it is null. An active user's existing roles/permissions are returned as data, never JPA entities.

## Incremental reads and timestamps

The current JPA auditing configuration uses the default local-time provider with `LocalDateTime`. It does not configure a UTC `DateTimeProvider`. The MySQL connection's `serverTimezone=UTC` is not a guarantee that application-generated local datetimes represent UTC. Unzoned `updatedSince`, for example `2026-11-13T09:00:00`, is interpreted in the server JVM time zone. Inputs with `Z` or an offset are converted to that zone before comparison. Keep the same JVM zone across replicas and restarts; historical timestamp conventions must be checked before changing deployment time zones.

The module timestamp is the latest `updatedAt` across the tenant assignment, shared catalog module, current subscription, current plan and applicable plan-module mapping. Filtering is strictly newer (`>`). If any contributing row has a missing timestamp, the result's timestamp is null and the row is included even with `updatedSince`, avoiding silent omissions. No columns are added.

Incremental lists are not a change log: deleted rows have no tombstones, and elapsed lifecycle deadlines do not update `updatedAt` until a writer runs. Consumers must periodically take full snapshots and query current entitlements before privileged operations. The other Phase 1 endpoints are single-resource endpoints and do not take `updatedSince`.

## Errors

Errors use `application/problem+json` with `type`, `title`, `status`, `detail` and stable `code`. The new advice is ordered ahead of the UI advice but applies only to `com.mbhoni_creative.api.controller`.

| Status | Code |
| --- | --- |
| 400 | `INVALID_REQUEST` |
| 401 | `UNAUTHENTICATED` |
| 403 | `FORBIDDEN` |
| 404 | `NOT_FOUND` |
| 409 | `CONFLICT` |
| 422 | `VALIDATION_FAILED` |
| 500 | `INTERNAL_ERROR` |

Malformed JSON/parameters produce 400; structurally valid access-check JSON with invalid fields produces 422. Details are sanitized and do not echo credentials, SQL or exception messages.

## Documentation routes

`GET /api/docs` is a public documentation shell. Enter a key to fetch the protected, generated springdoc specification at `/api/openapi.json`; the page does not persist the key. It renders the endpoint list and full specification. This is deliberately not Swagger UI: the API-only springdoc starter avoids exposing UI assets on the existing chain.

Only the new controller package and `/api/v1/**` paths are in the specification. The automatic springdoc YAML companion mapping is removed at startup. There are no added `/v3/api-docs`, `/v3/api-docs.yaml`, `/swagger-ui`, `/swagger-ui.html`, or `/api/openapi.json.yaml` endpoints. The existing chain's rules for those paths are unchanged. Tests assert both mappings and HTTP 404 responses.

The new chain has `@Order(0)` and exactly `securityMatcher("/api/v1/**", "/api/docs/**", "/api/openapi.json")`. Existing chains are unedited. `ApiKeyAuthenticationFilter` and `TenantContextFilter` each receive only the approved five-line `shouldNotFilter` addition; see [filter-exclusions.patch](filter-exclusions.patch).

## Future JWT integration

Before adding JWT authentication, define: trusted issuer URI or JWK set, expected audience, allowed signature algorithms, claim-to-tenant binding, subject/service identity, scope mapping, token lifetime/revocation expectations, and key rollover. Validate issuer, audience, signature and time bounds. Do not reuse OAuth client secrets as API token-verification credentials or derive tenancy from request parameters.

## Validation and rollout

No schema migration, data backfill, entity/table change, existing-test edit or UI-chain change is included in Phase 1. The only dependency addition is `springdoc-openapi-starter-webmvc-api:2.8.17` (plus its transitive dependencies). [Springdoc v2 documentation](https://springdoc.org/v2/) describes the API-only starter and path configuration.

Baseline on 2026-10-30: `mvn clean verify` — 205 tests, 0 failures, 0 errors, 1 existing opt-in MySQL skip (`V8_MYSQL_INTEGRATION` unset).

Phase 1 on 2026-11-13: `mvn clean verify` — **222 tests, 0 failures, 0 errors, 1 unchanged opt-in MySQL skip**. All 17 new API tests passed. Existing tests were not edited, disabled or skipped by this change. The exact changes to existing production files are also captured in [phase1-existing-files.patch](phase1-existing-files.patch).

New regression suites: `ApiSecurityIntegrationTest` (real legacy and v1 filter chains, scopes, RFC 7807, session isolation, docs, webhook signature path), `ApiReadServiceTest` (scoped queries, timestamps, pagination, permissions), and `ApiCredentialReadOnlyTest` (real JPA authentication with unchanged persisted key state).

This checkout already contained uncommitted billing/security work before Phase 1. API-only commits intentionally do not absorb that work; they depend on the reviewed working-tree baseline. Do not deploy the API commit in isolation from its existing baseline dependencies. No live database or provider operation was performed.

## Phase 2 workforce API

All endpoints are read-only, require `X-API-Key`, return numeric Long IDs, and use the existing `{data,page}` envelope. List endpoints accept `page` (zero-based, default 0) and `size` (1–100, default 25). Invalid bounds are rejected, not clamped. Lists have stable ascending ID order. Out-of-range pages are empty with the correct filtered total.

| Permission | Scope | Endpoints |
| --- | --- | --- |
| API_EMPLOYEE_READ | employees:read | /employees, /employees/{id}, /employee-fields |
| API_ORG_READ | orgunits:read | /org-units |
| API_REFERENCE_READ | reference:read | /reference/{type} |

No existing permission gains a workforce scope. `EMPLOYEE_VIEW`, `ORG_VIEW` and `MASTER_DATA_VIEW` remain unmapped for v1. Phase 1 has no API-only permission names. `WorkforcePermissions.API_ONLY` is the single API-only set; a non-empty key containing only these permissions is v1-only. On the legacy chain that key receives 403 before controllers run, including routes that merely require authentication. The v1 chain checks scopes, never the `API_KEY` authority.

**A key that also holds legacy permissions retains legacy access; create v1 integration keys with API-only permissions.** The current checkbox UI and key-creation service allow combining both kinds. Empty-permission keys retain their original legacy behavior and have no mapped v1 scopes. Startup adds only the three permission names, idempotently; it assigns no permissions to existing keys or roles. No schema migration is required.

### Filters and visibility

| Endpoint | Filters and behavior |
| --- | --- |
| GET /api/v1/employees | orgUnitId, status, updatedSince, page, size. Status is case-sensitive: ACTIVE, ON_LEAVE, PROBATION, SUSPENDED, TERMINATED. |
| GET /api/v1/employees/{id} | Positive numeric ID. Missing, foreign-tenant and excluded employees all return 404 EMPLOYEE_NOT_FOUND. |
| GET /api/v1/employee-fields | page, size. Safe effective global defaults plus tenant overrides. Inactive tenant definitions suppress the corresponding global default. |
| GET /api/v1/org-units | updatedSince, page, size. Flat hierarchy; tree is unsupported and rejected. There is no status attribute in the existing org-unit model, so none is fabricated. |
| GET /api/v1/reference/{type} | page, size. Only countries (COUNTRY), currencies (CURRENCY), employment-types (EMPLOYMENT_TYPE), employee-statuses (EMPLOYEE_STATUS). |

Organization visibility is computed once per request from one tenant-scoped projection query. Isolated units and all descendants are excluded. Cycles, missing parents and foreign-tenant parent references are excluded fail-closed. Employees with no org unit remain visible. Exclusions apply before employee counts, pagination and updatedSince. An excluded or foreign orgUnitId behaves like a nonexistent ID: an empty list and totalElements 0. managerId is null when the manager is hidden or belongs to another tenant. Org parent IDs come only from visible tenant units.

Reference category names are mapped from a fixed allowlist to bound SQL parameters, never interpolated into table or class names. Categories are data-driven and may be global or tenant-owned. Tenant code overrides take precedence; inactive tenant codes suppress global defaults. Only ID, code, value and locale are returned, never free-form metadata. No reference values are seeded by this phase; a supported type without configured records returns an empty list. These names refer to lookup categories, not Java enum expansion. Other category codes return 404 REFERENCE_TYPE_NOT_FOUND.

### Incremental synchronization

Phase 2 uses the server-local timestamp convention described above. Do not append Z to an unzoned stored datetime. ISO-8601 values with offsets are converted to the server JVM zone. The filter boundary is **inclusive (`>=`)** for employees and org units; the Phase 1 module filter remains strictly newer (`>`).

An employee matches if its updatedAt OR any tenant-valid custom value's recordedAt is >= updatedSince. SQL uses EXISTS, not per-employee queries. The returned updatedAt is the latest of those timestamps, including hidden custom fields so a permitted synchronization can detect a changed employee without receiving the sensitive value. Values linked to foreign-tenant definitions are ignored. Custom values have no audited updatedAt; the existing save service refreshes recordedAt on writes.

Deletions of custom values and changes to field definitions are not detected. Organization isolation changes can also change visibility without changing an employee timestamp. Run a periodic full resync (for example nightly) and reconcile removed records. Poll with updatedSince set to the **last successful sync's start time**, using a small overlap and deduplicating by ID. Keep the watermark until every page succeeds. Concurrent writes can shift offset pages; the overlap and periodic full resync remain necessary. Null timestamps do not match a non-null filter; full resync includes those rows. No updatedSince parameter is offered for definitions or reference data.

### Data deliberately excluded

The employee DTO explicitly exposes only id, employeeNumber, firstName, lastName, jobTitle, status, employmentType, orgUnitId, managerId, startDate, updatedAt and safe customFields. Summary and detail currently share the same disclosure boundary.

Excluded core data: email (not confirmed work-only), workPhone, nationalId, taxNumber, licenseNumber, licenseCategory, licenseExpiryDate, emergencyContactName, emergencyContactPhone, emergencyContactRelation, residentialAddress, complianceEmploymentStatus, backgroundCheckStatus, serviceProvider, terminationDate, costCenter, raw extendedAttributesJson, user/userId, tenant entities, organization entities, createdAt, createdBy and updatedBy. Salary, banking, passport, date of birth, personal contact, medical/disciplinary and attachment content are not added through dynamic data. The API never serializes a JPA entity.

`WorkforceFieldPolicy` is the single disclosure policy for both definitions and custom values. Known seed keys (including tenant overrides of those keys) must have an explicit safe decision and the expected section and type. Unknown global definitions are excluded. Tenant-created fields must pass section/type restrictions and normalized key AND label checks. BACKGROUND_CHECK, FILE and GPS_COORD are always excluded. Case and punctuation are normalized; sensitive patterns include identity, id number, national ID, clearance, vetting, license/licence, permit, passport, bank, account, salary, compensation, payroll, wage, tax, medical/health, disciplinary, birth/DOB, emergency, contact, phone/mobile, email, address/residence/residential, personal, document/attachment, GPS/latitude/longitude. Ambiguous duplicate field keys across sections are omitted from employee customFields. Custom values remain strings. Only SELECT definitions expose allowedOptions; validation expressions are not exposed.

Keyword checks cannot prove arbitrary tenant-authored fields are non-sensitive. Review tenant field configuration and the proposed sensitivity-flag migration before broader exposure. Full seeded-definition decisions are in phase2-report.md.

### Examples

Set BASE to the service URL and API_KEY to a v1-only key. The following responses illustrate the contract; IDs and values are examples.

```bash
curl -H "X-API-Key: $API_KEY" "$BASE/api/v1/employees?size=25&status=ACTIVE&updatedSince=2026-11-10T08:15:00"
```
```json
{"data":[{"id":101,"employeeNumber":"E101","firstName":"Avery","lastName":"Mokoena","jobTitle":"Engineer","status":"ACTIVE","employmentType":"FULL_TIME","orgUnitId":10,"managerId":null,"startDate":"2024-03-01","updatedAt":"2026-11-10T08:15:00","customFields":{"shirt_size":"M"}}],"page":{"number":0,"size":25,"totalElements":1}}
```
```bash
curl -H "X-API-Key: $API_KEY" "$BASE/api/v1/employees/101"
```
```json
{"data":{"id":101,"employeeNumber":"E101","firstName":"Avery","lastName":"Mokoena","jobTitle":"Engineer","status":"ACTIVE","employmentType":"FULL_TIME","orgUnitId":10,"managerId":null,"startDate":"2024-03-01","updatedAt":"2026-11-10T08:15:00","customFields":{"shirt_size":"M"}}}
```
```bash
curl -H "X-API-Key: $API_KEY" "$BASE/api/v1/employee-fields?size=25"
```
```json
{"data":[{"id":20,"key":"shirt_size","label":"Shirt size","section":"FIELD_ASSIGNMENT","type":"TEXT","required":false,"allowedOptions":[],"displayOrder":1}],"page":{"number":0,"size":25,"totalElements":1}}
```
```bash
curl -H "X-API-Key: $API_KEY" "$BASE/api/v1/org-units?updatedSince=2026-11-10T08:15:00"
```
```json
{"data":[{"id":10,"name":"Engineering","code":"ENG","parentId":null,"type":"DEPARTMENT","updatedAt":"2026-11-10T08:15:00"}],"page":{"number":0,"size":25,"totalElements":1}}
```
```bash
curl -H "X-API-Key: $API_KEY" "$BASE/api/v1/reference/countries"
```
```json
{"data":[{"id":1,"code":"ZA","value":"South Africa","locale":"en_ZA"}],"page":{"number":0,"size":25,"totalElements":1}}
```

Missing credentials: 401 UNAUTHENTICATED. Missing scope: 403 FORBIDDEN. Malformed timestamps, status or IDs: 400 INVALID_FILTER. Invalid pagination: 400 INVALID_PAGE_PARAMS. Employee lookup: 404 EMPLOYEE_NOT_FOUND. Unknown reference type: 404 REFERENCE_TYPE_NOT_FOUND. There is no org-unit detail endpoint; invalid orgUnitId filters return empty results instead of ORG_UNIT_NOT_FOUND. Errors use the Phase 1 problem-details infrastructure; successful reads never create rows.

## Phase 3a tenant billing reads

Phase 3a is GET-only. `/api/v1/invoices` represents **TenantInvoice, the tenant's customer invoices**, not Stripe subscription `Invoice` records. Billing accounts, payment details and Stripe identifiers are outside this API. No POST, PUT, PATCH, DELETE, sending, approval, payment, voiding or quotation conversion is exposed.

| Permission | Scope in Phase 3a | Behavior |
| --- | --- | --- |
| API_BILLING_READ | billing:read | All six billing GET endpoints |
| API_BILLING_WRITE | None yet | Reserved for Phase 3b; does not enable reads or writes |

Both permission names are included in the single `WorkforcePermissions.API_ONLY` constant along with the three workforce permissions. The unchanged legacy filter rejects keys containing only names from this set, including billing-only keys. Mixed keys retain legacy access. The catalogue initializers add names only; no existing key or role receives grants. Workforce initialization continues to seed its three workforce names; BillingPermissionInitializer handles the billing names separately. No dependency, entity, existing controller, UI owner check or global AuditorAware has changed.

| GET endpoint | Result and filters |
| --- | --- |
| /api/v1/invoices | Tenant customer invoices with stored line items; status, dateFrom/dateTo (issueDate), updatedSince, page, size |
| /api/v1/invoices/{id} | Invoice and stored line items |
| /api/v1/quotations | Quotations with stored line items; status, dateFrom/dateTo (issueDate), updatedSince, page, size |
| /api/v1/quotations/{id} | Quotation and stored line items |
| /api/v1/expenses | Expenses; dateFrom/dateTo (expenseDate), updatedSince, page, size |
| /api/v1/expenses/{id} | Expense; no status or line items exist in this model |

IDs are JSON numbers and positive numeric path parameters. Lists sort by id ascending, use page=0 and size=25 by default, and reject size outside 1–100. Date ranges include both endpoints; reversed ranges are invalid. Status names are case-sensitive. Unknown/duplicate list parameters are rejected. There is no customer ID relation: customer details and expense categories/vendors are strings, so customerId is unsupported and returns INVALID_FILTER. Expenses reject status rather than inventing a lifecycle. The three models have no employee/org-unit relations, so there are no linked isolated records to filter; tenant scoping is enforced on every parent and item query. No organization association is inferred from names, notes or descriptions.

updatedSince uses the server-local convention documented above, with an inclusive >= boundary. Invoice and quotation filtering includes either the parent updatedAt or any line item's updatedAt; responses report their latest timestamp. Expense filtering uses its own updatedAt. A foreign tenant's parent cannot contribute items or timestamps. Deletions have no tombstones. Use overlap, deduplicate by ID, and periodically take full snapshots; offset pages can move during concurrent updates. Null timestamps do not match a supplied watermark.

### Stored money and currency

All amounts, quantities and tax rates are decimal strings from stored BigDecimal values, never JSON floating-point numbers. GET does not recalculate, re-round, or normalize unknown currency. No authoritative single operating-currency field was found in Tenant, TenantSettings, IndustryProfile or BillingAccount. A country or multi-currency toggle is not a currency label. Therefore **currency is omitted**: amounts are in the tenant's operating currency with no per-document label. Reads work even when multiCurrencyEnabled is true. A nullable persisted currency field is a recommended later migration, not applied here.

The stored document total is authoritative. Stored line totals may sum to a different amount because the existing code rounds each line but calculates document tax from unrounded line bases. Examples verified for both invoices and quotations:

| Input at the existing 15 percent default | Sum of stored line totals | Stored document total |
| --- | --- | --- |
| 2 lines, quantity 1.00, price 0.03 | 0.06 | 0.07 |
| 100 lines, quantity 1.00, price 0.03 | 3.00 | 3.45 |
| 3 lines, quantity 0.33, price 0.05 | 0.06 | 0.06 |

The existing UI save service constructs invoice and quotation lines with a **15.00% tax default for every line**, without a VAT-registered, tax-exempt or zero-rate decision from tenant settings. This is unsuitable for exempt, zero-rated or non-VAT cases and requires a separate business fix. Phase 3a only returns stored data and changes no tax rules.

### Disclosure and errors

Invoice/quotation responses expose ID, number, customer name, status, dates, stored totals, updatedAt and explicit line DTOs. Invoices also expose stored amountPaid. Expense responses expose ID, expenseDate, vendor, category, reference, amount, taxAmount and updatedAt. Excluded: customer contact/address/tax numbers, internal notes, payment terms, expense paymentMethod, billing-account details, bank/payment credentials, gateway tokens/Stripe IDs, raw entity relationships, convertedInvoiceId, cost/margin figures and createdBy/updatedBy/createdAt. Line descriptions and counterparty names are intentionally business data returned under billing:read; they are not logged by these endpoints.

401 UNAUTHENTICATED means no valid key; 403 FORBIDDEN means missing read scope. Invalid IDs, statuses, dates or unsupported filters return 400 INVALID_FILTER; invalid paging returns 400 INVALID_PAGE_PARAMS. Missing and foreign resources are indistinguishable 404 INVOICE_NOT_FOUND, QUOTATION_NOT_FOUND or EXPENSE_NOT_FOUND. Every GET is read-only and creates no session, cookie, settings, document or idempotency row.

### Read examples

```bash
curl -H "X-API-Key: $API_KEY" "$BASE/api/v1/invoices?status=DRAFT&dateFrom=2026-01-01&size=25"
curl -H "X-API-Key: $API_KEY" "$BASE/api/v1/invoices/1"
```
```json
{"data":{"id":1,"number":"INV-2026-001","customer":"Example customer","status":"DRAFT","issueDate":"2026-01-01","dueDate":"2026-02-01","subtotal":"10.00","taxAmount":"1.50","totalAmount":"11.50","amountPaid":"0.00","updatedAt":"2026-01-01T10:00:00","items":[{"id":1,"description":"Service","quantity":"1.00","unitPrice":"10.00","taxRate":"15.00","totalAmount":"11.50"}]}}
```
```bash
curl -H "X-API-Key: $API_KEY" "$BASE/api/v1/quotations?updatedSince=2026-01-01T00:00:00"
curl -H "X-API-Key: $API_KEY" "$BASE/api/v1/quotations/1"
```
```json
{"data":{"id":1,"number":"QT-2026-001","customer":"Example customer","status":"DRAFT","issueDate":"2026-01-01","validUntilDate":"2026-01-15","subtotal":"10.00","taxAmount":"1.50","totalAmount":"11.50","updatedAt":"2026-01-01T10:00:00","items":[{"id":1,"description":"Service","quantity":"1.00","unitPrice":"10.00","taxRate":"15.00","totalAmount":"11.50"}]}}
```
```bash
curl -H "X-API-Key: $API_KEY" "$BASE/api/v1/expenses?dateFrom=2026-01-01&dateTo=2026-01-31"
curl -H "X-API-Key: $API_KEY" "$BASE/api/v1/expenses/1"
```
```json
{"data":{"id":1,"expenseDate":"2026-01-01","vendor":"Example vendor","category":"SUPPLIES","reference":"REF1","amount":"1250.50","taxAmount":"100.00","updatedAt":"2026-01-01T10:00:00"}}
```
List responses wrap the same DTOs in an array and add `"page":{"number":0,"size":25,"totalElements":1}`. Dates, IDs and values above are illustrative.

### V13 storage and Phase 3b boundary

V13 creates only api_idempotency and its expiry index. It stores tenant_id, api_key_id, a case-sensitive binary idempotency_key, SHA-256 request_hash, response_status/body/location, resource_id, created_at and expires_at. UNIQUE(tenant_id,idempotency_key) gives tenant-wide key identity across integrations. The tenant FK cascades tenant deletion; api_key_id is retained as historical attribution without a key-deletion cascade. A key is intended to be 1–128 printable ASCII characters; storage uses VARBINARY(128) to avoid MySQL collation folding. These header limits, canonicalization, replay, in-flight conflicts and the proposed 24-hour lifetime are **not active endpoint contracts yet**: no POST or idempotency writer exists in Phase 3a. No cleanup job is started for the unused table. V14, expiry/retry handling and atomic document creation are Phase 3b work.

The v1 Authentication now reports API_KEY:<id> through getName(), so the unchanged JPA auditor can attribute future v1 writes. A real-JPA regression verifies API attribution and unchanged UI username attribution. No audit write is made on GET. Future create operations must also record key ID, tenant, endpoint, idempotency key and resource ID with the existing audit service.

**Expense writes are not drafts.** In the existing model a new expense immediately contributes to the expense page's totalExpenses and netProfit calculation. That page currently uses paid Stripe subscription invoices for revenue. This report-impact concern must be reviewed before enabling expense POSTs in Phase 3b. No expense write is available now.
