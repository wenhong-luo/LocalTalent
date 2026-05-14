INSERT INTO sys_dict_type (dict_code, dict_name, status)
VALUES
  ('company_auth_status', '企业认证状态', 1),
  ('job_status', '职位状态', 1),
  ('application_status', '投递流程状态', 1),
  ('consent_status', '同意状态', 1),
  ('publishable_flag', '发布状态标记', 1),
  ('visibility_scope', '可见范围', 1),
  ('source_type', '来源类型', 1)
AS new
ON DUPLICATE KEY UPDATE
  dict_name = new.dict_name,
  status = new.status,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_dict_item (dict_type_id, item_code, item_name, sort_no, status)
SELECT t.id, v.item_code, v.item_name, v.sort_no, 1
FROM sys_dict_type t
JOIN (
  SELECT 'company_auth_status' AS dict_code, '0' AS item_code, '待提交' AS item_name, 0 AS sort_no
  UNION ALL SELECT 'company_auth_status', '1', '待审核', 1
  UNION ALL SELECT 'company_auth_status', '2', '已通过', 2
  UNION ALL SELECT 'company_auth_status', '3', '已驳回', 3
  UNION ALL SELECT 'company_auth_status', '4', '已禁用', 4
  UNION ALL SELECT 'job_status', '0', '草稿', 0
  UNION ALL SELECT 'job_status', '1', '待审核', 1
  UNION ALL SELECT 'job_status', '2', '在线', 2
  UNION ALL SELECT 'job_status', '3', '下线', 3
  UNION ALL SELECT 'job_status', '4', '删除', 4
  UNION ALL SELECT 'application_status', '0', '已投递', 0
  UNION ALL SELECT 'application_status', '1', '待筛选', 1
  UNION ALL SELECT 'application_status', '2', '邀约面试', 2
  UNION ALL SELECT 'application_status', '3', '已签到', 3
  UNION ALL SELECT 'application_status', '4', '已结束', 4
  UNION ALL SELECT 'application_status', '5', '已淘汰', 5
  UNION ALL SELECT 'consent_status', '0', '未同意', 0
  UNION ALL SELECT 'consent_status', '1', '已同意', 1
  UNION ALL SELECT 'consent_status', '2', '已撤回', 2
  UNION ALL SELECT 'publishable_flag', '0', '不可发布', 0
  UNION ALL SELECT 'publishable_flag', '1', '可发布', 1
  UNION ALL SELECT 'visibility_scope', '1', '仅本人', 1
  UNION ALL SELECT 'visibility_scope', '2', '本企业', 2
  UNION ALL SELECT 'visibility_scope', '3', '运营审核', 3
  UNION ALL SELECT 'visibility_scope', '4', '人才服务区', 4
  UNION ALL SELECT 'source_type', '1', '站内注册', 1
  UNION ALL SELECT 'source_type', '2', '站内投递', 2
  UNION ALL SELECT 'source_type', '3', '面试签到', 3
  UNION ALL SELECT 'source_type', '4', '对接接口预留', 4
  UNION ALL SELECT 'source_type', '6', '人工录入', 6
) v ON v.dict_code = t.dict_code
ON DUPLICATE KEY UPDATE
  item_name = v.item_name,
  sort_no = v.sort_no,
  status = 1,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_role (role_code, role_name, role_type, status)
VALUES
  ('candidate', '求职者', 'portal', 1),
  ('company_admin', '企业管理员', 'company', 1),
  ('recruiter', '招聘专员', 'company', 1),
  ('interviewer', '面试官', 'company', 1),
  ('operator', '运营人员', 'admin', 1),
  ('auditor', '系统审计员', 'admin', 1),
  ('open_client', '对接接口客户端', 'open', 1)
AS new
ON DUPLICATE KEY UPDATE
  role_name = new.role_name,
  role_type = new.role_type,
  status = new.status,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_menu (menu_code, menu_name, parent_id, menu_type, route_path, api_code, sort_no, status)
