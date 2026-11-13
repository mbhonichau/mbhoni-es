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
