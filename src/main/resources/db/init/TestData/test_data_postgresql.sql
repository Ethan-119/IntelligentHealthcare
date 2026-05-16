-- ============================================================
-- Intelligent Healthcare - PostgreSQL 测试数据（主数据库）
--
-- 架构说明：
--   PostgreSQL = 所有结构化业务数据（权威来源）
--     包含：patient、knowledge 模块（disease/hospital/dept/doctor/capability）、
--           triage 导诊会话、import 导入任务、audit 审计日志
--   MongoDB    = 仅 rag_document_chunks（文本块 + 1024 维向量，用于 RAG 语义检索）
--
-- 表按外键依赖顺序排列，先插入被引用表，再插入引用表
-- ============================================================

-- ========================== PATIENT 用户表 (15条)
-- 测试账号统一密码：12345678（BCrypt）
INSERT INTO patient (id, phone, username, password, status, role, patient_age, patient_gender, resident_city, area, triage_prefer, deleted) VALUES
(1,  '13800001001', '张伟',   '$2b$12$CmhLpq3SfxOt3NGJHY3ime65Dej.ux4TnMvm0pK2Er.KLDaIwZarC', 1, 'admin',   35, 'male',   '北京市', '东城区', 'nearby', 0),
(2,  '13800001002', '李娜',   '$2b$12$CmhLpq3SfxOt3NGJHY3ime65Dej.ux4TnMvm0pK2Er.KLDaIwZarC', 1, 'patient', 28, 'female', '上海市', '黄浦区', 'nearby', 0),
(3,  '13800001003', '王强',   '$2b$12$CmhLpq3SfxOt3NGJHY3ime65Dej.ux4TnMvm0pK2Er.KLDaIwZarC', 1, 'patient', 45, 'male',   '广州市', '越秀区', 'authority', 0),
(4,  '13800001004', '赵敏',   '$2b$12$CmhLpq3SfxOt3NGJHY3ime65Dej.ux4TnMvm0pK2Er.KLDaIwZarC', 1, 'patient', 52, 'female', '成都市', '武侯区', 'nearby', 0),
(5,  '13800001005', '刘洋',   '$2b$12$CmhLpq3SfxOt3NGJHY3ime65Dej.ux4TnMvm0pK2Er.KLDaIwZarC', 1, 'patient', 22, 'male',   '武汉市', '洪山区', 'nearby', 0),
(6,  '13800001006', '陈静',   '$2b$12$CmhLpq3SfxOt3NGJHY3ime65Dej.ux4TnMvm0pK2Er.KLDaIwZarC', 1, 'patient', 39, 'female', '北京市', '朝阳区', 'authority', 0),
(7,  '13800001007', '孙鹏',   '$2b$12$CmhLpq3SfxOt3NGJHY3ime65Dej.ux4TnMvm0pK2Er.KLDaIwZarC', 1, 'patient', 61, 'male',   '上海市', '浦东新区', 'nearby', 0),
(8,  '13800001008', '周婷',   '$2b$12$CmhLpq3SfxOt3NGJHY3ime65Dej.ux4TnMvm0pK2Er.KLDaIwZarC', 1, 'patient', 19, 'female', '广州市', '天河区', 'nearby', 0),
(9,  '13800001009', '吴磊',   '$2b$12$CmhLpq3SfxOt3NGJHY3ime65Dej.ux4TnMvm0pK2Er.KLDaIwZarC', 1, 'patient', 33, 'male',   '成都市', '锦江区', 'nearby', 0),
(10, '13800001010', '郑爽',   '$2b$12$CmhLpq3SfxOt3NGJHY3ime65Dej.ux4TnMvm0pK2Er.KLDaIwZarC', 1, 'patient', 27, 'female', '武汉市', '江汉区', 'authority', 0),
(11, '13800001011', '冯刚',   '$2b$12$CmhLpq3SfxOt3NGJHY3ime65Dej.ux4TnMvm0pK2Er.KLDaIwZarC', 1, 'patient', 48, 'male',   '北京市', '海淀区', 'nearby', 0),
(12, '13800001012', '蒋红',   '$2b$12$CmhLpq3SfxOt3NGJHY3ime65Dej.ux4TnMvm0pK2Er.KLDaIwZarC', 1, 'patient', 55, 'female', '上海市', '徐汇区', 'nearby', 0),
(13, '13800001013', '韩冰',   '$2b$12$CmhLpq3SfxOt3NGJHY3ime65Dej.ux4TnMvm0pK2Er.KLDaIwZarC', 1, 'patient', 16, 'male',   '广州市', '白云区', 'nearby', 0),
(14, '13800001014', '曹阳',   '$2b$12$CmhLpq3SfxOt3NGJHY3ime65Dej.ux4TnMvm0pK2Er.KLDaIwZarC', 1, 'patient', 42, 'male',   '成都市', '青羊区', 'nearby', 0),
(15, '13800001015', '许晴',   '$2b$12$CmhLpq3SfxOt3NGJHY3ime65Dej.ux4TnMvm0pK2Er.KLDaIwZarC', 1, 'patient', 30, 'female', '武汉市', '硚口区', 'authority', 0);
SELECT setval('patient_id_seq', 15);

-- ========================== HOSPITAL 医院表 (8家)
INSERT INTO hospital (id, hospital_id, hospital_name, city, district_name, latitude, longitude, hospital_level, is_emergency, authority_score, active_status, deleted) VALUES
(1,  'HOSP_001', '北京协和医院',                            '北京市', '东城区',  39.9145, 116.4074, '三级甲等', 1, 98.50, 1, 0),
(2,  'HOSP_002', '上海交通大学医学院附属瑞金医院',           '上海市', '黄浦区',  31.2304, 121.4737, '三级甲等', 1, 96.20, 1, 0),
(3,  'HOSP_003', '中山大学附属第一医院',                    '广州市', '越秀区',  23.1291, 113.2644, '三级甲等', 1, 94.80, 1, 0),
(4,  'HOSP_004', '四川大学华西医院',                        '成都市', '武侯区',  30.6394, 104.0668, '三级甲等', 1, 97.10, 1, 0),
(5,  'HOSP_005', '华中科技大学同济医学院附属同济医院',       '武汉市', '硚口区',  30.5801, 114.2631, '三级甲等', 1, 95.40, 1, 0),
(6,  'HOSP_006', '北京市海淀医院',                          '北京市', '海淀区',  39.9641, 116.2981, '三级乙等', 1, 82.30, 1, 0),
(7,  'HOSP_007', '上海市浦东新区人民医院',                   '上海市', '浦东新区', 31.2309, 121.5457, '二级甲等', 1, 76.50, 1, 0),
(8,  'HOSP_008', '广州市白云区第二人民医院',                 '广州市', '白云区',  23.1642, 113.2492, '二级甲等', 0, 68.90, 1, 0);
SELECT setval('hospital_id_seq', 8);

