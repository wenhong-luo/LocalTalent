import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { isHttpClientError } from '@/lib/httpClient';
import { JobFairDetailPage } from '@/pages/portal/content/PortalContentPages';
import { fetchPortalEvent } from '@/pages/portal/content/portalContentApi';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '招聘会详情',
  description: 'LocalTalent 招聘会详情仅展示活动公开字段。'
};

type PageParams = {
  id: string;
};

export default async function JobFairDetailRoute({ params }: { params: Promise<PageParams> }) {
  const { id } = await params;

  try {
    const result = await fetchPortalEvent(id);
    return (
      <PortalShell>
        <JobFairDetailPage event={result.data} traceId={result.traceId} title="招聘会" />
      </PortalShell>
    );
  } catch (error) {
    return (
      <PortalShell>
        <JobFairDetailPage
          title="招聘会"
          message={error instanceof Error ? error.message : '招聘会详情暂时不可用。'}
          traceId={isHttpClientError(error) ? error.traceId : undefined}
        />
      </PortalShell>
    );
  }
}
