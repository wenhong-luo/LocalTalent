'use client';

import { FormEvent, useEffect, useState } from 'react';
import { ApprovalDialog, type ApprovalDecision } from '@/components/backoffice/ApprovalDialog';
import { ReviewTable } from '@/components/backoffice/ReviewTable';
import { RouteGuard, type GuardContext } from '@/components/backoffice/RouteGuard';
import { StatusBadge } from '@/components/backoffice/StatusBadge';
import { StateView } from '@/components/StateView';
import {
  fetchAuditTrace,
  fetchCompanyReviewQueue,
  fetchExportReviewQueue,
  fetchJobReviewQueue,
  reviewCompany,
  reviewExport,
  reviewJob,
  type AuditTraceSummary,
  type CompanyReviewItem,
  type JobReviewItem
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
  const [auditTraceId, setAuditTraceId] = useState('trace-demo');
  const [auditTrace, setAuditTrace] = useState<AuditTraceSummary | null>(null);
  const [status, setStatus] = useState<'loading' | 'ready' | 'error' | 'retrying'>('loading');
  const [message, setMessage] = useState('正在读取运营后台队列。');
  const [traceId, setTraceId] = useState<string>();

  const canWrite = context.adminRoleHint === 'operator';

  async function load() {
    setStatus('retrying');
    setMessage('正在读取企业审核、职位审核与导出审批队列。');
    try {
      const [companyResult, jobResult, exportResult] = await Promise.all([
        fetchCompanyReviewQueue(context.token),
        fetchJobReviewQueue(context.token),
        fetchExportReviewQueue(context.token)
      ]);
      setCompanies(companyResult.data);
      setJobs(jobResult.data);
      setExports(exportResult.data);
      setTraceId(exportResult.traceId || jobResult.traceId || companyResult.traceId);
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
