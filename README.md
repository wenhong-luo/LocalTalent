# LocalTalent Phase-1 + Phase-2

LocalTalent 当前已完成一期合规底座，并进入二期高保真门户阶段。后端已完成认证、权限、发布快照、企业职位、投递面试、运营、导出、对接 stub、审计中心和门户公开读接口；前端已迁移为 Next/SSR，并按二期 Prompt 15-23 补齐首页、找工作、找企业、人才服务区、招聘会、资讯、HR 工具箱和登录注册入口体验，又通过 Prompt 24 补齐名企直聘、急聘、今日招聘、网络招聘会、校园招聘等低风险 P1 公开频道入口。本仓库仍严格遵守边界：不把人才服务区做成公共简历库，不把对接接口做成真实同步，不在公开层输出原始候选人数据。

## 一期硬边界

- 人才服务区仅展示发布快照，不展示或返回任何原始候选人数据。
- 对接接口仅 stub，不落真实同步、不接 ATS、不接受控候选池。
- 不输出原始候选人数据，不把人才服务区做成公共简历库。

## 统一术语

- 人才服务区：公开展示候选人发布快照的业务区，不是公共简历库。
- 发布快照：候选人在同意后由服务端生成的公开展示副本，是人才服务区唯一公开数据源。
- 原始候选人数据：联系方式、简历正文、附件、聊天记录、证据材料等未进入公开展示链路的数据。
- 同意：候选人明确授权的业务结果，由服务端写入历史记录并更新当前控制状态。
- 撤回：候选人撤回既有同意状态的动作，触发发布快照下线。
- 对接接口：面向外部系统的统一契约接口，一期只做签名、幂等、trace、日志与重试 stub。
- 数据域：服务端控制“谁能看哪一批数据”的边界规则。
- 字段级权限：服务端控制“字段显示、脱敏、隐藏”的边界规则。

## 技术基线

- Backend: Java 21, Spring Boot 4.0.5, Maven Wrapper, MySQL 8.4, Flyway
- Frontend: Next.js 16.2.4, React 19.2, Node SSR, Vitest 4.1.4
- Node baseline: 24 LTS
- Infra: docker-compose with MySQL 8.4, Redis 8.0-alpine, MinIO
- Trace: `X-Trace-Id` 优先，开放接口兼容 W3C `traceparent`

## 架构决策

- 认证体系：一期采用本地账号 + JWT，不接 SSO/OIDC。
- 前端部署：生产默认 Next standalone Node 服务；门户首屏 SSR，复访与交互走 CSR。
- SEO 口径：门户前台使用 Next metadata，已提供 `/robots.txt` 与 `/sitemap.xml` 基础入口。
- 容量口径：接口分页；导出异步；MinIO 短期下载；人才服务区读路径使用发布快照索引列，避免按 JSON 全表扫描。
- MinIO 口径：当前 `minio/minio:RELEASE.2025-09-07T16-13-09Z` 仅作一期本地开发占位，后续生产对象存储方案需单独确认。

## 目录树

```text
.
|-- README.md
|-- backend
|   |-- pom.xml
|   |-- openapi/auth.yaml
|   `-- src
|       |-- main
|       |   |-- java/cn/localtalent/backend/...
|       |   `-- resources
|       |       |-- application.yaml
|       |       `-- db/migration/
|       `-- test/java/cn/localtalent/backend/...
|-- frontend
|   |-- next.config.ts
|   |-- package.json
|   |-- package-lock.json
|   |-- src
|   |   |-- app/
|   |   |-- components/
|   |   |-- lib/
|   |   |-- pages/portal/
|   |   `-- test/
|   `-- vitest.config.ts
|-- infra
|   `-- docker-compose.yml
|-- scripts
|   |-- demo_login_tokens
|   |-- seed_demo_data
|   |-- check_boundary
|   `-- run_all
|-- demo
|   |-- localtalent_demo_data.sql
|   `-- localtalent_phase2_demo_data.sql
`-- docs
```

## 本地启动

基础依赖：

```bash
docker compose -f infra/docker-compose.yml up -d
```

后端开发启动：

```bash
cd backend
./mvnw spring-boot:run
```

后端打包启动：

```bash
cd backend
./mvnw clean package
java -jar target/backend-0.1.0-SNAPSHOT.jar
```

前端开发启动：

```bash
cd frontend
npm ci
npm run dev
```

前端生产构建与本地预览：

```bash
cd frontend
npm ci
npm run build
npm start
```

前端默认通过 `LOCALTALENT_API_BASE_URL=http://localhost:8080` 代理后端 API；`NEXT_PUBLIC_LOCALTALENT_SITE_URL` 用于生成 sitemap URL，默认 `http://localhost:3000`。

## 演示数据与新人上手

一期演示数据采用显式脚本加载，不进入 Flyway 自动迁移，不作为生产 seed。完整说明见 [docs/Phase_I/一期演示使用说明.md](docs/Phase_I/一期演示使用说明.md)。

推荐本地演示流程：

```bash
docker compose -f infra/docker-compose.yml up -d
cd backend
./mvnw spring-boot:run
```

