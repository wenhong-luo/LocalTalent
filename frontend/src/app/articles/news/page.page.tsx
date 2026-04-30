import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { isHttpClientError } from '@/lib/httpClient';
import { ContentChannelPage, type ContentChannelInitialState } from '@/pages/portal/content/PortalContentPages';
import { fetchPortalContents } from '@/pages/portal/content/portalContentApi';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '职场资讯',
  description: 'LocalTalent 职场资讯频道展示公开资讯和就业服务内容。'
};

type SearchParams = Record<string, string | string[] | undefined>;

function textParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

export default async function NewsRoute({ searchParams }: { searchParams?: Promise<SearchParams> }) {
  const params = (await searchParams) ?? {};
  const query = {
    city_code: textParam(params.city_code),
    page: Number.parseInt(textParam(params.page) ?? '1', 10) || 1,
    size: Number.parseInt(textParam(params.size) ?? '12', 10) || 12
  };
  let initialState: ContentChannelInitialState;

  try {
    const result = await fetchPortalContents({ ...query, content_type: 'news' });
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
      message: error instanceof Error ? error.message : '职场资讯暂时不可用。',
      traceId: isHttpClientError(error) ? error.traceId : undefined
    };
  }

  return (
    <PortalShell>
      <ContentChannelPage
        config={{
          contentType: 'news',
          eyebrow: 'Career News',
          title: '职场资讯',
          description: '展示公开职场资讯与地方招聘动态，不含个人联系信息或企业后台资料。',
          detailBasePath: '/articles',
          badge: '资讯公开频道'
        }}
        initialState={initialState}
      />
    </PortalShell>
  );
}
