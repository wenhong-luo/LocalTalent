# 给 Codex 的使用提示词

请先阅读 `template_list_42.csv`，再读取 `representative_templates/` 中的代表 HTML。

目标不是 1:1 复制 74CMS 静态页面，而是基于 42 个代表模板抽象出一个可开发、可维护的地方人才网项目。

关键要求：
1. 把筛选页、分页页、ID 详情页视为同一模板的不同数据状态。
2. 优先实现 P0：home、job_list、job_detail、company_list、company_detail、resume_list、job_emergency_list、job_famous_list、member_login_personal、member_register_personal。
3. 将相同结构抽象为公共组件：SiteHeader、SiteFooter、FilterPanel、SortTabs、Pagination、JobCard、CompanyCard、ResumeCard、DetailHeader、AuthShell。
4. 不要为每个 HTML 新建一个独立页面；应建立模板路由与动态参数。
5. 页面数据来自 API，不要把静态 HTML 内容写死进业务逻辑。
6. 自研重点是：AI 客户端职位同步、ATS 同步桥、面试扫码签到、候选人授权入库、本地人才库、推荐回流。
