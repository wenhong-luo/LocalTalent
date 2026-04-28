'use client';

import { FormEvent, useEffect, useState } from 'react';
import { ExportDownloadAction } from '@/components/backoffice/ExportDownloadAction';
import { ReviewTable } from '@/components/backoffice/ReviewTable';
import { RouteGuard, type GuardContext } from '@/components/backoffice/RouteGuard';
import { StatusBadge, statusLabel } from '@/components/backoffice/StatusBadge';
import { StateView } from '@/components/StateView';
import {
  applyCompanyExport,
  fetchCompanyApplications,
  fetchCompanyExportDetail,
  fetchCompanyJobs,
  issueCompanyExportDownloadUrl,
  submitCompanyApply,
  type CompanyApplication,
  type CompanyExportApply,
  type CompanyJob,
  type CompanyStatus
} from './companyApi';

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

function Field({
  label,
  value,
  onChange,
  placeholder
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
}) {
  return (
    <label>
      <span style={{ display: 'block', marginBottom: '7px', color: 'var(--lt-ink-muted)' }}>{label}</span>
      <input value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} style={inputStyle} />
    </label>
  );
}

export function CompanyDashboard() {
  return (
    <RouteGuard allowedIdentities={['company']} title="正在进入企业后台">
      {(context) => <CompanyDashboardContent context={context} />}
    </RouteGuard>
  );
}

