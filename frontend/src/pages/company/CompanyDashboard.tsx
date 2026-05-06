'use client';

import { type CSSProperties, type FormEvent, useEffect, useState } from 'react';
import { ExportDownloadAction } from '@/components/backoffice/ExportDownloadAction';
import { ReviewTable } from '@/components/backoffice/ReviewTable';
import { RouteGuard, type GuardContext } from '@/components/backoffice/RouteGuard';
import { StatusBadge, statusLabel } from '@/components/backoffice/StatusBadge';
import { StateView } from '@/components/StateView';
import { isHttpClientError } from '@/lib/httpClient';
import {
  applyCompanyExport,
  applyCompanyWorkbenchExport,
  changeCompanyWorkbenchApplicationStage,
  createCompanyWorkbenchInterviewSession,
  createCompanyWorkbenchJob,
  fetchCompanyApplications,
  fetchCompanyExportDetail,
  fetchCompanyJobs,
  fetchCompanyWorkbenchApplications,
  fetchCompanyWorkbenchInterviewSessions,
  fetchCompanyWorkbenchJobs,
  fetchCompanyWorkbenchOverview,
  issueCompanyExportDownloadUrl,
  offlineCompanyWorkbenchJob,
  saveCompanyWorkbenchProfile,
  submitCompanyApply,
  submitCompanyWorkbenchCertification,
  submitCompanyWorkbenchJobReview,
  type CompanyApplication,
  type CompanyExportApply,
  type CompanyInterviewSession,
  type CompanyJob,
  type CompanyStatus,
  type CompanyWorkbenchApplication,
  type CompanyWorkbenchOverview
} from './companyApi';

type LoadStatus = 'loading' | 'ready' | 'error' | 'retrying';
type WorkbenchMode = 'unknown' | 'enabled' | 'disabled';

const cardStyle: CSSProperties = {
  border: '1px solid var(--lt-line)',
  borderRadius: '24px',
  background: 'var(--lt-card)',
  padding: '22px',
  boxShadow: '0 16px 36px rgba(20, 33, 61, 0.08)'
};

const inputStyle: CSSProperties = {
  width: '100%',
  boxSizing: 'border-box',
  border: '1px solid var(--lt-line)',
  borderRadius: '14px',
  padding: '11px 12px',
  background: '#ffffff',
  color: 'var(--lt-ink)'
};

const buttonStyle: CSSProperties = {
  border: 'none',
  borderRadius: '999px',
  padding: '10px 16px',
  background: 'var(--lt-accent)',
  color: '#ffffff',
  cursor: 'pointer',
  fontWeight: 800
};

const secondaryButtonStyle: CSSProperties = {
  ...buttonStyle,
  background: '#0f766e'
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
      <span style={{ display: 'block', marginBottom: '7px', color: 'var(--lt-ink-muted)', fontWeight: 800 }}>
        {label}
      </span>
      <input value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} style={inputStyle} />
    </label>
  );
}

function TextAreaField({
  label,
  value,
  onChange,
  rows = 4
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  rows?: number;
}) {
  return (
    <label>
      <span style={{ display: 'block', marginBottom: '7px', color: 'var(--lt-ink-muted)', fontWeight: 800 }}>
        {label}
      </span>
      <textarea value={value} onChange={(event) => onChange(event.target.value)} rows={rows} style={inputStyle} />
    </label>
  );
}

function StatCard({ label, value, helper }: { label: string; value: number | string; helper: string }) {
  return (
    <article style={cardStyle}>
      <p style={{ margin: 0, color: 'var(--lt-ink-muted)', fontWeight: 800 }}>{label}</p>
      <strong style={{ display: 'block', marginTop: '10px', fontSize: '2rem' }}>{value}</strong>
      <p style={{ margin: '8px 0 0', color: 'var(--lt-ink-muted)', lineHeight: 1.7 }}>{helper}</p>
    </article>
  );
}

