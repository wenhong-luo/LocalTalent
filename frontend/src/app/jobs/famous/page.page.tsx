import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { JobSearchPage } from '@/pages/portal/jobs/JobSearchPage';
import { loadJobSearchInitialState, type JobRouteSearchParams } from '@/pages/portal/jobs/jobPageState';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '名企直聘',
  description: 'LocalTalent 名企直聘专题页仅展示在线职位和认证企业职位，不展示企业内部联系方式。'
};

export default async function FamousJobsRoute({ searchParams }: { searchParams?: Promise<JobRouteSearchParams> }) {
  const initialState = await loadJobSearchInitialState((await searchParams) ?? {}, {
    famous: '1',
    sort: 'updated_desc'
  });

  return (
    <PortalShell>
      <JobSearchPage
        initialState={initialState}
        topic={{
          eyebrow: 'Famous Employers',
          title: '名企直聘',
          subtitle: '聚合公开门户中的认证企业职位入口。会员置顶、付费推广和企业内部联系方式均不在本页实现。',
          badge: '认证企业职位'
        }}
      />
    </PortalShell>
  );
}
