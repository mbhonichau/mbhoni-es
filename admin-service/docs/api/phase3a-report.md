# Phase 3a billing read report

## Delivered and verified

Phase 3a is complete as GET-only billing reads, V13 idempotency storage preparation, and billing API-only key classification. Phase 3b has not started. No V14, sequence allocation, POST endpoint, replay handler, expiry job, financial write, payslip, webhook or provisioning feature was implemented.

- V13 commit: d8e682c (`feat(api): add idempotency schema (V13)`).
- Clean verification: 256 tests, zero failures/errors, three opt-in skips.
- Full `mvn verify` with local MySQL 8.0.46 enabled: 256 tests, zero failures/errors, zero skips. V8, V12 and V13 passed on disposable databases; V13 also passed on H2.
- V8 and V13 use V8_MYSQL_INTEGRATION; the unchanged preexisting V12 test uses V12_MYSQL_INTEGRATION. Both flags were enabled. No existing test was modified, weakened or skipped for any other reason.
- New test methods: ApiIdempotencyMigrationTest (2), BillingReadIntegrationTest (9), BillingReadContractTest (2), ApiAuditAttributionTest (1). Phase 2 baseline was 242, now 256.
- Invoice list including lines: 3 SQL queries for 1 document and 3 for 50 documents. Quotation lists: also 3 and 3. These are datasource-counter measurements of actual JDBC reads: count, page, and a tenant-scoped batch item query. Authentication/tenant/lifecycle repositories are mocked in that test and excluded from the count. Expense lists need count plus page. No per-document lazy entity loading is used.
- Complete table contents are unchanged after all six reads, including the empty idempotency table; tests also verify read-only transactions, tenant isolation, errors, money strings, OpenAPI, no session/cookie, and legacy-route protection.
- No schema change was applied to the application database during this task. Only read-only aggregate duplicate counts were queried there; migrations ran on disposable test databases.

## Scope and disclosure

/api/v1/invoices targets TenantInvoice only, not Stripe subscription Invoice. Quotations target TenantQuotation; expenses target Expense/business_expenses. Billing accounts are excluded. No existing UI owner authorization or billing service was changed, and no calculation helper extraction was needed for reads. ApiBillingReadService explicitly requires billing:read and validates the authenticated key's tenant through the existing API identity service. Read projections do not invoke owner-only UI write services or duplicate financial calculations.

| Permission | Phase 3a scope | Effect |
| --- | --- | --- |
| API_BILLING_READ | billing:read | Six GET endpoints |
| API_BILLING_WRITE | No scope until Phase 3b | Reserved catalogue name; no read/write access |

The shared API_ONLY constant now contains API_EMPLOYEE_READ, API_ORG_READ, API_REFERENCE_READ, API_BILLING_READ and API_BILLING_WRITE. There are no Phase 1 API-only names. The Phase 2 legacy filter is unchanged: its shared helper now also classifies billing-only keys and returns 403 on legacy paths. Mixed and existing keys retain their previous authority sets/access. Key creation still allows mixing names. No grant is assigned to an existing key or role. The permission-name column is VARCHAR(100), so no permission schema migration is needed.

Sensitive/excluded response fields: invoice/quotation customerEmail, customerPhone, customerAddress, customerTaxNumber, notes, paymentTerms, convertedInvoiceId; expense notes and paymentMethod; billing-account address/tax/contact details; payment/bank details, gateway tokens and Stripe IDs; tenant/entity graphs; createdAt/createdBy/updatedBy; cost/margin calculations. Customer name, vendor, reference and line description are intentionally exposed business data under billing:read. Controllers do not log their bodies or descriptions. updatedAt is exposed solely for synchronization.

BillingAccount itself has billing contact/address/tax properties and active status; no bank credential fields were found in that entity. StripeInvoiceService/Invoice contains separate provider identifiers and snapshots; those records are never queried by this API.

## Model and business rules found

