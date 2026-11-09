-- ============================================================================
-- MIGRATION: ADICIONAR PARTNER_IDENTIFIER E TORNAR CAMPOS OPCIONAIS EM PARTNERS
-- ============================================================================

ALTER TABLE partners
    ADD COLUMN partner_identifier VARCHAR(100) NULL AFTER company_id,
    MODIFY COLUMN document VARCHAR(14) NULL,
    MODIFY COLUMN email VARCHAR(255) NULL,
    MODIFY COLUMN street VARCHAR(255) NULL,
    MODIFY COLUMN number VARCHAR(20) NULL,
    MODIFY COLUMN neighborhood VARCHAR(100) NULL,
    MODIFY COLUMN ibge_city_code VARCHAR(7) NULL,
    MODIFY COLUMN city VARCHAR(100) NULL,
    MODIFY COLUMN state VARCHAR(2) NULL,
    MODIFY COLUMN zip_code VARCHAR(8) NULL;
