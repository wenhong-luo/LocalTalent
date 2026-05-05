'use client';

import { type FormEvent, type ReactNode, useEffect, useState } from 'react';
import { StateView } from '@/components/StateView';
import { isHttpClientError } from '@/lib/httpClient';
import {
  cancelFavorite,
  cancelSubscription,
  createFavorite,
  createSubscription,
  fetchCandidateCenterOverview,
  fetchCandidateClosureData,
  markNotificationRead,
  readCandidateToken,
  revokeCandidateConsent,
  saveCandidateResume,
  submitCandidateConsent,
  type CandidateCenterOverview,
  type CandidateClosureData,
  type CandidatePublishStatus
} from './candidateCenterApi';

type CandidateCenterStatus = 'loading' | 'ready' | 'unauthorized' | 'error' | 'retrying';
type ClosureStatus = 'idle' | 'loading' | 'ready' | 'error';

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

const sectionTitleStyle = {
  margin: '0 0 14px',
  fontSize: '1.2rem',
  color: 'var(--lt-ink)'
};

const fieldStyle = {
  width: '100%',
  boxSizing: 'border-box' as const,
  border: '1px solid var(--lt-line)',
  borderRadius: '14px',
  padding: '11px 12px',
  background: '#ffffff',
  color: 'var(--lt-ink)'
};