function featureDisabled(error: unknown): boolean {
  return isHttpClientError(error) && error.code === 'FEATURE_DISABLED_403';
}

function errorMessage(error: unknown, fallback: string): string {
  return error instanceof Error ? error.message : fallback;
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
  const [industryCode, setIndustryCode] = useState('software');
  const [natureCode, setNatureCode] = useState('private');
  const [scaleCode, setScaleCode] = useState('50-150');
  const [address, setAddress] = useState('上海市示例园区');
  const [companyProfile, setCompanyProfile] = useState('用于灰度试运营的企业资料摘要，公开展示仍以服务端裁剪字段为准。');
  const [materialSummary, setMaterialSummary] = useState('统一社会信用代码已核验；联系人已由企业账号持有人确认。');
  const [jobTitle, setJobTitle] = useState('三期招聘顾问');
  const [jobCityCode, setJobCityCode] = useState('310000');
  const [jobCategoryCode, setJobCategoryCode] = useState('software');
  const [jobDesc, setJobDesc] = useState('负责本地人才服务平台灰度试运营支持。');
  const [stageNote, setStageNote] = useState('企业已查看投递，进入面试沟通流程。');
  const [exportReason, setExportReason] = useState('导出本企业投递池候选人摘要，用于招聘流程复核。');
  const [exportIdInput, setExportIdInput] = useState('');

  const [workbenchMode, setWorkbenchMode] = useState<WorkbenchMode>('unknown');
  const [overview, setOverview] = useState<CompanyWorkbenchOverview | null>(null);
  const [companyStatus, setCompanyStatus] = useState<CompanyStatus | null>(null);
  const [jobs, setJobs] = useState<CompanyJob[]>([]);
  const [legacyApplications, setLegacyApplications] = useState<CompanyApplication[]>([]);
  const [workbenchApplications, setWorkbenchApplications] = useState<CompanyWorkbenchApplication[]>([]);
  const [interviewSessions, setInterviewSessions] = useState<CompanyInterviewSession[]>([]);
  const [exportApply, setExportApply] = useState<CompanyExportApply | null>(null);
  const [status, setStatus] = useState<LoadStatus>('loading');
  const [message, setMessage] = useState('正在读取企业后台数据。');
  const [traceId, setTraceId] = useState<string>();

  async function loadLegacy() {
    const [jobResult, applicationResult] = await Promise.all([
      fetchCompanyJobs(context.token),
      fetchCompanyApplications(context.token)
    ]);
    setJobs(jobResult.data.job_list);
    setLegacyApplications(applicationResult.data.application_list);
    setWorkbenchApplications([]);
    setInterviewSessions([]);
    setTraceId(applicationResult.traceId || jobResult.traceId);
  }

  async function loadWorkbench() {
    const overviewResult = await fetchCompanyWorkbenchOverview(context.token);
    const [jobResult, applicationResult, sessionResult] = await Promise.all([
      fetchCompanyWorkbenchJobs(context.token),
      fetchCompanyWorkbenchApplications(context.token),
      fetchCompanyWorkbenchInterviewSessions(context.token)
    ]);
    const profile = overviewResult.data.profile;

    setOverview(overviewResult.data);
    setCompanyStatus({
      company_id: profile.company_id,
      auth_status: profile.auth_status,
      reject_reason: profile.reject_reason
    });
    setCompanyName(profile.company_name || companyName);
    setCityCode(profile.city_code || cityCode);
    setIndustryCode(profile.industry_code || industryCode);
    setNatureCode(profile.nature_code || natureCode);
    setScaleCode(profile.scale_code || scaleCode);
    setAddress(profile.address || address);
    setCompanyProfile(profile.company_profile || companyProfile);
    setJobs(jobResult.data.job_list);
    setWorkbenchApplications(applicationResult.data.application_list);
    setInterviewSessions(sessionResult.data.session_list);
    setLegacyApplications([]);
    setWorkbenchMode('enabled');
    setTraceId(sessionResult.traceId || applicationResult.traceId || jobResult.traceId || overviewResult.traceId);
  }

  async function load() {
    setStatus((current) => (current === 'loading' ? 'loading' : 'retrying'));
    setMessage('正在读取企业工作台。');
    try {
      await loadWorkbench();
      setStatus('ready');
      setMessage('');
    } catch (error) {
      if (featureDisabled(error)) {
        setWorkbenchMode('disabled');
        setOverview(null);
        await loadLegacy();
        setStatus('ready');
        setMessage('三期企业工作台未开启，当前展示二期企业后台摘要。');
        return;
      }
      setStatus('error');
      setTraceId(isHttpClientError(error) ? error.traceId : undefined);
      setMessage(errorMessage(error, '企业后台暂时不可用。'));
    }
  }

  useEffect(() => {
    void load();
  }, []);

  async function runAction(description: string, action: () => Promise<void>) {
    setStatus('retrying');
    setMessage(description);
    try {
      await action();
      await load();
    } catch (error) {
      setStatus('error');
      setTraceId(isHttpClientError(error) ? error.traceId : undefined);
      setMessage(errorMessage(error, description));
    }
  }

  async function onLegacyApply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction('正在提交企业认证资料。', async () => {
      const result = await submitCompanyApply(context.token, {
        company_name: companyName,
        license_no: licenseNo,
        city_code: cityCode,
        industry_code: industryCode,
        company_profile: '二期企业后台兼容资料，真实审核仍由服务端强制。'
      });
      setCompanyStatus(result.data);
      setTraceId(result.traceId);
    });
  }

  async function onSaveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction('正在保存企业资料。', async () => {
      await saveCompanyWorkbenchProfile(context.token, {
        company_name: companyName,
        industry_code: industryCode,
        nature_code: natureCode,
        scale_code: scaleCode,
        city_code: cityCode,
        address,
        company_profile: companyProfile
      });
    });
  }

  async function submitCertification() {
    await runAction('正在提交企业认证材料摘要。', async () => {
      await submitCompanyWorkbenchCertification(context.token, {
        company_name: companyName,
        license_no: licenseNo,
        industry_code: industryCode,
        nature_code: natureCode,
        scale_code: scaleCode,
        city_code: cityCode,
        address,
        company_profile: companyProfile,
        certification_material_summary: {
          material_summary: materialSummary,
          submitted_by: 'company_account'
        }
      });
    });
  }

  async function onCreateJob(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction('正在创建企业职位草稿。', async () => {
      await createCompanyWorkbenchJob(context.token, {
        title: jobTitle,
        category_code: jobCategoryCode,
        city_code: jobCityCode,
        salary_min: 12000,
        salary_max: 22000,
        job_desc: jobDesc
      });
    });
  }

  async function onSubmitFirstJobReview() {
    const firstJob = jobs[0];
    if (!firstJob) {
      return;
    }
    await runAction('正在提交职位审核。', async () => {
      await submitCompanyWorkbenchJobReview(context.token, firstJob.job_id);
    });
  }

  async function onOfflineFirstJob() {
    const firstJob = jobs[0];
    if (!firstJob) {
      return;
    }
    await runAction('正在下线职位。', async () => {
      await offlineCompanyWorkbenchJob(context.token, firstJob.job_id, '企业工作台主动下线。');
    });
  }

  async function onChangeFirstApplicationStage() {
    const firstApplication = workbenchApplications[0];
    if (!firstApplication) {
      return;
    }
    await runAction('正在流转投递阶段。', async () => {
      await changeCompanyWorkbenchApplicationStage(context.token, firstApplication.application_id, 'interview_invited', stageNote);
    });
  }

  async function onCreateInterviewSession() {
    const firstApplication = workbenchApplications[0];
    if (!firstApplication) {
      return;
    }
    await runAction('正在创建站内面试邀约。', async () => {
      await createCompanyWorkbenchInterviewSession(context.token, firstApplication.application_id);
    });
  }

  async function onCreateExport(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction('正在提交导出申请。', async () => {
      const result = workbenchMode === 'enabled'
        ? await applyCompanyWorkbenchExport(context.token, exportReason)
        : await applyCompanyExport(context.token, { reason: exportReason });
      setExportApply(result.data);
      setTraceId(result.traceId);
    });
  }

  async function onLoadExportDetail(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const exportId = Number.parseInt(exportIdInput, 10);
    if (!Number.isFinite(exportId) || exportId <= 0) {
      setStatus('error');
      setMessage('请输入有效导出编号。');
      return;
    }
    await runAction('正在读取导出详情。', async () => {
      const result = await fetchCompanyExportDetail(context.token, exportId);
      setExportApply(result.data);
      setTraceId(result.traceId);
    });
  }

  async function issueDownloadUrl(exportId: number) {
    const result = await issueCompanyExportDownloadUrl(context.token, exportId);
    setTraceId(result.traceId);
    return result.data;
  }

  const workbenchEnabled = workbenchMode === 'enabled';
  const applicationRows = workbenchEnabled ? workbenchApplications : legacyApplications;

  return (
    <main style={{ minHeight: '100vh', padding: '32px 18px 56px' }}>
      <div style={{ maxWidth: '1180px', margin: '0 auto' }}>
        <section style={{ ...cardStyle, borderRadius: '34px' }}>
          <p style={{ margin: 0, color: 'var(--lt-accent-strong)', fontWeight: 900 }}>企业中心</p>
          <h1 style={{ margin: '12px 0', fontSize: 'clamp(2rem, 4vw, 4rem)' }}>
            企业招聘工作台，服务端状态驱动。
          </h1>
          <p style={{ margin: 0, color: 'var(--lt-ink-muted)', lineHeight: 1.8 }}>
            当前账号：{context.identity.display_name ?? '企业账号'}。企业公开主页与企业中心隔离，页面只展示本企业私有流程摘要。
            {traceId ? <span> trace_id：{traceId}</span> : null}
          </p>
        </section>

        {status === 'error' ? (
          <section style={{ marginTop: '18px' }}>
            <StateView variant="error" title="企业工作台操作失败" description={message} retryLabel="重新读取" onRetry={load} />
          </section>
        ) : status === 'retrying' || status === 'loading' ? (
          <section style={{ marginTop: '18px' }}>
            <StateView variant={status === 'loading' ? 'loading' : 'retrying'} title="企业工作台处理中" description={message} />
          </section>
        ) : null}

        {workbenchMode === 'disabled' ? (
          <section style={{ ...cardStyle, marginTop: '18px', borderStyle: 'dashed' }}>
            <h2 style={{ marginTop: 0 }}>三期企业工作台未开启</h2>
            <p style={{ color: 'var(--lt-ink-muted)', lineHeight: 1.8 }}>
              服务端返回 company_workbench_enabled=false，本页保留二期企业后台摘要。企业资料维护、职位工作台、投递阶段流转和站内面试邀约需要灰度开关开启后使用。
            </p>
          </section>
        ) : null}

        {workbenchEnabled && overview ? (
          <section style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))', gap: '14px', marginTop: '18px' }}>
            <StatCard label="职位总数" value={overview.stats.job_total} helper="本企业私有职位工作台统计。" />
            <StatCard label="投递总数" value={overview.stats.application_total} helper="仅包含本企业职位产生的投递。" />
            <StatCard label="待处理投递" value={overview.stats.pending_application_total} helper="状态由服务端返回，不由前端推断。" />
            <StatCard label="面试邀约" value={overview.stats.interview_total} helper="站内流程记录，不接外部推送。" />
          </section>
        ) : null}

        <section style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(310px, 1fr))', gap: '18px', marginTop: '22px' }}>
          <form style={cardStyle} onSubmit={workbenchEnabled ? onSaveProfile : onLegacyApply}>
            <h2 style={{ marginTop: 0 }}>企业资料与认证</h2>
            <p style={{ color: 'var(--lt-ink-muted)', lineHeight: 1.7 }}>
              认证状态以服务端返回为准。未认证企业可保存草稿，但不得提交审核或上线职位。
            </p>
            {companyStatus ? (
              <p>
                <StatusBadge kind="company_auth" value={companyStatus.auth_status} />
                {companyStatus.reject_reason ? <span> 驳回原因：{companyStatus.reject_reason}</span> : null}
              </p>
            ) : null}
            <div style={{ display: 'grid', gap: '12px' }}>
              <Field label="企业名称" value={companyName} onChange={setCompanyName} />
              <Field label="统一社会信用代码" value={licenseNo} onChange={setLicenseNo} />
              <Field label="行业代码" value={industryCode} onChange={setIndustryCode} />
              <Field label="企业性质代码" value={natureCode} onChange={setNatureCode} />
              <Field label="规模代码" value={scaleCode} onChange={setScaleCode} />
              <Field label="城市代码" value={cityCode} onChange={setCityCode} />
              {workbenchEnabled ? (
                <>
                  <Field label="企业地址（私有域）" value={address} onChange={setAddress} />
                  <TextAreaField label="企业资料摘要" value={companyProfile} onChange={setCompanyProfile} />
                  <TextAreaField label="认证材料摘要（不含附件 key）" value={materialSummary} onChange={setMaterialSummary} />
                  <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
                    <button type="submit" style={buttonStyle}>保存企业资料</button>
                    <button type="button" style={secondaryButtonStyle} onClick={() => void submitCertification()}>
                      提交认证材料
                    </button>
                  </div>
                </>
              ) : (
                <button type="submit" style={buttonStyle}>提交认证资料</button>
              )}
            </div>
          </form>

          <form style={cardStyle} onSubmit={onCreateExport}>
            <h2 style={{ marginTop: 0 }}>导出申请入口</h2>
            <p style={{ color: 'var(--lt-ink-muted)', lineHeight: 1.7 }}>
              企业端只提交申请与查看状态；未审批前不显示下载入口，后端仍拒绝未审批下载。
            </p>
            <TextAreaField label="申请原因" value={exportReason} onChange={setExportReason} />
            <button type="submit" style={{ ...buttonStyle, marginTop: '12px' }}>提交导出申请</button>
          </form>
        </section>

        {workbenchEnabled ? (
          <section style={{ ...cardStyle, marginTop: '18px' }}>
            <h2 style={{ marginTop: 0 }}>职位管理</h2>
            <form onSubmit={onCreateJob} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(210px, 1fr))', gap: '12px' }}>
              <Field label="职位标题" value={jobTitle} onChange={setJobTitle} />
              <Field label="职类代码" value={jobCategoryCode} onChange={setJobCategoryCode} />
              <Field label="城市代码" value={jobCityCode} onChange={setJobCityCode} />
              <TextAreaField label="职位描述" value={jobDesc} onChange={setJobDesc} rows={3} />
              <div style={{ display: 'flex', alignItems: 'end', gap: '10px', flexWrap: 'wrap' }}>
                <button type="submit" style={buttonStyle}>创建职位草稿</button>
                <button type="button" style={secondaryButtonStyle} onClick={() => void onSubmitFirstJobReview()} disabled={jobs.length === 0}>
                  提交首个职位审核
                </button>
                <button type="button" style={{ ...secondaryButtonStyle, background: '#475569' }} onClick={() => void onOfflineFirstJob()} disabled={jobs.length === 0}>
                  下线首个职位
                </button>
              </div>
            </form>
          </section>
        ) : null}

        <section style={{ ...cardStyle, marginTop: '18px' }}>
          <h2 style={{ marginTop: 0 }}>导出详情与下载入口</h2>
          <form onSubmit={onLoadExportDetail} style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'end' }}>
            <label>
              <span style={{ display: 'block', marginBottom: '7px', color: 'var(--lt-ink-muted)', fontWeight: 800 }}>导出编号</span>
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

        {workbenchEnabled ? (
          <section style={{ ...cardStyle, marginTop: '18px' }}>
            <h2 style={{ marginTop: 0 }}>投递阶段流转与面试邀约</h2>
            <p style={{ color: 'var(--lt-ink-muted)', lineHeight: 1.7 }}>
              投递池仅处理本企业职位产生的投递，不提供跨库检索或联系方式入口。
            </p>
            <TextAreaField label="企业处理备注（本企业私有域）" value={stageNote} onChange={setStageNote} rows={3} />
            <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', marginTop: '12px' }}>
              <button type="button" style={buttonStyle} onClick={() => void onChangeFirstApplicationStage()} disabled={workbenchApplications.length === 0}>
                邀约首个投递
              </button>
              <button type="button" style={secondaryButtonStyle} onClick={() => void onCreateInterviewSession()} disabled={workbenchApplications.length === 0}>
                创建站内面试邀约
              </button>
            </div>
          </section>
        ) : null}

        <div style={{ display: 'grid', gap: '18px', marginTop: '18px' }}>
          <ReviewTable<CompanyJob>
            title={workbenchEnabled ? '职位工作台' : '职位状态'}
            rows={jobs}
            columns={[
              { key: 'id', header: '职位编号', render: (row) => row.job_id },
              { key: 'title', header: '职位', render: (row) => row.title },
              { key: 'status', header: '职位状态', render: (row) => <StatusBadge kind="job_status" value={row.status} /> },
              { key: 'audit', header: '审核状态', render: (row) => <StatusBadge kind="job_audit" value={row.audit_status} /> },
              { key: 'reject', header: '说明', render: (row) => row.reject_reason || statusLabel('job_status', row.status) }
            ]}
          />
          {workbenchEnabled ? (
            <>
              <ReviewTable<CompanyWorkbenchApplication>
                title="投递池"
                rows={workbenchApplications}
                columns={[
                  { key: 'id', header: '投递编号', render: (row) => row.application_id },
                  { key: 'job', header: '职位', render: (row) => row.job_title },
                  { key: 'candidate', header: '候选人摘要', render: (row) => `${row.display_name_masked} / ${row.city_code || '未知城市'}` },
                  { key: 'skills', header: '技能摘要', render: (row) => row.skills_summary || '暂无摘要' },
                  { key: 'experience', header: '经验', render: (row) => (row.experience_years === null ? '未知' : `${row.experience_years}年`) },
                  { key: 'status', header: '阶段', render: (row) => row.status_label || statusLabel('application_status', row.application_status) },
                  { key: 'note', header: '备注', render: (row) => row.company_stage_note || '暂无' }
                ]}
              />
              <ReviewTable<CompanyInterviewSession>
                title="面试邀约"
                rows={interviewSessions}
                columns={[
                  { key: 'id', header: '场次编号', render: (row) => row.session_id },
                  { key: 'application', header: '投递编号', render: (row) => row.application_id },
                  { key: 'job', header: '职位', render: (row) => row.job_title },
                  { key: 'name', header: '场次', render: (row) => row.session_name },
                  { key: 'time', header: '时间', render: (row) => row.session_time || '待确认' },
                  { key: 'location', header: '地点', render: (row) => row.location || '站内邀约' }
                ]}
              />
            </>
          ) : (
            <ReviewTable<CompanyApplication>
              title="投递池最小列表"
              rows={applicationRows as CompanyApplication[]}
              columns={[
                { key: 'id', header: '投递编号', render: (row) => row.application_id },
                { key: 'job', header: '职位', render: (row) => row.job_title },
                { key: 'status', header: '投递状态', render: (row) => <StatusBadge kind="application_status" value={row.status} /> },
                { key: 'company', header: '企业', render: (row) => row.company_name },
                { key: 'time', header: '投递时间', render: (row) => row.apply_time || '未知' }
              ]}
            />
          )}
        </div>
      </div>
    </main>
  );
}