后端保持运行后，在新终端执行：

```bash
./scripts/seed_demo_data
cd frontend
npm ci
npm run dev
```

前端保持运行后，在新终端获取登录命令：

```bash
./scripts/demo_login_tokens company
```

演示账号默认密码统一为 `LocalTalent@123456`。登录助手支持 `candidate-consented`、`candidate-revoked`、`candidate-pending`、`company`、`operator`、`auditor`，并输出可粘贴到浏览器 Console 的 `localStorage` 命令。

二期高保真门户演示同样采用显式脚本加载，不进入 Flyway。完整说明见 [docs/Phase_II/二期演示使用说明.md](docs/Phase_II/二期演示使用说明.md)。

推荐二期演示流程：

```bash
docker compose -f infra/docker-compose.yml up -d
cd backend
./mvnw spring-boot:run
```

后端保持运行后，在新终端执行：

```bash
./scripts/seed_phase2_demo_data
cd frontend
npm ci
npm run dev
```

二期演示页面：

- 首页：`http://localhost:3000/`
- 找工作：`http://localhost:3000/jobs`
- 找企业：`http://localhost:3000/companies`
- 人才服务区：`http://localhost:3000/portal/talent-service-area`
- 招聘会：`http://localhost:3000/job-fairs`
- 就业政策：`http://localhost:3000/articles/policies`
- HR 工具箱：`http://localhost:3000/hr-tools`
- 登录入口：`http://localhost:3000/auth/login`

二期登录助手支持 `p2-candidate`、`p2-revoked`、`p2-pending`、`p2-company`，也可以直接使用登录页输入账号密码。

## 测试命令

```bash
./scripts/check_boundary
./scripts/check_portal_snapshot_fields
./scripts/check_phase2_planning
./scripts/check_phase2_demo_acceptance
cd backend && ./mvnw test
cd frontend && npm ci && npm test && npm run build
./scripts/run_all
```

若当前终端未直接提供 `node/npm` 或 `java/javac`，可先设置 `NODE_BIN_DIR` 或 `JAVA_HOME_OVERRIDE` 后再执行 `scripts/run_all`。若需复用本地 Maven 缓存或离线仓库，可额外设置 `BACKEND_MAVEN_ARGS`。

## CI / 测试矩阵 / Runbook

- CI workflow：`.github/workflows/ci.yml`，PR 与主干 push 必跑边界脚本、后端测试、前端测试与前端构建。
- 一期测试矩阵：`docs/Phase_I/TESTING.md`，列出一期硬边界与对应自动化测试/脚本。
- 一期运维手册：`docs/Phase_I/RUNBOOK.md`，覆盖发布、回滚、备份、监控、日志脱敏、MinIO 预签名短期下载策略。
- 二期测试矩阵：`docs/Phase_II/TESTING.md`，列出二期页面结构、字段白名单、SEO 和风险池闸门。
- 二期运维手册：`docs/Phase_II/RUNBOOK.md`，覆盖二期高保真门户演示、发布和排障。
- 三期开发入口：`docs/Phase_III/三期研发提示词.md`，当前从 Prompt 25（P3-0）开始逐轮执行。
- 三期灰度 Runbook：`docs/Phase_III/RUNBOOK.md`，覆盖 feature flag、环境模板、预发/灰度、暂停回滚、备份恢复、监控告警和容量烟测口径。
- 三期 feature flag 注册表：`docs/Phase_III/feature_flags.md`，三期新增能力默认关闭，不得绕过服务端权限、字段级权限、导出审批和审计。
- 三期 P3-0 闸门：`scripts/check_phase3_boundary` 与 `scripts/check_phase3_ops`，用于阻断风险能力、敏感字段、真实外部能力和环境模板密钥误配置。
- 本地总闸门：`./scripts/run_all`，顺序执行一期边界扫描、人才服务区字段扫描、二期规划闸门、二期演示验收闸门、后端测试、前端测试与前端构建。

## 自检清单

- [ ] `/health` 返回 200，且响应体包含 `code / message / trace_id / data`
- [ ] `scripts/check_boundary` 可执行并输出 `PASS`
- [ ] `scripts/check_portal_snapshot_fields` 可执行并输出 `PASS`
- [ ] `scripts/check_phase2_planning` 可执行并输出 `PASS`
- [ ] `scripts/check_phase2_demo_acceptance` 可执行并输出 `PASS`
- [ ] `scripts/check_phase3_boundary` 可执行并输出 `PASS`
- [ ] `scripts/check_phase3_ops` 可执行并输出 `PASS`
- [ ] 后端测试通过，Flyway 迁移只向前新增，不修改旧版本迁移
- [ ] 前端 `npm test` 与 `npm run build` 通过
- [ ] CI workflow 任一门禁失败时阻断合并
- [ ] README 与研发提示词明确一期硬边界和统一术语
- [ ] 人才服务区只使用 `candidate_publish_snapshot`，公开响应只包含发布快照白名单字段
- [ ] 对接接口仍为 stub，无真实 ATS、受控候选池或外部主库同步
- [ ] 审计、导出、开放接口日志均不输出敏感明文
