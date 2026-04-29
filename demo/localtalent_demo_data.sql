SET NAMES utf8mb4;
SET @demo_password_hash := '$2b$12$5G4kZ1x4dOar/VtPyL38UuxDYUreMg03NyJG11tnsQvlMZTSZhP2.';

START TRANSACTION;

DELETE FROM field_access_log WHERE trace_id LIKE 'trace-demo%';
DELETE FROM audit_log WHERE trace_id LIKE 'trace-demo%';
DELETE FROM open_api_log WHERE trace_id LIKE 'trace-demo%';
DELETE FROM integration_sync_task WHERE trace_id LIKE 'trace-demo%';

DELETE ea
FROM export_apply ea
JOIN company c ON c.id = ea.company_id
WHERE c.license_no LIKE 'LT-DEMO-%';

DELETE FROM cms_content
WHERE title = 'LocalTalent 一期演示公告';

DELETE FROM activity_event
WHERE title = 'LocalTalent 一期演示招聘会';

DELETE s
FROM interview_signin s
JOIN candidate_user cu ON cu.id = s.candidate_id
WHERE cu.email IN (
  'demo_candidate@localtalent.test',
  'demo_revoked@localtalent.test',
  'demo_pending@localtalent.test'
);

DELETE s
FROM interview_signin s
JOIN interview_session sess ON sess.id = s.session_id
JOIN job_post j ON j.id = sess.job_id
JOIN company c ON c.id = j.company_id
WHERE c.license_no LIKE 'LT-DEMO-%';

DELETE sess
FROM interview_session sess
JOIN job_post j ON j.id = sess.job_id
JOIN company c ON c.id = j.company_id
WHERE c.license_no LIKE 'LT-DEMO-%';

DELETE a
FROM job_application a
JOIN candidate_user cu ON cu.id = a.candidate_id
WHERE cu.email IN (
  'demo_candidate@localtalent.test',
  'demo_revoked@localtalent.test',
  'demo_pending@localtalent.test'
);

DELETE a
FROM job_application a
JOIN job_post j ON j.id = a.job_id
JOIN company c ON c.id = j.company_id
WHERE c.license_no LIKE 'LT-DEMO-%';

DELETE e
FROM candidate_consent_evidence e
JOIN candidate_consent cc ON cc.id = e.consent_id
JOIN candidate_user cu ON cu.id = cc.candidate_id
WHERE cu.email IN (
  'demo_candidate@localtalent.test',
  'demo_revoked@localtalent.test',
  'demo_pending@localtalent.test'
);

DELETE ps
FROM candidate_publish_snapshot ps
JOIN candidate_user cu ON cu.id = ps.candidate_id
WHERE cu.email IN (
  'demo_candidate@localtalent.test',
  'demo_revoked@localtalent.test',
  'demo_pending@localtalent.test'
);

DELETE cp
FROM candidate_control_profile cp
JOIN candidate_user cu ON cu.id = cp.candidate_id
WHERE cu.email IN (
  'demo_candidate@localtalent.test',
  'demo_revoked@localtalent.test',
  'demo_pending@localtalent.test'
);

DELETE cc
FROM candidate_consent cc
JOIN candidate_user cu ON cu.id = cc.candidate_id
WHERE cu.email IN (
  'demo_candidate@localtalent.test',
  'demo_revoked@localtalent.test',
  'demo_pending@localtalent.test'
);

DELETE cr
FROM candidate_resume cr
JOIN candidate_user cu ON cu.id = cr.candidate_id
WHERE cu.email IN (
  'demo_candidate@localtalent.test',
  'demo_revoked@localtalent.test',
  'demo_pending@localtalent.test'
);

