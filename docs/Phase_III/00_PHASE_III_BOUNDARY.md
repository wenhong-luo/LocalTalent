# LocalTalent Phase-3 Master Boundary

## 开发前必读卡

- **文件定位**：三期研发的主边界文件；后续 Step、蓝图、提示词、Prompt 开发和验收都必须先读本文件。
- **三期目标**：把一期合规安全底座和二期高保真门户，推进为可让真实求职者、企业和运营人员小范围试用的业务系统。
- **上线口径**：三期目标是灰度试运营，不是公网正式运营，也不是完整商业化人才网。
- **禁止越界点**：禁止公共简历库；禁止受控找人才、联系解锁、会员支付、真实短信、真实微信、小程序、App、真实地图、视频、直播、真实 ATS/RPA 或受控候选池同步。
- **实现纪律**：三期规划阶段只做文档；进入开发阶段后，每轮只执行当前 Prompt；不得修改一期 Prompt 0-14 和二期 Prompt 15-24 的既定事实。

**统一硬边界提醒**：人才服务区仅展示发布快照；对接接口仅 stub；不得公开原始候选人数据；同意、撤回、数据域、字段级权限、审计必须在服务端收口。

## 1. 三期文件状态与权威读取顺序

三期后续所有规划和研发必须按以下顺序读取：

1. `docs/Phase_III/00_PHASE_III_BOUNDARY.md`。
2. `docs/Phase_III/三期研发方向与上线阶段判断.md`。
3. `docs/Phase_III/LocalTalent_三期总规划.md`。
4. `docs/Phase_III/LocalTalent_phase3_step1.md`。
5. `docs/Phase_III/LocalTalent_phase3_step2.md`。
6. `docs/Phase_III/LocalTalent_phase3_step3.md`。
7. `docs/Phase_III/LocalTalent_phase3_step4.md`。
8. `docs/Phase_III/LocalTalent_phase3_step5.md`。
9. `docs/Phase_III/LocalTalent_phase3_step6.md`。
10. `docs/Phase_III/三期研发实施蓝图与任务拆解.md`。
11. `docs/Phase_III/三期研发提示词.md`。
12. `docs/Phase_II/00_PHASE_II_BOUNDARY.md`。
13. `docs/Phase_II/二期研发提示词.md`。
14. `docs/Phase_II/二期演示使用说明.md`。
15. `docs/Phase_II/74cms_42模板开发分析报告.md`。
16. `docs/Phase_II/74cms_42_templates/`。
17. `docs/Phase_I/00_MASTER_BOUNDARY.md`。
18. `docs/Phase_I/一期研发提示词.md`。
19. `docs/Phase_I/一期研发实施蓝图与任务拆解.md`。

当前三期规划状态：

| 文件 | 状态 | 用途 |
| --- | --- | --- |
| `00_PHASE_III_BOUNDARY.md` | 当前文件 | 三期目标、试运营边界、风险禁区、研发纪律总约束。 |
| `三期研发方向与上线阶段判断.md/html` | 已完成（决策备忘录） | 解释三期为什么做、还需要几期上线、哪些能力暂缓；作为三期方向判断依据。 |
| `LocalTalent_三期总规划.md/html` | 已完成（三期主规划/作战手册） | 承接方向判断，细化阶段拆分、工程默认项、投喂节奏与灰度试运营闸门；后续蓝图和 Prompt 拆解以此为主要依据。 |
| `LocalTalent_phase3_step1.md/html` | 已完成 | 三期产品目标与试运营闭环总设计。 |
| `LocalTalent_phase3_step2.md/html` | 已完成 | 求职者真实闭环生产化。 |
| `LocalTalent_phase3_step3.md/html` | 已完成 | 企业招聘工作台生产化。 |
| `LocalTalent_phase3_step4.md/html` | 已完成 | 运营后台生产化与门户运营化。 |
| `LocalTalent_phase3_step5.md/html` | 已完成 | 审计、导出、风控与安全收口。 |
| `LocalTalent_phase3_step6.md/html` | 已完成 | 灰度试运营工程治理与上线演练。 |
| `三期研发实施蓝图与任务拆解.md/html` | 已完成 | 承接 Step1-Step6，整理 P3-0 至 P3-6 实施路线、任务拆解、测试矩阵和灰度闸门。 |
| `三期研发提示词.md/html` | 已完成 | 将 P3-0 至 P3-6 拆成 Prompt 25 至 Prompt 31 的可复制投喂卡片；三期代码开发必须从 Prompt 25 开始。 |

