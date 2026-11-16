# Frontend testing guide

Verified against the local `mbhoni_es` database on 15 November 2026. Account names, business ownership, subscriptions and record counts below were read from the database. No accounts, passwords, business records or settings were changed. Page paths were checked against source; the walkthroughs are instructions for you to execute, not a claim that every page was manually tested in a browser.

## 1. Open the application

With admin-service running on its configured port, open **http://localhost:8080/login**. Use usernames with the spelling shown below. Use separate browser profiles/private windows for separate accounts, or log out fully before changing users.

## 2. Existing accounts you can use

| Username | Business | Actual role | Best testing purpose | Limitation |
| --- | --- | --- | --- | --- |
| `mbuso` | Platform-wide; no tenant assigned | Global administrator; ROLE_ADMIN and ROLE_USER | Tenant list, subscription plans, user administration and platform configuration | Not a representative tenant customer; some pages require a tenant context |
| `jusaqua_admin` | JusAqua (tenant 1) | ROLE_TENANT_ADMIN | Tenant workforce, users, roles, organization, master data and API-key administration | Is not the tenant Owner; owner-only billing/export operations may be denied |
| `Grace` | JusAqua (tenant 1) | SUPERVISOR | Restricted-user experience and negative permission tests | Current permissions are exactly USER_VIEW, USER_CREATE and ROLE_VIEW; the role name does not grant employee-management access |

All three users are active. There is no existing login assigned to Mavusana Scrap Metal in the database inspected.

### Passwords

Use the passwords already set for these accounts. The current application has no universal default password, and stored hashes do not reveal the original passwords. Startup does not reset existing accounts. The optional `jusaqua_admin` bootstrap uses an explicitly supplied password only when creating that account; it cannot establish the password currently stored for this existing user.

If you can log in as `mbuso`, use **Users → the account → Reset password** to set a known testing password. This changes the selected user's password, so do it only for the intended test account. Password-reset actions require USER_EDIT; Grace does not have it. If you cannot log in to any existing account, the cleanest frontend test is fresh registration below; there is no need to guess old seeded passwords. The public forgot-password flow also depends on configured email delivery.

## 3. Existing businesses and data

| Business | Subscription | Owner | Existing sample data | Use |
| --- | --- | --- | --- | --- |
| JusAqua, ID 1 | ACTIVE; Enterprise Suite Tier | None assigned | One employee: `Jus-123`, `sne Leve`, PROBATION | Existing-business administrative and workforce testing |
| Mavusana Scrap Metal, ID 2 | None | None assigned | No employees, invoices, quotations or expenses found | Inspect incomplete tenant setup as platform admin; not a ready end-to-end customer fixture |

No organization units, tenant invoices, quotations or expenses were present at inspection. Empty tables/screens in these areas are therefore expected initially. The sole employee is PROBATION, so it need not increase a dashboard count that counts only ACTIVE employees.

## 4. Recommended first test: register a fresh business Owner

Use this route for a complete customer journey and for testing billing/export ownership without changing the existing businesses.

1. Open **http://localhost:8080/register** in a separate browser profile.
2. Enter a unique organization, for example `QA Aqua 20261115`; username `qa_aqua_owner_20261115`; and an email you control if you want to test emails.
3. Choose and record your own password: at least 12 characters, at most 72 UTF-8 bytes.
4. Submit and log in with that new username/password.
5. Verify that you see the new business rather than JusAqua's records.

Expected source behavior: signup creates a tenant, explicitly assigns the registering user as Owner, creates billing/settings/branding records and a trial subscription. The default trial is 14 days with a five-user cap; EMPLOYEE, ORG, CONTRACT, MASTER_DATA and SERVICE are enabled by the seeded trial plan. Actual plan/configuration can override seeded defaults. Keep the trial's user cap in mind when adding test users.

These example accounts/businesses are **suggestions to create**, not pre-existing seeds.

## 5. Frontend walkthroughs

Open pages through the sidebar where available. Direct paths below help distinguish a missing navigation link from a denied page. A hidden menu alone does not prove authorization; checking the direct URL is useful too.

