# LocalTalent 一期测试矩阵

本文档说明一期交付的本地与 CI 测试门禁。目标不是“有测试文档”，而是让每条一期硬边界都有自动化守门人。

## 一键命令

```bash
./scripts/check_boundary
./scripts/check_portal_snapshot_fields
cd backend && ./mvnw test
cd frontend && npm ci && npm test && npm run build
./scripts/run_all
```

`scripts/run_all` 不执行 `npm ci`，避免脚本在未确认依赖安装策略时改动工作区。首次运行或锁文件变化后，请先在 `frontend/` 下显式执行 `npm ci`。

## CI 门禁

GitHub Actions workflow 位于 `.github/workflows/ci.yml`，PR 与主干 push 均会执行：

- `boundary`：`scripts/check_boundary` 与 `scripts/check_portal_snapshot_fields`。
- `backend`：JDK 21 下执行 `cd backend && ./mvnw test`。
- `frontend`：Node 24 下执行 `npm ci`、`npm test`、`npm run build`。

任一 job 失败即为红灯，不应合并。

## 硬边界映射

| 一期边界 | 自动化守门人 | 通过口径 |
| --- | --- | --- |
| 人才服务区仅展示发布快照 | `scripts/check_portal_snapshot_fields`、`PortalFieldBlacklistTest`、`TalentServiceArea.test.tsx` | 公开 DTO 与前端渲染只含 `snapshot_id/display_name_masked/city_code/category_code/skills_summary/experience_years/updated_at`，不含手机号、邮箱、简历正文、附件、证据或原始 JSON 字段。 |
| 撤回必下线 | `ConsentSnapshotFlowIT` | 同意后人才服务区可见；撤回后发布快照下线且人才服务区不可见。 |
| 未审批不可下载 | `ExportApprovalFlowIT` | 未审批、驳回、未生成或已发放下载链接时不得下载；审批通过并生成后才可获得短期 MinIO 预签名 URL。 |
| IDOR/BOLA 阻断 | `IdorBlockIntegrationTest`、`JobIdorBlockIT`、`ApplicationIdorBlockIT` | 跨用户、跨企业、跨对象 ID 访问统一被服务端拒绝。 |
| 字段级权限与脱敏 | `FieldPolicyEngineTest`、`AuditMaskingTest` | S3/S4 字段默认不泄露；审计中心响应不含敏感明文。 |
| 审计 trace 串联 | `AuditTraceQueryIT` | 同一 `trace_id` 可串联 `audit_log/field_access_log/open_api_log`。 |
| 对接接口仅 stub | `OpenApiContractTest`、`OpenApiRetryQueueIT`、`scripts/check_boundary` | 对接接口只做签名、幂等、trace、日志与重试，不连接 ATS/受控候选池或外部主库。 |
| 迁移只向前 | `MigrationTest` | 空库可应用全部 Flyway 迁移；后续修复通过新增版本迁移，不改旧脚本。 |
| 前端路由与状态 | `RouteGuard.test.tsx`、`CompanyDashboard.test.tsx`、`AdminDashboard.test.tsx`、`CandidateCenter.test.tsx`、`StateView.test.tsx` | 无权限态、错误态、重试态、审核按钮与导出下载按钮显示逻辑符合 Prompt 约束。 |

## 后端测试清单

- `HealthControllerTest`：`/health` 与统一响应体。
- `AuthIntegrationTest`：本地账号 + JWT 登录链路。
- `DataScopeIntegrationTest`、`IdorBlockIntegrationTest`：权限与数据域阻断。
- `ConsentSnapshotFlowIT`、`PortalFieldBlacklistTest`：同意、撤回、发布快照与公开字段黑名单。
- `JobVisibilityIT`、`JobIdorBlockIT`：企业/职位状态机和跨企业阻断。
- `ApplicationInterviewFlowIT`、`ApplicationIdorBlockIT`：投递、面试签到、防重放和跨企业阻断。
- `ExportApprovalFlowIT`：导出申请、审批、生成、一次性发放短期下载链接。
- `OpenApiContractTest`、`OpenApiRetryQueueIT`：开放接口 stub 契约和重试队列。
- `AuditTraceQueryIT`、`AuditMaskingTest`：审计中心查询、脱敏和 trace 串联。

## 前端测试清单

- `TalentServiceArea.test.tsx`：人才服务区状态机与发布快照字段白名单。
- `CandidateCenter.test.tsx`：求职者中心状态、同意/撤回入口和字段黑名单。
- `CompanyDashboard.test.tsx`：企业认证、职位状态、投递池摘要、导出下载入口显示规则。
- `AdminDashboard.test.tsx`：运营审核队列、operator 写按钮、auditor 只读展示、驳回原因校验。
- `RouteGuard.test.tsx`：company/admin 路由守卫。
- `httpClient.test.ts`：`X-Trace-Id`、Bearer token、幂等键与统一错误映射。
- `StateView.test.tsx`：loading/empty/error/unauthorized/retrying。

## 手工烟测

```bash
docker compose -f infra/docker-compose.yml up -d
docker exec localtalent-redis redis-cli ping
docker exec localtalent-mysql mysqladmin ping -h 127.0.0.1 -uroot -plocaltalent_root
curl http://127.0.0.1:9000/minio/health/live

cd backend
./mvnw spring-boot:run
curl -i http://127.0.0.1:8080/health

cd ../frontend
npm run dev
```

前端重点访问：

- `http://localhost:3000/portal/talent-service-area`
- `http://localhost:3000/candidate/center`
- `http://localhost:3000/company`
- `http://localhost:3000/admin`
