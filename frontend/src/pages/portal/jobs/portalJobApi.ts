import { apiGet, type ApiResult } from '@/lib/httpClient';

export type PortalJob = {
  job_id: number;
  title: string;
  company_name: string;
  category_code: string;
  city_code: string;
  salary_min: number | null;
  salary_max: number | null;
  job_desc: string;
  updated_at: string;
};

export type PortalJobPage = {
  job_list: PortalJob[];
  total: number;
};

export type JobSearchQuery = {
  keyword?: string;
  city_code?: string;
  category_code?: string;
  salary_range?: string;
  salary_min?: number;
  salary_max?: number;
  experience_code?: string;
  industry_code?: string;
  scale_code?: string;
  company_nature?: string;
  education_code?: string;
  updated_within?: string;
  sort?: string;
  famous?: string;
  page?: number;
  size?: number;
};

type RawJob = Record<string, unknown>;

function text(value: unknown): string {
  return typeof value === 'string' ? value : '';
}

function nullableNumber(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function numberOr(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback;
}

function toJob(raw: RawJob): PortalJob {
  return {
    job_id: numberOr(raw.job_id, 0),
    title: text(raw.title),
    company_name: text(raw.company_name),
    category_code: text(raw.category_code),
    city_code: text(raw.city_code),
    salary_min: nullableNumber(raw.salary_min),
    salary_max: nullableNumber(raw.salary_max),
    job_desc: text(raw.job_desc),
    updated_at: text(raw.updated_at)
  };
}

function toPage(raw: unknown): PortalJobPage {
  const payload = (raw && typeof raw === 'object' ? raw : {}) as Record<string, unknown>;
  const rows = Array.isArray(payload.job_list) ? payload.job_list : [];

  return {
    job_list: rows.map((row) => toJob((row && typeof row === 'object' ? row : {}) as RawJob)),
    total: numberOr(payload.total, rows.length)
  };
}

function salaryRange(query: JobSearchQuery): { salary_min?: number; salary_max?: number } {
  if (query.salary_min || query.salary_max) {
    return {
      salary_min: query.salary_min,
      salary_max: query.salary_max
    };
  }

  if (!query.salary_range) {
    return {};
  }

  const [min, max] = query.salary_range.split('-');
  return {
    salary_min: min ? Number.parseInt(min, 10) : undefined,
    salary_max: max ? Number.parseInt(max, 10) : undefined
  };
}

function queryString(query: JobSearchQuery): string {
  const params = new URLSearchParams();
  const salary = salaryRange(query);

  const entries: Record<string, string | number | undefined> = {
    keyword: query.keyword,
    city_code: query.city_code,
    category_code: query.category_code,
    salary_min: salary.salary_min,
    salary_max: salary.salary_max,
    industry_code: query.industry_code,
    scale_code: query.scale_code,
    updated_within: query.updated_within,
    sort: query.sort,
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

export function salaryText(job: Pick<PortalJob, 'salary_min' | 'salary_max'>): string {
  if (job.salary_min === null && job.salary_max === null) {
    return '薪资面议';
  }

  if (job.salary_min !== null && job.salary_max !== null) {
    return `${job.salary_min / 1000}K-${job.salary_max / 1000}K`;
  }

  if (job.salary_min !== null) {
    return `${job.salary_min / 1000}K 起`;
  }

  return `${(job.salary_max ?? 0) / 1000}K 以内`;
}

export async function fetchPortalJobs(query: JobSearchQuery): Promise<ApiResult<PortalJobPage>> {
  const result = await apiGet<unknown>(`/api/portal/jobs?${queryString(query)}`);

  return {
    data: toPage(result.data),
    traceId: result.traceId
  };
}

export async function fetchPortalJob(jobId: string | number): Promise<ApiResult<PortalJob>> {
  const result = await apiGet<unknown>(`/api/portal/jobs/${jobId}`);

  return {
    data: toJob((result.data && typeof result.data === 'object' ? result.data : {}) as RawJob),
    traceId: result.traceId
  };
}
