# 第四步骤最终定稿版：数据库与权限一体化设计

> 来源：`LocalTalent_step4.html`。
> 用途：开发前必读 Markdown 版；保留原文内容，移除 HTML/CSS 展示噪音，并补充开发前必读卡与执行索引。

## 开发前必读卡

- **文件定位**：数据库、权限、审批、审计一体化最终口径；后续迁移脚本与权限实现以此为主。
- **实现约束**：DDL、RBAC、数据域、字段级权限、导出审批、审计日志必须同步设计并在服务端强制执行。
- **禁止越界点**：禁止人才服务区裸查原始候选人表；禁止用前端隐藏代替字段级权限；禁止导出绕过审批。
- **下游影响**：直接约束 Flyway 迁移、Service 查询、DTO 组装、权限引擎、审计打点和导出审批。
- **自动化验收关注点**：迁移测试、越权阻断、字段级裁剪、审计日志、导出审批、公开层字段黑名单。

**统一硬边界提醒**：人才服务区仅展示发布快照；对接接口仅 stub；不得输出原始候选人数据；同意、撤回、数据域、字段级权限必须在服务端收口。

## 执行索引

- 总体原则
- candidate 核心边界字段主归属
- 分库分表建议
- 枚举字典策略
- 核心业务表 DDL
- 集成/日志/审计/证据表 DDL
- 角色/菜单/数据域/字段级权限
- 导出审批与高风险动作控制
- 日志打点位置
- 权限附加表 DDL
- 接口 × 权限 × 审批 × 日志矩阵

第四步骤最终定稿版 · 地方人才服务平台一期

本文件将 LocalTalent_step4_1 修订版中的数据库 DDL 草案与 LocalTalent_step4_2 修订版中的权限模型实现说明进行重构整合，形成真正可供研发、测试、安全、法务共同评审的最终定稿版。本文仅覆盖地方人才服务平台一期范围，不展开第五步骤及后续实施规划。

### 覆盖范围

地方人才服务平台一期

### 整合内容

DDL + 权限模型 + 审批 + 审计

### 关键增强

candidate 边界字段主归属明确

### 交付定位

联合评审最终稿

## 一、总体原则

### 原则 1

平台以“地方人才服务平台一期独立可运行”为前提建模，不依赖 ATS 与受控候选池当前上线。

### 原则 2

数据库结构与权限模型同步设计，避免“表已建完、权限另补”的割裂状态。

### 原则 3

坚持“公开层、业务层、审计层、证据层”分层设计，避免原始候选人数据直接暴露至公开链路。

本稿明确只覆盖一期范围：职位、企业、求职者、投递、活动、面试签到、同意、人才服务区（基于候选人发布快照）、运营后台、对接接口预留。禁止直接接入 ATS 原始候选人数据，禁止直接接入受控候选池原始主库。

| 术语 | 本文统一口径 |
| --- | --- |
| **人才服务区** | 指平台前台面向外部展示候选人信息的专区，本文统一定义为“基于候选人发布快照的公开展示区”，不等同于公共简历库。 |
| **发布快照** | 指经过同意、裁剪、脱敏后可进入人才服务区展示或同步的候选人摘要副本，本文统一称为“候选人发布快照”。 |
| **原始候选人数据** | 指候选人账号信息、联系方式、简历正文、附件、证据材料等未进入公开展示链路的原始业务数据。 |
| **同意 / 撤回** | “同意”指候选人对发布、收录、同步等范围作出的明示同意；“撤回”统一指撤回同意，并触发发布下架或同步停止。 |
| **对接接口** | 本文统一使用“对接接口”表示平台与 ATS、受控候选池或其他外部系统之间的开放集成接口。 |
| **数据域** | 指角色在服务端可访问的数据边界范围，例如本人、本企业、本岗位、本场次、聚合范围等。 |
| **字段级权限** | 指字段在接口出参中的显示、脱敏、隐藏三种控制策略，由后端统一执行。 |

