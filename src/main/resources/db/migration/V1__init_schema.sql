CREATE EXTENSION IF NOT EXISTS "pgcrypto" WITH SCHEMA public;

CREATE SCHEMA IF NOT EXISTS medichain;
SET search_path TO medichain;

-- ============================================================
-- HOSPITAL
-- ============================================================
CREATE TABLE hospital (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(50) UNIQUE,
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(10),
    gstin VARCHAR(15),
    pharmacy_license_number VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(255),
    bed_count INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- ============================================================
-- WARD
-- ============================================================
CREATE TABLE ward (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20),
    floor VARCHAR(20),
    description VARCHAR(500),
    hospital_id UUID NOT NULL REFERENCES hospital(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_ward_hospital ON ward(hospital_id);

-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    role VARCHAR(30) NOT NULL CHECK (role IN ('PHARMACY_MANAGER','WARD_PHARMACIST','PROCUREMENT_OFFICER','NGO_PARTNER','ADMIN')),
    hospital_id UUID NOT NULL REFERENCES hospital(id),
    ward_id UUID REFERENCES ward(id),
    must_change_password BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_user_hospital ON users(hospital_id);
CREATE INDEX idx_user_ward ON users(ward_id);
CREATE INDEX idx_user_role ON users(role);

-- ============================================================
-- DRUG SKU (Master Drug Catalog)
-- ============================================================
CREATE TABLE drug_sku (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    generic_name VARCHAR(255) NOT NULL,
    brand_name VARCHAR(255),
    form VARCHAR(50),
    strength VARCHAR(50),
    unit_of_measure VARCHAR(20),
    hsn_code VARCHAR(8),
    scheduled_drug BOOLEAN NOT NULL DEFAULT FALSE,
    narcotic BOOLEAN NOT NULL DEFAULT FALSE,
    ved_classification VARCHAR(20) CHECK (ved_classification IN ('VITAL','ESSENTIAL','DESIRABLE')),
    abc_classification VARCHAR(5) CHECK (abc_classification IN ('A','B','C')),
    storage_condition VARCHAR(20) CHECK (storage_condition IN ('AMBIENT','REFRIGERATED','CONTROLLED')),
    unit_price NUMERIC(12,2),
    manufacturer VARCHAR(255),
    gst_rate NUMERIC(5,2) DEFAULT 12.00,
    reorder_quantity INTEGER,
    lead_time_days INTEGER,
    min_stock_level INTEGER,
    max_stock_level INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_sku_generic ON drug_sku(generic_name);
CREATE INDEX idx_sku_ved ON drug_sku(ved_classification);

-- ============================================================
-- DRUG SHELF
-- ============================================================
CREATE TABLE drug_shelf (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shelf_code VARCHAR(50) NOT NULL,
    location_description VARCHAR(255),
    ward_id UUID NOT NULL REFERENCES ward(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_shelf_ward ON drug_shelf(ward_id);

-- ============================================================
-- DRUG BATCH
-- ============================================================
CREATE TABLE drug_batch (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_number VARCHAR(100) NOT NULL,
    drug_sku_id UUID NOT NULL REFERENCES drug_sku(id),
    shelf_id UUID NOT NULL REFERENCES drug_shelf(id),
    manufacture_date DATE,
    expiry_date DATE NOT NULL,
    quantity_on_hand INTEGER NOT NULL CHECK (quantity_on_hand >= 0),
    unit_cost NUMERIC(12,2),
    mrp NUMERIC(12,2),
    batch_notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_batch_expiry ON drug_batch(expiry_date);
CREATE INDEX idx_batch_sku_shelf ON drug_batch(drug_sku_id, shelf_id);
CREATE INDEX idx_batch_expiry_active ON drug_batch(expiry_date, quantity_on_hand);

-- ============================================================
-- STOCK TRANSACTION
-- ============================================================
CREATE TABLE stock_transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('RECEIPT','DISPENSE','TRANSFER','ADJUSTMENT','WASTE','NGO_TRANSFER')),
    drug_batch_id UUID NOT NULL REFERENCES drug_batch(id),
    ward_id UUID NOT NULL REFERENCES ward(id),
    source_ward_id UUID,
    destination_ward_id UUID,
    quantity INTEGER NOT NULL,
    quantity_before INTEGER,
    quantity_after INTEGER,
    performed_by_id UUID NOT NULL REFERENCES users(id),
    reference_number VARCHAR(100),
    notes VARCHAR(1000),
    transaction_timestamp TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_txn_batch ON stock_transaction(drug_batch_id);
CREATE INDEX idx_txn_timestamp ON stock_transaction(transaction_timestamp);
CREATE INDEX idx_txn_ward_type ON stock_transaction(ward_id, transaction_type);
CREATE INDEX idx_txn_performed_by ON stock_transaction(performed_by_id);

-- ============================================================
-- VENDOR
-- ============================================================
CREATE TABLE vendor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    phone VARCHAR(20),
    email VARCHAR(255),
    address VARCHAR(500),
    gstin VARCHAR(15),
    drug_license_number VARCHAR(50),
    payment_terms VARCHAR(255),
    lead_time_days INTEGER,
    is_preferred BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- ============================================================
-- PROCUREMENT ORDER
-- ============================================================
CREATE TABLE procurement_order (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT','PENDING_APPROVAL','APPROVED','DISPATCHED','RECEIVED','CANCELLED')),
    generated_by_id UUID NOT NULL REFERENCES users(id),
    approved_by_id UUID REFERENCES users(id),
    vendor_id UUID NOT NULL REFERENCES vendor(id),
    order_date DATE NOT NULL,
    expected_delivery_date DATE,
    delivery_date DATE,
    notes VARCHAR(2000),
    total_amount NUMERIC(14,2),
    gst_amount NUMERIC(14,2),
    grand_total NUMERIC(14,2),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_po_vendor ON procurement_order(vendor_id);
CREATE INDEX idx_po_status ON procurement_order(status);
CREATE INDEX idx_po_generated_by ON procurement_order(generated_by_id);

-- ============================================================
-- PROCUREMENT LINE ITEM
-- ============================================================
CREATE TABLE procurement_line_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    procurement_order_id UUID NOT NULL REFERENCES procurement_order(id),
    drug_sku_id UUID NOT NULL REFERENCES drug_sku(id),
    requested_quantity INTEGER NOT NULL,
    approved_quantity INTEGER,
    received_quantity INTEGER,
    unit_price NUMERIC(12,2),
    line_total NUMERIC(14,2),
    justification VARCHAR(1000),
    line_number INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_pli_order ON procurement_line_item(procurement_order_id);

-- ============================================================
-- NGO (Partner Non-Profit Registry)
-- ============================================================
CREATE TABLE ngo (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    registration_number VARCHAR(50),
    contact_person VARCHAR(255),
    phone VARCHAR(20),
    email VARCHAR(255),
    address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    has_cold_chain BOOLEAN NOT NULL DEFAULT FALSE,
    acceptance_categories VARCHAR(1000),
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- ============================================================
-- NGO REDISTRIBUTION REQUEST
-- ============================================================
CREATE TABLE ngo_redistribution_request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    drug_batch_id UUID NOT NULL REFERENCES drug_batch(id),
    requesting_ngo_id UUID NOT NULL REFERENCES ngo(id),
    quantity_requested INTEGER NOT NULL,
    quantity_approved INTEGER,
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT','PENDING_APPROVAL','APPROVED','REJECTED','COMPLETED')),
    reviewed_by_id UUID REFERENCES users(id),
    logistics_notes VARCHAR(2000),
    pickup_date DATE,
    donation_certificate_generated BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_ngo_req_status ON ngo_redistribution_request(status);
CREATE INDEX idx_ngo_req_batch ON ngo_redistribution_request(drug_batch_id);

-- ============================================================
-- STOCK ALERT
-- ============================================================
CREATE TABLE stock_alert (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ward_id UUID NOT NULL REFERENCES ward(id),
    drug_sku_id UUID NOT NULL REFERENCES drug_sku(id),
    drug_batch_id UUID REFERENCES drug_batch(id),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('CRITICAL','WARNING','WATCH','INFO')),
    alert_type VARCHAR(30) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    current_stock INTEGER,
    days_until_stockout INTEGER,
    days_until_expiry INTEGER,
    is_acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by_id UUID,
    acknowledged_at TIMESTAMP,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_alert_ward ON stock_alert(ward_id);
CREATE INDEX idx_alert_active ON stock_alert(is_active, resolved_at);

-- ============================================================
-- EXPIRY ALERT RECORD
-- ============================================================
CREATE TABLE expiry_alert_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    drug_batch_id UUID NOT NULL REFERENCES drug_batch(id),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('CRITICAL','WARNING','WATCH','INFO')),
    days_to_expiry INTEGER,
    alert_message VARCHAR(1000),
    notified_pharmacy_manager BOOLEAN NOT NULL DEFAULT FALSE,
    notified_ward_pharmacist BOOLEAN NOT NULL DEFAULT FALSE,
    ngo_draft_created BOOLEAN NOT NULL DEFAULT FALSE,
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_expiry_batch ON expiry_alert_record(drug_batch_id);
CREATE INDEX idx_expiry_severity ON expiry_alert_record(severity);

-- ============================================================
-- AI FORECAST REPORT
-- ============================================================
CREATE TABLE ai_forecast_report (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ward_id UUID NOT NULL REFERENCES ward(id),
    drug_sku_id UUID NOT NULL REFERENCES drug_sku(id),
    forecast_generated_at TIMESTAMP NOT NULL,
    predicted_stockout_date DATE,
    recommended_order_quantity INTEGER,
    confidence_score DOUBLE PRECISION,
    key_risk_factors TEXT,
    suggested_transfer_opportunities TEXT,
    raw_payload JSONB,
    model_version VARCHAR(50),
    processing_time_ms BIGINT,
    is_actionable BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_forecast_ward_sku ON ai_forecast_report(ward_id, drug_sku_id);
CREATE INDEX idx_forecast_actionable ON ai_forecast_report(is_actionable, is_active);

-- ============================================================
-- BATCH JOB EXECUTION TABLES (Spring Batch metadata)
-- ============================================================
CREATE TABLE BATCH_JOB_INSTANCE  (
    JOB_INSTANCE_ID BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    VERSION BIGINT,
    JOB_NAME VARCHAR(100) NOT NULL,
    JOB_KEY VARCHAR(32) NOT NULL,
    CONSTRAINT JOB_INST_UN UNIQUE (JOB_NAME, JOB_KEY)
);

CREATE TABLE BATCH_JOB_EXECUTION  (
    JOB_EXECUTION_ID BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    VERSION BIGINT,
    JOB_INSTANCE_ID BIGINT NOT NULL,
    CREATE_TIME TIMESTAMP NOT NULL,
    START_TIME TIMESTAMP DEFAULT NULL,
    END_TIME TIMESTAMP DEFAULT NULL,
    STATUS VARCHAR(10),
    EXIT_CODE VARCHAR(2500),
    EXIT_MESSAGE VARCHAR(2500),
    LAST_UPDATED TIMESTAMP,
    CONSTRAINT JOB_INST_EXEC_FK FOREIGN KEY (JOB_INSTANCE_ID) REFERENCES BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
);

CREATE TABLE BATCH_JOB_EXECUTION_PARAMS  (
    JOB_EXECUTION_ID BIGINT NOT NULL,
    PARAMETER_NAME VARCHAR(100) NOT NULL,
    PARAMETER_TYPE VARCHAR(100) NOT NULL,
    PARAMETER_VALUE VARCHAR(2500),
    IDENTIFYING CHAR(1) NOT NULL,
    CONSTRAINT JOB_EXEC_PARAMS_FK FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

CREATE TABLE BATCH_STEP_EXECUTION  (
    STEP_EXECUTION_ID BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    VERSION BIGINT NOT NULL,
    STEP_NAME VARCHAR(100) NOT NULL,
    JOB_EXECUTION_ID BIGINT NOT NULL,
    CREATE_TIME TIMESTAMP NOT NULL,
    START_TIME TIMESTAMP DEFAULT NULL,
    END_TIME TIMESTAMP DEFAULT NULL,
    STATUS VARCHAR(10),
    COMMIT_COUNT BIGINT,
    READ_COUNT BIGINT,
    FILTER_COUNT BIGINT,
    WRITE_COUNT BIGINT,
    READ_SKIP_COUNT BIGINT,
    WRITE_SKIP_COUNT BIGINT,
    PROCESS_SKIP_COUNT BIGINT,
    ROLLBACK_COUNT BIGINT,
    EXIT_CODE VARCHAR(2500),
    EXIT_MESSAGE VARCHAR(2500),
    LAST_UPDATED TIMESTAMP,
    CONSTRAINT JOB_EXEC_STEP_FK FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT  (
    STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    SHORT_CONTEXT VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    CONSTRAINT STEP_EXEC_CTX_FK FOREIGN KEY (STEP_EXECUTION_ID) REFERENCES BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)
);

CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT  (
    JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
    SHORT_CONTEXT VARCHAR(2500) NOT NULL,
    SERIALIZED_CONTEXT TEXT,
    CONSTRAINT JOB_EXEC_CTX_FK FOREIGN KEY (JOB_EXECUTION_ID) REFERENCES BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
);

CREATE SEQUENCE BATCH_STEP_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE BATCH_JOB_EXECUTION_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
CREATE SEQUENCE BATCH_JOB_SEQ MAXVALUE 9223372036854775807 NO CYCLE;
