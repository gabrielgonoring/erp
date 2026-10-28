-- ============================================================================
-- 1. CADASTROS PRINCIPAIS (EMPRESAS E PARCEIROS DE NEGÓCIO)
-- ============================================================================

-- TABELA DE EMPRESAS EMISSORAS
CREATE TABLE companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    corporate_name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    cnpj VARCHAR(14) NOT NULL UNIQUE,
    state_registration VARCHAR(20),
    municipal_registration VARCHAR(20),
    tax_regime INT NOT NULL DEFAULT 1,
    status VARCHAR(45) NOT NULL,
    
    street VARCHAR(255) NOT NULL,
    number VARCHAR(20) NOT NULL,
    complement VARCHAR(100),
    neighborhood VARCHAR(100) NOT NULL,
    ibge_city_code VARCHAR(7) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    zip_code VARCHAR(8) NOT NULL,
    country_code VARCHAR(4) DEFAULT '1058',
    country_name VARCHAR(50) DEFAULT 'Brasil',
    
    certificate_expiration_date TIMESTAMP NULL DEFAULT NULL,
    notaas_company_id VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- TABELA UNIFICADA: PARCEIROS (Clientes e Fornecedores)
CREATE TABLE partners (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    status VARCHAR(45) NOT NULL,

    is_customer BOOLEAN DEFAULT FALSE,
    is_supplier BOOLEAN DEFAULT FALSE,
    
    person_type VARCHAR(2) NOT NULL CHECK (person_type IN ('PF', 'PJ')),
    document VARCHAR(14) NOT NULL,
    name VARCHAR(255) NOT NULL,
    
    indicator_ie INT DEFAULT 9,
    state_registration VARCHAR(20),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    
    -- Endereço
    street VARCHAR(255) NOT NULL,
    number VARCHAR(20) NOT NULL,
    complement VARCHAR(100),
    neighborhood VARCHAR(100) NOT NULL,
    ibge_city_code VARCHAR(7) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(2) NOT NULL,
    zip_code VARCHAR(8) NOT NULL,
    country_code VARCHAR(4) DEFAULT '1058',
    country_name VARCHAR(50) DEFAULT 'Brasil',
    
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_partners_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================================
-- 2. CATÁLOGO DE ITENS DA EMPRESA
-- ============================================================================

-- TABELA MÃE DO CATÁLOGO
CREATE TABLE catalog_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('PHYSICAL', 'SERVICE')),
    internal_code VARCHAR(50),
    description VARCHAR(255) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    unit_of_measure VARCHAR(10) NOT NULL,
    status VARCHAR(45) NOT NULL,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_catalog_items_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- TABELA FILHA DO CATÁLOGO: PRODUTOS FÍSICOS
