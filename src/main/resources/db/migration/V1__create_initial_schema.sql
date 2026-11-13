-- defaultdb.companies definição

CREATE TABLE "companies" (
    "id" bigint NOT NULL AUTO_INCREMENT,
    "corporate_name" varchar(255) NOT NULL,
    "trade_name" varchar(255) DEFAULT NULL,
    "cnpj" varchar(14) NOT NULL,
    "state_registration" varchar(20) DEFAULT NULL,
    "municipal_registration" varchar(20) DEFAULT NULL,
    "tax_regime" int NOT NULL DEFAULT '1',
    "status" varchar(45) NOT NULL,
    "street" varchar(255) NOT NULL,
    "number" varchar(20) NOT NULL,
    "complement" varchar(100) DEFAULT NULL,
    "neighborhood" varchar(100) NOT NULL,
    "ibge_city_code" varchar(7) NOT NULL,
    "city" varchar(100) NOT NULL,
    "state" varchar(2) NOT NULL,
    "zip_code" varchar(8) NOT NULL,
    "country_code" varchar(4) DEFAULT '1058',
    "country_name" varchar(50) DEFAULT 'Brasil',
    "certificate_expiration_date" timestamp NULL DEFAULT NULL,
    "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    UNIQUE KEY "cnpj" ("cnpj")
);


-- defaultdb.users definição

CREATE TABLE "users" (
    "id" bigint unsigned NOT NULL AUTO_INCREMENT,
    "company_id" bigint DEFAULT NULL,
    "name" varchar(255) NOT NULL,
    "email" varchar(255) NOT NULL,
    "password_hash" varchar(255) NOT NULL,
    "status" varchar(45) NOT NULL,
    "last_login" timestamp NULL DEFAULT NULL,
    "reset_password_token" varchar(255) DEFAULT NULL,
    "reset_password_expires" timestamp NULL DEFAULT NULL,
    "created_at" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
    "temporary_password" tinyint(1) NOT NULL DEFAULT '0',
    PRIMARY KEY ("id"),
    UNIQUE KEY "id" ("id"),
    UNIQUE KEY "email" ("email"),
    KEY "idx_users_email" ("email")
);


-- defaultdb.catalog_items definição

CREATE TABLE "catalog_items" (
    "id" bigint NOT NULL AUTO_INCREMENT,
    "company_id" bigint NOT NULL,
    "type" varchar(10) NOT NULL,
    "internal_code" varchar(50) DEFAULT NULL,
    "description" varchar(255) NOT NULL,
    "unit_price" decimal(12,2) NOT NULL,
    "unit_of_measure" varchar(10) NOT NULL,
    "status" varchar(45) NOT NULL,
    "deleted_at" timestamp NULL DEFAULT NULL,
    "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    KEY "fk_catalog_items_company" ("company_id"),
    CONSTRAINT "fk_catalog_items_company" FOREIGN KEY ("company_id") REFERENCES "companies" ("id") ON DELETE CASCADE
);


-- defaultdb.catalog_physical_items definição

CREATE TABLE "catalog_physical_items" (
      "catalog_item_id" bigint NOT NULL,
      "ncm" varchar(8) NOT NULL,
      "default_cfop" varchar(4) NOT NULL,
      "default_csosn" varchar(4) NOT NULL DEFAULT '102',
      "gtin_ean" varchar(14) DEFAULT 'SEM GTIN',
      "cest" varchar(7) DEFAULT NULL,
      "origin" int DEFAULT '0',
      "default_icms_credit_rate" decimal(5,2) DEFAULT '0.00',
      "icms_rate" decimal(5,2) DEFAULT NULL,
      "pis_rate" decimal(5,2) DEFAULT NULL,
      "cofins_rate" decimal(5,2) DEFAULT NULL,
      PRIMARY KEY ("catalog_item_id"),
      CONSTRAINT "fk_catalog_physical_items_catalog_item" FOREIGN KEY ("catalog_item_id") REFERENCES "catalog_items" ("id") ON DELETE CASCADE
);


-- defaultdb.catalog_service_items definição

