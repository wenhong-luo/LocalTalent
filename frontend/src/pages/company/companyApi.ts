import { apiGet, apiPost, apiRequest, type ApiResult, createIdempotencyKey, createTraceId } from '@/lib/httpClient';

type RawRecord = Record<string, unknown>;

export type CompanyStatus = {
  company_id: number;
  auth_status: number;
  reject_reason: string;
};

export type CompanyApplyPayload = {
  company_name: string;
  license_no: string;
  industry_code?: string;
  scale_code?: string;
  city_code?: string;
  address?: string;
  company_profile?: string;
};

export type CompanyJob = {
  job_id: number;
  title: string;
  status: number;
  audit_status: number;
  reject_reason: string;
  updated_at: string;
};

export type CompanyJobPage = {
  job_list: CompanyJob[];
  total: number;
};

export type CompanyApplication = {
  application_id: number;
  job_title: string;
  company_name: string;
  status: number;
  apply_time: string;
};

export type CompanyApplicationPage = {
  application_list: CompanyApplication[];
  total: number;
};

export type CompanyExportApply = {
  export_id: number;
  biz_type: string;
  approve_status: number;
  generate_status: number;
  reason: string;
  reject_reason: string;
  expire_time: string;
  download_count: number;
  created_at: string;
};

export type CompanyExportApplyPayload = {
  reason: string;
  scope?: {
    job_id?: number;
    status?: number;
  };
};

export type CompanyWorkbenchFeature = {
  company_workbench_enabled: boolean;
  company_style_upload_enabled: boolean;
  company_logo_upload_enabled: boolean;
};

export type CompanyWorkbenchProfile = {
  company_id: number;
  company_name: string;
  industry_code: string;
  nature_code: string;
  scale_code: string;
  city_code: string;
  address: string;
  company_profile: string;
  registered_capital_amount: string;
  registered_capital_unit: string;
  website_url: string;
  benefit_codes: string[];
  contact_name: string;
  contact_mobile: string;
  contact_mobile_hidden: boolean;
  contact_wechat: string;
  contact_wechat_same_mobile: boolean;
  contact_phone: string;
  contact_email: string;
  contact_qq: string;
  auth_status: number;
  reject_reason: string;
  certification_material_summary: Record<string, unknown>;
  updated_at: string;
};

export type CompanyWorkbenchStats = {
  job_total: number;
  application_total: number;
  pending_application_total: number;
  interview_total: number;
  export_total: number;
};

export type CompanyWorkbenchOverview = {
  profile: CompanyWorkbenchProfile;
  stats: CompanyWorkbenchStats;
  features: CompanyWorkbenchFeature;
};

export type CompanyStyleImage = {
  image_id: number;
  file_name: string;
  content_type: string;
  size_bytes: number;
  display_order: number;
  status: number;
  review_status: number;
  uploaded_at: string;
  content_url: string;
};

export type CompanyStyleImagePage = {
  image_list: CompanyStyleImage[];
  total: number;
};

export type CompanyLogo = {
  has_logo: boolean;
  logo_status: string;
  file_name: string;
  content_type: string;
  size_bytes: number | null;
  uploaded_at: string;
  content_url: string;
};

export type CompanyWorkbenchApplication = {
  application_id: number;
  job_id: number;
  job_title: string;
  application_status: number;
  status_label: string;
  apply_time: string;
  display_name_masked: string;
  city_code: string;
  skills_summary: string;
  experience_years: number | null;
  has_resume_attachment: boolean;
  company_stage_note: string;
  stage_changed_at: string;
};

export type CompanyWorkbenchApplicationPage = {
  application_list: CompanyWorkbenchApplication[];
  total: number;
};

export type CompanyInterviewSession = {
  session_id: number;
  application_id: number;
  job_id: number;
  job_title: string;
  status: number;
  session_name: string;
  session_time: string;
  location: string;
};

export type CompanyInterviewSessionPage = {
  session_list: CompanyInterviewSession[];
  total: number;
};

export type WorkbenchProfilePayload = {
  company_name: string;
  industry_code?: string;
  nature_code?: string;
  scale_code?: string;
  city_code?: string;
  address?: string;
  company_profile?: string;
  registered_capital_amount?: string;
  registered_capital_unit?: string;
  website_url?: string;
  benefit_codes?: string[];
  contact_name?: string;
  contact_mobile?: string;
  contact_mobile_hidden?: boolean;
  contact_wechat?: string;
  contact_wechat_same_mobile?: boolean;
  contact_phone?: string;
  contact_email?: string;
  contact_qq?: string;
};