INSERT INTO company (
  company_name,
  license_no,
  industry_code,
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
  'LocalTalent 示例科技有限公司',
  'LT-DEMO-COMPANY-APPROVED',
  'software',
  '100-499',
  '310000',
  '上海市浦东新区演示路 88 号',
  '用于一期本地演示的已认证企业，可展示职位、投递池与导出审批链路。',
  2,
  NULL,
  1,
  DATE_SUB(NOW(), INTERVAL 8 DAY),
  DATE_SUB(NOW(), INTERVAL 9 DAY),
  'portal'
), (
  'LocalTalent 待审核企业',
  'LT-DEMO-COMPANY-PENDING',
  'digital_service',
  '20-99',
  '310000',
  '上海市静安区演示路 18 号',
  '用于运营后台企业审核队列演示。',
  1,
  NULL,
  NULL,
  NULL,
  DATE_SUB(NOW(), INTERVAL 1 DAY),
  'portal'
), (
  'LocalTalent 驳回企业',
  'LT-DEMO-COMPANY-REJECTED',
  'manufacturing',
  '20-99',
  '330100',
  '杭州市演示路 66 号',
  '用于企业驳回状态展示。',
  3,
  '演示驳回原因：营业执照资料不完整。',
  1,
  DATE_SUB(NOW(), INTERVAL 2 DAY),
  DATE_SUB(NOW(), INTERVAL 3 DAY),
  'portal'
) AS new
ON DUPLICATE KEY UPDATE
  company_name = new.company_name,
  industry_code = new.industry_code,
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

SET @company_id := (
  SELECT id FROM company WHERE license_no = 'LT-DEMO-COMPANY-APPROVED' LIMIT 1
);
SET @pending_company_id := (
  SELECT id FROM company WHERE license_no = 'LT-DEMO-COMPANY-PENDING' LIMIT 1
);
SET @rejected_company_id := (
  SELECT id FROM company WHERE license_no = 'LT-DEMO-COMPANY-REJECTED' LIMIT 1
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
  @company_id,
  '演示企业管理员',
  'company_admin',
  NULL,
  'demo_company@localtalent.test',
  @demo_password_hash,
  1
) AS new
ON DUPLICATE KEY UPDATE
  user_name = new.user_name,
  role_code = new.role_code,
  password_hash = new.password_hash,
  status = new.status,
  updated_at = CURRENT_TIMESTAMP;

