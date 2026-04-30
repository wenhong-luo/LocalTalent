import { isHttpClientError } from '@/lib/httpClient';
import { normalizeJobSearchParams, type JobSearchInitialState } from './JobSearchPage';
import { fetchPortalJobs, type JobSearchQuery } from './portalJobApi';

export type JobRouteSearchParams = Record<string, string | string[] | undefined>;

export async function loadJobSearchInitialState(
  searchParams: JobRouteSearchParams,
  defaults: Partial<JobSearchQuery> = {}
): Promise<JobSearchInitialState> {
  const incoming = normalizeJobSearchParams(searchParams);
  const query: JobSearchQuery = { ...defaults };
  for (const [key, value] of Object.entries(incoming) as [keyof JobSearchQuery, JobSearchQuery[keyof JobSearchQuery]][]) {
    if (typeof value !== 'undefined' && String(value).trim() !== '') {
      query[key] = value as never;
    }
  }

  try {
    const result = await fetchPortalJobs(query);
    return {
      status: result.data.job_list.length > 0 ? 'ready' : 'empty',
      query,
      page: result.data,
      traceId: result.traceId
    };
  } catch (error) {
    return {
      status: 'error',
      query,
      message: error instanceof Error ? error.message : '职位列表暂时不可用，请稍后重试。',
      traceId: isHttpClientError(error) ? error.traceId : undefined
    };
  }
}
