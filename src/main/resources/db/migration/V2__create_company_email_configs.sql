CREATE TABLE `company_email_configs` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `company_id` bigint NOT NULL,
    `notification_type` varchar(60) NOT NULL,
    `from_email` varchar(255) NOT NULL,
    `from_name` varchar(255) NOT NULL,
    `reply_to` varchar(255) DEFAULT NULL,
    `status` varchar(45) NOT NULL,
    `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_company_email_cfg_type` (`company_id`, `notification_type`),
    KEY `idx_company_email_cfg_lookup` (`company_id`, `status`, `notification_type`),
    CONSTRAINT `fk_company_email_cfg_company` FOREIGN KEY (`company_id`) REFERENCES `companies` (`id`) ON DELETE CASCADE
);