-- ========================== HOSPITAL DEPARTMENT 医院科室表 (30条)
INSERT INTO hospital_department (id, hospital_id, department_name, parent_department_name, department_intro, service_scope, active_status, deleted, gender_rule, age_min, age_max, crowd_tags_json, standard_dept_code, subspecialty_code, is_emergency, national_key_score, provincial_key_score, city_key_score, authority_score) VALUES
-- 北京协和医院
(1,  'HOSP_001', '心血管内科', '内科', '国家级重点学科，擅长各类心血管疾病的诊治', '冠心病、高血压、心力衰竭、心律失常等', 1, 0, 'all', 0, 200, '["老年人","慢性病"]',         'STD_CARD',   'SUB_CARD_INTERV',   1, 95.00, 98.00, 100.00, 97.50),
(2,  'HOSP_001', '神经内科',   '内科', '全国领先的神经系统疾病诊疗中心',           '脑卒中、癫痫、帕金森病、神经免疫疾病等', 1, 0, 'all', 0, 200, '["老年人","脑卒中高危"]',   'STD_NEURO',  'SUB_NEURO_CEREB',   1, 93.00, 96.00, 100.00, 95.00),
(3,  'HOSP_001', '骨科',       '外科', '骨科疑难重症诊疗中心',                     '关节置换、脊柱外科、创伤骨科、运动医学等', 1, 0, 'all', 18, 200, '["老年人","骨质疏松"]',     'STD_ORTHO',  'SUB_ORTHO_JOINT',   0, 90.00, 94.00, 100.00, 93.00),
(4,  'HOSP_001', '内分泌科',   '内科', '糖尿病与代谢疾病综合诊疗中心',              '糖尿病、甲状腺疾病、肥胖症等',           1, 0, 'all', 0, 200, '["糖尿病","代谢综合征"]',   'STD_ENDO',   'SUB_ENDO_DIAB',     0, 88.00, 92.00, 100.00, 91.00),
(5,  'HOSP_001', '妇产科',     '妇产科', '妇产科疾病综合诊疗中心',                  '妇科肿瘤、产科、生殖内分泌等',           1, 0, 'female', 18, 100, '["女性"]',                 'STD_OBGY',   'SUB_OBGY_GYNONC',   1, 85.00, 90.00, 100.00, 88.00),
-- 瑞金医院
(6,  'HOSP_002', '心血管内科', '内科', '心血管疾病国家重点学科',                  '高血压、冠心病、心肌病等',               1, 0, 'all', 0, 200, '["老年人","慢性病"]',         'STD_CARD',   'SUB_CARD_HTN',      1, 92.00, 96.00, 100.00, 95.00),
(7,  'HOSP_002', '呼吸内科',   '内科', '呼吸系统疾病国家重点学科',                  '慢阻肺、哮喘、肺部感染等',               1, 0, 'all', 0, 200, '["吸烟者","老年人"]',       'STD_RESPI',  'SUB_RESPI_COPD',    0, 88.00, 93.00, 100.00, 92.00),
(8,  'HOSP_002', '消化内科',   '内科', '消化系统疾病诊疗中心',                     '胃炎、肝硬化、消化道肿瘤等',             1, 0, 'all', 0, 200, '["慢性病","中老年人"]',     'STD_GASTRO', 'SUB_GASTRO_LIVER',  0, 86.00, 90.00, 100.00, 89.00),
(9,  'HOSP_002', '皮肤科',     '皮肤科', '皮肤疾病专科',                           '湿疹、银屑病、痤疮等',                   1, 0, 'all', 0, 200, NULL,                       'STD_DERM',   'SUB_DERM_ECZEMA',   0, 80.00, 85.00, 100.00, 83.00),
(10, 'HOSP_002', '儿科',       '儿科', '儿童疾病综合诊疗中心',                     '儿童呼吸、消化、生长发育等',             1, 0, 'all', 0, 14,  '["儿童"]',                 'STD_PED',    'SUB_PED_GROWTH',    1, 82.00, 88.00, 100.00, 86.00),
-- 中山一院
(11, 'HOSP_003', '神经内科',   '内科', '华南地区神经系统疾病诊疗中心',             '脑卒中、帕金森、神经免疫等',             1, 0, 'all', 0, 200, '["老年人","脑卒中高危"]',   'STD_NEURO',  'SUB_NEURO_PARK',    1, 90.00, 94.00, 100.00, 93.00),
(12, 'HOSP_003', '肾内科',     '内科', '肾脏疾病重点学科',                         '肾小球肾炎、肾功能不全、透析等',         1, 0, 'all', 0, 200, '["慢性肾病","糖尿病"]',     'STD_NEPHRO', 'SUB_NEPHRO_CKD',    0, 85.00, 90.00, 100.00, 90.00),
(13, 'HOSP_003', '普通外科',   '外科', '普外科疾病诊疗中心',                       '胆囊结石、阑尾炎、疝气等',               1, 0, 'all', 0, 200, NULL,                       'STD_GENSURG','SUB_GENSURG_GALL',  1, 82.00, 87.00, 100.00, 85.00),
(14, 'HOSP_003', '眼科',       '眼科', '眼科疾病诊疗中心',                         '白内障、青光眼、眼底病等',               1, 0, 'all', 0, 200, '["老年人"]',               'STD_OPHTH',  'SUB_OPHTH_CATARACT',0, 78.00, 84.00, 100.00, 82.00),
-- 华西医院
(15, 'HOSP_004', '呼吸内科',   '内科', '全国呼吸疾病重点学科',                     '慢阻肺、哮喘、间质性肺病等',             1, 0, 'all', 0, 200, '["吸烟者","老年人"]',       'STD_RESPI',  'SUB_RESPI_ASTHMA',  0, 94.00, 98.00, 100.00, 96.00),
(16, 'HOSP_004', '风湿免疫科', '内科', '西南地区风湿免疫疾病中心',                 '类风湿关节炎、狼疮、强直性脊柱炎等',     1, 0, 'all', 0, 200, '["中老年人","女性"]',       'STD_RHEUM',  'SUB_RHEUM_RA',      0, 88.00, 92.00, 100.00, 91.00),
(17, 'HOSP_004', '泌尿外科',   '外科', '泌尿系统疾病诊疗中心',                     '前列腺增生、泌尿系结石、肿瘤等',         1, 0, 'male', 18, 200, '["中老年男性"]',           'STD_URO',    'SUB_URO_BPH',       0, 84.00, 89.00, 100.00, 87.00),
(18, 'HOSP_004', '精神心理科', '精神科', '心理健康诊疗中心',                       '抑郁症、焦虑症、睡眠障碍等',             1, 0, 'all', 12, 200, '["青少年","职场人群"]',     'STD_PSYCH',  'SUB_PSYCH_DEPRESS', 0, 80.00, 85.00, 100.00, 83.00),
-- 同济医院
(19, 'HOSP_005', '心血管内科', '内科', '中部地区心血管疾病诊疗中心',               '冠心病、高血压、心力衰竭等',             1, 0, 'all', 0, 200, '["老年人","慢性病"]',         'STD_CARD',   'SUB_CARD_CAD',      1, 89.00, 93.00, 100.00, 92.00),
(20, 'HOSP_005', '耳鼻喉科',   '五官科', '耳鼻喉疾病专科',                         '过敏性鼻炎、中耳炎、喉部疾病等',         1, 0, 'all', 0, 200, NULL,                       'STD_ENT',    'SUB_ENT_RHINITIS',  0, 76.00, 82.00, 100.00, 80.00),
(21, 'HOSP_005', '骨科',       '外科', '骨科疾病诊疗中心',                         '脊柱外科、关节置换、运动创伤等',         1, 0, 'all', 0, 200, '["老年人","骨质疏松"]',     'STD_ORTHO',  'SUB_ORTHO_SPINE',   0, 85.00, 90.00, 100.00, 89.00),
(22, 'HOSP_005', '内分泌科',   '内科', '内分泌代谢疾病中心',                       '糖尿病、甲状腺、骨质疏松等',             1, 0, 'all', 0, 200, '["糖尿病","代谢综合征"]',   'STD_ENDO',   'SUB_ENDO_OSTEO',    0, 83.00, 88.00, 100.00, 86.00),
-- 海淀医院
(23, 'HOSP_006', '心血管内科', '内科', '心血管疾病专科',                           '高血压、冠心病等常见病诊治',             1, 0, 'all', 0, 200, '["老年人","慢性病"]',         'STD_CARD',   NULL,                1, 0, 65.00, 100.00, 70.00),
(24, 'HOSP_006', '儿科',       '儿科', '儿童常见病诊疗',                           '儿童感冒、肺炎、生长发育评估等',         1, 0, 'all', 0, 14,  '["儿童"]',                 'STD_PED',    NULL,                0, 0, 60.00, 100.00, 65.00),
-- 浦东新区人民医院
(25, 'HOSP_007', '妇产科',     '妇产科', '妇产科常见病诊疗',                       '产检、妇科炎症、子宫肌瘤等',             1, 0, 'female', 18, 100, '["女性"]',                 'STD_OBGY',   NULL,                1, 0, 55.00, 100.00, 60.00),
(26, 'HOSP_007', '急诊科',     '急诊科', '急诊抢救中心',                           '各类急危重症的初步处理',                 1, 0, 'all', 0, 200, NULL,                       'STD_EMERG',  NULL,                1, 0, 70.00, 100.00, 75.00),
-- 白云区第二人民医院
(27, 'HOSP_008', '消化内科',   '内科', '消化系统疾病专科',                         '胃病、肠炎、肝炎等常见病诊治',           1, 0, 'all', 0, 200, NULL,                       'STD_GASTRO', NULL,                0, 0, 50.00, 100.00, 55.00),
(28, 'HOSP_008', '口腔科',     '口腔科', '口腔疾病专科门诊',                       '龋齿、牙周病、口腔正畸等',               1, 0, 'all', 0, 200, NULL,                       'STD_DENT',   NULL,                0, 0, 45.00, 100.00, 50.00),
(29, 'HOSP_008', '皮肤科',     '皮肤科', '皮肤常见病门诊',                         '湿疹、荨麻疹、痤疮等',                   1, 0, 'all', 0, 200, '["中老年人","女性"]',       'STD_DERM',   NULL,                0, 0, 48.00, 100.00, 52.00),
(30, 'HOSP_008', '骨科',       '外科', '骨科常见病专科',                           '骨质疏松、骨折、颈肩腰腿痛等',           1, 0, 'all', 0, 200, '["老年人","骨质疏松"]',     'STD_ORTHO',  NULL,                0, 0, 52.00, 100.00, 58.00);
SELECT setval('hospital_department_id_seq', 30);

