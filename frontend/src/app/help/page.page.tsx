import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { isHttpClientError } from '@/lib/httpClient';
import { ContentChannelPage, type ContentChannelInitialState } from '@/pages/portal/content/PortalContentPages';
import { fetchPortalContents } from '@/pages/portal/content/portalContentApi';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '帮助中心',
  description: 'LocalTalent 帮助中心展示公开帮助内容。'
};

type SearchParams = Record<string, string | string[] | undefined>;

function textParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

export default async function HelpRoute({ searchParams }: { searchParams?: Promise<SearchParams> }) {
  const params = (await searchParams) ?? {};
  const query = {
    city_code: textParam(params.city_code),
    page: Number.parseInt(textParam(params.page) ?? '1', 10) || 1,
    size: Number.parseInt(textParam(params.size) ?? '12', 10) || 12
  };
  let initialState: ContentChannelInitialState;

  try {
    const result = await fetchPortalContents({ ...query, content_type: 'help' });
    initialState = {
      status: result.data.content_list.length > 0 ? 'ready' : 'empty',
      query,
      page: result.data,
      traceId: result.traceId
    };
  } catch (error) {
    initialState = {
      status: 'error',
      query,
      message: error instanceof Error ? error.message : '帮助中心暂时不可用。',
      traceId: isHttpClientError(error) ? error.traceId : undefined
    };
  }

  return (
    <PortalShell>
      <ContentChannelPage
        config={{
          contentType: 'help',
          eyebrow: 'Help Center',
          title: '帮助中心',
          description: '展示公开帮助文档和服务说明，不包含个人明细、审核材料或后台备注。',
          detailBasePath: '/articles',
          badge: '帮助公开频道'
        }}
        initialState={initialState}
      />
    </PortalShell>
  );
}
