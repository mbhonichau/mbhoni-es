# Draft aggregate exclusion follow-up

Status: proposed only; no query changes applied. Phase 3b writes must default to disabled.

**Do not enable in production until the aggregate exclusion task is complete.**

Source inventory reviewed on 2026-11-14. Paths below are relative to `admin-service/src/main/java/com/mbhoni_creative/`; line numbers refer to the pre-3b source.

## Status lifecycle

Invoices: DRAFT, ISSUED, PAID, PARTIALLY_PAID, OVERDUE, CANCELLED. Quotations: DRAFT, SENT, ACCEPTED, REJECTED, CONVERTED_TO_INVOICE, EXPIRED. Neither enum has VOID; quotations have no CANCELLED. Both entity initializers assign DRAFT (`adminentity/TenantInvoice.java:41`, `adminentity/TenantQuotation.java:38`). Existing save methods preserve the supplied entity status; API creation must explicitly assign DRAFT.

## Financial aggregates requiring a follow-up

All four sums use tenant-scoped lists with no status predicate. Invoice source: `adminrepository/TenantInvoiceRepository.java:10`; quotation source: `adminrepository/TenantQuotationRepository.java:10`.

| Consumer / source | Included statuses today | Proposed change, not applied | Regression to add |
| --- | --- | --- | --- |
| Total Invoiced: `admincontroller/TenantInvoiceViewController.java:61` | All invoice statuses | Exclude DRAFT and CANCELLED from this financial sum; exclude VOID if subsequently introduced | Draft and cancelled invoices do not increase total; issued invoice does; tenant isolation preserved |
| Total Collected: `admincontroller/TenantInvoiceViewController.java:62` | All invoice statuses | Propose excluding DRAFT and CANCELLED; confirm handling of payments retained on cancelled documents before implementation | Draft contributes zero; paid/partially-paid documents contribute actual payments; establish cancelled-payment accounting expectation |
| Outstanding Balance: `admincontroller/TenantInvoiceViewController.java:63` | All invoice statuses | Exclude DRAFT and CANCELLED; exclude VOID if introduced | Unpaid draft/cancelled invoice contributes nothing; issued/overdue remaining balance contributes correctly |
| Total Quoted Value: `admincontroller/TenantInvoiceViewController.java:164` | All quotation statuses | Exclude DRAFT; no CANCELLED/VOID exists. Decide separately whether rejected, expired or converted quotes belong in this metric | Draft excluded and each other status tested against the agreed metric definition |

Phase 3b should include an explicitly temporary test named `apiCreatedDraftCurrentlyAppearsInUiAggregates_pendingDraftAggregateExclusion`. It should exercise an actual API-created invoice and quotation and assert their inclusion in Total Invoiced, Outstanding Balance and Total Quoted Value, while Total Collected remains unchanged for an unpaid draft. Reverse those inclusion expectations when the follow-up lands.

## Other queries and consumers

These are not revenue aggregates. Applying financial-status exclusions indiscriminately would alter data portability, API pagination or numbering.

| Query / consumer | Included statuses | Proposed treatment / regression |
| --- | --- | --- |
| Owner data export: `billing/TenantExportController.java:59`, line-item allowlist at 62, generated SELECT at 66 | All invoice/quotation statuses and their lines; tenant scope only | Approved: retain drafts and status in the full data export in Phase 3b; add tenant-isolation and draft/line completeness tests. Proposed subsequent options are documented below |
| Invoice row count: `adminrepository/TenantInvoiceRepository.java:11`, used by `adminservice/impl/TenantInvoicingServiceImpl.java:60` and 162 | All invoice statuses | Preserve current UI count-based numbering in this task. API rows advance this count even with a separate API prefix. Do not exclude statuses from the numbering count; separately replace count-based allocation if approved. Test UI numbering behavior in presence of API drafts |
| Quotation row count: `adminrepository/TenantQuotationRepository.java:11`, used by `adminservice/impl/TenantInvoicingServiceImpl.java:130` | All quotation statuses | Same numbering caveat and regression as invoices; no status exclusion proposed |
| API pagination COUNT: `api/repository/BillingReadRepository.java:42` | All statuses unless an explicit status filter is supplied; tenant/date/update filters also apply | Preserve read contract and count/list consistency; test DRAFT filtering and tenant isolation |
| API latest-line timestamp MAX: `api/repository/BillingReadRepository.java:28` | Lines of the selected document, irrespective of document status | Preserve composite updatedSince behavior; this is a timestamp aggregation, not a monetary aggregate |

Manual UI lists and document printing also expose drafts. They are not scheduled senders or revenue recognition. No additional TenantInvoice/TenantQuotation scheduled reminder, overdue notice, email sender, tax report or revenue dashboard was found in the source inspection.

The net-profit calculation in `admincontroller/BillingController.java:339-344` uses PAID subscription `Invoice` records, not `TenantInvoice`, minus `business_expenses` amounts (expense sum at 328). API-created tenant document drafts therefore do not feed that calculation. Expense POST remains deferred.

## Release gate

### Approved retained behavior

The owner export **includes the status column**: `TenantExportController.java:65` selects `*` for both document tables, which have a persisted `status` column. It also includes their line items. The missing-status high-priority condition therefore does not apply. Retain this export unchanged in Phase 3b. Proposed follow-up options, not applied: exclude DRAFT documents (and their lines), or ensure every downstream export format includes an explicit status column so consumers can distinguish drafts. The current JSON format already supplies that column.

Total Collected has **no impact from new API drafts**, because their amountPaid is zero. The sum includes the rows but adding zero leaves the result unchanged. Retain its implementation unchanged.

API drafts advance the count-based UI series, so UI numbers skip. Worked example: a tenant has two invoices, `INV-2026-001` and `INV-2026-002`. Creating `API-INV-000001` raises the row count to three; the next UI invoice becomes `INV-2026-004`, skipping 003. Quotations behave the same way with their UI prefix. Proposed follow-up options, not applied: count only non-API documents, or move UI numbering to a sequence table like V14. Counting alone retains an existing deletion risk: after numbers 001, 002, 003, deleting 002 reduces the count to two, so count + 1 can reissue 003. A sequence avoids deriving the next number from remaining rows. Obtain accountant sign-off on both series and acceptable gaps.

Add a temporary regression named `apiCreatedDraftAdvancesUiNextNumber_pendingUiNumberingFix`; exercise actual API creation followed by UI creation and assert the skipped UI number. Revise this expectation only when the numbering follow-up is implemented.

Keep `api.billing.writes.enabled=false`. When enabled, Phase 3b must warn at startup that API drafts count in UI aggregates, without blocking startup. README and `.env.example` must carry the production warning adjacent to the switch. This follow-up does not authorize changing existing UI queries, statuses, exports or numbering.