export type WorkbenchCertificationPayload = WorkbenchProfilePayload & {
  license_no: string;
  certification_material_summary?: Record<string, unknown>;
};

export type WorkbenchJobPayload = {
  title: string;
  category_code?: string;
  city_code?: string;
  salary_min?: number;
  salary_max?: number;
  job_desc: string;
};

function asRecord(value: unknown): RawRecord {
  return value && typeof value === 'object' ? value as RawRecord : {};
}

function text(value: unknown, fallback = ''): string {
  return typeof value === 'string' ? value : fallback;
}

function textArray(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : [];
}

function numberOr(value: unknown, fallback = 0): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback;
}

function toCompanyStatus(raw: unknown): CompanyStatus {
  const row = asRecord(raw);
  return {
    company_id: numberOr(row.company_id),
    auth_status: numberOr(row.auth_status, 1),
    reject_reason: text(row.reject_reason)
  };
}

function toJob(raw: unknown): CompanyJob {
  const row = asRecord(raw);
  return {
    job_id: numberOr(row.job_id),
    title: text(row.title, '未命名职位'),
    status: numberOr(row.status, 1),
    audit_status: numberOr(row.audit_status, 1),
    reject_reason: text(row.reject_reason),
    updated_at: text(row.updated_at)
  };
}

function toApplication(raw: unknown): CompanyApplication {
  const row = asRecord(raw);
  return {
    application_id: numberOr(row.application_id),
    job_title: text(row.job_title, '未知职位'),
    company_name: text(row.company_name, '本企业'),
    status: numberOr(row.status),
    apply_time: text(row.apply_time)
  };
}

function toExportApply(raw: unknown): CompanyExportApply {
  const row = asRecord(raw);
  return {
    export_id: numberOr(row.export_id),
    biz_type: text(row.biz_type, 'application_candidate_detail'),
    approve_status: numberOr(row.approve_status),
    generate_status: numberOr(row.generate_status),
    reason: text(row.reason),
    reject_reason: text(row.reject_reason),
    expire_time: text(row.expire_time),
    download_count: numberOr(row.download_count),
    created_at: text(row.created_at)
  };
}

function toWorkbenchProfile(raw: unknown): CompanyWorkbenchProfile {
  const row = asRecord(raw);
  return {
    company_id: numberOr(row.company_id),
    company_name: text(row.company_name, '未命名企业'),
    industry_code: text(row.industry_code),
    nature_code: text(row.nature_code),
    scale_code: text(row.scale_code),
    city_code: text(row.city_code),
    address: text(row.address),
    company_profile: text(row.company_profile),
    registered_capital_amount: text(row.registered_capital_amount),
    registered_capital_unit: text(row.registered_capital_unit, 'cny_10k'),
    website_url: text(row.website_url),
    benefit_codes: textArray(row.benefit_codes),
    contact_name: text(row.contact_name),
    contact_mobile: text(row.contact_mobile),
    contact_mobile_hidden: row.contact_mobile_hidden !== false,
    contact_wechat: text(row.contact_wechat),
    contact_wechat_same_mobile: row.contact_wechat_same_mobile === true,
    contact_phone: text(row.contact_phone),
    contact_email: text(row.contact_email),
    contact_qq: text(row.contact_qq),
    auth_status: numberOr(row.auth_status, 1),
    reject_reason: text(row.reject_reason),
    certification_material_summary: asRecord(row.certification_material_summary),
    updated_at: text(row.updated_at)
  };
}

function toWorkbenchStats(raw: unknown): CompanyWorkbenchStats {
  const row = asRecord(raw);
  return {
    job_total: numberOr(row.job_total),
    application_total: numberOr(row.application_total),
    pending_application_total: numberOr(row.pending_application_total),
    interview_total: numberOr(row.interview_total),
    export_total: numberOr(row.export_total)
  };
}

function toWorkbenchApplication(raw: unknown): CompanyWorkbenchApplication {
  const row = asRecord(raw);
  return {
    application_id: numberOr(row.application_id),
    job_id: numberOr(row.job_id),
    job_title: text(row.job_title, '未知职位'),
    application_status: numberOr(row.application_status),
    status_label: text(row.status_label, '已投递'),
    apply_time: text(row.apply_time),
    display_name_masked: text(row.display_name_masked, '求职者*'),
    city_code: text(row.city_code),
    skills_summary: text(row.skills_summary),
    experience_years: typeof row.experience_years === 'number' ? row.experience_years : null,
    has_resume_attachment: row.has_resume_attachment === true,
    company_stage_note: text(row.company_stage_note),
    stage_changed_at: text(row.stage_changed_at)
  };
}

