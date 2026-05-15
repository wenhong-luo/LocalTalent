# LocalTalent 三期 Runbook：灰度试运营工程治理与上线演练

> 本 Runbook 对应 Prompt 31（P3-6）。它只描述三期小范围真实试运营的工程治理、演练和退出条件，不代表公网正式运营。

## 1. 当前阶段

- 三期目标：小范围真实试运营。
- 当前轮次：Prompt 31（P3-6）灰度试运营工程治理与上线演练已完成，三期主线 Prompt 25-31 已收口；Prompt 27（P3-2）后续补丁 `V0018`、`V0019`、`V0020` 和求职者个人中心首页高保真增强已完成；Prompt 28（P3-3）补充完成企业风采私有真实图片上传、企业基本资料 Logo 私有上传和招聘者受控搜索简历 Prompt 1-6。
- 当前能力：三期文档、配置模板、feature flag 注册表、静态闸门脚本、OIDC/SSO、candidate 私有闭环、首次完善简历 onboarding 状态持久化、candidate 本人私有简历附件上传、candidate 本人私有安全规则版简历优化建议、求职者个人中心首页高保真布局、company 私有工作台、company 私有受控发布快照搜索后端接口、operator/auditor 运营后台、推荐位/风险审核、审计/导出/风控安全收口、灰度 smoke/load、备份恢复演练和灰度记录模板。
- 当前非目标：不宣布公网正式上线；不接真实短信、微信、小程序、App、地图、视频、直播、支付、ATS/RPA；不开放公共简历库、受控找人才、联系解锁或会员商业化。

## 2. 必读文件

三期开发和灰度准备必须先读：

- `docs/Phase_III/00_PHASE_III_BOUNDARY.md`
- `docs/Phase_III/LocalTalent_三期总规划.md`
- `docs/Phase_III/LocalTalent_phase3_step1.md` 至 `LocalTalent_phase3_step6.md`
- `docs/Phase_III/三期研发实施蓝图与任务拆解.md`
- `docs/Phase_III/三期研发提示词.md`
- `docs/Phase_III/三期灰度试运营使用说明.md`
- `docs/Phase_III/三期主线收口验收报告.md`
- `docs/Phase_III/feature_flags.md`
- `docs/Phase_III/灰度上线暂停回滚检查单.md`
- `docs/Phase_III/灰度备份恢复演练记录模板.md`
- `docs/Phase_III/灰度故障记录模板.md`
- `docs/Phase_III/招聘者搜索简历分阶段开发规划.md`（后续专项规划；Prompt 1-6 已完成，当前搜索简历基础专项已收口，但仍不是公共简历库或联系解锁）

一二期文件只作为已完成基线和边界回顾，不得修改既定事实。

## 2.1 三期后续补丁记录