## 二、candidate 核心边界字段主归属

| 边界字段 | 主归属表 | 辅助表 | 说明 |
| --- | --- | --- | --- |
| **source_type** | candidate_control_profile | job_application / candidate_publish_snapshot | 候选人进入平台主链路的来源主属性，应有统一主归属；业务子链路可保留自身来源字段。 |
| **legal_basis** | candidate_control_profile | candidate_publish_snapshot | 候选人被平台继续处理、发布、同步的法律基础主口径，应集中管理。 |
| **consent_status** | candidate_control_profile | candidate_consent / candidate_publish_snapshot | candidate_consent 负责记录每次同意行为，candidate_control_profile 负责当前生效状态。 |
| **publishable_flag** | candidate_control_profile | candidate_publish_snapshot | 是否允许进入人才服务区（公开展示层）的主控制字段，应有统一当前值。 |
| **visibility_scope** | candidate_control_profile | candidate_publish_snapshot | 控制当前可见范围，快照表保存人才服务区（公开展示层）的具体生效副本。 |
| **consent_version** | candidate_control_profile | candidate_consent / candidate_publish_snapshot | 当前生效版本由主控制表保存，历史版本由同意记录表保存。 |

步骤四修订清单要求“明确候选人核心边界字段主归属，不再分散表达，必要时增加 candidate_state / candidate_control_profile 之类的边界主表”。因此本稿新增 candidate_control_profile 作为统一主控制表，避免这些边界字段继续散落在 consent 与 snapshot 两侧。

## 三、分库分表建议

| 逻辑库 | 建议表 | 说明 |
| --- | --- | --- |
| **local_talent_biz** | company, company_user, candidate_user, candidate_resume, candidate_control_profile, job_post, job_application, activity_event, activity_registration, interview_session, interview_signin, candidate_consent, candidate_publish_snapshot, cms_content | 平台主业务库，承载人才网、企业后台、求职者中心、活动与同意主流程。 |
| **local_talent_integration** | integration_sync_task, integration_mapping, open_api_log, open_client | 外部系统对接库，承载同步任务、映射关系、对接接口调用日志与对接客户端。 |
| **local_talent_audit** | audit_log, field_access_log, export_apply | 审计与导出审批库，建议与主业务库分离。 |
| **local_talent_evidence** | candidate_consent_evidence（元数据） | 证据元数据入库；页面快照、签章文件、附件建议落对象存储，库中保存引用地址与哈希。 |
| **local_talent_auth** | sys_role, sys_menu, sys_role_menu, sys_user_role, sys_role_data_scope, sys_role_field_policy | 权限配置库，承载角色、菜单、数据域、字段策略。 |

### 当前不建议分表

- company / job_post / candidate_user / candidate_resume / candidate_control_profile
- 一期量级可控，分表会提升复杂度

### 建议按时间分表或归档

- audit_log
- field_access_log
- integration_sync_task
- open_api_log

## 四、枚举字典策略

状态类字段统一采用“小整数编码 + 字典表解释”的方式，保证前后端、接口和统计口径一致。

| 字典类型 | 典型取值 | 说明 |
| --- | --- | --- |
| **company_auth_status** | 0待提交，1待审核，2已通过，3已驳回，4已禁用 | 企业认证状态 |
| **job_status** | 0草稿，1待审核，2在线，3下线，4删除 | 职位状态 |
| **application_status** | 0已投递，1待筛选，2邀约面试，3已签到，4已结束，5已淘汰 | 投递流程状态 |
| **consent_status** | 0未同意，1已同意，2已撤回 | 同意状态 |
| **publishable_flag** | 0不可发布，1可发布 | 是否可进入人才服务区（公开展示层） |
| **visibility_scope** | 1仅本人，2本企业，3运营审核，4人才服务区 | 可见范围 |
| **source_type** | 1站内注册，2站内投递，3面试签到，4ATS同步，5候选池推送，6人工录入 | 来源类型 |

