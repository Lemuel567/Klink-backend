-- V4: payments integrity + analytics performance
--
-- 1) `online` flag: the reliable manual-vs-online discriminator for the
--    collections summary. momoReference could not serve that role — a
--    Financial Secretary may attach a MoMo receipt number to a HAND-recorded
--    payment, which then wrongly counted as "through the app".
--    Backfill matches the ledger against actual Paystack transaction
--    references, which is exact: only materialised online payments carry one.
--
-- 2) Partial unique indexes closing two check-then-act races that silently
--    double-count money (no constraint backed the service-layer guards):
--      - one manual lump-sum OFFERING per church per service date
--      - one WELFARE row per member per month
--    Exact duplicates (same amount — i.e. double-submits) are removed first,
--    keeping one row. If NON-exact duplicates exist, index creation fails
--    loudly and must be resolved by hand — deliberately, since guessing which
--    financial record to drop is not a migration's call.
--
-- 3) Composite indexes for the analytics dashboard's range scans.

ALTER TABLE payments ADD COLUMN IF NOT EXISTS online boolean NOT NULL DEFAULT false;

UPDATE payments p
SET online = true
FROM paystack_transactions t
WHERE p.momo_reference = t.paystack_reference;

-- Remove exact double-submit duplicates before adding the unique indexes
DELETE FROM payments a
USING payments b
WHERE a.payment_type = 'OFFERING'
  AND b.payment_type = 'OFFERING'
  AND a.member_id IS NULL AND b.member_id IS NULL
  AND a.group_id IS NULL AND b.group_id IS NULL
  AND a.church_id = b.church_id
  AND a.payment_date = b.payment_date
  AND a.amount = b.amount
  AND a.id > b.id;

DELETE FROM payments a
USING payments b
WHERE a.payment_type = 'WELFARE'
  AND b.payment_type = 'WELFARE'
  AND a.group_id IS NULL AND b.group_id IS NULL
  AND a.church_id = b.church_id
  AND a.member_id = b.member_id
  AND a.payment_month = b.payment_month
  AND a.amount = b.amount
  AND a.id > b.id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_daily_manual_offering
    ON payments (church_id, payment_date)
    WHERE payment_type = 'OFFERING' AND member_id IS NULL AND group_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_member_welfare_month
    ON payments (church_id, member_id, payment_month)
    WHERE payment_type = 'WELFARE' AND group_id IS NULL;

-- Analytics / collections range-scan support (names match the entity @Index
-- declarations so ddl-auto:update never creates duplicates)
CREATE INDEX IF NOT EXISTS idx_payments_church_date
    ON payments (church_id, payment_date);

CREATE INDEX IF NOT EXISTS idx_attendance_church_status_date
    ON attendance (church_id, status, service_date);