定位说明：

- `三期研发方向与上线阶段判断.md/html` 保留为背景判断和上线阶段备忘，不再作为执行主文件。
- `LocalTalent_三期总规划.md/html` 是三期主规划与作战手册；`三期研发实施蓝图与任务拆解.md/html` 和 `三期研发提示词.md/html` 已继承它的阶段拆分和工程默认项。

## 2. 一期与二期继承基线

三期不是从零开始，也不是继续堆静态页面。

| 阶段 | 已完成事实 | 三期继承方式 |
| --- | --- | --- |
| 一期 | 合规安全底座、招聘基础闭环、同意/撤回、发布快照、企业/职位状态机、投递面试、导出审批、对接 stub、审计中心、CI/Runbook。 | 不重做底座；所有新增能力继续继承权限、数据域、字段级权限、审计和 trace。 |
| 二期 | 高保真门户：首页、找工作、找企业、人才服务区、招聘会、资讯、HR 工具箱、登录注册入口、P1 公开频道补齐、演示数据和总闸门。 | 不继续堆静态频道；重点转向真实业务可用和后台可运营。 |

三期的核心问题是：

```text
真实求职者能不能维护简历、投递、看状态？
真实企业能不能认证、发职位、处理投递、邀约面试？
真实运营能不能审核、配置、查审计、处理风险？
```

## 3. 三期目标与非目标

### 三期目标

- 真实用户闭环生产化：求职者注册后能维护简历、投递、收藏、订阅、查看面试与通知。
- 企业招聘工作台生产化：企业能维护资料、提交认证、发布职位、处理投递、邀约面试、申请导出。
- 运营后台生产化：运营能审核企业、职位、内容、活动，配置推荐位，查看审计和风险。
- 公开门户运营化：二期高保真页面从演示数据逐步转为后台配置和真实状态驱动。
- 试运营工程收口：预发环境、备份恢复、日志脱敏、监控告警、上线/回滚 Runbook。

### 三期非目标

- 不做公共简历库。
- 不做受控找人才。
- 不做联系解锁。
- 不做会员商业化和真实支付。
- 不接真实短信、微信、小程序、App。
- 不接真实地图、视频、直播。
- 不接真实 ATS/RPA 或受控候选池同步。
- 不让 AI 或外部模型直接处理原始候选人数据。

## 4. 强制边界

### 人才服务区

- 唯一公开数据源仍是 `candidate_publish_snapshot`。
- 只能展示发布快照白名单字段。
- 候选人撤回同意后，人才服务区必须不可见。
- 不得出现“查看完整简历”“联系TA”“解锁联系方式”等公共简历库能力；招聘者搜索简历只允许按 `docs/Phase_III/招聘者搜索简历分阶段开发规划.md` 逐轮实现 company 私有域内的受控发布快照搜索，且不得直接展示联系方式、完整简历、附件或原始候选人数据。

### 求职者中心

- 求职者中心是本人私有域，可展示本人简历、投递、收藏、订阅、面试、通知、同意/撤回状态。
- 状态必须由服务端返回，前端不得推断 consent、publish、application 或 interview 状态。
- 原始候选人数据不得进入公开门户、搜索索引、日志、缓存或导出文件。

### 企业中心

- 企业只能访问本企业资料、职位、投递、面试、导出申请。
- 企业未认证通过不得上线职位。
- 投递池不是公共简历库，不得泄露未授权手机号、邮箱、附件、证据或简历正文。
- 所有含对象 ID 的接口必须覆盖 IDOR/BOLA 测试。

### 运营后台

- `operator` 可在授权范围内写操作。
- `auditor` 只读，不得调用任何写接口。
- 审核、下线、软删除、推荐位调整、风险处理必须写 `audit_log`。
- 查看敏感或高风险字段必须写 `field_access_log`。
- 审计中心响应必须脱敏。

### 外部能力

- 对接接口继续 stub。
- 真实短信、微信、小程序、App、地图、视频、直播、支付、ATS/RPA 都必须先走风险准入，不得在三期主线偷跑。

## 5. 三期灰度试运营完成口径

三期完成后，可以进入小范围灰度试运营，但还不等于公网正式运营。

三期试运营至少满足：

