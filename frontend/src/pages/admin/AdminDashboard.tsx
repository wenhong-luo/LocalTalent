'use client';

import { FormEvent, useEffect, useState } from 'react';
import { ApprovalDialog, type ApprovalDecision } from '@/components/backoffice/ApprovalDialog';
import { ReviewTable } from '@/components/backoffice/ReviewTable';
import { RouteGuard, type GuardContext } from '@/components/backoffice/RouteGuard';
import { StatusBadge } from '@/components/backoffice/StatusBadge';
import { StateView } from '@/components/StateView';
import { isHttpClientError } from '@/lib/httpClient';
import {
  createRecommendation,
  fetchAuditTrace,
  fetchCompanyReviewQueue,
  fetchExportReviewQueue,
  fetchJobReviewQueue,
  fetchOpsOverview,
  fetchRecommendations,
  fetchRiskReviews,
  handleRiskReview,
  offlineRecommendation,
  reviewCompany,
  reviewExport,
  reviewJob,
  type AuditTraceSummary,
  type CompanyReviewItem,
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
  const [riskReviews, setRiskReviews] = useState<RiskReviewItem[]>([]);
  const [operatorOpsEnabled, setOperatorOpsEnabled] = useState(false);
  const [auditTraceId, setAuditTraceId] = useState('trace-demo');
  const [auditTrace, setAuditTrace] = useState<AuditTraceSummary | null>(null);
  const [recommendationTargetId, setRecommendationTargetId] = useState('1');
  const [recommendationTitle, setRecommendationTitle] = useState('首页推荐位');
  const [status, setStatus] = useState<'loading' | 'ready' | 'error' | 'retrying'>('loading');
  const [message, setMessage] = useState('正在读取运营后台队列。');
  const [traceId, setTraceId] = useState<string>();

  const canWrite = context.adminRoleHint === 'operator';

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
        const [recommendationResult, riskResult] = await Promise.all([
          fetchRecommendations(context.token),
          fetchRiskReviews(context.token)
        ]);
        setOpsOverview(overviewResult.data);
        setOperatorOpsEnabled(overviewResult.data.features.operator_portal_ops_enabled);
        setRecommendations(recommendationResult.data);
        setRiskReviews(riskResult.data);
        latestTraceId = riskResult.traceId || recommendationResult.traceId || overviewResult.traceId || latestTraceId;
      } catch (error) {
        if (isHttpClientError(error) && error.code === 'FEATURE_DISABLED_403') {
          setOperatorOpsEnabled(false);
          setOpsOverview(null);
          setRecommendations([]);
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
            <p>
              trace {auditTrace.trace_id}：审计 {auditTrace.audit_count} 条，字段访问 {auditTrace.access_count} 条，对接日志 {auditTrace.open_api_count} 条。
            </p>
          ) : null}
        </section>
      </div>
    </main>
  );
}
