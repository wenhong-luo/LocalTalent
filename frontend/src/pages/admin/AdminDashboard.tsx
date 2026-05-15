'use client';

import { FormEvent, useEffect, useState } from 'react';
import { ApprovalDialog, type ApprovalDecision } from '@/components/backoffice/ApprovalDialog';
import { ReviewTable } from '@/components/backoffice/ReviewTable';
import { RouteGuard, type GuardContext } from '@/components/backoffice/RouteGuard';
import { StatusBadge } from '@/components/backoffice/StatusBadge';
import { StateView } from '@/components/StateView';
import { isHttpClientError } from '@/lib/httpClient';
import {
  createHomeSlot,
  createRecommendation,
  deleteHomeSlotImage,
  fetchAuditTrace,
  fetchCompanyReviewQueue,
  fetchExportReviewQueue,
  fetchHomeSlotImageBlob,
  fetchHomeSlots,
  fetchJobReviewQueue,
  fetchOpsOverview,
  fetchRecommendations,
  fetchRiskReviews,
  handleRiskReview,
  offlineHomeSlot,
  offlineRecommendation,
  reviewCompany,
  reviewExport,
  reviewJob,
  uploadHomeSlotImage,
  type AuditTraceSummary,
  type CompanyReviewItem,
  type HomeSlotItem,
  type JobReviewItem,
  type OpsOverview,
  type RecommendationItem,
  type RiskReviewItem
} from './adminApi';
import { type CompanyExportApply } from '@/pages/company/companyApi';

const cardStyle = {
  border: '1px solid var(--lt-line)',
  borderRadius: '24px',
  background: 'var(--lt-card)',
  padding: '22px',
  boxShadow: '0 16px 36px rgba(20, 33, 61, 0.08)'
};

const inputStyle = {
  width: '100%',
  border: '1px solid var(--lt-line)',
  borderRadius: '14px',
  padding: '11px 12px',
  background: '#ffffff'
};

const buttonStyle = {
  border: 'none',
  borderRadius: '999px',
  padding: '10px 16px',
  background: 'var(--lt-accent)',
  color: '#ffffff',
  cursor: 'pointer',
  fontWeight: 800
};

export function AdminDashboard() {
  return (
    <RouteGuard allowedIdentities={['operator']} title="正在进入运营后台">
      {(context) => <AdminDashboardContent context={context} />}
    </RouteGuard>
  );
}

