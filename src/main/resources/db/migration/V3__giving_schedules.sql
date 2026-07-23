-- Recurring giving schedules: a member's standing monthly giving intention.
CREATE TABLE IF NOT EXISTS giving_schedules (
    id             UUID PRIMARY KEY,
    church_id      UUID NOT NULL REFERENCES churches(id),
    member_id      UUID NOT NULL REFERENCES members(id),
    payment_type   VARCHAR(40) NOT NULL,
    amount         NUMERIC(19, 2) NOT NULL,
    day_of_month   INTEGER NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    last_run_month VARCHAR(7),
    created_at     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_giving_sched_active_day
    ON giving_schedules (active, day_of_month);
