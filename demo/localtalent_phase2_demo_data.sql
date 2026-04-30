SET NAMES utf8mb4;
SET @phase2_password_hash := '$2b$12$5G4kZ1x4dOar/VtPyL38UuxDYUreMg03NyJG11tnsQvlMZTSZhP2.';

START TRANSACTION;

DELETE FROM field_access_log WHERE trace_id LIKE 'trace-p2-demo%';
DELETE FROM audit_log WHERE trace_id LIKE 'trace-p2-demo%';
DELETE FROM open_api_log WHERE trace_id LIKE 'trace-p2-demo%';
DELETE FROM integration_sync_task WHERE trace_id LIKE 'trace-p2-demo%';

DELETE FROM cms_content
WHERE title LIKE 'LocalTalent 二期%';

DELETE FROM activity_event
WHERE title LIKE 'LocalTalent 二期%';

DELETE s
FROM interview_signin s
JOIN candidate_user cu ON cu.id = s.candidate_id
WHERE cu.email IN (
  'p2_candidate@localtalent.test',
  'p2_revoked@localtalent.test',
  'p2_pending@localtalent.test'
);

DELETE sess
FROM interview_session sess
JOIN job_application app ON app.id = sess.application_id
JOIN candidate_user cu ON cu.id = app.candidate_id
WHERE cu.email IN (
  'p2_candidate@localtalent.test',
  'p2_revoked@localtalent.test',
  'p2_pending@localtalent.test'
);

DELETE app
FROM job_application app
JOIN candidate_user cu ON cu.id = app.candidate_id
WHERE cu.email IN (
  'p2_candidate@localtalent.test',
  'p2_revoked@localtalent.test',
  'p2_pending@localtalent.test'
);

DELETE ps
FROM candidate_publish_snapshot ps
JOIN candidate_user cu ON cu.id = ps.candidate_id
WHERE cu.email IN (
  'p2_candidate@localtalent.test',
  'p2_revoked@localtalent.test',
  'p2_pending@localtalent.test'
);

DELETE cp
FROM candidate_control_profile cp
JOIN candidate_user cu ON cu.id = cp.candidate_id
WHERE cu.email IN (
  'p2_candidate@localtalent.test',
  'p2_revoked@localtalent.test',
  'p2_pending@localtalent.test'
);

DELETE cc
FROM candidate_consent cc
JOIN candidate_user cu ON cu.id = cc.candidate_id
WHERE cu.email IN (
  'p2_candidate@localtalent.test',
  'p2_revoked@localtalent.test',
  'p2_pending@localtalent.test'
);

DELETE cr
FROM candidate_resume cr
JOIN candidate_user cu ON cu.id = cr.candidate_id
WHERE cu.email IN (
  'p2_candidate@localtalent.test',
  'p2_revoked@localtalent.test',
  'p2_pending@localtalent.test'
);

INSERT INTO company (
  company_name,
  license_no,
  industry_code,
  nature_code,
  scale_code,
  city_code,
  address,
  company_profile,
  auth_status,
  auth_reject_reason,
  auth_review_user_id,
  auth_review_time,
  auth_submit_time,
  source_system
) VALUES (
  'LocalTalent 二期智造科技有限公司',
  'LT-P2-COMPANY-APPROVED-1',
  'internet',
  'private',
  '100-500',
  '310000',
  '上海市浦东新区演示大道 100 号',
  '二期高保真门户演示企业，专注地方人才服务、企业数字化和招聘流程协同。',
  2,
  NULL,
  1,
  DATE_SUB(NOW(), INTERVAL 14 DAY),
  DATE_SUB(NOW(), INTERVAL 15 DAY),
  'portal'
), (
  'LocalTalent 二期制造服务有限公司',
  'LT-P2-COMPANY-APPROVED-2',
  'manufacturing',
  'private',
  '50-100',
  '320900',
  '盐城市演示园区 18 号',
  '二期找企业频道演示企业，提供制造业岗位与招聘会参会岗位。',
  2,
  NULL,
  1,
  DATE_SUB(NOW(), INTERVAL 12 DAY),
  DATE_SUB(NOW(), INTERVAL 13 DAY),
  'portal'
), (
  'LocalTalent 二期待审核企业',
  'LT-P2-COMPANY-PENDING',
  'service',
  'private',
  '1-49',
  '310000',
  '上海市演示路 19 号',
  '用于验证未认证企业不会出现在公开找企业频道。',
  1,
  NULL,
  NULL,
  NULL,
  DATE_SUB(NOW(), INTERVAL 1 DAY),
  'portal'
) AS new
ON DUPLICATE KEY UPDATE
  company_name = new.company_name,
  industry_code = new.industry_code,
  nature_code = new.nature_code,
  scale_code = new.scale_code,
  city_code = new.city_code,
  address = new.address,
  company_profile = new.company_profile,
  auth_status = new.auth_status,
  auth_reject_reason = new.auth_reject_reason,
  auth_review_user_id = new.auth_review_user_id,
  auth_review_time = new.auth_review_time,
  auth_submit_time = new.auth_submit_time,
  source_system = new.source_system,
  updated_at = CURRENT_TIMESTAMP;

