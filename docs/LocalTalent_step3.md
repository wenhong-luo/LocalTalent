# LocalTalent_step3 修订版

> 来源：`LocalTalent_step3.html`。
> 用途：开发前必读 Markdown 版；保留原文内容，移除 HTML/CSS 展示噪音，并补充开发前必读卡与执行索引。

## 开发前必读卡

- **文件定位**：页面域、接口分组、状态原型与对接接口契约文件。
- **实现约束**：前后端必须使用统一响应、trace 与状态视图；公开层只读取发布快照 DTO。
- **禁止越界点**：禁止真实外部同步客户端；禁止公开层读取原始候选人数据；禁止把对接接口从 stub 扩成业务接入。
- **下游影响**：约束 Controller/API 路径、前端页面状态、OpenAPI 契约、对接接口 stub 和回归测试。
- **自动化验收关注点**：统一响应体、X-Trace-Id、状态视图、对接契约、公开层字段裁剪。

**统一硬边界提醒**：人才服务区仅展示发布快照；对接接口仅 stub；不得输出原始候选人数据；同意、撤回、数据域、字段级权限必须在服务端收口。

## 执行索引

- 第三步骤修订结论
- 术语统一说明
- 页面结构总览
- 页面状态原型
- 接口分组总览
- 对接接口统一契约
- 权限与字段裁剪要求
- 第三步骤结论

第三步骤修订版 · 产品原型结构 + 页面状态原型 + 对接接口统一契约

本稿基于原 LocalTalent_step3 重写修订，保留门户前台、求职者中心、企业后台、运营后台四大页面域与 A/B/C/D/E 接口分组，并重点补齐《页面状态原型补充》与《对接接口统一契约》。本稿聚焦产品、研发、测试共同评审所需的业务结构、状态口径与接口契约，不展开数据库 DDL 与权限实现细节。

### 页面域

门户前台 / 求职者中心 / 企业后台 / 运营后台

### 接口分组

A / B / C / D / E 五组接口

### 本轮重点补齐

页面状态原型 / 对接接口统一契约

### 评审对象

产品 / 研发 / 测试

## 一、第三步骤修订结论

保留内容

四大页面域 + A/B/C/D/E 接口分组

新增内容

页面状态原型补充

新增内容

对接接口统一契约

术语强化

人才服务区 = 发布快照展示区

本稿的目标不是做高保真视觉稿，而是把“页面怎么分区、状态怎么流转、接口怎么统一”定清楚，避免后续研发与测试在状态和接口契约上各自理解。

## 一-A、术语统一说明

### 人才服务区

指前台公开展示候选人发布快照的业务区，不是公共简历库。

### 发布快照

指满足发布条件后进入公开展示链路的最小必要展示副本。

### 原始候选人数据

指联系方式、简历正文、附件、证据材料等未进入公开展示链路的数据。

### 数据域 / 字段级权限

数据域控制“能看哪一批数据”，字段级权限控制“能看哪些字段、看原值还是脱敏值”。

## 二、平台总体产品原型结构

**门户前台**面向游客、求职者。首页 / 职位 / 企业 / 活动 / 资讯 / 人才服务区

→

**求职者中心**面向求职者本人。账号 / 简历 / 投递 / 活动 / 面试签到 / 同意与撤回

→

**企业后台**面向企业与招聘人员。企业认证 / 职位管理 / 投递管理 / 面试场次 / 活动管理

→

**运营后台**面向审核与运营。企业审核 / 职位审核 / 内容管理 / 活动管理 / 统计看板 / 审计中心

## 三、门户前台产品原型

### 页面 1：首页

- 顶部导航：职位、企业、活动、资讯、人才服务、登录注册
- 首屏搜索：关键词 + 城市 + 职位类型
- 推荐区：重点职位 / 重点企业 / 热门活动
- 信息区：政策资讯 / 公告 / 平台介绍

### 页面 2：职位列表 / 详情

- 列表支持城市、行业、薪资、经验筛选
- 详情展示岗位职责、要求、企业信息、投递按钮
- 未登录点击投递先引导注册登录
- 已登录点击投递进入投递确认页

### 页面 3：企业列表 / 详情

- 企业卡片展示认证状态、招聘职位数、企业简介
- 企业详情展示企业介绍、在招职位、活动信息
- 不展示任何候选人明细

### 页面 4：活动列表 / 详情

- 招聘会 / 宣讲会 / 地方人才服务活动分类
- 支持报名按钮、时间地点信息、企业参会信息
- 详情页支持报名与签到引导

### 页面 5：资讯中心

- 政策资讯、平台公告、人才服务内容
- 按栏目、地区、发布时间分类

### 页面 6：人才服务区

