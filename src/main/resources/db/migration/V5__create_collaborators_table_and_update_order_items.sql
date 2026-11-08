-- ============================================================================
-- 5. TABELA DE COLABORADORES E ATUALIZAÇÃO DE ORDER_ITEMS
-- ============================================================================

CREATE TABLE collaborators (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    user_id BIGINT UNSIGNED NULL,
    name VARCHAR(255) NOT NULL,
    cpf VARCHAR(14) NULL,
    email VARCHAR(255) NULL,
    phone VARCHAR(20) NULL,
    status VARCHAR(45) NOT NULL DEFAULT 'ACTIVE',

    -- Endereço
    street VARCHAR(255) NULL,
    number VARCHAR(20) NULL,
    complement VARCHAR(100) NULL,
    neighborhood VARCHAR(100) NULL,
    city VARCHAR(100) NULL,
    state VARCHAR(2) NULL,
    zip_code VARCHAR(8) NULL,
    ibge_city_code VARCHAR(7) NULL,

    -- Campos trabalhistas / exigidos por lei (CLT / eSocial)
    rg VARCHAR(20) NULL,
    birth_date DATE NULL,
    admission_date DATE NULL,
    resignation_date DATE NULL,
    job_title VARCHAR(100) NULL,
    pis_pasep VARCHAR(20) NULL,

    -- Auditoria
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_collaborators_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_collaborators_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_collaborators_company ON collaborators(company_id);
CREATE INDEX idx_collaborators_user ON collaborators(user_id);
CREATE INDEX idx_collaborators_cpf ON collaborators(cpf);
CREATE INDEX idx_collaborators_name ON collaborators(name);

-- Remover chave estrangeira de order_items para users
ALTER TABLE order_items DROP FOREIGN KEY fk_order_items_professional;

-- Limpar vínculos legados de order_items para evitar violação de FK
UPDATE order_items SET professional_id = NULL WHERE professional_id IS NOT NULL;

-- Vincular order_items à tabela collaborators com a mesma cardinalidade
ALTER TABLE order_items
    ADD CONSTRAINT fk_order_items_professional
    FOREIGN KEY (professional_id) REFERENCES collaborators(id) ON DELETE SET NULL;
