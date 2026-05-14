# LocalTalent 三期 Feature Flag 注册表

> 本文件是 Prompt 25（P3-0）的三期灰度基线。三期新增能力必须先登记 flag，默认关闭，再按后续 Prompt 逐步接入运行时配置。

## 使用纪律

- 所有三期新增能力默认状态必须是 `off`。
- Feature flag 只能控制灰度暴露范围，不得绕过服务端 RBAC、数据域、字段级权限、导出审批、审计或撤回下线。
- 原始候选人数据不得公开；公开门户和人才服务区仍只能展示服务端裁剪后的公开白名单字段。
- 任何 flag 打开前必须具备回滚方式、验收命令、负责人和影响范围。
- 灰度环境打开 flag 时必须可在不发版的情况下关闭；如果后续实现暂不支持运行时关闭，必须在 Runbook 中写明暂停方案。
- 风险池能力不得通过 feature flag 偷跑：公共简历库、受控找人才、联系解锁、真实支付、真实短信、真实微信、小程序、App、真实地图、视频、直播、ATS、RPA、受控候选池同步均不属于三期主线 flag。

## 三期主线 Flag

| Flag Key | 默认状态 | 所属阶段 | 影响范围 | 负责人 | 回滚方式 | 硬边界 |
| --- | --- | --- | --- | --- | --- | --- |
| `phase3.baseline_gate` | off | P3-0 | 三期闸门、环境模板、Runbook 入口 | TBD | 关闭 flag 并回退到二期稳定入口 | 不启用任何业务功能 |
| `phase3.oidc_login` | off | P3-1 | OIDC/SSO 登录、回调、登出、身份映射 | TBD | 关闭 OIDC，回到 dev/test 本地 JWT fallback | 不把授权交给 IdP claim |
| `phase3.local_jwt_fallback` | off | P3-1 | 本地开发与测试账号登录兜底 | TBD | 关闭 fallback，仅允许 OIDC | gray/prod 默认关闭或白名单应急 |
| `phase3.candidate_closure` | off | P3-2 | 求职者简历、投递、收藏、订阅、站内通知、同意/撤回 | TBD | 关闭三期求职者增强，回到一期 overview | 公开层仍只展示发布快照 |
| `phase3.resume_attachment_upload` | off | P3-2 补充 | 求职者本人私有简历附件上传、下载、替换、删除 | TBD | 关闭附件上传入口，保留安全占位 | 附件只进本人私有域；不返回对象 key、预签名 URL 或文件内容到公开层 |
| `phase3.resume_ai_assist` | off | P3-2 补充 | Candidate 本人私有域内的安全规则版简历优化建议、逐条手动应用和忽略 | TBD | 关闭优化建议入口，回到“AI 优化暂未开放”占位 | 不接真实 LLM/外部模型；不上传原始候选人数据；建议不得自动发布到人才服务区 |
| `phase3.company_workbench` | off | P3-3 | 企业资料、认证材料、职位、投递池、面试邀约、导出申请 | TBD | 关闭企业增强入口，保留二期企业页 | 企业只能访问本企业数据 |
| `phase3.company_style_upload` | off | P3-3 补充 | 企业风采图片私有上传、预览、排序、删除 | TBD | 关闭风采上传入口，回到安全占位 | 只限企业私有域；不进入公开企业主页、推荐位、搜索、sitemap 或导出旁路 |
| `phase3.company_logo_upload` | off | P3-3 补充 | 企业基本资料 Logo 私有上传、预览、替换、删除 | TBD | 关闭 Logo 上传入口，回到安全占位 | 只限企业后台私有域；不返回 object key、预签名 URL，不进入公开企业页、推荐位、搜索、sitemap 或导出旁路 |
| `phase3.company_resume_search` | off | P3-3 搜索简历专项 Prompt 1 | 企业私有域内受控搜索候选人发布快照 | TBD | 关闭搜索简历入口，回到风险池占位 | 只允许读取 `candidate_publish_snapshot`；不开放公共简历库、联系解锁、完整简历下载、联系方式或原始候选人数据 |
| `phase3.operator_portal_ops` | off | P3-4 | 运营审核、内容活动、推荐位、风险审核、门户状态驱动 | TBD | 关闭运营配置入口，回到二期静态演示 | auditor 只读，operator 写审计 |
| `phase3.audit_security_gate` | off | P3-5 | 审计 trace、导出审批回归、撤回下线传播、字段黑名单、IDOR/BOLA | TBD | 阻断灰度入口，回退到上一个安全版本 | 不得放宽字段黑名单 |
| `phase3.gray_ops` | off | P3-6 | 预发/灰度配置、备份恢复、监控告警、容量烟测、上线演练 | TBD | 关闭灰度流量并执行暂停/回滚 Runbook | 不代表公网正式上线 |

## 打开前检查单

打开任意三期 flag 前，必须确认：

- `./scripts/check_phase3_boundary` 输出 `PASS`。
- `./scripts/check_phase3_ops` 输出 `PASS`。
- `./scripts/check_portal_snapshot_fields` 输出 `PASS`。
- 涉及写动作时已具备 `trace_id`、必要幂等、`audit_log`。
- 涉及敏感字段读取时已具备字段级权限和 `field_access_log`。
- 涉及对象 ID 时已有 IDOR/BOLA 测试。
- 涉及撤回状态时已验证人才服务区、推荐位、搜索索引、缓存和 sitemap 不可见。

## 禁止用法

- 禁止用 flag 直接打开公共简历库、搜索简历、查看简历、联系解锁、受控找人才。
- 禁止用 flag 绕过导出审批、字段裁剪、MinIO 短期下载或审计链。
- 禁止把真实短信、真实微信、小程序、App、真实地图、视频、直播、真实支付、ATS/RPA 伪装成三期主线 flag。
- 禁止在日志、错误响应、审计摘要、URL query 中输出 token、secret、手机号、邮箱、简历正文、附件对象 key、证据或原始 JSON。
