import { isHttpClientError } from '@/lib/httpClient';
import type { ContentChannelInitialState, EventChannelInitialState } from './PortalContentPages';
import { fetchPortalContents, fetchPortalEvents, type PortalContentQuery } from './portalContentApi';

export type PortalRouteSearchParams = Record<string, string | string[] | undefined>;

function textParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

function intParam(value: string | string[] | undefined, fallback: number): number {
  const parsed = Number.parseInt(textParam(value) ?? '', 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

export function routeQuery(searchParams: PortalRouteSearchParams, defaults: Partial<PortalContentQuery> = {}) {
  return {
    ...defaults,
    city_code: textParam(searchParams.city_code) ?? defaults.city_code,
    type_code: textParam(searchParams.type_code ?? searchParams.type) ?? defaults.type_code,
    page: intParam(searchParams.page, defaults.page ?? 1),
    size: intParam(searchParams.size, defaults.size ?? 12)
  };
}

export async function loadContentChannelInitialState(
  searchParams: PortalRouteSearchParams,
  contentType: string
): Promise<ContentChannelInitialState> {
  const query = routeQuery(searchParams);
  try {
    const result = await fetchPortalContents({ ...query, content_type: contentType });
    return {
      status: result.data.content_list.length > 0 ? 'ready' : 'empty',
      query,
      page: result.data,
      traceId: result.traceId
    };
  } catch (error) {
    return {
      status: 'error',
      query,
      message: error instanceof Error ? error.message : '公开内容暂时不可用。',
      traceId: isHttpClientError(error) ? error.traceId : undefined
    };
  }
}

export async function loadEventChannelInitialState(
  searchParams: PortalRouteSearchParams,
  defaults: Partial<PortalContentQuery> = {}
): Promise<EventChannelInitialState> {
  const query = routeQuery(searchParams, defaults);
  try {
    const result = await fetchPortalEvents(query);
    return {
      status: result.data.event_list.length > 0 ? 'ready' : 'empty',
      query,
      page: result.data,
      traceId: result.traceId
    };
  } catch (error) {
    return {
      status: 'error',
      query,
      message: error instanceof Error ? error.message : '招聘会暂时不可用，请稍后重试。',
      traceId: isHttpClientError(error) ? error.traceId : undefined
    };
  }
}