| Model | Storage and behavior |
| --- | --- |
| TenantInvoice | tenant_invoices; tenant relation, customer strings, DRAFT default, issue/due dates, DECIMAL(12,2) totals, stored amountPaid, notes/payment terms. No currency/customerId/employee/org-unit/billing-account relation. |
| TenantQuotation | tenant_quotations; tenant relation, customer strings, DRAFT default, issue/valid-until dates, DECIMAL(12,2) totals, notes, convertedInvoiceId. No currency/customerId/employee/org-unit relation. |
| Invoice/quotation line | Parent FK; quantity/unitPrice/totalAmount precision 12 scale 2; taxRate precision 5 scale 2; description; audited timestamps. Tax defaults to 15.00. No tax code or discount model. |
| Expense | business_expenses; tenant relation, category/vendor/reference strings, date, BigDecimal amount and taxAmount, paymentMethod, notes. JPA does not declare amount precision/scale explicitly. No draft/status, item, generated number, customer or org/employee relation. |

No isolated-organization linkage exists in these models. The API does not infer sensitive relationships from free-text names, descriptions or notes. All parent queries require the key's tenant; line queries join the parent and independently bind that tenant. Unsupported customerId, tenantId or organization filters are rejected on lists, rather than becoming query selectors.

The existing saveInvoice/saveQuotation methods replace items from parallel description/quantity/price lists, default missing quantity to one and price to zero, call entity calculations, and save. They require TenantGovernance.requireBillingOwner, which expects a real user principal. They are not safe API-key entry points unchanged. Phase 3b must use the approved explicit API service entry points and mirror calculations without impersonation. No new claim is made that the UI service validates all signs, scales or magnitudes; Phase 3b input validation is still required.

Invoice status enum: DRAFT, ISSUED, PAID, PARTIALLY_PAID, OVERDUE, CANCELLED. Quotation enum: DRAFT, SENT, ACCEPTED, REJECTED, CONVERTED_TO_INVOICE, EXPIRED. These enums are not an enforced transition graph. The UI save methods persist supplied entity status; recordPayment adds positive payment and sets PAID when the paid total reaches the total. Conversion creates a DRAFT invoice and marks the quotation CONVERTED_TO_INVOICE, and deletion methods exist. Phase 3a exposes none of these transitions or mutations.

## Numbering and requested duplicate inventory

Existing saveInvoice/saveQuotation and quotation conversion use tenant record count + 1, with INV-year-NNN / QT-year-NNN formats. No locking or per-tenant number uniqueness constraint protects those paths. Concurrent writes and deletion/recreation can collide.

On 2026-11-14, a read-only transaction against the configured application database returned:

| Table | Duplicate (tenant, document number) groups | Extra rows beyond first |
| --- | --- | --- |
| tenant_invoices | 0 | 0 |
| tenant_quotations | 0 | 0 |

Query shape: `SELECT COUNT(*), COALESCE(SUM(n-1),0) FROM (SELECT tenant_id, invoice_number, COUNT(*) n FROM tenant_invoices GROUP BY tenant_id, invoice_number HAVING COUNT(*)>1) duplicates`, and equivalently for quotation_number. No document contents or customer details were returned. This is a snapshot, not proof of safe numbering. The approved separate API series and V14 row-lock sequence table remain exclusively Phase 3b work, pending the accountant's separate invoice-series decision.

## Currency, tax and existing rounding

No authoritative operating/default currency field was found in Tenant, TenantSettings, IndustryProfile or BillingAccount. multiCurrencyEnabled is only a boolean; country/region is not a reliable currency. Responses therefore omit currency and preserve stored decimal strings without conversion. Reads also work for multi-currency tenants. A later nullable currency column is recommended; no currency schema change was added.

The earlier Stripe statement was taken from an earlier message in this chat, not a repository rule: "Stripe should be the sole source of truth for all charge calculation, tax, proration, and retries." Applying that broadly to TenantInvoice was incorrect. The current explicit decision confines Stripe authority to subscription billing and makes existing tenant-document calculations authoritative for this work.

UI saves construct each invoice/quotation item with a 15.00 taxRate default. No VAT-registered, exempt or zero-rate switch is checked in these save paths, and no such tenant-setting flag was found. The builder template also labels tax as 15%. This is an existing limitation for exempt, zero-rated and non-VAT tenants; it was reported and not changed. Future POSTs must not accept taxRate and must mirror the approved existing behavior unless a separate correction is authorized.

| Existing inputs, for both document types | Stored line sum | Stored document total | Difference |
| --- | --- | --- | --- |
| 2 lines at quantity 1.00 and price 0.03 | 0.06 | 0.07 | 0.01 |
| 100 lines at quantity 1.00 and price 0.03 | 3.00 | 3.45 | 0.45 |
| 3 lines at quantity 0.33 and price 0.05 | 0.06 | 0.06 | 0.00 |