- 真实 candidate 可注册、登录、维护简历、投递、收藏、订阅、查看面试与通知，并可便捷撤回同意。
- 真实 company 可注册、提交认证、维护企业资料、发布职位、处理投递、邀约面试、申请导出。
- operator 可审核企业、职位、内容、活动，配置公开门户推荐位，处理风险。
- auditor 可只读审计与运营数据，不可写。
- 首页、找工作、找企业、人才服务区、招聘会、资讯、工具箱等公开页面由真实状态和后台配置驱动。
- 边界脚本、后端测试、前端测试、构建、Runbook、演示数据与验收清单全绿。

公网正式运营还需要四期继续补：压测、监控告警、备份恢复演练、客服工单、合规材料、上线/回滚演练和生产治理。

## 6. 后续规划建议

三期 Step1-Step6、实施蓝图和研发提示词已经完成。Prompt 25（P3-0）三期基线冻结与灰度闸门、Prompt 26（P3-1）认证与身份映射、Prompt 27（P3-2）求职者真实闭环生产化、Prompt 28（P3-3）企业招聘工作台生产化、Prompt 29（P3-4）运营后台生产化与门户运营化、Prompt 30（P3-5）审计、导出、风控与安全收口、Prompt 31（P3-6）灰度试运营工程治理与上线演练已完成，三期主线 Prompt 25-31 已收口。

三期后续补丁已完成：

