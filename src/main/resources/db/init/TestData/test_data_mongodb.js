// ============================================================
// Intelligent Healthcare - MongoDB 测试数据（向量数据库）
//
// 架构说明：
//   PostgreSQL = 所有结构化业务数据（权威来源，含 knowledge 模块）
//   MongoDB    = 仅 rag_document_chunks（文本块 + 1024 维向量，用于 RAG 语义检索）
//
// 执行方式：mongosh intelligent_healthcare test_data_mongodb.js
// 注意：embedding 字段为 null 占位，实际需通过 text-embedding-v3 模型生成 1024 维向量后回填
// ============================================================

const db = connect("mongodb://localhost:27017/intelligent_healthcare");

function createEmbedding(seed) {
  const values = [];
  for (let i = 0; i < 1024; i++) {
    values.push(((seed * 131 + i) % 1000) / 1000);
  }
  return values;
}

// 清空已有测试数据（可选，按需取消注释）
// db.rag_document_chunks.deleteMany({ source_id: { $regex: /^TEST_/ } });

// ========================== KNOWLEDGE 类型 — 病种知识 (25条)
db.rag_document_chunks.insertMany([
  // ---- 病种知识 ----
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_001",
    chunk_key: "default",
    content: "原发性高血压（Hypertension）是以体循环动脉血压升高为主要特征的心血管综合征。诊断标准：在未使用降压药的情况下，非同日3次测量，收缩压≥140mmHg和/或舒张压≥90mmHg。常见症状包括头晕、头痛、心悸、眼花、耳鸣等。危险因素包括高盐饮食、肥胖、吸烟、过量饮酒、精神紧张等。治疗包括生活方式干预（限盐≤6g/日、减重、运动）和药物治疗（ACEI、ARB、CCB、利尿剂、β受体阻滞剂等五大类）。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_002",
    chunk_key: "default",
    content: "2型糖尿病（Type 2 Diabetes Mellitus）是由于胰岛素抵抗伴胰岛素分泌相对不足导致的以高血糖为特征的代谢性疾病。典型症状为「三多一少」：多饮、多尿、多食、体重下降，常伴乏力。诊断标准：空腹血糖≥7.0mmol/L，或OGTT 2h血糖≥11.1mmol/L，或HbA1c≥6.5%。慢性并发症包括糖尿病肾病、视网膜病变、周围神经病变、心脑血管疾病、糖尿病足等。治疗包括饮食控制、运动疗法、口服降糖药（二甲双胍、磺脲类、DPP-4i、SGLT-2i等）及胰岛素。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_003",
    chunk_key: "default",
    content: "冠状动脉粥样硬化性心脏病（冠心病，CAD）是冠状动脉粥样硬化导致管腔狭窄或闭塞，引起心肌缺血缺氧的心脏病。主要表现为心绞痛（稳定型/不稳定型）、心肌梗死、心力衰竭或猝死。典型心绞痛为胸骨后压榨性疼痛，可向左肩、左臂内侧放射，伴胸闷、气短、出汗，含服硝酸甘油可缓解。诊断依靠心电图、心脏彩超、冠脉CTA及冠状动脉造影（金标准）。治疗包括抗血小板（阿司匹林）、他汀类、β受体阻滞剂、ACEI/ARB，必要时PCI或CABG。高危人群：>40岁男性、绝经后女性、高血压、糖尿病、高脂血症、吸烟、肥胖者。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_004",
    chunk_key: "default",
    content: "脑梗死（缺血性脑卒中，Ischemic Stroke）是脑部血液供应障碍导致局部脑组织缺血缺氧性坏死。典型表现为突发偏瘫、口眼歪斜、言语不清、肢体麻木、头晕、行走不稳。FAST识别法：Face（面部下垂）、Arm（手臂无力）、Speech（言语含糊）、Time（立即就医）。急性期4.5小时内可行静脉溶栓（rt-PA），6-24小时内可机械取栓。二级预防包括抗血小板、降压、降脂、控制糖尿病、戒烟限酒。脑梗死是中国居民死亡和致残的首要原因，高血压是最重要的可干预危险因素。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_005",
    chunk_key: "default",
    content: "社区获得性肺炎（Community-Acquired Pneumonia, CAP）是在医院外罹患的肺实质感染性炎症。常见病原体包括肺炎链球菌、流感嗜血杆菌、肺炎支原体、病毒等。主要表现为发热、咳嗽、咳痰、胸痛、呼吸困难。严重度评分CURB-65：意识障碍、尿素>7mmol/L、呼吸频率≥30次/分、收缩压<90或舒张压≤60mmHg、年龄≥65岁，每项1分，≥2分需住院。治疗以抗感染为主，经验性抗生素选择需覆盖常见病原体。儿童和老年人是高危人群。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_006",
    chunk_key: "default",
    content: "支气管哮喘（Bronchial Asthma）是一种以慢性气道炎症和气道高反应性为特征的异质性疾病。典型症状为反复发作的喘息、气急、胸闷和咳嗽，常在夜间和凌晨加重。发作时双肺可闻及弥漫性哮鸣音。诊断依靠症状+可变气流受限证据（支气管舒张试验阳性、PEF变异率>10%等）。治疗采用阶梯式方案：控制药物（吸入糖皮质激素±长效β2激动剂）和缓解药物（短效β2激动剂SABA）。严重急性发作需紧急就医。诱因包括过敏原、病毒感染、运动、冷空气等。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_007",
    chunk_key: "default",
    content: "慢性胃炎（Chronic Gastritis）是胃黏膜的慢性炎症性病变。主要病因包括幽门螺杆菌（Hp）感染、自身免疫、胆汁反流、长期服用NSAIDs等。临床症状不特异，常见上腹部隐痛、腹胀、嗳气、反酸、恶心、食欲不振。诊断主要依靠胃镜+病理活检。Hp阳性者需行根除治疗（四联疗法：PPI+铋剂+两种抗生素，疗程14天）。同时注意饮食规律，避免辛辣刺激食物、戒烟限酒。慢性萎缩性胃炎伴肠化生是不典型增生和胃癌的危险因素，需定期随访。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_008",
    chunk_key: "default",
    content: "肝硬化（Liver Cirrhosis）是各种慢性肝病进展至以肝脏弥漫性纤维化、假小叶形成为特征的终末阶段。我国主要病因是乙型肝炎病毒感染，其他包括丙肝、酒精性肝病、非酒精性脂肪性肝病等。代偿期可无症状或仅轻度乏力、腹胀；失代偿期出现黄疸、腹水、食管胃底静脉曲张破裂出血、肝性脑病、脾功能亢进等。Child-Pugh分级评估肝功能储备。治疗以病因治疗（抗病毒、戒酒等）+ 对症支持为主。终末期肝病需考虑肝移植。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_009",
    chunk_key: "default",
    content: "慢性肾功能不全（Chronic Kidney Disease, CKD）是指肾脏结构或功能异常持续≥3个月。分期基于eGFR（ml/min/1.73m²）：G1≥90、G2 60-89、G3a 45-59、G3b 30-44、G4 15-29、G5<15为终末期肾病。常见病因：糖尿病肾病、高血压肾损害、慢性肾小球肾炎。症状包括乏力、水肿、尿少、食欲不振、恶心、皮肤瘙痒。治疗重点：控制原发病、优质低蛋白饮食、纠正水电解质紊乱、控制血压（目标<130/80mmHg），G5期需肾脏替代治疗（透析或移植）。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_010",
    chunk_key: "default",
    content: "甲状腺功能亢进症（甲亢，Hyperthyroidism）是甲状腺合成和分泌甲状腺激素过多导致的高代谢症候群。最常见为Graves病（弥漫性毒性甲状腺肿）。临床表现：心悸、手抖、多汗、怕热、体重下降、易激动、失眠，部分伴有突眼、甲状腺肿大。诊断依靠甲状腺功能检测：TSH降低，FT3、FT4升高。治疗方式：抗甲状腺药物（甲巯咪唑/丙硫氧嘧啶）、放射性碘131治疗、手术治疗。甲亢危象是急症，表现为高热、心动过速、神志障碍，死亡率高。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_011",
    chunk_key: "default",
    content: "类风湿关节炎（Rheumatoid Arthritis, RA）是一种以对称性多关节炎为主要表现的慢性自身免疫性疾病。好发于30-50岁女性。典型特征：双手掌指关节和近端指间关节对称性肿痛伴晨僵（≥30分钟），可累及腕、膝、踝等关节。关节外表现包括类风湿结节、间质性肺病、血管炎等。诊断依靠临床表现+血清学（RF、抗CCP抗体）+影像学。治疗原则：早期使用DMARDs（甲氨蝶呤为首选），必要时联合生物制剂（TNF-α抑制剂等），目标为达到临床缓解。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_012",
    chunk_key: "default",
    content: "骨质疏松症（Osteoporosis）是以骨量减少、骨微结构破坏、骨脆性增加和易骨折为特征的代谢性骨病。好发于绝经后女性和老年人。常无症状，出现骨痛、身高变矮、驼背时已属晚期，最严重后果是骨质疏松性骨折（椎体、髋部、腕部）。诊断金标准为双能X线骨密度仪（DXA）：T值≤-2.5。防治：补钙（1000-1200mg/日）、维生素D（800-1200IU/日）、抗骨质疏松药物（双膦酸盐、地舒单抗等）、负重运动、预防跌倒。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_013",
    chunk_key: "default",
    content: "抑郁症（Major Depressive Disorder, MDD）是以显著持久的情绪低落、兴趣减退为核心症状的情感障碍。诊断标准（DSM-5）需满足≥5条症状持续≥2周：情绪低落、兴趣/快感缺失（核心症状）、体重/食欲显著变化、失眠或嗜睡、精神运动迟滞或激越、疲乏、无价值感或过分内疚、注意力减退、反复出现死亡或自杀念头。严重程度分轻度、中度、重度。治疗：抗抑郁药（SSRI如舍曲林/艾司西酞普兰为首选）+ 心理治疗（CBT/IPT）。有自杀风险需紧急干预。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_014",
    chunk_key: "default",
    content: "湿疹（Eczema/Atopic Dermatitis）是一种慢性、复发性、瘙痒性炎症性皮肤病。皮损多形性：急性期以红斑、丘疹、水疱、渗出为主，慢性期以苔藓样变、脱屑为主。好发于屈侧（肘窝、腘窝）。剧烈瘙痒是核心特征，搔抓导致皮损加重（痒-抓循环）。治疗原则：基础护理为保湿（足量使用润肤剂）+ 避免诱因（过敏原、刺激物）。外用糖皮质激素为一线抗炎药物，钙调磷酸酶抑制剂（他克莫司/吡美莫司）用于敏感部位。重度可考虑光疗或系统治疗（环孢素、度普利尤单抗等）。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_015",
    chunk_key: "default",
    content: "过敏性鼻炎（Allergic Rhinitis, AR）是特应性个体接触变应原后由IgE介导的鼻黏膜非感染性炎性疾病。主要症状：阵发性喷嚏、清水样涕、鼻塞、鼻痒，可伴眼痒、流泪。分间歇性（<4天/周或<4周）和持续性。常见过敏原：尘螨、花粉、宠物皮屑、霉菌等。诊断依靠病史+皮肤点刺试验/血清特异性IgE。治疗策略：避免过敏原、药物治疗（鼻用糖皮质激素为一线、口服/鼻用抗组胺药、白三烯受体拮抗剂）、过敏原特异性免疫治疗（脱敏治疗）。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_016",
    chunk_key: "default",
    content: "急性阑尾炎（Acute Appendicitis）是阑尾腔梗阻或细菌感染引起的急性炎症，是最常见的外科急腹症。典型表现为转移性右下腹痛（上腹或脐周→右下腹），伴恶心、呕吐、低热。体格检查：麦氏点（McBurney点）固定压痛和反跳痛。实验室：白细胞和中性粒细胞升高。影像学：腹部超声或CT可辅助诊断。治疗首选急诊阑尾切除术（开放或腹腔镜），单纯性阑尾炎也可考虑抗生素保守治疗。延误可致穿孔、腹膜炎甚至感染性休克。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_017",
    chunk_key: "default",
    content: "胆囊结石（Cholelithiasis）是胆囊内形成的固体物质，主要成分为胆固醇或胆红素钙。危险因素：40岁以上女性、肥胖、多次妊娠、高脂饮食（4F：Female、Forty、Fat、Fertile）。症状：右上腹阵发性绞痛（胆绞痛），常在油腻餐后诱发，向右肩背部放射，可伴恶心呕吐。超声为首选诊断方法。无症状者无需手术，定期随访。有症状或合并胆囊炎者行腹腔镜胆囊切除术。并发症包括急性胆囊炎、胆总管结石、急性胰腺炎等。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_018",
    chunk_key: "default",
    content: "前列腺增生（Benign Prostatic Hyperplasia, BPH）是前列腺移行带平滑肌和上皮细胞的良性增生，导致膀胱出口梗阻。50岁以上男性发病率>50%。症状分储尿期（尿频、尿急、夜尿增多）和排尿期（排尿踌躇、尿线变细、排尿费力、尿不尽感）。国际前列腺症状评分（IPSS）用于评估严重程度。PSA检测用于排除前列腺癌。治疗：轻度可观察等待；中度用α受体阻滞剂（坦索罗辛）+ 5α还原酶抑制剂（非那雄胺）；重度或药物无效行TURP等手术治疗。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_019",
    chunk_key: "default",
    content: "子宫肌瘤（Uterine Leiomyoma）是子宫平滑肌组织增生形成的良性肿瘤，为女性最常见的盆腔肿瘤。好发于30-50岁育龄女性。症状取决于肌瘤大小、数目和位置：月经量增多、经期延长、下腹包块、压迫症状（尿频、便秘）、贫血、不孕或流产。根据位置分黏膜下肌瘤（症状最明显）、肌壁间肌瘤（最常见）、浆膜下肌瘤。超声为首选诊断方法。无症状者可定期随访；症状明显者可行药物（GnRH激动剂）或手术治疗（肌瘤剔除术或子宫切除术）。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_020",
    chunk_key: "default",
    content: "白内障（Cataract）是晶状体混浊导致的视力障碍，是全球首位致盲眼病。老年性白内障最常见，随年龄增长发病率增高（>60岁约50%，>80岁近100%）。主要症状：进行性无痛性视力下降、视物模糊、对比度下降、眩光、眼前固定黑影。诊断通过裂隙灯检查可见晶状体混浊。唯一有效的治疗是手术：白内障超声乳化摘除+人工晶体植入术，手术时机为视力下降影响日常生活时。危险因素包括紫外线暴露、糖尿病、吸烟、长期使用糖皮质激素等。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_021",
    chunk_key: "default",
    content: "帕金森病（Parkinson Disease, PD）是黑质多巴胺能神经元变性死亡导致的神经系统退行性疾病。核心运动症状：静止性震颤（搓丸样）、肌强直（铅管样/齿轮样）、运动迟缓（面具脸、写字过小征）、姿势步态异常（慌张步态）。非运动症状：嗅觉减退、便秘、睡眠障碍、抑郁焦虑、认知障碍等。诊断主要依据临床特征。治疗以左旋多巴制剂为金标准，多巴胺受体激动剂、MAO-B抑制剂等为辅。中晚期可出现运动波动（剂末现象、「开-关」现象）和异动症。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_022",
    chunk_key: "default",
    content: "银屑病（Psoriasis）俗称牛皮癣，是一种免疫介导的慢性复发性炎症性皮肤病。寻常型最常见占90%：特征为境界清楚的红色斑块，表面覆盖银白色鳞屑，好发于头皮、肘膝伸侧、腰骶部。Auspitz征（刮除鳞屑后点状出血）为特征性体征。可伴瘙痒，约30%患者伴关节损害（银屑病关节炎）。诱因包括感染（链球菌）、外伤（同形反应）、精神压力、药物等。治疗：轻度以外用为主（糖皮质激素、维生素D3衍生物）；中重度需光疗（NB-UVB）、系统药物（甲氨蝶呤、环孢素）或生物制剂（TNF-αi、IL-17i、IL-23i等）。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_023",
    chunk_key: "default",
    content: "痛风（Gout）是嘌呤代谢紊乱和/或尿酸排泄减少导致的高尿酸血症，尿酸盐结晶沉积在关节及周围组织引起的急性炎症。典型发作：夜间突发单个关节（最常见第一跖趾关节）红、肿、热、剧痛，24小时内达高峰，持续数天至数周自行缓解。急性期治疗以消炎止痛为主（秋水仙碱、NSAIDs、糖皮质激素）。缓解期需降尿酸治疗（别嘌醇/非布司他/苯溴马隆），目标是血清尿酸<360μmol/L（有痛风石者<300）。饮食限制：避免动物内脏、海鲜、啤酒等高嘌呤食物，多饮水促进尿酸排泄。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_024",
    chunk_key: "default",
    content: "焦虑症（广泛性焦虑障碍，Generalized Anxiety Disorder, GAD）是以持续过度的焦虑和担忧为核心特征的精神障碍。诊断标准：对多种事件或活动过度焦虑和担忧持续≥6个月，且难以控制，伴≥3项以下症状：坐立不安或紧张、易疲劳、注意力不集中、易激惹、肌肉紧张、睡眠障碍。常伴自主神经症状：心悸、出汗、颤抖、口干。常与抑郁症共病。治疗：心理治疗以认知行为治疗（CBT）为一线 + 药物（SSRI/SNRI为首选，如帕罗西汀、文拉法辛）。苯二氮卓类仅限短期使用。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DIS_025",
    chunk_key: "default",
    content: "腰椎间盘突出症（Lumbar Disc Herniation）是纤维环破裂，髓核突出压迫神经根引起的临床综合征。好发于L4/L5和L5/S1节段。主要症状：腰痛伴单侧下肢放射痛（坐骨神经痛），咳嗽、打喷嚏时加重。体征：直腿抬高试验阳性、受累神经根支配区感觉减退或肌力下降。MRI为最佳影像学诊断方法。治疗以保守为主（卧床休息、腰围保护、NSAIDs、理疗），严格保守治疗≥3个月无效或出现进行性神经功能缺损（如足下垂）、马尾综合征（大小便障碍）者需手术。",
    embedding: null
  },

  // ---- 医院与科室知识 ----
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_HOSP_001",
    chunk_key: "default",
    content: "北京协和医院是中国医学科学院北京协和医学院的临床医学院，位于北京市东城区，是一所集医疗、教学、科研为一体的三级甲等综合医院。医院创建于1921年，连续多年位居中国医院排行榜（复旦版）综合实力榜首。拥有国家级重点学科20余个，其中心血管内科、神经内科、骨科、内分泌科、妇产科均为国家临床重点专科，在疑难重症诊治方面处于国内领先水平。医院地址：北京市东城区东单帅府园1号。急诊24小时开放。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_HOSP_002",
    chunk_key: "default",
    content: "上海交通大学医学院附属瑞金医院创建于1907年，位于上海市黄浦区瑞金二路197号，是集医疗、教学、科研为一体的三级甲等综合医院。医院在内分泌代谢病、心血管病、呼吸疾病、消化疾病、血液病、烧伤等领域具有显著特色和优势。拥有国家临床重点专科22个，烧伤科、内分泌代谢病科、血液科均为国家级重点学科。医院连续多年位列中国十大医院。设有上海市内分泌代谢病研究所、上海市血液学研究所等科研平台。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_HOSP_003",
    chunk_key: "default",
    content: "中山大学附属第一医院（简称中山一院）位于广州市越秀区中山二路58号，是国家卫生健康委委属委管的三级甲等综合医院，华南地区最早成立的西医院。医院拥有国家重点学科12个和国家临床重点专科30个，器官移植、肾脏病、神经内科、普通外科、眼科等学科处于全国领先水平。医院连续多年位列复旦大学医院排行榜全国前十，是粤港澳大湾区医疗中心。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_HOSP_004",
    chunk_key: "default",
    content: "四川大学华西医院位于成都市武侯区国学巷37号，是中国西南地区最大的三级甲等综合医院。医院起源于1892年的成都仁济医院，现为四川大学华西医学中心的附属医院。在复旦版中国医院排行榜中长期位列全国第二，拥有麻醉学、呼吸内科、普通外科、精神医学等全国领先学科。华西医院以疑难危重症诊治闻名，年门急诊量超700万人次。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_HOSP_005",
    chunk_key: "default",
    content: "华中科技大学同济医学院附属同济医院位于武汉市硚口区解放大道1095号，创建于1900年，是华中地区最早成立的西医院之一。医院为国家卫健委委属委管的三级甲等综合医院，拥有国家重点学科8个和国家临床重点专科30个，妇产科、心血管内科、呼吸内科、普通外科在全国具有重要影响力。在器官移植（肝、肾、心）领域处于国内前列，累积完成肾脏移植超过6000例。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DEPT_CARD",
    chunk_key: "default",
    content: "心血管内科（Cardiology）是诊治心脏和血管系统疾病的内科专科。诊疗范围包括：冠心病（心绞痛、心肌梗死）、高血压病、心力衰竭、心律失常（房颤、室性早搏、传导阻滞）、心肌病、心脏瓣膜病、先天性心脏病、心包疾病等。常用检查：心电图（ECG）、动态心电图（Holter）、心脏彩超、冠脉CTA、冠状动脉造影、心脏核磁共振、运动负荷试验等。常见治疗：药物治疗、冠状动脉介入治疗（PCI/支架）、起搏器植入、射频消融术等。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DEPT_NEURO",
    chunk_key: "default",
    content: "神经内科（Neurology）是诊治中枢神经系统、周围神经系统和肌肉疾病的内科专科。诊疗范围：脑卒中（脑梗死、脑出血）、帕金森病、痴呆（阿尔茨海默病）、多发性硬化、癫痫、偏头痛、重症肌无力、周围神经病等。常用检查：头部CT、头部MRI/DWI、脑血管造影（DSA）、脑电图（EEG）、肌电图（EMG）、诱发电位等。脑卒中是神经内科最常见急症，开通闭塞血管有严格时间窗限制（静脉溶栓≤4.5h，机械取栓≤24h）。",
    embedding: null
  },
  {
    source_type: "KNOWLEDGE",
    source_id: "TEST_DEPT_RESPI",
    chunk_key: "default",
    content: "呼吸内科（Respiratory Medicine）是诊治呼吸系统疾病的专科。诊疗范围包括：社区获得性肺炎、支气管哮喘、慢性阻塞性肺疾病（COPD）、间质性肺病、肺栓塞、肺癌、睡眠呼吸暂停综合征、支气管扩张等。常用检查：胸部X线、胸部CT、肺功能检查（PFT）、支气管镜检查及肺泡灌洗、痰培养等。吸烟是呼吸系统疾病最重要的可预防危险因素，戒烟是预防COPD和肺癌的关键措施。",
    embedding: null
  }
]);

