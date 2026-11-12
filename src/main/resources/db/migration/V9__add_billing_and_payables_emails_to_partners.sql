-- ============================================================================
-- MIGRATION: ADICIONAR BILLING_EMAIL E PAYABLES_EMAIL EM PARTNERS
-- ============================================================================

ALTER TABLE partners
    ADD COLUMN billing_email VARCHAR(255) NULL AFTER email,
    ADD COLUMN payables_email VARCHAR(255) NULL AFTER billing_email;