-- ========================== DISEASE MASTER 病种表 (25条)
INSERT INTO disease_master (id, disease_code, disease_name, symptom_keywords, gender_rule, age_min, age_max, urgency_level, review_status, deleted) VALUES
(1,  'DIS_001', '原发性高血压',           '头晕,头痛,心悸,眼花,耳鸣',                                 'all',    18, 200, 'medium', 'approved', 0),
(2,  'DIS_002', '2型糖尿病',               '多饮,多尿,多食,体重下降,乏力',                              'all',    18, 200, 'medium', 'approved', 0),
(3,  'DIS_003', '冠状动脉粥样硬化性心脏病', '胸痛,胸闷,心悸,气短,放射痛',                                'all',    40, 200, 'high',   'approved', 0),
(4,  'DIS_004', '脑梗死',                   '突发偏瘫,口眼歪斜,言语不清,肢体麻木,头晕',                  'all',    50, 200, 'critical','approved', 0),
(5,  'DIS_005', '社区获得性肺炎',           '发热,咳嗽,咳痰,胸痛,呼吸困难',                              'all',    0,  200, 'high',   'approved', 0),
(6,  'DIS_006', '支气管哮喘',               '喘息,呼吸困难,咳嗽,胸闷,夜间加重',                          'all',    0,  200, 'high',   'approved', 0),
(7,  'DIS_007', '慢性胃炎',                 '上腹痛,腹胀,嗳气,反酸,恶心',                                'all',    18, 200, 'low',    'approved', 0),
(8,  'DIS_008', '肝硬化',                   '乏力,腹胀,黄疸,蜘蛛痣,肝掌',                                'all',    30, 200, 'high',   'approved', 0),
(9,  'DIS_009', '慢性肾功能不全',           '乏力,水肿,尿少,食欲不振,恶心',                              'all',    30, 200, 'high',   'approved', 0),
(10, 'DIS_010', '甲状腺功能亢进症',         '心悸,手抖,多汗,体重下降,突眼,易激动',                       'all',    18, 100, 'medium', 'approved', 0),
(11, 'DIS_011', '类风湿关节炎',             '关节肿痛,晨僵,对称性关节炎,关节畸形',                       'all',    30, 150, 'medium', 'approved', 0),
(12, 'DIS_012', '骨质疏松症',               '骨痛,易骨折,身高变矮,驼背',                                 'all',    50, 200, 'medium', 'approved', 0),
(13, 'DIS_013', '抑郁症',                   '情绪低落,兴趣减退,睡眠障碍,食欲下降,疲乏',                  'all',    12, 200, 'medium', 'approved', 0),
(14, 'DIS_014', '湿疹',                     '皮肤瘙痒,红斑,丘疹,水疱,脱屑',                              'all',    0,  200, 'low',    'approved', 0),
(15, 'DIS_015', '过敏性鼻炎',               '鼻塞,流涕,喷嚏,鼻痒,眼痒',                                  'all',    0,  200, 'low',    'approved', 0),
(16, 'DIS_016', '急性阑尾炎',               '右下腹痛,恶心,呕吐,发热,麦氏点压痛',                        'all',    5,  200, 'critical','approved', 0),
(17, 'DIS_017', '胆囊结石',                 '右上腹痛,恶心,呕吐,黄疸,油腻食物后加重',                    'all',    18, 200, 'high',   'approved', 0),
(18, 'DIS_018', '前列腺增生',               '尿频,尿急,夜尿增多,排尿困难,尿流变细',                      'male',   50, 200, 'medium', 'approved', 0),
(19, 'DIS_019', '子宫肌瘤',                 '月经量增多,经期延长,下腹包块,压迫症状,贫血',                 'female', 20, 60,  'medium', 'approved', 0),
(20, 'DIS_020', '白内障',                   '视力下降,视物模糊,眼前黑影,眩光',                           'all',    50, 200, 'medium', 'approved', 0),
(21, 'DIS_021', '帕金森病',                 '静止性震颤,肌强直,运动迟缓,姿势步态异常',                   'all',    50, 200, 'medium', 'approved', 0),
(22, 'DIS_022', '银屑病',                   '皮肤红斑,鳞屑,瘙痒,关节痛,指甲改变',                        'all',    0,  200, 'medium', 'approved', 0),
(23, 'DIS_023', '痛风',                     '关节剧痛,红肿,发热,第一跖趾关节,夜间发作',                  'all',    18, 200, 'high',   'approved', 0),
(24, 'DIS_024', '焦虑症',                   '紧张不安,心悸,出汗,睡眠困难,注意力不集中',                  'all',    12, 200, 'medium', 'approved', 0),
(25, 'DIS_025', '腰椎间盘突出症',           '腰痛,下肢放射痛,麻木,肌力下降,直腿抬高试验阳性',            'all',    18, 200, 'medium', 'approved', 0);
SELECT setval('disease_master_id_seq', 25);

-- ========================== DISEASE ALIAS 病种别名表 (30条)
INSERT INTO disease_alias (id, disease_code, alias_name, alias_type, source) VALUES
(1,  'DIS_001', '高血压病',         'synonym',  'ICD-10'),
(2,  'DIS_001', 'HTN',              'acronym',  'WHO'),
(3,  'DIS_002', 'II型糖尿病',       'synonym',  'ICD-10'),
(4,  'DIS_002', '成人糖尿病',        'synonym',  '临床'),
(5,  'DIS_003', '冠心病',           'synonym',  'ICD-10'),
(6,  'DIS_003', '缺血性心脏病',      'synonym',  'WHO'),
(7,  'DIS_004', '中风',             'synonym',  '民间'),
(8,  'DIS_004', '脑血栓',           'synonym',  '民间'),
(9,  'DIS_006', '过敏性哮喘',        'synonym',  '临床'),
(10, 'DIS_006', '变异性哮喘',        'synonym',  '临床'),
(11, 'DIS_007', '胃病',             'synonym',  '民间'),
(12, 'DIS_007', '胃痛',             'symptom',  '民间'),
(13, 'DIS_008', '肝病',             'synonym',  '民间'),
(14, 'DIS_010', '甲亢病',           'synonym',  '民间'),
(15, 'DIS_011', '类风湿',           'abbrev',   '临床'),
(16, 'DIS_012', '骨质疏松',         'abbrev',   '临床'),
(17, 'DIS_013', '抑郁',             'abbrev',   '民间'),
(18, 'DIS_016', '盲肠炎',           'synonym',  '民间'),
(19, 'DIS_017', '胆结石',           'synonym',  '民间'),
(20, 'DIS_017', '胆石症',           'synonym',  'ICD-10'),
(21, 'DIS_018', '前列腺肥大',        'synonym',  '民间'),
(22, 'DIS_018', 'BPH',              'acronym',  'WHO'),
(23, 'DIS_020', '老年性白内障',      'synonym',  '临床'),
(24, 'DIS_021', '帕金森',           'abbrev',   '临床'),
(25, 'DIS_021', '震颤麻痹',         'synonym',  'ICD-10'),
(26, 'DIS_022', '牛皮癣',           'synonym',  '民间'),
(27, 'DIS_023', '尿酸高',           'synonym',  '民间'),
(28, 'DIS_024', '焦虑',             'abbrev',   '民间'),
(29, 'DIS_025', '腰间盘突出',        'synonym',  '民间'),
(30, 'DIS_025', '坐骨神经痛',        'symptom',  '民间');
SELECT setval('disease_alias_id_seq', 30);