SET @p2_company_id := (
  SELECT id FROM company WHERE license_no = 'LT-P2-COMPANY-APPROVED-1' LIMIT 1
);
SET @p2_company_second_id := (
  SELECT id FROM company WHERE license_no = 'LT-P2-COMPANY-APPROVED-2' LIMIT 1
);
SET @p2_company_pending_id := (
  SELECT id FROM company WHERE license_no = 'LT-P2-COMPANY-PENDING' LIMIT 1
);

INSERT INTO company_user (
  company_id,
  user_name,
  role_code,
  mobile,
  email,
  password_hash,
  status
) VALUES (
  @p2_company_id,
  '二期演示企业管理员',
  'company_admin',
  NULL,
  'p2_company@localtalent.test',
  @phase2_password_hash,
  1
) AS new
ON DUPLICATE KEY UPDATE
  user_name = new.user_name,
  role_code = new.role_code,
  password_hash = new.password_hash,
  status = new.status,
  updated_at = CURRENT_TIMESTAMP;

SET @p2_company_user_id := (
  SELECT id FROM company_user
  WHERE company_id = @p2_company_id AND email = 'p2_company@localtalent.test'
  LIMIT 1
);

INSERT INTO candidate_user (
  mobile,
  email,
  password_hash,
  real_name,
  realname_verified_flag,
  register_channel,
  status
) VALUES (
  NULL,
  'p2_candidate@localtalent.test',
  @phase2_password_hash,
  '沈星河',
  1,
  'portal',
  1
), (
  NULL,
  'p2_revoked@localtalent.test',
  @phase2_password_hash,
  '顾青',
  1,
  'portal',
  1
), (
  NULL,
  'p2_pending@localtalent.test',
  @phase2_password_hash,
  '程澈',
  0,
  'portal',
  1
) AS new
ON DUPLICATE KEY UPDATE
  password_hash = new.password_hash,
  real_name = new.real_name,
  realname_verified_flag = new.realname_verified_flag,
  register_channel = new.register_channel,
  status = new.status,
  updated_at = CURRENT_TIMESTAMP;

SET @p2_candidate_id := (
  SELECT id FROM candidate_user WHERE email = 'p2_candidate@localtalent.test' LIMIT 1
);
SET @p2_revoked_id := (
  SELECT id FROM candidate_user WHERE email = 'p2_revoked@localtalent.test' LIMIT 1
);
SET @p2_pending_id := (
  SELECT id FROM candidate_user WHERE email = 'p2_pending@localtalent.test' LIMIT 1
);
SET @p2_operator_id := (
  SELECT id FROM admin_user WHERE username = 'operator' LIMIT 1
);

INSERT INTO sys_user_role (user_type, user_id, role_id)
SELECT 'candidate', v.user_id, r.id
FROM sys_role r
JOIN (
  SELECT @p2_candidate_id AS user_id
  UNION ALL SELECT @p2_revoked_id
  UNION ALL SELECT @p2_pending_id
) v
WHERE r.role_code = 'candidate'
ON DUPLICATE KEY UPDATE sys_user_role.id = sys_user_role.id;

INSERT INTO sys_user_role (user_type, user_id, role_id)
SELECT 'company_user', @p2_company_user_id, r.id
FROM sys_role r
WHERE r.role_code = 'company_admin'
ON DUPLICATE KEY UPDATE sys_user_role.id = sys_user_role.id;

