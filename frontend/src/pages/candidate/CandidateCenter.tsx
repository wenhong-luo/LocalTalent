'use client';

import { useEffect, useState } from 'react';
import { StateView } from '@/components/StateView';
import { isHttpClientError } from '@/lib/httpClient';
import {
  fetchCandidateCenterOverview,
  readCandidateToken,
  revokeCandidateConsent,
  submitCandidateConsent,
  type CandidateCenterOverview,
  type CandidatePublishStatus
} from './candidateCenterApi';

type CandidateCenterStatus = 'loading' | 'ready' | 'unauthorized' | 'error' | 'retrying';

const shellStyle = {
  minHeight: '100vh',
  padding: '32px 18px 56px',
  background: 'linear-gradient(140deg, #f8fbf8 0%, #eef8f4 45%, #f7f1e4 100%)'
};

const frameStyle = {
  maxWidth: '1120px',
  margin: '0 auto'
};

const heroStyle = {
  border: '1px solid var(--lt-line)',
  borderRadius: '34px',
  background: 'rgba(255, 255, 255, 0.92)',
  boxShadow: 'var(--lt-shadow)',
  padding: '34px'
};

const gridStyle = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(230px, 1fr))',
  gap: '16px',
  marginTop: '20px'
};

const cardStyle = {
  border: '1px solid var(--lt-line)',
  borderRadius: '24px',
  background: 'var(--lt-card)',
  padding: '22px',
  boxShadow: '0 16px 38px rgba(20, 33, 61, 0.07)'
};

const primaryButtonStyle = {
  border: 'none',
  borderRadius: '999px',
  padding: '12px 18px',
  background: 'var(--lt-accent)',
  color: '#ffffff',
  cursor: 'pointer',
  fontWeight: 800
};

const mutedButtonStyle = {
  ...primaryButtonStyle,
  background: '#6b7280',
  cursor: 'not-allowed'
};

function statusCopy(status: CandidatePublishStatus, reason: string) {
  switch (status) {
    case 'consented':
      return {
        title: '已同意，发布快照可见',
        description: '状态来源：接口返回 publish_status=consented。撤回后将联动发布快照下线。'
      };
    case 'revoked':
      return {
        title: '已撤回并下线',
        description: '状态来源：接口返回 publish_status=revoked。人才服务区不再展示你的发布快照。'
      };
    case 'not_publishable':
      return {
        title: '暂不可发布',
        description: reason || '状态来源：接口返回 publish_status=not_publishable，请先完成必要资料或核验。'
      };
    case 'unauthorized':
      return {
        title: '无权限查看',
        description: '状态来源：接口返回 publish_status=unauthorized。'
      };
    case 'unavailable':
      return {
        title: '发布状态暂不可用',
        description: reason || '状态来源：接口返回 publish_status=unavailable，请稍后重试。'
      };
  }
}

function SummaryCard({ title, value, helper }: { title: string; value: string; helper: string }) {
  return (
    <article style={cardStyle}>
      <p style={{ margin: 0, color: 'var(--lt-ink-muted)', fontWeight: 700 }}>{title}</p>
      <h2 style={{ margin: '12px 0 8px', fontSize: '1.35rem' }}>{value}</h2>
      <p style={{ margin: 0, color: 'var(--lt-ink-muted)', lineHeight: 1.7 }}>{helper}</p>
    </article>
  );
}

