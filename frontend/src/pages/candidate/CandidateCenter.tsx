'use client';

import Link from 'next/link';
import { type ChangeEvent, type FormEvent, type ReactNode, useEffect, useState } from 'react';
import { StateView } from '@/components/StateView';
import { isHttpClientError } from '@/lib/httpClient';
import styles from './CandidateCenter.module.css';
import { ExpectedPositionPicker } from '@/components/selectors/ExpectedPositionPicker';
import { ExpectedRegionPicker } from '@/components/selectors/RegionCascadePicker';
import { categoryCodeForExpectedPosition } from '@/shared/catalogs/positionCatalog';
import { cityCodeForExpectedRegion } from '@/shared/catalogs/regionCatalog';
import {
  applyCandidateResumeAiSuggestion,
  cancelFavorite,
  cancelSubscription,
  createFavorite,
  createSubscription,
  deleteCandidateResumeAttachment,
  dismissCandidateResumeAiSuggestion,
  downloadCandidateResumeAttachment,
  fetchCandidateCenterOverview,
  fetchCandidateClosureData,
  fetchCandidateResumeAiSuggestions,
  fetchCandidateResumeAttachment,
  generateCandidateResumeAiSuggestions,
  markNotificationRead,
  readCandidateToken,
  revokeCandidateConsent,
  saveCandidateResume,
  submitCandidateConsent,
  uploadCandidateResumeAttachment,
  type CandidateCenterOverview,
  type CandidateClosureData,
  type CandidatePublishStatus,
  type CandidateResumeAiSuggestionTask,
  type CandidateResumeAttachment
} from './candidateCenterApi';

type CandidateCenterStatus = 'loading' | 'ready' | 'unauthorized' | 'error' | 'retrying';
type ClosureStatus = 'idle' | 'loading' | 'ready' | 'error';

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

function maskPhone(value: string): string {
  const digits = value.replace(/\D/g, '');
  if (digits.length < 7) {
    return value ? '已绑定联系方式' : '未绑定联系方式';
  }
  return `${digits.slice(0, 3)}****${digits.slice(-4)}`;
}

function profileName(closureData: CandidateClosureData | null): string {
  return closureData?.resume.base_profile.display_name || closureData?.resume.resume_name || '求职者';
}

function profileMeta(closureData: CandidateClosureData | null): string[] {
  const profile = closureData?.resume.base_profile;
  const items = [
    profile?.gender,
    profile?.highest_education,
    profile?.experience_years != null ? `${profile.experience_years}年经验` : undefined
  ].filter((item): item is string => Boolean(item));

  return items.length > 0 ? items : ['资料待完善'];
}

function expectedText(values: string[], fallback: string): string {
  return values.length > 0 ? values.join('、') : fallback;
}

function latestUpdate(overview: CandidateCenterOverview, closureData: CandidateClosureData | null): string {
  return closureData?.resume.updated_at || overview.resume.updated_at || '待同步';
}

function MemberUtilityBar() {
  return (
    <div className={styles.utilityBar}>
      <div className={styles.utilityInner}>
        <nav aria-label="求职者快捷导航" className={styles.utilityLinks}>
          <Link href="/">网站首页</Link>
          <Link href="/jobs">搜职位</Link>
          <Link href="/portal/talent-service-area">人才服务区</Link>
          <Link href="/help">使用帮助</Link>
        </nav>
        <div className={styles.utilityLinks} aria-label="求职者账号入口">
          <span>欢迎回来</span>
          <Link href="/candidate/center">会员中心</Link>
          <Link href="/">返回首页</Link>
        </div>
      </div>
    </div>
  );
}

function MemberSearchHeader() {
  return (
    <header className={styles.searchHeader}>
      <Link href="/" className={styles.brand} aria-label="LocalTalent 首页">
        <span className={styles.brandMark}>LT</span>
        <span>
          <strong>LocalTalent</strong>
          <small>地方人才网</small>
        </span>
      </Link>
      <form className={styles.searchBox} action="/jobs">
        <select name="scope" aria-label="搜索范围" defaultValue="jobs">
          <option value="jobs">职位信息</option>
        </select>
        <input name="keyword" placeholder="请输入搜索关键字" />
        <button type="submit">搜索</button>
      </form>
      <div className={styles.headerActions}>
        <span aria-label="通知消息">通知消息</span>
        <Link href="/">返回首页</Link>
      </div>
    </header>
  );
}