VALUES
  ('portal_home', '门户首页', NULL, 'page', '/', NULL, 10, 1),
  ('portal_jobs', '职位浏览', NULL, 'page', '/jobs', NULL, 20, 1),
  ('portal_companies', '企业浏览', NULL, 'page', '/companies', NULL, 30, 1),
  ('portal_activities', '活动浏览', NULL, 'page', '/activities', NULL, 40, 1),
  ('portal_snapshots', '人才服务区发布快照', NULL, 'page', '/talent-service', NULL, 50, 1),
  ('candidate_center', '求职者中心', NULL, 'page', '/candidate', NULL, 100, 1),
  ('candidate_center_overview', '求职者中心概览读取', NULL, 'api', NULL, 'candidate.center.overview', 601, 1),
  ('candidate_center_resume_read', '求职者简历读取', NULL, 'api', NULL, 'candidate.center.resume.read', 602, 1),
  ('candidate_center_resume_write', '求职者简历写入', NULL, 'api', NULL, 'candidate.center.resume.write', 603, 1),
  ('candidate_center_application_list', '求职者投递记录读取', NULL, 'api', NULL, 'candidate.center.application.list', 604, 1),
  ('candidate_center_favorite_read', '求职者收藏读取', NULL, 'api', NULL, 'candidate.center.favorite.read', 605, 1),
  ('candidate_center_favorite_write', '求职者收藏写入', NULL, 'api', NULL, 'candidate.center.favorite.write', 606, 1),
  ('candidate_center_subscription_read', '求职者订阅读取', NULL, 'api', NULL, 'candidate.center.subscription.read', 607, 1),
  ('candidate_center_subscription_write', '求职者订阅写入', NULL, 'api', NULL, 'candidate.center.subscription.write', 608, 1),
  ('candidate_center_notification_read', '求职者站内通知读取', NULL, 'api', NULL, 'candidate.center.notification.read', 609, 1),
  ('candidate_center_notification_write', '求职者站内通知写入', NULL, 'api', NULL, 'candidate.center.notification.write', 611, 1),
  ('company_console', '企业后台', NULL, 'page', '/company', NULL, 200, 1),
  ('operator_console', '运营后台', NULL, 'page', '/admin', NULL, 300, 1),
  ('audit_console', '审计中心', NULL, 'page', '/audit', NULL, 400, 1),
  ('open_api_stub', '对接接口占位', NULL, 'api', NULL, 'open.stub', 500, 1),
  ('open_jobs_sync', '对接职位同步 Stub', NULL, 'api', NULL, 'open.jobs.sync', 501, 1),
  ('open_applications_sync', '对接投递同步 Stub', NULL, 'api', NULL, 'open.applications.sync', 502, 1),
  ('open_consents_callback', '对接同意回调 Stub', NULL, 'api', NULL, 'open.consents.callback', 503, 1),
  ('open_candidates_publishable_sync', '对接发布快照同步 Stub', NULL, 'api', NULL, 'open.candidates.publishable_sync', 504, 1),
  ('open_mappings_query', '对接映射查询 Stub', NULL, 'api', NULL, 'open.mappings.query', 505, 1),
  ('candidate_consent_create', '候选人提交同意', NULL, 'api', NULL, 'candidate.consent.create', 610, 1),
  ('candidate_consent_revoke', '候选人撤回同意', NULL, 'api', NULL, 'candidate.consent.revoke', 620, 1),
  ('company_apply', '企业认证提交', NULL, 'api', NULL, 'company.apply', 710, 1),
  ('company_workbench_read', '企业工作台读取', NULL, 'api', NULL, 'company.workbench.read', 711, 1),
  ('company_workbench_write', '企业工作台写入', NULL, 'api', NULL, 'company.workbench.write', 712, 1),
  ('company_profile_read', '企业资料读取', NULL, 'api', NULL, 'company.profile.read', 713, 1),
  ('company_profile_write', '企业资料写入', NULL, 'api', NULL, 'company.profile.write', 714, 1),
  ('company_certification_submit', '企业认证材料提交', NULL, 'api', NULL, 'company.certification.submit', 715, 1),
  ('company_resume_search_read', '企业受控简历快照搜索读取', NULL, 'api', NULL, 'company.resume-search.read', 716, 1),
  ('company_job_list', '企业职位列表', NULL, 'api', NULL, 'company.job.list', 720, 1),
  ('company_job_create', '企业职位创建', NULL, 'api', NULL, 'company.job.create', 730, 1),
  ('company_job_read', '企业职位查看', NULL, 'api', NULL, 'company.job.read', 740, 1),
  ('company_job_update', '企业职位编辑', NULL, 'api', NULL, 'company.job.update', 750, 1),
  ('company_job_status', '企业职位状态变更', NULL, 'api', NULL, 'company.job.status', 760, 1),
  ('candidate_application_create', '候选人职位投递', NULL, 'api', NULL, 'candidate.application.create', 770, 1),
  ('candidate_interview_signin', '候选人面试签到', NULL, 'api', NULL, 'candidate.interview.signin', 780, 1),
  ('company_application_list', '企业投递池列表', NULL, 'api', NULL, 'company.application.list', 790, 1),
  ('company_application_read', '企业投递详情', NULL, 'api', NULL, 'company.application.read', 800, 1),
  ('company_interview_session_create', '企业创建面试场次', NULL, 'api', NULL, 'company.interview.session.create', 805, 1),
  ('company_interview_qrcode_generate', '企业生成签到码', NULL, 'api', NULL, 'company.interview.qrcode.generate', 806, 1),
  ('company_interview_list', '企业面试场次读取', NULL, 'api', NULL, 'company.interview.list', 807, 1),
  ('company_application_stage', '企业投递阶段流转', NULL, 'api', NULL, 'company.application.stage', 808, 1),
  ('admin_company_review', '运营企业审核', NULL, 'api', NULL, 'admin.company.review', 810, 1),
  ('admin_company_review_read', '运营企业审核读取', NULL, 'api', NULL, 'admin.company.review.read', 811, 1),
  ('admin_company_review_write', '运营企业审核写入', NULL, 'api', NULL, 'admin.company.review.write', 812, 1),
  ('admin_job_review', '运营职位审核', NULL, 'api', NULL, 'admin.job.review', 820, 1),
  ('admin_job_review_read', '运营职位审核读取', NULL, 'api', NULL, 'admin.job.review.read', 821, 1),
  ('admin_job_review_write', '运营职位审核写入', NULL, 'api', NULL, 'admin.job.review.write', 822, 1),
  ('admin_cms_read', '运营内容读取', NULL, 'api', NULL, 'admin.cms.read', 830, 1),
  ('admin_cms_write', '运营内容写入', NULL, 'api', NULL, 'admin.cms.write', 831, 1),
  ('admin_event_read', '运营活动读取', NULL, 'api', NULL, 'admin.event.read', 840, 1),
  ('admin_event_write', '运营活动写入', NULL, 'api', NULL, 'admin.event.write', 841, 1),
  ('company_export_apply', '企业导出申请', NULL, 'api', NULL, 'company.export.apply', 850, 1),
  ('company_export_read', '企业导出查看', NULL, 'api', NULL, 'company.export.read', 851, 1),
  ('company_export_download', '企业导出下载链接', NULL, 'api', NULL, 'company.export.download', 852, 1),
  ('admin_export_review_read', '运营导出审批读取', NULL, 'api', NULL, 'admin.export.review.read', 860, 1),
  ('admin_export_review_write', '运营导出审批写入', NULL, 'api', NULL, 'admin.export.review.write', 861, 1),
  ('admin_audit_log_read', '审计日志读取', NULL, 'api', NULL, 'admin.audit-log.read', 870, 1),
  ('admin_field_access_log_read', '字段访问日志读取', NULL, 'api', NULL, 'admin.field-access-log.read', 871, 1),
  ('admin_open_api_log_read', '对接调用日志读取', NULL, 'api', NULL, 'admin.open-api-log.read', 872, 1),
  ('admin_audit_trace_read', '审计链路读取', NULL, 'api', NULL, 'admin.audit-trace.read', 873, 1),
  ('admin_ops_read', '运营生产化首页读取', NULL, 'api', NULL, 'admin.ops.read', 880, 1),
  ('admin_recommendation_read', '运营推荐位读取', NULL, 'api', NULL, 'admin.recommendation.read', 881, 1),
  ('admin_recommendation_write', '运营推荐位写入', NULL, 'api', NULL, 'admin.recommendation.write', 882, 1),
  ('admin_risk_review_read', '运营风险审核读取', NULL, 'api', NULL, 'admin.risk-review.read', 883, 1),
  ('admin_risk_review_write', '运营风险审核写入', NULL, 'api', NULL, 'admin.risk-review.write', 884, 1),
  ('admin_portal_config_read', '门户运营配置读取', NULL, 'api', NULL, 'admin.portal-config.read', 885, 1),
  ('admin_portal_config_write', '门户运营配置写入', NULL, 'api', NULL, 'admin.portal-config.write', 886, 1)