INSERT INTO job_post (
  company_id,
  external_job_id,
  source_type,
  title,
  category_code,
  city_code,
  salary_min,
  salary_max,
  job_desc,
  status,
  audit_status,
  review_memo,
  reject_reason,
  review_user_id,
  review_time,
  published_at,
  offline_reason,
  status_changed_at
) VALUES (
  @p2_company_id,
  'LT-P2-JOB-JAVA',
  1,
  'Java 平台工程师',
  'software',
  '310000',
  18000,
  32000,
  '参与地方人才网高保真门户与招聘闭环建设，负责 Java、Spring Boot、MySQL 与审计链路。',
  2,
  2,
  '二期演示审核通过',
  NULL,
  @p2_operator_id,
  DATE_SUB(NOW(), INTERVAL 10 DAY),
  DATE_SUB(NOW(), INTERVAL 10 DAY),
  NULL,
  DATE_SUB(NOW(), INTERVAL 10 DAY)
), (
  @p2_company_id,
  'LT-P2-JOB-FRONTEND',
  1,
  'Next.js 前端工程师',
  'software',
  '310000',
  16000,
  28000,
  '负责 Next/SSR 门户页面、搜索体验和公开字段白名单渲染。',
  2,
  2,
  '二期演示审核通过',
  NULL,
  @p2_operator_id,
  DATE_SUB(NOW(), INTERVAL 9 DAY),
  DATE_SUB(NOW(), INTERVAL 9 DAY),
  NULL,
  DATE_SUB(NOW(), INTERVAL 9 DAY)
), (
  @p2_company_second_id,
  'LT-P2-JOB-OPERATION',
  1,
  '招聘会运营专员',
  'operations',
  '320900',
  8000,
  12000,
  '负责招聘会公开频道、企业参会组织和线下活动运营。',
  2,
  2,
  '二期演示审核通过',
  NULL,
  @p2_operator_id,
  DATE_SUB(NOW(), INTERVAL 7 DAY),
  DATE_SUB(NOW(), INTERVAL 7 DAY),
  NULL,
  DATE_SUB(NOW(), INTERVAL 7 DAY)
), (
  @p2_company_pending_id,
  'LT-P2-JOB-HIDDEN',
  1,
  '未认证企业隐藏职位',
  'software',
  '310000',
  10000,
  18000,
  '该职位用于验证未认证企业职位不会公开展示。',
  2,
  2,
  '二期演示审核通过',
  NULL,
  @p2_operator_id,
  DATE_SUB(NOW(), INTERVAL 4 DAY),
  DATE_SUB(NOW(), INTERVAL 4 DAY),
  NULL,
  DATE_SUB(NOW(), INTERVAL 4 DAY)
) AS new
ON DUPLICATE KEY UPDATE
  company_id = new.company_id,
  title = new.title,
  category_code = new.category_code,
  city_code = new.city_code,
  salary_min = new.salary_min,
  salary_max = new.salary_max,
  job_desc = new.job_desc,
  status = new.status,
  audit_status = new.audit_status,
  review_memo = new.review_memo,
  reject_reason = new.reject_reason,
  review_user_id = new.review_user_id,
  review_time = new.review_time,
  published_at = new.published_at,
  offline_reason = new.offline_reason,
  status_changed_at = new.status_changed_at,
  updated_at = CURRENT_TIMESTAMP;

SET @p2_job_id := (
  SELECT id FROM job_post WHERE source_type = 1 AND external_job_id = 'LT-P2-JOB-JAVA' LIMIT 1
);

INSERT INTO candidate_resume (
  candidate_id,
  resume_name,
  base_profile_json,
  education_json,
  experience_json,
  skills_json,
  attachment_object_key,
  status
) VALUES (
  @p2_candidate_id,
  '二期演示简历-已发布',
  JSON_OBJECT('city_code', '310000', 'category_code', 'software', 'experience_years', 6),
  JSON_ARRAY(JSON_OBJECT('school', 'LocalTalent University', 'degree', 'Bachelor')),
  JSON_ARRAY(JSON_OBJECT('company', 'Demo Platform Team', 'title', 'Senior Engineer')),
  JSON_OBJECT('summary', 'Java / Spring Boot / Next SSR / Audit', 'skills', JSON_ARRAY('Java', 'Spring Boot', 'Next SSR', 'Audit')),
  'demo/private/p2-resume-published.pdf',
  1
), (
  @p2_revoked_id,
  '二期演示简历-已撤回',
  JSON_OBJECT('city_code', '310000', 'category_code', 'product', 'experience_years', 4),
  JSON_ARRAY(),
  JSON_ARRAY(JSON_OBJECT('company', 'Demo Product Team', 'title', 'Product Manager')),
  JSON_OBJECT('summary', '产品规划 / 数据分析 / 流程设计', 'skills', JSON_ARRAY('Product', 'Data')),
  'demo/private/p2-resume-revoked.pdf',
  1
);

