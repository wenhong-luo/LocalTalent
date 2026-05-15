'use client';

import { FormEvent, useState, useTransition } from 'react';
import Link from 'next/link';
import { isHttpClientError } from '@/lib/httpClient';
import { PortalAdRailFrame } from '@/components/portal/PortalAdRailFrame';
import { StateView, type StateVariant } from '@/components/StateView';
import {
  fetchTalentSnapshots,
  type TalentSnapshot,
  type TalentSnapshotPage,
  type TalentSnapshotQuery
} from './talentSnapshotApi';
import styles from './TalentServiceArea.module.css';

type TalentServiceAreaStatus = StateVariant | 'ready';

export type TalentServiceAreaInitialState = {
  status: TalentServiceAreaStatus;
  query: TalentSnapshotQuery;
  page?: TalentSnapshotPage;
  message?: string;
  traceId?: string;
};

type TalentServiceAreaProps = {
  initialState: TalentServiceAreaInitialState;
};

const cityOptions = [
  ['310000', '上海'],
  ['110000', '北京'],
  ['320900', '盐城'],
  ['140100', '太原']
];

const categoryOptions = [
  ['software', '互联网/软件'],
  ['operations', '运营/市场'],
  ['sales', '销售/客服'],
  ['manufacturing', '生产/物流'],
  ['general-worker', '普工招聘']
];

const updatedOptions = [
  ['3', '近 3 天'],
  ['7', '近 7 天'],
  ['30', '近 30 天']
];

const sortOptions = [
  ['updated_desc', '最近更新'],
  ['experience_desc', '经验从高到低'],
  ['experience_asc', '经验从低到高']
];

function stateVariant(status: TalentServiceAreaStatus): StateVariant {
  return status === 'ready' ? 'loading' : status;
}

function stateTitle(status: TalentServiceAreaStatus): string {
  if (status === 'unauthorized') {
    return '暂无权限读取人才服务区';
  }

  if (status === 'retrying') {
    return '正在重新读取发布快照';
  }

  if (status === 'empty') {
    return '暂无可展示发布快照';
  }

  if (status === 'error') {
    return '人才服务区暂时不可用';
  }

  return '正在读取人才服务区';
}

function initialLetter(name: string): string {
  return (name.trim().charAt(0) || '才').toUpperCase();
}

function queryHref(query: TalentSnapshotQuery): string {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (typeof value !== 'undefined' && String(value).trim() !== '') {
      params.set(key, String(value));
    }
  }
  return `/portal/talent-service-area?${params.toString()}`;
}

function SnapshotCard({ snapshot }: { snapshot: TalentSnapshot }) {
  return (
    <article className={styles.snapshotCard}>
      <span className={styles.avatar} aria-hidden="true">
        {initialLetter(snapshot.display_name_masked)}
      </span>
      <div>
        <p className={styles.eyebrow}>发布快照 #{snapshot.snapshot_id}</p>
        <h2 className={styles.snapshotTitle}>{snapshot.display_name_masked || '匿名候选人'}</h2>
        <p className={styles.summary}>{snapshot.skills_summary || '技能摘要待补充'}</p>
        <div className={styles.tagRow}>
          <span className={styles.tag}>{snapshot.city_code || '不限地区'}</span>
          <span className={styles.tag}>{snapshot.category_code || '未分类'}</span>
          <span className={styles.tag}>白名单展示</span>
          <span className={styles.tag}>已同意发布</span>
        </div>
      </div>
      <div className={styles.metaBox}>
        <span className={styles.metaValue}>
          {snapshot.experience_years === null ? '-' : snapshot.experience_years}
        </span>
        <span>经验年限</span>
        <small>{snapshot.updated_at || '更新时间未知'}</small>
      </div>
    </article>
  );
}

