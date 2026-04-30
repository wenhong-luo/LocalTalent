import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { isHttpClientError } from '@/lib/httpClient';
import {
  CompanySearchPage,
  normalizeCompanySearchParams,
  type CompanySearchInitialState
} from '@/pages/portal/companies/CompanySearchPage';
import { fetchPortalCompanies } from '@/pages/portal/companies/portalCompanyApi';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '找企业',
  description: 'LocalTalent 找企业频道仅展示认证企业公开字段和在线职位聚合。'
};

type SearchParams = Record<string, string | string[] | undefined>;

export default async function CompaniesPage({ searchParams }: { searchParams?: Promise<SearchParams> }) {
  const query = normalizeCompanySearchParams((await searchParams) ?? {});
  let initialState: CompanySearchInitialState;

  try {
    const result = await fetchPortalCompanies(query);
    initialState = {
      status: result.data.company_list.length > 0 ? 'ready' : 'empty',
      query,
      page: result.data,
      traceId: result.traceId
    };
  } catch (error) {
    initialState = {
      status: 'error',
      query,
      message: error instanceof Error ? error.message : '企业列表暂时不可用，请稍后重试。',
      traceId: isHttpClientError(error) ? error.traceId : undefined
    };
  }

  return (
    <PortalShell>
      <CompanySearchPage initialState={initialState} />
    </PortalShell>
  );
}