CREATE TABLE "catalog_service_items" (
     "catalog_item_id" bigint NOT NULL,
     "lc116_service_code" varchar(10) NOT NULL,
     "city_tax_code" varchar(20) DEFAULT NULL,
     "iss_rate" decimal(5,2) NOT NULL DEFAULT '0.00',
     "iss_withheld" tinyint(1) DEFAULT '0',
     PRIMARY KEY ("catalog_item_id"),
     CONSTRAINT "fk_catalog_service_items_catalog_item" FOREIGN KEY ("catalog_item_id") REFERENCES "catalog_items" ("id") ON DELETE CASCADE
);


-- defaultdb.collaborators definição

CREATE TABLE "collaborators" (
    "id" bigint unsigned NOT NULL AUTO_INCREMENT,
    "company_id" bigint NOT NULL,
    "user_id" bigint unsigned DEFAULT NULL,
    "name" varchar(255) NOT NULL,
    "document" varchar(14) DEFAULT NULL,
    "email" varchar(255) DEFAULT NULL,
    "phone" varchar(20) DEFAULT NULL,
    "status" varchar(45) NOT NULL DEFAULT 'ACTIVE',
    "street" varchar(255) DEFAULT NULL,
    "number" varchar(20) DEFAULT NULL,
    "complement" varchar(100) DEFAULT NULL,
    "neighborhood" varchar(100) DEFAULT NULL,
    "city" varchar(100) DEFAULT NULL,
    "state" varchar(2) DEFAULT NULL,
    "zip_code" varchar(8) DEFAULT NULL,
    "ibge_city_code" varchar(7) DEFAULT NULL,
    "state_registration" varchar(20) DEFAULT NULL,
    "birth_date" date DEFAULT NULL,
    "admission_date" date DEFAULT NULL,
    "resignation_date" date DEFAULT NULL,
    "job_title" varchar(100) DEFAULT NULL,
    "pis_pasep" varchar(20) DEFAULT NULL,
    "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    "default_commission_percentage" decimal(5,2) NOT NULL DEFAULT '0.00',
    PRIMARY KEY ("id"),
    KEY "idx_collaborators_company" ("company_id"),
    KEY "idx_collaborators_user" ("user_id"),
    KEY "idx_collaborators_document" ("document"),
    KEY "idx_collaborators_name" ("name"),
    CONSTRAINT "fk_collaborators_company" FOREIGN KEY ("company_id") REFERENCES "companies" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_collaborators_user" FOREIGN KEY ("user_id") REFERENCES "users" ("id") ON DELETE SET NULL
);


-- defaultdb.partners definição

CREATE TABLE "partners" (
    "id" bigint NOT NULL AUTO_INCREMENT,
    "company_id" bigint NOT NULL,
    "partner_identifier" varchar(100) DEFAULT NULL,
    "status" varchar(45) NOT NULL,
    "is_customer" tinyint(1) DEFAULT '0',
    "is_supplier" tinyint(1) DEFAULT '0',
    "is_carrier" tinyint(1) NOT NULL DEFAULT '0',
    "person_type" varchar(2) NOT NULL,
    "document" varchar(14) DEFAULT NULL,
    "name" varchar(255) NOT NULL,
    "indicator_ie" int DEFAULT '9',
    "state_registration" varchar(20) DEFAULT NULL,
    "email" varchar(255) DEFAULT NULL,
    "billing_email" varchar(255) DEFAULT NULL,
    "payables_email" varchar(255) DEFAULT NULL,
    "phone" varchar(20) DEFAULT NULL,
    "street" varchar(255) DEFAULT NULL,
    "number" varchar(20) DEFAULT NULL,
    "complement" varchar(100) DEFAULT NULL,
    "neighborhood" varchar(100) DEFAULT NULL,
    "ibge_city_code" varchar(7) DEFAULT NULL,
    "city" varchar(100) DEFAULT NULL,
    "state" varchar(2) DEFAULT NULL,
    "zip_code" varchar(8) DEFAULT NULL,
    "country_code" varchar(4) DEFAULT '1058',
    "country_name" varchar(50) DEFAULT 'Brasil',
    "deleted_at" timestamp NULL DEFAULT NULL,
    "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    KEY "fk_partners_company" ("company_id"),
    CONSTRAINT "fk_partners_company" FOREIGN KEY ("company_id") REFERENCES "companies" ("id") ON DELETE CASCADE
);


-- defaultdb.invoices definição