-- ========================== MEDICAL CAPABILITY CATALOG 医疗能力目录 (25条)
INSERT INTO medical_capability_catalog (id, capability_code, capability_name, capability_type, parent_code, standard_dept_code, aliases_json, gender_rule, age_min, age_max, crowd_tags_json, pathway_tags_json, active_status) VALUES
(1,  'CAP_001', '心电图',                 'EXAM',    NULL,           'STD_CARD',   '["ECG","EKG"]',                           'all', 0, 200, NULL,                               '["心血管筛查","常规体检"]',         1),
(2,  'CAP_002', '心脏彩超',               'EXAM',    NULL,           'STD_CARD',   '["超声心动图","心脏超声"]',                'all', 0, 200, NULL,                               '["心血管诊断"]',                     1),
(3,  'CAP_003', '冠脉CTA',                'EXAM',    NULL,           'STD_CARD',   '["冠状动脉CT血管成像"]',                   'all', 18, 200, '["冠心病高危"]',                     '["冠心病诊断"]',                     1),
(4,  'CAP_004', '空腹血糖',               'LAB',     NULL,           'STD_ENDO',   '["FBG","GLU"]',                            'all', 0, 200, NULL,                               '["糖尿病筛查","常规体检"]',         1),
(5,  'CAP_005', '糖化血红蛋白',           'LAB',     NULL,           'STD_ENDO',   '["HbA1c"]',                                'all', 0, 200, '["糖尿病"]',                         '["糖尿病监测"]',                     1),
(6,  'CAP_006', '头部CT',                 'EXAM',    NULL,           'STD_NEURO',  '["头颅CT","脑CT"]',                        'all', 0, 200, NULL,                               '["脑血管病诊断","外伤"]',           1),
(7,  'CAP_007', '头部MRI',                'EXAM',    NULL,           'STD_NEURO',  '["头颅磁共振","脑MRI"]',                   'all', 0, 200, NULL,                               '["神经系统诊断"]',                   1),
(8,  'CAP_008', '胸部X线',                'EXAM',    NULL,           'STD_RESPI',  '["胸片","胸部平片"]',                      'all', 0, 200, NULL,                               '["呼吸系统筛查","常规体检"]',       1),
(9,  'CAP_009', '肺功能检查',             'EXAM',    NULL,           'STD_RESPI',  '["肺量计","PFT"]',                          'all', 5, 200, '["吸烟者","哮喘"]',                   '["呼吸功能评估"]',                   1),
(10, 'CAP_010', '胃镜检查',               'PROC',    NULL,           'STD_GASTRO', '["上消化道内镜","胃镜"]',                   'all', 18, 200, '["胃病患者"]',                        '["消化道诊断"]',                     1),
(11, 'CAP_011', '肝功能全套',             'LAB',     NULL,           'STD_GASTRO', '["肝功能","ALT,AST,GGT,TBIL"]',             'all', 0, 200, NULL,                               '["肝脏评估","常规体检"]',           1),
(12, 'CAP_012', '肾功能全套',             'LAB',     NULL,           'STD_NEPHRO', '["肾功能","BUN,Cr,eGFR"]',                  'all', 0, 200, NULL,                               '["肾脏评估","常规体检"]',           1),
(13, 'CAP_013', '甲状腺功能全套',         'LAB',     NULL,           'STD_ENDO',   '["甲功","TSH,T3,T4"]',                      'all', 0, 200, '["甲状腺疾病可疑"]',                 '["甲状腺评估"]',                     1),
(14, 'CAP_014', '类风湿因子检测',         'LAB',     NULL,           'STD_RHEUM',  '["RF","抗CCP抗体"]',                        'all', 0, 200, '["关节痛"]',                          '["风湿免疫筛查"]',                   1),
(15, 'CAP_015', '骨密度检测',             'EXAM',    NULL,           'STD_ORTHO',  '["BMD","DEXA"]',                            'all', 40, 200, '["绝经后女性","老年人"]',            '["骨质疏松筛查"]',                   1),
(16, 'CAP_016', '心理量表评估',           'ASSESS',  NULL,           'STD_PSYCH',  '["SCL-90","SDS","SAS"]',                    'all', 12, 200, NULL,                               '["心理健康评估"]',                   1),
(17, 'CAP_017', '过敏原检测',             'LAB',     NULL,           'STD_DERM',   '["过敏原筛查","变应原检测"]',               'all', 0, 200, '["过敏体质"]',                        '["过敏诊断"]',                       1),
(18, 'CAP_018', '腹部超声',               'EXAM',    NULL,           'STD_GASTRO', '["腹部B超","肝胆脾胰超声"]',               'all', 0, 200, NULL,                               '["腹部筛查","常规体检"]',           1),
(19, 'CAP_019', '前列腺特异性抗原',       'LAB',     NULL,           'STD_URO',    '["PSA"]',                                  'male', 50, 200, '["中老年男性"]',                      '["前列腺癌筛查"]',                   1),
(20, 'CAP_020', '妇科超声',               'EXAM',    NULL,           'STD_OBGY',   '["妇科B超","子宫附件超声"]',               'female', 0, 100, '["女性"]',                            '["妇科筛查"]',                       1),
(21, 'CAP_021', '眼底检查',               'EXAM',    NULL,           'STD_OPHTH',  '["眼底照相","OCT"]',                       'all', 0, 200, '["糖尿病","高血压"]',                 '["眼科筛查"]',                       1),
(22, 'CAP_022', '血常规',                 'LAB',     NULL,           NULL,         '["全血细胞计数","CBC"]',                   'all', 0, 200, NULL,                               '["常规体检","感染筛查"]',           1),
(23, 'CAP_023', '尿常规',                 'LAB',     NULL,           NULL,         '["尿液分析","尿检"]',                      'all', 0, 200, NULL,                               '["常规体检","泌尿系统筛查"]',       1),
(24, 'CAP_024', '动态血压监测',           'EXAM',    NULL,           'STD_CARD',   '["24小时血压","ABPM"]',                     'all', 18, 200, '["高血压"]',                          '["血压评估"]',                       1),
(25, 'CAP_025', '动态心电图',             'EXAM',    NULL,           'STD_CARD',   '["Holter","24小时心电图"]',                 'all', 0, 200, '["心悸","心律失常可疑"]',             '["心律失常诊断"]',                   1);
SELECT setval('medical_capability_catalog_id_seq', 25);

-- ========================== DISEASE CAPABILITY REL 病种-能力关联 (40条)
INSERT INTO disease_capability_rel (id, disease_code, capability_code, rel_type, priority_score, crowd_constraint, note) VALUES
(1,  'DIS_001', 'CAP_001', 'DIAGNOSIS',  9.00, NULL,        '高血压常规检查'),
(2,  'DIS_001', 'CAP_002', 'DIAGNOSIS',  8.50, NULL,        '评估心脏结构与功能'),
(3,  'DIS_001', 'CAP_024', 'MONITORING', 9.50, NULL,        '确诊高血压的重要手段'),
(4,  'DIS_002', 'CAP_004', 'DIAGNOSIS',  9.50, NULL,        '糖尿病诊断核心指标'),
(5,  'DIS_002', 'CAP_005', 'MONITORING', 10.00, NULL,       '血糖控制评估金标准'),
(6,  'DIS_002', 'CAP_021', 'SCREENING',  8.00, NULL,        '糖尿病视网膜病变筛查'),
(7,  'DIS_003', 'CAP_001', 'DIAGNOSIS',  9.00, NULL,        '心电图ST-T改变'),
(8,  'DIS_003', 'CAP_002', 'DIAGNOSIS',  9.00, NULL,        '评估室壁运动与心功能'),
(9,  'DIS_003', 'CAP_003', 'DIAGNOSIS',  9.50, NULL,        '冠脉狭窄评估金标准'),
(10, 'DIS_003', 'CAP_025', 'MONITORING', 8.50, NULL,        '心律失常监测'),
(11, 'DIS_004', 'CAP_006', 'DIAGNOSIS',  10.00, NULL,       '脑梗死首选影像检查'),
(12, 'DIS_004', 'CAP_007', 'DIAGNOSIS',  9.50, NULL,        '更敏感的脑梗死影像'),
(13, 'DIS_005', 'CAP_008', 'DIAGNOSIS',  9.00, NULL,        '肺炎首选影像'),
(14, 'DIS_005', 'CAP_022', 'SUPPORT',    8.50, NULL,        '感染指标评估'),
(15, 'DIS_006', 'CAP_009', 'DIAGNOSIS',  9.50, NULL,        '哮喘诊断与严重度评估'),
(16, 'DIS_006', 'CAP_017', 'ETIOLOGY',   8.00, '["过敏体质"]', '过敏原筛查'),
(17, 'DIS_007', 'CAP_010', 'DIAGNOSIS',  9.50, NULL,        '慢性胃炎确诊手段'),
(18, 'DIS_007', 'CAP_011', 'SUPPORT',    7.00, NULL,        '排除其他消化系统疾病'),
(19, 'DIS_008', 'CAP_011', 'MONITORING', 9.50, NULL,        '肝硬化肝功能评估'),
(20, 'DIS_008', 'CAP_018', 'DIAGNOSIS',  9.00, NULL,        '肝硬化影像学评估'),
(21, 'DIS_009', 'CAP_012', 'MONITORING', 10.00, NULL,       '肾功能评估核心指标'),
(22, 'DIS_009', 'CAP_023', 'SUPPORT',    8.00, NULL,        '尿液异常评估'),
(23, 'DIS_010', 'CAP_013', 'DIAGNOSIS',  10.00, NULL,       '甲亢确诊关键检查'),
(24, 'DIS_010', 'CAP_001', 'SUPPORT',    8.00, NULL,        '甲亢心脏影响评估'),
(25, 'DIS_011', 'CAP_014', 'DIAGNOSIS',  9.50, NULL,        '类风湿血清学检测'),
(26, 'DIS_012', 'CAP_015', 'DIAGNOSIS',  10.00, '["绝经后女性",">65"]', '骨密度诊断金标准'),
(27, 'DIS_013', 'CAP_016', 'DIAGNOSIS',  9.50, NULL,        '抑郁严重度评估'),
(28, 'DIS_014', 'CAP_017', 'ETIOLOGY',   8.50, '["过敏体质"]', '湿疹过敏原检测'),
(29, 'DIS_015', 'CAP_017', 'ETIOLOGY',   9.00, NULL,        '过敏性鼻炎过敏原筛查'),
(30, 'DIS_016', 'CAP_018', 'DIAGNOSIS',  9.00, NULL,        '阑尾炎超声诊断'),
(31, 'DIS_016', 'CAP_022', 'SUPPORT',    8.50, NULL,        '感染指标评估'),
(32, 'DIS_017', 'CAP_018', 'DIAGNOSIS',  9.50, NULL,        '胆囊结石首选影像检查'),
(33, 'DIS_018', 'CAP_019', 'SCREENING',  8.50, '["男性>50"]', '前列腺癌筛查'),
(34, 'DIS_018', 'CAP_023', 'SUPPORT',    7.50, '["男性"]',    '排尿异常辅助检查'),
(35, 'DIS_019', 'CAP_020', 'DIAGNOSIS',  9.50, '["女性"]',    '子宫肌瘤首选影像检查'),
(36, 'DIS_020', 'CAP_021', 'DIAGNOSIS',  9.00, NULL,        '白内障眼底评估'),
(37, 'DIS_021', 'CAP_007', 'DIAGNOSIS',  9.00, NULL,        '帕金森影像评估'),
(38, 'DIS_023', 'CAP_012', 'SUPPORT',    8.00, NULL,        '痛风肾功能评估'),
(39, 'DIS_025', 'CAP_006', 'DIAGNOSIS',  8.50, NULL,        '腰椎间盘CT检查'),
(40, 'DIS_025', 'CAP_007', 'DIAGNOSIS',  9.00, NULL,        '腰椎间盘MRI检查');
SELECT setval('disease_capability_rel_id_seq', 40);