function toInterviewSession(raw: unknown): CompanyInterviewSession {
  const row = asRecord(raw);
  return {
    session_id: numberOr(row.session_id),
    application_id: numberOr(row.application_id),
    job_id: numberOr(row.job_id),
    job_title: text(row.job_title, '未知职位'),
    status: numberOr(row.status, 1),
    session_name: text(row.session_name, '面试邀约'),
    session_time: text(row.session_time),
    location: text(row.location)
  };
}

function toStyleImage(raw: unknown): CompanyStyleImage {
  const row = asRecord(raw);
  return {
    image_id: numberOr(row.image_id),
    file_name: text(row.file_name, '企业风采图片'),
    content_type: text(row.content_type, 'image/jpeg'),
    size_bytes: numberOr(row.size_bytes),
    display_order: numberOr(row.display_order),
    status: numberOr(row.status, 1),
    review_status: numberOr(row.review_status),
    uploaded_at: text(row.uploaded_at),
    content_url: text(row.content_url)
  };
}

function toCompanyLogo(raw: unknown): CompanyLogo {
  const row = asRecord(raw);
  return {
    has_logo: row.has_logo === true,
    logo_status: text(row.logo_status, 'empty'),
    file_name: text(row.file_name),
    content_type: text(row.content_type, 'image/png'),
    size_bytes: typeof row.size_bytes === 'number' ? row.size_bytes : null,
    uploaded_at: text(row.uploaded_at),
    content_url: text(row.content_url, '/api/company/workbench/logo/content')
  };
}

export async function submitCompanyApply(
  token: string,
  payload: CompanyApplyPayload
): Promise<ApiResult<CompanyStatus>> {
  const result = await apiPost<unknown>('/api/company/apply', payload, { token });
  return { data: toCompanyStatus(result.data), traceId: result.traceId };
}

export async function fetchCompanyJobs(token: string): Promise<ApiResult<CompanyJobPage>> {
  const result = await apiGet<unknown>('/api/company/jobs?page=1&size=20', { token });
  const payload = asRecord(result.data);
  const rows = Array.isArray(payload.job_list) ? payload.job_list : [];
  return {
    data: {
      job_list: rows.map(toJob),
      total: numberOr(payload.total, rows.length)
    },
    traceId: result.traceId
  };
}

export async function fetchCompanyApplications(token: string): Promise<ApiResult<CompanyApplicationPage>> {
  const result = await apiGet<unknown>('/api/company/applications?page=1&size=20', { token });
  const payload = asRecord(result.data);
  const rows = Array.isArray(payload.application_list) ? payload.application_list : [];
  return {
    data: {
      application_list: rows.map(toApplication),
      total: numberOr(payload.total, rows.length)
    },
    traceId: result.traceId
  };
}

export async function applyCompanyExport(
  token: string,
  payload: CompanyExportApplyPayload
): Promise<ApiResult<CompanyExportApply>> {
  const result = await apiPost<unknown>(
    '/api/company/exports',
    {
      biz_type: 'application_candidate_detail',
      scope: payload.scope ?? {},
      reason: payload.reason
    },
    { token }
  );
  return { data: toExportApply(result.data), traceId: result.traceId };
}

export async function fetchCompanyExportDetail(token: string, exportId: number): Promise<ApiResult<CompanyExportApply>> {
  const result = await apiGet<unknown>(`/api/company/exports/${exportId}`, { token });
  return { data: toExportApply(result.data), traceId: result.traceId };
}

export async function issueCompanyExportDownloadUrl(token: string, exportId: number): Promise<ApiResult<string>> {
  const result = await apiRequest<unknown>(`/api/company/exports/${exportId}/download-url`, {
    method: 'GET',
    token
  });
  return { data: text(asRecord(result.data).download_url), traceId: result.traceId };
}

export async function fetchCompanyWorkbenchOverview(token: string): Promise<ApiResult<CompanyWorkbenchOverview>> {
  const result = await apiGet<unknown>('/api/company/workbench/overview', { token });
  const payload = asRecord(result.data);
  return {
    data: {
      profile: toWorkbenchProfile(payload.profile),
      stats: toWorkbenchStats(payload.stats),
      features: {
        company_workbench_enabled: asRecord(payload.features).company_workbench_enabled === true,
        company_style_upload_enabled: asRecord(payload.features).company_style_upload_enabled === true,
        company_logo_upload_enabled: asRecord(payload.features).company_logo_upload_enabled === true
      }
    },
    traceId: result.traceId
  };
}