function BoundarySideBar() {
  return (
    <aside className={styles.sideBar} aria-label="人才服务区边界提示">
      <section className={`${styles.card} ${styles.sideCard}`}>
        <h2 className={styles.sectionTitle}>发布快照边界</h2>
        <ul className={styles.sideList}>
          <li>唯一公开数据源为候选人发布快照。</li>
          <li>撤回或下线后不会出现在本页。</li>
          <li>公开层只渲染字段白名单。</li>
        </ul>
      </section>
      <section className={`${styles.card} ${styles.sideCard}`}>
        <h2 className={styles.sectionTitle}>高风险能力</h2>
        <button className={styles.disabledAction} type="button" disabled aria-disabled="true">
          详情读取：不开放
        </button>
        <button className={styles.disabledAction} type="button" disabled aria-disabled="true">
          联系方式：不开放
        </button>
      </section>
    </aside>
  );
}

export function TalentServiceArea({ initialState }: TalentServiceAreaProps) {
  const [status, setStatus] = useState<TalentServiceAreaStatus>(initialState.status);
  const [page, setPage] = useState<TalentSnapshotPage | undefined>(initialState.page);
  const [message, setMessage] = useState(initialState.message);
  const [traceId, setTraceId] = useState(initialState.traceId);
  const [cityCode, setCityCode] = useState(initialState.query.city_code ?? '');
  const [categoryCode, setCategoryCode] = useState(initialState.query.category_code ?? '');
  const [experienceMin, setExperienceMin] = useState(String(initialState.query.experience_min ?? ''));
  const [experienceMax, setExperienceMax] = useState(String(initialState.query.experience_max ?? ''));
  const [updatedWithin, setUpdatedWithin] = useState(initialState.query.updated_within ?? '');
  const [sort, setSort] = useState(initialState.query.sort ?? 'updated_desc');
  const [isPending, startTransition] = useTransition();

  function buildQuery(nextPage = 1): TalentSnapshotQuery {
    return {
      city_code: cityCode.trim() || undefined,
      category_code: categoryCode.trim() || undefined,
      experience_min: experienceMin.trim() || undefined,
      experience_max: experienceMax.trim() || undefined,
      updated_within: updatedWithin || undefined,
      sort: sort || 'updated_desc',
      page: nextPage,
      size: initialState.query.size ?? 12
    };
  }

  async function loadSnapshots(nextPage = 1) {
    const query = buildQuery(nextPage);
    setStatus('retrying');
    setMessage(undefined);

    try {
      const result = await fetchTalentSnapshots(query);
      setPage(result.data);
      setTraceId(result.traceId);
      setStatus(result.data.snapshot_list.length > 0 ? 'ready' : 'empty');
      window.history.replaceState(null, '', queryHref(query));
    } catch (error) {
      setTraceId(isHttpClientError(error) ? error.traceId : undefined);
      setMessage(error instanceof Error ? error.message : '人才服务区暂时不可用，请稍后重试。');
      setStatus(isHttpClientError(error) && error.kind === 'unauthorized' ? 'unauthorized' : 'error');
    }
  }

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    startTransition(() => {
      void loadSnapshots(1);
    });
  }

  const effectiveStatus = isPending ? 'retrying' : status;
  const snapshots = page?.snapshot_list ?? [];
  const currentPage = page?.page ?? initialState.query.page ?? 1;
  const total = page?.total ?? 0;
  const size = page?.size ?? initialState.query.size ?? 12;
  const hasPrevious = currentPage > 1;
  const hasNext = currentPage * size < total;

  return (
    <main className={styles.page} data-layout="portal-ad-rails" data-testid="talent-service-area-page">
      <PortalAdRailFrame label="人才服务区">
        <section className={styles.hero} aria-label="人才服务区首屏">
          <div>
            <p className={styles.eyebrow}>LocalTalent Snapshot Area</p>
            <h1 className={styles.title}>人才服务区：只展示候选人授权发布的快照摘要</h1>
            <p className={styles.subtitle}>
              本页参考目标站列表体验，但能力口径全部改写为发布快照展示。公开页面不提供详情读取、联系方式或高风险检索入口。
            </p>
          </div>
          <div className={styles.heroPanel}>
            <span className={styles.heroNumber}>{total}</span>
            <strong>当前可展示快照</strong>
            <span>SSR 首屏读取 · 可分享筛选</span>
          </div>
        </section>

        <form className={`${styles.card} ${styles.filters}`} aria-label="人才服务区筛选" onSubmit={onSubmit}>
          <label className={styles.filterLabel}>
            城市
            <select className={styles.select} value={cityCode} onChange={(event) => setCityCode(event.target.value)}>
              <option value="">不限城市</option>
              {cityOptions.map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <label className={styles.filterLabel}>
            职类
            <select className={styles.select} value={categoryCode} onChange={(event) => setCategoryCode(event.target.value)}>
              <option value="">不限职类</option>
              {categoryOptions.map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <label className={styles.filterLabel}>
            经验下限
            <input
              className={styles.input}
              inputMode="numeric"
              value={experienceMin}
              onChange={(event) => setExperienceMin(event.target.value)}
              placeholder="例如 3"
            />
          </label>
          <label className={styles.filterLabel}>
            经验上限
            <input
              className={styles.input}
              inputMode="numeric"
              value={experienceMax}
              onChange={(event) => setExperienceMax(event.target.value)}
              placeholder="例如 8"
            />
          </label>
          <label className={styles.filterLabel}>
            更新时间
            <select className={styles.select} value={updatedWithin} onChange={(event) => setUpdatedWithin(event.target.value)}>
              <option value="">不限时间</option>
              {updatedOptions.map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <label className={styles.filterLabel}>
            排序
            <select className={styles.select} value={sort} onChange={(event) => setSort(event.target.value)}>
              {sortOptions.map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <button className={styles.searchButton} type="submit">
            查询发布快照
          </button>
        </form>

        <section className={styles.contentGrid}>
          <section className={`${styles.card} ${styles.listCard}`} aria-label="发布快照列表">
            <div className={styles.sectionHeader}>
              <div>
                <h2 className={styles.sectionTitle}>发布快照列表</h2>
                <p className={styles.muted}>共 {total} 条，当前第 {currentPage} 页。{traceId ? ` trace_id：${traceId}` : ''}</p>
              </div>
              <span className={styles.tag}>字段白名单</span>
            </div>

            {effectiveStatus === 'ready' && snapshots.length > 0 ? (
              <>
                <div className={styles.snapshotList}>
                  {snapshots.map((snapshot) => (
                    <SnapshotCard key={snapshot.snapshot_id} snapshot={snapshot} />
                  ))}
                </div>
                <nav className={styles.pagination} aria-label="发布快照分页">
                  {hasPrevious ? (
                    <Link className={styles.pageButton} href={queryHref(buildQuery(currentPage - 1))}>
                      上一页
                    </Link>
                  ) : (
                    <span className={styles.pageButtonDisabled}>上一页</span>
                  )}
                  {hasNext ? (
                    <Link className={styles.pageButton} href={queryHref(buildQuery(currentPage + 1))}>
                      下一页
                    </Link>
                  ) : (
                    <span className={styles.pageButtonDisabled}>下一页</span>
                  )}
                </nav>
              </>
            ) : (
              <div className={styles.emptyWrap}>
                <StateView
                  variant={stateVariant(effectiveStatus)}
                  title={stateTitle(effectiveStatus)}
                  description={message ?? '请调整筛选条件或稍后重试。'}
                  retryLabel="重新读取"
                  onRetry={effectiveStatus === 'retrying' ? undefined : () => loadSnapshots(currentPage)}
                />
              </div>
            )}
          </section>
          <BoundarySideBar />
        </section>
      </PortalAdRailFrame>
    </main>
  );
}