function CompanyDashboardContent({ context }: { context: GuardContext }) {
  const [companyName, setCompanyName] = useState('LocalTalent 示例企业');
  const [licenseNo, setLicenseNo] = useState('LT-DEMO-2026');
  const [cityCode, setCityCode] = useState('310000');
  const [companyStatus, setCompanyStatus] = useState<CompanyStatus | null>(null);
  const [jobs, setJobs] = useState<CompanyJob[]>([]);
  const [applications, setApplications] = useState<CompanyApplication[]>([]);
  const [exportApply, setExportApply] = useState<CompanyExportApply | null>(null);
  const [exportReason, setExportReason] = useState('导出本企业投递池候选人明细，用于招聘流程复核。');
  const [exportIdInput, setExportIdInput] = useState('');
  const [status, setStatus] = useState<'loading' | 'ready' | 'error' | 'retrying'>('loading');
  const [message, setMessage] = useState('正在读取企业后台数据。');
  const [traceId, setTraceId] = useState<string>();

  async function load() {
    setStatus('retrying');
    setMessage('正在读取职位状态与投递池。');
    try {
      const [jobResult, applicationResult] = await Promise.all([
        fetchCompanyJobs(context.token),
        fetchCompanyApplications(context.token)
      ]);
      setJobs(jobResult.data.job_list);
      setApplications(applicationResult.data.application_list);
      setTraceId(applicationResult.traceId || jobResult.traceId);
      setStatus('ready');
      setMessage('');
    } catch (error) {
      setStatus('error');
      setMessage(error instanceof Error ? error.message : '企业后台暂时不可用。');
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function onApply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setStatus('retrying');
    setMessage('正在提交企业认证资料。');
    try {
      const result = await submitCompanyApply(context.token, {
        company_name: companyName,
        license_no: licenseNo,
        city_code: cityCode,
        industry_code: 'software',
        company_profile: 'Prompt 13 前端联调占位资料，真实审核仍由服务端强制。'
      });
      setCompanyStatus(result.data);
      setTraceId(result.traceId);
      setStatus('ready');
      setMessage('');
    } catch (error) {
      setStatus('error');
      setMessage(error instanceof Error ? error.message : '提交企业认证失败。');
    }
  }

  async function onCreateExport(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setStatus('retrying');
    setMessage('正在提交导出申请。');
    try {
      const result = await applyCompanyExport(context.token, { reason: exportReason });
      setExportApply(result.data);
      setTraceId(result.traceId);
      setStatus('ready');
      setMessage('');
    } catch (error) {
      setStatus('error');
      setMessage(error instanceof Error ? error.message : '提交导出申请失败。');
    }
  }

  async function onLoadExportDetail(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const exportId = Number.parseInt(exportIdInput, 10);
    if (!Number.isFinite(exportId) || exportId <= 0) {
      setStatus('error');
      setMessage('请输入有效导出编号。');
      return;
    }

    setStatus('retrying');
    setMessage('正在读取导出详情。');
    try {
      const result = await fetchCompanyExportDetail(context.token, exportId);
      setExportApply(result.data);
      setTraceId(result.traceId);
      setStatus('ready');
      setMessage('');
    } catch (error) {
      setStatus('error');
      setMessage(error instanceof Error ? error.message : '读取导出详情失败。');
    }
  }

  async function issueDownloadUrl(exportId: number) {
    const result = await issueCompanyExportDownloadUrl(context.token, exportId);
    setTraceId(result.traceId);
    return result.data;
  }

  return (
    <main style={{ minHeight: '100vh', padding: '32px 18px 56px' }}>
      <div style={{ maxWidth: '1180px', margin: '0 auto' }}>
        <section style={{ ...cardStyle, borderRadius: '34px' }}>
          <p style={{ margin: 0, color: 'var(--lt-accent-strong)', fontWeight: 900 }}>企业后台</p>
          <h1 style={{ margin: '12px 0', fontSize: 'clamp(2rem, 4vw, 4rem)' }}>
            企业认证、职位状态、投递池与导出申请。
          </h1>
          <p style={{ margin: 0, color: 'var(--lt-ink-muted)', lineHeight: 1.8 }}>
            当前账号：{context.identity.display_name ?? '企业账号'}。页面只展示本企业流程摘要，权限与数据域仍由后端强制。
            {traceId ? <span> trace_id：{traceId}</span> : null}
          </p>
        </section>

        {status === 'error' ? (
          <section style={{ marginTop: '18px' }}>
            <StateView variant="error" title="企业后台操作失败" description={message} retryLabel="重新读取" onRetry={load} />
          </section>
        ) : status === 'retrying' || status === 'loading' ? (
          <section style={{ marginTop: '18px' }}>
            <StateView variant={status === 'loading' ? 'loading' : 'retrying'} title="企业后台处理中" description={message} />
          </section>
        ) : null}

        <section style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(310px, 1fr))', gap: '18px', marginTop: '22px' }}>
          <form style={cardStyle} onSubmit={onApply}>
            <h2 style={{ marginTop: 0 }}>企业认证状态</h2>
            <p style={{ color: 'var(--lt-ink-muted)', lineHeight: 1.7 }}>
              初始状态以服务端返回为准；提交认证资料后按 `auth_status` 展示。
            </p>
            {companyStatus ? (
              <p>
                <StatusBadge kind="company_auth" value={companyStatus.auth_status} />
                {companyStatus.reject_reason ? <span> 驳回原因：{companyStatus.reject_reason}</span> : null}
              </p>
            ) : (
              <p style={{ color: 'var(--lt-ink-muted)' }}>尚未读取到认证状态，请提交或等待后续接口返回。</p>
            )}
            <div style={{ display: 'grid', gap: '12px' }}>
              <Field label="企业名称" value={companyName} onChange={setCompanyName} />
              <Field label="统一社会信用代码" value={licenseNo} onChange={setLicenseNo} />
              <Field label="城市代码" value={cityCode} onChange={setCityCode} />
              <button type="submit" style={buttonStyle}>提交认证资料</button>
            </div>
          </form>

          <form style={cardStyle} onSubmit={onCreateExport}>
            <h2 style={{ marginTop: 0 }}>导出申请入口</h2>
            <p style={{ color: 'var(--lt-ink-muted)', lineHeight: 1.7 }}>
              未审批前不显示下载入口；后端仍会拒绝未审批下载。
            </p>
            <label>
              <span style={{ display: 'block', marginBottom: '7px', color: 'var(--lt-ink-muted)' }}>申请原因</span>
              <textarea
                value={exportReason}
                onChange={(event) => setExportReason(event.target.value)}
                rows={4}
                style={inputStyle}
              />
            </label>
            <button type="submit" style={{ ...buttonStyle, marginTop: '12px' }}>提交导出申请</button>
          </form>
        </section>

        <section style={{ ...cardStyle, marginTop: '18px' }}>
          <h2 style={{ marginTop: 0 }}>导出详情与下载入口</h2>
          <form onSubmit={onLoadExportDetail} style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'end' }}>
            <label>
              <span style={{ display: 'block', marginBottom: '7px', color: 'var(--lt-ink-muted)' }}>导出编号</span>
              <input value={exportIdInput} onChange={(event) => setExportIdInput(event.target.value)} style={inputStyle} />
            </label>
            <button type="submit" style={buttonStyle}>查询导出详情</button>
          </form>
          {exportApply ? (
            <div style={{ marginTop: '14px', display: 'grid', gap: '10px' }}>
              <div>导出 #{exportApply.export_id}：{exportApply.reason || '无申请原因'}</div>
              <ExportDownloadAction exportApply={exportApply} onIssueDownloadUrl={issueDownloadUrl} />
            </div>
          ) : (
            <p style={{ color: 'var(--lt-ink-muted)' }}>暂无导出详情。</p>
          )}
        </section>

        <div style={{ display: 'grid', gap: '18px', marginTop: '18px' }}>
          <ReviewTable<CompanyJob>
            title="职位状态"
            rows={jobs}
            columns={[
              { key: 'id', header: '职位编号', render: (row) => row.job_id },
              { key: 'title', header: '职位', render: (row) => row.title },
              { key: 'status', header: '职位状态', render: (row) => <StatusBadge kind="job_status" value={row.status} /> },
              { key: 'audit', header: '审核状态', render: (row) => <StatusBadge kind="job_audit" value={row.audit_status} /> },
              { key: 'reject', header: '说明', render: (row) => row.reject_reason || statusLabel('job_status', row.status) }
            ]}
          />
          <ReviewTable<CompanyApplication>
            title="投递池最小列表"
            rows={applications}
            columns={[
              { key: 'id', header: '投递编号', render: (row) => row.application_id },
              { key: 'job', header: '职位', render: (row) => row.job_title },
              { key: 'status', header: '投递状态', render: (row) => <StatusBadge kind="application_status" value={row.status} /> },
              { key: 'company', header: '企业', render: (row) => row.company_name },
              { key: 'time', header: '投递时间', render: (row) => row.apply_time || '未知' }
            ]}
          />
        </div>
      </div>
    </main>
  );
}