- **人才服务区 = 发布快照展示区，不是公共简历库**
- 只展示发布快照，不展示原始候选人数据
- 仅允许 publishable_flag=1 且 consent_status=已同意 的快照进入
- 支持脱敏展示与条件检索

门户前台的核心原则：公开层只展示职位、企业、活动、资讯和“候选人发布快照”，不得直接展示原始候选人数据、联系方式、简历正文、附件和证据材料。

术语统一口径：人才服务区 = 发布快照展示区；发布快照 = 进入公开展示链路的最小必要候选人展示副本；原始候选人数据 = 联系方式、简历正文、附件、证据材料及其他未进入公开展示链路的数据。

## 四、求职者中心产品原型

### 页面 1：账号与安全

- 手机号/邮箱登录
- 基础资料维护
- 实名核验入口

### 页面 2：简历管理

- 创建、编辑、预览简历
- 支持多份简历
- 上传附件简历

### 页面 3：投递记录

- 查看职位投递状态
- 查看企业反馈、面试通知
- 支持撤销投递策略位预留

### 页面 4：活动报名记录

- 查看已报名活动
- 查看签到入口

### 页面 5：面试签到

- 签到二维码 / 签到码录入
- 签到成功页
- 签到后进入同意页或状态页

### 页面 6：同意与撤回

- 查看同意文本版本
- 勾选同意、二次确认
- 查看同意记录与撤回状态

## 五、企业后台产品原型

### 页面 1：企业入驻与认证

- 企业资料填写
- 证照上传
- 认证状态查询

### 页面 2：职位管理

- 职位创建、编辑、上下架
- 职位审核状态查看
- 职位数据统计

### 页面 3：投递管理

- 查看本企业职位的投递记录
- 筛选、标记处理状态
- 查看授权范围内候选人卡片

### 页面 4：面试场次管理

- 创建面试场次
- 生成签到码
- 查看签到结果

### 页面 5：活动管理

- 参与招聘会、活动报名管理
- 查看活动报名求职者名单（按规则）

### 页面 6：导出申请

- 对投递结果或活动名单发起导出审批
- 查看审批状态

企业后台的关键要求是“只处理本企业边界内的数据”。第三步骤不展开权限实现，但页面与接口设计必须已经体现企业边界与无权限状态。

## 六、运营后台产品原型

### 页面 1：企业审核

- 审核企业入驻与认证
- 查看企业基本资料、证照

### 页面 2：职位审核

- 审核职位发布内容
- 执行上下架和违规处理

### 页面 3：活动与内容管理

- 活动发布、活动审核
- 资讯、公告、首页栏目配置

### 页面 4：统计看板

- 职位数、企业数、投递数、签到数、同意数、发布数
- 默认看聚合统计

### 页面 5：审计中心

- 审计日志、字段访问日志、导出记录
- 异常查询与追溯

### 页面 6：对接任务中心

- 查看对接接口同步任务
- 查看重试状态、映射状态、失败原因

## 七、关键业务流程原型

**流程 1：投递**浏览职位 → 登录/注册 → 选择简历 → 投递成功投递后进入企业投递池

→

**流程 2：面试签到**企业创建场次 → 生成签到码 → 候选人签到签到后进入同意页或结果页

→

**流程 3：同意发布/同步**实名核验 → 勾选同意 → 二次确认 → 写入同意记录触发发布控制或对接接口回调

## 八、《页面状态原型补充》

| 场景 | 状态 | 页面表现 | 操作与说明 |
| --- | --- | --- | --- |
| **企业认证** | 待审核 | 显示“认证审核中”状态条，不开放职位正式发布 | 允许查看提交资料；允许补充资料入口预留 |
| **企业认证** | 已驳回 | 显示驳回原因、重新提交入口 | 禁止继续使用需认证后才能使用的功能 |
| **企业认证** | 已通过 | 显示“已认证”标识 | 开放职位发布、场次创建等正式能力 |
| **职位** | 待审核 | 职位管理列表显示“待审核” | 前台不可见；允许企业编辑或撤回 |
| **职位** | 在线 | 前台可见；职位详情可投递 | 支持查看数据统计、手动下线 |
| **职位** | 下线 | 前台不可见；后台保留记录 | 可重新上线或归档 |
| **候选人发布控制** | 已同意 | 求职者中心显示“已同意发布/同步” | 满足规则时允许生成发布快照 |
| **候选人发布控制** | 已撤回 | 求职者中心显示“已撤回” | 平台停止继续发布；对接接口触发撤回回调或下架任务 |
| **候选人发布控制** | 不可发布 | 人才服务区不展示；企业后台不显示公开层候选卡片 | 可能原因：未同意、已撤回、publishable_flag=0 |
| **无权限查看** | 无访问权 | 显示“无权限查看”空态页，不显示敏感字段 | 不得通过前端隐藏但接口仍返回原值 |
| **对接任务** | 同步失败 | 对接任务中心显示失败标识、错误信息、trace_id | 支持查看失败原因；进入重试队列前不可直接标记成功 |
| **对接任务** | 重试中 | 显示“重试中”状态与重试次数 | 达到上限后转失败并告警 |

