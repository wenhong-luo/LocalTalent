# LocalTalent Phase-1

LocalTalent 当前是地方人才服务平台一期工程。后端已按 Prompt 0-11.5 完成认证、权限、候选人发布快照、企业职位、投递面试、运营、导出、对接 stub、审计中心、门户公开读接口与发布快照读路径补强；前端已迁移为 Next/SSR 门户基线。本仓库仍严格遵守一期边界：不把人才服务区做成公共简历库，不把对接接口做成真实同步，不在公开层输出原始候选人数据。

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
|   |-- check_boundary
|   `-- run_all
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

## 测试命令

```bash
./scripts/check_boundary
cd backend && ./mvnw test
cd frontend && npm ci && npm test && npm run build
./scripts/run_all
```

若当前终端未直接提供 `node/npm` 或 `java/javac`，可先设置 `NODE_BIN_DIR` 或 `JAVA_HOME_OVERRIDE` 后再执行 `scripts/run_all`。若需复用本地 Maven 缓存或离线仓库，可额外设置 `BACKEND_MAVEN_ARGS`。

## 自检清单

- [ ] `/health` 返回 200，且响应体包含 `code / message / trace_id / data`
- [ ] `scripts/check_boundary` 可执行并输出 `PASS`
- [ ] 后端测试通过，Flyway 迁移只向前新增，不修改旧版本迁移
- [ ] 前端 `npm test` 与 `npm run build` 通过
- [ ] README 与研发提示词明确一期硬边界和统一术语
- [ ] 人才服务区只使用 `candidate_publish_snapshot`，公开响应只包含发布快照白名单字段
- [ ] 对接接口仍为 stub，无真实 ATS、受控候选池或外部主库同步
- [ ] 审计、导出、开放接口日志均不输出敏感明文