AS new
ON DUPLICATE KEY UPDATE
  menu_name = new.menu_name,
  menu_type = new.menu_type,
  route_path = new.route_path,
  api_code = new.api_code,
  sort_no = new.sort_no,
  status = new.status,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON (
  (r.role_code = 'candidate' AND m.menu_code IN (
    'portal_home',
    'portal_jobs',
    'portal_companies',
    'portal_activities',
    'portal_snapshots',
    'candidate_center',
    'candidate_center_overview',
    'candidate_center_resume_read',
    'candidate_center_resume_write',
    'candidate_center_application_list',
    'candidate_center_favorite_read',
    'candidate_center_favorite_write',
    'candidate_center_subscription_read',
    'candidate_center_subscription_write',
    'candidate_center_notification_read',
    'candidate_center_notification_write',
    'candidate_consent_create',
    'candidate_consent_revoke',
    'candidate_application_create',
    'candidate_interview_signin'
  ))
  OR (r.role_code = 'company_admin' AND m.menu_code IN (
    'company_console',
    'company_apply',
    'company_workbench_read',
    'company_workbench_write',
    'company_profile_read',
    'company_profile_write',
    'company_certification_submit',
    'company_resume_search_read',
    'company_job_list',
    'company_job_create',
    'company_job_read',
    'company_job_update',
    'company_job_status',
    'company_application_list',
    'company_application_read',
    'company_interview_session_create',
    'company_interview_qrcode_generate',
    'company_interview_list',
    'company_application_stage',
    'company_export_apply',
    'company_export_read',
    'company_export_download'
  ))
  OR (r.role_code = 'recruiter' AND m.menu_code IN (
    'company_console',
    'company_apply',
    'company_workbench_read',
    'company_workbench_write',
    'company_profile_read',
    'company_profile_write',
    'company_certification_submit',
    'company_resume_search_read',
    'company_job_list',
    'company_job_create',
    'company_job_read',
    'company_job_update',
    'company_job_status',
    'company_application_list',
    'company_application_read',
    'company_interview_session_create',
    'company_interview_qrcode_generate',
    'company_interview_list',
    'company_application_stage',
    'company_export_apply',
    'company_export_read',
    'company_export_download'
  ))
  OR (r.role_code = 'interviewer' AND m.menu_code IN ('company_console'))
  OR (r.role_code = 'operator' AND m.menu_code IN (
    'operator_console',
    'admin_company_review',
    'admin_company_review_read',
    'admin_company_review_write',
    'admin_job_review',
    'admin_job_review_read',
    'admin_job_review_write',
    'admin_cms_read',
    'admin_cms_write',
    'admin_event_read',
    'admin_event_write',
    'admin_export_review_read',
    'admin_export_review_write',
    'admin_audit_log_read',
    'admin_field_access_log_read',
    'admin_open_api_log_read',
    'admin_audit_trace_read',
    'admin_ops_read',
    'admin_recommendation_read',
    'admin_recommendation_write',
    'admin_risk_review_read',
    'admin_risk_review_write',
    'admin_portal_config_read',
    'admin_portal_config_write'
  ))
  OR (r.role_code = 'auditor' AND m.menu_code IN (
    'audit_console',
    'admin_company_review_read',
    'admin_job_review_read',
    'admin_cms_read',
    'admin_event_read',
    'admin_export_review_read',
    'admin_audit_log_read',
    'admin_field_access_log_read',
    'admin_open_api_log_read',
    'admin_audit_trace_read',
    'admin_ops_read',
    'admin_recommendation_read',
    'admin_risk_review_read',
    'admin_portal_config_read'
  ))
  OR (r.role_code = 'open_client' AND m.menu_code IN (
    'open_api_stub',
    'open_jobs_sync',
    'open_applications_sync',
    'open_consents_callback',
    'open_candidates_publishable_sync',
    'open_mappings_query'
  ))
)
ON DUPLICATE KEY UPDATE
  sys_role_menu.id = sys_role_menu.id;

