import { apiGet, apiPost, apiRequest, createIdempotencyKey, createTraceId, type ApiResult } from '@/lib/httpClient';
import { type CompanyExportApply } from '@/pages/company/companyApi';

type RawRecord = Record<string, unknown>;

export type CompanyReviewItem = {
  company_id: number;
  company_name: string;
  license_no: string;
  city_code: string;
  auth_status: number;
  reject_reason: string;
  submitted_at: string;
};

export type JobReviewItem = {
  job_id: number;
  title: string;
  company_id: number;
  status: number;
  audit_status: number;
  reject_reason: string;
  updated_at: string;
};

export type AuditTraceSummary = {
  trace_id: string;
  audit_count: number;
  access_count: number;
  open_api_count: number;
  audit_log_list: AuditLogItem[];
  access_log_list: FieldAccessLogItem[];
  open_api_log_list: OpenApiLogItem[];
};

export type AuditLogItem = {
  id: number;
  biz_type: string;
  biz_id: number;
  action_type: string;
  operator_role: string;
  trace_id: string;
  created_at: string;
};

export type FieldAccessLogItem = {
  id: number;
  biz_type: string;
  biz_id: number;
  field_name: string;
  access_type: string;
  operator_role: string;
  trace_id: string;
  created_at: string;
};

export type OpenApiLogItem = {
  id: number;
  source_system: string;
  client_code: string;
  api_code: string;
  biz_type: string;
  biz_id: number;
  http_status: number;
  success_flag: boolean;
  trace_id: string;
  created_at: string;
};

export type OpsOverview = {
  features: {
    operator_portal_ops_enabled: boolean;
  };
  pending_company_count: number;
  pending_job_count: number;
  pending_export_count: number;
  published_content_count: number;
  published_event_count: number;
  active_recommendation_count: number;
  pending_risk_count: number;
  recent_audit_count: number;
};

export type RecommendationItem = {
  recommendation_id: number;
  slot_code: string;
  target_type: string;
  target_id: number;
  title_override: string;
  summary_override: string;
  display_order: number;
  status: number;
  target_valid: boolean;
  invalid_reason: string;
  updated_at: string;
};

export type HomeSlotItem = {
  slot_id: number;
  slot_code: string;
  title: string;
  subtitle: string;
  image_url: string;
  image_alt: string;
  has_image: boolean;
  image_file_name: string;
  image_content_type: string;
  image_size_bytes: number;
  image_uploaded_at: string;
  image_content_url: string;
  link_type: string;
  link_url: string;
  target_type: string;
  target_id: number;
  display_order: number;
  status: number;
  updated_at: string;
};

export type RiskReviewItem = {
  risk_id: number;
  risk_type: string;
  target_type: string;
  target_id: number;
  severity: string;
  status: number;
  title: string;
  summary: string;
  decision: string;
  handled_at: string;
  updated_at: string;
};

function asRecord(value: unknown): RawRecord {
  return value && typeof value === 'object' ? value as RawRecord : {};
}

function text(value: unknown, fallback = ''): string {
  return typeof value === 'string' ? value : fallback;
}

function numberOr(value: unknown, fallback = 0): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback;
}

function toCompanyReviewItem(raw: unknown): CompanyReviewItem {
  const row = asRecord(raw);
  return {
    company_id: numberOr(row.company_id),
    company_name: text(row.company_name, '未命名企业'),
    license_no: text(row.license_no),
    city_code: text(row.city_code),
    auth_status: numberOr(row.auth_status, 1),
    reject_reason: text(row.reject_reason),
    submitted_at: text(row.submitted_at)
  };
}