-- ========================== DEPARTMENT CAPABILITY REL 科室-能力关联 (40条)
INSERT INTO department_capability_rel (id, department_id, capability_code, support_level, weight, source) VALUES
(1,  1,  'CAP_001', 'PRIMARY',  1.00, 'SYSTEM'),
(2,  1,  'CAP_002', 'PRIMARY',  1.00, 'SYSTEM'),
(3,  1,  'CAP_003', 'PRIMARY',  1.00, 'SYSTEM'),
(4,  1,  'CAP_024', 'PRIMARY',  0.90, 'SYSTEM'),
(5,  1,  'CAP_025', 'PRIMARY',  0.90, 'SYSTEM'),
(6,  4,  'CAP_004', 'PRIMARY',  1.00, 'SYSTEM'),
(7,  4,  'CAP_005', 'PRIMARY',  1.00, 'SYSTEM'),
(8,  2,  'CAP_006', 'PRIMARY',  1.00, 'SYSTEM'),
(9,  2,  'CAP_007', 'PRIMARY',  1.00, 'SYSTEM'),
(10, 7,  'CAP_008', 'SECONDARY',0.80, 'SYSTEM'),
(11, 7,  'CAP_009', 'PRIMARY',  1.00, 'SYSTEM'),
(12, 8,  'CAP_010', 'PRIMARY',  1.00, 'SYSTEM'),
(13, 8,  'CAP_011', 'PRIMARY',  1.00, 'SYSTEM'),
(14, 12, 'CAP_012', 'PRIMARY',  1.00, 'SYSTEM'),
(15, 4,  'CAP_013', 'PRIMARY',  1.00, 'SYSTEM'),
(16, 16, 'CAP_014', 'PRIMARY',  1.00, 'SYSTEM'),
(17, 3,  'CAP_015', 'PRIMARY',  1.00, 'SYSTEM'),
(18, 18, 'CAP_016', 'PRIMARY',  1.00, 'SYSTEM'),
(19, 9,  'CAP_017', 'PRIMARY',  1.00, 'SYSTEM'),
(20, 13, 'CAP_018', 'SECONDARY',0.80, 'SYSTEM'),
(21, 17, 'CAP_019', 'PRIMARY',  1.00, 'SYSTEM'),
(22, 5,  'CAP_020', 'PRIMARY',  1.00, 'SYSTEM'),
(23, 14, 'CAP_021', 'PRIMARY',  1.00, 'SYSTEM'),
(24, 1,  'CAP_022', 'SECONDARY',0.60, 'SYSTEM'),
(25, 1,  'CAP_023', 'SECONDARY',0.60, 'SYSTEM'),
(26, 6,  'CAP_001', 'PRIMARY',  1.00, 'SYSTEM'),
(27, 6,  'CAP_002', 'PRIMARY',  0.95, 'SYSTEM'),
(28, 11, 'CAP_006', 'PRIMARY',  1.00, 'SYSTEM'),
(29, 11, 'CAP_007', 'PRIMARY',  0.95, 'SYSTEM'),
(30, 15, 'CAP_009', 'PRIMARY',  1.00, 'SYSTEM'),
(31, 19, 'CAP_001', 'PRIMARY',  1.00, 'SYSTEM'),
(32, 19, 'CAP_002', 'SECONDARY',0.85, 'SYSTEM'),
(33, 19, 'CAP_025', 'PRIMARY',  0.90, 'SYSTEM'),
(34, 20, 'CAP_017', 'SECONDARY',0.80, 'SYSTEM'),
(35, 21, 'CAP_015', 'PRIMARY',  1.00, 'SYSTEM'),
(36, 22, 'CAP_004', 'PRIMARY',  1.00, 'SYSTEM'),
(37, 22, 'CAP_005', 'PRIMARY',  0.95, 'SYSTEM'),
(38, 22, 'CAP_013', 'SECONDARY',0.85, 'SYSTEM'),
(39, 23, 'CAP_001', 'PRIMARY',  1.00, 'SYSTEM'),
(40, 23, 'CAP_024', 'SECONDARY',0.75, 'SYSTEM');
SELECT setval('department_capability_rel_id_seq', 40);