CREATE TABLE catalog_physical_items (
    catalog_item_id BIGINT PRIMARY KEY,
    ncm VARCHAR(8) NOT NULL,
    default_cfop VARCHAR(4) NOT NULL,
    default_csosn VARCHAR(4) NOT NULL DEFAULT '102',
    
    gtin_ean VARCHAR(14) DEFAULT 'SEM GTIN',
    cest VARCHAR(7),
    origin INT DEFAULT 0,
    
    default_icms_credit_rate DECIMAL(5, 2) DEFAULT 0.00,
    icms_rate DECIMAL(5, 2) DEFAULT NULL,
    pis_rate DECIMAL(5, 2) DEFAULT NULL,
    cofins_rate DECIMAL(5, 2) DEFAULT NULL,
    
    CONSTRAINT fk_catalog_physical_items_catalog_item FOREIGN KEY (catalog_item_id) REFERENCES catalog_items(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- TABELA FILHA DO CATÁLOGO: SERVIÇOS
CREATE TABLE catalog_service_items (
    catalog_item_id BIGINT PRIMARY KEY,
    lc116_service_code VARCHAR(10) NOT NULL,
    city_tax_code VARCHAR(20),
    iss_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    iss_withheld BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT fk_catalog_service_items_catalog_item FOREIGN KEY (catalog_item_id) REFERENCES catalog_items(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- ============================================================================
-- 3. CABEÇALHO E ITENS DA NOTA FISCAL
-- ============================================================================

CREATE TABLE invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    partner_id BIGINT NOT NULL,
    
    external_reference VARCHAR(100),
    operation_type INT NOT NULL DEFAULT 1,
    document_type VARCHAR(10) NOT NULL CHECK (document_type IN ('NFE', 'NFCE', 'NFSE')),
    environment VARCHAR(10) NOT NULL CHECK (environment IN ('HOMOLOGACAO', 'PRODUCAO')),

    status VARCHAR(45) NOT NULL,
    
    invoice_number INT,
    series INT,
    access_key VARCHAR(44),
    authorization_protocol VARCHAR(100),
    
    payment_method VARCHAR(20),
    due_date DATE,
    freight_mode INT DEFAULT 9,
    freight_amount DECIMAL(12, 2) DEFAULT 0.00,
    additional_expenses DECIMAL(12, 2) DEFAULT 0.00,
    
    products_amount DECIMAL(12, 2) DEFAULT 0.00,
    discount_amount DECIMAL(12, 2) DEFAULT 0.00,
    total_amount DECIMAL(12, 2) NOT NULL,
    
    additional_information TEXT,
    
    canceled_at TIMESTAMP NULL DEFAULT NULL,
    cancellation_reason TEXT,
    cancellation_protocol VARCHAR(100),
    correction_letter TEXT,
    
    notaas_id VARCHAR(100),
    error_message TEXT,
    pdf_url TEXT,
    xml_url TEXT,
    raw_payload_sent JSON,
    raw_response_received JSON,
    
    issued_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_invoices_company FOREIGN KEY (company_id) REFERENCES companies(id),
    CONSTRAINT fk_invoices_partner FOREIGN KEY (partner_id) REFERENCES partners(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- TABELA MÃE DOS ITENS DA NOTA
CREATE TABLE invoice_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    catalog_item_id BIGINT NULL,
    
    description VARCHAR(255) NOT NULL,
    quantity DECIMAL(12, 4) NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    discount_amount DECIMAL(12, 2) DEFAULT 0.00,
    total_price DECIMAL(12, 2) NOT NULL,
    unit_of_measure VARCHAR(10) NOT NULL,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_invoice_items_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    CONSTRAINT fk_invoice_items_catalog_item FOREIGN KEY (catalog_item_id) REFERENCES catalog_items(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- TABELA FILHA DOS ITENS DA NOTA: PRODUTOS EMITIDOS
CREATE TABLE physical_invoice_items (
    invoice_item_id BIGINT PRIMARY KEY,
    
    ncm VARCHAR(8) NOT NULL,
    cfop VARCHAR(4) NOT NULL,
    csosn VARCHAR(4) NOT NULL,
    cest VARCHAR(7),
    gtin_ean VARCHAR(14) DEFAULT 'SEM GTIN',
    origin INT DEFAULT 0,
    
    icms_credit_rate DECIMAL(5, 2) DEFAULT 0.00,
    icms_credit_amount DECIMAL(12, 2) DEFAULT 0.00,
    
    pis_cst VARCHAR(2) DEFAULT '49',
    cofins_cst VARCHAR(2) DEFAULT '49',
    
    CONSTRAINT fk_physical_invoice_items_item FOREIGN KEY (invoice_item_id) REFERENCES invoice_items(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- TABELA FILHA DOS ITENS DA NOTA: SERVIÇOS EMITIDOS
CREATE TABLE service_invoice_items (
    invoice_item_id BIGINT PRIMARY KEY,
    
    lc116_service_code VARCHAR(10) NOT NULL,
    city_tax_code VARCHAR(20),
    service_execution_city_ibge VARCHAR(7),
    
    iss_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    iss_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    iss_withheld BOOLEAN DEFAULT FALSE,
    deductions_amount DECIMAL(12, 2) DEFAULT 0.00,
    
    CONSTRAINT fk_service_invoice_items_item FOREIGN KEY (invoice_item_id) REFERENCES invoice_items(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



-- TABELA DE USUÁRIOS (Acesso ao Sistema)
CREATE TABLE users (
   id SERIAL PRIMARY KEY,

    -- Chave Estrangeira: Garante que o usuário só enxerga os dados da sua empresa
   company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,

    -- Credenciais e Dados
   name VARCHAR(255) NOT NULL,
   email VARCHAR(255) UNIQUE NOT NULL,
   password_hash VARCHAR(255) NOT NULL, -- Senha deve ser salva criptografada (ex: BCrypt)

    -- Status da Conta
   status VARCHAR(45) NOT NULL,

    -- Controle de Sessão e Segurança
   last_login TIMESTAMP,
   reset_password_token VARCHAR(255),
   reset_password_expires TIMESTAMP,

    -- Auditoria
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índice para acelerar a busca no momento do login (onde usamos o email)
CREATE INDEX idx_users_email ON users(email);