import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { CampusLandingPage } from '@/pages/portal/content/CampusLandingPage';
import { fetchPortalContents, fetchPortalEvents } from '@/pages/portal/content/portalContentApi';
import { fetchPortalJobs } from '@/pages/portal/jobs/portalJobApi';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '校园招聘',
  description: 'LocalTalent 校园招聘公开门户，聚合校园职位入口、校园招聘会和校园资讯。'
};

export default async function CampusRoute() {
  const [jobsResult, eventsResult, contentsResult] = await Promise.allSettled([
    fetchPortalJobs({ sort: 'updated_desc', page: 1, size: 6 }),
    fetchPortalEvents({ type_code: 'campus', page: 1, size: 6 }),
    fetchPortalContents({ content_type: 'campus', page: 1, size: 6 })
  ]);

  return (
    <PortalShell>
      <CampusLandingPage
        jobs={jobsResult.status === 'fulfilled' ? jobsResult.value.data : undefined}
        events={eventsResult.status === 'fulfilled' ? eventsResult.value.data : undefined}
        contents={contentsResult.status === 'fulfilled' ? contentsResult.value.data : undefined}
      />
    </PortalShell>
  );
}