export async function fetchCompanyLogo(token: string): Promise<ApiResult<CompanyLogo>> {
  const result = await apiGet<unknown>('/api/company/workbench/logo', { token });
  return { data: toCompanyLogo(result.data), traceId: result.traceId };
}

export async function uploadCompanyLogo(token: string, file: File): Promise<ApiResult<CompanyLogo>> {
  const formData = new FormData();
  formData.append('file', file);
  const result = await apiRequest<unknown>('/api/company/workbench/logo', {
    method: 'POST',
    token,
    idempotencyKey: createIdempotencyKey('company-logo'),
    body: formData
  });
  return { data: toCompanyLogo(result.data), traceId: result.traceId };
}

export async function fetchCompanyLogoBlob(token: string, logo: CompanyLogo): Promise<Blob> {
  const traceId = createTraceId();
  const response = await fetch(logo.content_url, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
      'X-Trace-Id': traceId,
      Accept: logo.content_type
    },
    cache: 'no-store'
  });
  if (!response.ok) {
    throw new Error('企业 Logo 读取失败');
  }
  return response.blob();
}

export async function deleteCompanyLogo(token: string): Promise<ApiResult<CompanyLogo>> {
  const result = await apiRequest<unknown>('/api/company/workbench/logo', {
    method: 'DELETE',
    token,
    idempotencyKey: createIdempotencyKey('company-logo-delete')
  });
  return { data: toCompanyLogo(result.data), traceId: result.traceId };
}

export async function fetchCompanyStyleImages(token: string): Promise<ApiResult<CompanyStyleImagePage>> {
  const result = await apiGet<unknown>('/api/company/workbench/style-images', { token });
  const payload = asRecord(result.data);
  const rows = Array.isArray(payload.image_list) ? payload.image_list : [];
  return {
    data: {
      image_list: rows.map(toStyleImage),
      total: numberOr(payload.total, rows.length)
    },
    traceId: result.traceId
  };
}

export async function uploadCompanyStyleImage(token: string, file: File): Promise<ApiResult<CompanyStyleImage>> {
  const formData = new FormData();
  formData.append('file', file);
  const result = await apiRequest<unknown>('/api/company/workbench/style-images', {
    method: 'POST',
    token,
    idempotencyKey: createIdempotencyKey('company-style-image'),
    body: formData
  });
  return { data: toStyleImage(result.data), traceId: result.traceId };
}

export async function fetchCompanyStyleImageBlob(token: string, image: CompanyStyleImage): Promise<Blob> {
  const traceId = createTraceId();
  const response = await fetch(image.content_url, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
      'X-Trace-Id': traceId,
      Accept: image.content_type
    },
    cache: 'no-store'
  });
  if (!response.ok) {
    throw new Error('企业风采图片读取失败');
  }
  return response.blob();
}

export async function saveCompanyStyleImageOrder(token: string, imageIds: number[]): Promise<ApiResult<CompanyStyleImagePage>> {
  const result = await apiRequest<unknown>('/api/company/workbench/style-images/order', {
    method: 'PUT',
    token,
    idempotencyKey: createIdempotencyKey('company-style-image-order'),
    body: { image_ids: imageIds }
  });
  const payload = asRecord(result.data);
  const rows = Array.isArray(payload.image_list) ? payload.image_list : [];
  return {
    data: { image_list: rows.map(toStyleImage), total: numberOr(payload.total, rows.length) },
    traceId: result.traceId
  };
}

export async function deleteCompanyStyleImage(token: string, imageId: number): Promise<ApiResult<CompanyStyleImagePage>> {
  const result = await apiRequest<unknown>(`/api/company/workbench/style-images/${imageId}`, {
    method: 'DELETE',
    token,
    idempotencyKey: createIdempotencyKey('company-style-image-delete')
  });
  const payload = asRecord(result.data);
  const rows = Array.isArray(payload.image_list) ? payload.image_list : [];
  return {
    data: { image_list: rows.map(toStyleImage), total: numberOr(payload.total, rows.length) },
    traceId: result.traceId
  };
}

export async function saveCompanyWorkbenchProfile(
  token: string,
  payload: WorkbenchProfilePayload
): Promise<ApiResult<CompanyWorkbenchProfile>> {
  const result = await apiRequest<unknown>('/api/company/workbench/profile', {
    method: 'PUT',
    token,
    idempotencyKey: createIdempotencyKey('company-profile'),
    body: payload
  });
  return { data: toWorkbenchProfile(result.data), traceId: result.traceId };
}