SET @p2_resume_id := (
  SELECT id FROM candidate_resume
  WHERE candidate_id = @p2_candidate_id AND resume_name = '二期演示简历-已发布'
  ORDER BY id DESC LIMIT 1
);

INSERT INTO candidate_consent (
  candidate_id,
  consent_status,
  consent_scope,
  consent_version,
  consent_channel,
  consent_time,
  realname_verified_flag,
  second_confirm_flag,
  revoke_status,
  revoke_time
) VALUES (
  @p2_candidate_id,
  1,
  JSON_ARRAY('talent_service_area'),
  'phase2-v1',
  'portal',
  DATE_SUB(NOW(), INTERVAL 6 DAY),
  1,
  1,
  0,
  NULL
), (
  @p2_revoked_id,
  2,
  JSON_ARRAY('talent_service_area'),
  'phase2-v1',
  'portal',
  DATE_SUB(NOW(), INTERVAL 8 DAY),
  1,
  1,
  1,
  DATE_SUB(NOW(), INTERVAL 2 DAY)
);

SET @p2_consent_id := (
  SELECT id FROM candidate_consent
  WHERE candidate_id = @p2_candidate_id
  ORDER BY id DESC LIMIT 1
);
SET @p2_revoked_consent_id := (
  SELECT id FROM candidate_consent
  WHERE candidate_id = @p2_revoked_id
  ORDER BY id DESC LIMIT 1
);

INSERT INTO candidate_publish_snapshot (
  candidate_id,
  source_type,
  legal_basis,
  consent_status,
  publishable_flag,
  consent_version,
  visibility_scope,
  snapshot_json,
  sync_version,
  status,
  created_at,
  updated_at
) VALUES (
  @p2_candidate_id,
  1,
  'consent',
  1,
  1,
  'phase2-v1',
  4,
  JSON_OBJECT(
    'display_name_masked', '沈**',
    'city_code', '310000',
    'category_code', 'software',
    'skills_summary', 'Java / Spring Boot / Next SSR / Audit',
    'experience_years', 6,
    'updated_at', DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%i:%s')
  ),
  1,
  1,
  DATE_SUB(NOW(), INTERVAL 6 DAY),
  NOW()
), (
  @p2_revoked_id,
  1,
  'consent',
  2,
  0,
  'phase2-v1',
  4,
  JSON_OBJECT(
    'display_name_masked', '顾**',
    'city_code', '310000',
    'category_code', 'product',
    'skills_summary', '产品规划 / 数据分析 / 流程设计',
    'experience_years', 4,
    'updated_at', DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 2 DAY), '%Y-%m-%dT%H:%i:%s')
  ),
  1,
  0,
  DATE_SUB(NOW(), INTERVAL 8 DAY),
  DATE_SUB(NOW(), INTERVAL 2 DAY)
);

SET @p2_snapshot_id := (
  SELECT id FROM candidate_publish_snapshot
  WHERE candidate_id = @p2_candidate_id
  ORDER BY id DESC LIMIT 1
);

INSERT INTO candidate_control_profile (
  candidate_id,
  source_type,
  legal_basis,
  consent_status,
  publishable_flag,
  visibility_scope,
  consent_version,
  current_consent_id,
  current_snapshot_id,
  control_status
) VALUES (
  @p2_candidate_id,
  1,
  'consent',
  1,
  1,
  4,
  'phase2-v1',
  @p2_consent_id,
  @p2_snapshot_id,
  1
), (
  @p2_revoked_id,
  1,
  'consent',
  2,
  0,
  4,
  'phase2-v1',
  @p2_revoked_consent_id,
  NULL,
  1
), (
  @p2_pending_id,
  1,
  'consent',
  0,
  0,
  2,
  NULL,
  NULL,
  NULL,
  1
) AS new
ON DUPLICATE KEY UPDATE
  consent_status = new.consent_status,
  publishable_flag = new.publishable_flag,
  visibility_scope = new.visibility_scope,
  consent_version = new.consent_version,
  current_consent_id = new.current_consent_id,
  current_snapshot_id = new.current_snapshot_id,
  control_status = new.control_status,
  updated_at = CURRENT_TIMESTAMP;