SET @company_user_id := (
  SELECT id FROM company_user
  WHERE company_id = @company_id AND email = 'demo_company@localtalent.test'
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
  'demo_candidate@localtalent.test',
  @demo_password_hash,
  '林嘉禾',
  1,
  'portal',
  1
), (
  NULL,
  'demo_revoked@localtalent.test',
  @demo_password_hash,
  '周青',
  1,
  'portal',
  1
), (
  NULL,
  'demo_pending@localtalent.test',
  @demo_password_hash,
  '陈澈',
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

SET @candidate_consented_id := (
  SELECT id FROM candidate_user WHERE email = 'demo_candidate@localtalent.test' LIMIT 1
);
SET @candidate_revoked_id := (
  SELECT id FROM candidate_user WHERE email = 'demo_revoked@localtalent.test' LIMIT 1
);
SET @candidate_pending_id := (
  SELECT id FROM candidate_user WHERE email = 'demo_pending@localtalent.test' LIMIT 1
);

INSERT INTO admin_user (
  username,
  display_name,
  mobile,
  email,
  password_hash,
  role_code,
  status
) VALUES (
  'auditor_demo',
  '本地演示审计员',
  NULL,
  'auditor_demo@localtalent.test',
  @demo_password_hash,
  'auditor',
  1
) AS new
ON DUPLICATE KEY UPDATE
  display_name = new.display_name,
  email = new.email,
  password_hash = new.password_hash,
  role_code = new.role_code,
  status = new.status,
  updated_at = CURRENT_TIMESTAMP;

SET @operator_id := (
  SELECT id FROM admin_user WHERE username = 'operator' LIMIT 1
);
SET @auditor_id := (
  SELECT id FROM admin_user WHERE username = 'auditor_demo' LIMIT 1
);

INSERT INTO sys_user_role (user_type, user_id, role_id)
SELECT 'candidate', v.user_id, r.id
FROM sys_role r
JOIN (
  SELECT @candidate_consented_id AS user_id
  UNION ALL SELECT @candidate_revoked_id
  UNION ALL SELECT @candidate_pending_id
) v
WHERE r.role_code = 'candidate'
ON DUPLICATE KEY UPDATE sys_user_role.id = sys_user_role.id;

INSERT INTO sys_user_role (user_type, user_id, role_id)
SELECT 'company_user', @company_user_id, r.id
FROM sys_role r
WHERE r.role_code = 'company_admin'
ON DUPLICATE KEY UPDATE sys_user_role.id = sys_user_role.id;

INSERT INTO sys_user_role (user_type, user_id, role_id)
SELECT 'admin_user', v.user_id, r.id
FROM sys_role r
JOIN (
  SELECT @operator_id AS user_id, 'operator' AS role_code
  UNION ALL SELECT @auditor_id, 'auditor'
) v ON v.role_code = r.role_code
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
  @company_id,
  'LT-DEMO-JOB-ONLINE',
  1,
  'Java 后端工程师',
  'software',
  '310000',
  18000,
  32000,
  '负责地方人才服务平台后端能力建设，重点关注权限、审计与稳定性。',
  2,
  2,
  '演示审核通过',
  NULL,
  @operator_id,
  DATE_SUB(NOW(), INTERVAL 6 DAY),
  DATE_SUB(NOW(), INTERVAL 6 DAY),
  NULL,
  DATE_SUB(NOW(), INTERVAL 6 DAY)
), (
  @company_id,
  'LT-DEMO-JOB-PENDING',
  1,
  '数据产品经理',
  'product',
  '310000',
  16000,
  26000,
  '负责人才服务产品的数据看板和流程优化，当前用于职位待审核队列演示。',
  1,
  1,
  NULL,
  NULL,
  NULL,
  NULL,
  NULL,
  NULL,
  DATE_SUB(NOW(), INTERVAL 1 DAY)
), (
  @company_id,
  'LT-DEMO-JOB-OFFLINE',
  1,
  '前端工程师',
  'frontend',
  '310000',
  15000,
  28000,
  '用于企业后台职位下线状态演示，不在门户公开展示。',
  3,
  2,
  '演示审核通过',
  NULL,
  @operator_id,
  DATE_SUB(NOW(), INTERVAL 10 DAY),
  NULL,
  '演示下线',
  DATE_SUB(NOW(), INTERVAL 1 DAY)
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

SET @job_online_id := (
  SELECT id FROM job_post WHERE source_type = 1 AND external_job_id = 'LT-DEMO-JOB-ONLINE' LIMIT 1
);
SET @job_pending_id := (
  SELECT id FROM job_post WHERE source_type = 1 AND external_job_id = 'LT-DEMO-JOB-PENDING' LIMIT 1
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
  @candidate_consented_id,
  '演示简历-已同意',
  JSON_OBJECT('city_code', '310000', 'category_code', 'software', 'experience_years', 5),
  JSON_ARRAY(JSON_OBJECT('school', 'LocalTalent University', 'degree', 'Bachelor')),
  JSON_ARRAY(JSON_OBJECT('company', 'Demo Studio', 'title', 'Backend Engineer')),
  JSON_OBJECT('summary', 'Java / Spring Boot / MySQL / Redis / Audit', 'skills', JSON_ARRAY('Java', 'Spring Boot', 'MySQL', 'Redis', 'Audit')),
  'demo/private/resume-consented.pdf',
  1
), (
  @candidate_revoked_id,
  '演示简历-已撤回',
  JSON_OBJECT('city_code', '310000', 'category_code', 'product', 'experience_years', 4),
  JSON_ARRAY(JSON_OBJECT('school', 'LocalTalent University', 'degree', 'Master')),
  JSON_ARRAY(JSON_OBJECT('company', 'Demo Product Lab', 'title', 'Product Manager')),
  JSON_OBJECT('summary', 'B 端产品 / 数据分析 / 流程设计', 'skills', JSON_ARRAY('Product', 'Data Analysis', 'Workflow')),
  'demo/private/resume-revoked.pdf',
  1
), (
  @candidate_pending_id,
  '演示简历-不可发布',
  JSON_OBJECT('city_code', '330100', 'category_code', 'operation', 'experience_years', 2),
  JSON_ARRAY(),
  JSON_ARRAY(JSON_OBJECT('company', 'Demo Operation Team', 'title', 'Operation Assistant')),
  JSON_OBJECT('summary', '运营支持 / 活动执行', 'skills', JSON_ARRAY('Operation', 'Event')),
  NULL,
  1
);

SET @resume_consented_id := (
  SELECT id FROM candidate_resume
  WHERE candidate_id = @candidate_consented_id AND resume_name = '演示简历-已同意'
  ORDER BY id DESC LIMIT 1
);
SET @resume_revoked_id := (
  SELECT id FROM candidate_resume
  WHERE candidate_id = @candidate_revoked_id AND resume_name = '演示简历-已撤回'
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
  @candidate_consented_id,
  1,
  JSON_ARRAY('talent_service_area'),
  'phase1-v1',
  'portal',
  DATE_SUB(NOW(), INTERVAL 5 DAY),
  1,
  1,
  0,
  NULL
), (
  @candidate_revoked_id,
  2,
  JSON_ARRAY('talent_service_area'),
  'phase1-v1',
  'portal',
  DATE_SUB(NOW(), INTERVAL 9 DAY),
  1,
  1,
  1,
  DATE_SUB(NOW(), INTERVAL 2 DAY)
);

SET @consent_consented_id := (
  SELECT id FROM candidate_consent
  WHERE candidate_id = @candidate_consented_id
  ORDER BY id DESC LIMIT 1
);
SET @consent_revoked_id := (
  SELECT id FROM candidate_consent
  WHERE candidate_id = @candidate_revoked_id
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
  @candidate_consented_id,
  1,
  'consent',
  1,
  1,
  'phase1-v1',
  4,
  JSON_OBJECT(
    'display_name_masked', '林**',
    'city_code', '310000',
    'category_code', 'software',
    'skills_summary', 'Java / Spring Boot / MySQL / Redis / Audit',
    'experience_years', 5,
    'updated_at', DATE_FORMAT(NOW(), '%Y-%m-%dT%H:%i:%s')
  ),
  1,
  1,
  DATE_SUB(NOW(), INTERVAL 5 DAY),
  NOW()
), (
  @candidate_revoked_id,
  1,
  'consent',
  2,
  0,
  'phase1-v1',
  4,
  JSON_OBJECT(
    'display_name_masked', '周**',
    'city_code', '310000',
    'category_code', 'product',
    'skills_summary', 'B 端产品 / 数据分析 / 流程设计',
    'experience_years', 4,
    'updated_at', DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 2 DAY), '%Y-%m-%dT%H:%i:%s')
  ),
  1,
  0,
  DATE_SUB(NOW(), INTERVAL 9 DAY),
  DATE_SUB(NOW(), INTERVAL 2 DAY)
);

