import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { isHttpClientError } from '@/lib/httpClient';
import { PortalContentDetailPage } from '@/pages/portal/content/PortalContentPages';
import { fetchPortalContent } from '@/pages/portal/content/portalContentApi';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '公开说明',
  description: 'LocalTalent 公开说明详情仅展示已发布公开内容。'
};

type PageParams = {
  id: string;
};

export default async function ExplainDetailRoute({ params }: { params: Promise<PageParams> }) {
  const { id } = await params;

  try {
    const result = await fetchPortalContent(id);
    return (
      <PortalShell>
        <PortalContentDetailPage content={result.data} title="公开说明" traceId={result.traceId} />
      </PortalShell>
    );
  } catch (error) {
    return (
      <PortalShell>
        <PortalContentDetailPage
          title="公开说明"
          message={error instanceof Error ? error.message : '公开说明暂时不可用。'}
          traceId={isHttpClientError(error) ? error.traceId : undefined}
        />
      </PortalShell>
    );
  }
}