INSERT INTO sys_role_field_policy (role_id, biz_type, field_name, policy_type, mask_rule, status)
SELECT r.id, v.biz_type, v.field_name, v.policy_type, v.mask_rule, 1
FROM sys_role r
JOIN (
  SELECT 'company_admin' AS role_code, 'export_application' AS biz_type, 'application_id' AS field_name, 'ALLOW' AS policy_type, NULL AS mask_rule
  UNION ALL SELECT 'company_admin', 'export_application', 'job_title', 'ALLOW', NULL
  UNION ALL SELECT 'company_admin', 'export_application', 'application_status', 'ALLOW', NULL
  UNION ALL SELECT 'company_admin', 'export_application', 'apply_time', 'ALLOW', NULL
  UNION ALL SELECT 'company_admin', 'export_application', 'display_name', 'MASK', 'NAME'
  UNION ALL SELECT 'company_admin', 'export_application', 'mobile', 'MASK', 'MOBILE'
  UNION ALL SELECT 'company_admin', 'export_application', 'email', 'MASK', 'EMAIL'
  UNION ALL SELECT 'company_admin', 'export_application', 'skills_summary', 'MASK', 'DEFAULT'
  UNION ALL SELECT 'recruiter', 'export_application', 'application_id', 'ALLOW', NULL
  UNION ALL SELECT 'recruiter', 'export_application', 'job_title', 'ALLOW', NULL
  UNION ALL SELECT 'recruiter', 'export_application', 'application_status', 'ALLOW', NULL
  UNION ALL SELECT 'recruiter', 'export_application', 'apply_time', 'ALLOW', NULL
  UNION ALL SELECT 'recruiter', 'export_application', 'display_name', 'MASK', 'NAME'
  UNION ALL SELECT 'recruiter', 'export_application', 'mobile', 'MASK', 'MOBILE'
  UNION ALL SELECT 'recruiter', 'export_application', 'email', 'MASK', 'EMAIL'
  UNION ALL SELECT 'recruiter', 'export_application', 'skills_summary', 'MASK', 'DEFAULT'
) v ON v.role_code = r.role_code
ON DUPLICATE KEY UPDATE
  policy_type = v.policy_type,
  mask_rule = v.mask_rule,
  status = 1;