-- ========================== DOCTOR PROFILE 医生信息表 (20条)
INSERT INTO doctor_profile (id, hospital_id, department_id, doctor_name, title, specialty_text, gender_rule, age_min, age_max, crowd_tags_json, authority_score, academic_title_score, is_expert, campus_name, active_status) VALUES
(1,  'HOSP_001', 1,  '陈建国', '主任医师', '冠心病介入治疗、高血压管理、心力衰竭综合治疗',                         'all', 18, 200, '["老年人","冠心病"]',     98.00, 95.00, 1, '协和医院东院', 1),
(2,  'HOSP_001', 1,  '李明华', '副主任医师', '心律失常射频消融、起搏器植入',                                         'all', 18, 200, '["心律失常"]',           85.00, 80.00, 1, '协和医院东院', 1),
(3,  'HOSP_001', 2,  '王志强', '主任医师', '脑血管病介入治疗、帕金森病综合管理',                                   'all', 40, 200, '["脑卒中","老年人"]',     95.00, 92.00, 1, '协和医院东院', 1),
(4,  'HOSP_001', 3,  '张永康', '主任医师', '关节置换、脊柱微创手术',                                                 'all', 18, 200, '["骨质疏松","老年人"]',   90.00, 88.00, 1, '协和医院东院', 1),
(5,  'HOSP_001', 4,  '刘芳',   '主任医师', '糖尿病综合管理、甲状腺疾病诊治',                                         'all', 18, 200, '["糖尿病","代谢综合征"]', 92.00, 90.00, 1, '协和医院东院', 1),
(6,  'HOSP_002', 6,  '赵明辉', '主任医师', '复杂冠心病介入、结构性心脏病',                                           'all', 18, 200, '["冠心病","老年人"]',     96.00, 94.00, 1, '瑞金医院总院', 1),
(7,  'HOSP_002', 7,  '孙文博', '主任医师', '慢阻肺康复管理、支气管镜介入治疗',                                       'all', 18, 200, '["吸烟者","慢阻肺"]',     88.00, 85.00, 1, '瑞金医院总院', 1),
(8,  'HOSP_002', 8,  '周建华', '副主任医师', '慢性肝病管理、消化道早癌筛查',                                         'all', 18, 200, '["肝病","中老年人"]',     82.00, 78.00, 0, '瑞金医院总院', 1),
(9,  'HOSP_002', 9,  '吴敏',   '主任医师', '湿疹、银屑病、痤疮综合治疗',                                             'all', 0,  200, '["女性","儿童"]',         78.00, 75.00, 0, '瑞金医院总院', 1),
(10, 'HOSP_003', 11, '郑晓峰', '主任医师', '脑卒中急救、神经介入治疗',                                               'all', 40, 200, '["脑卒中","老年人"]',     93.00, 90.00, 1, '中山一院院本部', 1),
(11, 'HOSP_003', 12, '冯志远', '主任医师', '慢性肾病管理、腹膜透析、血液透析',                                       'all', 18, 200, '["慢性肾病","糖尿病"]',   86.00, 83.00, 1, '中山一院院本部', 1),
(12, 'HOSP_003', 14, '蒋海燕', '副主任医师', '白内障超声乳化手术、青光眼诊疗',                                       'all', 40, 200, '["老年人"]',              80.00, 76.00, 0, '中山一院院本部', 1),
(13, 'HOSP_004', 15, '韩志强', '主任医师', '间质性肺病、哮喘综合管理、呼吸危重症',                                   'all', 18, 200, '["吸烟者","哮喘"]',       94.00, 92.00, 1, '华西医院主院区', 1),
(14, 'HOSP_004', 16, '曹丽华', '主任医师', '类风湿关节炎生物制剂治疗、狼疮综合管理',                                 'all', 18, 200, '["中老年女性","自身免疫"]',89.00, 87.00, 1, '华西医院主院区', 1),
(15, 'HOSP_004', 18, '许文婷', '副主任医师', '抑郁症、焦虑症认知行为治疗、青少年心理健康',                             'all', 12, 200, '["青少年","职场人群"]',   75.00, 72.00, 0, '华西医院主院区', 1),
(16, 'HOSP_005', 19, '沈志刚', '主任医师', '冠心病介入、心力衰竭综合管理',                                           'all', 40, 200, '["冠心病","老年人"]',     91.00, 88.00, 1, '同济医院主院区', 1),
(17, 'HOSP_005', 20, '彭艳',   '主任医师', '过敏性鼻炎免疫治疗、鼻内镜手术',                                         'all', 0,  200, NULL,                      77.00, 73.00, 0, '同济医院主院区', 1),
(18, 'HOSP_005', 22, '吕明',   '副主任医师', '糖尿病教育管理、骨质疏松综合治疗',                                     'all', 18, 200, '["糖尿病","骨质疏松"]',   79.00, 75.00, 0, '同济医院主院区', 1),
(19, 'HOSP_006', 23, '魏红',   '主任医师', '高血压社区管理、心血管慢病预防',                                         'all', 18, 200, '["老年人","慢性病"]',     72.00, 68.00, 0, '海淀医院', 1),
(20, 'HOSP_007', 25, '田丽',   '主任医师', '高危妊娠管理、妇科微创手术',                                             'female', 18, 100, '["孕产妇","女性"]',       74.00, 70.00, 0, '浦东新区人民医院', 1);
SELECT setval('doctor_profile_id_seq', 20);

-- ========================== DOCTOR CAPABILITY REL 医生-能力关联 (25条)
INSERT INTO doctor_capability_rel (id, doctor_id, capability_code, weight) VALUES
(1,  1,  'CAP_003', 0.95),
(2,  1,  'CAP_002', 0.90),
(3,  2,  'CAP_025', 0.95),
(4,  3,  'CAP_006', 0.95),
(5,  3,  'CAP_007', 0.90),
(6,  4,  'CAP_015', 0.80),
(7,  5,  'CAP_004', 0.90),
(8,  5,  'CAP_005', 0.95),
(9,  6,  'CAP_003', 0.95),
(10, 6,  'CAP_001', 0.85),
(11, 7,  'CAP_009', 0.90),
(12, 8,  'CAP_010', 0.90),
(13, 8,  'CAP_011', 0.85),
(14, 10, 'CAP_006', 0.95),
(15, 11, 'CAP_012', 0.95),
(16, 12, 'CAP_021', 0.80),
(17, 13, 'CAP_009', 0.90),
(18, 14, 'CAP_014', 0.95),
(19, 15, 'CAP_016', 0.85),
(20, 16, 'CAP_003', 0.85),
(21, 16, 'CAP_001', 0.90),
(22, 17, 'CAP_017', 0.80),
(23, 18, 'CAP_004', 0.85),
(24, 18, 'CAP_005', 0.85),
(25, 18, 'CAP_015', 0.80);
SELECT setval('doctor_capability_rel_id_seq', 25);

-- ========================== IMPORT JOB RECORD 导入任务记录 (5条)
INSERT INTO import_job_record (id, dataset_type, file_name, status, success_count, failure_count, review_count, auto_mapped_count, message) VALUES
(1, 'HOSPITAL',     '北京地区医院数据_2026Q1.xlsx',     'COMPLETED', 15, 2,  3,  12, '导入完成：15条成功，2条失败（科室编码不匹配），3条待审核'),
(2, 'DISEASE',      'ICD10疾病分类_导入模板.csv',       'COMPLETED', 200, 5, 10, 180, '导入完成：200条成功，5条失败（年龄范围格式错误），10条待确认'),
(3, 'DOCTOR',       '华西医院医生信息_2026.xlsx',       'COMPLETED', 48, 0,  5,  40, '导入完成：48条成功，5条待审核（职称信息待确认）'),
(4, 'DEPARTMENT',   '上海三甲医院科室数据.csv',         'REVIEWING', 30, 3,  8,  25, '待审核：8条科室名称与标准编码差异较大需人工确认'),
(5, 'CAPABILITY',   '医疗能力目录_全量导入_2026.xlsx',  'FAILED',    0,  50, 0,  0,  '导入失败：文件格式校验失败，缺少capability_code必填列');
SELECT setval('import_job_record_id_seq', 5);

-- ========================== IMPORT FAILURE LOG 导入失败日志 (6条)
INSERT INTO import_failure_log (id, job_id, row_number, raw_content, error_message) VALUES
(1, 1, 3,  '{"hospital_name":"北京仁爱医院","department":"心内科"}',  '科室编码 STD_CARDX 不存在于标准编码库'),
(2, 1, 18, '{"hospital_name":"北京仁爱医院","department":"神内"}',   'hospital_id 为空，无法关联'),
(3, 2, 45, '{"disease_name":"急性上呼吸道感染","age_min":"成人"}',   'age_min 值格式错误，期望整型，实为文本'),
(4, 2, 102,'{"disease_name":"2型糖尿病","symptom_keywords":""}',    'symptom_keywords 为空'),
(5, 4, 7,  '{"hospital_name":"瑞金医院-东院","department":"综合内科"}', '综合内科 匹配到多个标准科室编码，需人工选择'),
(6, 4, 15, '{"hospital_name":"华山医院","department":"老年病科"}',     '老年病科 未找到匹配的标准科室编码');
SELECT setval('import_failure_log_id_seq', 6);

-- ========================== IMPORT REVIEW ITEM 导入待审核项 (6条)
INSERT INTO import_review_item (id, job_id, item_key, issue_type, raw_content, suggestion, resolved, resolution_note) VALUES
(1, 2, 'DIS_MIGRAINE',  'AMBIGUOUS_NAME', '{"disease_name":"偏头痛","aliases":["头疼"]}',  '别名"头疼"范围过宽，建议改为"偏侧头痛"', 0, NULL),
(2, 2, 'DIS_IRON_DEF',  'MISSING_FIELD',  '{"disease_name":"缺铁性贫血","age_min":null}',  '建议补充适用年龄范围',                    1, '已补充：age_min=0, age_max=200'),
(3, 3, 'DOC_ZHANG_SAN', 'UNCERTAIN_TITLE', '{"doctor_name":"张三","title":"主治/副主任（待确认）"}', '职称信息不明确，请确认实际职称',           0, NULL),
(4, 3, 'DOC_LI_SI',     'UNCERTAIN_DEPT',  '{"doctor_name":"李四","department":"综合科"}',          '"综合科"跨度较大，建议细化至具体专科',     1, '已确认为心内科'),
(5, 4, 'DEPT_GERIATRIC','NEW_ENTRY',       '{"department_name":"老年病科","hospital":"华山医院"}',   '标准库中暂无对应编码，建议新建 STD_GERIATRIC', 0, NULL),
(6, 4, 'DEPT_PAIN',     'NEW_ENTRY',       '{"department_name":"疼痛科","hospital":"瑞金医院"}',     '标准库中暂无对应编码，建议新建 STD_PAIN',       0, NULL);
SELECT setval('import_review_item_id_seq', 6);

