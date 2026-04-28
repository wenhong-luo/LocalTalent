import { apiGet, apiPost, type ApiResult } from '@/lib/httpClient';
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
      open_api_count: openApiRows.length
    },
    traceId: result.traceId
  };
}
