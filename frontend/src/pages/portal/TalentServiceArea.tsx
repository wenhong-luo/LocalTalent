'use client';

import { FormEvent, useState, useTransition } from 'react';
import { isHttpClientError } from '@/lib/httpClient';
import { StateView, type StateVariant } from '@/components/StateView';
import {
  fetchTalentSnapshots,
  type TalentSnapshot,
  type TalentSnapshotPage,
  type TalentSnapshotQuery
} from './talentSnapshotApi';

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

const shellStyle = {
  minHeight: '100vh',
  padding: '32px 18px 56px'
};

const frameStyle = {
  maxWidth: '1180px',
  margin: '0 auto'
};

const heroStyle = {
  border: '1px solid var(--lt-line)',
  borderRadius: '34px',
  background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(239, 251, 247, 0.8))',
  boxShadow: 'var(--lt-shadow)',
  padding: '36px',
  overflow: 'hidden'
};

const formStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
  gap: '14px',
  marginTop: '26px'
};

const inputStyle = {
  width: '100%',
  border: '1px solid var(--lt-line)',
  borderRadius: '18px',
  padding: '13px 14px',
  background: '#ffffff',
  color: 'var(--lt-ink)'
};

const buttonStyle = {
  border: 'none',
  borderRadius: '18px',
  padding: '13px 18px',
  background: 'var(--lt-accent)',
  color: '#ffffff',
  cursor: 'pointer',
  fontWeight: 800
};

const gridStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
  gap: '18px',
  marginTop: '24px'
};

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

function SnapshotCard({ snapshot }: { snapshot: TalentSnapshot }) {
  return (
    <article
      style={{
        border: '1px solid var(--lt-line)',
        borderRadius: '26px',
        background: 'var(--lt-card)',
        padding: '22px',
        boxShadow: '0 18px 42px rgba(20, 33, 61, 0.08)'
      }}
    >
      <p style={{ margin: 0, color: 'var(--lt-accent-strong)', fontWeight: 800 }}>
        发布快照 #{snapshot.snapshot_id}
      </p>
      <h2 style={{ margin: '12px 0 10px', fontSize: '1.5rem' }}>{snapshot.display_name_masked || '匿名候选人'}</h2>
      <p style={{ margin: 0, color: 'var(--lt-ink-muted)', lineHeight: 1.7 }}>{snapshot.skills_summary || '技能摘要待补充'}</p>
      <dl style={{ display: 'grid', gap: '10px', margin: '18px 0 0' }}>
        <div>
          <dt style={{ color: 'var(--lt-ink-muted)', fontSize: '0.86rem' }}>城市</dt>
          <dd style={{ margin: '4px 0 0' }}>{snapshot.city_code || '不限'}</dd>
        </div>
        <div>
          <dt style={{ color: 'var(--lt-ink-muted)', fontSize: '0.86rem' }}>方向</dt>
          <dd style={{ margin: '4px 0 0' }}>{snapshot.category_code || '未分类'}</dd>
        </div>
        <div>
          <dt style={{ color: 'var(--lt-ink-muted)', fontSize: '0.86rem' }}>经验年限</dt>
          <dd style={{ margin: '4px 0 0' }}>
            {snapshot.experience_years === null ? '未填写' : `${snapshot.experience_years} 年`}
          </dd>
        </div>
        <div>
          <dt style={{ color: 'var(--lt-ink-muted)', fontSize: '0.86rem' }}>更新时间</dt>
          <dd style={{ margin: '4px 0 0' }}>{snapshot.updated_at || '未知'}</dd>
        </div>
      </dl>
    </article>
  );
}

