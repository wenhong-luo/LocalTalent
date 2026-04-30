import { apiGet, type ApiResult } from '@/lib/httpClient';
import type { PortalJob } from '@/pages/portal/jobs/portalJobApi';

export type PortalCompany = {
  company_id: number;
  company_name: string;
  city_code: string;
  industry_code: string;
  nature_code: string;
  scale_code: string;
  company_verified: boolean;
  company_profile: string;
  open_job_count: number;
  updated_at: string;
  open_jobs: PortalJob[];
};

export type PortalCompanyPage = {
  company_list: PortalCompany[];
  total: number;
};

export type CompanySearchQuery = {
  keyword?: string;
  city_code?: string;
  industry_code?: string;
  nature_code?: string;
  scale_code?: string;
  verified?: string;
  page?: number;
  size?: number;
};

type RawObject = Record<string, unknown>;

function text(value: unknown): string {
  return typeof value === 'string' ? value : '';
}

function numberOr(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback;
}

function nullableNumber(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function bool(value: unknown): boolean {
  return typeof value === 'boolean' ? value : false;
}

function toJob(raw: RawObject): PortalJob {
  return {
    job_id: numberOr(raw.jobId ?? raw.job_id, 0),
    title: text(raw.title),
    company_name: '',
    category_code: text(raw.categoryCode ?? raw.category_code),
    city_code: text(raw.cityCode ?? raw.city_code),
    salary_min: nullableNumber(raw.salaryMin ?? raw.salary_min),
    salary_max: nullableNumber(raw.salaryMax ?? raw.salary_max),
    job_desc: '',
    updated_at: text(raw.updatedAt ?? raw.updated_at)
  };
}

function toCompany(raw: RawObject): PortalCompany {
  const rawJobs = raw.openJobs ?? raw.open_jobs;
  const jobs: unknown[] = Array.isArray(rawJobs) ? rawJobs : [];
  return {
    company_id: numberOr(raw.companyId ?? raw.company_id, 0),
    company_name: text(raw.companyName ?? raw.company_name),
    city_code: text(raw.cityCode ?? raw.city_code),
    industry_code: text(raw.industryCode ?? raw.industry_code),
    nature_code: text(raw.natureCode ?? raw.nature_code),
    scale_code: text(raw.scaleCode ?? raw.scale_code),
    company_verified: bool(raw.companyVerified ?? raw.company_verified),
    company_profile: text(raw.companyProfile ?? raw.company_profile),
    open_job_count: numberOr(raw.openJobCount ?? raw.open_job_count, 0),
    updated_at: text(raw.updatedAt ?? raw.updated_at),
    open_jobs: jobs.map((job) => toJob((job && typeof job === 'object' ? job : {}) as RawObject))
  };
}

function toPage(raw: unknown): PortalCompanyPage {
  const payload = (raw && typeof raw === 'object' ? raw : {}) as RawObject;
  const rawRows = payload.companyList ?? payload.company_list;
  const rows: unknown[] = Array.isArray(rawRows) ? rawRows : [];
  return {
    company_list: rows.map((row) => toCompany((row && typeof row === 'object' ? row : {}) as RawObject)),
    total: numberOr(payload.total, rows.length)
  };
}

function queryString(query: CompanySearchQuery): string {
  const params = new URLSearchParams();
  const entries: Record<string, string | number | undefined> = {
    keyword: query.keyword,
    city_code: query.city_code,
    industry_code: query.industry_code,
    nature_code: query.nature_code,
    scale_code: query.scale_code,
    verified: query.verified,
    page: query.page ?? 1,
    size: query.size ?? 12
  };

  for (const [key, value] of Object.entries(entries)) {
    if (typeof value !== 'undefined' && String(value).trim() !== '') {
      params.set(key, String(value));
    }
  }

  return params.toString();
}

export async function fetchPortalCompanies(query: CompanySearchQuery): Promise<ApiResult<PortalCompanyPage>> {
  const result = await apiGet<unknown>(`/api/portal/companies?${queryString(query)}`);
  return {
    data: toPage(result.data),
    traceId: result.traceId
  };
}

export async function fetchPortalCompany(companyId: string | number): Promise<ApiResult<PortalCompany>> {
  const result = await apiGet<unknown>(`/api/portal/companies/${companyId}`);
  return {
    data: toCompany((result.data && typeof result.data === 'object' ? result.data : {}) as RawObject),
    traceId: result.traceId
  };
}
