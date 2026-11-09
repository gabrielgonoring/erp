-- ============================================================================
-- 6. ADICIONAR PORCENTAGEM PADRÃO DE COMISSÃO NA TABELA DE COLABORADORES
-- ============================================================================

ALTER TABLE collaborators
    ADD COLUMN default_commission_percentage DECIMAL(5, 2) NOT NULL DEFAULT 0.00;
