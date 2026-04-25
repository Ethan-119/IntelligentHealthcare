-- ========================== PATIENT 用户表
CREATE TABLE IF NOT EXISTS patient (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(32) NOT NULL UNIQUE,
    username VARCHAR(64),
    password VARCHAR(128) NOT NULL,
    status SMALLINT DEFAULT 1,
    patient_age INT,
    patient_gender VARCHAR(16) DEFAULT 'unknown',
    resident_city VARCHAR(64),
    area VARCHAR(64),
    triage_prefer VARCHAR(32) DEFAULT 'nearby',
    deleted SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================== DISEASE MASTER
CREATE TABLE IF NOT EXISTS disease_master (
    id BIGSERIAL PRIMARY KEY,
    disease_code VARCHAR(64) NOT NULL UNIQUE,
    disease_name VARCHAR(255) NOT NULL,
    aliases_json TEXT,
    symptom_keywords TEXT,
    gender_rule VARCHAR(32) DEFAULT 'all',
    age_min INT,
    age_max INT,
    age_group VARCHAR(32),
    urgency_level VARCHAR(32) DEFAULT 'medium',
    review_status VARCHAR(32) DEFAULT 'approved',
    deleted SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================== DISEASE ALIAS
CREATE TABLE IF NOT EXISTS disease_alias (
    id BIGSERIAL PRIMARY KEY,
    disease_code VARCHAR(64) NOT NULL,
    alias_name VARCHAR(255) NOT NULL,
    alias_type VARCHAR(64),
    source VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================== MEDICAL CAPABILITY CATALOG
CREATE TABLE IF NOT EXISTS medical_capability_catalog (
    id BIGSERIAL PRIMARY KEY,
    capability_code VARCHAR(64) NOT NULL UNIQUE,
    capability_name VARCHAR(255) NOT NULL,
    capability_type VARCHAR(64) NOT NULL,
    parent_code VARCHAR(64),
    standard_dept_code VARCHAR(64),
    aliases_json TEXT,
    gender_rule VARCHAR(32) DEFAULT 'all',
    age_min INT,
    age_max INT,
    crowd_tags_json TEXT,
    pathway_tags_json TEXT,
    active_status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================== DISEASE CAPABILITY REL
CREATE TABLE IF NOT EXISTS disease_capability_rel (
    id BIGSERIAL PRIMARY KEY,
    disease_code VARCHAR(64) NOT NULL,
    capability_code VARCHAR(64) NOT NULL,
    rel_type VARCHAR(64),
    priority_score DECIMAL(8,2) DEFAULT 1.00,
    crowd_constraint VARCHAR(255),
    note VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================== HOSPITAL
CREATE TABLE IF NOT EXISTS hospital (
    id BIGSERIAL PRIMARY KEY,
    hospital_id VARCHAR(64) NOT NULL UNIQUE,
    hospital_name VARCHAR(255) NOT NULL,
    city VARCHAR(64),
    district_name VARCHAR(64),
    latitude DECIMAL(10,6),
    longitude DECIMAL(10,6),
    hospital_level VARCHAR(32),
    is_emergency SMALLINT DEFAULT 0,
    authority_score DECIMAL(10,2) DEFAULT 0,
    active_status SMALLINT DEFAULT 1,
    deleted SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================== HOSPITAL DEPARTMENT
CREATE TABLE IF NOT EXISTS hospital_department (
    id BIGSERIAL PRIMARY KEY,
    hospital_id VARCHAR(64) NOT NULL,
    department_name VARCHAR(255) NOT NULL,
    parent_department_name VARCHAR(255),
    department_intro TEXT,
    service_scope TEXT,
    active_status SMALLINT DEFAULT 1,
    deleted SMALLINT DEFAULT 0,
    gender_rule VARCHAR(32) DEFAULT 'all',
    age_min INT,
    age_max INT,
    crowd_tags_json TEXT,
    standard_dept_code VARCHAR(64),
    subspecialty_code VARCHAR(64),
    district_name VARCHAR(64),
    latitude DECIMAL(10,6),
    longitude DECIMAL(10,6),
    is_emergency SMALLINT DEFAULT 0,
    national_key_score DECIMAL(10,2) DEFAULT 0,
    provincial_key_score DECIMAL(10,2) DEFAULT 0,
    city_key_score DECIMAL(10,2) DEFAULT 0,
    authority_score DECIMAL(10,2) DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================== DEPARTMENT CAPABILITY REL
CREATE TABLE IF NOT EXISTS department_capability_rel (
    id BIGSERIAL PRIMARY KEY,
    department_id BIGINT NOT NULL,
    capability_code VARCHAR(64) NOT NULL,
    support_level VARCHAR(32) DEFAULT 'PRIMARY',
    weight DECIMAL(8,2) DEFAULT 1.00,
    source VARCHAR(64),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================== IMPORT JOB RECORD
CREATE TABLE IF NOT EXISTS import_job_record (
    id BIGSERIAL PRIMARY KEY,
    dataset_type VARCHAR(64) NOT NULL,
    file_name VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    success_count INT DEFAULT 0,
    failure_count INT DEFAULT 0,
    review_count INT DEFAULT 0,
    auto_mapped_count INT DEFAULT 0,
    message VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================== IMPORT FAILURE LOG
CREATE TABLE IF NOT EXISTS import_failure_log (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    row_number INT,
    raw_content TEXT,
    error_message VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================== IMPORT REVIEW ITEM
CREATE TABLE IF NOT EXISTS import_review_item (
    id BIGSERIAL PRIMARY KEY,
    job_id BIGINT NOT NULL,
    dataset_type VARCHAR(64),
    item_key VARCHAR(128),
    issue_type VARCHAR(128),
    raw_content TEXT,
    suggestion VARCHAR(500),
    resolved SMALLINT DEFAULT 0,
    resolution_note VARCHAR(500),
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================== DOCTOR PROFILE
CREATE TABLE IF NOT EXISTS doctor_profile (
    id BIGSERIAL PRIMARY KEY,
    hospital_id VARCHAR(64) NOT NULL,
    department_id BIGINT NOT NULL,
    doctor_name VARCHAR(128) NOT NULL,
    title VARCHAR(64),
    specialty_text TEXT,
    gender_rule VARCHAR(32) DEFAULT 'all',
    age_min INT,
    age_max INT,
    crowd_tags_json TEXT,
    authority_score DECIMAL(10,2) DEFAULT 0,
    academic_title_score DECIMAL(10,2) DEFAULT 0,
    is_expert SMALLINT DEFAULT 0,
    campus_name VARCHAR(128),
    active_status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================== DOCTOR CAPABILITY REL
CREATE TABLE IF NOT EXISTS doctor_capability_rel (
    id BIGSERIAL PRIMARY KEY,
    doctor_id BIGINT NOT NULL,
    capability_code VARCHAR(64),
    weight DECIMAL(8,2) DEFAULT 0.30,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================== AI RECALL AUDIT LOG
CREATE TABLE IF NOT EXISTS ai_recall_audit_log (
    id BIGSERIAL PRIMARY KEY,
    symptoms TEXT,
    gender VARCHAR(32),
    age INT,
    age_group VARCHAR(32),
    eligible_disease_count INT DEFAULT 0,
    rule_candidate_codes_json TEXT,
    suggested_codes_json TEXT,
    status VARCHAR(64),
    message VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================== TRIAGE SESSION
CREATE TABLE IF NOT EXISTS triage_session (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    user_id VARCHAR(64),
    dialog_id VARCHAR(64),
    current_stage VARCHAR(32),
    ask_round INT DEFAULT 0,
    invalid_answer_count INT DEFAULT 0,
    city VARCHAR(64),
    area VARCHAR(64),
    nearby SMALLINT DEFAULT 0,
    latitude DECIMAL(10,6),
    longitude DECIMAL(10,6),
    patient_age INT,
    patient_gender VARCHAR(32),
    severity_level VARCHAR(32),
    route_type VARCHAR(32),
    status VARCHAR(32) DEFAULT 'active',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================== TRIAGE TURN
CREATE TABLE IF NOT EXISTS triage_turn (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    turn_no INT NOT NULL,
    user_message TEXT,
    normalized_query TEXT,
    intent VARCHAR(64),
    stage VARCHAR(32),
    reply_text TEXT,
    raw_decision_json TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ========================== TRIAGE SLOT STATE
CREATE TABLE IF NOT EXISTS triage_slot_state (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    symptoms_json TEXT,
    disease_name VARCHAR(255),
    target_hospital VARCHAR(255),
    target_department VARCHAR(255),
    target_doctor VARCHAR(255),
    missing_slots_json TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);