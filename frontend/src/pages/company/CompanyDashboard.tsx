'use client';

import { type CSSProperties, type FormEvent, useEffect, useRef, useState } from 'react';
import { ExportDownloadAction } from '@/components/backoffice/ExportDownloadAction';
import { ReviewTable } from '@/components/backoffice/ReviewTable';
import { RouteGuard, type GuardContext } from '@/components/backoffice/RouteGuard';
import { StatusBadge, statusLabel } from '@/components/backoffice/StatusBadge';
import { DictionaryMultiSelect, DictionarySelect, dictionaryOptionLabel } from '@/components/selectors/DictionarySelect';
import { RegionCascadePicker } from '@/components/selectors/RegionCascadePicker';
import { useOutsidePointerDown } from '@/components/selectors/useOutsidePointerDown';
import { StateView } from '@/components/StateView';
import { isHttpClientError } from '@/lib/httpClient';
import {
  companyBenefitOptions,
  companyCapitalUnitOptions,
  companyIndustryOptions,
  companyNatureOptions,
  companyScaleOptions
} from '@/shared/catalogs/companyProfileOptions';
import {
  jobContactModeOptions,
  jobEducationOptions,
  jobExperienceOptions,
  jobNatureOptions,
  jobRecruitmentTimeOptions
} from '@/shared/catalogs/jobPostOptions';
import {
  applyCompanyExport,
  applyCompanyWorkbenchExport,
  changeCompanyWorkbenchApplicationStage,
  createCompanyWorkbenchInterviewSession,
  createCompanyWorkbenchJob,
  deleteCompanyLogo,
  deleteCompanyStyleImage,
  fetchCompanyApplications,
  fetchCompanyExportDetail,
  fetchCompanyJobs,
  fetchCompanyLogo,
  fetchCompanyLogoBlob,
  fetchCompanyStyleImageBlob,
  fetchCompanyStyleImages,
  fetchCompanyWorkbenchApplications,
  fetchCompanyWorkbenchInterviewSessions,
  fetchCompanyWorkbenchJobs,
  fetchCompanyWorkbenchOverview,
  issueCompanyExportDownloadUrl,
  offlineCompanyWorkbenchJob,
  saveCompanyWorkbenchProfile,
  saveCompanyStyleImageOrder,
  submitCompanyApply,
  submitCompanyWorkbenchCertification,
  submitCompanyWorkbenchJobReview,
  uploadCompanyLogo,
  uploadCompanyStyleImage,
  type CompanyApplication,
  type CompanyExportApply,
  type CompanyInterviewSession,
  type CompanyJob,
  type CompanyLogo,
  type CompanyStyleImage,
  type CompanyStatus,
  type CompanyWorkbenchApplication,
  type CompanyWorkbenchOverview
} from './companyApi';
import styles from './CompanyDashboard.module.css';

type LoadStatus = 'loading' | 'ready' | 'error' | 'retrying';
type WorkbenchMode = 'unknown' | 'enabled' | 'disabled';
type JobTab = 'published' | 'reviewing' | 'offline';
type CompanySection =
  | 'home'
  | 'jobs'
  | 'talentSearch'
  | 'chat'
  | 'applications'
  | 'services'
  | 'profile'
  | 'style'
  | 'recommend'
  | 'jobfair'
  | 'account'
  | 'exports'
  | 'interviews';

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

const jobTabs: Array<{ id: JobTab; label: string; hint: string }> = [
  { id: 'published', label: '发布中', hint: '在线且审核通过' },
  { id: 'reviewing', label: '审核中', hint: '草稿、待审核或驳回' },
  { id: 'offline', label: '已下线', hint: '企业主动关闭' }
];

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