function AdminDashboardContent({ context }: { context: GuardContext }) {
  const [companies, setCompanies] = useState<CompanyReviewItem[]>([]);
  const [jobs, setJobs] = useState<JobReviewItem[]>([]);
  const [exports, setExports] = useState<CompanyExportApply[]>([]);
  const [opsOverview, setOpsOverview] = useState<OpsOverview | null>(null);
  const [recommendations, setRecommendations] = useState<RecommendationItem[]>([]);
  const [homeSlots, setHomeSlots] = useState<HomeSlotItem[]>([]);
  const [homeSlotImageUrls, setHomeSlotImageUrls] = useState<Record<number, string>>({});
  const [riskReviews, setRiskReviews] = useState<RiskReviewItem[]>([]);
  const [operatorOpsEnabled, setOperatorOpsEnabled] = useState(false);
  const [auditTraceId, setAuditTraceId] = useState('trace-demo');
  const [auditTrace, setAuditTrace] = useState<AuditTraceSummary | null>(null);
  const [recommendationTargetId, setRecommendationTargetId] = useState('1');
  const [recommendationTitle, setRecommendationTitle] = useState('首页推荐位');
  const [homeSlotCode, setHomeSlotCode] = useState('home_hero_banner');
  const [homeSlotTitle, setHomeSlotTitle] = useState('首页首屏运营位');
  const [homeSlotImageUrl, setHomeSlotImageUrl] = useState('/demo/home-ad-full.svg');
  const [status, setStatus] = useState<'loading' | 'ready' | 'error' | 'retrying'>('loading');
  const [message, setMessage] = useState('正在读取运营后台队列。');
  const [traceId, setTraceId] = useState<string>();

  const canWrite = context.adminRoleHint === 'operator';

  async function hydrateHomeSlotImageUrls(items: HomeSlotItem[]) {
    const withImages = items.filter((item) => item.has_image && item.image_content_url);
    const entries = await Promise.all(withImages.map(async (item) => {
      const blob = await fetchHomeSlotImageBlob(context.token, item);
      return [item.slot_id, URL.createObjectURL(blob)] as const;
    }));
    setHomeSlotImageUrls((previous) => {
      Object.values(previous).forEach((url) => URL.revokeObjectURL(url));
      return Object.fromEntries(entries);
    });
  }

  async function load() {
    setStatus('retrying');
    setMessage('正在读取企业审核、职位审核、导出审批与运营化配置。');
    try {
      const [companyResult, jobResult, exportResult] = await Promise.all([
        fetchCompanyReviewQueue(context.token),
        fetchJobReviewQueue(context.token),
        fetchExportReviewQueue(context.token)
      ]);
      setCompanies(companyResult.data);
      setJobs(jobResult.data);
      setExports(exportResult.data);
      let latestTraceId = exportResult.traceId || jobResult.traceId || companyResult.traceId;

      try {
        const overviewResult = await fetchOpsOverview(context.token);
        const [recommendationResult, homeSlotResult, riskResult] = await Promise.all([
          fetchRecommendations(context.token),
          fetchHomeSlots(context.token),
          fetchRiskReviews(context.token)
        ]);
        setOpsOverview(overviewResult.data);
        setOperatorOpsEnabled(overviewResult.data.features.operator_portal_ops_enabled);
        setRecommendations(recommendationResult.data);
        setHomeSlots(homeSlotResult.data);
        await hydrateHomeSlotImageUrls(homeSlotResult.data);
        setRiskReviews(riskResult.data);
        latestTraceId = riskResult.traceId || homeSlotResult.traceId || recommendationResult.traceId || overviewResult.traceId || latestTraceId;
      } catch (error) {
        if (isHttpClientError(error) && error.code === 'FEATURE_DISABLED_403') {
          setOperatorOpsEnabled(false);
          setOpsOverview(null);
          setRecommendations([]);
          setHomeSlots([]);
          setHomeSlotImageUrls((previous) => {
            Object.values(previous).forEach((url) => URL.revokeObjectURL(url));
            return {};
          });
          setRiskReviews([]);
        } else {
          throw error;
        }
      }

      setTraceId(latestTraceId);
      setStatus('ready');
      setMessage('');
    } catch (error) {
      setStatus('error');
      setMessage(error instanceof Error ? error.message : '运营后台暂时不可用。');
    }
  }

  useEffect(() => {
    void load();
    return () => {
      setHomeSlotImageUrls((previous) => {
        Object.values(previous).forEach((url) => URL.revokeObjectURL(url));
        return {};
      });
    };
  }, []);

  async function submitCompanyReview(companyId: number, decision: ApprovalDecision, memo: string) {
    await reviewCompany(context.token, {
      company_id: companyId,
      audit_status: decision === 'approve' ? 2 : 3,
      memo
    });
    await load();
  }

  async function submitJobReview(jobId: number, decision: ApprovalDecision, memo: string) {
    await reviewJob(context.token, {
      job_id: jobId,
      audit_status: decision === 'approve' ? 2 : 3,
      memo
    });
    await load();
  }

  async function submitExportReview(exportId: number, decision: ApprovalDecision, memo: string) {
    await reviewExport(context.token, {
      export_id: exportId,
      approve_status: decision === 'approve' ? 1 : 2,
      memo
    });
    await load();
  }

  async function onCreateRecommendation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const targetId = Number.parseInt(recommendationTargetId, 10);
    if (!Number.isFinite(targetId) || targetId <= 0) {
      setStatus('error');
      setMessage('请输入合法的推荐目标 ID。');
      return;
    }
    setStatus('retrying');
    setMessage('正在保存推荐位配置。');
    try {
      const result = await createRecommendation(context.token, {
        slot_code: 'home_hot_jobs',
        target_type: 'job',
        target_id: targetId,
        title_override: recommendationTitle,
        summary_override: '运营推荐位公开摘要',
        display_order: 1,
        status: 1
      });
      setTraceId(result.traceId);
      await load();
    } catch (error) {
      setStatus('error');
      setMessage(error instanceof Error ? error.message : '推荐位保存失败。');
    }
  }

  async function onOfflineRecommendation(recommendationId: number) {
    setStatus('retrying');
    setMessage('正在下线推荐位。');
    try {
      const result = await offlineRecommendation(context.token, recommendationId);
      setTraceId(result.traceId);
      await load();
    } catch (error) {
      setStatus('error');
      setMessage(error instanceof Error ? error.message : '推荐位下线失败。');
    }
  }

  async function onCreateHomeSlot(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setStatus('retrying');
    setMessage('正在保存首页运营位。');
    try {
      const result = await createHomeSlot(context.token, {
        slot_code: homeSlotCode,
        title: homeSlotTitle,
        subtitle: '后台运营可配置；不做广告售卖、支付或外部投放。',
        image_url: homeSlotImageUrl,
        image_alt: 'LocalTalent 首页运营位',
        link_type: 'internal',
        link_url: '/jobs',
        display_order: 1,
        status: 1
      });
      setTraceId(result.traceId);
      await load();
    } catch (error) {
      setStatus('error');
      setMessage(error instanceof Error ? error.message : '首页运营位保存失败。');
    }
  }

  async function onOfflineHomeSlot(slotId: number) {
    setStatus('retrying');
    setMessage('正在下线首页运营位。');
    try {
      const result = await offlineHomeSlot(context.token, slotId);
      setTraceId(result.traceId);
      await load();
    } catch (error) {
      setStatus('error');
      setMessage(error instanceof Error ? error.message : '首页运营位下线失败。');
    }
  }

  async function onUploadHomeSlotImage(slotId: number, file?: File) {
    if (!file) return;
    setStatus('retrying');
    setMessage('正在上传首页运营位图片。');
    try {
      const result = await uploadHomeSlotImage(context.token, slotId, file);
      setTraceId(result.traceId);
      await load();
    } catch (error) {
      setStatus('error');
      setMessage(error instanceof Error ? error.message : '首页运营位图片上传失败。');
    }
  }

  async function onDeleteHomeSlotImage(slotId: number) {
    setStatus('retrying');
    setMessage('正在删除首页运营位图片。');
    try {
      const result = await deleteHomeSlotImage(context.token, slotId);
      setTraceId(result.traceId);
      await load();
    } catch (error) {
      setStatus('error');
      setMessage(error instanceof Error ? error.message : '首页运营位图片删除失败。');
    }
  }

  async function onHandleRisk(riskId: number) {
    setStatus('retrying');
    setMessage('正在处理风险审核任务。');
    try {
      const result = await handleRiskReview(context.token, riskId, {
        status: 2,
        decision: '已核查，按低风险公开对象处理。'
      });
      setTraceId(result.traceId);
      await load();
    } catch (error) {
      setStatus('error');
      setMessage(error instanceof Error ? error.message : '风险审核处理失败。');
    }
  }

  async function onAuditTrace(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!auditTraceId.trim()) {
      setStatus('error');
      setMessage('请输入 trace_id。');
      return;
    }

    setStatus('retrying');
    setMessage('正在查询审计链路。');
    try {
      const result = await fetchAuditTrace(context.token, auditTraceId.trim());
      setAuditTrace(result.data);
      setTraceId(result.traceId);
      setStatus('ready');
      setMessage('');
    } catch (error) {
      setStatus('error');
      setMessage(error instanceof Error ? error.message : '审计链路查询失败。');
    }
  }

  return (
    <main style={{ minHeight: '100vh', padding: '32px 18px 56px' }}>
      <div style={{ maxWidth: '1220px', margin: '0 auto' }}>
        <section style={{ ...cardStyle, borderRadius: '34px' }}>
          <p style={{ margin: 0, color: 'var(--lt-accent-strong)', fontWeight: 900 }}>运营后台</p>
          <h1 style={{ margin: '12px 0', fontSize: 'clamp(2rem, 4vw, 4rem)' }}>
            企业审核、职位审核、导出审批与审计入口。
          </h1>
          <p style={{ margin: 0, color: 'var(--lt-ink-muted)', lineHeight: 1.8 }}>
            当前账号：{context.identity.display_name ?? '内部账号'}。
            {canWrite ? ' operator 写操作入口已显示。' : ' auditor/未知角色按只读展示，写入口隐藏。'}
            {operatorOpsEnabled ? ' 三期运营化已开启。' : ' 三期运营化未开启，保留二期审核基础界面。'}
            {traceId ? <span> trace_id：{traceId}</span> : null}
          </p>
        </section>

        {status === 'error' ? (
          <section style={{ marginTop: '18px' }}>
            <StateView variant="error" title="运营后台操作失败" description={message} retryLabel="重新读取" onRetry={load} />
          </section>
        ) : status === 'retrying' || status === 'loading' ? (
          <section style={{ marginTop: '18px' }}>
            <StateView variant={status === 'loading' ? 'loading' : 'retrying'} title="运营后台处理中" description={message} />
          </section>
        ) : null}

        <div style={{ display: 'grid', gap: '18px', marginTop: '22px' }}>
          <section style={cardStyle} aria-label="运营生产化首页">
            <h2 style={{ marginTop: 0 }}>运营首页</h2>
            {operatorOpsEnabled && opsOverview ? (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(150px, 1fr))', gap: '12px' }}>
                {[
                  ['待审企业', opsOverview.pending_company_count],
                  ['待审职位', opsOverview.pending_job_count],
                  ['待审导出', opsOverview.pending_export_count],
                  ['已发布内容', opsOverview.published_content_count],
                  ['已发布活动', opsOverview.published_event_count],
                  ['有效推荐位', opsOverview.active_recommendation_count],
                  ['待处理风险', opsOverview.pending_risk_count],
                  ['近 7 日审计', opsOverview.recent_audit_count]
                ].map(([label, value]) => (
                  <div key={label} style={{ border: '1px solid var(--lt-line)', borderRadius: '18px', padding: '14px' }}>
                    <strong style={{ display: 'block', fontSize: '1.7rem' }}>{value}</strong>
                    <span style={{ color: 'var(--lt-ink-muted)' }}>{label}</span>
                  </div>
                ))}
              </div>
            ) : (
              <p style={{ color: 'var(--lt-ink-muted)' }}>
                phase3.operator_portal_ops 默认关闭；当前仅展示二期企业审核、职位审核、导出审批与审计入口。
              </p>
            )}
          </section>

          <ReviewTable<CompanyReviewItem>
            title="企业审核"
            rows={companies}
            columns={[
              { key: 'id', header: '企业编号', render: (row) => row.company_id },
              { key: 'name', header: '企业名称', render: (row) => row.company_name },
              { key: 'license', header: '证照编号', render: (row) => row.license_no || '未填写' },
              { key: 'status', header: '认证状态', render: (row) => <StatusBadge kind="company_auth" value={row.auth_status} /> },
              {
                key: 'action',
                header: '操作',
                render: (row) => canWrite
                  ? <ApprovalDialog subject={`企业 #${row.company_id}`} onSubmit={(decision, memo) => submitCompanyReview(row.company_id, decision, memo)} />
                  : <span style={{ color: 'var(--lt-ink-muted)' }}>只读</span>
              }
            ]}
          />

          <ReviewTable<JobReviewItem>
            title="职位审核"
            rows={jobs}
            columns={[
              { key: 'id', header: '职位编号', render: (row) => row.job_id },
              { key: 'title', header: '职位', render: (row) => row.title },
              { key: 'jobStatus', header: '职位状态', render: (row) => <StatusBadge kind="job_status" value={row.status} /> },
              { key: 'auditStatus', header: '审核状态', render: (row) => <StatusBadge kind="job_audit" value={row.audit_status} /> },
              {
                key: 'action',
                header: '操作',
                render: (row) => canWrite
                  ? <ApprovalDialog subject={`职位 #${row.job_id}`} onSubmit={(decision, memo) => submitJobReview(row.job_id, decision, memo)} />
                  : <span style={{ color: 'var(--lt-ink-muted)' }}>只读</span>
              }
            ]}
          />

          <ReviewTable<CompanyExportApply>
            title="导出审批"
            rows={exports}
            columns={[
              { key: 'id', header: '导出编号', render: (row) => row.export_id },
              { key: 'biz', header: '导出类型', render: (row) => row.biz_type },
              { key: 'approve', header: '审批状态', render: (row) => <StatusBadge kind="export_approve" value={row.approve_status} /> },
              { key: 'generate', header: '生成状态', render: (row) => <StatusBadge kind="export_generate" value={row.generate_status} /> },
              {
                key: 'action',
                header: '操作',
                render: (row) => canWrite
                  ? <ApprovalDialog subject={`导出 #${row.export_id}`} onSubmit={(decision, memo) => submitExportReview(row.export_id, decision, memo)} />
                  : <span style={{ color: 'var(--lt-ink-muted)' }}>只读</span>
              }
            ]}
          />

          <section style={cardStyle} aria-label="内容管理与活动招聘会管理">
            <h2 style={{ marginTop: 0 }}>内容管理 / 活动与招聘会管理</h2>
            <p style={{ color: 'var(--lt-ink-muted)', lineHeight: 1.7 }}>
              本轮复用既有 CMS 与 activity_event 后台能力；公开门户只读取已发布、在线对象。推荐位不得引用撤回快照、下线职位、未认证企业或未发布内容。
            </p>
          </section>

          <section style={cardStyle} aria-label="推荐位配置">
            <h2 style={{ marginTop: 0 }}>推荐位配置</h2>
            <p style={{ color: 'var(--lt-ink-muted)', lineHeight: 1.7 }}>
              推荐位是运营配置，不是广告售卖系统；服务端会校验目标必须已审核、已发布、在线且合规。
            </p>
            {operatorOpsEnabled && canWrite ? (
              <form onSubmit={onCreateRecommendation} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '10px', alignItems: 'end' }}>
                <label>
                  <span style={{ display: 'block', marginBottom: '7px', color: 'var(--lt-ink-muted)' }}>推荐职位 ID</span>
                  <input value={recommendationTargetId} onChange={(event) => setRecommendationTargetId(event.target.value)} style={inputStyle} />
                </label>
                <label>
                  <span style={{ display: 'block', marginBottom: '7px', color: 'var(--lt-ink-muted)' }}>标题覆盖</span>
                  <input value={recommendationTitle} onChange={(event) => setRecommendationTitle(event.target.value)} style={inputStyle} />
                </label>
                <button type="submit" style={buttonStyle}>保存推荐位</button>
              </form>
            ) : (
              <p style={{ color: 'var(--lt-ink-muted)' }}>{canWrite ? '三期运营化未开启。' : 'auditor/未知角色只读，不显示推荐位写入口。'}</p>
            )}
            <div style={{ display: 'grid', gap: '10px', marginTop: '14px' }}>
              {recommendations.length === 0 ? (
                <p style={{ color: 'var(--lt-ink-muted)' }}>暂无推荐位配置。</p>
              ) : recommendations.map((item) => (
                <article key={item.recommendation_id} style={{ border: '1px solid var(--lt-line)', borderRadius: '16px', padding: '12px' }}>
                  <strong>{item.slot_code} · {item.target_type} #{item.target_id}</strong>
                  <p style={{ margin: '6px 0', color: 'var(--lt-ink-muted)' }}>
                    {item.title_override || '未设置标题覆盖'} · {item.target_valid ? '目标合法' : '目标已失效'}
                  </p>
                  {operatorOpsEnabled && canWrite && item.status === 1 ? (
                    <button type="button" style={buttonStyle} onClick={() => onOfflineRecommendation(item.recommendation_id)}>下线推荐位</button>
                  ) : null}
                </article>
              ))}
            </div>
          </section>

          <section style={cardStyle} aria-label="首页运营位配置">
            <h2 style={{ marginTop: 0 }}>首页运营位配置</h2>
            <p style={{ color: 'var(--lt-ink-muted)', lineHeight: 1.7 }}>
              首页运营位用于 banner、通栏、半宽和快捷入口等展示位；支持运营后台私有上传图片，公开首页通过后端代理读取，不暴露对象存储路径。
            </p>
            {operatorOpsEnabled && canWrite ? (
              <form onSubmit={onCreateHomeSlot} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '10px', alignItems: 'end' }}>
                <label>
                  <span style={{ display: 'block', marginBottom: '7px', color: 'var(--lt-ink-muted)' }}>首页位置</span>
                  <select value={homeSlotCode} onChange={(event) => setHomeSlotCode(event.target.value)} style={inputStyle}>
                    <option value="home_hero_banner">首屏大 banner</option>
                    <option value="home_full_width_banner">通栏运营位</option>
                    <option value="home_half_left">半宽运营位左</option>
                    <option value="home_half_right">半宽运营位右</option>
                    <option value="home_third_1">三列运营位 1</option>
                    <option value="home_third_2">三列运营位 2</option>
                    <option value="home_third_3">三列运营位 3</option>
                    <option value="home_quick_1">快捷入口 1</option>
                    <option value="home_quick_2">快捷入口 2</option>
                    <option value="home_quick_3">快捷入口 3</option>
                    <option value="home_bottom_banner">底部运营位</option>
                  </select>
                </label>
                <label>
                  <span style={{ display: 'block', marginBottom: '7px', color: 'var(--lt-ink-muted)' }}>标题</span>
                  <input value={homeSlotTitle} onChange={(event) => setHomeSlotTitle(event.target.value)} style={inputStyle} />
                </label>
                <label>
                  <span style={{ display: 'block', marginBottom: '7px', color: 'var(--lt-ink-muted)' }}>图片 URL</span>
                  <input value={homeSlotImageUrl} onChange={(event) => setHomeSlotImageUrl(event.target.value)} style={inputStyle} />
                </label>
                <button type="submit" style={buttonStyle}>保存首页运营位</button>
              </form>
            ) : (
              <p style={{ color: 'var(--lt-ink-muted)' }}>{canWrite ? '三期运营化未开启。' : 'auditor/未知角色只读，不显示首页运营位写入口。'}</p>
            )}
            <div style={{ display: 'grid', gap: '10px', marginTop: '14px' }}>
              {homeSlots.length === 0 ? (
                <p style={{ color: 'var(--lt-ink-muted)' }}>暂无首页运营位配置。</p>
              ) : homeSlots.map((item) => {
                const previewUrl = homeSlotImageUrls[item.slot_id] || item.image_url;
                return (
                  <article key={item.slot_id} style={{ border: '1px solid var(--lt-line)', borderRadius: '16px', padding: '12px', display: 'grid', gap: '12px' }}>
                    <div style={{ display: 'flex', gap: '14px', alignItems: 'center', flexWrap: 'wrap' }}>
                      {previewUrl ? (
                        // eslint-disable-next-line @next/next/no-img-element
                        <img
                          src={previewUrl}
                          alt={item.image_alt || item.title || '首页运营位图片'}
                          style={{ width: '156px', height: '72px', objectFit: 'cover', borderRadius: '14px', border: '1px solid var(--lt-line)' }}
                        />
                      ) : (
                        <div style={{ width: '156px', height: '72px', borderRadius: '14px', border: '1px dashed var(--lt-line)', display: 'grid', placeItems: 'center', color: 'var(--lt-ink-muted)' }}>
                          暂无图片
                        </div>
                      )}
                      <div style={{ flex: '1 1 260px' }}>
                        <strong>{item.slot_code} · {item.title}</strong>
                        <p style={{ margin: '6px 0', color: 'var(--lt-ink-muted)' }}>
                          {item.subtitle || '未设置副标题'} · {item.link_type === 'internal' ? item.link_url : item.link_type} · 状态 {item.status}
                        </p>
                        <p style={{ margin: 0, color: 'var(--lt-ink-muted)' }}>
                          {item.has_image
                            ? `首页运营位图片已上传：${item.image_file_name || '未命名图片'} · ${Math.round(item.image_size_bytes / 1024)}KB`
                            : '未上传图片，公开首页将使用安全图片 URL 或占位图。'}
                        </p>
                      </div>
                    </div>
                    {operatorOpsEnabled && canWrite ? (
                      <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'center' }}>
                        <label style={{ ...buttonStyle, display: 'inline-flex', alignItems: 'center' }}>
                          上传首页运营位图片
                          <input
                            aria-label={`上传 ${item.slot_code} 图片`}
                            type="file"
                            accept="image/jpeg,image/png,image/webp,.jpg,.jpeg,.png,.webp"
                            style={{ display: 'none' }}
                            onChange={(event) => {
                              const file = event.currentTarget.files?.[0];
                              event.currentTarget.value = '';
                              void onUploadHomeSlotImage(item.slot_id, file);
                            }}
                          />
                        </label>
                        {item.has_image ? (
                          <button type="button" style={{ ...buttonStyle, background: '#b91c1c' }} onClick={() => onDeleteHomeSlotImage(item.slot_id)}>删除首页运营位图片</button>
                        ) : null}
                        {item.status === 1 ? (
                          <button type="button" style={buttonStyle} onClick={() => onOfflineHomeSlot(item.slot_id)}>下线首页运营位</button>
                        ) : null}
                      </div>
                    ) : <span style={{ color: 'var(--lt-ink-muted)' }}>只读：auditor 可预览配置，不能上传或删除图片。</span>}
                  </article>
                );
              })}
            </div>
          </section>

          <section style={cardStyle} aria-label="风险审核">
            <h2 style={{ marginTop: 0 }}>风险审核</h2>
            {riskReviews.length === 0 ? (
              <p style={{ color: 'var(--lt-ink-muted)' }}>暂无风险审核任务。高风险能力仍在风险池，不会自动进入主线。</p>
            ) : (
              <div style={{ display: 'grid', gap: '10px' }}>
                {riskReviews.map((risk) => (
                  <article key={risk.risk_id} style={{ border: '1px solid var(--lt-line)', borderRadius: '16px', padding: '12px' }}>
                    <strong>{risk.title}</strong>
                    <p style={{ margin: '6px 0', color: 'var(--lt-ink-muted)' }}>
                      {risk.risk_type} · {risk.target_type} #{risk.target_id} · {risk.severity} · 状态 {risk.status}
                    </p>
                    <p style={{ color: 'var(--lt-ink-muted)' }}>{risk.summary}</p>
                    {operatorOpsEnabled && canWrite && risk.status === 0 ? (
                      <button type="button" style={buttonStyle} onClick={() => onHandleRisk(risk.risk_id)}>处理为低风险</button>
                    ) : <span style={{ color: 'var(--lt-ink-muted)' }}>只读</span>}
                  </article>
                ))}
              </div>
            )}
          </section>
        </div>

        <section style={{ ...cardStyle, marginTop: '18px' }}>
          <h2 style={{ marginTop: 0 }}>审计中心入口</h2>
          <p style={{ color: 'var(--lt-ink-muted)', lineHeight: 1.7 }}>
            输入 trace_id 串联 audit_log / field_access_log / open_api_log；本页只读，不提供审计写入口。
          </p>
          <form onSubmit={onAuditTrace} style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'end' }}>
            <label>
              <span style={{ display: 'block', marginBottom: '7px', color: 'var(--lt-ink-muted)' }}>trace_id</span>
              <input value={auditTraceId} onChange={(event) => setAuditTraceId(event.target.value)} style={inputStyle} />
            </label>
            <button type="submit" style={buttonStyle}>查询审计链路</button>
          </form>
          {auditTrace ? (
            <div style={{ display: 'grid', gap: '12px', marginTop: '14px' }}>
              <p style={{ margin: 0 }}>
                trace {auditTrace.trace_id}：审计 {auditTrace.audit_count} 条，字段访问 {auditTrace.access_count} 条，对接日志 {auditTrace.open_api_count} 条。
              </p>
              <AuditTraceList
                title="audit_log 摘要"
                empty="暂无审计动作。"
                rows={auditTrace.audit_log_list.map((item) => (
                  `#${item.id} ${item.biz_type}/${item.biz_id} · ${item.action_type} · ${item.operator_role}`
                ))}
              />
              <AuditTraceList
                title="field_access_log 摘要"
                empty="暂无字段访问记录。"
                rows={auditTrace.access_log_list.map((item) => (
                  `#${item.id} ${item.biz_type}/${item.biz_id} · ${item.field_name} · ${item.access_type} · ${item.operator_role}`
                ))}
              />
              <AuditTraceList
                title="open_api_log 摘要"
                empty="暂无对接日志。"
                rows={auditTrace.open_api_log_list.map((item) => (
                  `#${item.id} ${item.source_system}/${item.client_code} · ${item.api_code} · HTTP ${item.http_status}`
                ))}
              />
            </div>
          ) : null}
        </section>
      </div>
    </main>
  );
}

function AuditTraceList({ title, rows, empty }: { title: string; rows: string[]; empty: string }) {
  return (
    <div style={{ border: '1px solid var(--lt-line)', borderRadius: '16px', padding: '12px', background: '#fff' }}>
      <strong>{title}</strong>
      {rows.length === 0 ? (
        <p style={{ margin: '8px 0 0', color: 'var(--lt-ink-muted)' }}>{empty}</p>
      ) : (
        <ul style={{ margin: '8px 0 0', paddingLeft: '18px', color: 'var(--lt-ink-muted)' }}>
          {rows.slice(0, 8).map((row) => (
            <li key={row}>{row}</li>
          ))}
        </ul>
      )}
    </div>
  );
}
