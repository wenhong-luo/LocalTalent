import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { JobFairListPage } from '@/pages/portal/content/PortalContentPages';
import { loadEventChannelInitialState, type PortalRouteSearchParams } from '@/pages/portal/content/portalContentPageState';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '校园宣讲与双选会',
  description: 'LocalTalent 校园宣讲与双选会入口只展示公开活动信息。'
};

export default async function CampusElectionsRoute({ searchParams }: { searchParams?: Promise<PortalRouteSearchParams> }) {
  const initialState = await loadEventChannelInitialState((await searchParams) ?? {}, {
    type_code: 'campus'
  });

  return (
    <PortalShell>
      <JobFairListPage
        initialState={initialState}
        topic={{
          eyebrow: 'Campus Elections',
          title: '校园宣讲与双选会',
          description: '只展示校园公开活动信息；报名名单、学生个人明细和签到证据不进入公开页面。',
          badge: '校园活动入口'
        }}
      />
    </PortalShell>
  );
}
