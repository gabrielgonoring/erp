-- ============================================================================
-- MIGRATION: REMOVE VÍNCULOS DIRETOS EM ORDERS E CRIA TABELA ORDER_INVOICES
-- ============================================================================

-- 1. Remove colunas e chaves estrangeiras legadas da tabela orders
ALTER TABLE orders DROP FOREIGN KEY fk_orders_nfse_invoice;
ALTER TABLE orders DROP FOREIGN KEY fk_orders_nfe_invoice;
ALTER TABLE orders DROP COLUMN nfse_invoice_id;
ALTER TABLE orders DROP COLUMN nfe_invoice_id;

-- 2. Cria tabela order_invoices para ligação entre orders e invoices
CREATE TABLE order_invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    invoice_id BIGINT NOT NULL,
    invoice_purpose VARCHAR(45) NOT NULL DEFAULT 'PRODUCT',
    notes VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Chaves Estrangeiras com integridade referencial
    CONSTRAINT fk_order_invoices_company FOREIGN KEY (company_id) 
        REFERENCES companies(id) ON DELETE CASCADE,
        
    CONSTRAINT fk_order_invoices_order FOREIGN KEY (order_id) 
        REFERENCES orders(id) ON DELETE CASCADE,
        
    -- RESTRICT protege a nota: impede apagar a nota se ela estiver vinculada a um pedido
    CONSTRAINT fk_order_invoices_invoice FOREIGN KEY (invoice_id) 
        REFERENCES invoices(id) ON DELETE RESTRICT,
        
    -- Garante que a mesma nota não seja vinculada em duplicidade com o mesmo propósito
    CONSTRAINT uq_order_invoice_purpose UNIQUE (order_id, invoice_id, invoice_purpose)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Índices de consulta rápida
CREATE INDEX idx_order_invoices_order ON order_invoices(company_id, order_id);
CREATE INDEX idx_order_invoices_invoice ON order_invoices(company_id, invoice_id);
CREATE INDEX idx_order_invoices_purpose ON order_invoices(invoice_purpose);