function toJobReviewItem(raw: unknown): JobReviewItem {
  const row = asRecord(raw);
  return {
    job_id: numberOr(row.job_id),
    title: text(row.title, '未命名职位'),
    company_id: numberOr(row.company_id),
    status: numberOr(row.status, 1),
    audit_status: numberOr(row.audit_status, 1),
    reject_reason: text(row.reject_reason),
    updated_at: text(row.updated_at)
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

function toOpsOverview(raw: unknown): OpsOverview {
  const row = asRecord(raw);
  const features = asRecord(row.features);
  return {
    features: {
      operator_portal_ops_enabled: Boolean(features.operator_portal_ops_enabled)
    },
    pending_company_count: numberOr(row.pending_company_count),
    pending_job_count: numberOr(row.pending_job_count),
    pending_export_count: numberOr(row.pending_export_count),
    published_content_count: numberOr(row.published_content_count),
    published_event_count: numberOr(row.published_event_count),
    active_recommendation_count: numberOr(row.active_recommendation_count),
    pending_risk_count: numberOr(row.pending_risk_count),
    recent_audit_count: numberOr(row.recent_audit_count)
  };
}

function toRecommendationItem(raw: unknown): RecommendationItem {
  const row = asRecord(raw);
  return {
    recommendation_id: numberOr(row.recommendation_id),
    slot_code: text(row.slot_code),
    target_type: text(row.target_type),
    target_id: numberOr(row.target_id),
    title_override: text(row.title_override),
    summary_override: text(row.summary_override),
    display_order: numberOr(row.display_order),
    status: numberOr(row.status),
    target_valid: Boolean(row.target_valid),
    invalid_reason: text(row.invalid_reason),
    updated_at: text(row.updated_at)
  };
}

function toHomeSlotItem(raw: unknown): HomeSlotItem {
  const row = asRecord(raw);
  return {
    slot_id: numberOr(row.slot_id),
    slot_code: text(row.slot_code),
    title: text(row.title),
    subtitle: text(row.subtitle),
    image_url: text(row.image_url),
    image_alt: text(row.image_alt),
    has_image: Boolean(row.has_image),
    image_file_name: text(row.image_file_name),
    image_content_type: text(row.image_content_type),
    image_size_bytes: numberOr(row.image_size_bytes),
    image_uploaded_at: text(row.image_uploaded_at),
    image_content_url: text(row.image_content_url),
    link_type: text(row.link_type, 'none'),
    link_url: text(row.link_url),
    target_type: text(row.target_type),
    target_id: numberOr(row.target_id),
    display_order: numberOr(row.display_order, 100),
    status: numberOr(row.status),
    updated_at: text(row.updated_at)
  };
}

function toRiskReviewItem(raw: unknown): RiskReviewItem {
  const row = asRecord(raw);
  return {
    risk_id: numberOr(row.risk_id),
    risk_type: text(row.risk_type),
    target_type: text(row.target_type),
    target_id: numberOr(row.target_id),
    severity: text(row.severity, 'low'),
    status: numberOr(row.status),
    title: text(row.title, '风险任务'),
    summary: text(row.summary),
    decision: text(row.decision),
    handled_at: text(row.handled_at),
    updated_at: text(row.updated_at)
  };
}

function toAuditLogItem(raw: unknown): AuditLogItem {
  const row = asRecord(raw);
  return {
    id: numberOr(row.id),
    biz_type: text(row.biz_type),
    biz_id: numberOr(row.biz_id),
    action_type: text(row.action_type),
    operator_role: text(row.operator_role),
    trace_id: text(row.trace_id),
    created_at: text(row.created_at)
  };
}

function toFieldAccessLogItem(raw: unknown): FieldAccessLogItem {
  const row = asRecord(raw);
  return {
    id: numberOr(row.id),
    biz_type: text(row.biz_type),
    biz_id: numberOr(row.biz_id),
    field_name: text(row.field_name),
    access_type: text(row.access_type),
    operator_role: text(row.operator_role),
    trace_id: text(row.trace_id),
    created_at: text(row.created_at)
  };
}

function toOpenApiLogItem(raw: unknown): OpenApiLogItem {
  const row = asRecord(raw);
  return {
    id: numberOr(row.id),
    source_system: text(row.source_system),
    client_code: text(row.client_code),
    api_code: text(row.api_code),
    biz_type: text(row.biz_type),
    biz_id: numberOr(row.biz_id),
    http_status: numberOr(row.http_status),
    success_flag: Boolean(row.success_flag),
    trace_id: text(row.trace_id),
    created_at: text(row.created_at)
  };
}

export async function fetchCompanyReviewQueue(token: string): Promise<ApiResult<CompanyReviewItem[]>> {
  const result = await apiGet<unknown>('/api/admin/companies/review?auth_status=1&page=1&size=20', { token });
  const rows = Array.isArray(asRecord(result.data).company_list) ? asRecord(result.data).company_list as unknown[] : [];
  return { data: rows.map(toCompanyReviewItem), traceId: result.traceId };
}

export async function fetchJobReviewQueue(token: string): Promise<ApiResult<JobReviewItem[]>> {
  const result = await apiGet<unknown>('/api/admin/jobs/review?audit_status=1&page=1&size=20', { token });
  const rows = Array.isArray(asRecord(result.data).job_list) ? asRecord(result.data).job_list as unknown[] : [];
  return { data: rows.map(toJobReviewItem), traceId: result.traceId };
}

export async function fetchExportReviewQueue(token: string): Promise<ApiResult<CompanyExportApply[]>> {
  const result = await apiGet<unknown>('/api/admin/exports/review?approve_status=0&page=1&size=20', { token });
  const rows = Array.isArray(asRecord(result.data).export_list) ? asRecord(result.data).export_list as unknown[] : [];
  return { data: rows.map(toExportApply), traceId: result.traceId };
}

export async function fetchOpsOverview(token: string): Promise<ApiResult<OpsOverview>> {
  const result = await apiGet<unknown>('/api/admin/ops/overview', { token });
  return { data: toOpsOverview(result.data), traceId: result.traceId };
}

export async function fetchRecommendations(token: string): Promise<ApiResult<RecommendationItem[]>> {
  const result = await apiGet<unknown>('/api/admin/recommendations?page=1&size=20', { token });
  const rows = Array.isArray(asRecord(result.data).recommendation_list)
    ? asRecord(result.data).recommendation_list as unknown[]
    : [];
  return { data: rows.map(toRecommendationItem), traceId: result.traceId };
}

export async function fetchHomeSlots(token: string): Promise<ApiResult<HomeSlotItem[]>> {
  const result = await apiGet<unknown>('/api/admin/home-slots?page=1&size=20', { token });
  const rows = Array.isArray(asRecord(result.data).slot_list)
    ? asRecord(result.data).slot_list as unknown[]
    : [];
  return { data: rows.map(toHomeSlotItem), traceId: result.traceId };
}

export async function createRecommendation(
  token: string,
  payload: {
    slot_code: string;
    target_type: string;
    target_id: number;
    title_override: string;
    summary_override: string;
    display_order: number;
    status: number;
  }
): Promise<ApiResult<RecommendationItem>> {
  const result = await apiPost<unknown>('/api/admin/recommendations', payload, {
    token,
    idempotencyKey: createIdempotencyKey('admin-recommendation')
  });
  return { data: toRecommendationItem(result.data), traceId: result.traceId };
}

export async function createHomeSlot(
  token: string,
  payload: {
    slot_code: string;
    title: string;
    subtitle: string;
    image_url: string;
    image_alt: string;
    link_type: string;
    link_url: string;
    display_order: number;
    status: number;
  }
): Promise<ApiResult<HomeSlotItem>> {
  const result = await apiPost<unknown>('/api/admin/home-slots', payload, {
    token,
    idempotencyKey: createIdempotencyKey('admin-home-slot')
  });
  return { data: toHomeSlotItem(result.data), traceId: result.traceId };
}

export async function offlineRecommendation(token: string, recommendationId: number): Promise<ApiResult<RecommendationItem>> {
  const result = await apiPost<unknown>(`/api/admin/recommendations/${recommendationId}/offline`, {}, {
    token,
    idempotencyKey: createIdempotencyKey('admin-recommendation-offline')
  });
  return { data: toRecommendationItem(result.data), traceId: result.traceId };
}

export async function offlineHomeSlot(token: string, slotId: number): Promise<ApiResult<HomeSlotItem>> {
  const result = await apiPost<unknown>(`/api/admin/home-slots/${slotId}/offline`, {}, {
    token,
    idempotencyKey: createIdempotencyKey('admin-home-slot-offline')
  });
  return { data: toHomeSlotItem(result.data), traceId: result.traceId };
}

export async function uploadHomeSlotImage(token: string, slotId: number, file: File): Promise<ApiResult<HomeSlotItem>> {
  const formData = new FormData();
  formData.append('file', file);
  const result = await apiRequest<unknown>(`/api/admin/home-slots/${slotId}/image`, {
    method: 'POST',
    token,
    idempotencyKey: createIdempotencyKey('admin-home-slot-image'),
    body: formData
  });
  return { data: toHomeSlotItem(result.data), traceId: result.traceId };
}

export async function fetchHomeSlotImageBlob(token: string, item: HomeSlotItem): Promise<Blob> {
  const traceId = createTraceId();
  const response = await fetch(item.image_content_url, {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${token}`,
      'X-Trace-Id': traceId,
      Accept: item.image_content_type || 'image/*'
    },
    cache: 'no-store'
  });
  if (!response.ok) {
    throw new Error('首页运营位图片读取失败');
  }
  return response.blob();
}

export async function deleteHomeSlotImage(token: string, slotId: number): Promise<ApiResult<HomeSlotItem>> {
  const result = await apiRequest<unknown>(`/api/admin/home-slots/${slotId}/image`, {
    method: 'DELETE',
    token,
    idempotencyKey: createIdempotencyKey('admin-home-slot-image-delete')
  });
  return { data: toHomeSlotItem(result.data), traceId: result.traceId };
}

export async function fetchRiskReviews(token: string): Promise<ApiResult<RiskReviewItem[]>> {
  const result = await apiGet<unknown>('/api/admin/risk-reviews?page=1&size=20', { token });
  const rows = Array.isArray(asRecord(result.data).risk_review_list)
    ? asRecord(result.data).risk_review_list as unknown[]
    : [];
  return { data: rows.map(toRiskReviewItem), traceId: result.traceId };
}

export async function handleRiskReview(
  token: string,
  riskId: number,
  payload: { status: number; decision: string }
): Promise<ApiResult<RiskReviewItem>> {
  const result = await apiPost<unknown>(`/api/admin/risk-reviews/${riskId}/handle`, payload, {
    token,
    idempotencyKey: createIdempotencyKey('admin-risk-review')
  });
  return { data: toRiskReviewItem(result.data), traceId: result.traceId };
}

export async function reviewCompany(
  token: string,
  payload: { company_id: number; audit_status: 2 | 3; memo: string }
): Promise<ApiResult<unknown>> {
  return apiPost('/api/admin/companies/review', payload, { token });
}

export async function reviewJob(
  token: string,
  payload: { job_id: number; audit_status: 2 | 3; memo: string }
): Promise<ApiResult<unknown>> {
  return apiPost('/api/admin/jobs/review', payload, { token });
}

export async function reviewExport(
  token: string,
  payload: { export_id: number; approve_status: 1 | 2; memo: string }
): Promise<ApiResult<unknown>> {
  return apiPost('/api/admin/exports/review', payload, { token });
}

export async function updateRecommendation(
  token: string,
  recommendationId: number,
  payload: {
    slot_code: string;
    target_type: string;
    target_id: number;
    title_override: string;
    summary_override: string;
    display_order: number;
    status: number;
  }
): Promise<ApiResult<RecommendationItem>> {
  const result = await apiRequest<unknown>(`/api/admin/recommendations/${recommendationId}`, {
    method: 'PUT',
    body: payload,
    token,
    idempotencyKey: createIdempotencyKey('admin-recommendation-update')
  });
  return { data: toRecommendationItem(result.data), traceId: result.traceId };
}

export async function fetchAuditTrace(token: string, traceId: string): Promise<ApiResult<AuditTraceSummary>> {
  const result = await apiGet<unknown>(`/api/admin/audit-traces/${encodeURIComponent(traceId)}`, { token });
  const payload = asRecord(result.data);
  const auditRows = Array.isArray(payload.audit_log_list) ? payload.audit_log_list : [];
  const accessRows = Array.isArray(payload.access_log_list) ? payload.access_log_list : [];
  const openApiRows = Array.isArray(payload.open_api_log_list) ? payload.open_api_log_list : [];
  return {
    data: {
      trace_id: text(payload.trace_id, traceId),
      audit_count: auditRows.length,
      access_count: accessRows.length,
      open_api_count: openApiRows.length,
      audit_log_list: auditRows.map(toAuditLogItem),
      access_log_list: accessRows.map(toFieldAccessLogItem),
      open_api_log_list: openApiRows.map(toOpenApiLogItem)
    },
    traceId: result.traceId
  };
}
