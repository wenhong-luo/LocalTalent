import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { isHttpClientError } from '@/lib/httpClient';
import { ContentChannelPage, type ContentChannelInitialState } from '@/pages/portal/content/PortalContentPages';
import { fetchPortalContents } from '@/pages/portal/content/portalContentApi';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: 'HR 工具箱',
  description: 'LocalTalent HR 工具箱展示公开工具文章和招聘服务指南。'
};

type SearchParams = Record<string, string | string[] | undefined>;

function textParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

export default async function HrToolsRoute({ searchParams }: { searchParams?: Promise<SearchParams> }) {
  const params = (await searchParams) ?? {};
  const query = {
    city_code: textParam(params.city_code),
    page: Number.parseInt(textParam(params.page) ?? '1', 10) || 1,
    size: Number.parseInt(textParam(params.size) ?? '12', 10) || 12
  };
  let initialState: ContentChannelInitialState;

  try {
    const result = await fetchPortalContents({ ...query, content_type: 'hr_tool' });
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
      message: error instanceof Error ? error.message : 'HR 工具箱暂时不可用。',
      traceId: isHttpClientError(error) ? error.traceId : undefined
    };
  }

  return (
    <PortalShell>
      <ContentChannelPage
        config={{
          contentType: 'hr_tool',
          eyebrow: 'HR Toolkit',
          title: 'HR 工具箱',
          description: '展示公开 HR 工具、招聘流程指南和政策说明，不包含企业后台材料或候选人个人信息。',
          detailBasePath: '/hr-tools',
          badge: 'HR 工具公开频道'
        }}
        initialState={initialState}
      />
    </PortalShell>
  );
}
