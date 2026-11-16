-- Read-only inventory. Run AFTER V6. No state, owner, paid status or timestamps are inferred.
SELECT t.id, t.name, t.active, t.status AS tenant_status, s.id AS subscription_id,
       s.status AS subscription_status, s.trial_ends_at, s.grace_ends_at, s.retention_ends_at,
       s.stripe_subscription_id
FROM tenants t LEFT JOIN tenant_subscriptions s ON s.tenant_id = t.id
WHERE s.id IS NULL
   OR t.status <> s.status
   OR t.active <> (s.status IN ('TRIAL', 'ACTIVE', 'PAST_DUE'))
   OR (s.status = 'TRIAL' AND s.trial_ends_at IS NULL)
   OR (s.status IN ('PAST_DUE', 'SUSPENDED') AND s.grace_ends_at IS NULL)
   OR (s.status = 'CANCELLED' AND s.retention_ends_at IS NULL);