状态原型的目标不是补视觉细节，而是把“同一对象在不同状态下页面如何表现、允许做什么、不允许做什么”提前定清楚。

状态术语统一：同意 = 候选人完成明确授权并处于有效状态；撤回 = 候选人取消原同意且应触发发布停止或下架联动。

## 九、接口分组总览

| 接口分组 | 服务对象 | 主要功能 |
| --- | --- | --- |
| **A. 门户公开接口** | 游客 / 搜索引擎 / 前台页面 | 首页、职位列表、职位详情、企业列表、活动列表、资讯、人才服务区发布快照 |
| **B. 求职者中心接口** | 求职者 | 注册登录、简历管理、投递、活动报名、签到、同意与撤回 |
| **C. 企业后台接口** | 企业管理员 / 招聘专员 / 面试官 | 企业认证、职位管理、投递管理、面试场次、活动管理、导出申请 |
| **D. 运营后台接口** | 运营人员 / 审计员 | 企业审核、职位审核、内容管理、统计看板、审计日志 |
| **E. 对接接口（预留）** | ATS / 受控候选池 / 未来集成方 | 职位同步、投递同步、同意回调、发布快照同步、映射查询 |

## 十、门户公开接口清单

| 接口 | 方法 | 用途 | 关键入参 | 关键出参 |
| --- | --- | --- | --- | --- |
| /api/portal/home | GET | 首页数据聚合 | city_code | banners, featured_jobs, featured_companies, hot_events, news_list |
| /api/portal/jobs | GET | 职位列表 | keyword, city_code, salary_range, category_code, page | job_list, total |
| /api/portal/jobs/{id} | GET | 职位详情 | id | job_detail, company_summary |
| /api/portal/companies | GET | 企业列表 | keyword, city_code, industry_code, page | company_list, total |
| /api/portal/companies/{id} | GET | 企业详情 | id | company_detail, jobs |
| /api/portal/events | GET | 活动列表 | type_code, city_code, date_range, page | event_list, total |
| /api/portal/news | GET | 资讯列表 | category, page | news_list, total |
| /api/portal/talent-snapshots | GET | 人才服务区发布快照列表 | city_code, category_code, page | snapshot_list, total |

A 组接口只允许输出 S1 字段和“发布快照”最小必要字段，不允许输出原始候选人数据。

## 十一、求职者中心接口清单

| 接口 | 方法 | 用途 | 关键入参 | 关键出参 |
| --- | --- | --- | --- | --- |
| /api/user/register | POST | 注册 | mobile/email, code, password | user_id, token |
| /api/user/profile | GET/PUT | 查看/修改个人资料 | 基础资料字段 | profile |
| /api/resumes | GET/POST | 简历列表/创建简历 | resume payload | resume_list / resume_id |
| /api/resumes/{id} | GET/PUT/DELETE | 查看/编辑/删除简历 | id, resume payload | resume_detail |
| /api/applications | POST | 职位投递 | job_id, resume_id | application_id, status |
| /api/my/applications | GET | 我的投递记录 | page, status | application_list |
| /api/events/{id}/register | POST | 活动报名 | event_id | registration_id |
| /api/interview/signin | POST | 面试签到 | qr_code / session_code | signin_id, redirect_type |
| /api/consents | POST | 提交同意 | candidate_id, consent_scope, consent_version, verify payload | consent_id, consent_status |
| /api/consents/my | GET | 查看同意记录 | page | consent_list |
| /api/consents/{id}/revoke | POST | 撤回同意 | id, reason | revoke_status |

## 十二、企业后台接口清单

| 接口 | 方法 | 用途 | 关键入参 | 关键出参 |
| --- | --- | --- | --- | --- |
| /api/company/apply | POST | 企业入驻申请 | company profile, license files | company_id, auth_status |
| /api/company/jobs | GET/POST | 职位列表/创建职位 | job payload | job_list / job_id |
| /api/company/jobs/{id} | GET/PUT | 查看/编辑职位 | job payload | job_detail |
| /api/company/jobs/{id}/status | POST | 上下架职位 | status | status |
| /api/company/applications | GET | 本企业投递管理 | job_id, status, page | application_list |
| /api/company/interview-sessions | GET/POST | 场次列表/创建场次 | job_id, session_time, location | session_list / session_id |
| /api/company/interview-sessions/{id}/qrcode | POST | 生成签到码 | id | qr_code, expire_time |
| /api/company/events | GET | 活动参与记录 | page | event_join_list |
| /api/company/export-apply | POST | 导出申请 | biz_type, scope_json, reason | apply_id, approve_status |