SET @snapshot_consented_id := (
  SELECT id FROM candidate_publish_snapshot
  WHERE candidate_id = @candidate_consented_id
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
  @candidate_consented_id,
  1,
  'consent',
  1,
  1,
  4,
  'phase1-v1',
  @consent_consented_id,
  @snapshot_consented_id,
  1
), (
  @candidate_revoked_id,
  1,
  'consent',
  2,
  0,
  4,
  'phase1-v1',
  @consent_revoked_id,
  NULL,
  1
), (
  @candidate_pending_id,
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
  source_type = new.source_type,
  legal_basis = new.legal_basis,
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
  @job_online_id,
  @candidate_consented_id,
  @resume_consented_id,
  1,
  'LT-DEMO-APPLICATION-CONSENTED',
  3,
  DATE_SUB(NOW(), INTERVAL 4 DAY)
), (
  @job_online_id,
  @candidate_revoked_id,
  @resume_revoked_id,
  1,
  'LT-DEMO-APPLICATION-REVOKED',
  0,
  DATE_SUB(NOW(), INTERVAL 3 DAY)
) AS new
ON DUPLICATE KEY UPDATE
  resume_id = new.resume_id,
  application_status = new.application_status,
  apply_time = new.apply_time,
  updated_at = CURRENT_TIMESTAMP;