```sql
CREATE TABLE sys_dict_type (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  dict_code VARCHAR(64) NOT NULL,
  dict_name VARCHAR(128) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_dict_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_dict_item (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  dict_type_id BIGINT UNSIGNED NOT NULL,
  item_code VARCHAR(64) NOT NULL,
  item_name VARCHAR(128) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_type_code (dict_type_id, item_code),
  KEY idx_type_sort (dict_type_id, sort_no),
  CONSTRAINT fk_dict_item_type FOREIGN KEY (dict_type_id) REFERENCES sys_dict_type(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

## 五、核心业务表 DDL

### 1. 企业与账号

```sql
CREATE TABLE company (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  company_name VARCHAR(200) NOT NULL,
  license_no VARCHAR(64) NOT NULL,
  industry_code VARCHAR(64) DEFAULT NULL,
  scale_code VARCHAR(64) DEFAULT NULL,
  city_code VARCHAR(32) DEFAULT NULL,
  address VARCHAR(255) DEFAULT NULL,
  company_profile TEXT DEFAULT NULL,
  auth_status TINYINT NOT NULL DEFAULT 1,
  source_system VARCHAR(32) NOT NULL DEFAULT 'portal',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_license_no (license_no),
  KEY idx_auth_status (auth_status),
  KEY idx_city_code (city_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE company_user (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  company_id BIGINT UNSIGNED NOT NULL,
  user_name VARCHAR(100) NOT NULL,
  role_code VARCHAR(64) NOT NULL,
  mobile VARCHAR(32) DEFAULT NULL,
  email VARCHAR(128) DEFAULT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  last_login_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_company_user_mobile (company_id, mobile),
  UNIQUE KEY uk_company_user_email (company_id, email),
  KEY idx_company_role (company_id, role_code),
  CONSTRAINT fk_company_user_company FOREIGN KEY (company_id) REFERENCES company(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

### 2. 求职者、简历与边界主控

```sql
CREATE TABLE candidate_user (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  mobile VARCHAR(32) DEFAULT NULL,
  email VARCHAR(128) DEFAULT NULL,
  password_hash VARCHAR(255) NOT NULL,
  real_name VARCHAR(64) DEFAULT NULL,
  realname_verified_flag TINYINT NOT NULL DEFAULT 0,
  register_channel VARCHAR(32) NOT NULL DEFAULT 'portal',
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_candidate_mobile (mobile),
  UNIQUE KEY uk_candidate_email (email),
  KEY idx_register_channel (register_channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE candidate_resume (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  candidate_id BIGINT UNSIGNED NOT NULL,
  resume_name VARCHAR(150) NOT NULL,
  base_profile_json JSON DEFAULT NULL,
  education_json JSON DEFAULT NULL,
  experience_json JSON DEFAULT NULL,
  skills_json JSON DEFAULT NULL,
  attachment_url VARCHAR(500) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_candidate_status (candidate_id, status),
  CONSTRAINT fk_resume_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE candidate_control_profile (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  candidate_id BIGINT UNSIGNED NOT NULL,
  source_type TINYINT NOT NULL DEFAULT 1,
  legal_basis VARCHAR(64) NOT NULL DEFAULT 'consent',
  consent_status TINYINT NOT NULL DEFAULT 0,
  publishable_flag TINYINT NOT NULL DEFAULT 0,
  visibility_scope TINYINT NOT NULL DEFAULT 2,
  consent_version VARCHAR(32) DEFAULT NULL,
  current_consent_id BIGINT UNSIGNED DEFAULT NULL,
  current_snapshot_id BIGINT UNSIGNED DEFAULT NULL,
  control_status TINYINT NOT NULL DEFAULT 1 COMMENT '1有效 0冻结',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_candidate_control (candidate_id),
  KEY idx_publishable_scope (publishable_flag, visibility_scope),
  CONSTRAINT fk_control_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

### 3. 职位与投递

```sql
CREATE TABLE job_post (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  company_id BIGINT UNSIGNED NOT NULL,
  external_job_id VARCHAR(64) DEFAULT NULL,
  source_type TINYINT NOT NULL DEFAULT 1,
  title VARCHAR(150) NOT NULL,
  category_code VARCHAR(64) DEFAULT NULL,
  city_code VARCHAR(32) DEFAULT NULL,
  salary_min INT DEFAULT NULL,
  salary_max INT DEFAULT NULL,
  job_desc TEXT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  audit_status TINYINT NOT NULL DEFAULT 1,
  sync_version INT NOT NULL DEFAULT 1,
  published_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_source_external_job (source_type, external_job_id),
  KEY idx_company_status (company_id, status),
  KEY idx_city_status (city_code, status),
  KEY idx_audit_status (audit_status),
  CONSTRAINT fk_job_company FOREIGN KEY (company_id) REFERENCES company(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE job_application (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  job_id BIGINT UNSIGNED NOT NULL,
  candidate_id BIGINT UNSIGNED NOT NULL,
  resume_id BIGINT UNSIGNED DEFAULT NULL,
  source_type TINYINT NOT NULL DEFAULT 1,
  external_application_id VARCHAR(64) DEFAULT NULL,
  application_status TINYINT NOT NULL DEFAULT 0,
  apply_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_job_candidate_once (job_id, candidate_id),
  UNIQUE KEY uk_source_external_apply (source_type, external_application_id),
  KEY idx_candidate_status (candidate_id, application_status),
  KEY idx_job_status (job_id, application_status),
  CONSTRAINT fk_application_job FOREIGN KEY (job_id) REFERENCES job_post(id),
  CONSTRAINT fk_application_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id),
  CONSTRAINT fk_application_resume FOREIGN KEY (resume_id) REFERENCES candidate_resume(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

### 4. 活动、面试与签到

```sql
CREATE TABLE activity_event (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  type_code VARCHAR(64) NOT NULL,
  city_code VARCHAR(32) DEFAULT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  location VARCHAR(255) DEFAULT NULL,
  organizer_company_id BIGINT UNSIGNED DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_city_time (city_code, start_time),
  KEY idx_status_time (status, start_time),
  CONSTRAINT fk_event_company FOREIGN KEY (organizer_company_id) REFERENCES company(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE activity_registration (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  event_id BIGINT UNSIGNED NOT NULL,
  candidate_id BIGINT UNSIGNED NOT NULL,
  register_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sign_status TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_event_candidate (event_id, candidate_id),
  KEY idx_candidate_event (candidate_id, event_id),
  CONSTRAINT fk_registration_event FOREIGN KEY (event_id) REFERENCES activity_event(id),
  CONSTRAINT fk_registration_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE interview_session (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  job_id BIGINT UNSIGNED NOT NULL,
  company_id BIGINT UNSIGNED NOT NULL,
  event_id BIGINT UNSIGNED DEFAULT NULL,
  session_name VARCHAR(150) NOT NULL,
  session_time DATETIME NOT NULL,
  location VARCHAR(255) DEFAULT NULL,
  qr_code VARCHAR(255) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_company_time (company_id, session_time),
  KEY idx_job_time (job_id, session_time),
  CONSTRAINT fk_session_job FOREIGN KEY (job_id) REFERENCES job_post(id),
  CONSTRAINT fk_session_company FOREIGN KEY (company_id) REFERENCES company(id),
  CONSTRAINT fk_session_event FOREIGN KEY (event_id) REFERENCES activity_event(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE interview_signin (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT UNSIGNED NOT NULL,
  candidate_id BIGINT UNSIGNED NOT NULL,
  sign_channel VARCHAR(32) NOT NULL DEFAULT 'qrcode',
  sign_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  device_id VARCHAR(128) DEFAULT NULL,
  consent_redirect_flag TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_session_candidate (session_id, candidate_id),
  KEY idx_candidate_signin (candidate_id, sign_time),
  CONSTRAINT fk_signin_session FOREIGN KEY (session_id) REFERENCES interview_session(id),
  CONSTRAINT fk_signin_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

### 5. 同意、发布快照与内容

```sql
CREATE TABLE candidate_consent (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  candidate_id BIGINT UNSIGNED NOT NULL,
  consent_status TINYINT NOT NULL DEFAULT 0,
  consent_scope JSON NOT NULL,
  consent_version VARCHAR(32) NOT NULL,
  consent_channel VARCHAR(32) NOT NULL DEFAULT 'portal',
  consent_time DATETIME DEFAULT NULL,
  realname_verified_flag TINYINT NOT NULL DEFAULT 0,
  second_confirm_flag TINYINT NOT NULL DEFAULT 0,
  revoke_status TINYINT NOT NULL DEFAULT 0,
  revoke_time DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_candidate_consent (candidate_id, consent_status),
  KEY idx_revoke_status (revoke_status),
  CONSTRAINT fk_consent_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE candidate_publish_snapshot (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  candidate_id BIGINT UNSIGNED NOT NULL,
  source_type TINYINT NOT NULL DEFAULT 1,
  legal_basis VARCHAR(64) NOT NULL DEFAULT 'consent',
  consent_status TINYINT NOT NULL DEFAULT 0,
  publishable_flag TINYINT NOT NULL DEFAULT 0,
  consent_version VARCHAR(32) DEFAULT NULL,
  visibility_scope TINYINT NOT NULL DEFAULT 2,
  snapshot_json JSON NOT NULL,
  sync_version INT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_publishable_status (publishable_flag, status),
  KEY idx_candidate_snapshot (candidate_id, status),
  KEY idx_visibility_scope (visibility_scope),
  CONSTRAINT fk_snapshot_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cms_content (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  content_type VARCHAR(32) NOT NULL,
  title VARCHAR(200) NOT NULL,
  cover_url VARCHAR(500) DEFAULT NULL,
  summary VARCHAR(500) DEFAULT NULL,
  body_html MEDIUMTEXT DEFAULT NULL,
  city_code VARCHAR(32) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  publish_time DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_type_status_time (content_type, status, publish_time),
  KEY idx_city_type (city_code, content_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

## 六、集成表 / 日志表 / 审计表 / 证据表 DDL

```sql
CREATE TABLE candidate_consent_evidence (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  consent_id BIGINT UNSIGNED NOT NULL,
  page_snapshot_url VARCHAR(500) DEFAULT NULL,
  content_hash VARCHAR(128) DEFAULT NULL,
  device_id VARCHAR(128) DEFAULT NULL,
  ip VARCHAR(64) DEFAULT NULL,
  user_agent VARCHAR(500) DEFAULT NULL,
  sms_record_id VARCHAR(64) DEFAULT NULL,
  verify_vendor VARCHAR(32) DEFAULT NULL,
  verify_result_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_consent_id (consent_id),
  CONSTRAINT fk_evidence_consent FOREIGN KEY (consent_id) REFERENCES candidate_consent(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE integration_mapping (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  local_biz_type VARCHAR(32) NOT NULL,
  local_id BIGINT UNSIGNED NOT NULL,
  source_system VARCHAR(32) NOT NULL,
  external_id VARCHAR(64) NOT NULL,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_mapping (local_biz_type, local_id, source_system),
  UNIQUE KEY uk_external_mapping (source_system, external_id),
  KEY idx_source_system (source_system)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE integration_sync_task (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  source_system VARCHAR(32) NOT NULL,
  target_system VARCHAR(32) NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  biz_id BIGINT UNSIGNED NOT NULL,
  payload_json JSON DEFAULT NULL,
  sync_status TINYINT NOT NULL DEFAULT 0,
  retry_count INT NOT NULL DEFAULT 0,
  sync_version INT NOT NULL DEFAULT 1,
  last_sync_time DATETIME DEFAULT NULL,
  next_retry_time DATETIME DEFAULT NULL,
  error_msg VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_sync_status_time (sync_status, next_retry_time),
  KEY idx_biz (biz_type, biz_id),
  KEY idx_source_target (source_system, target_system)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE open_api_log (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  source_system VARCHAR(32) NOT NULL,
  api_code VARCHAR(64) NOT NULL,
  trace_id VARCHAR(64) DEFAULT NULL,
  request_uri VARCHAR(255) NOT NULL,
  request_method VARCHAR(16) NOT NULL,
  biz_type VARCHAR(32) DEFAULT NULL,
  biz_id BIGINT UNSIGNED DEFAULT NULL,
  http_status INT NOT NULL DEFAULT 200,
  success_flag TINYINT NOT NULL DEFAULT 1,
  request_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  response_time DATETIME DEFAULT NULL,
  error_msg VARCHAR(500) DEFAULT NULL,
  KEY idx_source_api_time (source_system, api_code, request_time),
  KEY idx_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE audit_log (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT UNSIGNED NOT NULL,
  operator_role VARCHAR(64) NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  biz_id BIGINT UNSIGNED NOT NULL,
  action_type VARCHAR(64) NOT NULL,
  before_json JSON DEFAULT NULL,
  after_json JSON DEFAULT NULL,
  trace_id VARCHAR(64) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_biz_action_time (biz_type, biz_id, created_at),
  KEY idx_operator_time (operator_id, created_at),
  KEY idx_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE field_access_log (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT UNSIGNED NOT NULL,
  operator_role VARCHAR(64) NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  biz_id BIGINT UNSIGNED NOT NULL,
  field_name VARCHAR(128) NOT NULL,
  access_type VARCHAR(32) NOT NULL,
  trace_id VARCHAR(64) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_operator_field_time (operator_id, field_name, created_at),
  KEY idx_biz_field_time (biz_type, biz_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE export_apply (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  apply_user_id BIGINT UNSIGNED NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  scope_json JSON NOT NULL,
  reason VARCHAR(500) DEFAULT NULL,
  approve_status TINYINT NOT NULL DEFAULT 0,
  approve_user_id BIGINT UNSIGNED DEFAULT NULL,
  approve_time DATETIME DEFAULT NULL,
  download_url VARCHAR(500) DEFAULT NULL,
  expire_time DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_apply_status_time (approve_status, created_at),
  KEY idx_apply_user_time (apply_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

日志表、访问日志表、同步任务表不建议与业务主表继续建立强物理外键，推荐使用 biz_type + biz_id 逻辑关联，以降低日志写入与归档成本。

## 七、角色模型

| 角色编码 | 角色名称 | 菜单范围 | 数据边界 |
| --- | --- | --- | --- |
| **visitor** | 游客 | 门户公开页 | 仅 S1 公开数据 |
| **candidate** | 求职者 | 求职者中心、活动、签到、同意 | 仅本人数据 |
| **company_admin** | 企业管理员 | 企业后台全菜单 | 仅本企业数据，可见本企业授权范围内候选人数据 |
| **recruiter** | 招聘专员 | 职位管理、投递管理、面试管理 | 仅本企业、本岗位、本流程中数据 |
| **interviewer** | 面试官 | 面试场次、签到结果 | 仅本企业、本人场次范围 |
| **operator** | 运营人员 | 审核、内容、活动、统计 | 默认看聚合；查看明细时进入细粒度授权 |
| **auditor** | 系统审计员 | 审计中心、日志中心、导出审批 | 可看日志与证据，不参与业务操作 |
| **open_client** | 外部对接客户端 | 对接接口 | 仅按签约接口访问，不进入页面系统 |

角色模型支持“一人多角色”，权限取并集，但必须继续受数据域边界和字段级策略约束。

## 八、菜单模型

### 求职者菜单

- 我的资料
- 简历管理
- 投递记录
- 活动报名
- 面试签到
- 同意与撤回

### 企业后台菜单

- 企业认证
- 职位管理
- 投递管理
- 面试场次
- 活动管理
- 导出申请

### 运营后台菜单

- 企业审核
- 职位审核
- 活动管理
- 内容管理
- 统计看板
- 审计中心
- 对接任务中心

菜单权限只解决“是否能进入页面”，不能解决“进入页面后看见什么字段”。字段展示必须由字段级策略继续收口。

## 九、数据域边界

| 边界类型 | 适用角色 | 实现口径 |
| --- | --- | --- |
| **本人边界** | candidate | 服务端自动附加条件：candidate_id = 当前用户 candidate_id |
| **本企业边界** | company_admin / recruiter / interviewer | 服务端自动附加条件：company_id = 当前用户 company_id |
| **本岗位 / 本场次边界** | recruiter / interviewer | 进一步缩小到 job_id、session_id 范围 |
| **运营聚合边界** | operator | 默认只给统计结果；查看明细时进入细粒度授权 |
| **审计边界** | auditor | 可看审计对象与日志，不可修改业务状态 |
| **对接接口边界** | open_client | 按 client_id + api_scope 控制，仅调用签约接口 |

数据域边界必须在服务端查询层强制实现，不能依赖前端筛选。

## 十、字段级权限策略

| 字段级别 | 典型字段 | 实现方式 |
| --- | --- | --- |
| **S1 公开级** | 职位名称、企业名称、活动名称、资讯标题 | 可直接展示 |
| **S2 业务级** | 投递状态、活动报名状态、面试状态 | 按角色和数据域判断 |
| **S3 受限级** | 手机号、邮箱、简历正文、教育经历、工作经历 | 显示 / 脱敏显示 / 隐藏 |
| **S4 高敏级** | 同意证据、实名核验结果、导出范围、字段访问日志 | 仅审计和高授权安全角色可见 |

### ALLOW

当前角色对当前对象具备完整查看权，返回原值。

### MASK

可知晓字段存在，但无权查看原值，例如手机号仅显示部分片段。

### DENY

当前角色不应获知该字段，接口中不返回或返回 null。

maskRule = fieldPolicyEngine.evaluate( roleCodes, dataScope, bizType, fieldName, recordContext ) 返回结果：ALLOW / MASK / DENY

字段级权限必须在后端出参组装层统一处理，不能由前端自行决定是否隐藏。

## 十一、导出审批与高风险动作控制

| 动作 | 是否必须审批 | 实现说明 |
| --- | --- | --- |
| **导出候选人明细** | 是 | 必须创建 export_apply，审批通过后生成一次性下载链接，并记录下载日志。 |
| **导出活动报名名单** | 视字段而定 | 若含个人联系方式或 S3 字段，进入审批。 |
| **候选人发布快照上线** | 建议纳入审核流 | 校验 consent_status 与 publishable_flag，必要时运营审核后生效。 |
| **候选人撤回同意** | 否，但必须强审计 | 撤回后必须记录日志并联动下架。 |
| **对接接口同步失败重试** | 否 | 保留 sync_task 重试轨迹和错误信息。 |

统一原则：能审批的必须审批，不能审批的必须强审计。

## 十二、日志打点位置

### 必须写 audit_log 的动作

- 企业审核、职位审核、活动审核
- 职位创建、编辑、上下架
- 投递状态变更、面试场次创建
- 同意提交、撤回
- 发布快照上线、下线
- 对接接口同步成功/失败的业务状态变化

### 必须写 field_access_log 的动作

- 查看手机号、邮箱
- 查看简历正文、附件
- 查看同意证据材料
- 执行导出前的字段读取

日志打点不能只在 Controller 层，至少应在 Service 层或统一审计切面层补打一层，以覆盖内部调用与批处理任务。

## 十三、权限附加表 DDL

```sql
CREATE TABLE sys_role (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(64) NOT NULL,
  role_name VARCHAR(128) NOT NULL,
  role_type VARCHAR(32) NOT NULL COMMENT 'portal/company/admin/open',
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_menu (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  menu_code VARCHAR(64) NOT NULL,
  menu_name VARCHAR(128) NOT NULL,
  parent_id BIGINT UNSIGNED DEFAULT NULL,
  menu_type VARCHAR(32) NOT NULL COMMENT 'menu/page/api/button',
  route_path VARCHAR(255) DEFAULT NULL,
  api_code VARCHAR(128) DEFAULT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_menu_code (menu_code),
  KEY idx_parent_sort (parent_id, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_role_menu (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT UNSIGNED NOT NULL,
  menu_id BIGINT UNSIGNED NOT NULL,
  UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_user_role (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_type VARCHAR(32) NOT NULL COMMENT 'candidate/company_user/admin_user',
  user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  UNIQUE KEY uk_user_role (user_type, user_id, role_id),
  KEY idx_role_user (role_id, user_type, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_role_data_scope (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT UNSIGNED NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  scope_type VARCHAR(32) NOT NULL COMMENT 'self/company/job/session/aggregate/open_scope',
  scope_value_rule VARCHAR(255) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_role_biz_scope (role_id, biz_type, scope_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_role_field_policy (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT UNSIGNED NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  field_name VARCHAR(128) NOT NULL,
  policy_type VARCHAR(16) NOT NULL COMMENT 'ALLOW/MASK/DENY',
  mask_rule VARCHAR(64) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_role_field_policy (role_id, biz_type, field_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE open_client (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  client_code VARCHAR(64) NOT NULL,
  client_secret_hash VARCHAR(255) NOT NULL,
  source_system VARCHAR(32) NOT NULL,
  api_scope_json JSON NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  last_call_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_client_code (client_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

## 十四、接口 × 权限 × 审批 × 日志矩阵

| 接口分组 | 允许角色 | 数据域控制 | 是否审批 | 是否记 audit_log | 是否记 field_access_log |
| --- | --- | --- | --- | --- | --- |
| **A. 门户公开接口** | visitor / candidate / company_user / operator | 无，仅公开展示层 | 否 | 否 | 否 |
| **B. 求职者中心接口** | candidate | 本人边界 | 提交同意不审批；撤回不审批 | 提交同意、撤回记 | 查看证据或高敏字段时记 |
| **C. 企业后台查询接口** | company_admin / recruiter / interviewer | 本企业 / 本岗位 / 本场次边界 | 普通查询否；导出是 | 状态变更记 | 读取手机号、邮箱、简历正文、附件时记 |
| **D. 运营后台接口** | operator / auditor | 聚合优先；明细授权 | 审核类按流程；导出审批 | 审核、发布、下线记 | 查阅高敏字段时记 |
| **E. 对接接口** | open_client | 按 api_scope 与 biz_type | 否 | 同步成功/失败记 | 通常否；若返回受限字段元数据则记 |

本矩阵作为接口设计、权限策略、审批要求、日志要求的统一收口章节，后续研发和测试以此为准。

## 十五、最终定稿结论

### 本稿已经定下的内容

- 地方人才服务平台一期的数据库分层与分库策略
- 核心业务表、集成表、日志表、审计表、证据表 DDL
- candidate 核心边界字段主归属与统一主控表
- 角色、菜单、数据域、字段级权限、审批、日志打点口径
- 权限附加表 DDL 与接口 × 权限 × 审批 × 日志矩阵

### 本稿的使用方式

- 研发：作为建表、鉴权、出参裁剪、导出审批实现依据
- 测试：作为接口权限、字段脱敏、日志留痕验证依据
- 安全：作为数据访问和高风险动作控制依据
- 法务：作为一期平台对候选人发布、撤回、同步边界的技术实现说明

结论：本文件已形成《第四步骤最终定稿版：数据库与权限一体化设计》，可作为地方人才服务平台一期的联合评审最终稿。

第四步骤最终定稿版：数据库与权限一体化设计
