# LocalTalent 二期 Runbook

本文用于二期高保真门户阶段的本地演示、预发演练和问题排查。边界仍以 `00_PHASE_II_BOUNDARY.md` 为最高优先级。

## 本地依赖

```bash
docker compose -f infra/docker-compose.yml up -d
docker exec localtalent-redis redis-cli ping
docker exec -e MYSQL_PWD=localtalent localtalent-mysql mysql -ulocaltalent localtalent -e "SELECT 1;"
curl http://127.0.0.1:9000/minio/health/live
```

## 后端与前端

```bash
cd backend
./mvnw spring-boot:run
curl http://127.0.0.1:8080/health
```

```bash
cd frontend
npm ci
npm run dev
```

生产预览：

```bash
cd frontend
npm run build
npm start
```

## 二期演示数据

二期演示数据不进入 Flyway，只能显式导入：

```bash
./scripts/seed_phase2_demo_data
```

脚本会校验：

- MySQL 容器正在运行。
- Flyway 已创建二期所需表和列。
- `candidate_publish_snapshot.snapshot_json` 不含原始候选人字段。
- 导入后输出核心页面路径和演示账号。

## 发布顺序

1. 运行 `./scripts/run_all`，确认边界、后端、前端全部通过。
2. 备份数据库和 MinIO export bucket。
3. 发布后端，Flyway 只向前迁移，不修改旧迁移。
4. 确认 `/health` 正常。
5. 发布 Next standalone Node 服务。
6. 烟测公开门户：`/`、`/jobs`、`/companies`、`/portal/talent-service-area`、`/job-fairs`、`/articles/policies`、`/hr-tools`、`/auth/login`。
7. 观察 30 分钟：5xx、P95/P99、DB 连接、导出生成、open api 重试队列。

## 回滚策略

- 前端异常：先回滚 Next 服务，后端保持兼容。
- 后端异常：回滚应用版本；数据库通过新增迁移 roll-forward 修复。
- 演示数据异常：仅在本地开发环境使用 `docker compose -f infra/docker-compose.yml down -v` 后重建。
- MinIO 下载异常：暂停下载 URL 发放，保留 `export_apply` 与审计日志。

## 二期监控关注点

- 公开门户：SSR 错误率、页面 5xx、sitemap/robots 可访问性。
- 搜索接口：`/api/portal/jobs`、`/api/portal/companies`、`/api/portal/talent-snapshots` 慢查询。
- 人才服务区：撤回后不可见、字段白名单、发布快照索引列。
- 内容频道：`cms_content.status=0`、`activity_event.status=0` 不公开。
- 审计与对接：`audit_log/field_access_log/open_api_log` 写入异常、stub 重试任务堆积。

## 高风险能力处理

以下能力出现需求时不得直接实现，必须回到 Step6 准入：

- 公共简历库、受控找人才、联系解锁。
- 会员商业化、真实支付。
- 真实短信、微信、小程序、App。
- 真实地图、视频、直播。
- 真实 ATS/RPA、受控候选池同步。

## 日志脱敏

日志和审计中心响应不得包含手机号、邮箱、密码、token、secret、简历正文、附件对象 key、同意证据明文。公开门户问题排查只记录 `trace_id`、接口路径、错误码和对象 ID。
