-- ============================================================================
-- MIGRATION: MÓDULO DE PEDIDOS, COBRANÇAS E EXPEDIÇÃO (MYSQL / INNODB)
-- Compatível com a Versão 1 (BIGINT, InnoDB, utf8mb4)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. ATUALIZAÇÃO DE PARCEIROS (Suporte a Transportadoras)
-- ----------------------------------------------------------------------------
ALTER TABLE partners
    ADD COLUMN is_carrier BOOLEAN NOT NULL DEFAULT FALSE AFTER is_supplier;

-- ----------------------------------------------------------------------------
-- 2. CABEÇALHO DO PEDIDO / COMANDA
-- ----------------------------------------------------------------------------
CREATE TABLE orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        company_id BIGINT NOT NULL,
                        partner_id BIGINT NOT NULL,

    -- Vínculos Fiscais Opcionais e Independentes (NFS-e e NFC-e/NF-e)
                        nfse_invoice_id BIGINT NULL UNIQUE,
                        nfe_invoice_id  BIGINT NULL UNIQUE,

    -- Status Operacional da Venda
                        status VARCHAR(20) NOT NULL, -- OPEN, COMPLETED, CANCELED

    -- Subtotais Gerenciais e Fechamento
                        services_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                        products_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                        freight_amount  DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                        discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                        total_amount    DECIMAL(12, 2) NOT NULL DEFAULT 0.00,

                        notes TEXT,
                        completed_at TIMESTAMP NULL DEFAULT NULL,

                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                        CONSTRAINT fk_orders_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
                        CONSTRAINT fk_orders_partner FOREIGN KEY (partner_id) REFERENCES partners(id) ON DELETE RESTRICT,
                        CONSTRAINT fk_orders_nfse_invoice FOREIGN KEY (nfse_invoice_id) REFERENCES invoices(id) ON DELETE SET NULL,
                        CONSTRAINT fk_orders_nfe_invoice FOREIGN KEY (nfe_invoice_id) REFERENCES invoices(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_orders_company_status ON orders(company_id, status);
CREATE INDEX idx_orders_partner ON orders(partner_id);

-- ----------------------------------------------------------------------------
-- 3. ITENS DO PEDIDO (PRODUTOS FÍSICOS E SERVIÇOS SEM COMISSÃO)
-- ----------------------------------------------------------------------------
CREATE TABLE order_items (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             order_id BIGINT NOT NULL,
                             catalog_item_id BIGINT NULL,

                             item_type VARCHAR(10) NOT NULL CHECK (item_type IN ('PHYSICAL', 'SERVICE')),
                             description VARCHAR(255) NOT NULL,

                             quantity DECIMAL(12, 4) NOT NULL DEFAULT 1.0000,
                             unit_price DECIMAL(12, 2) NOT NULL,
                             discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
                             total_price DECIMAL(12, 2) NOT NULL, -- (quantity * unit_price) - discount_amount

    -- Profissional executor (identificação operacional, SERIAL em MySQL é BIGINT UNSIGNED)
                             professional_id BIGINT UNSIGNED NULL,

    -- Controle de saída física de cosméticos/produtos
                             stock_deducted BOOLEAN NOT NULL DEFAULT FALSE,

                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                             CONSTRAINT fk_order_items_catalog_item FOREIGN KEY (catalog_item_id) REFERENCES catalog_items(id) ON DELETE SET NULL,
                             CONSTRAINT fk_order_items_professional FOREIGN KEY (professional_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_type ON order_items(item_type);
CREATE INDEX idx_order_items_professional ON order_items(professional_id);

-- ----------------------------------------------------------------------------
-- 4. LOGÍSTICA E DESPACHO POR TRANSPORTADORA (1:1 COM ORDERS)
-- ----------------------------------------------------------------------------
CREATE TABLE order_shipping (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                order_id BIGINT NOT NULL UNIQUE,
                                carrier_id BIGINT NULL,

    -- Modalidade de Frete SEFAZ: 0=CIF, 1=FOB, 2=Terceiros, 9=Sem Frete
                                freight_mode INT NOT NULL DEFAULT 0,

    -- Dados do Veículo
                                vehicle_plate VARCHAR(8),
                                vehicle_state VARCHAR(2),
                                rntrc_code VARCHAR(20),

    -- Dados da Carga / Volumes (Grupo <vol> da NF-e)
                                package_quantity INT NOT NULL DEFAULT 1,
                                package_species VARCHAR(50) DEFAULT 'CAIXA',
                                package_brand VARCHAR(50),
                                package_numbering VARCHAR(50),
                                gross_weight_kg DECIMAL(10, 3) NOT NULL DEFAULT 0.000,
                                net_weight_kg DECIMAL(10, 3) NOT NULL DEFAULT 0.000,

                                tracking_code VARCHAR(100),
                                shipping_notes VARCHAR(255),

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                CONSTRAINT fk_order_shipping_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                                CONSTRAINT fk_order_shipping_carrier FOREIGN KEY (carrier_id) REFERENCES partners(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_order_shipping_carrier ON order_shipping(carrier_id);

-- ----------------------------------------------------------------------------
-- 5. CONTAS A RECEBER / PARCELAS / FIADO (COMPROMISSO GERENCIAL)
-- ----------------------------------------------------------------------------
CREATE TABLE order_installments (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    order_id BIGINT NOT NULL,
                                    company_id BIGINT NOT NULL,
                                    partner_id BIGINT NOT NULL,

                                    installment_number INT NOT NULL DEFAULT 1,
                                    due_date DATE NOT NULL,
                                    amount DECIMAL(12, 2) NOT NULL,

    -- PIX, CREDIT_CARD, DEBIT_CARD, CASH, BOLETO, ON_ACCOUNT (Fiado)
                                    payment_method VARCHAR(20) NOT NULL,

    -- PENDING, PARTIALLY_PAID, PAID, OVERDUE, CANCELED
                                    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

                                    bank_slip_code VARCHAR(100),
                                    bank_slip_url TEXT,
                                    notes VARCHAR(255),

                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_order_installments_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
                                    CONSTRAINT fk_order_installments_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
                                    CONSTRAINT fk_order_installments_partner FOREIGN KEY (partner_id) REFERENCES partners(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_installments_status_due ON order_installments(company_id, status, due_date);
CREATE INDEX idx_installments_order ON order_installments(order_id);
CREATE INDEX idx_installments_partner ON order_installments(partner_id);

-- ----------------------------------------------------------------------------
-- 6. BAIXAS E TRANSAÇÕES DE CAIXA (LIQUIDAÇÃO REAL DO DINHEIRO)
-- ----------------------------------------------------------------------------
CREATE TABLE payment_transactions (
                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      installment_id BIGINT NOT NULL,
                                      company_id BIGINT NOT NULL,

                                      paid_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      amount_paid DECIMAL(12, 2) NOT NULL,

                                      payment_method VARCHAR(20) NOT NULL,
                                      card_brand VARCHAR(50),
                                      nsu_authorization VARCHAR(50),
                                      notes VARCHAR(255),

                                      created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                      CONSTRAINT fk_transactions_installment FOREIGN KEY (installment_id) REFERENCES order_installments(id) ON DELETE CASCADE,
                                      CONSTRAINT fk_transactions_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_transactions_company_paid ON payment_transactions(company_id, paid_at);
CREATE INDEX idx_transactions_installment ON payment_transactions(installment_id);
