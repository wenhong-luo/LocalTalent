# LocalTalent 一期 Runbook

本文档用于一期本地、预发和生产前演练。业务边界仍以 `docs/00_MASTER_BOUNDARY.md` 与 Step1-Step4 为准。

## 本地启动

一条命令拉起依赖：

```bash
docker compose -f infra/docker-compose.yml up -d
```

依赖健康检查：

```bash
docker exec localtalent-redis redis-cli ping
docker exec localtalent-mysql mysqladmin ping -h 127.0.0.1 -uroot -plocaltalent_root
curl http://127.0.0.1:9000/minio/health/live
```

后端：

```bash
cd backend
./mvnw spring-boot:run
curl http://127.0.0.1:8080/health
```

前端：

```bash
cd frontend
npm ci
npm run dev
```

## 环境变量

后端默认本地配置已对齐 compose 服务；需要覆盖时使用：

```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/localtalent
export SPRING_DATASOURCE_USERNAME=localtalent
export SPRING_DATASOURCE_PASSWORD=localtalent
export LOCALTALENT_MINIO_ENDPOINT=http://localhost:9000
export LOCALTALENT_MINIO_ACCESS_KEY=localtalent
export LOCALTALENT_MINIO_SECRET_KEY=localtalent123
export LOCALTALENT_MINIO_BUCKET=export
```

前端：

```bash
export LOCALTALENT_API_BASE_URL=http://localhost:8080
export NEXT_PUBLIC_LOCALTALENT_SITE_URL=http://localhost:3000
```

## 发布顺序

1. 确认 CI 全绿：boundary、backend、frontend 三类 job 均通过。
2. 备份数据库与对象存储关键 bucket。
3. 执行后端发布，Flyway 自动向前迁移；禁止修改已发布的旧迁移脚本。
4. 启动后端并确认 `/health` 返回 200。
5. 发布前端 Next standalone Node 服务。
6. 做烟测：人才服务区、求职者中心、企业后台、运营后台、导出审批、审计 trace 查询。
7. 观察 30 分钟：5xx、P95/P99、DB 连接池、导出生成失败、open api 重试队列。

## 回滚策略

- 应用代码可回滚到上一版本镜像或上一提交构建。
- 数据库迁移采用 roll-forward：已执行的 Flyway 脚本不回改、不删除；修复通过新增更高版本迁移。
- 若新版本前端异常，可先回滚 Next 服务；后端保持兼容旧前端接口。
- 若导出生成或 MinIO 异常，暂停发放下载链接，保留 `export_apply/audit_log` 记录用于追溯。

## 备份与恢复

数据库：

```bash
docker exec localtalent-mysql mysqldump -uroot -plocaltalent_root localtalent > localtalent_backup.sql
```

恢复演练：

```bash
docker exec -i localtalent-mysql mysql -uroot -plocaltalent_root localtalent < localtalent_backup.sql
```

MinIO：

- 本地数据位于 docker volume `infra_minio-data`。
- 生产应启用对象存储生命周期、版本或备份策略。
- 导出 bucket 为 `export`；导出文件是短期下载材料，不应长期公开暴露。

## 监控与告警

最小指标：

- API：5xx 数量、P95/P99、认证失败率、`AUTHZ_403` 异常峰值。
- DB：连接池使用率、慢查询、Flyway 迁移失败。
- MinIO：4xx/5xx、对象写入失败、预签名 URL 发放失败。
- 导出：`export_apply` 待审批堆积、生成失败、重复下载 URL 请求。
- 对接 stub：`integration_sync_task` 重试次数、失败任务堆积。
- 审计：`audit_log/field_access_log/open_api_log` 写入异常。

建议告警：

- `/health` 连续 3 次失败。
- 5xx 超过阈值。
- 导出生成失败连续出现。
- 开放接口签名失败或重放攻击异常增长。
- IDOR/BOLA 阻断异常增长。

## 日志脱敏

日志必须遵守：

- 不记录手机号、邮箱、密码、token、secret、简历正文、附件对象 key、同意证据明文。
- 业务日志只记录 `trace_id`、操作结果、对象 ID 与错误码。
- 审计中心响应必须经过脱敏层，`AuditMaskingTest` 是回归守门人。
- 对接接口 `open_api_log` 只记录摘要、hash、耗时、状态与 trace，不记录原始请求体敏感明文。

## MinIO 预签名短期下载

一期导出策略：

- 企业必须先发起 `export_apply`。
- operator 审批通过后异步生成 CSV 并写入 MinIO `export` bucket。
- 企业按需获取短期预签名 URL。
- 后端只发放一次下载 URL；MinIO 预签名 URL 本身在过期前可能被重复访问。
- 未审批、驳回、未生成、已发放过 URL 均不得显示或发放下载入口。

故障处理：

- 生成失败：查看 `export_apply.error_msg` 与 `audit_log`。
- URL 发放失败：检查 MinIO endpoint、access key、secret key、bucket。
- 文件不可下载：确认 URL 未过期、对象 key 存在、MinIO 容器健康。

## docker-compose 运维

启动：

```bash
docker compose -f infra/docker-compose.yml up -d
```

停止：

```bash
docker compose -f infra/docker-compose.yml down
```

清空本地依赖数据，仅限开发环境：

```bash
docker compose -f infra/docker-compose.yml down -v
```

## 未来 K8s 探针建议

一期不提交 K8s manifests。未来上 K8s 时建议：

- 后端配置 startupProbe，避免 Flyway 或冷启动期间被误杀。
- 后端配置 readinessProbe 指向 `/health`，未就绪不接流量。
- 后端配置 livenessProbe 指向 `/health`，连续失败后重启。
- 前端 Next Node 服务配置 readiness/liveness 探针，确保滚动发布期间不把流量打到未就绪实例。
- MinIO、MySQL、Redis 优先使用托管服务或成熟 chart，避免自行维护状态ful 复杂度。

## 上线后抽查清单

- 人才服务区只展示发布快照字段白名单。
- 撤回同意后人才服务区不可见。
- 未审批导出不可下载。
- 跨企业职位/投递访问被拒绝。
- 审计中心可按 trace_id 串联日志且脱敏。
- 对接接口仍为 stub，无 ATS/受控候选池真实同步。
