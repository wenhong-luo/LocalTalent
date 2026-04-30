import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { JobFairListPage } from '@/pages/portal/content/PortalContentPages';
import { loadEventChannelInitialState, type PortalRouteSearchParams } from '@/pages/portal/content/portalContentPageState';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '校园招聘会',
  description: 'LocalTalent 校园招聘会只展示公开活动信息，不展示报名名单或学生个人明细。'
};

export default async function CampusJobFairsRoute({ searchParams }: { searchParams?: Promise<PortalRouteSearchParams> }) {
  const initialState = await loadEventChannelInitialState((await searchParams) ?? {}, {
    type_code: 'campus'
  });

  return (
    <PortalShell>
      <JobFairListPage
        initialState={initialState}
        topic={{
          eyebrow: 'Campus Job Fairs',
          title: '校园招聘会',
          description: '展示校园招聘会、双选会等公开信息。学生联系方式、报名名单、签到证据均不公开。',
          badge: '校园公开活动'
        }}
      />
    </PortalShell>
  );
}
