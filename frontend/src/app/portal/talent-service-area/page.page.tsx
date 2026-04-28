import type { Metadata } from 'next';
import { isHttpClientError } from '@/lib/httpClient';
import { TalentServiceArea, type TalentServiceAreaInitialState } from '@/pages/portal/TalentServiceArea';
import { fetchTalentSnapshots, type TalentSnapshotQuery } from '@/pages/portal/talentSnapshotApi';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '人才服务区',
  description: 'LocalTalent 人才服务区仅展示候选人发布快照，不展示原始候选人数据。'
};

type SearchParams = Record<string, string | string[] | undefined>;

function firstParam(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

function toPositiveInt(value: string | undefined, fallback: number): number {
  const parsed = Number.parseInt(value ?? '', 10);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}

function buildQuery(searchParams: SearchParams): TalentSnapshotQuery {
  return {
    city_code: firstParam(searchParams.city_code),
    category_code: firstParam(searchParams.category_code),
    page: toPositiveInt(firstParam(searchParams.page), 1),
    size: toPositiveInt(firstParam(searchParams.size), 12)
  };
}

export default async function TalentServiceAreaPage({
  searchParams
}: {
  searchParams?: Promise<SearchParams>;
}) {
  const query = buildQuery((await searchParams) ?? {});
  let initialState: TalentServiceAreaInitialState;

  try {
    const result = await fetchTalentSnapshots(query);
    initialState = {
      status: result.data.snapshot_list.length > 0 ? 'ready' : 'empty',
      query,
      page: result.data,
      traceId: result.traceId
    };
  } catch (error) {
    initialState = {
      status: isHttpClientError(error) && error.kind === 'unauthorized' ? 'unauthorized' : 'error',
      query,
      message: error instanceof Error ? error.message : '人才服务区暂时不可用，请稍后重试。'
    };
  }

  return <TalentServiceArea initialState={initialState} />;
}