| 补丁 | 归属 | 说明 | 边界 |
| --- | --- | --- | --- |
| `V0018__create_candidate_resume_onboarding.sql` | Prompt 27（P3-2）求职者真实闭环生产化 | 新增 `candidate_resume_onboarding`，将首次完善简历 1-3 页的 `not_started/basic_saved/detail_saved/completed` 与 `basic/detail/done` 状态落为服务端权威状态；无记录时兼容旧 `candidate_resume` 完整度。 | 只存流程状态；不存手机号、邮箱、简历正文、附件 key、AI 文本或原始 JSON；该迁移不包含个人中心首页 UI（后续已由独立补丁完成），不做 AI、短信、微信、小程序或 App。 |
| `V0019__extend_candidate_resume_attachment_metadata.sql` | Prompt 27（P3-2）求职者真实闭环生产化 | 扩展 `candidate_resume` 私有附件元数据，配合 `phase3.resume_attachment_upload` 支持本人私有简历附件上传、下载、替换、删除。 | 只支持 candidate 本人私有附件；object key 仅服务端保存，不返回前端、不进入公开门户、人才服务区、搜索、sitemap、日志明文或导出旁路；不做企业查看附件、附件解析、AI 优化，也不改变已完成的个人中心首页布局。 |
| `V0020__create_candidate_resume_ai_suggestion.sql` | Prompt 27（P3-2）求职者真实闭环生产化 | 新增 candidate 私有简历优化建议任务与建议项表，配合 `phase3.resume_ai_assist` 支持本地规则生成、最近任务读取、逐条手动应用和忽略。 | 只做安全规则版建议；不接真实 LLM/外部模型，不保存完整 prompt、模型原文、手机号、邮箱、附件 object key、证据或原始 JSON；建议不会自动发布到人才服务区。 |
| `V0021__create_company_style_image.sql` | Prompt 28（P3-3）企业招聘工作台生产化 | 新增企业风采私有图片表，配合 `phase3.company_style_upload` 支持本企业图片上传、鉴权预览、排序和删除。 | 只在 `/company` 企业管理 > 企业风采私有域可见；object key 仅服务端保存，不返回前端、不进入公开企业主页、推荐位、搜索、sitemap、导出旁路或日志明文；公开展示待后续图片审核专项。 |
| `V0022__create_company_logo_asset.sql` | Prompt 28（P3-3）企业招聘工作台生产化 | 新增企业 Logo 私有资产表，配合 `phase3.company_logo_upload` 支持基本资料页 Logo 上传、鉴权预览、替换和删除。 | 只在 `/company` 企业管理 > 基本资料私有域可见；object key 仅服务端保存，不返回前端、不进入公开企业列表、公开主页、推荐位、搜索、sitemap、导出旁路或日志明文；公开展示待后续图片审核专项。 |
| `V0026__extend_publish_snapshot_resume_search_fields.sql` | Prompt 28（P3-3）企业招聘工作台生产化补充：招聘者受控搜索简历 Prompt 2 | 为 `candidate_publish_snapshot` 增加受控搜索安全筛选生成列与索引，并配合 `phase3.company_resume_search` 支持 company 私有 `GET /api/company/workbench/resume-search` 后端接口。 | 只查候选人授权发布快照；默认关闭；仅已认证 company 且具备 `company.resume-search.read` 可访问；响应只返回安全摘要，不返回 candidate_id、手机号、邮箱、微信、附件、证据、完整简历正文或原始 JSON。 |
| `V0027__create_company_resume_snapshot_report.sql` | 招聘者受控搜索简历 Prompt 4 | 新增 company 私有简历快照举报表，配合详情抽屉和举报弹窗将举报原因、备注摘要、状态和 trace 写入风控/审计链。 | 只存摘要；不存联系方式、附件、证据、完整简历或原始 JSON。 |
| `V0028__create_company_resume_access_request.sql` | 招聘者受控搜索简历 Prompt 5 | 新增 company 私有受控访问申请表，下载简历、查看联系方式、聊一聊、面试邀请均只记录申请。 | 不生成完整简历、不展示联系方式、不创建真实职聊、不发送外部通知、不接支付或套餐扣点。 |
| 求职者个人中心首页高保真增强 | Prompt 27（P3-2）求职者真实闭环生产化 | 增强 `/candidate/center` 为接近 `docs/page/4、会员中心.png` 的个人中心首页，包含个人横幅、统计卡、左侧菜单、简历状态卡、优选服务占位、职位推荐区、底部栏和右侧客服占位。 | 不新增迁移和后端业务能力；“会员中心”只作为个人中心视觉参考，不实现会员商业化、真实支付、联系解锁、公共简历库或真实短信/微信/小程序/App；页面继续 `noindex`，只复用 candidate 本人私有接口。 |

补充说明：`docs/Phase_III/招聘者搜索简历分阶段开发规划.md/html` 已记录后续 6 轮规划，Prompt 1-6 均已完成；`scripts/check_company_resume_search_acceptance` 已作为专项验收脚本接入三期安全收口。当前搜索简历基础能力仍限定为 company 私有域内的受控发布快照搜索，不得开放公共简历库、联系解锁或完整简历下载。

