# Phase 3b implementation report

Branch: feature/json-api. Scope: invoice and quotation draft creation only. Deployment writes default to disabled. No expense creation, payslips, webhook delivery or provisioning was implemented.

## Findings and resolution

| Original finding / requirement | Fix applied | Files | Regression | Breaking change | Follow-up |
| --- | --- | --- | --- | --- | --- |
| Reserved write permission had no scope | Exact billing:write mapping; write-only keys can access only the two POST paths | ApiScopes, ApiKeyFilter | Approved assertions plus BillingDraftIntegrationTest | No; explicitly approved additive capability | Assign grants deliberately; default switch remains off |
| No separate API number allocator | V14 new tenant/type sequence, pessimistic locks and lazy creation inside the document transaction | V14__api_document_sequence.sql, ApiBillingWriteService | ApiDocumentSequenceMigrationTest; parallel, rollback and tenant tests | No | Accountant sign-off; UI count-based series still skips |
| First API writes need safe boundaries | Explicit tenant/scope/switch checks, strict request parser, DRAFT-only entity construction, reuse existing calculation methods | BillingWriteSettings, BillingDraftParser, BillingDraftRequest, BillingWriteController, ApiBillingWriteService, BillingWriteProblem, BillingWriteExceptionHandler | Both POSTs: success, auth, switch, validation, money, multicurrency | No | Existing universal 15% behavior requires separate tax-policy work |
| Retries could duplicate documents | Tenant-scoped V13 key, canonical request hash, stored response/Location, 24-hour expiry and cleanup | ApiBillingWriteService, ApiIdempotencyCleanup | Replay, different body/endpoint, expiry, race, tenant isolation, rollback | No | Retrying after expiry requires reconciliation |
| API attribution must not impersonate UI users | Existing Authentication name and JPA auditor preserved; existing audit service receives tenant/key ID/endpoint/idempotency key/resource ID | ApiBillingWriteService | Persisted API and UI createdBy assertions; audit verification | No | No raw API key logging |
| Drafts affect existing consumers | WARN on enabled startup, production checklist and documented retained behavior | BillingWriteSettings, README, .env.example, draft-aggregate-followup.md | Named aggregate/numbering tests; real owner export test; warning test | No | Apply aggregate/export/numbering follow-up before production |

The two authorized existing-test edits are isolated in commit `fbae2b2` (`test(api): update scope contract for billing:write`), before V14 commit `168aed8` and before endpoint implementation. Every other existing test is unchanged. Exact diffs: [scope contract](phase3b-scope-contract.patch) and [other modified existing files](phase3b-existing-files.patch).

No shared helper was extracted, so there is no helper-extraction patch. The API-only permission constant already contained API_BILLING_WRITE and is unchanged, so there is no API-only-constant patch. No legacy filter, controller, entity, global auditor, export, aggregate or UI allocator changed.

## New and modified files

New runtime files under `src/main/java/com/mbhoni_creative/api/`:

- `config/BillingWriteSettings.java`
- `controller/BillingWriteController.java`
- `dto/BillingDraftRequest.java`
- `error/BillingWriteProblem.java`, `error/BillingWriteExceptionHandler.java`
- `service/ApiBillingWriteService.java`, `service/BillingDraftParser.java`, `service/ApiIdempotencyCleanup.java`

New migration: `src/main/resources/db/migration/V14__api_document_sequence.sql`.

New tests under `src/test/java/com/mbhoni_creative/api/`: `BillingDraftIntegrationTest.java`, `ApiDocumentSequenceMigrationTest.java`, `BillingWriteWarningTest.java`.

Modified existing runtime files: `api/security/ApiScopes.java`, `api/security/ApiKeyFilter.java`, `api/config/BillingPermissionInitializer.java`. Modified existing tests: only `BillingReadContractTest.java:23` and the replacement block at `WorkforcePermissionTest.java:44`. Documentation: `.env.example`, `docs/api/README.md`, and new follow-up/report/patch artifacts in this directory. Existing stored permission descriptions are not rewritten; fresh initialization uses the current draft-create description, with grants unchanged.

## Calculation and status evidence

Current central scope mapping (unmapped permissions grant nothing):

| Permission | Scopes |
| --- | --- |
| USER_VIEW | users:read, access:check |
| TENANT_VIEW | tenant:read, quotas:read |
| CUSTOMIZATION_VIEW | branding:read |
| MODULE_VIEW | modules:read |
| SUBSCRIPTION_VIEW | entitlements:read |
| API_EMPLOYEE_READ | employees:read |
| API_ORG_READ | orgunits:read |
| API_REFERENCE_READ | reference:read |
| API_BILLING_READ | billing:read |
| API_BILLING_WRITE | billing:write only |

The complete set of write scopes is tested as exactly `{billing:write}`. These scopes are v1-only; the legacy filter continues to reject keys containing only API-only permissions. Mixed keys retain baseline legacy behavior.

The UI service has no private calculation helper to extract. `TenantInvoicingServiceImpl.saveInvoice` and `saveQuotation` create existing line entities, call `addItem`, then `recalculateTotals`. The new API uses those same public entity methods. Its request validation is deliberately stricter than the UI's blank-line skipping and missing-value defaults; required descriptions, quantities and prices are supplied explicitly.

`TenantInvoiceItem.calculateTotal` and `TenantQuotationItem.calculateTotal` compute quantity × price × (1 + taxRate / 100), rounding each line to scale 2 with HALF_UP. Both documents' `recalculateTotals` sum unrounded bases and tax, round subtotal and tax separately with HALF_UP, then add them. Default taxRate is 15.00. No existing method was changed. No tenant VAT-registration/exemption boolean or tenant tax-status decision was found; tax-number strings do not establish such status.