| Test | Login | Page / route | Actions | Expected result |
| --- | --- | --- | --- | --- |
| Login and logout | Each existing account | `/login`, then dashboard | Log in, inspect identity, log out, revisit a protected page | Correct identity; logged-out session cannot access protected content |
| Platform business inventory | mbuso | `/tenants` | Locate both existing businesses | JusAqua and Mavusana Scrap Metal appear; inspect details without changing subscriptions |
| User inventory | mbuso | `/users` | Locate mbuso, jusaqua_admin and Grace | Match business assignments and roles above |
| Plan inventory | mbuso | `/subscriptions/plans`, `/subscriptions/tenants` | Inspect plans and tenant subscriptions | JusAqua has Enterprise; Mavusana has no subscription at inspection |
| Tenant workforce | jusaqua_admin | `/employees` | Find `Jus-123`; inspect its details | `sne Leve` appears with PROBATION status |
| New employee | Fresh QA Owner | `/employees` | Create employee `QA-AQUA-001`, name `Test Aqua`; use fictional details | Employee saves and remains visible after refresh |
| Organization hierarchy | Fresh QA Owner | `/organization` | Add `QA Operations`, then a child `QA Field Team`; assign test employee if the UI offers it | Hierarchy and employee assignment persist |
| Reference data | jusaqua_admin or QA Owner | `/master-data` | Inspect available categories; add a clearly marked QA value through supported controls | Tenant-scoped value persists; record any missing category/setup requirements |
| User and role controls | QA Owner | `/users`, `/roles` | Create one additional test user with a restricted role; log in separately | Restricted account has only the assigned capabilities |
| Restricted access | Grace | `/users`, `/roles`, `/employees`, `/api-keys` | View users/roles, then try employee and key pages directly | User/role viewing is permitted by current grants; employee/key access should be denied. Grace cannot edit users or reset passwords |
| Own profile | Each user | `/profile` | Inspect current-user details; change one harmless field if desired | Changes belong to that user and persist |
| API-key UI | jusaqua_admin or QA Owner | `/api-keys` | Inspect key list; optionally create a clearly labelled QA integration key | Creation follows assigned permissions; a key is not a browser-login account |
| Invoice draft | Fresh QA Owner | `/tenant/invoices` | Create a draft with customer `QA Customer`, one line quantity 2, unit price 100 | Existing calculation yields subtotal 200.00, tax 30.00, total 230.00; refresh and inspect saved status |
| Quotation draft | Fresh QA Owner | `/tenant/quotations` | Create a similar draft and inspect/print it | Correct customer, lines, dates and totals; no automatic send/convert action is required |
| Rounding case | Fresh QA Owner | Invoice/quotation creation UI | Enter two separate lines, each quantity 1 and price 0.03 | Stored line sum can be 0.06 while document total is 0.07; this is the retained calculation behavior |
| Owner export | Fresh QA Owner | `/account/export` | Download the JSON export; inspect only your QA business data | Export includes draft document status and their lines; no other tenant's records |

For billing tests, use drafts and the fictional customer above. Do not use payment/send/convert controls merely to check draft creation. UI expense creation immediately affects expense totals and net profit; it is not a draft workflow. Invoice and quotation tax is currently 15% on every line, regardless of customer tax circumstances.

## 6. Tenant-isolation walkthrough

1. Register a second QA business in a different browser profile, for example `QA Metal 20261115`, username `qa_metal_owner_20261115`.
2. In QA Aqua, create employee `QA-AQUA-001` and a draft with customer `QA Aqua Only`.
3. In QA Metal, create employee `QA-METAL-001` and a draft with customer `QA Metal Only`.
4. Compare employee and document lists in each session. Each business should see only its own records.
5. Where a detail page exposes an ID in the URL, copy the QA Aqua detail URL into the QA Metal session. It must not reveal the other business's data; record the actual denied/not-found response.
6. Export each business independently and confirm that the other business's QA records are absent.

Do not use mbuso for this isolation test: global administration is deliberately different from an ordinary tenant session.

## 7. Known setup gaps versus defects

| Observation | Interpretation |
| --- | --- |
| jusaqua_admin cannot perform an owner-only action | JusAqua has no owner_user_id; tenant administrator and Owner are different checks. Use a fresh registered QA Owner for the full journey |
| Mavusana cannot access paid/subscription-gated features | No subscription or login is assigned; this business is incomplete test setup |
| Grace cannot open Employees | Expected from the actual SUPERVISOR grants; do not infer permissions from the role label |
| Organization or billing lists start empty | Expected: no such records existed at inspection |
| A browser user can create a UI invoice while API writes are disabled | Expected: api.billing.writes.enabled controls only the two v1 POSTs; UI behavior remains unchanged |
| No new Phase 3b button appears | Expected: Phase 3b added integration endpoints, not a new UI workflow |
| UI draft numbers use INV-/QT- rather than API-INV-/API-QUO- | Expected: API number series applies only to API-created drafts |
| Draft values appear in UI financial aggregates | Known retained behavior, tracked in docs/api/draft-aggregate-followup.md |

Keep API writes disabled for this frontend pass. If you later want to test an actual API-created draft appearing in the UI, that requires a separate API request with the switch enabled and an appropriately scoped key; ordinary UI creation does not test that path.

## 8. Record each test result

Use this format: **username → business → page URL → action → expected result → actual result → screenshot/error text**. Include the test record identifier. Start with login, role restrictions and tenant isolation, then organization/employees, then invoice/quotation drafts and export. This order makes permission/setup problems easier to distinguish from feature defects.