## 3. 环境模板

三期配置模板位于：

```bash
.env.phase3.example
```

使用方式：

```bash
cp .env.phase3.example .env.phase3.local
```

注意：

- 模板只包含占位值，不包含真实密钥。
- `local`、`staging`、`gray` 必须使用各自的真实配置文件，不得提交。
- OIDC、JWT fallback、MySQL、Redis、MinIO、Next API base URL、导出 TTL、灰度比例、烟测目标、监控阈值必须按环境显式设置。
- `gray` 环境本地 JWT fallback 默认关闭，只允许白名单应急。

## 4. Feature Flag

三期 flag 注册表位于：

```bash
docs/Phase_III/feature_flags.md
```

默认要求：

- 所有三期新增能力默认关闭。
- `LOCALTALENT_PHASE3_RESUME_ATTACHMENT_UPLOAD=false` 默认关闭；只有在 `LOCALTALENT_PHASE3_CANDIDATE_CLOSURE=true` 且明确演示 P3-2 附件流时才开启。
- `LOCALTALENT_PHASE3_RESUME_AI_ASSIST=false` 默认关闭；只有在 `LOCALTALENT_PHASE3_CANDIDATE_CLOSURE=true` 且明确演示 P3-2 私有简历规则建议时才开启。该能力不接真实 LLM，不调用外部模型，不上传原始候选人数据。
- flag 不得绕过 RBAC、数据域、字段级权限、导出审批或审计。
- 风险池能力不得通过 flag 偷跑。
- 打开任意 flag 前必须记录负责人、影响范围、观察窗口、回滚点和验收命令。

打开任意 flag 前，至少执行：

```bash
./scripts/check_phase3_boundary
./scripts/check_phase3_ops
./scripts/check_phase3_security_acceptance
./scripts/check_phase3_gray_acceptance
./scripts/check_portal_snapshot_fields
./scripts/check_boundary
```

## 5. local / staging / gray 配置

### 5.1 本地开发入口

`staging` 即预发环境，必须在进入 `gray` 前完成配置、备份恢复、回滚、smoke/load 和监控告警联通。

| 环境 | 用途 | 必须满足 | 禁止事项 |
| --- | --- | --- | --- |
| `local` | 开发和脚本验证 | 可使用 compose MySQL/Redis/MinIO；可用演示数据；三期 flag 默认关闭。 | 不写真实密钥，不连接真实外部供应商。 |
| `staging` | 灰度前演练 | 使用脱敏样本数据；完成备份恢复、回滚、smoke/load、监控告警联通。 | 不用生产明文数据，不绕过审批和审计。 |
| `gray` | 小范围真实试运营 | 白名单用户进入；feature flag 分批开启；监控、告警、暂停、回滚可用。 | 不直接变成公网正式运营，不开放风险池能力。 |

灰度前必须确认：

- `LOCALTALENT_ENV=gray`。
- 所有未计划开启的三期业务 flag 仍为 `off`。
- OIDC issuer、client id、callback URL 使用环境变量，不写入仓库。
- token、secret、claims、手机号、邮箱、简历正文不得进入日志。
- 导出 TTL、下载次数、导出频控必须显式配置。
- MinIO 或对象存储 bucket 不复用本地演示 bucket。

## 6. 灰度发布、暂停与回滚

### 6.1 灰度发布步骤

1. 在 staging 部署候选版本。
2. 执行 `./scripts/run_all`。
3. 执行 `./scripts/smoke_phase3`。
4. 执行 `./scripts/load_phase3_public`。
5. 执行 `./scripts/backup_phase3_mysql` 与 `restore_phase3_mysql_drill`。
6. 填写 `docs/Phase_III/灰度上线暂停回滚检查单.md`。
7. 在 gray 环境对白名单账号或小流量打开目标 flag。
8. 观察 30-60 分钟；若健康、5xx、延迟、登录、导出、审计、撤回下线、openapi stub 均稳定，再扩大灰度。