export async function submitCompanyWorkbenchCertification(
  token: string,
  payload: WorkbenchCertificationPayload
): Promise<ApiResult<CompanyWorkbenchProfile>> {
  const result = await apiPost<unknown>('/api/company/workbench/certification', payload, {
    token,
    idempotencyKey: createIdempotencyKey('company-certification')
  });
  return { data: toWorkbenchProfile(result.data), traceId: result.traceId };
}

export async function createCompanyWorkbenchJob(
  token: string,
  payload: WorkbenchJobPayload
): Promise<ApiResult<CompanyJob>> {
  const result = await apiPost<unknown>('/api/company/workbench/jobs', payload, {
    token,
    idempotencyKey: createIdempotencyKey('company-job')
  });
  return { data: toJob(result.data), traceId: result.traceId };
}

export async function fetchCompanyWorkbenchJobs(token: string): Promise<ApiResult<CompanyJobPage>> {
  const result = await apiGet<unknown>('/api/company/workbench/jobs?page=1&size=20', { token });
  const payload = asRecord(result.data);
  const rows = Array.isArray(payload.job_list) ? payload.job_list : [];
  return {
    data: {
      job_list: rows.map(toJob),
      total: numberOr(payload.total, rows.length)
    },
    traceId: result.traceId
  };
}

export async function submitCompanyWorkbenchJobReview(token: string, jobId: number): Promise<ApiResult<CompanyJob>> {
  const result = await apiPost<unknown>(`/api/company/workbench/jobs/${jobId}/submit-review`, {}, {
    token,
    idempotencyKey: createIdempotencyKey('company-job-review')
  });
  return { data: toJob(result.data), traceId: result.traceId };
}

export async function offlineCompanyWorkbenchJob(token: string, jobId: number, reason: string): Promise<ApiResult<CompanyJob>> {
  const result = await apiPost<unknown>(`/api/company/workbench/jobs/${jobId}/offline`, { reason }, {
    token,
    idempotencyKey: createIdempotencyKey('company-job-offline')
  });
  return { data: toJob(result.data), traceId: result.traceId };
}

export async function fetchCompanyWorkbenchApplications(token: string): Promise<ApiResult<CompanyWorkbenchApplicationPage>> {
  const result = await apiGet<unknown>('/api/company/workbench/applications?page=1&size=20', { token });
  const payload = asRecord(result.data);
  const rows = Array.isArray(payload.application_list) ? payload.application_list : [];
  return {
    data: { application_list: rows.map(toWorkbenchApplication), total: numberOr(payload.total, rows.length) },
    traceId: result.traceId
  };
}

export async function changeCompanyWorkbenchApplicationStage(
  token: string,
  applicationId: number,
  stage: string,
  note: string
): Promise<ApiResult<CompanyWorkbenchApplication>> {
  const result = await apiPost<unknown>(`/api/company/workbench/applications/${applicationId}/stage`, { stage, note }, {
    token,
    idempotencyKey: createIdempotencyKey('company-application-stage')
  });
  return { data: toWorkbenchApplication(result.data), traceId: result.traceId };
}

export async function fetchCompanyWorkbenchInterviewSessions(token: string): Promise<ApiResult<CompanyInterviewSessionPage>> {
  const result = await apiGet<unknown>('/api/company/workbench/interview-sessions?page=1&size=20', { token });
  const payload = asRecord(result.data);
  const rows = Array.isArray(payload.session_list) ? payload.session_list : [];
  return {
    data: { session_list: rows.map(toInterviewSession), total: numberOr(payload.total, rows.length) },
    traceId: result.traceId
  };
}

export async function createCompanyWorkbenchInterviewSession(
  token: string,
  applicationId: number
): Promise<ApiResult<CompanyInterviewSession>> {
  const result = await apiPost<unknown>(
    `/api/company/workbench/applications/${applicationId}/interview-sessions`,
    {
      session_name: '企业工作台面试邀约',
      session_time: '2099-01-01T10:00:00',
      location: '站内面试邀约占位'
    },
    { token, idempotencyKey: createIdempotencyKey('company-interview') }
  );
  return { data: toInterviewSession(result.data), traceId: result.traceId };
}

export async function applyCompanyWorkbenchExport(
  token: string,
  reason: string
): Promise<ApiResult<CompanyExportApply>> {
  const result = await apiPost<unknown>(
    '/api/company/workbench/exports',
    {
      biz_type: 'application_candidate_detail',
      scope: {},
      reason
    },
    { token, idempotencyKey: createIdempotencyKey('company-export') }
  );
  return { data: toExportApply(result.data), traceId: result.traceId };
}
