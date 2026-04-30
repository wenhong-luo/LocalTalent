import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { isHttpClientError } from '@/lib/httpClient';
import { PortalContentDetailPage } from '@/pages/portal/content/PortalContentPages';
import { fetchPortalContent } from '@/pages/portal/content/portalContentApi';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: 'HR 工具详情',
  description: 'LocalTalent HR 工具详情仅展示公开内容。'
};

type PageParams = {
  id: string;
};

export default async function HrToolDetailRoute({ params }: { params: Promise<PageParams> }) {
  const { id } = await params;

  try {
    const result = await fetchPortalContent(id);
    return (
      <PortalShell>
        <PortalContentDetailPage content={result.data} title="HR 工具" traceId={result.traceId} />
      </PortalShell>
    );
  } catch (error) {
    return (
      <PortalShell>
        <PortalContentDetailPage
          title="HR 工具"
          message={error instanceof Error ? error.message : 'HR 工具详情暂时不可用。'}
          traceId={isHttpClientError(error) ? error.traceId : undefined}
        />
      </PortalShell>
    );
  }
}