INSERT INTO job_application (
  job_id,
  candidate_id,
  resume_id,
  source_type,
  external_application_id,
  application_status,
  apply_time
) VALUES (
  @p2_job_id,
  @p2_candidate_id,
  @p2_resume_id,
  1,
  'LT-P2-APPLICATION-PUBLISHED',
  3,
  DATE_SUB(NOW(), INTERVAL 3 DAY)
) AS new
ON DUPLICATE KEY UPDATE
  resume_id = new.resume_id,
  application_status = new.application_status,
  apply_time = new.apply_time,
  updated_at = CURRENT_TIMESTAMP;

SET @p2_application_id := (
  SELECT id FROM job_application
  WHERE external_application_id = 'LT-P2-APPLICATION-PUBLISHED'
  LIMIT 1
);

INSERT INTO interview_session (
  application_id,
  job_id,
  company_id,
  session_name,
  session_time,
  location,
  qr_code,
  signin_code_hash,
  signin_code_expires_at,
  signin_code_used_at,
  status
) VALUES (
  @p2_application_id,
  @p2_job_id,
  @p2_company_id,
  '二期演示面试场次',
  DATE_ADD(NOW(), INTERVAL 3 DAY),
  '上海市浦东新区二期演示会议室',
  NULL,
  'phase2-demo-signin-code-hash',
  DATE_ADD(NOW(), INTERVAL 1 DAY),
  NOW(),
  1
) AS new
ON DUPLICATE KEY UPDATE
  session_name = new.session_name,
  session_time = new.session_time,
  location = new.location,
  signin_code_hash = new.signin_code_hash,
  signin_code_expires_at = new.signin_code_expires_at,
  signin_code_used_at = new.signin_code_used_at,
  status = new.status,
  updated_at = CURRENT_TIMESTAMP;

SET @p2_session_id := (
  SELECT id FROM interview_session
  WHERE application_id = @p2_application_id
  LIMIT 1
);

INSERT INTO interview_signin (
  session_id,
  candidate_id,
  sign_channel,
  sign_time,
  device_id,
  consent_redirect_flag
) VALUES (
  @p2_session_id,
  @p2_candidate_id,
  'qrcode',
  DATE_SUB(NOW(), INTERVAL 1 DAY),
  'p2-demo-device',
  0
) AS new
ON DUPLICATE KEY UPDATE
  sign_channel = new.sign_channel,
  sign_time = new.sign_time,
  device_id = new.device_id,
  consent_redirect_flag = new.consent_redirect_flag;

INSERT INTO activity_event (
  title,
  type_code,
  city_code,
  start_time,
  end_time,
  location,
  organizer_company_id,
  status
) VALUES (
  'LocalTalent 二期春季综合招聘会',
  'onsite',
  '310000',
  DATE_ADD(NOW(), INTERVAL 9 DAY),
  DATE_ADD(DATE_ADD(NOW(), INTERVAL 9 DAY), INTERVAL 4 HOUR),
  '上海市人才服务中心二期演示厅',
  @p2_company_id,
  1
), (
  'LocalTalent 二期网络招聘会占位',
  'online',
  '310000',
  DATE_ADD(NOW(), INTERVAL 12 DAY),
  DATE_ADD(DATE_ADD(NOW(), INTERVAL 12 DAY), INTERVAL 6 HOUR),
  '线上公开展示占位，不接真实直播或视频',
  @p2_company_id,
  1
), (
  'LocalTalent 二期下线招聘会',
  'onsite',
  '310000',
  DATE_SUB(NOW(), INTERVAL 5 DAY),
  DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 3 HOUR,
  '该活动用于验证下线不可见',
  @p2_company_id,
  0
);

