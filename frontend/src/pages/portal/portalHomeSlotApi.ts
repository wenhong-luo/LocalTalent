import { apiGet } from '@/lib/httpClient';

type RawRecord = Record<string, unknown>;

export type PortalHomeSlotItem = {
  slot_id: number;
  slot_code: string;
  title: string;
  subtitle: string;
  image_url: string;
  image_alt: string;
  link_url: string;
  display_order: number;
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

function toHomeSlotItem(raw: unknown): PortalHomeSlotItem {
  const row = asRecord(raw);
  return {
    slot_id: numberOr(row.slot_id),
    slot_code: text(row.slot_code),
    title: text(row.title, '运营位待配置'),
    subtitle: text(row.subtitle),
    image_url: text(row.image_url),
    image_alt: text(row.image_alt),
    link_url: text(row.link_url),
    display_order: numberOr(row.display_order),
    updated_at: text(row.updated_at)
  };
}

export async function fetchPortalHomeSlots(slotCodes: string[], limit = 12): Promise<PortalHomeSlotItem[]> {
  const normalizedCodes = slotCodes.map((slotCode) => slotCode.trim()).filter(Boolean);
  if (normalizedCodes.length === 0) {
    return [];
  }

  const params = new URLSearchParams({
    slot_codes: normalizedCodes.join(','),
    limit: String(limit)
  });
  const result = await apiGet<unknown>(`/api/portal/home-slots?${params.toString()}`);
  const rows = Array.isArray(asRecord(result.data).slot_list)
    ? asRecord(result.data).slot_list as unknown[]
    : [];
  return rows.map(toHomeSlotItem).sort((left, right) => left.display_order - right.display_order);
}
