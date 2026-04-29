import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { isHttpClientError } from '@/lib/httpClient';
import {
  JobSearchPage,
  normalizeJobSearchParams,
  type JobSearchInitialState
} from '@/pages/portal/jobs/JobSearchPage';
import { fetchPortalJobs } from '@/pages/portal/jobs/portalJobApi';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '找工作',
  description: 'LocalTalent 找工作频道仅展示在线职位和认证企业职位，筛选 query 稳定可分享。'
};

type SearchParams = Record<string, string | string[] | undefined>;

export default async function JobsPage({ searchParams }: { searchParams?: Promise<SearchParams> }) {
  const query = normalizeJobSearchParams((await searchParams) ?? {});
  let initialState: JobSearchInitialState;

  try {
    const result = await fetchPortalJobs(query);
    initialState = {
      status: result.data.job_list.length > 0 ? 'ready' : 'empty',
      query,
      page: result.data,
      traceId: result.traceId
    };
  } catch (error) {
    initialState = {
      status: 'error',
      query,
      message: error instanceof Error ? error.message : '职位列表暂时不可用，请稍后重试。',
      traceId: isHttpClientError(error) ? error.traceId : undefined
    };
  }

  return (
    <PortalShell>
      <JobSearchPage initialState={initialState} />
    </PortalShell>
  );
}