// ========================== AUDIT 类型 — AI召回审计记录 (8条)
db.rag_document_chunks.insertMany([
  {
    source_type: "AUDIT",
    source_id: "TEST_AUDIT_001",
    chunk_key: "default",
    content: "[AI召回审计日志] 患者：男，55岁。主诉症状：头痛、头晕、心悸。年龄分组：老年。候选病种（5个）：原发性高血压(DIS_001)、冠心病(DIS_003)、脑梗死(DIS_004)、甲状腺功能亢进症(DIS_010)、焦虑症(DIS_024)。AI推荐（3个）：原发性高血压(DIS_001)、冠心病(DIS_003)、焦虑症(DIS_024)。召回状态：SUCCESS。评估：准确率良好，症状与高血压/冠心病高度相关，建议进一步检查心电图和动态血压监测。",
    embedding: null
  },
  {
    source_type: "AUDIT",
    source_id: "TEST_AUDIT_002",
    chunk_key: "default",
    content: "[AI召回审计日志] 患者：男，62岁。主诉症状：胸痛、胸闷、气短、放射痛（左肩左臂）。年龄分组：老年。候选病种（4个）：冠心病(DIS_003)、社区获得性肺炎(DIS_005)、焦虑症(DIS_024)、原发性高血压(DIS_001)。AI推荐（2个）：冠心病(DIS_003)、原发性高血压(DIS_001)。召回状态：SUCCESS。评估：症状描述典型，与冠心病高度匹配，胸痛放射至左臂为心绞痛特征性表现，建议行冠脉CTA或造影。",
    embedding: null
  },
  {
    source_type: "AUDIT",
    source_id: "TEST_AUDIT_003",
    chunk_key: "default",
    content: "[AI召回审计日志] 患者：女，45岁。主诉症状：多饮、多尿、体重下降、乏力。年龄分组：成人。候选病种（5个）：2型糖尿病(DIS_002)、甲状腺功能亢进症(DIS_010)、慢性肾功能不全(DIS_009)、抑郁症(DIS_013)、焦虑症(DIS_024)。AI推荐（2个）：2型糖尿病(DIS_002)、甲状腺功能亢进症(DIS_010)。召回状态：SUCCESS。评估：「三多一少」症状提示糖尿病与甲亢需鉴别，建议查空腹血糖、HbA1c和甲状腺功能。",
    embedding: null
  },
  {
    source_type: "AUDIT",
    source_id: "TEST_AUDIT_004",
    chunk_key: "default",
    content: "[AI召回审计日志] 患者：男，8岁。主诉症状：咳嗽、发热、胸痛、呼吸困难。年龄分组：儿童。候选病种（3个）：社区获得性肺炎(DIS_005)、支气管哮喘(DIS_006)、过敏性鼻炎(DIS_015)。AI推荐（2个）：社区获得性肺炎(DIS_005)、支气管哮喘(DIS_006)。召回状态：SUCCESS。评估：儿童呼吸道症状需鉴别肺炎与哮喘，发热更倾向肺炎，建议胸部X线和肺功能检查。",
    embedding: null
  },
  {
    source_type: "AUDIT",
    source_id: "TEST_AUDIT_005",
    chunk_key: "default",
    content: "[AI召回审计日志] 患者：女，52岁。主诉症状：关节肿痛、晨僵（对称性）。年龄分组：老年。候选病种（4个）：类风湿关节炎(DIS_011)、骨质疏松症(DIS_012)、痛风(DIS_023)、腰椎间盘突出症(DIS_025)。AI推荐（2个）：类风湿关节炎(DIS_011)、痛风(DIS_023)。召回状态：SUCCESS。评估：对称性小关节肿痛伴晨僵高度提示类风湿关节炎，建议查RF和抗CCP抗体。",
    embedding: null
  },
  {
    source_type: "AUDIT",
    source_id: "TEST_AUDIT_006",
    chunk_key: "default",
    content: "[AI召回审计日志] 患者：女，28岁。主诉症状：情绪低落、睡眠障碍、食欲下降。年龄分组：成人。候选病种（4个）：抑郁症(DIS_013)、焦虑症(DIS_024)、甲状腺功能亢进症(DIS_010)、冠心病(DIS_003)。AI推荐（2个）：抑郁症(DIS_013)、焦虑症(DIS_024)。召回状态：SUCCESS。评估：核心症状符合抑郁发作，需排除甲亢导致的精神症状，建议心理量表评估和甲功检查。",
    embedding: null
  },
  {
    source_type: "AUDIT",
    source_id: "TEST_AUDIT_007",
    chunk_key: "default",
    content: "[AI召回审计日志] 患者：女，25岁。主诉症状：下腹痛、发热、恶心。年龄分组：成人。候选病种（6个）：急性阑尾炎(DIS_016)、子宫肌瘤(DIS_019)、社区获得性肺炎(DIS_005)、慢性胃炎(DIS_007)、胆囊结石(DIS_017)、慢性肾功能不全(DIS_009)。AI推荐（2个）：急性阑尾炎(DIS_016)、子宫肌瘤(DIS_019)。召回状态：PARTIAL。评估：育龄女性下腹痛需鉴别妇科急症与外科急腹症，建议妇科超声和腹部超声同时检查。",
    embedding: null
  },
  {
    source_type: "AUDIT",
    source_id: "TEST_AUDIT_008",
    chunk_key: "default",
    content: "[AI召回审计日志] 患者：男，35岁。主诉症状：（空输入）。年龄分组：成人。候选病种（0个）：无。AI推荐（0个）：无。召回状态：EMPTY_INPUT。评估：输入症状为空，无法进行AI召回。需引导用户至少提供1个核心症状以启动导诊流程。系统已触发空输入提示，引导用户重新描述不适。",
    embedding: null
  }
]);

