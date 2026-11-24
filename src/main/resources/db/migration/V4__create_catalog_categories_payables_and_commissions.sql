-- ============================================================================
-- BLOCO 1: AJUSTES NO CATÁLOGO E PEDIDOS (ERP)
-- ============================================================================

CREATE TABLE `catalog_categories` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint NOT NULL,
  `name` varchar(100) NOT NULL,
  `status` varchar(45) NOT NULL DEFAULT 'ACTIVE',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_catalog_categories_company` (`company_id`),
  CONSTRAINT `fk_catalog_categories_company` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE
);

ALTER TABLE `catalog_items`
  ADD COLUMN `category_id` bigint DEFAULT NULL AFTER `company_id`,
  ADD KEY `idx_catalog_items_category` (`category_id`),
  ADD CONSTRAINT `fk_catalog_items_category` FOREIGN KEY (`category_id`) REFERENCES `catalog_categories` (`id`) ON DELETE SET NULL;



-- ============================================================================
-- BLOCO 2: MÓDULO DE CONTAS A PAGAR
-- ============================================================================

CREATE TABLE `payables` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint NOT NULL,
  `partner_id` bigint DEFAULT NULL,
  `collaborator_id` bigint unsigned DEFAULT NULL,
  `origin_type` varchar(45) NOT NULL,
  `origin_id` bigint DEFAULT NULL,
  `description` varchar(255) NOT NULL,
  `installment_number` int NOT NULL DEFAULT '1',
  `due_date` date NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `pending_amount` decimal(12,2) NOT NULL,
  `payment_method` varchar(20) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `notes` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_payables_company_status_due` (`company_id`, `status`, `due_date`),
  KEY `idx_payables_collaborator` (`company_id`, `collaborator_id`, `status`),
  KEY `idx_payables_origin` (`company_id`, `origin_type`, `origin_id`),
  CONSTRAINT `fk_payables_company` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_payables_partner` FOREIGN KEY (`partner_id`) REFERENCES `partners` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_payables_collaborator` FOREIGN KEY (`collaborator_id`) REFERENCES `collaborators` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `chk_payables_beneficiary` CHECK (`partner_id` IS NOT NULL OR `collaborator_id` IS NOT NULL)
);

CREATE TABLE `payable_transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `payable_id` bigint NOT NULL,
  `company_id` bigint NOT NULL,
  `transaction_type` varchar(15) NOT NULL,          -- PAYMENT, REVERSAL
  `paid_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `amount_paid` decimal(12,2) NOT NULL,
  `payment_method` varchar(20) NOT NULL,
  `receipt_url` text DEFAULT NULL,
  `notes` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_payable_tx_company_paid` (`company_id`, `paid_at`),
  KEY `idx_payable_tx_payable` (`payable_id`),
  CONSTRAINT `fk_payable_tx_company` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_payable_tx_payable` FOREIGN KEY (`payable_id`) REFERENCES `payables` (`id`) ON DELETE CASCADE
);


-- ============================================================================
-- BLOCO 3: SISTEMA DE COMISSÕES (DESACOPLADO)
-- ============================================================================

CREATE TABLE `commission_rules` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint NOT NULL,
  `collaborator_id` bigint unsigned DEFAULT NULL,   -- NULL = vale para qualquer colaborador
  `category_id` bigint DEFAULT NULL,                -- NULL = não se aplica a categoria
  `catalog_item_id` bigint DEFAULT NULL,            -- NULL = não se aplica a item específico
  `commission_percentage` decimal(5,2) NOT NULL,
  `valid_from` date DEFAULT NULL,
  `valid_until` date DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE, INACTIVE
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_rules_lookup` (`company_id`, `status`, `collaborator_id`, `catalog_item_id`, `category_id`),
  CONSTRAINT `chk_commission_rules_target`
    CHECK (NOT (`category_id` IS NOT NULL AND `catalog_item_id` IS NOT NULL))
);

CREATE TABLE `commission_statements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint NOT NULL,
  `collaborator_id` bigint unsigned NOT NULL,
  `collaborator_name` varchar(255) NOT NULL,
  `payable_id` bigint DEFAULT NULL,        -- ID gerado em payables ao aprovar o lote
  `period_start` date NOT NULL,
  `period_end` date NOT NULL,
  `due_date` date NOT NULL,
  `total_sales_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `total_commission_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
  `status` varchar(20) NOT NULL,
  `paid_at` timestamp NULL DEFAULT NULL,
  `notes` varchar(255) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_statements_payable` (`company_id`, `payable_id`),
  KEY `idx_statements_payable` (`payable_id`),
  KEY `idx_statements_company_collab` (`company_id`, `collaborator_id`, `status`),
  KEY `idx_statements_period` (`company_id`, `period_start`, `period_end`),
  CONSTRAINT `fk_commission_statements_payable` FOREIGN KEY (`payable_id`) REFERENCES `payables` (`id`) ON DELETE SET NULL
);

CREATE TABLE `commission_entries` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `company_id` bigint NOT NULL,
  `collaborator_id` bigint unsigned NOT NULL,
  `collaborator_name` varchar(255) NOT NULL,
  `statement_id` bigint DEFAULT NULL,               -- Preenchido quando o item entra em um lote
  `source_type` varchar(20) NOT NULL,               -- ORDER_ITEM, SERVICE_ORDER_ITEM
  `source_document_id` bigint NOT NULL,             -- orders.id ou service_orders.id
  `source_item_id` bigint NOT NULL,                 -- order_items.id ou service_order_items.id
  `catalog_item_id` bigint DEFAULT NULL,
  `category_id` bigint DEFAULT NULL,
  `item_description` varchar(255) NOT NULL,
  `base_amount` decimal(12,2) NOT NULL,
  `applied_percentage` decimal(5,2) NOT NULL,
  `commission_amount` decimal(12,2) NOT NULL,
  `rule_id` bigint DEFAULT NULL,
  `rule_source` varchar(30) NOT NULL,
  `status` varchar(20) NOT NULL,
  `occurred_at` timestamp NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_commission_entry_source` (`company_id`, `source_type`, `source_item_id`),
  KEY `idx_entries_company_collab_status` (`company_id`, `collaborator_id`, `status`),
  KEY `idx_entries_statement` (`statement_id`),
  CONSTRAINT `fk_entries_statement` FOREIGN KEY (`statement_id`) REFERENCES `commission_statements` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_entries_rule` FOREIGN KEY (`rule_id`) REFERENCES `commission_rules` (`id`) ON DELETE SET NULL
);
