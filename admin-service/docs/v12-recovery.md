# V12 duplicate currency column

V1 already defines `invoices.currency`. The original V12 attempted to add it
again, causing MySQL error 1060 and preventing application startup. V12 now
preserves that column and adds only the Stripe snapshot fields and unique key.
No currency values are inferred, overwritten, or backfilled.

For the reported failure, rebuild and restart the application. The existing
`FlywayRepairConfig` repairs failed migration history before calling migrate.
Do not drop the currency column or delete invoices. A V13 migration cannot fix
this failure because Flyway must first complete V12. This is a correction to
the failed, unreleased V12, not a new schema migration or breaking change.
If another environment has already completed a different V12, inspect its
schema and migration history before deploying this correction there.

Regression: `V12FlywayMigrationTest` uses a disposable MySQL 8 server bound to
127.0.0.1:13369 with data directory `admin-service/target/v12-mysql-regression`.
It refuses to run against another data directory. Set
`V12_MYSQL_INTEGRATION=true` and run `mvn -Dtest=V12FlywayMigrationTest test`
from `admin-service`. The test creates and removes its own random database.
It uses V1's actual invoice DDL, checks existing ZAR/USD amounts and currency
values, all added columns, Stripe ID uniqueness, Flyway validation, and a
second migration run with no pending work. Without the environment flag it
is skipped; H2 is not used to validate MySQL DDL.

Verified on MySQL 8.0.46: original V12 fails with error 1060; corrected V12
passes. Application database contents and full application startup were not
tested or modified during this regression run.
