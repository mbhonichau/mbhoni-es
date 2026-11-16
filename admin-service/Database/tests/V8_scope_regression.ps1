# Run only against a disposable mysqld with datadir admin-service/target/v8-mysql-regression.
param([string]$MySql = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe', [int]$Port = 13368)
$ErrorActionPreference = 'Stop'
$connectionArgs = @('--no-defaults', '--protocol=TCP', '--host=127.0.0.1', "--port=$Port", '--user=root', '--batch', '--skip-column-names')
$actualDirectory = (& $MySql @connectionArgs -e 'SELECT @@datadir').Replace('\\', '\').TrimEnd('\', '/')
$expectedDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../target/v8-mysql-regression')).TrimEnd('\', '/')
if ($LASTEXITCODE -ne 0 -or $actualDirectory -ne $expectedDirectory) { throw 'Refusing to test outside the disposable MySQL data directory' }
$schema = 'v8_regression_' + [Guid]::NewGuid().ToString('N')
function Invoke-TestSql([string]$Sql, [string]$ExpectedError = '') {
    $result = $Sql | & $MySql @connectionArgs 2>&1
    $code = $LASTEXITCODE
    if ($ExpectedError) {
        if ($code -eq 0 -or "$result" -notmatch $ExpectedError) { throw "Expected $ExpectedError, got: $result" }
    } elseif ($code -ne 0) { throw "SQL failed: $result" }
    else { return "$result".Trim() }
}
Invoke-TestSql @"
CREATE DATABASE $schema;
USE $schema;
CREATE TABLE tenants(id BIGINT PRIMARY KEY) ENGINE=InnoDB;
CREATE TABLE field_definitions(id BIGINT PRIMARY KEY, tenant_id BIGINT NULL, section VARCHAR(40) NOT NULL, field_key VARCHAR(80) NOT NULL,
 FOREIGN KEY(tenant_id) REFERENCES tenants(id) ON DELETE CASCADE) ENGINE=InnoDB;
CREATE TABLE lookup_categories(id BIGINT PRIMARY KEY, tenant_id BIGINT NULL, code VARCHAR(80) NOT NULL UNIQUE,
 FOREIGN KEY(tenant_id) REFERENCES tenants(id) ON DELETE CASCADE) ENGINE=InnoDB;
INSERT INTO tenants VALUES(1),(2);
INSERT INTO field_definitions VALUES(1,NULL,'CORE','name'),(2,1,'CORE','name'),(3,2,'CORE','name');
INSERT INTO lookup_categories VALUES(1,NULL,'COUNTRY');
"@ | Out-Null
Invoke-TestSql "USE $schema; ALTER TABLE field_definitions ADD tenant_scope BIGINT GENERATED ALWAYS AS (COALESCE(tenant_id,0)) STORED;" '1215|Cannot add foreign key' | Out-Null
$migration = Get-Content (Join-Path $PSScriptRoot '../../src/main/resources/db/migration/V8__scope_shared_defaults.sql') -Raw
Invoke-TestSql "USE $schema; $migration" | Out-Null
# Repeat a completed migration, then resume columns whose indexes were never added.
Invoke-TestSql "USE $schema; $migration" | Out-Null
Invoke-TestSql "USE $schema; ALTER TABLE field_definitions DROP INDEX uk_field_scope_key; ALTER TABLE lookup_categories DROP INDEX uk_lookup_scope_code;" | Out-Null
Invoke-TestSql "USE $schema; $migration" | Out-Null
Invoke-TestSql "USE $schema; INSERT INTO lookup_categories VALUES(2,1,'COUNTRY',DEFAULT),(3,2,'COUNTRY',DEFAULT);" | Out-Null
Invoke-TestSql "USE $schema; INSERT INTO field_definitions VALUES(4,NULL,'CORE','name',DEFAULT);" '1062' | Out-Null
Invoke-TestSql "USE $schema; INSERT INTO field_definitions VALUES(4,1,'CORE','name',DEFAULT);" '1062' | Out-Null
Invoke-TestSql "USE $schema; INSERT INTO lookup_categories VALUES(4,NULL,'COUNTRY',DEFAULT);" '1062' | Out-Null
Invoke-TestSql "USE $schema; INSERT INTO lookup_categories VALUES(4,1,'COUNTRY',DEFAULT);" '1062' | Out-Null
Invoke-TestSql "USE $schema; DELETE FROM tenants WHERE id=1;" | Out-Null
$counts = Invoke-TestSql "USE $schema; SELECT CONCAT((SELECT COUNT(*) FROM field_definitions),',',(SELECT COUNT(*) FROM lookup_categories),',',(SELECT COUNT(*) FROM field_definitions WHERE tenant_id=1));"
if ($counts -ne '2,2,0') { throw "Unexpected rows after cascade: $counts" }
# Reproduce the reported first-table-already-complete, second-table-not-started case.
Invoke-TestSql "USE $schema; ALTER TABLE lookup_categories DROP INDEX uk_lookup_scope_code, DROP COLUMN tenant_scope;" | Out-Null
Invoke-TestSql "USE $schema; $migration" | Out-Null
# Refuse an unrelated ordinary column instead of masking a broken isolation constraint.
Invoke-TestSql "USE $schema; ALTER TABLE field_definitions DROP INDEX uk_field_scope_key, DROP COLUMN tenant_scope; ALTER TABLE field_definitions ADD tenant_scope BIGINT;" | Out-Null
Invoke-TestSql "USE $schema; $migration" 'incompatible field_definitions.tenant_scope' | Out-Null
Write-Output "PASS: fresh migration, repeat run, missing indexes, partial table completion, incompatible-column rejection, uniqueness and cascades. Schema: $schema"