-- ========================== AI RECALL AUDIT LOG AI召回审计日志 (8条)
INSERT INTO ai_recall_audit_log (id, symptoms, gender, age, age_group, eligible_disease_count, rule_candidate_codes_json, suggested_codes_json, status, message) VALUES
(1, '头痛,头晕,心悸', 'male', 55, 'elder', 5,
 '["DIS_001","DIS_003","DIS_004","DIS_010","DIS_024"]',
 '["DIS_001","DIS_003","DIS_024"]',
 'SUCCESS', '召回成功：规则候选5个，AI精选3个，准确率良好'),
(2, '胸痛,胸闷,气短,放射痛', 'male', 62, 'elder', 4,
 '["DIS_003","DIS_005","DIS_024","DIS_001"]',
 '["DIS_003","DIS_001"]',
 'SUCCESS', '召回成功：症状与冠心病高度匹配'),
(3, '多饮,多尿,体重下降,乏力', 'female', 45, 'adult', 5,
 '["DIS_002","DIS_010","DIS_009","DIS_013","DIS_024"]',
 '["DIS_002","DIS_010"]',
 'SUCCESS', '召回成功：建议进一步血糖及甲功检查'),
(4, '咳嗽,发热,胸痛,呼吸困难', 'male', 8, 'child', 3,
 '["DIS_005","DIS_006","DIS_015"]',
 '["DIS_005","DIS_006"]',
 'SUCCESS', '召回成功：儿童肺炎与哮喘待鉴别'),
(5, '关节肿痛,晨僵,对称性', 'female', 52, 'elder', 4,
 '["DIS_011","DIS_012","DIS_023","DIS_025"]',
 '["DIS_011","DIS_023"]',
 'SUCCESS', '召回成功：类风湿关节炎可能性较大'),
(6, '情绪低落,睡眠障碍,食欲下降', 'female', 28, 'adult', 4,
 '["DIS_013","DIS_024","DIS_010","DIS_003"]',
 '["DIS_013","DIS_024"]',
 'SUCCESS', '召回成功：抑郁与焦虑待评估'),
(7, '下腹痛,发热,恶心', 'female', 25, 'adult', 6,
 '["DIS_016","DIS_019","DIS_005","DIS_007","DIS_017","DIS_009"]',
 '["DIS_016","DIS_019"]',
 'PARTIAL', '部分命中：需排除妇科急症与阑尾炎'),
(8, '', 'male', 35, 'adult', 0,
 '[]',
 '[]',
 'EMPTY_INPUT', '输入症状为空，无法进行AI召回');
SELECT setval('ai_recall_audit_log_id_seq', 8);

-- ========================== TRIAGE SESSION 导诊会话 (6条)
INSERT INTO triage_session (id, session_id, user_id, dialog_id, current_stage, ask_round, invalid_answer_count, city, area, nearby, latitude, longitude, patient_age, patient_gender, severity_level, route_type, status) VALUES
(1, 'SESS_001', '1',  'DIALOG_001', 'DISEASE_DONE',   5, 0, '北京市', '东城区',  1, 39.9145, 116.4074, 35, 'male',   'medium', 'DEPARTMENT', 'completed'),
(2, 'SESS_002', '2',  'DIALOG_002', 'SLOT_FILLING',   3, 0, '上海市', '黄浦区',  1, 31.2304, 121.4737, 28, 'female', 'low',    'DISEASE',     'active'),
(3, 'SESS_003', '3',  'DIALOG_003', 'COMPLETED',      7, 1, '广州市', '越秀区',  1, 23.1291, 113.2644, 45, 'male',   'high',   'HOSPITAL',    'completed'),
(4, 'SESS_004', '7',  'DIALOG_004', 'DISEASE_DONE',   4, 0, '上海市', '浦东新区', 1, 31.2309, 121.5457, 61, 'male',   'high',   'EMERGENCY',   'completed'),
(5, 'SESS_005', '12', 'DIALOG_005', 'SYMPTOM_INPUT',  2, 0, '上海市', '徐汇区',  1, 31.1886, 121.4384, 55, 'female', 'medium', 'DISEASE',     'active'),
(6, 'SESS_006', '15', 'DIALOG_006', 'COMPLETED',      6, 2, '武汉市', '硚口区',  1, 30.5801, 114.2631, 30, 'female', 'low',    'DEPARTMENT',  'completed');
SELECT setval('triage_session_id_seq', 6);

-- ========================== TRIAGE TURN 导诊对话轮次 (25条)
INSERT INTO triage_turn (id, session_id, turn_no, user_message, normalized_query, intent, stage, reply_text, raw_decision_json) VALUES
-- SESS_001: 张伟 — 头痛、头晕、心悸 → 高血压
(1,  'SESS_001', 1, '我最近总是头疼头晕',                       '头痛 头晕',                         'SYMPTOM_REPORT', 'SYMPTOM_INPUT',  '您好，我是AI导诊助手。请问您的症状持续多久了？除了头痛头晕，还有其他不适吗？', '{"stage":"SYMPTOM_INPUT","slots":{"symptoms":["头痛","头晕"]}}'),
(2,  'SESS_001', 2, '有一个多月了，有时候还会心慌',              '头痛 头晕 心悸 持续1月',             'SYMPTOM_REPORT', 'SYMPTOM_INPUT',  '了解了。为了更好地为您分析，请问您的年龄和性别是？',                             '{"stage":"SYMPTOM_INPUT","slots":{"symptoms":["头痛","头晕","心悸"],"duration":"1月"}}'),
(3,  'SESS_001', 3, '我今年35岁，男的',                         '年龄35 性别男',                      'PROFILE_INFO',   'SLOT_FILLING',  '好的，已记录。根据您的症状和基本信息，为您分析可能的疾病方向...',                  '{"stage":"SLOT_FILLING","slots":{"age":35,"gender":"male"}}'),
(4,  'SESS_001', 4, NULL,                                       NULL,                                'DISEASE_SUGGEST','DISEASE_DONE',  '根据您的症状（头痛、头晕、心悸），可能涉及：\n1. 原发性高血压\n2. 冠心病\n建议您到心血管内科就诊检查。', '{"suggested":["DIS_001","DIS_003"],"confidence":0.87}'),
(5,  'SESS_001', 5, '好的，帮我推荐附近的医院',                  '推荐附近医院 心内科',                 'HOSPITAL_QUERY', 'DEPARTMENT',   '为您找到以下附近医院的心血管内科：\n1. 北京协和医院（东城区）— 国家临床重点专科\n2. 北京市海淀医院（海淀区）', '{"recommended_hospitals":[{"id":"HOSP_001","dept":"心血管内科","score":97.5}]}'),

-- SESS_002: 李娜 — 皮肤问题
(6,  'SESS_002', 1, '我手上长了红疹子，很痒',                   '手部 红疹 瘙痒',                      'SYMPTOM_REPORT', 'SYMPTOM_INPUT',  '您好，请问红疹出现多久了？有没有接触过什么特殊的东西？',                        '{"stage":"SYMPTOM_INPUT","slots":{"symptoms":["红疹","瘙痒"],"location":"手部"}}'),
(7,  'SESS_002', 2, '大概两周了，涂了药膏好一点但反复',          '手部红疹 瘙痒 2周 反复',              'SYMPTOM_REPORT', 'SYMPTOM_INPUT',  '了解了。请问您有没有过敏史？最近是否接触了新的化妆品或洗涤用品？',               '{"stage":"SYMPTOM_INPUT","slots":{"symptoms":["红疹","瘙痒"],"duration":"2周"}}'),
(8,  'SESS_002', 3, '没有特别的过敏史，但最近换了洗手液',        '无过敏史 换洗手液',                    'PROFILE_INFO',   'SLOT_FILLING',  '好的，可能与接触性刺激有关。让我为您分析一下...',                               '{"stage":"SLOT_FILLING","slots":{"allergy":"none","trigger":"洗手液"}}'),

