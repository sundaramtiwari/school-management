CREATE UNIQUE INDEX IF NOT EXISTS uk_pricing_plans_name_ci ON pricing_plans (LOWER(name));

CREATE UNIQUE INDEX IF NOT EXISTS uk_subscription_live_per_school
    ON subscriptions (school_id)
    WHERE status IN ('TRIAL', 'ACTIVE', 'PAST_DUE');

DROP INDEX IF EXISTS idx_subscription_payment_sub_payment_date;
CREATE INDEX IF NOT EXISTS idx_subscription_payment_sub_payment_date
    ON subscription_payments (subscription_id, payment_date DESC);
