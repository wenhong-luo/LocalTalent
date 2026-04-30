# LocalTalent 二期测试矩阵

本文说明二期高保真门户阶段的自动化守门人。目标是让每个公开页面和风险边界都有测试或脚本兜底。

## 一键命令

```bash
./scripts/check_phase2_planning
./scripts/check_phase2_demo_acceptance
./scripts/check_portal_snapshot_fields
./scripts/check_boundary
cd backend && ./mvnw test
cd frontend && npm test && npm run build
./scripts/run_all
```

## 二期页面验收映射

| 页面/能力 | 自动化守门人 | 验收口径 |
| --- | --- | --- |
| 首页高保真首屏 | `PortalHomePage.test.tsx` | 顶部工具条、搜索区、分类墙、登录卡、公告 tab、广告位、扫码 CTA、推荐企业、热招职位存在。 |
| 公共门户 Shell | `PortalShell.test.tsx` | 导航、更多服务、Footer、右侧工具条存在；公开导航不出现“简历库/搜索简历”。 |
| 找工作 | `JobSearchPage.test.tsx`、`JobVisibilityIT` | 只展示在线职位和认证企业职位；职位卡片不含联系人、审核材料或营业执照附件。 |
| 找企业 | `CompanySearchPage.test.tsx`、`PortalCompanyVisibilityIT` | 只展示已认证企业公开字段；企业主页只聚合在线且审核通过职位。 |
| 人才服务区 | `TalentServiceArea.test.tsx`、`PortalFieldBlacklistTest`、`check_portal_snapshot_fields` | 唯一公开数据源是发布快照；DOM 和响应只含白名单字段；撤回下线不可见。 |
| 招聘会/资讯/HR 工具箱 | `PortalContentChannels.test.tsx`、`PortalContentEventVisibilityIT` | `status=0` 不公开；不展示报名名单、候选人明细、签到证据或联系方式。 |
| 登录注册入口 | `AuthPages.test.tsx` | 本地账号/JWT 可登录分流；短信、微信、小程序、App 均为禁用占位。 |
| SEO/Next SSR | `npm run build`、`sitemap.xml/route.page.ts` | 新增公开频道可构建，sitemap 覆盖首页、职位、企业、人才服务区、招聘会、资讯和工具箱。 |

## 一期边界回归映射

| 边界 | 守门人 |
| --- | --- |
| 撤回后人才服务区不可见 | `ConsentSnapshotFlowIT` |
| IDOR/BOLA 阻断 | `IdorBlockIntegrationTest`、`JobIdorBlockIT`、`ApplicationIdorBlockIT` |
| 审计 trace 串联与脱敏 | `AuditTraceQueryIT`、`AuditMaskingTest` |
| 对接接口仅 stub | `OpenApiContractTest`、`OpenApiRetryQueueIT`、`check_boundary` |
| 导出审批与短期下载 | `ExportApprovalFlowIT` |

## 二期总闸门

`scripts/check_phase2_demo_acceptance` 静态检查：

- 二期演示 SQL、导入脚本和文档存在。
- 公开页面路由和对应测试存在。
- sitemap 覆盖新增公开频道。
- Prompt 23 完成记录和边界文件收口记录存在。
- 敏感字段黑名单仍在测试中被断言。

`scripts/run_all` 顺序执行：

1. `check_boundary`
2. `check_portal_snapshot_fields`
3. `check_phase2_planning`
4. `check_phase2_demo_acceptance`
5. 后端测试
6. 前端测试
7. 前端构建

## 手工演示验收

按 `docs/Phase_II/二期演示使用说明.md` 启动后，逐页确认：

- 首页模块密度接近 demo 首屏。
- 找工作、找企业、人才服务区有演示数据。
- 招聘会、就业政策、职场资讯、HR 工具箱可展示公开内容。
- 登录注册入口可用，但外部能力均禁用。
- 人才服务区不出现原始候选人数据。
