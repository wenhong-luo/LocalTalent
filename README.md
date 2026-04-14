# LocalTalent Phase-1 P0 Scaffold

LocalTalent 当前只落地方人才服务平台一期的 monorepo 骨架，不实现业务流程、认证流程、真实对接、导出、职位、同意、撤回或发布快照逻辑。`docs/` 为主控文档，本轮不修改。

## 一期硬边界

- 人才服务区仅展示发布快照，不展示或返回任何原始候选人数据。
- 对接接口仅 stub，本轮不落真实同步、不接 ATS、不接受控候选池。
- 不输出原始候选人数据，不把人才服务区做成公共简历库。

## 统一术语

- 人才服务区：公开展示候选人发布快照的业务区，不是公共简历库。
- 发布快照：满足后续发布条件后进入公开展示链路的最小必要展示副本。
- 原始候选人数据：联系方式、简历正文、附件、聊天记录、证据材料等未进入公开展示链路的数据。
- 同意：候选人明确授权的业务结果，本轮只保留术语，不实现流程。
- 撤回：候选人撤回既有同意状态的动作，本轮只保留术语，不实现流程。
- 对接接口：面向 ATS 与受控候选池的预留集成接口，本轮仅保留 stub 口径。
- 数据域：服务端控制“谁能看哪一批数据”的边界规则，本轮只写入 README，不实现。
- 字段级权限：服务端控制“字段显示、脱敏、隐藏”的边界规则，本轮只写入 README，不实现。

## 默认占位决策

- 认证体系：后续阶段默认采用本地账号 + JWT；本轮不实现认证接口。
- 部署形态：本轮使用 `docker-compose` 预留 `MySQL / Redis / MinIO`。
- 容量策略：后续默认采用分页、异步导出、MinIO 短期下载；本轮不实现导出能力。
- Trace：后端统一支持 `X-Trace-Id`，未传时自动生成并回传。
- MinIO：本轮使用历史官方镜像 `minio/minio:RELEASE.2025-09-07T16-13-09Z` 作为本地开发占位，后续再切换为官方源码构建方案。

## 技术基线

- Backend: Java 21, Spring Boot 4.0.5, Maven Wrapper
- Frontend: React 19.2, Vite 8, Vitest 4.1.4
- Node baseline: 24 LTS
- Infra: docker-compose with MySQL 8.4, Redis 8.0-alpine, MinIO

## 目录树

```text
.
|-- README.md
|-- .gitignore
|-- backend
|   |-- .mvn/wrapper/
|   |-- mvnw
|   |-- mvnw.cmd
|   |-- pom.xml
|   `-- src
|       |-- main
|       |   |-- java/cn/localtalent/backend/...
|       |   `-- resources/application.yaml
|       `-- test
|           `-- java/cn/localtalent/backend/health/HealthControllerTest.java
|-- frontend
|   |-- index.html
|   |-- package.json
|   |-- package-lock.json
|   |-- tsconfig.json
|   |-- vite.config.ts
|   |-- vitest.config.ts
|   `-- src
|       |-- App.tsx
|       |-- App.test.tsx
|       |-- components/StateView.tsx
|       |-- components/StateView.test.tsx
|       |-- main.tsx
|       `-- test/setup.ts
|-- infra
|   `-- docker-compose.yml
|-- scripts
|   |-- check_boundary
|   `-- run_all
`-- docs
```

## 后端说明

- 唯一公开接口为 `GET /health`。
- 统一响应体格式为 `{ code, message, trace_id, data }`。
- 每个请求都支持 `X-Trace-Id`；若未传入，服务端会生成并在响应头与响应体中回传。
- 本轮不创建业务实体、Repository、Flyway、对接客户端或任何业务 Controller。

## 前端说明

- `App.tsx` 仅展示骨架说明与 `StateView` 占位状态。
- `StateView` 仅支持 `loading | error | forbidden | retrying` 四种 `variant`。
- 前端不接 API、不加路由、不引入状态管理库。

## Infra 说明

- `infra/docker-compose.yml` 仅拉起基础依赖，不包含应用镜像。
- MySQL、Redis、MinIO 都只服务于本地开发占位。
- 当前仓库未提供 Docker Desktop，本地启动依赖宿主机已安装 `docker compose`。

## 启动命令

```bash
docker compose -f infra/docker-compose.yml up -d
cd backend && ./mvnw spring-boot:run
cd frontend && npm ci && npm run dev
```

## 测试命令

```bash
cd backend && ./mvnw test
cd frontend && npm ci && npm test
scripts/check_boundary
scripts/run_all
```

若当前终端未直接提供 `node/npm` 或 `java/javac`，可先设置 `NODE_BIN_DIR` 或 `JAVA_HOME_OVERRIDE` 后再执行 `scripts/run_all`。若需复用本地 Maven 缓存或离线仓库，可额外设置 `BACKEND_MAVEN_ARGS`。

## 自检清单

- [ ] `/health` 返回 200，且响应体包含 `code / message / trace_id / data`
- [ ] 前端最小渲染测试通过
- [ ] `scripts/check_boundary` 可执行并输出 `PASS`
- [ ] 一期硬边界已写入 README
- [ ] 统一术语已写入 README
- [ ] 未实现任何同意、撤回、发布快照、职位、导出、对接业务流程
- [ ] 未接入 ATS、受控候选池或任何真实同步客户端
- [ ] 未输出任何原始候选人数据字段到公开层