export function CandidateCenter() {
  const [status, setStatus] = useState<CandidateCenterStatus>('loading');
  const [overview, setOverview] = useState<CandidateCenterOverview | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [message, setMessage] = useState<string>();
  const [traceId, setTraceId] = useState<string>();

  async function loadOverview(nextStatus: CandidateCenterStatus = 'retrying', activeToken = token) {
    if (!activeToken) {
      setStatus('unauthorized');
      setMessage('请先登录求职者账号后再访问求职者中心。');
      return;
    }

    setStatus(nextStatus);
    setMessage(undefined);

    try {
      const result = await fetchCandidateCenterOverview(activeToken);
      setOverview(result.data);
      setTraceId(result.traceId);
      setStatus(result.data.consent.publish_status === 'unauthorized' ? 'unauthorized' : 'ready');
      setMessage(
        result.data.consent.publish_status === 'unauthorized'
          ? '当前账号无权限读取求职者中心。'
          : undefined
      );
    } catch (error) {
      setTraceId(isHttpClientError(error) ? error.traceId : undefined);
      setMessage(error instanceof Error ? error.message : '求职者中心暂时不可用，请稍后重试。');
      setStatus(isHttpClientError(error) && error.kind === 'unauthorized' ? 'unauthorized' : 'error');
    }
  }

  useEffect(() => {
    const storedToken = readCandidateToken();
    setToken(storedToken);
    void loadOverview('loading', storedToken);
  }, []);

  async function onConsent() {
    if (!token) {
      setStatus('unauthorized');
      setMessage('请先登录求职者账号后再提交同意。');
      return;
    }

    setStatus('retrying');
    setMessage('正在提交同意，并重新读取服务端状态。');
    try {
      await submitCandidateConsent(token);
      await loadOverview('retrying', token);
    } catch (error) {
      setTraceId(isHttpClientError(error) ? error.traceId : undefined);
      setMessage(error instanceof Error ? error.message : '提交同意失败，请稍后重试。');
      setStatus(isHttpClientError(error) && error.kind === 'unauthorized' ? 'unauthorized' : 'error');
    }
  }

  async function onRevoke() {
    if (!token || !overview?.consent.consent_id) {
      setStatus('unauthorized');
      setMessage('缺少可撤回的同意记录，请重新登录后刷新。');
      return;
    }

    setStatus('retrying');
    setMessage('正在撤回同意，并重新读取服务端状态。');
    try {
      await revokeCandidateConsent(token, overview.consent.consent_id);
      await loadOverview('retrying', token);
    } catch (error) {
      setTraceId(isHttpClientError(error) ? error.traceId : undefined);
      setMessage(error instanceof Error ? error.message : '撤回同意失败，请稍后重试。');
      setStatus(isHttpClientError(error) && error.kind === 'unauthorized' ? 'unauthorized' : 'error');
    }
  }

  const showState = status !== 'ready' || !overview;
  const copy = overview ? statusCopy(overview.consent.publish_status, overview.consent.reason) : null;

  return (
    <main aria-label="求职者中心" style={shellStyle}>
      <div style={frameStyle}>
        <section style={heroStyle}>
          <p style={{ margin: 0, color: 'var(--lt-accent-strong)', fontWeight: 900 }}>求职者中心</p>
          <h1 style={{ maxWidth: '760px', margin: '14px 0', fontSize: 'clamp(2.1rem, 5vw, 4.2rem)', lineHeight: 1 }}>
            简历、投递、签到、同意与撤回，一处看清。
          </h1>
          <p style={{ maxWidth: '760px', margin: 0, color: 'var(--lt-ink-muted)', lineHeight: 1.9 }}>
            本页状态完全以接口返回为准，不在前端推断同意状态；手机号、邮箱、简历正文、附件、证据材料不会进入本页展示。
          </p>
        </section>

        <section style={{ marginTop: '24px' }}>
          {showState ? (
            <StateView
              variant={status === 'ready' ? 'loading' : status}
              title={
                status === 'unauthorized'
                  ? '无权限访问求职者中心'
                  : status === 'error'
                    ? '求职者中心暂时不可用'
                    : status === 'retrying'
                      ? '正在重新读取求职者中心'
                      : '正在读取求职者中心'
              }
              description={message ?? '请稍候，系统正在读取最新状态。'}
              retryLabel="重新读取"
              onRetry={status === 'loading' || status === 'retrying' ? undefined : () => loadOverview('retrying')}
            />
          ) : (
            <>
              <article style={{ ...cardStyle, borderColor: 'rgba(15, 118, 110, 0.35)' }}>
                <p style={{ margin: 0, color: 'var(--lt-accent-strong)', fontWeight: 900 }}>
                  同意与撤回状态
                </p>
                <h2 style={{ margin: '12px 0 8px', fontSize: '1.55rem' }}>{copy?.title}</h2>
                <p style={{ margin: 0, color: 'var(--lt-ink-muted)', lineHeight: 1.8 }}>{copy?.description}</p>
                {overview.consent.status_label ? (
                  <p style={{ margin: '12px 0 0', color: 'var(--lt-ink-muted)' }}>
                    服务端状态：{overview.consent.status_label}
                  </p>
                ) : null}
                {traceId ? (
                  <p style={{ margin: '8px 0 0', color: 'var(--lt-ink-muted)' }}>trace_id：{traceId}</p>
                ) : null}
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px', marginTop: '20px' }}>
                  {overview.consent.publish_status === 'consented' ? (
                    <button type="button" style={primaryButtonStyle} onClick={onRevoke}>
                      撤回同意
                    </button>
                  ) : null}
                  {overview.consent.publish_status === 'revoked' || overview.consent.publish_status === 'unavailable' ? (
                    <button type="button" style={primaryButtonStyle} onClick={onConsent}>
                      重新同意发布
                    </button>
                  ) : null}
                  {overview.consent.publish_status === 'not_publishable' ? (
                    <button type="button" style={mutedButtonStyle} disabled>
                      暂不可同意发布
                    </button>
                  ) : null}
                </div>
              </article>

              <section aria-label="求职者摘要" style={gridStyle}>
                <SummaryCard
                  title="简历"
                  value={`${overview.resume.completion_percent}% 完成`}
                  helper={`技能摘要：${overview.resume.skills_summary || '待补充'}；更新时间：${overview.resume.updated_at || '未知'}`}
                />
                <SummaryCard
                  title="投递"
                  value={`${overview.applications.total} 条投递`}
                  helper={`最近职位：${overview.applications.latest_job_title}；状态：${overview.applications.latest_status}`}
                />
                <SummaryCard
                  title="签到"
                  value={overview.signin.latest_status}
                  helper={`最近签到时间：${overview.signin.latest_time || '暂无'}`}
                />
              </section>
            </>
          )}
        </section>
      </div>
    </main>
  );
}