// ========================== IMPORT_JOB 类型 — 导入任务摘要 (5条)
db.rag_document_chunks.insertMany([
  {
    source_type: "IMPORT_JOB",
    source_id: "TEST_IMPORT_001",
    chunk_key: "default",
    content: "[数据导入任务 #1] 数据集类型：HOSPITAL。文件：北京地区医院数据_2026Q1.xlsx。状态：COMPLETED。导入结果：15条成功，2条失败（科室编码不匹配），3条待审核，12条自动映射。失败原因：(1) 科室编码STD_CARDX不存在于标准编码库；(2) hospital_id为空。经验：导入前需校验科室编码与标准编码库的一致性，确保必填字段完整。",
    embedding: null
  },
  {
    source_type: "IMPORT_JOB",
    source_id: "TEST_IMPORT_002",
    chunk_key: "default",
    content: "[数据导入任务 #2] 数据集类型：DISEASE。文件：ICD10疾病分类_导入模板.csv。状态：COMPLETED。导入结果：200条成功，5条失败（年龄范围格式错误），10条待确认，180条自动映射。待确认项包括：偏头痛别名「头疼」范围过宽建议改为「偏侧头痛」、缺铁性贫血年龄范围已补充。经验：年龄字段需严格校验数据类型（应为整型），疾病别名需精准不要泛化。",
    embedding: null
  },
  {
    source_type: "IMPORT_JOB",
    source_id: "TEST_IMPORT_003",
    chunk_key: "default",
    content: "[数据导入任务 #3] 数据集类型：DOCTOR。文件：华西医院医生信息_2026.xlsx。状态：COMPLETED。导入结果：48条成功，0条失败，5条待审核（职称信息待确认），40条自动映射。待确认项：张三职称「主治/副主任」待明确、李四所属「综合科」需细化至具体专科（已确认为心内科）。经验：医生职称应标准化，科室名称应使用标准编码。",
    embedding: null
  },
  {
    source_type: "IMPORT_JOB",
    source_id: "TEST_IMPORT_004",
    chunk_key: "default",
    content: "[数据导入任务 #4] 数据集类型：DEPARTMENT。文件：上海三甲医院科室数据.csv。状态：REVIEWING（待审核）。导入结果：30条成功，3条失败，8条待审核，25条自动映射。待审核项：瑞金医院东院「综合内科」匹配到多个标准科室编码需人工选择；华山医院「老年病科」、「疼痛科」未找到匹配的标准科室编码（建议新建STD_GERIATRIC和STD_PAIN）。",
    embedding: null
  },
  {
    source_type: "IMPORT_JOB",
    source_id: "TEST_IMPORT_005",
    chunk_key: "default",
    content: "[数据导入任务 #5] 数据集类型：CAPABILITY。文件：医疗能力目录_全量导入_2026.xlsx。状态：FAILED（导入失败）。导入结果：0条成功，50条全部失败。失败原因：文件格式校验失败，缺少capability_code必填列。根因分析：模板版本不一致，导入文件使用了旧版模板（缺少必填列capability_code）。改进措施：(1) 建立严格的导入模板版本校验机制；(2) 在导入前进行列名校验并给出友好提示；(3) 更新导入文档和模板下载链接。",
    embedding: null
  }
]);

// 为 TEST_ 测试文档回填 1024 维占位向量，确保 RAG 检索链路可联调。
let seed = 1;
db.rag_document_chunks
  .find({ source_id: { $regex: /^TEST_/ }, embedding: null }, { _id: 1 })
  .forEach((doc) => {
    db.rag_document_chunks.updateOne(
      { _id: doc._id },
      { $set: { embedding: createEmbedding(seed) } }
    );
    seed += 1;
  });

// 统计插入结果
const totalDocs = db.rag_document_chunks.countDocuments({ source_id: { $regex: /^TEST_/ } });
print("已成功插入 " + totalDocs + " 条测试文档到 rag_document_chunks 集合。");
print("已为 TEST_ 文档回填 1024 维占位向量，可用于本地联调。");
