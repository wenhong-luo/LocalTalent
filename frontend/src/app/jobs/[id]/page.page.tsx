import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { isHttpClientError } from '@/lib/httpClient';
import { JobDetailPage } from '@/pages/portal/jobs/JobDetailPage';
import { fetchPortalJob } from '@/pages/portal/jobs/portalJobApi';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '职位详情',
  description: 'LocalTalent 公开职位详情仅展示职位与企业公开字段。'
};

type PageParams = {
  id: string;
};

export default async function JobDetailRoute({ params }: { params: Promise<PageParams> }) {
  const { id } = await params;

  try {
    const result = await fetchPortalJob(id);
    return (
      <PortalShell>
        <JobDetailPage job={result.data} traceId={result.traceId} />
      </PortalShell>
    );
  } catch (error) {
    return (
      <PortalShell>
        <JobDetailPage
          message={error instanceof Error ? error.message : '职位详情暂时不可用，请稍后重试。'}
          traceId={isHttpClientError(error) ? error.traceId : undefined}
        />
      </PortalShell>
    );
  }
}
