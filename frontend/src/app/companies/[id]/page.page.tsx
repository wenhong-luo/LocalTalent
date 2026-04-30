import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { isHttpClientError } from '@/lib/httpClient';
import { CompanyDetailPage } from '@/pages/portal/companies/CompanyDetailPage';
import { fetchPortalCompany } from '@/pages/portal/companies/portalCompanyApi';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '企业公开主页',
  description: 'LocalTalent 企业公开主页仅展示认证企业公开信息和在线职位聚合。'
};

type PageParams = {
  id: string;
};

export default async function CompanyDetailRoute({ params }: { params: Promise<PageParams> }) {
  const { id } = await params;

  try {
    const result = await fetchPortalCompany(id);
    return (
      <PortalShell>
        <CompanyDetailPage company={result.data} traceId={result.traceId} />
      </PortalShell>
    );
  } catch (error) {
    return (
      <PortalShell>
        <CompanyDetailPage
          message={error instanceof Error ? error.message : '企业公开主页暂时不可用，请稍后重试。'}
          traceId={isHttpClientError(error) ? error.traceId : undefined}
        />
      </PortalShell>
    );
  }
}