function MemberProfileHero({
  overview,
  closureData
}: {
  overview: CandidateCenterOverview;
  closureData: CandidateClosureData | null;
}) {
  const resume = closureData?.resume;
  const profile = resume?.base_profile;
  const interviewCount = closureData?.applications.filter((item) => item.status_label.includes('面试')).length ?? 0;
  const stats = [
    { label: '我的足迹', value: '0' },
    { label: '对我感兴趣', value: '0' },
    { label: '面试邀请', value: String(interviewCount) },
    { label: '我的投递', value: String(overview.applications.total) }
  ];

  return (
    <section className={styles.profileHero} aria-label="求职者个人信息横幅">
      <div className={styles.profileInner}>
        <div className={styles.avatarCard} aria-hidden="true">
          <span>{profileName(closureData).slice(0, 1)}</span>
        </div>
        <div className={styles.profileCopy}>
          <p className={styles.kicker}>求职者个人中心</p>
          <h1>{profileName(closureData)}</h1>
          <p>{profileMeta(closureData).join(' | ')}</p>
          <p className={styles.phoneLine}>{maskPhone(profile?.contact_phone ?? '')}</p>
        </div>
        <div className={styles.heroStats} aria-label="求职者服务统计">
          {stats.map((item) => (
            <article key={item.label}>
              <strong>{item.value}</strong>
              <span>{item.label}</span>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

function CandidateSideNav({ overview }: { overview: CandidateCenterOverview }) {
  const items = [
    ['会员首页', '#member-home'],
    ['我的简历', '#resume-card'],
    ['我的职聊', '#service-chat'],
    ['求职管理', '#candidate-closure'],
    ['收藏关注', '#candidate-closure'],
    ['智能推荐', '#job-recommendations'],
    ['增值服务', '#preferred-services'],
    ['我的积分', '#preferred-services'],
    ['隐私设置', '#resume-card'],
    ['账号管理', '#member-home']
  ] as const;

  return (
    <aside className={styles.sideColumn} aria-label="求职者中心菜单">
      <div className={styles.pointsCard}>
        <span className={styles.trophy} aria-hidden="true">★</span>
        <strong>{Math.max(overview.resume.completion_percent, 0)}</strong>
        <span>积分</span>
        <em>未签到</em>
      </div>
      <nav className={styles.sideNav}>
        {items.map(([label, href]) => (
          <a href={href} key={label}>{label}</a>
        ))}
      </nav>
      <div className={styles.qrCard}>
        <div className={styles.qrBox} aria-hidden="true">QR</div>
        <strong>手机找工作</strong>
        <span>微信/小程序入口暂未开放</span>
      </div>
    </aside>
  );
}

function ResumeStatusCard({
  overview,
  closureData,
  copy,
  onConsent,
  onRevoke
}: {
  overview: CandidateCenterOverview;
  closureData: CandidateClosureData | null;
  copy: ReturnType<typeof statusCopy> | null;
  onConsent: () => void;
  onRevoke: () => void;
}) {
  const resume = closureData?.resume;
  const profile = resume?.base_profile;
  const completion = resume?.completion_percent ?? overview.resume.completion_percent;
  const publishStatus = overview.consent.publish_status === 'consented' ? '已公开' : '未公开';

  return (
    <section id="resume-card" className={styles.dashboardCard} aria-label="我的简历">
      <div className={styles.cardHeader}>
        <div>
          <h2>我的简历 <Link href="/candidate/resume/create">预览简历</Link></h2>
          <p>更新时间：{latestUpdate(overview, closureData)}</p>
        </div>
        <div className={styles.actionGroup}>
          <Link href="/candidate/resume/create" className={styles.blueButton}>修改简历</Link>
          <button type="button" className={styles.blueButton}>刷新简历</button>
          {overview.consent.publish_status === 'consented' ? (
            <button type="button" className={styles.outlineButton} onClick={onRevoke}>撤回同意</button>
          ) : overview.consent.publish_status === 'not_publishable' ? (
            <button type="button" className={styles.disabledButton} disabled>暂不可同意发布</button>
          ) : (
            <button type="button" className={styles.outlineButton} onClick={onConsent}>重新同意发布</button>
          )}
        </div>
      </div>

      <div className={styles.resumeGrid}>
        <div className={styles.resumeFacts}>
          <p>期望职位：{expectedText(profile?.expected_positions ?? [], profile?.category_code || '待补充')}</p>
          <p>期望地区：{expectedText(profile?.expected_cities ?? [], profile?.city_code || '待补充')}</p>
          <p>期望薪资：{profile?.expected_salary || '待补充'}</p>
          <p>求职状态：{profile?.job_status || '待补充'}</p>
        </div>
        <div className={styles.resumeStamp}>
          <span>{publishStatus}</span>
        </div>
      </div>

      <div className={styles.completionRow}>
        <span>简历完整度：</span>
        <div className={styles.progressTrack}>
          <div style={{ width: `${Math.min(Math.max(completion, 0), 100)}%` }} />
        </div>
        <strong>{completion}% 完成</strong>
        {completion < 80 ? (
          <Link href="/candidate/resume/create">立即完善</Link>
        ) : (
          <span>状态良好</span>
        )}
      </div>

      <div className={styles.noticeLine}>
        <span>{copy?.title}</span>
        <small>{copy?.description}</small>
      </div>
    </section>
  );
}

function PreferredServices({ overview }: { overview: CandidateCenterOverview }) {
  const services = [
    { title: '简历置顶', desc: '灰度占位，暂未开放', status: '暂未开放', tone: 'gold' },
    { title: '醒目标签', desc: '灰度占位，不接商业化标签', status: '暂未开放', tone: 'green' },
    { title: '职位订阅', desc: `${overview.stats.subscription_count} 条订阅，站内保存条件`, status: '可用', tone: 'blue' },
    { title: '委托投递', desc: '灰度占位，不做自动投递', status: '暂未开放', tone: 'orange' },
    { title: '隐私设置', desc: overview.consent.status_label || '由服务端返回发布状态', status: '可查看', tone: 'purple' },
    { title: '我的积分', desc: '仅展示占位，不接积分兑换', status: '占位', tone: 'teal' }
  ];

  return (
    <section id="preferred-services" className={styles.dashboardCard} aria-label="优选服务">
      <h2>优选服务</h2>
      <div className={styles.serviceGrid}>
        {services.map((item) => (
          <article key={item.title} className={styles.serviceCard} data-tone={item.tone}>
            <span aria-hidden="true">{item.title.slice(0, 1)}</span>
            <strong>{item.title}</strong>
            <p>{item.desc}</p>
            <em>{item.status}</em>
          </article>
        ))}
      </div>
    </section>
  );
}

function JobRecommendations({ closureData }: { closureData: CandidateClosureData | null }) {
  const recommended = closureData?.favorites.slice(0, 3) ?? [];

  return (
    <section id="job-recommendations" className={styles.dashboardCard} aria-label="职位推荐">
      <div className={styles.cardHeader}>
        <h2>职位推荐</h2>
        <span>仅展示公开职位白名单字段</span>
      </div>
      {recommended.length === 0 ? (
        <div className={styles.emptyState}>
          <span aria-hidden="true">▦</span>
          <p>没有数据了</p>
          <small>后续可由公开职位接口补充推荐，不读取原始候选人数据。</small>
        </div>
      ) : (
        <div className={styles.recommendationList}>
          {recommended.map((item) => (
            <Link href={`/jobs/${item.job_id}`} key={item.favorite_id}>
              <strong>{item.job_title}</strong>
              <span>{item.company_name} · {item.city_code || '城市待同步'}</span>
            </Link>
          ))}
        </div>
      )}
    </section>
  );
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
  attachment?: CandidateResumeAttachment;
  aiSuggestions?: CandidateResumeAiSuggestionTask;
  aiBusy: boolean;
  onAttachmentUpload: (event: ChangeEvent<HTMLInputElement>) => void;
  onAttachmentDownload: () => void;
  onAttachmentDelete: () => void;
  onAiGenerate: () => void;
  onAiApply: (suggestionId: number) => void;
  onAiDismiss: (suggestionId: number) => void;
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
  onNotificationRead,
  attachment,
  aiSuggestions,
  aiBusy,
  onAttachmentUpload,
  onAttachmentDownload,
  onAttachmentDelete,
  onAiGenerate,
  onAiApply,
  onAiDismiss
}: ClosurePanelProps) {
  const activeResume = closureData?.resume;
  const [selectedExpectedPositions, setSelectedExpectedPositions] = useState<string[]>([]);
  const [selectedExpectedCategoryCode, setSelectedExpectedCategoryCode] = useState('');
  const [selectedExpectedRegions, setSelectedExpectedRegions] = useState<string[]>([]);
  const [selectedExpectedCityCode, setSelectedExpectedCityCode] = useState('');

  useEffect(() => {
    if (!activeResume) {
      return;
    }
    setSelectedExpectedPositions(activeResume.base_profile.expected_positions ?? []);
    setSelectedExpectedCategoryCode(activeResume.base_profile.category_code ?? '');
    setSelectedExpectedRegions(activeResume.base_profile.expected_cities ?? []);
    setSelectedExpectedCityCode(activeResume.base_profile.city_code ?? '');
  }, [
    activeResume?.resume_id,
    activeResume?.base_profile.category_code,
    activeResume?.base_profile.city_code,
    (activeResume?.base_profile.expected_positions ?? []).join('|'),
    (activeResume?.base_profile.expected_cities ?? []).join('|')
  ]);

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
              期望地区
              <ExpectedRegionPicker
                selectedRegions={selectedExpectedRegions}
                selectedCityCode={selectedExpectedCityCode}
                onSave={({ regions, cityCode }) => {
                  setSelectedExpectedRegions(regions);
                  setSelectedExpectedCityCode(cityCode);
                }}
              />
              <input type="hidden" name="city_code" value={selectedExpectedCityCode} />
              <input type="hidden" name="expected_cities" value={selectedExpectedRegions.join('，')} />
            </label>
            <label style={labelStyle}>
              期望职位
              <ExpectedPositionPicker
                selectedPositions={selectedExpectedPositions}
                selectedCategoryCode={selectedExpectedCategoryCode}
                onSave={({ positions, categoryCode }) => {
                  setSelectedExpectedPositions(positions);
                  setSelectedExpectedCategoryCode(categoryCode);
                }}
              />
              <input type="hidden" name="category_code" value={selectedExpectedCategoryCode} />
              <input type="hidden" name="expected_positions" value={selectedExpectedPositions.join('，')} />
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
        {overview.features.resume_attachment_upload_enabled ? (
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '10px', marginTop: '16px' }}>
            <span style={{ color: 'var(--lt-ink-muted)', fontWeight: 800 }}>
              附件：{attachment?.has_attachment ? `${attachment.file_name || '附件简历'} · ${attachment.size_bytes ?? 0} bytes` : '暂无附件'}
            </span>
            <label style={secondaryButtonStyle}>
              {attachment?.has_attachment ? '替换附件' : '上传附件'}
              <input
                aria-label="求职者中心上传附件简历"
                type="file"
                accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                style={{ display: 'none' }}
                onChange={onAttachmentUpload}
              />
            </label>
            {attachment?.has_attachment ? (
              <>
                <button type="button" style={secondaryButtonStyle} onClick={onAttachmentDownload}>下载附件</button>
                <button type="button" style={primaryButtonStyle} onClick={onAttachmentDelete}>删除附件</button>
              </>
            ) : null}
          </div>
        ) : (
          <p style={{ margin: '14px 0 0', color: 'var(--lt-ink-muted)' }}>
            附件上传开关未开启，当前仅展示附件状态占位。
          </p>
        )}
      </section>

      <section style={cardStyle} aria-label="智能优化建议">
        <h2 style={sectionTitleStyle}>智能优化建议（安全规则版）</h2>
        <p style={{ margin: 0, color: 'var(--lt-ink-muted)', lineHeight: 1.8 }}>
          只在求职者本人私有域内基于服务端规范化简历生成规则建议；不调用外部模型，不上传原始候选人数据，建议需手动逐条应用。
        </p>
        {overview.features.resume_ai_assist_enabled ? (
          <>
            <button
              type="button"
              style={{ ...primaryButtonStyle, marginTop: '16px' }}
              disabled={aiBusy}
              onClick={onAiGenerate}
            >
              {aiBusy ? '正在处理建议' : '生成优化建议'}
            </button>
            <div style={{ display: 'grid', gap: '12px', marginTop: '16px' }}>
              {(aiSuggestions?.items ?? []).length === 0 ? (
                <p style={{ margin: 0, color: 'var(--lt-ink-muted)' }}>
                  暂无优化建议。补充工作经历、自我描述或求职意向后可重新生成。
                </p>
              ) : aiSuggestions?.items.map((item) => (
                <article
                  key={item.suggestion_id}
                  style={{
                    border: '1px solid var(--lt-line)',
                    borderRadius: '18px',
                    padding: '16px',
                    background: '#ffffff'
                  }}
                >
                  <strong>{item.title}</strong>
                  <p style={{ margin: '8px 0 0', color: 'var(--lt-ink-muted)', lineHeight: 1.7 }}>
                    {item.reason_summary}
                  </p>
                  {item.before_preview ? (
                    <p style={{ margin: '8px 0 0', color: 'var(--lt-ink-muted)' }}>当前：{item.before_preview}</p>
                  ) : null}
                  {item.suggested_value ? (
                    <p style={{ margin: '8px 0 0', color: 'var(--lt-ink)' }}>建议：{item.suggested_value}</p>
                  ) : null}
                  <div style={{ display: 'flex', flexWrap: 'wrap', gap: '10px', marginTop: '12px' }}>
                    <button
                      type="button"
                      style={secondaryButtonStyle}
                      disabled={!item.can_apply || item.apply_status !== 'pending' || aiBusy}
                      onClick={() => onAiApply(item.suggestion_id)}
                    >
                      手动应用
                    </button>
                    <button
                      type="button"
                      style={primaryButtonStyle}
                      disabled={item.apply_status !== 'pending' || aiBusy}
                      onClick={() => onAiDismiss(item.suggestion_id)}
                    >
                      忽略
                    </button>
                    <span style={{ color: 'var(--lt-ink-muted)', alignSelf: 'center' }}>
                      状态：{item.apply_status}
                    </span>
                  </div>
                </article>
              ))}
            </div>
          </>
        ) : (
          <p style={{ margin: '14px 0 0', color: 'var(--lt-ink-muted)' }}>
            AI 优化建议开关未开启，当前仅展示安全占位；不会调用外部模型。
          </p>
        )}
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
  const [attachment, setAttachment] = useState<CandidateResumeAttachment>();
  const [aiSuggestions, setAiSuggestions] = useState<CandidateResumeAiSuggestionTask>();
  const [aiBusy, setAiBusy] = useState(false);
  const [token, setToken] = useState<string | null>(null);
  const [message, setMessage] = useState<string>();
  const [traceId, setTraceId] = useState<string>();

  async function reloadClosure(
    activeToken = token,
    attachmentUploadEnabled = overview?.features.resume_attachment_upload_enabled ?? false,
    aiAssistEnabled = overview?.features.resume_ai_assist_enabled ?? false
  ) {
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
      if (attachmentUploadEnabled) {
        const attachmentResult = await fetchCandidateResumeAttachment(activeToken);
        setAttachment(attachmentResult.data);
      } else {
        setAttachment(undefined);
      }
      if (aiAssistEnabled) {
        const aiResult = await fetchCandidateResumeAiSuggestions(activeToken).catch(() => undefined);
        setAiSuggestions(aiResult?.data);
      } else {
        setAiSuggestions(undefined);
      }
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
        await reloadClosure(
          activeToken,
          result.data.features.resume_attachment_upload_enabled,
          result.data.features.resume_ai_assist_enabled
        );
      } else {
        setClosureData(null);
        setAiSuggestions(undefined);
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
    const currentResume = closureData?.resume;
    const expectedPositions = formLines(formData, 'expected_positions');
    const categoryCode = formText(formData, 'category_code')
      || categoryCodeForExpectedPosition(expectedPositions[0] ?? '')
      || currentResume?.base_profile.category_code
      || '';
    const expectedCities = formLines(formData, 'expected_cities');
    const cityCode = formText(formData, 'city_code')
      || cityCodeForExpectedRegion(expectedCities[0] ?? '')
      || currentResume?.base_profile.city_code
      || '';
    void runCandidateWrite(() => saveCandidateResume(token ?? '', {
      resume_name: formText(formData, 'resume_name'),
      base_profile: {
        display_name: formText(formData, 'display_name'),
        city_code: cityCode,
        category_code: categoryCode,
        experience_years: formNumber(formData, 'experience_years'),
        summary: formText(formData, 'summary'),
        gender: currentResume?.base_profile.gender ?? '',
        birth_date: currentResume?.base_profile.birth_date ?? '',
        highest_education: currentResume?.base_profile.highest_education ?? '',
        start_work_date: currentResume?.base_profile.start_work_date ?? '',
        no_experience: currentResume?.base_profile.no_experience ?? false,
        contact_phone: currentResume?.base_profile.contact_phone ?? '',
        contact_wechat: currentResume?.base_profile.contact_wechat ?? '',
        wechat_same_as_phone: currentResume?.base_profile.wechat_same_as_phone ?? false,
        expected_positions: expectedPositions,
        expected_salary: currentResume?.base_profile.expected_salary ?? '',
        expected_cities: expectedCities,
        job_status: currentResume?.base_profile.job_status ?? ''
      },
      education: formLines(formData, 'education'),
      experience: formLines(formData, 'experience'),
      skills: formLines(formData, 'skills'),
      work_experience: [],
      education_experience: [],
      self_description: formText(formData, 'summary')
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

  function onAttachmentUpload(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) {
      return;
    }
    void runCandidateWrite(async () => {
      const result = await uploadCandidateResumeAttachment(token ?? '', file);
      setAttachment(result.data);
    });
  }

  function onAttachmentDownload() {
    if (!token) {
      setStatus('unauthorized');
      setMessage('请先登录求职者账号后再下载附件。');
      return;
    }
    void downloadCandidateResumeAttachment(token).then((blob) => {
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = attachment?.file_name || 'resume-attachment';
      link.click();
      URL.revokeObjectURL(url);
    }).catch((error) => {
      setClosureStatus('error');
      setClosureMessage(error instanceof Error ? error.message : '附件下载失败，请稍后重试。');
    });
  }

  function onAttachmentDelete() {
    void runCandidateWrite(async () => {
      const result = await deleteCandidateResumeAttachment(token ?? '');
      setAttachment(result.data);
    });
  }

  function onAiGenerate() {
    setAiBusy(true);
    void runCandidateWrite(async () => {
      const result = await generateCandidateResumeAiSuggestions(token ?? '');
      setAiSuggestions(result.data);
    }).finally(() => setAiBusy(false));
  }

  function onAiApply(suggestionId: number) {
    setAiBusy(true);
    void runCandidateWrite(async () => {
      const result = await applyCandidateResumeAiSuggestion(token ?? '', suggestionId);
      setAiSuggestions(result.data);
    }).finally(() => setAiBusy(false));
  }

  function onAiDismiss(suggestionId: number) {
    setAiBusy(true);
    void runCandidateWrite(async () => {
      const result = await dismissCandidateResumeAiSuggestion(token ?? '', suggestionId);
      setAiSuggestions(result.data);
    }).finally(() => setAiBusy(false));
  }

  const showState = status !== 'ready' || !overview;
  const copy = overview ? statusCopy(overview.consent.publish_status, overview.consent.reason) : null;

  return (
    <main aria-label="求职者中心" className={styles.memberShell}>
      <MemberUtilityBar />
      <MemberSearchHeader />

      {showState ? (
        <section className={styles.memberFrame}>
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
        </section>
      ) : (
        <>
          <MemberProfileHero overview={overview} closureData={closureData} />
          <section id="member-home" className={styles.memberFrame}>
            {overview.onboarding?.onboarding_required ? (
              <article className={styles.onboardingAlert}>
                <div>
                  <strong>首次登录资料待完善</strong>
                  <p>服务端判断当前简历尚未完成。请先完成基本信息和简历详情，再进入完整个人中心体验。</p>
                </div>
                <Link href="/candidate/resume/create">去完善简历</Link>
              </article>
            ) : null}

            <div className={styles.dashboardLayout}>
              <CandidateSideNav overview={overview} />
              <div className={styles.mainColumn}>
                <ResumeStatusCard
                  overview={overview}
                  closureData={closureData}
                  copy={copy}
                  onConsent={onConsent}
                  onRevoke={onRevoke}
                />
                {traceId ? (
                  <p className={styles.traceLine}>trace_id：{traceId}</p>
                ) : null}
                <PreferredServices overview={overview} />
                <JobRecommendations closureData={closureData} />
                <section id="candidate-closure" className={styles.detailSection}>
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
                    attachment={attachment}
                    aiSuggestions={aiSuggestions}
                    aiBusy={aiBusy}
                    onAttachmentUpload={onAttachmentUpload}
                    onAttachmentDownload={onAttachmentDownload}
                    onAttachmentDelete={onAttachmentDelete}
                    onAiGenerate={onAiGenerate}
                    onAiApply={onAiApply}
                    onAiDismiss={onAiDismiss}
                  />
                </section>
              </div>
            </div>
          </section>
          <button type="button" className={styles.floatChat} aria-label="在线客服占位">
            …
          </button>
          <footer className={styles.memberFooter}>
            <nav aria-label="会员中心页脚">
              <Link href="/jobs">找工作</Link>
              <Link href="/companies">找企业</Link>
              <Link href="/job-fairs">招聘会</Link>
              <Link href="/help">帮助中心</Link>
            </nav>
            <p>LocalTalent 求职者个人中心 · 私有页面 noindex · 不接真实微信/小程序/App</p>
          </footer>
        </>
      )}
    </main>
  );
}