CREATE TABLE "invoices" (
    "id" bigint NOT NULL AUTO_INCREMENT,
    "company_id" bigint NOT NULL,
    "partner_id" bigint NOT NULL,
    "external_reference" varchar(100) DEFAULT NULL,
    "operation_type" int NOT NULL DEFAULT '1',
    "document_type" varchar(10) NOT NULL,
    "environment" varchar(10) NOT NULL,
    "status" varchar(45) NOT NULL,
    "invoice_number" int DEFAULT NULL,
    "series" int DEFAULT NULL,
    "access_key" varchar(44) DEFAULT NULL,
    "authorization_protocol" varchar(100) DEFAULT NULL,
    "payment_method" varchar(20) DEFAULT NULL,
    "due_date" date DEFAULT NULL,
    "freight_mode" int DEFAULT '9',
    "freight_amount" decimal(12,2) DEFAULT '0.00',
    "additional_expenses" decimal(12,2) DEFAULT '0.00',
    "products_amount" decimal(12,2) DEFAULT '0.00',
    "discount_amount" decimal(12,2) DEFAULT '0.00',
    "total_amount" decimal(12,2) NOT NULL,
    "additional_information" text,
    "canceled_at" timestamp NULL DEFAULT NULL,
    "cancellation_reason" text,
    "cancellation_protocol" varchar(100) DEFAULT NULL,
    "correction_letter" text,
    "notaas_id" varchar(100) DEFAULT NULL,
    "error_message" text,
    "pdf_url" text,
    "xml_url" text,
    "raw_payload_sent" json DEFAULT NULL,
    "raw_response_received" json DEFAULT NULL,
    "issued_at" timestamp NULL DEFAULT NULL,
    "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    KEY "fk_invoices_company" ("company_id"),
    KEY "fk_invoices_partner" ("partner_id"),
    CONSTRAINT "fk_invoices_company" FOREIGN KEY ("company_id") REFERENCES "companies" ("id"),
    CONSTRAINT "fk_invoices_partner" FOREIGN KEY ("partner_id") REFERENCES "partners" ("id")
);


-- defaultdb.orders definição

CREATE TABLE "orders" (
    "id" bigint NOT NULL AUTO_INCREMENT,
    "company_id" bigint NOT NULL,
    "partner_id" bigint NOT NULL,
    "status" varchar(20) NOT NULL,
    "services_amount" decimal(12,2) NOT NULL DEFAULT '0.00',
    "products_amount" decimal(12,2) NOT NULL DEFAULT '0.00',
    "freight_amount" decimal(12,2) NOT NULL DEFAULT '0.00',
    "discount_amount" decimal(12,2) NOT NULL DEFAULT '0.00',
    "total_amount" decimal(12,2) NOT NULL DEFAULT '0.00',
    "notes" text,
    "completed_at" timestamp NULL DEFAULT NULL,
    "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    KEY "idx_orders_company_status" ("company_id","status"),
    KEY "idx_orders_partner" ("partner_id"),
    CONSTRAINT "fk_orders_company" FOREIGN KEY ("company_id") REFERENCES "companies" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_orders_partner" FOREIGN KEY ("partner_id") REFERENCES "partners" ("id") ON DELETE RESTRICT
);


-- defaultdb.invoice_items definição

CREATE TABLE "invoice_items" (
    "id" bigint NOT NULL AUTO_INCREMENT,
    "invoice_id" bigint NOT NULL,
    "catalog_item_id" bigint DEFAULT NULL,
    "description" varchar(255) NOT NULL,
    "quantity" decimal(12,4) NOT NULL,
    "unit_price" decimal(12,2) NOT NULL,
    "discount_amount" decimal(12,2) DEFAULT '0.00',
    "total_price" decimal(12,2) NOT NULL,
    "unit_of_measure" varchar(10) NOT NULL,
    "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    KEY "fk_invoice_items_invoice" ("invoice_id"),
    KEY "fk_invoice_items_catalog_item" ("catalog_item_id"),
    CONSTRAINT "fk_invoice_items_catalog_item" FOREIGN KEY ("catalog_item_id") REFERENCES "catalog_items" ("id") ON DELETE SET NULL,
    CONSTRAINT "fk_invoice_items_invoice" FOREIGN KEY ("invoice_id") REFERENCES "invoices" ("id") ON DELETE CASCADE
);


-- defaultdb.order_installments definição