function CapitalAmountField({
  amount,
  unit,
  onAmountChange,
  onUnitChange
}: {
  amount: string;
  unit: string;
  onAmountChange: (value: string) => void;
  onUnitChange: (value: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLLabelElement>(null);
  const unitLabel = dictionaryOptionLabel(companyCapitalUnitOptions, unit);
  useOutsidePointerDown(rootRef, open, () => setOpen(false));

  return (
    <label className={styles.capitalField} ref={rootRef}>
      <span className={styles.capitalLabel}>注册资金</span>
      <span className={open ? styles.capitalControlOpen : styles.capitalControl}>
        <input
          aria-label="注册资金"
          className={styles.capitalInput}
          value={amount}
          onChange={(event) => onAmountChange(event.target.value)}
          placeholder="请输入注册资金"
        />
        <button
          type="button"
          className={styles.capitalUnitButton}
          aria-label={`注册资金单位 ${unitLabel}`}
          aria-haspopup="listbox"
          aria-expanded={open}
          onClick={() => setOpen((current) => !current)}
        >
          <span>{unit ? unitLabel : '请选择'}</span>
          <span aria-hidden="true" className={styles.capitalArrow}>⌃</span>
        </button>
      </span>
      {open ? (
        <div className={styles.capitalMenu} role="listbox">
          {companyCapitalUnitOptions.map((option) => (
            <button
              key={option.value}
              type="button"
              role="option"
              aria-selected={unit === option.value}
              className={unit === option.value ? styles.capitalOptionActive : styles.capitalOption}
              onClick={() => {
                onUnitChange(option.value);
                setOpen(false);
              }}
            >
              {option.label}
            </button>
          ))}
        </div>
      ) : null}
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

function CheckboxField({
  label,
  checked,
  onChange
}: {
  label: string;
  checked: boolean;
  onChange: (checked: boolean) => void;
}) {
  return (
    <label className={styles.checkboxField}>
      <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
      <span>{label}</span>
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

function hasText(value: string | null | undefined): boolean {
  return typeof value === 'string' && value.trim().length > 0;
}

const COMPANY_PROFILE_REQUIRED_FIELDS: Array<{
  label: string;
  value: (profile: CompanyWorkbenchOverview['profile']) => string | null | undefined;
}> = [
  { label: '企业名称', value: (profile) => profile.company_name },
  { label: '企业性质', value: (profile) => profile.nature_code },
  { label: '企业规模', value: (profile) => profile.scale_code },
  { label: '所属行业', value: (profile) => profile.industry_code },
  { label: '所在地区', value: (profile) => profile.city_code },
  { label: '详细地址', value: (profile) => profile.address },
  { label: '企业简介', value: (profile) => profile.company_profile },
  { label: '联系人', value: (profile) => profile.contact_name },
  { label: '联系电话', value: (profile) => profile.contact_mobile }
];

function getMissingCompanyProfileFields(profile: CompanyWorkbenchOverview['profile'] | null | undefined): string[] {
  if (!profile) {
    return COMPANY_PROFILE_REQUIRED_FIELDS.map((field) => field.label);
  }
  return COMPANY_PROFILE_REQUIRED_FIELDS
    .filter((field) => !hasText(field.value(profile)))
    .map((field) => field.label);
}

function isBasicProfileComplete(profile: CompanyWorkbenchOverview['profile'] | null | undefined): boolean {
  return getMissingCompanyProfileFields(profile).length === 0;
}

function jobMatchesTab(job: CompanyJob, tab: JobTab): boolean {
  if (tab === 'published') {
    return job.status === 2 && job.audit_status === 2;
  }
  if (tab === 'offline') {
    return job.status === 3;
  }
  return job.status !== 3 && !(job.status === 2 && job.audit_status === 2);
}

function formatDateText(value: string): string {
  if (!value) {
    return '暂无更新';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' });
}

function optionalNumber(value: string): number | undefined {
  if (!value.trim()) {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
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
  const [scaleCode, setScaleCode] = useState('50-200');
  const [address, setAddress] = useState('上海市示例园区');
  const [companyProfile, setCompanyProfile] = useState('用于灰度试运营的企业资料摘要，公开展示仍以服务端裁剪字段为准。');
  const [registeredCapitalAmount, setRegisteredCapitalAmount] = useState('');
  const [registeredCapitalUnit, setRegisteredCapitalUnit] = useState('cny_10k');
  const [websiteUrl, setWebsiteUrl] = useState('');
  const [benefitCodes, setBenefitCodes] = useState<string[]>([]);
  const [contactName, setContactName] = useState('');
  const [contactMobile, setContactMobile] = useState('');
  const [contactMobileHidden, setContactMobileHidden] = useState(true);
  const [contactWechat, setContactWechat] = useState('');
  const [contactWechatSameMobile, setContactWechatSameMobile] = useState(false);
  const [contactPhone, setContactPhone] = useState('');
  const [contactEmail, setContactEmail] = useState('');
  const [contactQq, setContactQq] = useState('');
  const [materialSummary, setMaterialSummary] = useState('统一社会信用代码已核验；联系人已由企业账号持有人确认。');
  const [jobTitle, setJobTitle] = useState('三期招聘顾问');
  const [jobCityCode, setJobCityCode] = useState('310000');
  const [jobCategoryCode, setJobCategoryCode] = useState('software');
  const [jobNatureCode, setJobNatureCode] = useState('full_time');
  const [jobCategoryName, setJobCategoryName] = useState('互联网/电子商务');
  const [jobExperienceCode, setJobExperienceCode] = useState('1_3_years');
  const [jobEducationCode, setJobEducationCode] = useState('college');
  const [jobRecruitCount, setJobRecruitCount] = useState('3');
  const [jobWorkRegionPath, setJobWorkRegionPath] = useState('上海 / 上海市 / 浦东新区');
  const [jobAddress, setJobAddress] = useState('上海市浦东新区演示大道 100 号');
  const [jobSalaryMin, setJobSalaryMin] = useState('12000');
  const [jobSalaryMax, setJobSalaryMax] = useState('22000');
  const [jobSalaryNegotiable, setJobSalaryNegotiable] = useState(false);
  const [jobWelfareCodes, setJobWelfareCodes] = useState<string[]>(['five_insurance', 'weekend_double']);
  const [jobDepartmentName, setJobDepartmentName] = useState('招聘运营部');
  const [jobAgeMin, setJobAgeMin] = useState('');
  const [jobAgeMax, setJobAgeMax] = useState('');
  const [jobAgeUnlimited, setJobAgeUnlimited] = useState(true);
  const [jobRecruitmentTimeCode, setJobRecruitmentTimeCode] = useState('long_term');
  const [jobContactMode, setJobContactMode] = useState('company_profile');
  const [jobContactName, setJobContactName] = useState('');
  const [jobContactMobile, setJobContactMobile] = useState('');
  const [jobContactPhone, setJobContactPhone] = useState('');
  const [jobContactEmail, setJobContactEmail] = useState('');
  const [jobContactWechat, setJobContactWechat] = useState('');
  const [jobContactHidden, setJobContactHidden] = useState(true);
  const [jobNotifyEnabled, setJobNotifyEnabled] = useState(false);
  const [jobResumeSubscriptionEnabled, setJobResumeSubscriptionEnabled] = useState(false);
  const [jobDesc, setJobDesc] = useState('负责本地人才服务平台灰度试运营支持。');
  const [stageNote, setStageNote] = useState('企业已查看投递，进入面试沟通流程。');
  const [exportReason, setExportReason] = useState('导出本企业投递池候选人摘要，用于招聘流程复核。');
  const [exportIdInput, setExportIdInput] = useState('');
  const [activeSection, setActiveSection] = useState<CompanySection>('home');
  const [showProfileGateModal, setShowProfileGateModal] = useState(false);
  const [jobTab, setJobTab] = useState<JobTab>('published');
  const [selectedJobIds, setSelectedJobIds] = useState<number[]>([]);
  const [showJobCreateForm, setShowJobCreateForm] = useState(false);

  const [workbenchMode, setWorkbenchMode] = useState<WorkbenchMode>('unknown');
  const [overview, setOverview] = useState<CompanyWorkbenchOverview | null>(null);
  const [companyStatus, setCompanyStatus] = useState<CompanyStatus | null>(null);
  const [jobs, setJobs] = useState<CompanyJob[]>([]);
  const [legacyApplications, setLegacyApplications] = useState<CompanyApplication[]>([]);
  const [workbenchApplications, setWorkbenchApplications] = useState<CompanyWorkbenchApplication[]>([]);
  const [interviewSessions, setInterviewSessions] = useState<CompanyInterviewSession[]>([]);
  const [companyLogo, setCompanyLogo] = useState<CompanyLogo | null>(null);
  const [companyLogoUrl, setCompanyLogoUrl] = useState('');
  const [companyLogoMessage, setCompanyLogoMessage] = useState('');
  const [styleImages, setStyleImages] = useState<CompanyStyleImage[]>([]);
  const [styleImageUrls, setStyleImageUrls] = useState<Record<number, string>>({});
  const [styleUploadMessage, setStyleUploadMessage] = useState('');
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
    setRegisteredCapitalAmount(profile.registered_capital_amount || '');
    setRegisteredCapitalUnit(profile.registered_capital_unit || 'cny_10k');
    setWebsiteUrl(profile.website_url || '');
    setBenefitCodes(profile.benefit_codes ?? []);
    setContactName(profile.contact_name || '');
    setContactMobile(profile.contact_mobile || '');
    setContactMobileHidden(profile.contact_mobile_hidden !== false);
    setContactWechat(profile.contact_wechat || '');
    setContactWechatSameMobile(profile.contact_wechat_same_mobile === true);
    setContactPhone(profile.contact_phone || '');
    setContactEmail(profile.contact_email || '');
    setContactQq(profile.contact_qq || '');
    setJobs(jobResult.data.job_list);
    setWorkbenchApplications(applicationResult.data.application_list);
    setInterviewSessions(sessionResult.data.session_list);
    setLegacyApplications([]);
    setWorkbenchMode('enabled');
    setTraceId(sessionResult.traceId || applicationResult.traceId || jobResult.traceId || overviewResult.traceId);
    if (overviewResult.data.features.company_logo_upload_enabled) {
      await loadCompanyLogo();
    } else {
      setCompanyLogo(null);
      setCompanyLogoUrl('');
      setCompanyLogoMessage('企业 Logo 上传暂未开放。');
    }
    if (overviewResult.data.features.company_style_upload_enabled) {
      await loadStyleImages();
    } else {
      setStyleImages([]);
      setStyleImageUrls({});
      setStyleUploadMessage('企业风采上传暂未开放。');
    }
  }

  async function loadCompanyLogo() {
    const result = await fetchCompanyLogo(context.token);
    setCompanyLogo(result.data);
    setTraceId(result.traceId);
    await hydrateCompanyLogoUrl(result.data);
  }

  async function hydrateCompanyLogoUrl(logo: CompanyLogo) {
    if (!logo.has_logo || !logo.content_url) {
      setCompanyLogoUrl((previous) => {
        if (previous) URL.revokeObjectURL(previous);
        return '';
      });
      return;
    }
    try {
      const blob = await fetchCompanyLogoBlob(context.token, logo);
      const nextUrl = URL.createObjectURL(blob);
      setCompanyLogoUrl((previous) => {
        if (previous) URL.revokeObjectURL(previous);
        return nextUrl;
      });
    } catch {
      setCompanyLogoUrl((previous) => {
        if (previous) URL.revokeObjectURL(previous);
        return '';
      });
    }
  }

  async function loadStyleImages() {
    const result = await fetchCompanyStyleImages(context.token);
    setStyleImages(result.data.image_list);
    setTraceId(result.traceId);
    await hydrateStyleImageUrls(result.data.image_list);
  }

  async function hydrateStyleImageUrls(images: CompanyStyleImage[]) {
    const entries = await Promise.all(images.map(async (image) => {
      try {
        const blob = await fetchCompanyStyleImageBlob(context.token, image);
        return [image.image_id, URL.createObjectURL(blob)] as const;
      } catch {
        return [image.image_id, ''] as const;
      }
    }));
    setStyleImageUrls((previous) => {
      Object.values(previous).forEach((url) => {
        if (url) URL.revokeObjectURL(url);
      });
      return Object.fromEntries(entries.filter(([, url]) => Boolean(url)));
    });
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

  useEffect(() => () => {
    Object.values(styleImageUrls).forEach((url) => {
      if (url) URL.revokeObjectURL(url);
    });
  }, [styleImageUrls]);

  useEffect(() => () => {
    if (companyLogoUrl) URL.revokeObjectURL(companyLogoUrl);
  }, [companyLogoUrl]);

  useEffect(() => {
    if (workbenchMode !== 'enabled' || !overview || isBasicProfileComplete(overview.profile)) {
      return;
    }
    if (activeSection !== 'profile' && activeSection !== 'style') {
      setActiveSection('profile');
    }
  }, [activeSection, overview, workbenchMode]);

  useEffect(() => {
    setSelectedJobIds((current) => current.filter((jobId) => jobs.some((job) => job.job_id === jobId)));
  }, [jobs]);

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

  async function runStyleAction(description: string, action: () => Promise<void>) {
    setStatus('retrying');
    setStyleUploadMessage(description);
    try {
      await action();
      await loadStyleImages();
      setStatus('ready');
      setMessage('');
    } catch (error) {
      setStatus('error');
      setTraceId(isHttpClientError(error) ? error.traceId : undefined);
      const fallback = errorMessage(error, description);
      setMessage(fallback);
      setStyleUploadMessage(fallback);
    }
  }

  async function runLogoAction(description: string, action: () => Promise<void>) {
    setStatus('retrying');
    setCompanyLogoMessage(description);
    try {
      await action();
      await loadCompanyLogo();
      setStatus('ready');
      setMessage('');
    } catch (error) {
      setStatus('error');
      setTraceId(isHttpClientError(error) ? error.traceId : undefined);
      const fallback = errorMessage(error, description);
      setMessage(fallback);
      setCompanyLogoMessage(fallback);
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
        company_profile: companyProfile,
        registered_capital_amount: registeredCapitalAmount,
        registered_capital_unit: registeredCapitalUnit,
        website_url: websiteUrl,
        benefit_codes: benefitCodes,
        contact_name: contactName,
        contact_mobile: contactMobile,
        contact_mobile_hidden: contactMobileHidden,
        contact_wechat: contactWechatSameMobile ? contactMobile : contactWechat,
        contact_wechat_same_mobile: contactWechatSameMobile,
        contact_phone: contactPhone,
        contact_email: contactEmail,
        contact_qq: contactQq
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
        registered_capital_amount: registeredCapitalAmount,
        registered_capital_unit: registeredCapitalUnit,
        website_url: websiteUrl,
        benefit_codes: benefitCodes,
        contact_name: contactName,
        contact_mobile: contactMobile,
        contact_mobile_hidden: contactMobileHidden,
        contact_wechat: contactWechatSameMobile ? contactMobile : contactWechat,
        contact_wechat_same_mobile: contactWechatSameMobile,
        contact_phone: contactPhone,
        contact_email: contactEmail,
        contact_qq: contactQq,
        certification_material_summary: {
          material_summary: materialSummary,
          submitted_by: 'company_account'
        }
      });
    });
  }

  async function onUploadStyleImage(file: File | undefined) {
    if (!file) {
      return;
    }
    await runStyleAction('正在上传企业风采图片。', async () => {
      await uploadCompanyStyleImage(context.token, file);
      setStyleUploadMessage('企业风采图片已上传，仅在企业私有域可见。');
    });
  }

  async function onUploadCompanyLogo(file: File | undefined) {
    if (!file) {
      return;
    }
    await runLogoAction('正在上传企业 Logo。', async () => {
      await uploadCompanyLogo(context.token, file);
      setCompanyLogoMessage('企业 Logo 已上传，仅在企业私有域可见。');
    });
  }

  async function onDeleteCompanyLogo() {
    await runLogoAction('正在删除企业 Logo。', async () => {
      await deleteCompanyLogo(context.token);
      setCompanyLogoMessage('企业 Logo 已删除。');
    });
  }

  async function onDeleteStyleImage(imageId: number) {
    await runStyleAction('正在删除企业风采图片。', async () => {
      await deleteCompanyStyleImage(context.token, imageId);
      setStyleUploadMessage('企业风采图片已删除。');
    });
  }

  async function onMoveStyleImage(imageId: number, direction: -1 | 1) {
    const index = styleImages.findIndex((image) => image.image_id === imageId);
    const targetIndex = index + direction;
    if (index < 0 || targetIndex < 0 || targetIndex >= styleImages.length) {
      return;
    }
    const next = [...styleImages];
    [next[index], next[targetIndex]] = [next[targetIndex], next[index]];
    await runStyleAction('正在保存企业风采排序。', async () => {
      await saveCompanyStyleImageOrder(context.token, next.map((image) => image.image_id));
      setStyleUploadMessage('企业风采排序已保存。');
    });
  }

  function jobPayload() {
    return {
      title: jobTitle,
      job_nature_code: jobNatureCode,
      category_code: jobCategoryCode,
      category_name: jobCategoryName,
      experience_code: jobExperienceCode,
      education_code: jobEducationCode,
      recruit_count: optionalNumber(jobRecruitCount),
      city_code: jobCityCode,
      work_region_path: jobWorkRegionPath,
      address: jobAddress,
      salary_min: jobSalaryNegotiable ? undefined : optionalNumber(jobSalaryMin),
      salary_max: jobSalaryNegotiable ? undefined : optionalNumber(jobSalaryMax),
      salary_negotiable: jobSalaryNegotiable,
      welfare_codes: jobWelfareCodes,
      department_name: jobDepartmentName,
      age_min: jobAgeUnlimited ? undefined : optionalNumber(jobAgeMin),
      age_max: jobAgeUnlimited ? undefined : optionalNumber(jobAgeMax),
      age_unlimited: jobAgeUnlimited,
      recruitment_time_code: jobRecruitmentTimeCode,
      contact_mode: jobContactMode,
      contact_name: jobContactName,
      contact_mobile: jobContactMobile,
      contact_phone: jobContactPhone,
      contact_email: jobContactEmail,
      contact_wechat: jobContactWechat,
      contact_hidden: jobContactHidden,
      notify_enabled: jobNotifyEnabled,
      resume_subscription_enabled: jobResumeSubscriptionEnabled,
      job_desc: jobDesc
    };
  }

  function loadJobIntoForm(job: CompanyJob) {
    setJobTitle(job.title);
    setJobNatureCode(job.job_nature_code || 'full_time');
    setJobCategoryCode(job.category_code || 'software');
    setJobCategoryName(job.category_name || '');
    setJobExperienceCode(job.experience_code || 'none');
    setJobEducationCode(job.education_code || 'none');
    setJobRecruitCount(job.recruit_count == null ? '' : String(job.recruit_count));
    setJobCityCode(job.city_code || '310000');
    setJobWorkRegionPath(job.work_region_path || '');
    setJobAddress(job.address || '');
    setJobSalaryMin(job.salary_min == null ? '' : String(job.salary_min));
    setJobSalaryMax(job.salary_max == null ? '' : String(job.salary_max));
    setJobSalaryNegotiable(job.salary_negotiable);
    setJobWelfareCodes(job.welfare_codes ?? []);
    setJobDepartmentName(job.department_name || '');
    setJobAgeMin(job.age_min == null ? '' : String(job.age_min));
    setJobAgeMax(job.age_max == null ? '' : String(job.age_max));
    setJobAgeUnlimited(job.age_unlimited);
    setJobRecruitmentTimeCode(job.recruitment_time_code || 'long_term');
    setJobContactMode(job.contact_mode || 'company_profile');
    setJobContactName(job.contact_name || '');
    setJobContactMobile(job.contact_mobile || '');
    setJobContactPhone(job.contact_phone || '');
    setJobContactEmail(job.contact_email || '');
    setJobContactWechat(job.contact_wechat || '');
    setJobContactHidden(job.contact_hidden);
    setJobNotifyEnabled(job.notify_enabled);
    setJobResumeSubscriptionEnabled(job.resume_subscription_enabled);
    setJobDesc(job.job_desc || jobDesc);
  }

  async function onCreateJob(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await runAction('正在创建企业职位草稿。', async () => {
      await createCompanyWorkbenchJob(context.token, jobPayload());
    });
    setJobTab('reviewing');
  }

  async function onSubmitJobReview(jobId: number) {
    if (!jobId) {
      return;
    }
    await runAction('正在提交职位审核。', async () => {
      await submitCompanyWorkbenchJobReview(context.token, jobId);
    });
  }

  async function onOfflineJob(jobId: number) {
    if (!jobId) {
      return;
    }
    await runAction('正在下线职位。', async () => {
      await offlineCompanyWorkbenchJob(context.token, jobId, '企业工作台主动下线。');
    });
    setJobTab('offline');
  }

  async function onSubmitFirstJobReview() {
    const firstJob = jobs[0];
    if (!firstJob) {
      return;
    }
    await onSubmitJobReview(firstJob.job_id);
  }

  async function onOfflineFirstJob() {
    const firstJob = jobs[0];
    if (!firstJob) {
      return;
    }
    await onOfflineJob(firstJob.job_id);
  }

  async function onOfflineSelectedJobs() {
    const jobIds = selectedJobIds.filter((jobId) => jobs.some((job) => job.job_id === jobId));
    if (jobIds.length === 0) {
      return;
    }
    await runAction('正在批量关闭所选职位。', async () => {
      for (const jobId of jobIds) {
        await offlineCompanyWorkbenchJob(context.token, jobId, '企业工作台批量关闭。');
      }
    });
    setSelectedJobIds([]);
    setJobTab('offline');
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
  const profile = overview?.profile ?? null;
  const missingProfileFields = workbenchEnabled ? getMissingCompanyProfileFields(profile) : [];
  const basicProfileComplete = !workbenchEnabled || missingProfileFields.length === 0;
  const gateLocked = workbenchEnabled && Boolean(overview) && !basicProfileComplete;

  const menuItems: Array<{
    id: CompanySection;
    label: string;
    disabledWhenLocked?: boolean;
    risk?: boolean;
    children?: Array<{ id: CompanySection; label: string; disabledWhenLocked?: boolean }>;
  }> = [
    { id: 'home', label: '会员首页', disabledWhenLocked: true },
    { id: 'jobs', label: '职位管理', disabledWhenLocked: true },
    { id: 'talentSearch', label: '搜索简历', disabledWhenLocked: true, risk: true },
    { id: 'chat', label: '我的职聊', disabledWhenLocked: true, risk: true },
    { id: 'applications', label: '简历管理', disabledWhenLocked: true },
    { id: 'services', label: '会员服务', disabledWhenLocked: true, risk: true },
    {
      id: 'profile',
      label: '企业管理',
      children: [
        { id: 'profile', label: '基本资料' },
        { id: 'style', label: '企业风采' }
      ]
    },
    { id: 'recommend', label: '智能推荐', disabledWhenLocked: true, risk: true },
    { id: 'jobfair', label: '招聘会', disabledWhenLocked: true, risk: true },
    { id: 'account', label: '账号管理', disabledWhenLocked: true, risk: true }
  ];

  function canOpenSection(section: CompanySection, disabledWhenLocked = true): boolean {
    if (!gateLocked) {
      return true;
    }
    if (section === 'profile' || section === 'style') {
      return true;
    }
    return !disabledWhenLocked;
  }

  function chooseSection(section: CompanySection, disabledWhenLocked = true) {
    if (!canOpenSection(section, disabledWhenLocked)) {
      setActiveSection('profile');
      setShowProfileGateModal(true);
      return;
    }
    setActiveSection(section);
  }

  function renderMenu() {
    return (
      <aside className={styles.sidebar} aria-label="企业会员中心菜单">
        <div className={styles.sidebarHeader}>
          <strong>企业会员中心</strong>
          <span>{context.identity.display_name ?? '招聘者账号'}</span>
        </div>
        <nav className={styles.menu}>
          {menuItems.map((item) => {
            const itemLocked = !canOpenSection(item.id, item.disabledWhenLocked);
            const isParentActive = item.children
              ? item.children.some((child) => child.id === activeSection)
              : item.id === activeSection;
            return (
              <div key={item.label}>
                <button
                  type="button"
                  className={[
                    styles.menuItem,
                    isParentActive ? styles.menuItemActive : '',
                    itemLocked ? styles.menuItemLocked : ''
                  ].join(' ')}
                  aria-disabled={itemLocked}
                  onClick={() => chooseSection(item.id, item.disabledWhenLocked)}
                >
                  <span>{item.label}</span>
                  {itemLocked ? <span className={styles.lock}>锁定</span> : item.risk ? <span className={styles.lock}>占位</span> : null}
                </button>
                {item.children ? (
                  <div className={styles.submenu}>
                    {item.children.map((child) => (
                      <button
                        key={child.id}
                        type="button"
                        className={[
                          styles.submenuItem,
                          activeSection === child.id ? styles.submenuItemActive : ''
                        ].join(' ')}
                        onClick={() => chooseSection(child.id, child.disabledWhenLocked)}
                      >
                        {child.label}
                      </button>
                    ))}
                  </div>
                ) : null}
              </div>
            );
          })}
        </nav>
      </aside>
    );
  }

  function renderStatusView() {
    if (status === 'error') {
      return (
        <section className={styles.card}>
          <StateView variant="error" title="企业工作台操作失败" description={message} retryLabel="重新读取" onRetry={load} />
        </section>
      );
    }
    if (status === 'retrying' || status === 'loading') {
      return (
        <section className={styles.card}>
          <StateView variant={status === 'loading' ? 'loading' : 'retrying'} title="企业工作台处理中" description={message} />
        </section>
      );
    }
    return null;
  }

  function renderProfileForm() {
    const logoUploadEnabled = overview?.features.company_logo_upload_enabled === true;
    return (
      <section className={styles.card}>
        <div className={styles.cardTitle}>
          <div>
            <h2>企业资料</h2>
            <p className={styles.muted}>
              请先完善基本资料。保存后页面会重新读取服务端 profile，只有服务端返回完整资料后才解锁其它菜单。
            </p>
          </div>
          {companyStatus ? (
            <p style={{ margin: 0 }}>
              <StatusBadge kind="company_auth" value={companyStatus.auth_status} />
            </p>
          ) : null}
        </div>

        {gateLocked ? (
          <div className={styles.noticeBar}>
            根据相关法律法规要求，需要您完善企业基本资料后才能使用职位、投递、面试等企业中心功能。
          </div>
        ) : null}

        <form onSubmit={workbenchEnabled ? onSaveProfile : onLegacyApply} className={styles.profileGrid}>
          <div className={styles.logoPanel}>
            {companyLogo?.has_logo && companyLogoUrl ? (
              <img src={companyLogoUrl} alt="企业 Logo" className={styles.logoImagePreview} />
            ) : (
              <div className={styles.logoPreview}>{(companyName || '企').slice(0, 1)}</div>
            )}
            <label className={styles.logoUploadButton}>
              <span>{companyLogo?.has_logo ? '替换 logo' : '+ 上传logo'}</span>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                disabled={!logoUploadEnabled}
                onChange={(event) => {
                  const file = event.currentTarget.files?.[0];
                  event.currentTarget.value = '';
                  void onUploadCompanyLogo(file);
                }}
              />
            </label>
            {companyLogo?.has_logo ? (
              <button type="button" className={styles.placeholderButton} onClick={() => void onDeleteCompanyLogo()}>
                删除 Logo
              </button>
            ) : null}
            <button type="button" className={styles.placeholderButton} disabled style={{ marginTop: 10 }}>
              生成 LOGO（占位）
            </button>
            <button type="button" className={styles.placeholderButton} disabled>
              扫码上传（占位）
            </button>
            <p className={styles.muted} style={{ marginBottom: 0 }}>
              建议尺寸 120×120。Logo 仅在企业私有域预览，不进入公开企业主页。
            </p>
            {companyLogoMessage ? <p className={styles.muted}>{companyLogoMessage}</p> : null}
          </div>

          <div>
            <div className={styles.formGrid}>
              <div className={styles.formSectionTitle}>基本信息</div>
              <div className={styles.formWide}>
                <Field label="企业名称 *" value={companyName} onChange={setCompanyName} />
              </div>
              <DictionarySelect
                label="企业性质 *"
                value={natureCode}
                options={companyNatureOptions}
                onChange={setNatureCode}
              />
              <DictionarySelect
                label="企业规模 *"
                value={scaleCode}
                options={companyScaleOptions}
                onChange={setScaleCode}
              />
              <DictionarySelect
                label="所属行业 *"
                value={industryCode}
                options={companyIndustryOptions}
                onChange={setIndustryCode}
              />
              <CapitalAmountField
                amount={registeredCapitalAmount}
                unit={registeredCapitalUnit}
                onAmountChange={setRegisteredCapitalAmount}
                onUnitChange={setRegisteredCapitalUnit}
              />
              <div className={styles.formWide}>
                <Field label="企业网址" value={websiteUrl} onChange={setWebsiteUrl} placeholder="http://" />
              </div>
              <div className={styles.formWide}>
                <DictionaryMultiSelect
                  label="企业福利"
                  values={benefitCodes}
                  options={companyBenefitOptions}
                  onChange={setBenefitCodes}
                  placeholder="请选择企业福利"
                  max={12}
                />
              </div>
              <RegionCascadePicker
                mode="single"
                label="所在地区 *"
                value={cityCode}
                onChange={setCityCode}
                dialogLabel="所在地选择"
              />
              <div className={styles.addressWithMap}>
                <Field label="详细地址 *" value={address} onChange={setAddress} />
                <button type="button" className={styles.mapPlaceholderButton} disabled>标注（地图占位）</button>
              </div>
              <div className={styles.formWide}>
                <TextAreaField label="企业简介 *" value={companyProfile} onChange={setCompanyProfile} rows={5} />
                <button type="button" className={styles.aiPlaceholderButton} disabled>AI一键优化（占位）</button>
              </div>

              <div className={styles.formSectionTitle}>联系方式</div>
              <Field label="联系人 *" value={contactName} onChange={setContactName} placeholder="请输入联系人" />
              <div>
                <Field label="联系电话 *" value={contactMobile} onChange={setContactMobile} placeholder="请输入联系电话" />
                <CheckboxField label="不对外显示，仅接收站内通知" checked={contactMobileHidden} onChange={setContactMobileHidden} />
              </div>
              <div>
                <Field
                  label="联系微信"
                  value={contactWechatSameMobile ? contactMobile : contactWechat}
                  onChange={setContactWechat}
                  placeholder="请输入联系微信"
                />
                <CheckboxField label="同手机号" checked={contactWechatSameMobile} onChange={setContactWechatSameMobile} />
              </div>
              <Field label="联系固话" value={contactPhone} onChange={setContactPhone} placeholder="如 021-88889999" />
              <Field label="联系邮箱" value={contactEmail} onChange={setContactEmail} placeholder="hr@example.com" />
              <Field label="联系 QQ" value={contactQq} onChange={setContactQq} placeholder="请输入联系 QQ" />

              <div className={styles.formSectionTitle}>认证资料</div>
              <div className={styles.formWide}>
                <Field label="统一社会信用代码" value={licenseNo} onChange={setLicenseNo} />
              </div>
              {workbenchEnabled ? (
                <div className={styles.formWide}>
                  <TextAreaField label="认证摘要（不含附件 key）" value={materialSummary} onChange={setMaterialSummary} rows={3} />
                </div>
              ) : null}
            </div>
            <div className={styles.actionRow}>
              <button type="submit" className={styles.primaryButton}>{workbenchEnabled ? '保存企业资料' : '提交认证资料'}</button>
              {workbenchEnabled ? (
                <button type="button" className={styles.secondaryButton} onClick={() => void submitCertification()}>
                  提交认证摘要
                </button>
              ) : null}
              {companyStatus?.reject_reason ? <span className={styles.muted}>驳回原因：{companyStatus.reject_reason}</span> : null}
            </div>
          </div>
        </form>
      </section>
    );
  }

  function renderCompanyStyle() {
    const styleUploadEnabled = overview?.features.company_style_upload_enabled === true;
    const canUploadMoreStyleImages = styleImages.length < 6;
    return (
      <section className={styles.card}>
        <div className={styles.cardTitle}>
          <div>
            <h2>企业风采</h2>
            <p className={styles.muted}>
              最多可上传 6 张企业风采图片。当前仅在企业私有域管理，不进入公开企业主页、推荐位、搜索或 sitemap。
            </p>
          </div>
          <span className={styleUploadEnabled ? styles.enabledTag : styles.disabledTag}>
            {styleUploadEnabled ? '私有上传已开启' : '暂未开放'}
          </span>
        </div>
        <div className={`${styles.noticeBar} ${styles.styleNoticeBar}`}>
          <span>最多可上传 6 张企业风采图片</span>
          <span>手机扫码上传待后续开放</span>
        </div>
        {styleUploadMessage ? <p className={styles.muted}>{styleUploadMessage}</p> : null}
        <div className={styles.styleUploadGrid}>
          {styleImages.map((image, index) => (
            <div className={`${styles.styleUploadCard} ${styles.styleImageCard}`} key={image.image_id}>
              {styleImageUrls[image.image_id] ? (
                <img src={styleImageUrls[image.image_id]} alt={`企业风采 ${index + 1}`} className={styles.styleImagePreview} />
              ) : (
                <div className={styles.styleImagePlaceholder}>预览读取中</div>
              )}
              <div className={styles.styleImageMeta}>
                <strong>{image.file_name}</strong>
                <span>{Math.ceil(image.size_bytes / 1024)} KB · {image.content_type}</span>
                <span>审核状态：私有待审</span>
              </div>
              <div className={styles.styleImageActions}>
                <button type="button" onClick={() => void onMoveStyleImage(image.image_id, -1)} disabled={index === 0}>上移</button>
                <button type="button" onClick={() => void onMoveStyleImage(image.image_id, 1)} disabled={index === styleImages.length - 1}>下移</button>
                <button type="button" onClick={() => void onDeleteStyleImage(image.image_id)}>删除</button>
              </div>
            </div>
          ))}
          {canUploadMoreStyleImages ? (
            <label className={`${styles.styleUploadCard} ${styles.styleUploadInput}`}>
              <strong>+ 上传风采</strong>
              <span>{styleUploadEnabled ? '支持 JPG / PNG / WebP，5MiB 内' : '上传暂未开放'}</span>
              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                disabled={!styleUploadEnabled}
                onChange={(event) => {
                  const file = event.currentTarget.files?.[0];
                  event.currentTarget.value = '';
                  void onUploadStyleImage(file);
                }}
              />
            </label>
          ) : (
            <p className={styles.styleFullHint}>已上传 6 张企业风采图片。如需新增，请先删除或替换已有图片。</p>
          )}
        </div>
        <p className={styles.muted}>
          安全边界：前端仅通过后端鉴权代理读取图片，不接收 MinIO object key 或预签名 URL；公开展示和图片审核属于后续专项。
        </p>
      </section>
    );
  }

  function renderHome() {
    const currentProfile = overview?.profile;
    const profileInitial = (currentProfile?.company_name || companyName || '企').slice(0, 1);
    const companyNatureLabel = dictionaryOptionLabel(companyNatureOptions, currentProfile?.nature_code || natureCode);
    const companyScaleLabel = dictionaryOptionLabel(companyScaleOptions, currentProfile?.scale_code || scaleCode);
    const companyIndustryLabel = dictionaryOptionLabel(companyIndustryOptions, currentProfile?.industry_code || industryCode);
    const stats = overview?.stats;
    const activeJobTotal = jobs.filter((job) => job.status === 2).length;
    const reviewJobTotal = jobs.filter((job) => job.audit_status === 1).length;
    const pendingApplications = stats?.pending_application_total ?? 0;
    const totalApplications = stats?.application_total ?? applicationRows.length;
    const interviewTotal = stats?.interview_total ?? interviewSessions.length;
    const jobTotal = stats?.job_total ?? jobs.length;

    const talentItems = [
      { label: '新招呼', value: 0, action: '去查看', section: 'chat' as CompanySection },
      { label: '待查看', value: pendingApplications, action: '去查看', section: 'applications' as CompanySection },
      { label: '待处理', value: pendingApplications, action: '去处理', section: 'applications' as CompanySection },
      { label: '面试邀请', value: interviewTotal, action: '去查看', section: 'interviews' as CompanySection }
    ];

    const chanceItems = [
      { label: '我的收藏', value: 0, helper: '职位与人才收藏暂为灰度占位' },
      { label: '看过我', value: 0, helper: '不开放候选人联系方式' },
      { label: '我看过', value: 0, helper: '不形成公共简历库' },
      { label: '智能推荐', value: 0, helper: '风险池占位' }
    ];

    const jobItems = [
      { label: '在招职位', value: activeJobTotal, action: '职位管理', section: 'jobs' as CompanySection },
      { label: '审核中', value: reviewJobTotal, action: '去查看', section: 'jobs' as CompanySection },
      { label: '收到投递', value: totalApplications, action: '去处理', section: 'applications' as CompanySection },
      { label: '导出申请', value: stats?.export_total ?? 0, action: '看状态', section: 'exports' as CompanySection }
    ];

    const packageItems = [
      { label: '可发职位数', value: '灰度不限', action: '发布职位', section: 'jobs' as CompanySection, risk: false },
      { label: '简历下载点数', value: '审批后可用', action: '导出申请', section: 'exports' as CompanySection, risk: false },
      { label: '聊聊剩余次数', value: '站内占位', action: '我的职聊', section: 'chat' as CompanySection, risk: true },
      { label: '每天免费刷新数', value: '3', action: '一键刷新', section: 'services' as CompanySection, risk: true },
      { label: '我的积分', value: '灰度占位', action: '去赚积分', section: 'services' as CompanySection, risk: true }
    ];

    const quickMenus = [
      { icon: '签', label: '今日签到', section: 'services' as CompanySection, risk: true },
      { icon: '发', label: '发布职位', section: 'jobs' as CompanySection },
      { icon: '职', label: '职位管理', section: 'jobs' as CompanySection },
      { icon: '刷', label: '一键刷新', section: 'services' as CompanySection, risk: true },
      { icon: '才', label: '搜索人才', section: 'talentSearch' as CompanySection, risk: true },
      { icon: '套', label: '我的套餐', section: 'services' as CompanySection, risk: true },
      { icon: '增', label: '增值服务', section: 'services' as CompanySection, risk: true },
      { icon: '顶', label: '职位置顶', section: 'services' as CompanySection, risk: true },
      { icon: '急', label: '紧急招聘', section: 'services' as CompanySection, risk: true },
      { icon: '推', label: '智能推荐', section: 'recommend' as CompanySection, risk: true },
      { icon: '券', label: '优惠券', section: 'services' as CompanySection, risk: true },
      { icon: '分', label: '我的积分', section: 'services' as CompanySection, risk: true },
      { icon: '单', label: '我的订单', section: 'services' as CompanySection, risk: true },
      { icon: '聊', label: '我的职聊', section: 'chat' as CompanySection, risk: true },
      { icon: '会', label: '招聘会', section: 'jobfair' as CompanySection, risk: true },
      { icon: '海', label: '朋友圈海报', section: 'services' as CompanySection, risk: true },
      { icon: '页', label: '我的主页', section: 'profile' as CompanySection }
    ];

    return (
      <>
        {workbenchEnabled && overview ? (
          <div className={styles.memberHome}>
            <div className={styles.memberMain}>
              <div className={styles.memberHomeTitle}>
                <h2>企业会员首页</h2>
                <span>招聘者企业工作台首页</span>
              </div>
              <section className={`${styles.card} ${styles.companyHomeCard}`}>
                <div className={styles.companyHomeHeader}>
                  <div className={styles.companyHomeLogo}>
                    {companyLogo?.has_logo && companyLogoUrl ? (
                      <img src={companyLogoUrl} alt="企业 Logo" />
                    ) : (
                      <span>{profileInitial}</span>
                    )}
                  </div>
                  <div className={styles.companyHomeInfo}>
                    <div className={styles.companyHomeTitleRow}>
                      <h2>我的公司</h2>
                      <StatusBadge kind="company_auth" value={overview.profile.auth_status} />
                    </div>
                    <strong>{overview.profile.company_name || '待完善企业名称'}</strong>
                    <p>
                      {[companyNatureLabel, companyIndustryLabel, companyScaleLabel].filter(Boolean).join(' / ') || '企业资料待完善'}
                    </p>
                    <p className={styles.muted}>trace_id：{traceId || '等待服务端返回'}</p>
                  </div>
                  <div className={styles.companyHomeActions}>
                    <button type="button" className={styles.primaryButton} onClick={() => chooseSection('profile')}>
                      编辑资料
                    </button>
                    {overview.profile.auth_status === 2 ? (
                      <a className={styles.outlineLink} href={`/companies/${overview.profile.company_id}`}>我的主页</a>
                    ) : (
                      <span className={styles.disabledInline}>认证后开放主页预览</span>
                    )}
                  </div>
                </div>
              </section>

              <div className={styles.memberMetricGrid}>
                <section className={`${styles.card} ${styles.memberMetricCard}`}>
                  <div className={styles.memberBlockTitle}>
                    <h3>我的人才</h3>
                    <button type="button" onClick={() => chooseSection('talentSearch')}>搜索人才</button>
                  </div>
                  <div className={styles.memberMiniGrid}>
                    {talentItems.map((item) => (
                      <button key={item.label} type="button" className={styles.memberMiniItem} onClick={() => chooseSection(item.section)}>
                        <strong>{item.value}</strong>
                        <span>{item.label}</span>
                        <em>{item.action}</em>
                      </button>
                    ))}
                  </div>
                </section>

                <section className={`${styles.card} ${styles.memberMetricCard}`}>
                  <div className={styles.memberBlockTitle}>
                    <h3>我的机会</h3>
                    <button type="button" onClick={() => chooseSection('recommend')}>智能推荐</button>
                  </div>
                  <div className={styles.memberMiniGrid}>
                    {chanceItems.map((item) => (
                      <button key={item.label} type="button" className={styles.memberMiniItem} onClick={() => chooseSection('recommend')}>
                        <strong>{item.value}</strong>
                        <span>{item.label}</span>
                        <em>{item.helper}</em>
                      </button>
                    ))}
                  </div>
                </section>

                <section className={`${styles.card} ${styles.memberMetricCard}`}>
                  <div className={styles.memberBlockTitle}>
                    <h3>招聘岗位</h3>
                    <button type="button" onClick={() => chooseSection('jobs')}>发布职位</button>
                  </div>
                  <div className={styles.memberMiniGrid}>
                    {jobItems.map((item) => (
                      <button key={item.label} type="button" className={styles.memberMiniItem} onClick={() => chooseSection(item.section)}>
                        <strong>{item.value}</strong>
                        <span>{item.label}</span>
                        <em>{item.action}</em>
                      </button>
                    ))}
                  </div>
                </section>
              </div>

              <section className={`${styles.card} ${styles.packageCard}`}>
                <div className={styles.memberBlockTitle}>
                  <div>
                    <h3>我的套餐</h3>
                    <p className={styles.muted}>灰度试用套餐：用于演示企业工作台，不产生真实会员权益或计费。</p>
                  </div>
                  <button type="button" onClick={() => chooseSection('services')}>立即升级（占位）</button>
                </div>
                <div className={styles.packageRibbon}>
                  <strong>试用套餐</strong>
                  <span>无限期</span>
                  <em>欢迎加入试用套餐！升级享会员高价值服务（商业化暂未开放）</em>
                </div>
                <div className={styles.packageBenefitGrid}>
                  {packageItems.map((item) => (
                    <button key={item.label} type="button" className={styles.packageBenefitItem} onClick={() => chooseSection(item.section)}>
                      <span>{item.label}</span>
                      <strong>{item.value}</strong>
                      <em>{item.risk ? `${item.action} · 风险池占位` : item.action}</em>
                    </button>
                  ))}
                </div>
              </section>

              <section className={`${styles.card} ${styles.recommendTalentCard}`}>
                <div className={styles.memberBlockTitle}>
                  <div>
                    <h3>人才推荐</h3>
                    <p className={styles.muted}>根据职位与投递情况展示安全摘要。本轮不开放公共简历库、联系解锁或候选人搜索。</p>
                  </div>
                  <button type="button" onClick={() => chooseSection('jobs')}>选择职位</button>
                </div>
                <div className={styles.recommendEmpty}>
                  <strong>暂无可推荐人才</strong>
                  <span>请先发布并审核通过职位，系统仅会展示本企业合规来源的投递摘要。</span>
                </div>
              </section>
            </div>

            <aside className={styles.memberAside}>
              <section className={`${styles.card} ${styles.asideCard}`}>
                <div className={styles.memberBlockTitle}>
                  <h3>我的信息</h3>
                  <button type="button" onClick={() => chooseSection('profile')}>编辑资料</button>
                </div>
                <div className={styles.accountProfile}>
                  <div className={styles.accountAvatar}>{(context.identity.display_name || '企').slice(0, 1)}</div>
                  <div>
                    <strong>{context.identity.display_name ?? '招聘者账号'}</strong>
                    <p className={styles.muted}>认证状态：{overview.profile.auth_status === 2 ? '已认证' : '待认证'}</p>
                    <p className={styles.muted}>绑定状态：站内账号已绑定；外部微信/小程序/App 均为占位。</p>
                  </div>
                </div>
              </section>

              <section className={`${styles.card} ${styles.asideCard}`}>
                <div className={styles.memberBlockTitle}>
                  <h3>我的会员</h3>
                  <button type="button" onClick={() => chooseSection('services')}>升级套餐</button>
                </div>
                <div className={styles.vipMiniCard}>
                  <strong>试用套餐</strong>
                  <span>无限期</span>
                  <p>会员商业化、真实支付、发票与订单暂未开放。</p>
                </div>
              </section>

              <section className={`${styles.card} ${styles.asideCard}`}>
                <div className={styles.memberBlockTitle}>
                  <h3>快捷菜单</h3>
                </div>
                <div className={styles.quickMenuGrid}>
                  {quickMenus.map((item) => (
                    <button key={item.label} type="button" onClick={() => chooseSection(item.section)}>
                      <span>{item.icon}</span>
                      <strong>{item.label}</strong>
                      {item.risk ? <em>占位</em> : null}
                    </button>
                  ))}
                </div>
              </section>

              <section className={`${styles.card} ${styles.asideCard}`}>
                <div className={styles.memberBlockTitle}>
                  <h3>专属客服</h3>
                  <button type="button" onClick={() => chooseSection('services')}>投诉客服</button>
                </div>
                <div className={styles.customerCard}>
                  <strong>暂未分配客服</strong>
                  <span>18003431191（演示占位）</span>
                  <p>仅展示站内服务占位，不接真实短信、微信、小程序或 App。</p>
                </div>
              </section>
            </aside>
          </div>
        ) : null}
        {workbenchMode === 'disabled' ? (
          <>
            <section className={styles.card}>
              <h2>三期企业工作台未开启</h2>
              <p className={styles.muted}>
                服务端返回 company_workbench_enabled=false，本页保留二期企业后台摘要。企业资料维护、职位工作台、投递阶段流转和站内面试邀约需要灰度开关开启后使用。
              </p>
            </section>
            {renderJobs()}
            {renderApplications()}
          </>
        ) : null}
        <section className={styles.card}>
          <div className={styles.cardTitle}>
            <h3>快捷入口</h3>
          </div>
          <div className={styles.serviceGrid}>
            <button type="button" className={styles.serviceCard} onClick={() => chooseSection('profile')}>
              <strong>完善企业资料</strong>
              <span className={styles.muted}>企业管理 / 基本资料</span>
            </button>
            <button type="button" className={styles.serviceCard} onClick={() => chooseSection('jobs')}>
              <strong>职位管理</strong>
              <span className={styles.muted}>创建、送审、下线</span>
            </button>
            <button type="button" className={styles.serviceCard} onClick={() => chooseSection('applications')}>
              <strong>投递池</strong>
              <span className={styles.muted}>处理本企业投递</span>
            </button>
            <button type="button" className={styles.serviceCard} onClick={() => chooseSection('exports')}>
              <strong>导出申请</strong>
              <span className={styles.muted}>继续走审批链</span>
            </button>
          </div>
        </section>
      </>
    );
  }

  function renderJobs() {
    if (!workbenchEnabled) {
      return (
        <section className={styles.card}>
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
        </section>
      );
    }

    const visibleJobs = jobs.filter((job) => jobMatchesTab(job, jobTab));
    const selectedVisibleIds = visibleJobs.filter((job) => selectedJobIds.includes(job.job_id)).map((job) => job.job_id);
    const allVisibleSelected = visibleJobs.length > 0 && selectedVisibleIds.length === visibleJobs.length;
    const publishedCount = jobs.filter((job) => jobMatchesTab(job, 'published')).length;
    const reviewingCount = jobs.filter((job) => jobMatchesTab(job, 'reviewing')).length;
    const offlineCount = jobs.filter((job) => jobMatchesTab(job, 'offline')).length;
    const tabCounts: Record<JobTab, number> = {
      published: publishedCount,
      reviewing: reviewingCount,
      offline: offlineCount
    };
    const guardedJobTotal = publishedCount + reviewingCount;

    function countApplicationsForJob(job: CompanyJob): number {
      if (workbenchEnabled) {
        return workbenchApplications.filter((application) => application.job_id === job.job_id).length;
      }
      return legacyApplications.filter((application) => application.job_title === job.title).length;
    }

    function toggleJob(jobId: number, checked: boolean) {
      setSelectedJobIds((current) => {
        if (checked) {
          return current.includes(jobId) ? current : [...current, jobId];
        }
        return current.filter((id) => id !== jobId);
      });
    }

    function toggleVisibleJobs(checked: boolean) {
      const visibleIds = visibleJobs.map((job) => job.job_id);
      setSelectedJobIds((current) => {
        if (checked) {
          return Array.from(new Set([...current, ...visibleIds]));
        }
        return current.filter((jobId) => !visibleIds.includes(jobId));
      });
    }

    return (
      <section className={`${styles.card} ${styles.jobManagementCard}`}>
        <div className={styles.jobToolbar}>
          <div>
            <p className={styles.eyebrow}>职位工作台</p>
            <h2>管理职位</h2>
            <span>发布、审核、下线状态全部由服务端返回；推广能力仅做灰度占位。</span>
          </div>
          <button
            type="button"
            className={styles.primaryButton}
            onClick={() => setShowJobCreateForm((value) => !value)}
          >
            + 发布职位
          </button>
        </div>

        <div className={styles.jobTabs} role="tablist" aria-label="职位状态筛选">
          {jobTabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              role="tab"
              aria-selected={jobTab === tab.id}
              className={`${styles.jobTab} ${jobTab === tab.id ? styles.jobTabActive : ''}`}
              onClick={() => {
                setJobTab(tab.id);
                setSelectedJobIds([]);
              }}
            >
              <strong>{tab.label}</strong>
              <span>{tab.hint}</span>
              <em>{tabCounts[tab.id]}</em>
            </button>
          ))}
        </div>

        <div className={styles.jobQuotaAlert}>
          <strong>亲爱的 HR，您的账号可同时发布 1 个职位。</strong>
          <span>现已占用 {guardedJobTotal} 个名额（包含审核中和未通过职位）；本提示仅为灰度演示，不产生真实套餐权益。</span>
        </div>

        {showJobCreateForm ? (
          <form onSubmit={onCreateJob} className={`${styles.formGrid} ${styles.jobQuickForm}`}>
            <div className={styles.formWide}>
              <div className={styles.cardTitle}>
                <h3>发布职位（基础草稿）</h3>
                <button type="button" className={styles.plainButton} onClick={() => setShowJobCreateForm(false)}>收起</button>
              </div>
              <p className={styles.muted}>职位字段会保存到数据库；地图标注、AI 生成、职位推广和短信微信通知仍为占位或站内配置。</p>
            </div>
            <Field label="职位标题" value={jobTitle} onChange={setJobTitle} />
            <DictionarySelect label="职位性质" value={jobNatureCode} options={jobNatureOptions} onChange={setJobNatureCode} />
            <Field label="职类代码" value={jobCategoryCode} onChange={setJobCategoryCode} />
            <Field label="职位类别名称" value={jobCategoryName} onChange={setJobCategoryName} />
            <DictionarySelect label="经验要求" value={jobExperienceCode} options={jobExperienceOptions} onChange={setJobExperienceCode} />
            <DictionarySelect label="学历要求" value={jobEducationCode} options={jobEducationOptions} onChange={setJobEducationCode} />
            <Field label="招聘人数" value={jobRecruitCount} onChange={setJobRecruitCount} />
            <Field label="城市代码" value={jobCityCode} onChange={setJobCityCode} />
            <Field label="工作地区" value={jobWorkRegionPath} onChange={setJobWorkRegionPath} />
            <Field label="详细地址" value={jobAddress} onChange={setJobAddress} />
            <Field label="最低薪资" value={jobSalaryMin} onChange={setJobSalaryMin} />
            <Field label="最高薪资" value={jobSalaryMax} onChange={setJobSalaryMax} />
            <CheckboxField label="薪资面议" checked={jobSalaryNegotiable} onChange={setJobSalaryNegotiable} />
            <DictionaryMultiSelect label="岗位福利" values={jobWelfareCodes} options={companyBenefitOptions} onChange={setJobWelfareCodes} max={10} />
            <Field label="所属部门" value={jobDepartmentName} onChange={setJobDepartmentName} />
            <Field label="最低年龄" value={jobAgeMin} onChange={setJobAgeMin} />
            <Field label="最高年龄" value={jobAgeMax} onChange={setJobAgeMax} />
            <CheckboxField label="年龄不限" checked={jobAgeUnlimited} onChange={setJobAgeUnlimited} />
            <DictionarySelect label="招聘时间" value={jobRecruitmentTimeCode} options={jobRecruitmentTimeOptions} onChange={setJobRecruitmentTimeCode} />
            <DictionarySelect label="联系方式配置" value={jobContactMode} options={jobContactModeOptions} onChange={setJobContactMode} />
            <Field label="联系人" value={jobContactName} onChange={setJobContactName} />
            <Field label="联系手机" value={jobContactMobile} onChange={setJobContactMobile} />
            <Field label="联系固话" value={jobContactPhone} onChange={setJobContactPhone} />
            <Field label="联系邮箱" value={jobContactEmail} onChange={setJobContactEmail} />
            <Field label="联系微信" value={jobContactWechat} onChange={setJobContactWechat} />
            <CheckboxField label="联系方式保密" checked={jobContactHidden} onChange={setJobContactHidden} />
            <CheckboxField label="站内接收通知" checked={jobNotifyEnabled} onChange={setJobNotifyEnabled} />
            <CheckboxField label="开启简历订阅（站内配置）" checked={jobResumeSubscriptionEnabled} onChange={setJobResumeSubscriptionEnabled} />
            <div className={styles.formWide}>
              <TextAreaField label="职位描述" value={jobDesc} onChange={setJobDesc} rows={3} />
            </div>
            <div className={styles.formWide}>
              <div className={styles.actionRow}>
                <button type="submit" className={styles.primaryButton}>创建职位草稿</button>
                <button type="button" className={styles.secondaryButton} onClick={() => void onSubmitFirstJobReview()} disabled={jobs.length === 0}>提交首个职位审核</button>
                <button type="button" className={styles.plainButton} onClick={() => void onOfflineFirstJob()} disabled={jobs.length === 0}>下线首个职位</button>
              </div>
            </div>
          </form>
        ) : null}

        <div className={styles.jobBulkBar}>
          <label className={styles.jobSelectAll}>
            <input
              type="checkbox"
              checked={allVisibleSelected}
              onChange={(event) => toggleVisibleJobs(event.currentTarget.checked)}
              aria-label="选择当前页全部职位"
            />
            <span>已选 {selectedVisibleIds.length} 项</span>
          </label>
          <div>
            <button type="button" className={styles.plainButton} disabled>刷新职位（占位）</button>
            <button type="button" className={styles.plainButton} disabled>订阅简历（占位）</button>
            <button type="button" className={styles.secondaryButton} onClick={() => void onOfflineSelectedJobs()} disabled={selectedVisibleIds.length === 0}>关闭所选</button>
            <button type="button" className={styles.plainButton} disabled>删除职位（占位）</button>
          </div>
        </div>

        <div className={styles.jobTableWrap}>
          <table className={styles.jobTable}>
            <thead>
              <tr>
                <th aria-label="选择职位" />
                <th>职位名称</th>
                <th>招聘情况</th>
                <th>收到简历</th>
                <th>职位状态</th>
                <th>简历订阅</th>
                <th>职位推广</th>
              </tr>
            </thead>
            <tbody>
              {visibleJobs.length === 0 ? (
                <tr>
                  <td colSpan={7} className={styles.jobEmptyCell}>
                    暂无{jobTabs.find((tab) => tab.id === jobTab)?.label}职位。点击“发布职位”可先创建安全草稿。
                  </td>
                </tr>
              ) : visibleJobs.map((job) => {
                const applicationCount = countApplicationsForJob(job);
                const selected = selectedJobIds.includes(job.job_id);
                return (
                  <tr key={job.job_id}>
                    <td className={styles.jobCheckCell}>
                      <input
                        type="checkbox"
                        checked={selected}
                        aria-label={`选择职位 ${job.title}`}
                        onChange={(event) => toggleJob(job.job_id, event.currentTarget.checked)}
                      />
                    </td>
                    <td className={styles.jobNameCell}>
                      <strong>{job.title}</strong>
                      <span>更新于 {formatDateText(job.updated_at)} · 编号 {job.job_id}</span>
                      <em>{job.reject_reason || statusLabel('job_audit', job.audit_status)}</em>
                      <div className={styles.jobInlineActions}>
                        <button type="button" onClick={() => {
                          loadJobIntoForm(job);
                          setShowJobCreateForm(true);
                        }}>修改</button>
                        <button type="button" disabled>匹配（占位）</button>
                        <button type="button" onClick={() => void onOfflineJob(job.job_id)} disabled={job.status === 3}>关闭</button>
                        <button type="button" disabled>删除（占位）</button>
                      </div>
                    </td>
                    <td>
                      <div className={styles.jobMetricList}>
                        <span>被投递 <strong>{applicationCount}</strong> 次</span>
                        <span>被浏览 <strong>0</strong> 次</span>
                        <button type="button" disabled>一键分享（占位）</button>
                      </div>
                    </td>
                    <td>
                      <div className={styles.jobMetricList}>
                        <span><strong>{applicationCount}</strong> 份</span>
                        <span>未查看简历 {Math.max(applicationCount - 1, 0)} 份</span>
                      </div>
                    </td>
                    <td>
                      <div className={styles.jobStatusStack}>
                        <StatusBadge kind="job_status" value={job.status} />
                        <StatusBadge kind="job_audit" value={job.audit_status} />
                      </div>
                    </td>
                    <td>
                      <button type="button" className={styles.jobSwitchMock} disabled>
                        站内订阅占位
                      </button>
                    </td>
                    <td>
                      <div className={styles.jobPromoGrid}>
                        <button type="button" disabled>职位置顶</button>
                        <button type="button" disabled>智能刷新</button>
                        <button type="button" disabled>紧急招聘</button>
                        <button type="button" disabled>推广统计</button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>

        <div className={styles.jobPagination}>
          <span>共 {visibleJobs.length} 条记录</span>
          <button type="button" disabled>上一页</button>
          <strong>1</strong>
          <button type="button" disabled>下一页</button>
        </div>
      </section>
    );
  }

  function renderApplications() {
    return (
      <section className={styles.card}>
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
            <div style={{ marginTop: 16 }}>
              <TextAreaField label="企业处理备注（本企业私有域）" value={stageNote} onChange={setStageNote} rows={3} />
              <div className={styles.actionRow}>
                <button type="button" className={styles.primaryButton} onClick={() => void onChangeFirstApplicationStage()} disabled={workbenchApplications.length === 0}>邀约首个投递</button>
                <button type="button" className={styles.secondaryButton} onClick={() => void onCreateInterviewSession()} disabled={workbenchApplications.length === 0}>创建站内面试邀约</button>
              </div>
            </div>
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
      </section>
    );
  }

  function renderInterviews() {
    return (
      <>
        {workbenchEnabled ? (
          <section className={styles.card}>
            <div className={styles.cardTitle}><h2>投递阶段流转与面试邀约</h2></div>
            <p className={styles.muted}>投递池仅处理本企业职位产生的投递，不提供跨库检索或联系方式入口。</p>
            <TextAreaField label="企业处理备注（本企业私有域）" value={stageNote} onChange={setStageNote} rows={3} />
            <div className={styles.actionRow}>
              <button type="button" className={styles.primaryButton} onClick={() => void onChangeFirstApplicationStage()} disabled={workbenchApplications.length === 0}>邀约首个投递</button>
              <button type="button" className={styles.secondaryButton} onClick={() => void onCreateInterviewSession()} disabled={workbenchApplications.length === 0}>创建站内面试邀约</button>
            </div>
          </section>
        ) : null}
        <section className={styles.card}>
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
        </section>
      </>
    );
  }

  function renderExports() {
    return (
      <>
        <form className={styles.card} onSubmit={onCreateExport}>
          <div className={styles.cardTitle}><h2>导出申请入口</h2></div>
          <p className={styles.muted}>企业端只提交申请与查看状态；未审批前不显示下载入口，后端仍拒绝未审批下载。</p>
          <TextAreaField label="申请原因" value={exportReason} onChange={setExportReason} />
          <button type="submit" className={styles.primaryButton} style={{ marginTop: '12px' }}>提交导出申请</button>
        </form>
        <section className={styles.card}>
          <div className={styles.cardTitle}><h2>导出详情与下载入口</h2></div>
          <form onSubmit={onLoadExportDetail} className={styles.actionRow}>
            <label>
              <span style={{ display: 'block', marginBottom: '7px', color: 'var(--lt-ink-muted)', fontWeight: 800 }}>导出编号</span>
              <input value={exportIdInput} onChange={(event) => setExportIdInput(event.target.value)} style={inputStyle} />
            </label>
            <button type="submit" className={styles.primaryButton}>查询导出详情</button>
          </form>
          {exportApply ? (
            <div style={{ marginTop: '14px', display: 'grid', gap: '10px' }}>
              <div>导出 #{exportApply.export_id}：{exportApply.reason || '无申请原因'}</div>
              <ExportDownloadAction exportApply={exportApply} onIssueDownloadUrl={issueDownloadUrl} />
            </div>
          ) : (
            <p className={styles.muted}>暂无导出详情。</p>
          )}
        </section>
      </>
    );
  }

  function renderRiskPlaceholder(title: string) {
    return (
      <section className={styles.riskPlaceholder}>
        <h2 style={{ marginTop: 0 }}>{title}</h2>
        <p>
          该入口当前仅作为企业会员中心菜单占位。后续如需开放，必须单独走风险准入、权限设计、字段白名单和审计验收。
        </p>
      </section>
    );
  }

  function renderActiveContent() {
    const section = gateLocked && activeSection !== 'profile' && activeSection !== 'style' ? 'profile' : activeSection;
    if (section === 'profile') return renderProfileForm();
    if (section === 'style') return renderCompanyStyle();
    if (section === 'jobs') return renderJobs();
    if (section === 'applications') return renderApplications();
    if (section === 'interviews') return renderInterviews();
    if (section === 'exports') return renderExports();
    if (section === 'talentSearch') return renderRiskPlaceholder('搜索简历：风险池占位');
    if (section === 'chat') return renderRiskPlaceholder('我的职聊：站内沟通占位');
    if (section === 'services') return renderRiskPlaceholder('会员服务：灰度占位');
    if (section === 'recommend') return renderRiskPlaceholder('智能推荐：风险池占位');
    if (section === 'jobfair') return renderRiskPlaceholder('招聘会：公开报名能力暂未开放');
    if (section === 'account') return renderRiskPlaceholder('账号管理：后续安全专项开放');
    return renderHome();
  }

  return (
    <main className={styles.page}>
      <div className={styles.utilityBar}>
        <div className={styles.utilityInner}>
          <span>总站 [切换城市]</span>
          <nav className={styles.utilityLinks} aria-label="企业中心辅助入口">
            <span>企业会员登录</span>
            <span>使用帮助</span>
            <span>网站导航</span>
          </nav>
        </div>
      </div>

      <header className={styles.header}>
        <div className={styles.headerInner}>
          <div className={styles.brand}>
            <div className={styles.brandMark}>LT</div>
            <div>
              <h1 className={styles.brandTitle}>企业会员管理中心</h1>
              <p className={styles.brandSub}>LocalTalent 招聘者工作台</p>
            </div>
          </div>
          <div className={styles.searchBox}>
            <input aria-label="企业中心搜索占位" placeholder="搜索职位、投递或帮助内容（占位）" disabled />
            <span>搜索</span>
          </div>
          <div className={styles.headerActions}>
            <span>通知：站内</span>
            <a href="/">返回首页</a>
          </div>
        </div>
      </header>

      <div className={styles.workspace}>
        {renderMenu()}
        <div className={styles.content}>
          {renderStatusView()}
          {gateLocked ? (
            <div className={styles.noticeBar}>请先完善企业管理中的基本资料。其它功能会在服务端确认资料完整后解锁。</div>
          ) : null}
          {renderActiveContent()}
        </div>
      </div>

      <footer className={styles.footer}>
        <div className={styles.utilityInner}>
          <span>LocalTalent 企业会员中心</span>
          <nav className={styles.footerLinks} aria-label="企业中心页脚">
            <span>关于我们</span>
            <span>帮助中心</span>
            <span>隐私边界</span>
            <span>人才服务区仅展示发布快照</span>
          </nav>
        </div>
      </footer>

      {showProfileGateModal ? (
        <div className={styles.modalBackdrop} role="dialog" aria-modal="true" aria-labelledby="company-profile-gate-title">
          <div className={styles.modal}>
            <h2 id="company-profile-gate-title" className={styles.modalTitle}>系统提示</h2>
            <p className={styles.muted}>根据相关法律法规要求，需要您完善企业认证信息。</p>
            {missingProfileFields.length > 0 ? (
              <p className={styles.muted}>请先完善：{missingProfileFields.join('、')}</p>
            ) : null}
            <div className={styles.actionRow}>
              <button type="button" className={styles.plainButton} onClick={() => setShowProfileGateModal(false)}>取消</button>
              <button
                type="button"
                className={styles.primaryButton}
                onClick={() => {
                  setShowProfileGateModal(false);
                  setActiveSection('profile');
                }}
              >
                去完善
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </main>
  );
}
