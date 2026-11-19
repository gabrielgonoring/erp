-- defaultdb.company_terms definição

CREATE TABLE `company_terms` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `company_id` bigint NOT NULL,
    `title` varchar(100) NOT NULL,
    `content` text NOT NULL,
    `is_default` tinyint(1) NOT NULL DEFAULT '0',
    `status` varchar(100) NOT NULL,
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_company_terms_company` (`company_id`),
    CONSTRAINT `fk_company_terms_company` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE
);


-- defaultdb.service_orders definição

CREATE TABLE `service_orders` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `company_id` bigint NOT NULL,
    `partner_id` bigint NOT NULL,
    `technician_id` bigint unsigned DEFAULT NULL,
    `order_id` bigint DEFAULT NULL,
    `term_id` bigint DEFAULT NULL,
    `status` varchar(30) NOT NULL,
    `object_title` varchar(150) NOT NULL,
    `object_identifier` varchar(100) DEFAULT NULL,
    `entry_details` text,
    `reported_issue` text NOT NULL,
    `technical_report` text,
    `internal_notes` text,
    `services_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
    `products_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
    `discount_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
    `total_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
    `entry_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `expected_delivery_at` timestamp NULL DEFAULT NULL,
    `completed_at` timestamp NULL DEFAULT NULL,
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_service_orders_order` (`order_id`),
    KEY `idx_service_orders_company_status` (`company_id`, `status`),
    KEY `idx_service_orders_partner` (`partner_id`),
    KEY `idx_service_orders_technician` (`technician_id`),
    KEY `idx_service_orders_term` (`term_id`),
    KEY `idx_service_orders_identifier` (`company_id`, `object_identifier`),
    CONSTRAINT `fk_service_orders_company` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_service_orders_partner` FOREIGN KEY (`partner_id`) REFERENCES `partners` (`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_service_orders_technician` FOREIGN KEY (`technician_id`) REFERENCES `collaborators` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_service_orders_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_service_orders_term` FOREIGN KEY (`term_id`) REFERENCES `company_terms` (`id`) ON DELETE SET NULL
);


-- defaultdb.service_order_items definição

CREATE TABLE `service_order_items` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `service_order_id` bigint NOT NULL,
    `catalog_item_id` bigint DEFAULT NULL,
    `item_type` varchar(10) NOT NULL,
    `status` varchar(30) NOT NULL,
    `description` varchar(255) NOT NULL,
    `quantity` decimal(12,4) NOT NULL DEFAULT '1.0000',
    `unit_price` decimal(12,2) NOT NULL,
    `discount_amount` decimal(12,2) NOT NULL DEFAULT '0.00',
    `total_price` decimal(12,2) NOT NULL,
    `professional_id` bigint unsigned DEFAULT NULL,
    `stock_reserved` tinyint(1) NOT NULL DEFAULT '0',
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_service_order_items_os` (`service_order_id`),
    KEY `idx_service_order_items_catalog_item` (`catalog_item_id`),
    KEY `idx_service_order_items_type` (`item_type`),
    KEY `idx_service_order_items_status` (`status`),
    KEY `idx_service_order_items_professional` (`professional_id`),
    CONSTRAINT `fk_service_order_items_os` FOREIGN KEY (`service_order_id`) REFERENCES `service_orders` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_service_order_items_catalog_item` FOREIGN KEY (`catalog_item_id`) REFERENCES `catalog_items` (`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_service_order_items_professional` FOREIGN KEY (`professional_id`) REFERENCES `collaborators` (`id`) ON DELETE SET NULL,
    CONSTRAINT `service_order_items_chk_1` CHECK (`item_type` IN ('PHYSICAL', 'SERVICE'))
);


-- defaultdb.service_order_updates definição

CREATE TABLE `service_order_updates` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `company_id` bigint NOT NULL,
    `service_order_id` bigint NOT NULL,
    `event_type` varchar(45) NOT NULL,
    `description` text NOT NULL,
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_os_updates_os_created` (`service_order_id`, `created_at`),
    KEY `idx_os_updates_company` (`company_id`),
    CONSTRAINT `fk_os_updates_company` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_os_updates_os` FOREIGN KEY (`service_order_id`) REFERENCES `service_orders` (`id`) ON DELETE CASCADE
);