SET @application_consented_id := (
  SELECT id FROM job_application
  WHERE external_application_id = 'LT-DEMO-APPLICATION-CONSENTED'
  LIMIT 1
);
SET @application_revoked_id := (
  SELECT id FROM job_application
  WHERE external_application_id = 'LT-DEMO-APPLICATION-REVOKED'
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
  @application_consented_id,
  @job_online_id,
  @company_id,
  '一期演示面试场次',
  DATE_ADD(NOW(), INTERVAL 2 DAY),
  '上海市浦东新区演示会议室 A',
  NULL,
  'demo-signin-code-hash-consented',
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

SET @session_consented_id := (
  SELECT id FROM interview_session
  WHERE application_id = @application_consented_id
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
  @session_consented_id,
  @candidate_consented_id,
  'qrcode',
  DATE_SUB(NOW(), INTERVAL 1 DAY),
  'demo-device',
  0
) AS new
ON DUPLICATE KEY UPDATE
  sign_channel = new.sign_channel,
  sign_time = new.sign_time,
  device_id = new.device_id,
  consent_redirect_flag = new.consent_redirect_flag;

INSERT INTO export_apply (
  company_id,
  apply_user_id,
  apply_identity_type,
  apply_role_code,
  biz_type,
  scope_json,
  reason,
  approve_status,
  generate_status,
  download_count,
  created_at,
  updated_at
) VALUES (
  @company_id,
  @company_user_id,
  'company',
  'company_admin',
  'application_candidate_detail',
  JSON_OBJECT('job_id', @job_online_id),
  '演示导出申请：本企业投递池候选人明细，等待运营审批。',
  0,
  0,
  0,
  NOW(),
  NOW()
);

SET @export_demo_id := LAST_INSERT_ID();

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
  'notice',
  'LocalTalent 一期演示公告',
  NULL,
  '本公告用于运营后台 CMS 内容管理演示。',
  '<p>LocalTalent 一期演示环境已准备好，可验证门户、求职者中心、企业后台和运营后台。</p>',
  '310000',
  1,
  NOW()
) AS new
ON DUPLICATE KEY UPDATE
  summary = new.summary,
  body_html = new.body_html,
  city_code = new.city_code,
  status = new.status,
  publish_time = new.publish_time,
  updated_at = CURRENT_TIMESTAMP;

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
  'LocalTalent 一期演示招聘会',
  'job_fair',
  '310000',
  DATE_ADD(NOW(), INTERVAL 7 DAY),
  DATE_ADD(DATE_ADD(NOW(), INTERVAL 7 DAY), INTERVAL 3 HOUR),
  '上海市人才服务中心演示厅',
  @company_id,
  1
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
  @candidate_consented_id,
  'candidate',
  'candidate_consent',
  @consent_consented_id,
  'consent_create',
  NULL,
  JSON_OBJECT('status', 'consented', 'demo', TRUE),
  'trace-demo',
  DATE_SUB(NOW(), INTERVAL 5 DAY)
), (
  @candidate_revoked_id,
  'candidate',
  'candidate_consent',
  @consent_revoked_id,
  'consent_revoke',
  JSON_OBJECT('status', 'consented', 'demo', TRUE),
  JSON_OBJECT('status', 'revoked', 'demo', TRUE),
  'trace-demo',
  DATE_SUB(NOW(), INTERVAL 2 DAY)
), (
  @operator_id,
  'operator',
  'job_post',
  @job_online_id,
  'job_review_pass',
  JSON_OBJECT('audit_status', 1),
  JSON_OBJECT('audit_status', 2, 'memo', '演示审核通过'),
  'trace-demo',
  DATE_SUB(NOW(), INTERVAL 6 DAY)
), (
  @company_user_id,
  'company_admin',
  'export_apply',
  @export_demo_id,
  'export_apply',
  NULL,
  JSON_OBJECT('approve_status', 0, 'demo', TRUE),
  'trace-demo',
  NOW()
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
  @candidate_consented_id,
  'candidate',
  'candidate_publish_snapshot',
  @snapshot_consented_id,
  'display_name',
  'MASK',
  'trace-demo',
  DATE_SUB(NOW(), INTERVAL 5 DAY)
), (
  @company_user_id,
  'company_admin',
  'export_application',
  @export_demo_id,
  'contact',
  'MASK',
  'trace-demo',
  NOW()
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
  'trace-demo',
  '/api/open/v1/jobs/sync',
  'POST',
  SHA2('demo-open-api-request', 256),
  'demo-open-idempotency-key',
  'open_stub',
  @job_online_id,
  200,
  1,
  18,
  JSON_OBJECT('stub', TRUE, 'request_hash', SHA2('demo-open-api-request', 256), 'has_body', TRUE),
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
  'trace-demo-retry',
  'open.applications.sync',
  NULL,
  'stub_partner',
  'localtalent',
  'open.applications.sync',
  @application_consented_id,
  JSON_OBJECT('stub', TRUE, 'reason', 'demo retry queue'),
  0,
  1,
  4,
  1,
  DATE_ADD(NOW(), INTERVAL 5 MINUTE),
  'demo retry scheduled'
);

COMMIT;
