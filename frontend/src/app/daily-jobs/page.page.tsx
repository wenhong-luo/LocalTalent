import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { JobSearchPage } from '@/pages/portal/jobs/JobSearchPage';
import { loadJobSearchInitialState, type JobRouteSearchParams } from '@/pages/portal/jobs/jobPageState';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '今日招聘',
  description: 'LocalTalent 今日招聘专题页展示近期更新的公开职位。'
};

export default async function DailyJobsRoute({ searchParams }: { searchParams?: Promise<JobRouteSearchParams> }) {
  const initialState = await loadJobSearchInitialState((await searchParams) ?? {}, {
    updated_within: '1',
    sort: 'updated_desc'
  });

  return (
    <PortalShell>
      <JobSearchPage
        initialState={initialState}
        topic={{
          eyebrow: 'Daily Jobs',
          title: '今日招聘',
          subtitle: '展示近期更新的在线职位和认证企业职位。短信通知、微信提醒和外部推送均保持禁用占位。',
          badge: '近期更新'
        }}
      />
    </PortalShell>
  );
}