BillingReadContractTest records these cases against the existing entities, while HTTP tests assert the API returns stored totals and exact decimal strings unchanged. The line calculation rounds each total; document subtotal and tax sum unrounded bases before rounding. The stored document total is authoritative. API-created-versus-UI-service golden comparisons are deferred until creates exist in 3b. These read tests do not establish general tax-law or multi-currency correctness.

## Expense reporting concern before Phase 3b

The current expense category list is hardcoded in BillingController: OFFICE_RENT, UTILITIES, SALARIES, SOFTWARE_SERVICES, SUPPLIES, TRAVEL_TRANSPORT, EQUIPMENT, MARKETING. It is not a tenant-owned category entity.

New expenses are immediately normal records. The expense page sums all amounts into totalExpenses and subtracts this from totalPaidRevenue to display netProfit. That revenue is taken from paid subscription Invoice records, not TenantInvoice customer invoices. No expense approval workflow was found in the inspected paths. Unreviewed API expenses would immediately alter this report, and the revenue basis is itself a separate concern. Review this before enabling expense creation in 3b; Phase 3a remains safe read-only work.

## Attribution and idempotency boundary

ApiKeyAuthentication is used only by the v1 chain and reports API_KEY:<id> through getName(). The existing global SpringSecurityAuditorAware is unchanged. ApiAuditAttributionTest persists audited entities with real JPA to verify stable API key attribution and unchanged logged-in UI username attribution. GET requests do not create audit events or mutate records. In 3b, explicit audit calls must include tenant, key ID, endpoint, idempotency key and created resource ID, never the raw key; the existing audit mechanism already understands ApiKeyPrincipal.

V13 creates api_idempotency, its tenant FK, tenant/key uniqueness and expiry index, with no changes to existing tables. Binary keys enforce case-sensitive byte identity consistently on MySQL and H2; request_hash holds a SHA-256 hex digest. response_location was included so future replays can preserve Location without recomputing it. api_key_id is historical attribution without a key FK, so key deletion cannot erase replay state; tenant deletion cascades it. No request body is stored apart from the future response snapshot. The intended 1–128 printable ASCII key contract and proposed 24-hour TTL are preparation only. No writes/replay/409 handling/cleanup job are active in 3a. V13 tests cover migration, validation/rerun, tenant uniqueness, case sensitivity, FK rejection, rollback and response-field storage. They do not claim a working idempotent POST race; that belongs to 3b.

Rollback before any Phase 3b use: stop application versions that depend on this table, verify it is empty, then a separately approved `DROP TABLE api_idempotency` removes the new table and index. Do not automatically delete Flyway history or response records. Once keys are in use, preserve/export replay records and plan rollback explicitly. No rollback was run.

## Indexes and operational follow-ups not applied

Recommend reviewing live execution plans and indexes before adding (tenant_id, issue_date, id), (tenant_id, updated_at, id), and optionally (tenant_id, status, id) on tenant_invoices/tenant_quotations; (invoice_id, updated_at) and (quotation_id, updated_at) on their item tables; and (tenant_id, expense_date, id)/(tenant_id, updated_at, id) on business_expenses. These are recommendations, not applied migrations. Avoid duplicate indexes if deployment schema already has them. Audited BaseEntity timestamps exist on all these entities and lines; incremental reads include child timestamps. Deletes have no tombstones, and full reconciliation is still necessary.

Rate limiting is out of scope. Recommend per-tenant and per-key distributed token buckets at the gateway, with separate read/write budgets, 429 plus Retry-After, and idempotent retries counted consistently. No rate limiter or dependency was added.

Lists cap documents at 100, but nested line counts are not independently paginated. High-line-count documents need payload/latency monitoring. Stored customer/description text is intentionally disclosed to billing:read; API-only scope grants should be deliberate. Full application deployment was not performed. Existing UI financial calculations, numbering, permissions and report definitions remain unchanged.

Before 3b: review the expense-report concern; implement V14 and API series, narrow DTO writes and unknown-property rejection, money/length validation, multicurrency write refusal, calculation golden tests, transactionally atomic idempotency, concurrency/replay/expiry tests and write audit events. No payslip, webhook or provisioning work is included.

## Files and exact diffs