INSERT INTO cms_content (
  content_type,
  title,
  cover_url,
  summary,
  body_html,
  city_code,
  status,
  publish_time
) VALUES (
  'policy',
  'LocalTalent 二期就业政策：高校毕业生就业支持',
  NULL,
  '面向公开门户的就业政策摘要，用于二期政策频道演示。',
  '<p>本政策内容为公开演示文本，介绍高校毕业生就业支持、职业指导和公共服务资源。</p>',
  '310000',
  1,
  DATE_SUB(NOW(), INTERVAL 2 DAY)
), (
  'news',
  'LocalTalent 二期职场资讯：高保真人才网体验升级',
  NULL,
  '职场资讯频道演示内容，展示公开摘要和安全正文。',
  '<p>二期门户补齐首页、职位、企业、招聘会与工具箱频道，帮助求职者更快进入招聘闭环。</p>',
  '310000',
  1,
  DATE_SUB(NOW(), INTERVAL 1 DAY)
), (
  'notice',
  'LocalTalent 二期公告：公开频道演示数据已准备',
  NULL,
  '公告频道演示内容，提示二期 demo 数据可用于本地验收。',
  '<p>二期演示数据仅用于本地，不作为生产初始化数据。</p>',
  '310000',
  1,
  NOW()
), (
  'hr_tool',
  'LocalTalent 二期 HR 工具箱：面试安排清单',
  NULL,
  'HR 工具箱演示内容，提供公开流程建议，不包含候选人明细。',
  '<p>面试安排建议包含场次确认、候选人通知、签到复核和审计留痕。</p>',
  '310000',
  1,
  DATE_SUB(NOW(), INTERVAL 3 DAY)
), (
  'help',
  'LocalTalent 二期帮助中心：如何使用演示账号',
  NULL,
  '帮助中心演示内容，说明本地账号和登录入口。',
  '<p>请使用 /auth/login 或 scripts/demo_login_tokens 获取本地演示登录状态。</p>',
  '310000',
  1,
  DATE_SUB(NOW(), INTERVAL 4 DAY)
), (
  'policy',
  'LocalTalent 二期下线政策',
  NULL,
  '该内容用于验证下线不可见。',
  '<p>下线内容不应出现在公开频道。</p>',
  '310000',
  0,
  DATE_SUB(NOW(), INTERVAL 8 DAY)
);

INSERT INTO audit_log (
  operator_id,
  operator_role,
  biz_type,
  biz_id,
  action_type,
  before_json,
  after_json,
  trace_id,
  created_at
) VALUES (
  @p2_operator_id,
  'operator',
  'phase2_demo',
  @p2_job_id,
  'phase2_demo_seed',
  NULL,
  JSON_OBJECT('prompt', 'Prompt 23', 'demo', TRUE, 'scope', 'portal'),
  'trace-p2-demo',
  NOW()
), (
  @p2_candidate_id,
  'candidate',
  'candidate_consent',
  @p2_consent_id,
  'consent_create',
  NULL,
  JSON_OBJECT('status', 'consented', 'demo', TRUE),
  'trace-p2-demo',
  DATE_SUB(NOW(), INTERVAL 6 DAY)
);

INSERT INTO field_access_log (
  operator_id,
  operator_role,
  biz_type,
  biz_id,
  field_name,
  access_type,
  trace_id,
  created_at
) VALUES (
  @p2_candidate_id,
  'candidate',
  'candidate_publish_snapshot',
  @p2_snapshot_id,
  'display_name',
  'MASK',
  'trace-p2-demo',
  DATE_SUB(NOW(), INTERVAL 6 DAY)
);

INSERT INTO open_api_log (
  source_system,
  client_code,
  api_code,
  trace_id,
  request_uri,
  request_method,
  request_hash,
  idempotency_key,
  biz_type,
  biz_id,
  http_status,
  success_flag,
  duration_ms,
  request_summary_json,
  response_summary_json,
  request_time,
  response_time
) VALUES (
  'stub_partner',
  'localtalent_stub',
  'open.jobs.sync',
  'trace-p2-demo',
  '/api/open/v1/jobs/sync',
  'POST',
  SHA2('phase2-demo-open-api-request', 256),
  'phase2-demo-idempotency-key',
  'open_stub',
  @p2_job_id,
  200,
  1,
  16,
  JSON_OBJECT('stub', TRUE, 'request_hash', SHA2('phase2-demo-open-api-request', 256)),
  JSON_OBJECT('accepted', TRUE, 'stub', TRUE),
  DATE_SUB(NOW(), INTERVAL 1 DAY),
  DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 1 SECOND
);

INSERT INTO integration_sync_task (
  trace_id,
  api_code,
  open_api_log_id,
  source_system,
  target_system,
  biz_type,
  biz_id,
  payload_json,
  sync_status,
  retry_count,
  max_retry_count,
  sync_version,
  next_retry_time,
  error_msg
) VALUES (
  'trace-p2-demo-retry',
  'open.applications.sync',
  NULL,
  'stub_partner',
  'localtalent',
  'open.applications.sync',
  @p2_application_id,
  JSON_OBJECT('stub', TRUE, 'reason', 'phase2 demo retry queue'),
  0,
  1,
  4,
  1,
  DATE_ADD(NOW(), INTERVAL 5 MINUTE),
  'phase2 demo retry scheduled'
);

COMMIT;
