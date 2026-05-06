import { apiGet } from '@/lib/httpClient';

type RawRecord = Record<string, unknown>;

export type PortalRecommendationItem = {
  target_type: string;
  target_id: number;
  title: string;
  summary: string;
  tags: string[];
  url: string;
  city_code: string;
  updated_at: string;
  display_order: number;
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

function toRecommendationItem(raw: unknown): PortalRecommendationItem {
  const row = asRecord(raw);
  return {
    target_type: text(row.target_type),
    target_id: numberOr(row.target_id),
    title: text(row.title, '运营位待配置'),
    summary: text(row.summary),
    tags: Array.isArray(row.tags) ? row.tags.map((tag) => text(tag)).filter(Boolean) : [],
    url: text(row.url, '#'),
    city_code: text(row.city_code),
    updated_at: text(row.updated_at),
    display_order: numberOr(row.display_order)
  };
}

export async function fetchPortalRecommendations(slotCode: string, limit = 8): Promise<PortalRecommendationItem[]> {
  const params = new URLSearchParams({
    slot_code: slotCode,
    limit: String(limit)
  });
  const result = await apiGet<unknown>(`/api/portal/recommendations?${params.toString()}`);
  const rows = Array.isArray(asRecord(result.data).recommendation_list)
    ? asRecord(result.data).recommendation_list as unknown[]
    : [];
  return rows.map(toRecommendationItem);
}