-- SESS_003: 王强 — 胸痛→冠心病
(9,  'SESS_003', 1, '我胸口疼得厉害',                           '胸痛 剧烈',                           'SYMPTOM_REPORT', 'SYMPTOM_INPUT',  '胸痛需要重视！请问疼痛是什么样的感觉？是闷痛、刺痛还是绞痛？疼痛会向其他部位放射吗？', '{"stage":"SYMPTOM_INPUT","slots":{"symptoms":["胸痛"],"severity":"severe"}}'),
(10, 'SESS_003', 2, '闷痛，有时候会串到左手臂',                  '胸痛 闷痛 左手臂放射',                'SYMPTOM_REPORT', 'SYMPTOM_INPUT',  '请问您多大年龄？这种情况持续多久了？有没有高血压或糖尿病史？',                    '{"stage":"SYMPTOM_INPUT","slots":{"symptoms":["胸痛","闷痛","放射痛"],"radiation":"左臂"}}'),
(11, 'SESS_003', 3, '45岁，有高血压，最近半年偶尔发作',          '年龄45 高血压史 胸痛半年',             'PROFILE_INFO',   'SLOT_FILLING',  '请稍等，您的症状需要认真评估...',                                                 '{"stage":"SLOT_FILLING","slots":{"age":45,"history":"高血压"}}'),
(12, 'SESS_003', 4, '会不会是心脏病？',                          '胸痛 心脏病 担心',                     'DISEASE_QUERY',  'SLOT_FILLING',  '根据您的症状，确实需要排除冠心病的可能。建议尽快到心内科就诊检查。',              '{"stage":"SLOT_FILLING","candidate":"DIS_003"}'),
(13, 'SESS_003', 5, '什么医院最好？',                            '推荐 心血管 权威医院',                'HOSPITAL_QUERY', 'DISEASE_DONE',  '为您推荐广州市心血管内科权威医院：\n1. 中山大学附属第一医院（越秀区）',         '{"recommended":[{"hospital":"HOSP_003","dept":"神经内科"}]}'),
(14, 'SESS_003', 6, NULL,                                       NULL,                                 'HOSPITAL_CHOOSE','HOSPITAL',     '已为您定位越秀区中山大学附属第一医院，该院心内科为国家重点专科。',                 '{"stage":"HOSPITAL","selected":"HOSP_003"}'),
(15, 'SESS_003', 7, '帮我挂号',                                  '挂号 预约',                           'APPOINTMENT',    'COMPLETED',    '好的，已为您生成就诊建议。请注意：如胸痛加剧或持续不缓解，请立即拨打120急救！',   '{"stage":"COMPLETED","final_diagnosis":"DIS_003"}'),

-- SESS_004: 孙鹏 — 脑卒中
(16, 'SESS_004', 1, '我爸突然说话不清楚了，嘴巴有点歪',          '老年人 言语不清 口眼歪斜 突发',        'SYMPTOM_REPORT', 'SYMPTOM_INPUT', '这是紧急情况！请立即拨打120或前往最近医院的急诊科！这可能是脑卒中的征兆！',      '{"stage":"SYMPTOM_INPUT","severity":"critical","alert":"STROKE_WARNING"}'),
(17, 'SESS_004', 2, '他61岁，有高血压，会不会是中风？',          '61岁 高血压史 疑似中风',               'PROFILE_INFO',   'SLOT_FILLING',  '根据症状描述（突发言语不清、口角歪斜），高度怀疑急性脑卒中。请立刻就医！每延误一分钟，脑细胞都在受损。', '{"stage":"DISEASE_DONE","suggested":["DIS_004"],"urgency":"critical"}'),
(18, 'SESS_004', 3, '附近有什么医院可以急救？',                  '附近 急诊 医院',                       'HOSPITAL_QUERY', 'EMERGENCY',    '为您紧急定位附近急诊医院：\n1. 上海市浦东新区人民医院 — 急诊科\n请立即前往！', '{"recommended":[{"hospital":"HOSP_007","dept":"急诊科","type":"EMERGENCY"}]}'),
(19, 'SESS_004', 4, '好，马上去',                                '确认 立即就医',                        'CONFIRM',        'COMPLETED',    '请务必尽快！路上注意安全，到达医院后直接去急诊科。',                               '{"stage":"COMPLETED","user_action":"EMERGENCY_DEPART"}'),

-- SESS_005: 蒋红 — 关节痛
(20, 'SESS_005', 1, '我最近手指关节疼，早上起来特别僵硬',       '手指关节痛 晨僵',                      'SYMPTOM_REPORT', 'SYMPTOM_INPUT',  '您好，请问晨僵大概持续多长时间？关节有没有红肿？',                               '{"stage":"SYMPTOM_INPUT","slots":{"symptoms":["关节痛","晨僵"],"location":"手指"}}'),
(21, 'SESS_005', 2, '大概半小时到一小时，手指有点肿',            '晨僵30-60分钟 手指肿胀',              'SYMPTOM_REPORT', 'SYMPTOM_INPUT',  '了解了。根据您描述的对称性关节肿痛伴晨僵，需要考虑类风湿关节炎的可能...',        '{"stage":"SYMPTOM_INPUT","slots":{"symptoms":["关节痛","晨僵","肿胀"]}}'),

-- SESS_006: 许晴 — 情绪问题
(22, 'SESS_006', 1, '我最近心情很差，睡不着觉',                  '情绪低落 失眠',                        'SYMPTOM_REPORT', 'SYMPTOM_INPUT',  '您好，感谢您的信任。请问这种情况持续多久了？是否影响了您的日常工作和生活？',     '{"stage":"SYMPTOM_INPUT","slots":{"symptoms":["情绪低落","失眠"]}}'),
(23, 'SESS_006', 2, '快两个月了，对什么都没兴趣，也不想吃东西',  '情绪低落 兴趣减退 食欲下降 2月',       'SYMPTOM_REPORT', 'SYMPTOM_INPUT',  '您的描述让我担心，这可能是抑郁发作的表现。请问您最近有没有想伤害自己的想法？',   '{"stage":"SYMPTOM_INPUT","slots":{"symptoms":["情绪低落","兴趣减退","食欲下降"],"duration":"2月"},"alert":"DEPRESSION_SCREEN"}'),
(24, 'SESS_006', 3, '没有自杀的想法，就是觉得生活没意思',        '无自杀意念 生活无趣',                   'SAFETY_CHECK',   'SLOT_FILLING',  '好的，安全第一。根据您的症状（情绪低落、兴趣减退、睡眠困难、食欲下降，持续2月），高度符合抑郁发作的诊断标准。', '{"stage":"SLOT_FILLING","safety":"no_self_harm"}'),
(25, 'SESS_006', 4, NULL,                                       NULL,                                  'DISEASE_SUGGEST','DISEASE_DONE',  '为您推荐以下方向：\n1. 抑郁症（MDD）\n建议到精神心理科进行专业评估。\n同时注意规律作息，适当运动，保持社交联系。', '{"suggested":["DIS_013"],"recommended_dept":"精神心理科"}'),
(26, 'SESS_006', 5, '那我应该去哪个医院看？',                    '推荐 精神心理科 医院',                  'HOSPITAL_QUERY', 'DEPARTMENT',   '为您推荐：\n1. 四川大学华西医院 — 精神心理科\n2. 武汉市同济医院 — 心理咨询中心', '{"recommended":[{"hospital":"HOSP_004","dept":"精神心理科"}]}'),
(27, 'SESS_006', 6, '华西医院吧，帮我预约',                      '选择 华西医院 精神心理科',              'APPOINTMENT',    'COMPLETED',    '好的，已为您生成就诊建议。心理健康同样重要，及时寻求专业帮助是最明智的选择！',    '{"stage":"COMPLETED","selected":{"hospital":"HOSP_004","dept":"精神心理科"},"suggested":["DIS_013"]}');
SELECT setval('triage_turn_id_seq', 27);

-- ========================== TRIAGE SLOT STATE 导诊槽位状态 (6条)
INSERT INTO triage_slot_state (id, session_id, symptoms_json, disease_name, target_hospital, target_department, target_doctor, missing_slots_json) VALUES
(1, 'SESS_001', '["头痛","头晕","心悸"]',            '原发性高血压',          '北京协和医院',          '心血管内科', NULL,      '[]'),
(2, 'SESS_002', '["红疹","瘙痒"]',                    NULL,                    NULL,                    NULL,         NULL,      '["disease_name","target_hospital","target_department"]'),
(3, 'SESS_003', '["胸痛","闷痛","放射痛"]',           '冠状动脉粥样硬化性心脏病','中山大学附属第一医院',   '神经内科',   '郑晓峰',  '[]'),
(4, 'SESS_004', '["言语不清","口眼歪斜"]',            '脑梗死',                 '上海市浦东新区人民医院', '急诊科',     NULL,      '[]'),
(5, 'SESS_005', '["关节痛","晨僵","肿胀"]',           NULL,                    NULL,                    NULL,         NULL,      '["disease_name","target_hospital","target_department"]'),
(6, 'SESS_006', '["情绪低落","失眠","兴趣减退","食欲下降"]', '抑郁症',           '四川大学华西医院',       '精神心理科', '许文婷',  '[]');
SELECT setval('triage_slot_state_id_seq', 6);