export function TalentServiceArea({ initialState }: TalentServiceAreaProps) {
  const [status, setStatus] = useState<TalentServiceAreaStatus>(initialState.status);
  const [page, setPage] = useState<TalentSnapshotPage | undefined>(initialState.page);
  const [message, setMessage] = useState(initialState.message);
  const [traceId, setTraceId] = useState(initialState.traceId);
  const [cityCode, setCityCode] = useState(initialState.query.city_code ?? '');
  const [categoryCode, setCategoryCode] = useState(initialState.query.category_code ?? '');
  const [isPending, startTransition] = useTransition();

  const query: TalentSnapshotQuery = {
    city_code: cityCode.trim() || undefined,
    category_code: categoryCode.trim() || undefined,
    page: 1,
    size: initialState.query.size ?? 12
  };

  async function loadSnapshots() {
    setStatus('retrying');
    setMessage(undefined);

    try {
      const result = await fetchTalentSnapshots(query);
      setPage(result.data);
      setTraceId(result.traceId);
      setStatus(result.data.snapshot_list.length > 0 ? 'ready' : 'empty');
      window.history.replaceState(null, '', `/portal/talent-service-area?${new URLSearchParams({
        ...(query.city_code ? { city_code: query.city_code } : {}),
        ...(query.category_code ? { category_code: query.category_code } : {}),
        page: '1',
        size: String(query.size ?? 12)
      }).toString()}`);
    } catch (error) {
      setTraceId(isHttpClientError(error) ? error.traceId : undefined);
      setMessage(error instanceof Error ? error.message : '人才服务区暂时不可用，请稍后重试。');
      setStatus(isHttpClientError(error) && error.kind === 'unauthorized' ? 'unauthorized' : 'error');
    }
  }

  function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    startTransition(() => {
      void loadSnapshots();
    });
  }

  const effectiveStatus = isPending ? 'retrying' : status;

  return (
    <main style={shellStyle}>
      <div style={frameStyle}>
        <section style={heroStyle}>
          <p style={{ margin: 0, color: 'var(--lt-accent-strong)', fontWeight: 900 }}>人才服务区</p>
          <h1 style={{ maxWidth: '760px', margin: '14px 0', fontSize: 'clamp(2.2rem, 5vw, 4.5rem)', lineHeight: 1 }}>
            只展示发布快照，不做公共简历库。
          </h1>
          <p style={{ maxWidth: '760px', margin: 0, color: 'var(--lt-ink-muted)', lineHeight: 1.9 }}>
            本页仅渲染发布快照字段白名单。手机号、邮箱、简历正文、附件、证据材料等原始候选人数据不会进入展示层。
          </p>

          <form aria-label="人才服务区筛选" style={formStyle} onSubmit={onSubmit}>
            <label>
              <span style={{ display: 'block', marginBottom: '8px', color: 'var(--lt-ink-muted)' }}>城市代码</span>
              <input
                value={cityCode}
                onChange={(event) => setCityCode(event.target.value)}
                placeholder="例如 310000"
                style={inputStyle}
              />
            </label>
            <label>
              <span style={{ display: 'block', marginBottom: '8px', color: 'var(--lt-ink-muted)' }}>岗位方向</span>
              <input
                value={categoryCode}
                onChange={(event) => setCategoryCode(event.target.value)}
                placeholder="例如 software"
                style={inputStyle}
              />
            </label>
            <label style={{ alignSelf: 'end' }}>
              <span style={{ display: 'block', marginBottom: '8px', visibility: 'hidden' }}>查询</span>
              <button type="submit" style={buttonStyle}>
                查询发布快照
              </button>
            </label>
          </form>
        </section>

        <section aria-label="发布快照列表" style={{ marginTop: '24px' }}>
          {effectiveStatus === 'ready' && page ? (
            <>
              <div style={{ color: 'var(--lt-ink-muted)' }}>
                共 {page.total} 条发布快照，当前第 {page.page} 页
                {traceId ? <span>，trace_id：{traceId}</span> : null}
              </div>
              <div style={gridStyle}>
                {page.snapshot_list.map((snapshot) => (
                  <SnapshotCard key={snapshot.snapshot_id} snapshot={snapshot} />
                ))}
              </div>
            </>
          ) : (
            <StateView
              variant={stateVariant(effectiveStatus)}
              title={stateTitle(effectiveStatus)}
              description={message ?? '请调整筛选条件或稍后重试。'}
              retryLabel="重新读取"
              onRetry={effectiveStatus === 'retrying' ? undefined : loadSnapshots}
            />
          )}
        </section>
      </div>
    </main>
  );
}
