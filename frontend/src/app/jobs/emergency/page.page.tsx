import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { JobSearchPage } from '@/pages/portal/jobs/JobSearchPage';
import { loadJobSearchInitialState, type JobRouteSearchParams } from '@/pages/portal/jobs/jobPageState';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '急聘岗位',
  description: 'LocalTalent 急聘岗位专题页复用公开职位列表，不引入付费置顶或会员推广。'
};

export default async function EmergencyJobsRoute({ searchParams }: { searchParams?: Promise<JobRouteSearchParams> }) {
  const initialState = await loadJobSearchInitialState((await searchParams) ?? {}, {
    updated_within: '7',
    sort: 'updated_desc'
  });

  return (
    <PortalShell>
      <JobSearchPage
        initialState={initialState}
        topic={{
          eyebrow: 'Emergency Jobs',
          title: '急聘 / 快速招聘',
          subtitle: '当前按近期更新的公开职位提供专题入口；不接会员置顶、付费推广、真实地图或外部招聘服务。',
          badge: '快速入口占位'
        }}
      />
    </PortalShell>
  );
}
