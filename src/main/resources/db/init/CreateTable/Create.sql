-- ========================== PATIENT 用户表
CREATE TABLE IF NOT EXISTS patient (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    phone VARCHAR(32) NOT NULL UNIQUE, -- 手机号（登录账号）
    username VARCHAR(64), -- 用户名/昵称
    password VARCHAR(128) NOT NULL, -- 密码哈希
    status SMALLINT DEFAULT 1, -- 账号状态：1启用，0禁用
    role VARCHAR(16) DEFAULT 'patient', -- 角色：patient/admin
    patient_age INT, -- 患者年龄
    patient_gender VARCHAR(16) DEFAULT 'unknown', -- 性别：male/female/unknown
    resident_city VARCHAR(64), -- 常住城市
    area VARCHAR(64), -- 区域/区县
    triage_prefer VARCHAR(32) DEFAULT 'nearby', -- 就医偏好
    deleted SMALLINT DEFAULT 0, -- 逻辑删除：0未删，1已删
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 更新时间
);

ALTER TABLE IF EXISTS patient
    ADD COLUMN IF NOT EXISTS role VARCHAR(16) DEFAULT 'patient';

UPDATE patient
SET role = 'patient'
WHERE role IS NULL OR role = '';

-- ========================== DISEASE MASTER
CREATE TABLE IF NOT EXISTS disease_master (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    disease_code VARCHAR(64) NOT NULL UNIQUE, -- 疾病编码
    disease_name VARCHAR(255) NOT NULL, -- 疾病名称
    symptom_keywords TEXT, -- 症状关键词
    gender_rule VARCHAR(32) DEFAULT 'all', -- 性别约束
    age_min INT, -- 最小年龄
    age_max INT, -- 最大年龄
    urgency_level VARCHAR(32) DEFAULT 'medium', -- 紧急程度
    review_status VARCHAR(32) DEFAULT 'approved', -- 审核状态
    deleted SMALLINT DEFAULT 0, -- 逻辑删除标记
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 更新时间
);

-- ========================== DISEASE ALIAS
CREATE TABLE IF NOT EXISTS disease_alias (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    disease_code VARCHAR(64) NOT NULL, -- 关联疾病编码
    alias_name VARCHAR(255) NOT NULL, -- 别名
    alias_type VARCHAR(64), -- 别名类型
    source VARCHAR(64), -- 来源
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 创建时间
);

-- ========================== MEDICAL CAPABILITY CATALOG
CREATE TABLE IF NOT EXISTS medical_capability_catalog (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    capability_code VARCHAR(64) NOT NULL UNIQUE, -- 能力编码
    capability_name VARCHAR(255) NOT NULL, -- 能力名称
    capability_type VARCHAR(64) NOT NULL, -- 能力类型
    parent_code VARCHAR(64), -- 父级编码
    standard_dept_code VARCHAR(64), -- 标准科室编码
    aliases_json TEXT, -- 别名JSON
    gender_rule VARCHAR(32) DEFAULT 'all', -- 性别约束
    age_min INT, -- 最小年龄
    age_max INT, -- 最大年龄
    crowd_tags_json TEXT, -- 人群标签JSON
    pathway_tags_json TEXT, -- 路径标签JSON
    active_status SMALLINT DEFAULT 1, -- 启用状态
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 更新时间
);

-- ========================== DISEASE CAPABILITY REL
CREATE TABLE IF NOT EXISTS disease_capability_rel (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    disease_code VARCHAR(64) NOT NULL, -- 疾病编码
    capability_code VARCHAR(64) NOT NULL, -- 能力编码
    rel_type VARCHAR(64), -- 关系类型
    priority_score DECIMAL(8,2) DEFAULT 1.00, -- 优先级分数
    crowd_constraint VARCHAR(255), -- 人群限制
    note VARCHAR(500), -- 备注
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 创建时间
);

-- ========================== HOSPITAL
CREATE TABLE IF NOT EXISTS hospital (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    hospital_id VARCHAR(64) NOT NULL UNIQUE, -- 医院业务ID
    hospital_name VARCHAR(255) NOT NULL, -- 医院名称
    city VARCHAR(64), -- 城市
    district_name VARCHAR(64), -- 区县
    latitude DECIMAL(10,6), -- 纬度
    longitude DECIMAL(10,6), -- 经度
    hospital_level VARCHAR(32), -- 医院等级
    is_emergency SMALLINT DEFAULT 0, -- 是否急诊
    authority_score DECIMAL(10,2) DEFAULT 0, -- 权威分
    active_status SMALLINT DEFAULT 1, -- 启用状态
    deleted SMALLINT DEFAULT 0, -- 逻辑删除标记
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 更新时间
);