## 十三、运营后台接口清单

| 接口 | 方法 | 用途 | 关键入参 | 关键出参 |
| --- | --- | --- | --- | --- |
| /api/admin/companies/review | GET/POST | 企业审核列表/审核提交 | company_id, audit_status, memo | review_result |
| /api/admin/jobs/review | GET/POST | 职位审核列表/审核提交 | job_id, audit_status, memo | review_result |
| /api/admin/events | GET/POST/PUT | 活动管理 | event payload | event_detail |
| /api/admin/content | GET/POST/PUT | 资讯与首页配置 | content payload | content_detail |
| /api/admin/dashboard | GET | 统计看板 | date_range, city_code | job_count, company_count, apply_count, sign_count, consent_count, publish_count |
| /api/admin/audit-logs | GET | 审计日志查询 | biz_type, operator_id, date_range | audit_log_list |
| /api/admin/field-access-logs | GET | 字段访问日志查询 | field_name, operator_id, date_range | access_log_list |
| /api/admin/export-approve | POST | 导出审批 | apply_id, approve_status | approve_result |

## 十四、对接接口清单（预留）

| 接口 | 方法 | 用途 | 关键入参 | 关键出参 |
| --- | --- | --- | --- | --- |
| /api/open/v1/jobs/sync | POST | ATS 同步职位 | source_system, company_external_id, job_external_id, job payload, sync_version | local_job_id, sync_status |
| /api/open/v1/applications/sync | POST | ATS 同步投递记录 | application_external_id, job_external_id, candidate_external_id, source_type | local_application_id, sync_status |
| /api/open/v1/consents/callback | POST | 平台回传同意状态 | candidate_id, consent_status, consent_scope, consent_version, revoke_status | callback_status |
| /api/open/v1/candidates/publishable-sync | POST | 候选池推送发布快照 | candidate_id, publishable_flag, visibility_scope, snapshot_json, sync_version | local_snapshot_id, sync_status |
| /api/open/v1/mappings/query | GET | 外部映射查询 | source_system, local_biz_type, local_id | external_id, version |

E 组对接接口只交换“业务状态、映射关系、发布快照、授权状态”，不交换不必要的原始候选人数据和高敏证据材料。

## 十五、《对接接口统一契约》

| 契约项 | 统一规则 |
| --- | --- |
| **接口版本号** | 统一放入 URI，例如 `/api/open/v1/...`；重大变更再升 v2。 |
| **签名方式** | 采用 client_code + timestamp + nonce + body_hash 的签名机制；服务端校验签名与时间窗。 |
| **时间戳** | 请求头必须包含 `X-Timestamp`；默认允许时间窗建议 5 分钟。 |
| **trace_id** | 请求头必须包含 `X-Trace-Id`；若未传入，平台生成并回传。 |
| **幂等键** | 写接口必须包含 `X-Idempotency-Key`；服务端按接口类型 + 幂等键做幂等控制。 |
| **错误码规范** | 统一分为：认证类、签名类、参数类、业务状态类、权限类、重试类、系统类错误码。 |
| **请求体规范** | 统一 JSON；必须带 `source_system`、业务主键、版本号、必要状态字段。 |
| **响应结构** | 统一返回：`code / message / trace_id / data`。 |
| **回调重试策略** | 失败后进入重试队列；建议按 1min / 5min / 15min / 1h 退避；达到上限后转人工处理或失败告警。 |
| **版本兼容策略** | 新增字段向后兼容；删除字段或改含义必须升版本。 |

建议的对接接口请求头：X-Client-Code: partner_xxx X-Timestamp: 2026-04-12T10:00:00+08:00 X-Nonce: 8f3d1ab2 X-Trace-Id: trace-20260412-000001 X-Idempotency-Key: idem-20260412-apply-000001 X-Signature: sha256(...) 统一响应体：{ "code": "0", "message": "success", "trace_id": "trace-20260412-000001", "data": { ... } }

统一契约的目标，是让 E 组对接接口从“接口清单”升级成“真正可对接的接口设计基础”。

## 十六、修订版结论

### 本稿已经明确

四大页面域与 A/B/C/D/E 五组接口继续保留。

### 本稿新增补齐

页面状态原型补充、对接接口统一契约。

### 本稿术语统一

人才服务区 = 发布快照展示区，不是公共简历库。

结论：本稿已形成《LocalTalent_step3 修订版》，可以作为产品、研发、测试对 step3 的统一修订版本，并与 step4 最终定稿版继续保持一致。

LocalTalent_step3 修订版