const labelStyle = {
  display: 'grid',
  gap: '6px',
  color: 'var(--lt-ink-muted)',
  fontWeight: 800
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

const secondaryButtonStyle = {
  ...primaryButtonStyle,
  background: '#0f766e'
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

function formText(formData: FormData, name: string): string {
  const value = formData.get(name);
  return typeof value === 'string' ? value.trim() : '';
}

function formNumber(formData: FormData, name: string): number | null {
  const value = formText(formData, name);
  if (!value) {
    return null;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : null;
}

function formLines(formData: FormData, name: string): string[] {
  return formText(formData, name)
    .split(/\n|,/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function privateList<T>({
  title,
  items,
  empty,
  render
}: {
  title: string;
  items: T[];
  empty: string;
  render: (item: T) => ReactNode;
}) {
  return (
    <section style={cardStyle}>
      <h3 style={sectionTitleStyle}>{title}</h3>
      {items.length === 0 ? (
        <p style={{ margin: 0, color: 'var(--lt-ink-muted)' }}>{empty}</p>
      ) : (
        <div style={{ display: 'grid', gap: '12px' }}>{items.map(render)}</div>
      )}
    </section>
  );
}

type ClosurePanelProps = {
  overview: CandidateCenterOverview;
  closureData: CandidateClosureData | null;
  closureStatus: ClosureStatus;
  closureMessage?: string;
  onRetry: () => void;
  onResumeSave: (event: FormEvent<HTMLFormElement>) => void;
  onFavoriteCreate: (event: FormEvent<HTMLFormElement>) => void;
  onFavoriteCancel: (favoriteId: number) => void;
  onSubscriptionCreate: (event: FormEvent<HTMLFormElement>) => void;
  onSubscriptionCancel: (subscriptionId: number) => void;
  onNotificationRead: (notificationId: number) => void;
};

function CandidateClosurePanel({
  overview,
  closureData,
  closureStatus,
  closureMessage,
  onRetry,
  onResumeSave,
  onFavoriteCreate,
  onFavoriteCancel,
  onSubscriptionCreate,
  onSubscriptionCancel,
  onNotificationRead
}: ClosurePanelProps) {
  if (!overview.features.candidate_closure_enabled) {
    return (
      <section style={{ ...cardStyle, marginTop: '20px', borderStyle: 'dashed' }}>
        <h2 style={sectionTitleStyle}>三期求职者闭环未开启</h2>
        <p style={{ margin: 0, color: 'var(--lt-ink-muted)', lineHeight: 1.8 }}>
          服务端返回 candidate_closure_enabled=false，因此本页仅展示一期/二期概览能力。简历编辑、收藏、订阅和站内通知需要灰度开关开启后才可使用。
        </p>
      </section>
    );
  }

  if (closureStatus === 'loading' || !closureData) {
    return (
      <section style={{ marginTop: '20px' }}>
        <StateView
          variant={closureStatus === 'error' ? 'error' : 'loading'}
          title={closureStatus === 'error' ? '三期求职者闭环暂不可用' : '正在读取三期求职者闭环'}
          description={closureMessage ?? '正在从服务端读取简历、投递、收藏、订阅和站内通知。'}
          retryLabel="重新读取"
          onRetry={closureStatus === 'error' ? onRetry : undefined}
        />
      </section>
    );
  }

  const resume = closureData.resume;
  const preview = closureData.preview;

  return (
    <section aria-label="三期求职者真实闭环" style={{ marginTop: '20px', display: 'grid', gap: '18px' }}>
      <section aria-label="三期私有统计" style={gridStyle}>
        <SummaryCard
          title="职位收藏"
          value={`${overview.stats.favorite_count} 条`}
          helper="只保存公开可见职位，取消收藏后由服务端返回最新状态。"
        />
        <SummaryCard
          title="搜索订阅"
          value={`${overview.stats.subscription_count} 条`}
          helper="仅站内保存订阅条件，不触发短信、微信、小程序或 App 推送。"
        />
        <SummaryCard
          title="未读通知"
          value={`${overview.stats.unread_notification_count} 条`}
          helper="站内通知只在求职者本人中心展示，已读状态由服务端返回。"
        />
      </section>

      <section style={cardStyle}>
        <h2 style={sectionTitleStyle}>简历编辑</h2>
        <form
          aria-label="简历编辑表单"
          onSubmit={onResumeSave}
          style={{ display: 'grid', gap: '14px' }}
        >
          <label style={labelStyle}>
            简历名称
            <input name="resume_name" defaultValue={resume.resume_name} style={fieldStyle} />
          </label>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '12px' }}>
            <label style={labelStyle}>
              展示名
              <input name="display_name" defaultValue={resume.base_profile.display_name} style={fieldStyle} />
            </label>
            <label style={labelStyle}>
              城市
              <input name="city_code" defaultValue={resume.base_profile.city_code} style={fieldStyle} />
            </label>
            <label style={labelStyle}>
              职类
              <input name="category_code" defaultValue={resume.base_profile.category_code} style={fieldStyle} />
            </label>
            <label style={labelStyle}>
              经验年限
              <input
                name="experience_years"
                type="number"
                min="0"
                max="80"
                defaultValue={resume.base_profile.experience_years ?? ''}
                style={fieldStyle}
              />
            </label>
          </div>
          <label style={labelStyle}>
            个人摘要
            <textarea name="summary" defaultValue={resume.base_profile.summary} rows={3} style={fieldStyle} />
          </label>
          <label style={labelStyle}>
            技能标签（逗号或换行分隔）
            <textarea name="skills" defaultValue={resume.skills.join(', ')} rows={2} style={fieldStyle} />
          </label>
          <label style={labelStyle}>
            教育经历（每行一条）
            <textarea name="education" defaultValue={resume.education.join('\n')} rows={3} style={fieldStyle} />
          </label>
          <label style={labelStyle}>
            工作经历（每行一条）
            <textarea name="experience" defaultValue={resume.experience.join('\n')} rows={3} style={fieldStyle} />
          </label>
          <button type="submit" style={primaryButtonStyle}>
            保存简历并重新读取状态
          </button>
        </form>
      </section>

      <section style={cardStyle}>
        <h2 style={sectionTitleStyle}>简历预览</h2>
        <p style={{ margin: 0, color: 'var(--lt-ink-muted)', lineHeight: 1.8 }}>
          状态：{preview.resume_status}；完成度：{preview.completion_percent}%；附件：{preview.has_attachment ? '已存在附件' : '暂无附件'}。
        </p>
        <p style={{ margin: '10px 0 0', color: 'var(--lt-ink-muted)', lineHeight: 1.8 }}>
          展示名：{preview.base_profile.display_name || '待补充'}；技能：{preview.skills.join(' / ') || '待补充'}。
        </p>
      </section>

      {privateList({
        title: '投递记录',
        items: closureData.applications,
        empty: '暂无投递记录。',
        render: (item) => (
          <article key={item.application_id} style={{ borderBottom: '1px solid var(--lt-line)', paddingBottom: '10px' }}>
            <strong>{item.job_title}</strong>
            <p style={{ margin: '6px 0 0', color: 'var(--lt-ink-muted)' }}>
              {item.company_name} · {item.status_label} · {item.apply_time || '时间待同步'}
            </p>
          </article>
        )
      })}

      <section style={cardStyle}>
        <h3 style={sectionTitleStyle}>职位收藏</h3>
        <form aria-label="收藏职位表单" onSubmit={onFavoriteCreate} style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          <input name="job_id" type="number" min="1" placeholder="公开职位 ID" style={{ ...fieldStyle, maxWidth: '220px' }} />
          <button type="submit" style={secondaryButtonStyle}>收藏公开职位</button>
        </form>
        <div style={{ display: 'grid', gap: '12px', marginTop: '16px' }}>
          {closureData.favorites.length === 0 ? (
            <p style={{ margin: 0, color: 'var(--lt-ink-muted)' }}>暂无收藏职位。</p>
          ) : closureData.favorites.map((item) => (
            <article key={item.favorite_id} style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', borderTop: '1px solid var(--lt-line)', paddingTop: '12px' }}>
              <span>{item.job_title} · {item.company_name}</span>
              <button type="button" style={secondaryButtonStyle} onClick={() => onFavoriteCancel(item.favorite_id)}>
                取消收藏
              </button>
            </article>
          ))}
        </div>
      </section>

      <section style={cardStyle}>
        <h3 style={sectionTitleStyle}>搜索订阅</h3>
        <form aria-label="搜索订阅表单" onSubmit={onSubscriptionCreate} style={{ display: 'grid', gap: '10px' }}>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '10px' }}>
            <input name="subscription_name" placeholder="订阅名称" style={fieldStyle} />
            <input name="keyword" placeholder="关键词" style={fieldStyle} />
            <input name="city_code" placeholder="城市" style={fieldStyle} />
            <input name="category_code" placeholder="职类" style={fieldStyle} />
          </div>
          <button type="submit" style={secondaryButtonStyle}>保存站内订阅</button>
        </form>
        <div style={{ display: 'grid', gap: '12px', marginTop: '16px' }}>
          {closureData.subscriptions.length === 0 ? (
            <p style={{ margin: 0, color: 'var(--lt-ink-muted)' }}>暂无搜索订阅。</p>
          ) : closureData.subscriptions.map((item) => (
            <article key={item.subscription_id} style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', borderTop: '1px solid var(--lt-line)', paddingTop: '12px' }}>
              <span>{item.subscription_name} · {item.keyword || '不限关键词'} · {item.subscription_status}</span>
              <button type="button" style={secondaryButtonStyle} onClick={() => onSubscriptionCancel(item.subscription_id)}>
                取消订阅
              </button>
            </article>
          ))}
        </div>
      </section>

      <section style={cardStyle}>
        <h3 style={sectionTitleStyle}>站内通知</h3>
        <div style={{ display: 'grid', gap: '12px' }}>
          {closureData.notifications.length === 0 ? (
            <p style={{ margin: 0, color: 'var(--lt-ink-muted)' }}>暂无站内通知。</p>
          ) : closureData.notifications.map((item) => (
            <article key={item.notification_id} style={{ display: 'flex', justifyContent: 'space-between', gap: '12px', borderTop: '1px solid var(--lt-line)', paddingTop: '12px' }}>
              <span>{item.title} · {item.content_summary || '无摘要'} · {item.read_status}</span>
              {item.read_status === 'unread' ? (
                <button type="button" style={secondaryButtonStyle} onClick={() => onNotificationRead(item.notification_id)}>
                  标记已读
                </button>
              ) : null}
            </article>
          ))}
        </div>
      </section>
    </section>
  );
}

export function CandidateCenter() {
  const [status, setStatus] = useState<CandidateCenterStatus>('loading');
  const [overview, setOverview] = useState<CandidateCenterOverview | null>(null);
  const [closureData, setClosureData] = useState<CandidateClosureData | null>(null);
  const [closureStatus, setClosureStatus] = useState<ClosureStatus>('idle');
  const [closureMessage, setClosureMessage] = useState<string>();
  const [token, setToken] = useState<string | null>(null);
  const [message, setMessage] = useState<string>();
  const [traceId, setTraceId] = useState<string>();

  async function reloadClosure(activeToken = token) {
    if (!activeToken) {
      setClosureStatus('error');
      setClosureMessage('请先登录求职者账号后再读取三期求职者闭环。');
      return;
    }

    setClosureStatus('loading');
    setClosureMessage(undefined);
    try {
      const data = await fetchCandidateClosureData(activeToken);
      setClosureData(data);
      setClosureStatus('ready');
    } catch (error) {
      setTraceId(isHttpClientError(error) ? error.traceId : undefined);
      setClosureMessage(error instanceof Error ? error.message : '三期求职者闭环暂时不可用。');
      setClosureStatus('error');
    }
  }

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

      if (result.data.features.candidate_closure_enabled) {
        await reloadClosure(activeToken);
      } else {
        setClosureData(null);
        setClosureStatus('idle');
        setClosureMessage(undefined);
      }
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

  async function runCandidateWrite(operation: () => Promise<unknown>) {
    if (!token) {
      setStatus('unauthorized');
      setMessage('请先登录求职者账号后再操作。');
      return;
    }

    setClosureStatus('loading');
    setClosureMessage('正在提交，并重新读取服务端状态。');
    try {
      await operation();
      await loadOverview('retrying', token);
    } catch (error) {
      setTraceId(isHttpClientError(error) ? error.traceId : undefined);
      setClosureMessage(error instanceof Error ? error.message : '操作失败，请稍后重试。');
      setClosureStatus(isHttpClientError(error) && error.kind === 'unauthorized' ? 'error' : 'error');
    }
  }

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

  function onResumeSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    void runCandidateWrite(() => saveCandidateResume(token ?? '', {
      resume_name: formText(formData, 'resume_name'),
      base_profile: {
        display_name: formText(formData, 'display_name'),
        city_code: formText(formData, 'city_code'),
        category_code: formText(formData, 'category_code'),
        experience_years: formNumber(formData, 'experience_years'),
        summary: formText(formData, 'summary')
      },
      education: formLines(formData, 'education'),
      experience: formLines(formData, 'experience'),
      skills: formLines(formData, 'skills')
    }));
  }

  function onFavoriteCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const jobId = formNumber(new FormData(event.currentTarget), 'job_id');
    if (!jobId) {
      setClosureStatus('error');
      setClosureMessage('请输入公开职位 ID。');
      return;
    }
    void runCandidateWrite(() => createFavorite(token ?? '', jobId));
  }

  function onSubscriptionCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    void runCandidateWrite(() => createSubscription(token ?? '', {
      subscription_name: formText(formData, 'subscription_name'),
      keyword: formText(formData, 'keyword'),
      city_code: formText(formData, 'city_code'),
      category_code: formText(formData, 'category_code')
    }));
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

              <CandidateClosurePanel
                overview={overview}
                closureData={closureData}
                closureStatus={closureStatus}
                closureMessage={closureMessage}
                onRetry={() => void reloadClosure()}
                onResumeSave={onResumeSave}
                onFavoriteCreate={onFavoriteCreate}
                onFavoriteCancel={(favoriteId) => void runCandidateWrite(() => cancelFavorite(token ?? '', favoriteId))}
                onSubscriptionCreate={onSubscriptionCreate}
                onSubscriptionCancel={(subscriptionId) => void runCandidateWrite(() => cancelSubscription(token ?? '', subscriptionId))}
                onNotificationRead={(notificationId) => void runCandidateWrite(() => markNotificationRead(token ?? '', notificationId))}
              />
            </>
          )}
        </section>
      </div>
    </main>
  );
}