### 6.2 暂停条件

出现以下任一情况，应暂停扩大灰度，必要时关闭相关 flag：

- 公开门户 5xx 或延迟持续超过阈值。
- 登录失败率、403/401 异常波动。
- 导出审批、短期下载、审计链路异常。
- 撤回后人才服务区或推荐位仍可见。
- 字段黑名单、IDOR/BOLA、auditor 只读、风险池阻断任一失败。
- 备份恢复、回滚演练或告警联通失败。

### 6.3 回滚原则

- feature flag 是第一回滚手段；应用版本回滚是第二手段。
- 数据库结构优先 roll-forward 修复，不直接回滚已应用结构。
- 数据修复必须保留审计记录。
- 对外部能力仍为 stub 或占位，不应出现供应商级回滚复杂度。

## 7. 备份恢复演练

灰度前必须证明数据库可备份、可恢复、可验证。

```bash
./scripts/backup_phase3_mysql
LOCALTALENT_PHASE3_BACKUP_FILE=/tmp/localtalent-phase3-backups/<dump>.sql ./scripts/restore_phase3_mysql_drill
```

演练要求：

- 备份默认写入 `/tmp/localtalent-phase3-backups`，不得提交。
- 恢复默认导入 scratch 数据库 `localtalent_restore_drill`，不得覆盖 `localtalent`。
- 恢复后校验 Flyway、核心表和核心记录数。
- 演练结束默认清理 scratch 数据库；如需保留，显式设置 `KEEP_DRILL_DB=true`。
- 结果记录到 `docs/Phase_III/灰度备份恢复演练记录模板.md` 的副本中。

## 8. 对象存储生命周期与导出文件保留策略

三期灰度仍使用短期下载和最小保留：

- 导出文件默认短期保留，建议 7 天内清理。
- 预签名 URL 默认短期有效，建议 15 分钟。
- 单个导出申请默认最多发放一次下载链接。
- 导出文件不得包含手机号、邮箱、简历正文、附件对象 key、证据、原始 JSON 或审核材料。
- 清理任务失败必须有人工巡检或告警入口。
- MinIO/对象存储 bucket 不复用本地演示 bucket。

## 9. 日志脱敏、监控告警与故障排查

### 9.1 日志脱敏

不得在日志、错误响应、审计摘要、openapi 日志或 URL 中输出：

- token、secret、client_secret、id_token、access_token、refresh_token、claims、signature、nonce。
- 手机号、邮箱、简历正文、附件对象 key、证据、原始 JSON。
- 营业执照附件、审核材料、报名名单、签到证据。

### 9.2 最小监控指标

灰度至少关注：

- `/health` 成功率。
- 公开门户 5xx 与 P95 延迟。
- 登录成功率、登录失败率、OIDC callback 失败率。
- 401/403 权限拒绝异常波动。
- 导出审批失败率、生成失败率、短期下载失败率。
- `audit_log`、`field_access_log`、`open_api_log` trace 串联异常。
- 撤回后人才服务区或推荐位仍可见异常。
- openapi stub 调用失败率与签名错误率。

### 9.3 最小告警门槛

| 指标 | 建议门槛 | 处理 |
| --- | --- | --- |
| `/health` 连续失败 | 3 次 | 暂停灰度并检查应用/DB/依赖。 |
| 公开门户 5xx | 5 分钟内 > 1% | 暂停扩大灰度。 |
| 公开门户 P95 | 连续 10 分钟 > 1500ms | 降低灰度比例并排查慢查询/依赖。 |
| 登录失败率 | 连续 10 分钟 > 5% | 检查 OIDC、fallback、时钟和重定向。 |
| 导出失败率 | 连续 10 分钟 > 3% | 暂停导出申请入口。 |
| 审计 trace 缺失 | 任一关键写动作缺失 | 暂停相关写入口。 |
| 撤回后仍可见 | 任一案例 | 立即关闭相关推荐/快照入口并追踪修复。 |

