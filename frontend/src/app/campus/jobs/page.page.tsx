import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { JobSearchPage } from '@/pages/portal/jobs/JobSearchPage';
import { loadJobSearchInitialState, type JobRouteSearchParams } from '@/pages/portal/jobs/jobPageState';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '校园职位',
  description: 'LocalTalent 校园职位入口复用公开职位列表，不展示个人联系方式或原始简历。'
};

export default async function CampusJobsRoute({ searchParams }: { searchParams?: Promise<JobRouteSearchParams> }) {
  const initialState = await loadJobSearchInitialState((await searchParams) ?? {}, {
    sort: 'updated_desc'
  });

  return (
    <PortalShell>
      <JobSearchPage
        initialState={initialState}
        topic={{
          eyebrow: 'Campus Jobs',
          title: '校园职位',
          subtitle: '校园职位先作为公开职位入口承接；结构化校招字段、报名、短信和微信通知均留到后续评估。',
          badge: '校园入口'
        }}
      />
    </PortalShell>
  );
}
