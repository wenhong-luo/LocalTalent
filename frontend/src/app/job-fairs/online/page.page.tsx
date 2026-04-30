import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { JobFairListPage } from '@/pages/portal/content/PortalContentPages';
import { loadEventChannelInitialState, type PortalRouteSearchParams } from '@/pages/portal/content/portalContentPageState';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '网络招聘会',
  description: 'LocalTalent 网络招聘会只展示公开活动信息，不接直播、视频或在线互动。'
};

export default async function OnlineJobFairsRoute({ searchParams }: { searchParams?: Promise<PortalRouteSearchParams> }) {
  const initialState = await loadEventChannelInitialState((await searchParams) ?? {}, {
    type_code: 'online'
  });

  return (
    <PortalShell>
      <JobFairListPage
        initialState={initialState}
        topic={{
          eyebrow: 'Online Job Fairs',
          title: '网络招聘会',
          description: '展示网络招聘会公开信息。直播、视频、在线互动、报名名单和联系方式均不在公开层实现。',
          badge: '网络招聘会公开频道'
        }}
      />
    </PortalShell>
  );
}
