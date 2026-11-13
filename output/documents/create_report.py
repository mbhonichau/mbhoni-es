from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from pathlib import Path

doc=Document()
s=doc.sections[0]; s.top_margin=s.bottom_margin=Inches(.65); s.left_margin=s.right_margin=Inches(.65)
normal=doc.styles['Normal']; normal.font.name='Calibri'; normal.font.size=Pt(10)
normal.paragraph_format.space_after=Pt(5)
for style in ['Title','Heading 1']:
 doc.styles[style].font.color.rgb=RGBColor.from_string('000000')
doc.styles['Title'].font.size=Pt(26)
doc.styles['Heading 1'].font.size=Pt(17)
def table(headers, rows, widths):
 t=doc.add_table(rows=1, cols=2); t.autofit=False
 for c,w in zip(t.columns,widths): c.width=Inches(w)
 for i,h in enumerate(headers): t.rows[0].cells[i].text=h
 for row in rows:
  cells=t.add_row().cells
  for i,value in enumerate(row): cells[i].text=value
 for ri,row in enumerate(t.rows):
  pr=row._tr.get_or_add_trPr(); pr.append(OxmlElement('w:cantSplit'))
  if ri==0: pr.append(OxmlElement('w:tblHeader'))
  for ci,cell in enumerate(row.cells):
   cell.width=Inches(widths[ci])
   tcpr=cell._tc.get_or_add_tcPr(); shade=OxmlElement('w:shd'); shade.set(qn('w:fill'),'17365D' if ri==0 else ('EDF2F7' if ri%2 else 'FFFFFF')); tcpr.append(shade)
   margins=OxmlElement('w:tcMar')
   for edge in ['top','left','bottom','right']:
    e=OxmlElement('w:'+edge); e.set(qn('w:w'),'85'); e.set(qn('w:type'),'dxa'); margins.append(e)
   tcpr.append(margins)
   for p in cell.paragraphs:
    p.paragraph_format.space_after=Pt(2); p.paragraph_format.space_before=Pt(2)
    for r in p.runs:
     r.font.size=Pt(10)
     if ri==0: r.bold=True; r.font.color.rgb=RGBColor(255,255,255)
 return t

doc.add_paragraph('Phase 1 API Delivery Report','Title')
doc.add_paragraph('MBHONI admin service')
doc.add_paragraph('Phase 1 delivers a tenant-scoped, read-only JSON API with API-key authentication. This report records the problems addressed, the features delivered, and the validation results, with the subsequent billing migration fix identified separately.')
doc.add_heading('Problems addressed',level=1)
table(['Problem','Phase 1 fix'],[
('API and UI authentication needed separation','Added a higher-priority security chain for /api/v1/** and the new documentation paths.'),
('Existing filters could interfere or create sessions','Added narrowly scoped filter exclusions and stateless API authentication.'),
('Key permissions did not define API scopes','Added centralized permission-to-scope mapping. Unmapped permissions grant no API access.'),
('Tenant context could leak between requests','Set context from the authenticated key and clear it in a finally block.'),
('Record IDs could allow cross-tenant requests','Used tenant-scoped lookups and added cross-tenant regression tests.'),
('GET operations could create default rows','Used read-only repository calls and DTO defaults without creating records.'),
('Authentication failures could redirect to login','Added RFC 7807 JSON errors, including HTTP 401 for missing credentials.'),
('Documentation could expose unintended routes','Restricted documentation to /api/docs and /api/openapi.json.'),
('Module incremental retrieval needed consistent timestamps','Used the latest timestamp across related records and documented timestamp limitations.'),
('New security behavior could disrupt existing endpoints','Added legacy-route parity, CSRF, session, and Stripe signature-verification regression tests.')],[2.25,4.95])
doc.add_page_break()
doc.add_heading('Features delivered',level=1)
doc.add_paragraph('The API exposes tenant data through explicit DTOs and checks API scopes before granting access.')
table(['Feature','Endpoint or behavior'],[
('Authenticated key identity','GET /api/v1/users/me'),
('Tenant-scoped user profile','GET /api/v1/users/{id}'),
('User permission check','POST /api/v1/access/check\nRead-only despite using POST.'),
('Tenant profile and settings','GET /api/v1/tenant'),
('Tenant branding','GET /api/v1/tenant/branding'),
('Tenant quota information','GET /api/v1/tenant/quotas'),
('Paginated modules and incremental filtering','GET /api/v1/tenant/modules\nSupports updatedSince.'),
('Module entitlement lookup','GET /api/v1/tenant/entitlements/{module}'),
('API documentation and OpenAPI specification','/api/docs\n/api/openapi.json'),
('Consistent response contracts','Explicit DTOs, response envelopes, pagination metadata, and structured errors.'),
('Integration guide','Scope mapping, timestamp behavior, limitations, and prerequisites for future JWT support.')],[2.25,4.95])
doc.add_page_break()
doc.add_heading('Validation and scope',level=1)
table(['Item','Result or boundary'],[
('Phase 1 test suite','222 tests; zero failures or errors; one existing opt-in test skipped.'),
('Authentication supported','API keys. JWT support is documented for future implementation and is not included in the code.'),
('Database migrations','No database migrations were introduced by Phase 1.'),
('Legacy-route compatibility','Regression coverage checks existing route behavior, CSRF requirements, and handling of X-API-Key headers.'),
('Stateless behavior','Regression coverage checks that API requests do not create sessions or emit session cookies.'),
('Stripe webhook compatibility','Regression coverage exercises the existing signature-verification path.')],[2.25,4.95])
doc.add_heading('Subsequent billing startup fix',level=1)
doc.add_paragraph('This fix was completed after Phase 1 and is separate from the API delivery.')
table(['Item','Detail'],[
('Problem','Billing migration V12 attempted to add invoices.currency, which migration V1 already creates. MySQL error 1060 blocked startup.'),
('Fix','Removed the duplicate column addition from V12, preserving the existing currency column and data.'),
('Regression validation','A MySQL regression test reproduced the original failure and passed after the fix. It checked existing currency values and amounts, added fields, Stripe invoice ID uniqueness, and Flyway rerun behavior.'),
('Breaking change or backfill','Neither is required for the reported failure.'),
('Remaining verification','Full application startup against the application database remains unverified.')],[2.25,4.95])
doc.core_properties.title='Phase 1 API Delivery Report'; doc.core_properties.subject='Problems addressed and features delivered'; doc.core_properties.author=''
out=Path('output/documents/Phase 1 API Delivery Report.docx'); doc.save(out)
print(out.resolve())
