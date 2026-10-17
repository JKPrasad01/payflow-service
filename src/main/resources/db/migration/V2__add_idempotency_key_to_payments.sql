ALTER TABLE payments
ADD COLUMN idempotency_key VARCHAR(100);

-- Give existing payment records unique placeholder keys.
UPDATE payments
SET idempotency_key = 'legacy-' || id
WHERE idempotency_key IS NULL;

-- Now every row has a key, so enforce the constraint.
ALTER TABLE payments
ALTER COLUMN idempotency_key SET NOT NULL;

CREATE UNIQUE INDEX uk_payments_idempotency_key
ON payments (idempotency_key);