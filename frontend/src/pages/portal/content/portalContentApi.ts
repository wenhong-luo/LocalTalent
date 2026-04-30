import { apiGet, type ApiResult } from '@/lib/httpClient';

export type PortalContent = {
  content_id: number;
  content_type: string;
  title: string;
  cover_url: string;
  summary: string;
  body_text: string;
  city_code: string;
  publish_time: string;
  updated_at: string;
};

export type PortalContentPage = {
  content_list: PortalContent[];
  total: number;
};

export type PortalEvent = {
  event_id: number;
  title: string;
  type_code: string;
  city_code: string;
  start_time: string;
  end_time: string;
  location: string;
  status: number;
  updated_at: string;
};

export type PortalEventPage = {
  event_list: PortalEvent[];
  total: number;
};

export type PortalContentQuery = {
  content_type?: string;
  city_code?: string;
  type_code?: string;
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

function toContent(raw: RawObject): PortalContent {
  return {
    content_id: numberOr(raw.content_id, 0),
    content_type: text(raw.content_type),
    title: text(raw.title),
    cover_url: text(raw.cover_url),
    summary: text(raw.summary),
    body_text: text(raw.body_text),
    city_code: text(raw.city_code),
    publish_time: text(raw.publish_time),
    updated_at: text(raw.updated_at)
  };
}

function toEvent(raw: RawObject): PortalEvent {
  return {
    event_id: numberOr(raw.event_id, 0),
    title: text(raw.title),
    type_code: text(raw.type_code),
    city_code: text(raw.city_code),
    start_time: text(raw.start_time),
    end_time: text(raw.end_time),
    location: text(raw.location),
    status: numberOr(raw.status, 0),
    updated_at: text(raw.updated_at)
  };
}

function toContentPage(raw: unknown): PortalContentPage {
  const payload = (raw && typeof raw === 'object' ? raw : {}) as RawObject;
  const rows = Array.isArray(payload.content_list) ? payload.content_list : [];
  return {
    content_list: rows.map((row) => toContent((row && typeof row === 'object' ? row : {}) as RawObject)),
    total: numberOr(payload.total, rows.length)
  };
}

function toEventPage(raw: unknown): PortalEventPage {
  const payload = (raw && typeof raw === 'object' ? raw : {}) as RawObject;
  const rows = Array.isArray(payload.event_list) ? payload.event_list : [];
  return {
    event_list: rows.map((row) => toEvent((row && typeof row === 'object' ? row : {}) as RawObject)),
    total: numberOr(payload.total, rows.length)
  };
}

function queryString(entries: Record<string, string | number | undefined>): string {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(entries)) {
    if (typeof value !== 'undefined' && String(value).trim() !== '') {
      params.set(key, String(value));
    }
  }
  return params.toString();
}

export async function fetchPortalContents(query: PortalContentQuery): Promise<ApiResult<PortalContentPage>> {
  const result = await apiGet<unknown>(
    `/api/portal/contents?${queryString({
      content_type: query.content_type,
      city_code: query.city_code,
      page: query.page ?? 1,
      size: query.size ?? 12
    })}`
  );
  return {
    data: toContentPage(result.data),
    traceId: result.traceId
  };
}

export async function fetchPortalContent(contentId: string | number): Promise<ApiResult<PortalContent>> {
  const result = await apiGet<unknown>(`/api/portal/contents/${contentId}`);
  return {
    data: toContent((result.data && typeof result.data === 'object' ? result.data : {}) as RawObject),
    traceId: result.traceId
  };
}

export async function fetchPortalEvents(query: PortalContentQuery): Promise<ApiResult<PortalEventPage>> {
  const result = await apiGet<unknown>(
    `/api/portal/job-fairs?${queryString({
      type_code: query.type_code,
      city_code: query.city_code,
      page: query.page ?? 1,
      size: query.size ?? 12
    })}`
  );
  return {
    data: toEventPage(result.data),
    traceId: result.traceId
  };
}

export async function fetchPortalEvent(eventId: string | number): Promise<ApiResult<PortalEvent>> {
  const result = await apiGet<unknown>(`/api/portal/job-fairs/${eventId}`);
  return {
    data: toEvent((result.data && typeof result.data === 'object' ? result.data : {}) as RawObject),
    traceId: result.traceId
  };
}