告警不是为了吓人，是为了别让凌晨三点的你成为唯一监控系统。

## 10. 容量烟测

公开门户灰度读流量烟测：

```bash
./scripts/load_phase3_public
```

默认目标：

- 公开门户核心页面可在小流量下稳定返回。
- 默认轮次和并发较低，不等同公网正式压测。
- 建议灰度目标：公开门户读请求 120 QPS 以内，私有中心/后台 25 QPS 以内，导出 worker 小并发。

关键 smoke：

```bash
./scripts/smoke_phase3
```

覆盖：

- 后端 `/health`。
- 首页、找工作、找企业、人才服务区、招聘会、资讯、HR 工具箱、登录页。
- 公开 API。
- candidate/company/admin 私有入口未授权阻断。
- 如提供 token，再验证对应私有 overview 只读接口。

## 11. 上线、暂停、回滚检查单

灰度上线、暂停、回滚必须复制并填写：

```bash
docs/Phase_III/灰度上线暂停回滚检查单.md
```

故障必须复制并填写：

```bash
docs/Phase_III/灰度故障记录模板.md
```

检查单必须包含：

- commit、环境、负责人、观察窗口、回滚点。
- flag 状态和白名单范围。
- 备份恢复演练结果。
- smoke/load 结果。
- 监控告警联通结果。
- 暂停和回滚步骤是否演练通过。

## 12. 灰度 KPI 与试运营退出条件

### 12.1 灰度 KPI

| KPI | 建议门槛 |
| --- | --- |
| 公开门户 API 5xx | < 0.5% |
| 公开门户 P95 | < 1500ms |
| 私有中心 P95 | < 2000ms |
| 登录成功率 | > 95% |
| 导出生成成功率 | > 97% |
| 审计 trace 完整率 | 100% |
| 撤回后公开不可见 | 100% |
| 字段黑名单 | 100% PASS |

### 12.2 试运营退出条件

满足以下条件，才可以进入四期正式上线治理规划：

- 灰度连续稳定运行不少于 7 天。
- 核心 KPI 达标。
- 无 P0/P1 安全问题。
- 备份恢复、回滚演练、告警联通和容量烟测都有记录。
- 真实 candidate、company、operator、auditor 的关键路径均完成验收。

## 13. 可选 K8s 说明

三期只允许把 K8s 作为未来无状态应用层进阶选项：

- 可考虑 backend/frontend 无状态部署的健康检查、就绪检查、启动探针和滚动发布。
- MySQL、Redis、MinIO 状态层不在 Prompt 31 做迁移。
- 不新增 Helm chart 或 K8s manifests。
- 正式迁移状态层必须单独评审备份、恢复、监控、权限、密钥和数据一致性。

## 14. 验收命令

Prompt 31 必跑：

```bash
./scripts/check_phase3_gray_acceptance
./scripts/check_phase3_boundary
./scripts/check_phase3_ops
./scripts/check_phase3_security_acceptance
./scripts/check_portal_snapshot_fields
./scripts/check_boundary
```

灰度演练：

```bash
./scripts/smoke_phase3
./scripts/load_phase3_public
./scripts/backup_phase3_mysql
LOCALTALENT_PHASE3_BACKUP_FILE=/tmp/localtalent-phase3-backups/<dump>.sql ./scripts/restore_phase3_mysql_drill
```

全量回归：

```bash
cd backend && ./mvnw test
cd frontend && npm test && npm run build
./scripts/run_all
```

## 15. 硬边界复述

- 人才服务区仅展示发布快照。
- 原始候选人数据不得公开。
- 对接接口仅 stub。
- 公共简历库、受控找人才、联系解锁、真实支付、真实短信、真实微信、小程序、App、真实地图、视频、直播、ATS、RPA 仍在风险池，不进入三期主线。
