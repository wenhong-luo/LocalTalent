import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { isHttpClientError } from '@/lib/httpClient';
import { JobFairListPage, type EventChannelInitialState } from '@/pages/portal/content/PortalContentPages';
import { fetchPortalEvents } from '@/pages/portal/content/portalContentApi';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '招聘会',
  description: 'LocalTalent 招聘会公开频道仅展示活动公开信息，不展示报名名单、参会明细或签到证据。'
};

type SearchParams = Record<string, string | string[] | undefined>;

function textParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

export default async function JobFairsRoute({ searchParams }: { searchParams?: Promise<SearchParams> }) {
  const params = (await searchParams) ?? {};
  const query = {
    type_code: textParam(params.type_code ?? params.type),
    city_code: textParam(params.city_code),
    page: Number.parseInt(textParam(params.page) ?? '1', 10) || 1,
    size: Number.parseInt(textParam(params.size) ?? '12', 10) || 12
  };
  let initialState: EventChannelInitialState;

  try {
    const result = await fetchPortalEvents(query);
    initialState = {
      status: result.data.event_list.length > 0 ? 'ready' : 'empty',
      query,
      page: result.data,
      traceId: result.traceId
    };
  } catch (error) {
    initialState = {
      status: 'error',
      query,
      message: error instanceof Error ? error.message : '招聘会暂时不可用，请稍后重试。',
      traceId: isHttpClientError(error) ? error.traceId : undefined
    };
  }

  return (
    <PortalShell>
      <JobFairListPage initialState={initialState} />
    </PortalShell>
  );
}
