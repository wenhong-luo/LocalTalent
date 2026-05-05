# LocalTalent 三期 Runbook：灰度基线与闸门

> 本 Runbook 对应 Prompt 25（P3-0）。它只描述三期灰度基线、环境模板、feature flag 和闸门命令，不代表三期业务功能已经实现。

## 1. 当前阶段

- 三期目标：小范围真实试运营。
- 当前轮次：Prompt 27（P3-2）求职者真实闭环生产化已完成，下一轮进入 Prompt 28（P3-3）企业招聘工作台生产化。
- 当前能力：三期文档、配置模板、feature flag 注册表、静态闸门脚本、OIDC/SSO 授权码登录、内部账号映射、dev/test 本地 JWT fallback、candidate 私有简历/投递/收藏/订阅/站内通知闭环。
- 当前非目标：不实现企业工作台、运营后台业务闭环；不接真实短信、微信、小程序、App；不把授权交给 IdP claim；不开放公共简历库、联系解锁或受控找人才。

## 2. 必读文件

三期开发和灰度准备必须先读：

- `docs/Phase_III/00_PHASE_III_BOUNDARY.md`
- `docs/Phase_III/LocalTalent_三期总规划.md`
- `docs/Phase_III/LocalTalent_phase3_step1.md` 至 `LocalTalent_phase3_step6.md`
- `docs/Phase_III/三期研发实施蓝图与任务拆解.md`
- `docs/Phase_III/三期研发提示词.md`
- `docs/Phase_III/feature_flags.md`

一二期文件只作为已完成基线和边界回顾，不得修改既定事实。

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
- 本地、预发、灰度必须使用各自的真实配置文件，不得提交。
- OIDC、JWT fallback、MinIO、Redis、MySQL、Next API base URL、导出 TTL 等配置必须按环境显式设置。

## 4. Feature Flag

三期 flag 注册表位于：

```bash
docs/Phase_III/feature_flags.md
```

默认要求：

- 所有三期新增能力默认关闭。
- flag 不得绕过 RBAC、数据域、字段级权限、导出审批或审计。
- 风险池能力不得通过 flag 偷跑。

打开任意 flag 前，至少执行：

```bash
./scripts/check_phase3_boundary
./scripts/check_phase3_ops
./scripts/check_portal_snapshot_fields
./scripts/check_boundary
```

## 5. 本地开发入口

三期 P3-0 不改变一二期启动方式。

基础依赖：

```bash
docker compose -f infra/docker-compose.yml up -d
```

后端：

```bash
cd backend
./mvnw spring-boot:run
```

前端：

```bash
cd frontend
npm ci
npm run dev
```

三期 Prompt 26 已引入 OIDC/SSO。OIDC 只负责认证和基础 claims；LocalTalent 后端继续负责 RBAC、数据域、字段级权限、导出审批和审计。dev/test 可使用本地 JWT fallback；gray/prod 默认关闭本地 fallback，只有显式白名单账号可作为应急入口。

## 6. 预发与灰度配置

预发/灰度至少需要确认：

- `LOCALTALENT_ENV` 明确为 `staging` 或 `gray`。
- 所有三期业务 flag 默认 `off`。
- OIDC issuer、client id、callback URL 使用环境变量，不写入仓库。
- token、secret、claims、手机号、邮箱、简历正文不得进入日志。
- 导出 TTL、下载次数、导出频控必须显式配置。
- MinIO 或对象存储 bucket 不复用本地演示 bucket。

## 7. 暂停与回滚

暂停灰度时：

1. 关闭对应 feature flag。
2. 确认新增入口不再展示或回退到二期稳定入口。
3. 执行边界脚本。
4. 检查审计日志和错误日志。

回滚发布时：

1. 停止新增灰度流量。
2. 回退应用版本。
3. 保留数据库 forward-only 迁移，不做破坏性回滚。
4. 验证 `/health`、公开门户、登录入口、人才服务区字段白名单。
5. 记录 trace_id、commit、影响范围和处理结论。

## 8. 备份恢复

三期进入灰度前必须完成备份恢复演练规划：

- MySQL：记录备份频率、恢复点目标、恢复验证命令。
- Redis：明确可丢失缓存与不可丢失状态，不把敏感原始数据写入缓存。
- MinIO/对象存储：导出文件短期保留，过期清理，下载 URL 短期有效。
- 配置：真实 `.env` 不入库，密钥轮换方案留给后续安全治理。

## 9. 监控告警与故障排查

灰度至少关注：

- 登录失败率。
- 403/401 权限拒绝异常波动。
- 导出审批失败率与生成失败率。
- 撤回后人才服务区仍可见的异常。
- 公开页面 5xx 与响应耗时。
- `audit_log`、`field_access_log`、`open_api_log` trace 串联异常。

故障排查时不得输出或粘贴 token、secret、手机号、邮箱、简历正文、附件对象 key、证据、原始 JSON。

## 10. 容量烟测

P3-0 不执行压测，只冻结后续检查口径：

- 公开门户：首页、找工作、找企业、人才服务区、招聘会、资讯、HR 工具箱。
- 私有中心：求职者中心、企业中心、运营后台。
- 安全链路：登录、撤回、投递、导出审批、审计 trace。
- 失败时优先暂停灰度，不扩大流量。

## 11. 验收命令

Prompt 25 至少执行：

```bash
./scripts/check_phase3_boundary
./scripts/check_phase3_ops
./scripts/check_portal_snapshot_fields
./scripts/check_boundary
```

后续 Prompt 涉及业务代码时，再按对应提示词补：

```bash
cd backend && ./mvnw test
cd frontend && npm test && npm run build
```

## 12. 硬边界复述

- 人才服务区仅展示发布快照。
- 原始候选人数据不得公开。
- 对接接口仅 stub。
- 公共简历库、受控找人才、联系解锁、真实支付、真实短信、真实微信、小程序、App、真实地图、视频、直播、ATS、RPA 仍在风险池，不进入三期主线。