-- ========================== HOSPITAL DEPARTMENT
CREATE TABLE IF NOT EXISTS hospital_department (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    hospital_id VARCHAR(64) NOT NULL, -- 医院业务ID
    department_name VARCHAR(255) NOT NULL, -- 科室名称
    parent_department_name VARCHAR(255), -- 父级科室
    department_intro TEXT, -- 科室介绍
    service_scope TEXT, -- 服务范围
    active_status SMALLINT DEFAULT 1, -- 启用状态
    deleted SMALLINT DEFAULT 0, -- 逻辑删除标记
    gender_rule VARCHAR(32) DEFAULT 'all', -- 性别约束
    age_min INT, -- 最小年龄
    age_max INT, -- 最大年龄
    crowd_tags_json TEXT, -- 人群标签JSON
    standard_dept_code VARCHAR(64), -- 标准科室编码
    subspecialty_code VARCHAR(64), -- 亚专科编码
    is_emergency SMALLINT DEFAULT 0, -- 是否急诊
    national_key_score DECIMAL(10,2) DEFAULT 0, -- 国家重点评分
    provincial_key_score DECIMAL(10,2) DEFAULT 0, -- 省级重点评分
    city_key_score DECIMAL(10,2) DEFAULT 0, -- 市级重点评分
    authority_score DECIMAL(10,2) DEFAULT 0, -- 权威分
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 更新时间
);

-- ========================== DEPARTMENT CAPABILITY REL
CREATE TABLE IF NOT EXISTS department_capability_rel (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    department_id BIGINT NOT NULL, -- 科室ID
    capability_code VARCHAR(64) NOT NULL, -- 能力编码
    support_level VARCHAR(32) DEFAULT 'PRIMARY', -- 支持级别
    weight DECIMAL(8,2) DEFAULT 1.00, -- 权重
    source VARCHAR(64), -- 来源
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 创建时间
);

-- ========================== IMPORT JOB RECORD
CREATE TABLE IF NOT EXISTS import_job_record (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    dataset_type VARCHAR(64) NOT NULL, -- 导入数据集类型
    file_name VARCHAR(255), -- 文件名
    status VARCHAR(32) NOT NULL, -- 任务状态
    success_count INT DEFAULT 0, -- 成功数量
    failure_count INT DEFAULT 0, -- 失败数量
    review_count INT DEFAULT 0, -- 待复核数量
    auto_mapped_count INT DEFAULT 0, -- 自动映射数量
    message VARCHAR(500), -- 任务说明
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 更新时间
);

-- ========================== IMPORT FAILURE LOG
CREATE TABLE IF NOT EXISTS import_failure_log (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    job_id BIGINT NOT NULL, -- 导入任务ID
    row_number INT, -- 行号
    raw_content TEXT, -- 原始内容
    error_message VARCHAR(500), -- 错误信息
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 创建时间
);

-- ========================== IMPORT REVIEW ITEM
CREATE TABLE IF NOT EXISTS import_review_item (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    job_id BIGINT NOT NULL, -- 导入任务ID
    item_key VARCHAR(128), -- 问题项键
    issue_type VARCHAR(128), -- 问题类型
    raw_content TEXT, -- 原始内容
    suggestion VARCHAR(500), -- 建议
    resolved SMALLINT DEFAULT 0, -- 是否已处理
    resolution_note VARCHAR(500), -- 处理备注
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 更新时间
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 创建时间
);

ALTER TABLE IF EXISTS disease_master
    DROP COLUMN IF EXISTS aliases_json,
    DROP COLUMN IF EXISTS age_group;

ALTER TABLE IF EXISTS import_review_item
    DROP COLUMN IF EXISTS dataset_type;

ALTER TABLE IF EXISTS hospital_department
    DROP COLUMN IF EXISTS district_name,
    DROP COLUMN IF EXISTS latitude,
    DROP COLUMN IF EXISTS longitude;