- `V0018__create_candidate_resume_onboarding.sql` 属于 Prompt 27（P3-2）求职者真实闭环生产化的补充，用于把首次完善简历 1-3 页的 `onboarding_status/current_step/completion_score` 持久化为服务端权威状态。该补丁只服务 candidate 本人私有域，不存手机号、邮箱、简历正文、附件对象 key、AI 文本、证据或原始 JSON；该迁移不包含个人中心首页 UI（后续已由独立补丁完成），不做 AI 优化、短信、微信、小程序或 App，不改变人才服务区发布快照边界。
- `V0019__extend_candidate_resume_attachment_metadata.sql` 属于 Prompt 27（P3-2）求职者真实闭环生产化的补充，用于支持 candidate 本人私有简历附件上传、下载、替换、删除。该补丁默认由 `phase3.resume_attachment_upload=false` 关闭；附件 object key 仅服务端保存，不返回前端、不进入公开门户、人才服务区、搜索、sitemap、日志明文或导出旁路；不做企业查看附件、附件解析、AI 优化、真实短信/微信/小程序/App，也不改变已完成的个人中心首页布局。
- `V0020__create_candidate_resume_ai_suggestion.sql` 属于 Prompt 27（P3-2）求职者真实闭环生产化的补充，用于支持 candidate 本人私有域内的安全规则版简历优化建议。该补丁默认由 `phase3.resume_ai_assist=false` 关闭；只使用服务端规范化 DTO 生成规则建议，由本人逐条手动应用或忽略；不接真实 LLM/外部模型，不上传原始候选人数据，不保存完整 prompt、模型原文、手机号、邮箱、附件 object key、证据或原始 JSON，不自动发布到人才服务区。
- `V0021__create_company_style_image.sql` 属于 Prompt 28（P3-3）企业招聘工作台生产化的补充，用于支持 company 本企业私有域内企业风采图片上传、预览、排序和删除。该补丁默认由 `phase3.company_style_upload=false` 关闭，并依赖 `phase3.company_workbench=true`；图片 object key 仅服务端保存，不返回前端、不进入公开企业主页、推荐位、搜索、sitemap、日志明文或导出旁路；不做微信扫码上传、小程序/App、图片审核公开展示、会员商业化或真实外部图床。
- `V0022__create_company_logo_asset.sql` 属于 Prompt 28（P3-3）企业招聘工作台生产化的补充，用于支持 company 本企业后台基本资料中的 Logo 私有上传、预览、替换和删除。该补丁默认由 `phase3.company_logo_upload=false` 关闭，并依赖 `phase3.company_workbench=true`；Logo object key 仅服务端保存，不返回前端、不进入公开企业列表、企业公开主页、推荐位、搜索、sitemap、日志明文或导出旁路；不做生成 LOGO、扫码上传、小程序/App、图片审核公开展示、会员商业化或真实外部图床。
- `V0025__add_job_post_soft_delete_fields.sql` 属于 Prompt 28（P3-3）企业招聘工作台生产化的补充，用于支持 company 本企业职位软删除基础闭环；后续补充已完成 `已删除/回收站` 列表与 `恢复为草稿`。该补丁只做软删除，不物理删除 `job_post`，不删除或改写历史投递、面试、导出、审计记录；删除后企业职位列表、公开 `/jobs`、职位详情和企业公开主页职位聚合默认不可见；恢复后必须回到草稿/待审核状态，仍需重新提交审核和运营审核通过后才可能公开；职位推广、置顶、紧急招聘、会员支付、搜索简历、联系解锁和真实外部能力仍保持占位或禁用。
- `/candidate/center` 求职者个人中心首页高保真增强属于 Prompt 27（P3-2）求职者真实闭环生产化的补充，用于把截图 `docs/page/4、会员中心.png` 收口为 LocalTalent 私有个人中心首页。该补丁不新增迁移或后端业务能力，只复用 candidate 私有 overview、resume、attachment、AI 建议、收藏、订阅、通知等接口；“会员中心”仅作为视觉参考，不代表会员商业化、真实支付、联系解锁、公共简历库或真实短信/微信/小程序/App，页面继续 `noindex`，不得进入公开门户、人才服务区、搜索、sitemap 或导出旁路。
- Candidate 期望职位三层专业选择器属于 Prompt 27（P3-2）求职者真实闭环生产化的补充，用于将 `/candidate/resume/create` 和 `/candidate/center` 的期望职位选择升级为 `大类 -> 小类 -> 具体岗位` 三层结构。该补丁不新增迁移、后端接口或 `subcategory_code` 字段；保存仍使用现有 `expected_positions` 与 `category_code`；职业小类仅用于 candidate 本人私有简历编辑体验，不接企业发布职位、职位搜索、订阅分类或全站职业分类改造，不进入公开门户、人才服务区、搜索、sitemap 或导出旁路。
- `docs/Phase_III/招聘者搜索简历分阶段开发规划.md/html` 已新增为后续专项规划文件，并将 `/company` 的“搜索简历”拆成 6 轮；Prompt 1（边界重签与数据源设计）、Prompt 2（后端受控搜索接口与六个下拉筛选）、Prompt 3（搜索简历列表页高保真 UI）、Prompt 4（安全简历详情抽屉与举报简历）、Prompt 5（下载/联系/职聊/面试邀请门禁占位）和 Prompt 6（安全收口与验收矩阵）均已完成。当前已登记 `phase3.company_resume_search=false`，默认关闭；新增 `V0026__extend_publish_snapshot_resume_search_fields.sql`、`V0027__create_company_resume_snapshot_report.sql`、`V0028__create_company_resume_access_request.sql`、company 私有 `GET /api/company/workbench/resume-search`、`GET /api/company/workbench/resume-search/{snapshotId}`、`POST /api/company/workbench/resume-search/{snapshotId}/reports` 和 `POST /api/company/workbench/resume-search/{snapshotId}/access-requests`。该能力唯一允许数据源为 `candidate_publish_snapshot` 发布快照，详情只返回安全摘要，举报和下载/联系/职聊/面试邀请申请只保存摘要并写审计/风控任务；Prompt 6 已补齐 IDOR/BOLA、撤回不可见、字段黑名单、审计脱敏和 `scripts/check_company_resume_search_acceptance` 专项闸门；仍不得开放公共简历库、联系解锁、真实下载完整简历、手机号/邮箱/微信展示、真实短信微信通知、会员支付或套餐扣点。

```text
下一步：进入灰度试运营复核，或另起四期正式上线治理规划。
```

`三期研发提示词.md/html` 已承接 `三期研发实施蓝图与任务拆解.md/html`，将 P3-0 至 P3-6 拆成 Prompt 25 至 Prompt 31 的可逐轮投喂卡片并完成回填。后续不得把新需求混入已收口的三期主线，也不得把灰度试运营扩展成公网正式上线或商业化能力。

后续每轮三期开发仍必须遵守一二期纪律：

1. 每轮先读三期边界、三期总规划、Step1-Step6、三期蓝图和三期提示词。
2. 每轮只执行当前 Prompt，不实现后续 Prompt 内容。
3. 若进入四期或风险池专项，必须另起边界、蓝图和提示词。
4. 不修改一期 Prompt 0-14 和二期 Prompt 15-24 的既定事实。

## 7. 自动化验收关注点

三期每轮必须继续守住：

- 人才服务区字段白名单。
- 撤回后下线联动。
- 公开页面字段黑名单。
- RBAC 与数据域。
- IDOR/BOLA。
- `audit_log` 与 `field_access_log`。
- 统一响应体与 `trace_id`。
- 写接口幂等。
- `auditor` 只读。
- 风险池能力不得进入主线。
