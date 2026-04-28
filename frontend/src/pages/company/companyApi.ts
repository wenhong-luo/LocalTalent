import { apiGet, apiPost, apiRequest, type ApiResult } from '@/lib/httpClient';

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

function asRecord(value: unknown): RawRecord {
  return value && typeof value === 'object' ? value as RawRecord : {};
}

function text(value: unknown, fallback = ''): string {
  return typeof value === 'string' ? value : fallback;
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