New runtime files: api/config/BillingPermissionInitializer.java; api/controller/BillingReadController.java; api/dto/BillingReadDtos.java; api/error/BillingReadProblem.java and BillingReadExceptionHandler.java; api/repository/BillingReadRepository.java; api/service/ApiBillingReadService.java; api/security/ApiKeyAuthentication.java; db/migration/V13__api_idempotency.sql.

New tests: ApiIdempotencyMigrationTest.java, BillingReadIntegrationTest.java, BillingReadContractTest.java, ApiAuditAttributionTest.java, all under src/test/java/com/mbhoni_creative/api.

Modified existing API files: WorkforcePermissions.java extends the shared set; ApiScopes.java maps billing:read; WorkforcePermissionInitializer.java leaves billing names to its dedicated initializer; ApiKeyFilter.java instantiates the named v1 authentication token. README.md documents the read contract and phase boundary. No existing service, controller, filter outside api, global auditor, entity, dependency, migration or existing test was changed.

`phase3a-existing-files.patch` contains exact existing-file changes. `phase3a-legacy-key.patch` separately records the shared API-only constant change; the legacy ApiKeyAuthenticationFilter itself has zero diff in Phase 3a and retains its Phase 2 403 guard. The original guard diff remains in phase2-filter.patch. New file contents are in the commits.

## Legacy billing route inventory

The table below gives each route and its exact controller authorization expression. The API-key filter authenticates existing/mixed keys on this chain. These expressions are only the controller layer: UI invoice/quotation services additionally require the billing owner, CSRF still applies to legacy POSTs, and tenant checks remain in place. No billing endpoint was found in /api/integrations (only identity/user reads), and no extra billing fetch/AJAX endpoint was found in the inspected billing/invoice templates.

BillingReadIntegrationTest iterates every mapping in BillingController and TenantInvoiceViewController, including /billing with no method path, and tests both API_BILLING_READ-only and API_BILLING_WRITE-only keys. Every request receives 403; POST tests supply CSRF so the result exercises the key restriction. Module entitlements are deliberately true, proving the alternative predicates do not bypass the filter. Existing/mixed key behavior is compared on representative legacy routes; the original regression suite remains unchanged.

| Method and route | Exact @PreAuthorize expression |
| --- | --- |
| GET /billing/accounts | `hasAuthority('BILLING_VIEW')` |
| POST /billing/accounts/save | `hasAuthority('BILLING_EDIT')` |
| GET /billing/invoices | `hasAuthority('BILLING_VIEW')` |
| POST /billing/invoices/view | `hasAuthority('BILLING_VIEW')` |
| POST /billing/invoices/sync | `hasAuthority('BILLING_VIEW')` |
| POST /billing/invoices/refresh-overdue | `hasAuthority('BILLING_EDIT')` |
| POST /billing/invoices/suspend-overdue | `hasAuthority('BILLING_EDIT')` |
| GET /billing | `hasAuthority('BILLING_VIEW')` |
| GET /billing/invoices/{id}/print | `hasAuthority('BILLING_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')` |
| POST /billing/invoices/print | `hasAuthority('BILLING_VIEW')` |
| GET /billing/expenses | `hasAuthority('BILLING_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')` |
| POST /billing/expenses/create | `hasAuthority('BILLING_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')` |
| POST /billing/expenses/delete | `hasAuthority('BILLING_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')` |
| GET /tenant/invoices | `@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_VIEW')` |
| POST /tenant/invoices/save | `@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_EDIT')` |
| POST /tenant/invoices/{id}/pay | `@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_EDIT')` |
| POST /tenant/invoices/{id}/delete | `@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')` |
| GET /tenant/invoices/{id}/print | `@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_VIEW')` |
| GET /tenant/quotations | `@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_VIEW')` |
| POST /tenant/quotations/save | `@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_EDIT')` |
| POST /tenant/quotations/{id}/convert | `@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_EDIT')` |
| POST /tenant/quotations/{id}/delete | `@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')` |
| GET /tenant/quotations/{id}/print | `@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_VIEW')` |
| GET /tenant/invoices/builder | `@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_VIEW')` |
| POST /tenant/invoices/builder/save | `@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_EDIT')` |
| GET /tenant/invoices/excel/sample | `None (chain authentication still applies)` |
| GET /tenant/invoices/word/sample | `None (chain authentication still applies)` |