CREATE TABLE "order_installments" (
  "id" bigint NOT NULL AUTO_INCREMENT,
  "order_id" bigint NOT NULL,
  "company_id" bigint NOT NULL,
  "partner_id" bigint NOT NULL,
  "installment_number" int NOT NULL DEFAULT '1',
  "due_date" date NOT NULL,
  "amount" decimal(12,2) NOT NULL,
  "pending_amount" decimal(12,2) NOT NULL DEFAULT '0.00',
  "payment_method" varchar(20) NOT NULL,
  "status" varchar(20) NOT NULL DEFAULT 'PENDING',
  "bank_slip_code" varchar(100) DEFAULT NULL,
  "bank_slip_url" text,
  "notes" varchar(255) DEFAULT NULL,
  "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY ("id"),
  KEY "idx_installments_status_due" ("company_id","status","due_date"),
  KEY "idx_installments_order" ("order_id"),
  KEY "idx_installments_partner" ("partner_id"),
  CONSTRAINT "fk_order_installments_company" FOREIGN KEY ("company_id") REFERENCES "companies" ("id") ON DELETE CASCADE,
  CONSTRAINT "fk_order_installments_order" FOREIGN KEY ("order_id") REFERENCES "orders" ("id") ON DELETE CASCADE,
  CONSTRAINT "fk_order_installments_partner" FOREIGN KEY ("partner_id") REFERENCES "partners" ("id") ON DELETE RESTRICT
);


-- defaultdb.order_invoices definição

CREATE TABLE "order_invoices" (
    "id" bigint NOT NULL AUTO_INCREMENT,
    "company_id" bigint NOT NULL,
    "order_id" bigint NOT NULL,
    "invoice_id" bigint NOT NULL,
    "invoice_purpose" varchar(45) NOT NULL DEFAULT 'PRODUCT',
    "notes" varchar(255) DEFAULT NULL,
    "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    UNIQUE KEY "uq_order_invoice_purpose" ("order_id","invoice_id","invoice_purpose"),
    KEY "fk_order_invoices_invoice" ("invoice_id"),
    KEY "idx_order_invoices_order" ("company_id","order_id"),
    KEY "idx_order_invoices_invoice" ("company_id","invoice_id"),
    KEY "idx_order_invoices_purpose" ("invoice_purpose"),
    CONSTRAINT "fk_order_invoices_company" FOREIGN KEY ("company_id") REFERENCES "companies" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_order_invoices_invoice" FOREIGN KEY ("invoice_id") REFERENCES "invoices" ("id") ON DELETE RESTRICT,
    CONSTRAINT "fk_order_invoices_order" FOREIGN KEY ("order_id") REFERENCES "orders" ("id") ON DELETE CASCADE
);


-- defaultdb.order_items definição

CREATE TABLE "order_items" (
    "id" bigint NOT NULL AUTO_INCREMENT,
    "order_id" bigint NOT NULL,
    "catalog_item_id" bigint DEFAULT NULL,
    "item_type" varchar(10) NOT NULL,
    "description" varchar(255) NOT NULL,
    "quantity" decimal(12,4) NOT NULL DEFAULT '1.0000',
    "unit_price" decimal(12,2) NOT NULL,
    "discount_amount" decimal(12,2) NOT NULL DEFAULT '0.00',
    "total_price" decimal(12,2) NOT NULL,
    "professional_id" bigint unsigned DEFAULT NULL,
    "stock_deducted" tinyint(1) NOT NULL DEFAULT '0',
    "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    KEY "fk_order_items_catalog_item" ("catalog_item_id"),
    KEY "idx_order_items_order" ("order_id"),
    KEY "idx_order_items_type" ("item_type"),
    KEY "idx_order_items_professional" ("professional_id"),
    CONSTRAINT "fk_order_items_catalog_item" FOREIGN KEY ("catalog_item_id") REFERENCES "catalog_items" ("id") ON DELETE SET NULL,
    CONSTRAINT "fk_order_items_order" FOREIGN KEY ("order_id") REFERENCES "orders" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_order_items_professional" FOREIGN KEY ("professional_id") REFERENCES "collaborators" ("id") ON DELETE SET NULL,
    CONSTRAINT "order_items_chk_1" CHECK ((`item_type` in (_utf8mb4'PHYSICAL',_utf8mb4'SERVICE')))
);


-- defaultdb.order_shipping definição

