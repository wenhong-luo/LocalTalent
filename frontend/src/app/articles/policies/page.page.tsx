import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { isHttpClientError } from '@/lib/httpClient';
import { ContentChannelPage, type ContentChannelInitialState } from '@/pages/portal/content/PortalContentPages';
import { fetchPortalContents } from '@/pages/portal/content/portalContentApi';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '就业政策',
  description: 'LocalTalent 就业政策频道展示公开政策内容，不展示个人明细或审核材料。'
};

type SearchParams = Record<string, string | string[] | undefined>;

function textParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

export default async function PoliciesRoute({ searchParams }: { searchParams?: Promise<SearchParams> }) {
  const params = (await searchParams) ?? {};
  const query = {
    city_code: textParam(params.city_code),
    page: Number.parseInt(textParam(params.page) ?? '1', 10) || 1,
    size: Number.parseInt(textParam(params.size) ?? '12', 10) || 12
  };
  let initialState: ContentChannelInitialState;

  try {
    const result = await fetchPortalContents({ ...query, content_type: 'policy' });
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
      message: error instanceof Error ? error.message : '就业政策暂时不可用。',
      traceId: isHttpClientError(error) ? error.traceId : undefined
    };
  }

  return (
    <PortalShell>
      <ContentChannelPage
        config={{
          contentType: 'policy',
          eyebrow: 'Employment Policy',
          title: '就业政策',
          description: '展示本地就业政策、服务公告和求职扶持信息。公开内容不含个人明细和审核材料。',
          detailBasePath: '/articles',
          badge: '政策公开频道'
        }}
        initialState={initialState}
      />
    </PortalShell>
  );
}