-- ========================== DOCTOR PROFILE
CREATE TABLE IF NOT EXISTS doctor_profile (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    hospital_id VARCHAR(64) NOT NULL, -- 医院业务ID
    department_id BIGINT NOT NULL, -- 科室ID
    doctor_name VARCHAR(128) NOT NULL, -- 医生姓名
    title VARCHAR(64), -- 职称
    specialty_text TEXT, -- 擅长描述
    gender_rule VARCHAR(32) DEFAULT 'all', -- 性别约束
    age_min INT, -- 最小年龄
    age_max INT, -- 最大年龄
    crowd_tags_json TEXT, -- 人群标签JSON
    authority_score DECIMAL(10,2) DEFAULT 0, -- 权威分
    academic_title_score DECIMAL(10,2) DEFAULT 0, -- 学术头衔分
    is_expert SMALLINT DEFAULT 0, -- 是否专家
    campus_name VARCHAR(128), -- 院区名称
    active_status SMALLINT DEFAULT 1, -- 启用状态
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 更新时间
);

-- ========================== DOCTOR CAPABILITY REL
CREATE TABLE IF NOT EXISTS doctor_capability_rel (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    doctor_id BIGINT NOT NULL, -- 医生ID
    capability_code VARCHAR(64), -- 能力编码
    weight DECIMAL(8,2) DEFAULT 0.30, -- 权重
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 创建时间
);

-- ========================== AI RECALL AUDIT LOG
CREATE TABLE IF NOT EXISTS ai_recall_audit_log (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    symptoms TEXT, -- 症状原文
    gender VARCHAR(32), -- 性别
    age INT, -- 年龄
    age_group VARCHAR(32), -- 年龄分组
    eligible_disease_count INT DEFAULT 0, -- 建议疾病数量
    rule_candidate_codes_json TEXT, -- 候选疾病编码JSON
    suggested_codes_json TEXT, -- 最终建议编码JSON
    status VARCHAR(64), -- 状态：SUCCESS/FAILED/EMERGENCY_ALERT
    message VARCHAR(500), -- 结果摘要/错误信息
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 创建时间
);

-- ========================== TRIAGE SESSION
CREATE TABLE IF NOT EXISTS triage_session (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    session_id VARCHAR(64) NOT NULL UNIQUE, -- 会话ID
    user_id VARCHAR(64), -- 用户ID
    dialog_id VARCHAR(64), -- 对话ID
    current_stage VARCHAR(32), -- 当前阶段
    ask_round INT DEFAULT 0, -- 问诊轮数
    invalid_answer_count INT DEFAULT 0, -- 无效回答次数
    city VARCHAR(64), -- 城市
    area VARCHAR(64), -- 区域
    nearby SMALLINT DEFAULT 0, -- 是否就近偏好
    latitude DECIMAL(10,6), -- 纬度
    longitude DECIMAL(10,6), -- 经度
    patient_age INT, -- 年龄快照
    patient_gender VARCHAR(32), -- 性别快照
    severity_level VARCHAR(32), -- 严重程度
    route_type VARCHAR(32), -- 路由类型
    status VARCHAR(32) DEFAULT 'active', -- 会话状态
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 更新时间
);

-- ========================== TRIAGE TURN
CREATE TABLE IF NOT EXISTS triage_turn (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    session_id VARCHAR(64) NOT NULL, -- 会话ID
    turn_no INT NOT NULL, -- 轮次
    user_message TEXT, -- 用户输入
    normalized_query TEXT, -- 归一化查询
    intent VARCHAR(64), -- 意图
    stage VARCHAR(32), -- 阶段
    reply_text TEXT, -- 回复文本
    raw_decision_json TEXT, -- 决策轨迹JSON
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 创建时间
);

-- ========================== TRIAGE SLOT STATE
CREATE TABLE IF NOT EXISTS triage_slot_state (
    id BIGSERIAL PRIMARY KEY, -- 主键ID
    session_id VARCHAR(64) NOT NULL UNIQUE, -- 会话ID
    symptoms_json TEXT, -- 症状槽位JSON
    disease_name VARCHAR(255), -- 疾病名称
    target_hospital VARCHAR(255), -- 目标医院
    target_department VARCHAR(255), -- 目标科室
    target_doctor VARCHAR(255), -- 目标医生
    missing_slots_json TEXT, -- 缺失槽位JSON
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- 更新时间
);