CREATE TABLE "order_shipping" (
    "id" bigint NOT NULL AUTO_INCREMENT,
    "order_id" bigint NOT NULL,
    "carrier_id" bigint DEFAULT NULL,
    "freight_mode" int NOT NULL DEFAULT '0',
    "vehicle_plate" varchar(8) DEFAULT NULL,
    "vehicle_state" varchar(2) DEFAULT NULL,
    "rntrc_code" varchar(20) DEFAULT NULL,
    "package_quantity" int NOT NULL DEFAULT '1',
    "package_species" varchar(50) DEFAULT 'CAIXA',
    "package_brand" varchar(50) DEFAULT NULL,
    "package_numbering" varchar(50) DEFAULT NULL,
    "gross_weight_kg" decimal(10,3) NOT NULL DEFAULT '0.000',
    "net_weight_kg" decimal(10,3) NOT NULL DEFAULT '0.000',
    "tracking_code" varchar(100) DEFAULT NULL,
    "shipping_notes" varchar(255) DEFAULT NULL,
    "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    UNIQUE KEY "order_id" ("order_id"),
    KEY "idx_order_shipping_carrier" ("carrier_id"),
    CONSTRAINT "fk_order_shipping_carrier" FOREIGN KEY ("carrier_id") REFERENCES "partners" ("id") ON DELETE RESTRICT,
    CONSTRAINT "fk_order_shipping_order" FOREIGN KEY ("order_id") REFERENCES "orders" ("id") ON DELETE CASCADE
);


-- defaultdb.payment_transactions definição

CREATE TABLE "payment_transactions" (
    "id" bigint NOT NULL AUTO_INCREMENT,
    "installment_id" bigint NOT NULL,
    "company_id" bigint NOT NULL,
    "paid_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "amount_paid" decimal(12,2) NOT NULL,
    "payment_method" varchar(20) NOT NULL,
    "card_brand" varchar(50) DEFAULT NULL,
    "nsu_authorization" varchar(50) DEFAULT NULL,
    "notes" varchar(255) DEFAULT NULL,
    "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    KEY "idx_transactions_company_paid" ("company_id","paid_at"),
    KEY "idx_transactions_installment" ("installment_id"),
    CONSTRAINT "fk_transactions_company" FOREIGN KEY ("company_id") REFERENCES "companies" ("id") ON DELETE CASCADE,
    CONSTRAINT "fk_transactions_installment" FOREIGN KEY ("installment_id") REFERENCES "order_installments" ("id") ON DELETE CASCADE
);


-- defaultdb.physical_invoice_items definição

CREATE TABLE "physical_invoice_items" (
      "invoice_item_id" bigint NOT NULL,
      "ncm" varchar(8) NOT NULL,
      "cfop" varchar(4) NOT NULL,
      "csosn" varchar(4) NOT NULL,
      "cest" varchar(7) DEFAULT NULL,
      "gtin_ean" varchar(14) DEFAULT 'SEM GTIN',
      "origin" int DEFAULT '0',
      "icms_credit_rate" decimal(5,2) DEFAULT '0.00',
      "icms_credit_amount" decimal(12,2) DEFAULT '0.00',
      "pis_cst" varchar(2) DEFAULT '49',
      "cofins_cst" varchar(2) DEFAULT '49',
      PRIMARY KEY ("invoice_item_id"),
      CONSTRAINT "fk_physical_invoice_items_item" FOREIGN KEY ("invoice_item_id") REFERENCES "invoice_items" ("id") ON DELETE CASCADE
);


-- defaultdb.service_invoice_items definição

CREATE TABLE "service_invoice_items" (
     "invoice_item_id" bigint NOT NULL,
     "lc116_service_code" varchar(10) NOT NULL,
     "city_tax_code" varchar(20) DEFAULT NULL,
     "service_execution_city_ibge" varchar(7) DEFAULT NULL,
     "iss_rate" decimal(5,2) NOT NULL DEFAULT '0.00',
     "iss_amount" decimal(12,2) NOT NULL DEFAULT '0.00',
     "iss_withheld" tinyint(1) DEFAULT '0',
     "deductions_amount" decimal(12,2) DEFAULT '0.00',
     PRIMARY KEY ("invoice_item_id"),
     CONSTRAINT "fk_service_invoice_items_item" FOREIGN KEY ("invoice_item_id") REFERENCES "invoice_items" ("id") ON DELETE CASCADE
);