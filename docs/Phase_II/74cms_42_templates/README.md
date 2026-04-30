# 74CMS 人才网 42 个代表 HTML 模板包

这个压缩包只保留“开发模板维度”的代表 HTML 页面，不包含 5,000+ 个原始筛选页、分页页、ID 详情页变体。

## 文件结构

```text
74cms_42_templates_only/
├── README.md
├── CODEX_README.md
├── template_list_42.csv
└── representative_templates/   # 42 个代表 HTML 页面
```

## 去重口径

将仅筛选条件不同、分页不同、ID 不同、职位/企业/文章内容不同，但页面结构相同的 HTML 归为同一模板。比如大量 `job_c1_*`、`job_w1_*`、`job.html_page=*` 都归为 `job_list`。

## Codex 使用建议

让 Codex 先读取 `README.md` 和 `template_list_42.csv`，再按优先级读取 `representative_templates/` 下的 HTML。开发时不要把这些 HTML 当成 42 个完全独立页面，而应抽象成公共组件：Header、Footer、FilterPanel、CardList、DetailHeader、Pagination、AuthForm 等。
