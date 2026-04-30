import { apiGet, type ApiResult } from '@/lib/httpClient';

export type TalentSnapshot = {
  snapshot_id: number;
  display_name_masked: string;
  city_code: string;
  category_code: string;
  skills_summary: string;
  experience_years: number | null;
  updated_at: string;
};

export type TalentSnapshotPage = {
  snapshot_list: TalentSnapshot[];
  total: number;
  page: number;
  size: number;
};

export type TalentSnapshotQuery = {
  city_code?: string;
  category_code?: string;
  experience_min?: string | number;
  experience_max?: string | number;
  updated_within?: string;
  sort?: string;
  page?: number;
  size?: number;
};

type RawTalentSnapshot = Record<string, unknown>;

export const TALENT_SNAPSHOT_RENDER_FIELDS = [
  'snapshot_id',
  'display_name_masked',
  'city_code',
  'category_code',
  'skills_summary',
  'experience_years',
  'updated_at'
] as const;

function text(value: unknown): string {
  return typeof value === 'string' ? value : '';
}

function nullableNumber(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function numberOr(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback;
}

function toSnapshot(raw: RawTalentSnapshot): TalentSnapshot {
  return {
    snapshot_id: numberOr(raw.snapshot_id, 0),
    display_name_masked: text(raw.display_name_masked),
    city_code: text(raw.city_code),
    category_code: text(raw.category_code),
    skills_summary: text(raw.skills_summary),
    experience_years: nullableNumber(raw.experience_years),
    updated_at: text(raw.updated_at)
  };
}

function toPage(raw: unknown): TalentSnapshotPage {
  const payload = (raw && typeof raw === 'object' ? raw : {}) as Record<string, unknown>;
  const rows = Array.isArray(payload.snapshot_list) ? payload.snapshot_list : [];

  return {
    snapshot_list: rows.map((row) => toSnapshot((row && typeof row === 'object' ? row : {}) as RawTalentSnapshot)),
    total: numberOr(payload.total, rows.length),
    page: numberOr(payload.page, 1),
    size: numberOr(payload.size, 12)
  };
}

function queryString(query: TalentSnapshotQuery): string {
  const params = new URLSearchParams();

  const entries: Record<string, string | number | undefined> = {
    city_code: query.city_code,
    category_code: query.category_code,
    experience_min: query.experience_min,
    experience_max: query.experience_max,
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

export async function fetchTalentSnapshots(query: TalentSnapshotQuery): Promise<ApiResult<TalentSnapshotPage>> {
  const result = await apiGet<unknown>(`/api/portal/talent-snapshots?${queryString(query)}`);

  return {
    data: toPage(result.data),
    traceId: result.traceId
  };
}