**Financial rounding has regression coverage.** Actual API-created and UI-service-created invoices and quotations are compared for 2 and 100 lines at 1.00 × 0.03, and 3 lines at 0.33 × 0.05. Expected document totals are 0.07, 3.45 and 0.06. Decimal bounds, negative values, excess scale, numeric JSON, zero, overflow and maximum inputs are tested. **Currency calculation is not supported or claimed:** request currency is rejected and multi-currency tenants cannot create drafts; those exclusions are tested.

Invoice statuses: DRAFT, ISSUED, PAID, PARTIALLY_PAID, OVERDUE, CANCELLED. Quotation statuses: DRAFT, SENT, ACCEPTED, REJECTED, CONVERTED_TO_INVOICE, EXPIRED. DRAFT is the default for both entities, and API creation sets it explicitly. No transition, issue, send, approve, pay, cancel, void, convert, edit or delete operation is exposed. Neither enum contains VOID. Existing UI transitions remain as they were.

## Concurrency, query costs and indexes

The service locks the authenticated tenant row with FOR UPDATE NOWAIT, then performs current/locking idempotency reads and locks the tenant/type sequence row. This makes lazy initialization safe across nodes and avoids repeatable-read snapshot races on MySQL. The tenant lock is intentionally coarse: simultaneous distinct writes in the same tenant can receive the same retryable 409 as a duplicate. Clients must use jittered backoff and the same idempotency key. Different tenants remain independent. Only recognized lock failures are translated; unrelated database failures propagate and roll back.

Measured H2 prepared-statement counts for either document: 1 line = 12, 50 lines = 61, 200 lines = 211; replay = 5 at every size. These counts include real tenant/subscription/settings reads and JPA document/line persistence, but credential authentication and AuditLogService are mocked. Production adds credential lookup queries and the audit insert. There are no per-line SELECTs; each line requires an insert. Existing read-list tests still verify 3 queries for both 1 and 50 documents, with authentication/tenant repositories mocked.

V13 already indexes expiry and uniquely indexes tenant/key. V14 uniquely indexes tenant/type; no additional sequence index is needed. Recommended only, not applied: review `(tenant_id, status, issue_date, id)` on document tables against actual read plans, and `(invoice_id, updated_at)` / `(quotation_id, updated_at)` on lines for composite updatedSince. Assess existing indexes first. No existing-table index or backfill was added.

Recommend gateway limits per tenant and key, with a conservative write burst, bounded request size, 429/Retry-After, and metrics for conflicts/replays/latency before production. No new dependency or rate-limiter was added. Item count is capped at 200 and request text at 262144 characters.

## Expense readers and retained behavior

`BillingController` `/billing/expenses` reads ExpenseRepository, computes totalExpenses at line 328 and netProfit at line 344. Its revenue source is PAID subscription Invoice records, not TenantInvoice. `TenantExportController` exports business_expenses. `BillingReadRepository` backs `/api/v1/expenses` and `/api/v1/expenses/{id}`. These are the expense-table readers found; no additional dedicated expense dashboard or scheduled report consumer was identified. Expense creation remains unavailable through v1.

API drafts continue to affect Total Invoiced, Outstanding Balance and Total Quoted Value, appear in the owner export with status, and advance UI count-based numbering. Unpaid drafts add zero to Total Collected. Temporary regression names contain `pendingDraftAggregateExclusion` and `pendingUiNumberingFix`. See [the complete query inventory and proposed follow-up](draft-aggregate-followup.md). No follow-up query fix was applied.

## Operational limits and next phases

V14 is backward-compatible new storage, with no data backfill. Disable writes before rolling back application code; retain idempotency and sequence state to avoid duplicate/reused numbers. Existing documents/number columns gain no constraints. Retained UI numbering can reissue numbers after deletions; accountant sign-off and the documented production checklist remain required. The deployment switch has no tenant allowlist: initial rollout must use a controlled deployment and grants only for the test tenant.

Before payslips: define payroll-specific permissions, confidentiality and redaction, statutory calculations, approval lifecycle and retention. Before outbound webhooks: approve an event contract, durable outbox, tenant-scoped signing/rotation, delivery retries and replay protection. Before provisioning: define tenant-owner authority, entitlement checks, idempotent lifecycle transitions, rollback/compensation and audit contracts. None is implemented here.

## Verification

Initial `mvn clean verify`: 267 tests, zero failures/errors, three existing opt-in MySQL skips. Full MySQL-enabled `mvn verify`: 269 tests, zero failures/errors/skips, including V14 on MySQL 8.0.46 and the existing V8/V12/V13 checks. Existing V12 uses its own `V12_MYSQL_INTEGRATION` gate, so both it and `V8_MYSQL_INTEGRATION` were enabled without changing the existing test. Disposable servers used ports 13368/13369 and verified test data directories; the application database was not touched. New query-count/export regressions are included in the final clean run recorded below.

Final `mvn clean verify`: **271 tests, 0 failures, 0 errors, 3 skips** (only the existing opt-in MySQL tests), including both added query-count and real owner-export tests. Production code was unchanged after the successful MySQL-enabled run. Both disposable servers were shut down before the final clean run. Evidence logs: `admin-service/api-phase3b-final-verify.log`, `admin-service/api-phase3b-mysql-verify.log`, and `admin-service/api-phase3b-query-verify.log` (local ignored build logs). Existing ApiPaths parity, legacy-route authority/CSRF behavior and Stripe signature regressions pass unchanged.
