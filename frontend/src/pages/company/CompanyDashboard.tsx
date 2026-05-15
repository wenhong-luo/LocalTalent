'use client';

import { type CSSProperties, type FormEvent, useEffect, useRef, useState } from 'react';
import { ExportDownloadAction } from '@/components/backoffice/ExportDownloadAction';
import { ReviewTable } from '@/components/backoffice/ReviewTable';
import { RouteGuard, type GuardContext } from '@/components/backoffice/RouteGuard';
import { StatusBadge, statusLabel } from '@/components/backoffice/StatusBadge';
import { DictionaryMultiSelect, DictionarySelect, dictionaryOptionLabel } from '@/components/selectors/DictionarySelect';
import { ExpectedPositionPicker } from '@/components/selectors/ExpectedPositionPicker';
import { RegionCascadePicker, regionLabelForCode } from '@/components/selectors/RegionCascadePicker';
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
  jobAgeOptions,
  jobNatureOptions,
  jobRecruitmentTimeOptions,
  jobSalaryPresetOptions
} from '@/shared/catalogs/jobPostOptions';
import {
  applyCompanyExport,
  applyCompanyWorkbenchExport,
  changeCompanyWorkbenchApplicationStage,
  createCompanyWorkbenchInterviewSession,
  createCompanyWorkbenchJob,
  deleteCompanyLogo,
  deleteCompanyStyleImage,
  deleteCompanyWorkbenchJob,
  fetchDeletedCompanyWorkbenchJobs,
  fetchCompanyApplications,
  fetchCompanyExportDetail,
  fetchCompanyJobs,
  fetchCompanyLogo,
  fetchCompanyLogoBlob,
  fetchCompanyResumeSearch,
  fetchCompanyResumeSearchDetail,
  fetchCompanyStyleImageBlob,
  fetchCompanyStyleImages,
  fetchCompanyWorkbenchApplications,
  fetchCompanyWorkbenchInterviewSessions,
  fetchCompanyWorkbenchJobs,
  fetchCompanyWorkbenchOverview,
  issueCompanyExportDownloadUrl,
  offlineCompanyWorkbenchJob,
  reportCompanyResumeSnapshot,
  requestCompanyResumeAccess,
  restoreCompanyWorkbenchJobDraft,
  saveCompanyWorkbenchProfile,
  saveCompanyStyleImageOrder,
  submitCompanyApply,
  submitCompanyWorkbenchCertification,
  submitCompanyWorkbenchJobReview,
  updateCompanyWorkbenchJob,
  uploadCompanyLogo,
  uploadCompanyStyleImage,
  type CompanyApplication,
  type CompanyExportApply,
  type CompanyInterviewSession,
  type CompanyJob,
  type CompanyLogo,
  type CompanyResumeSearchDetail,
  type CompanyResumeAccessRequestType,
  type CompanyResumeSearchItem,
  type CompanyResumeSearchPage,
  type CompanyResumeSearchParams,
  type CompanyStyleImage,
  type CompanyStatus,
  type CompanyWorkbenchApplication,
  type CompanyWorkbenchOverview
} from './companyApi';
import styles from './CompanyDashboard.module.css';

type LoadStatus = 'loading' | 'ready' | 'error' | 'retrying';
type WorkbenchMode = 'unknown' | 'enabled' | 'disabled';
type JobTab = 'published' | 'reviewing' | 'offline' | 'deleted';
type ResumeSearchStatus = 'idle' | 'loading' | 'ready' | 'error' | 'disabled';
type ResumeDetailStatus = 'idle' | 'loading' | 'ready' | 'error';
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
  { id: 'offline', label: '已下线', hint: '企业主动关闭' },
  { id: 'deleted', label: '已删除', hint: '回收站职位' }
];

const resumeSearchGenderOptions = [
  { value: 'male', label: '男性' },
  { value: 'female', label: '女性' }
];

const resumeTagOptions = [
  { value: 'image_good', label: '形象好' },
  { value: 'temperament_good', label: '气质佳' },
  { value: 'travel_ready', label: '能出差' },
  { value: 'technical', label: '技术精悍' },
  { value: 'friendly', label: '有亲和力' },
  { value: 'experienced', label: '经验丰富' },
  { value: 'overtime_ready', label: '能加班' },
  { value: 'driving', label: '会开车' },
  { value: 'communicative', label: '口才好' },
  { value: 'honest', label: '诚实守信' },
  { value: 'foreign_language', label: '外语好' },
  { value: 'positive', label: '有上进心' }
];

const resumeMajorOptions = [
  { value: 'computer_science', label: '计算机类' },
  { value: 'business_admin', label: '工商管理类' },
  { value: 'accounting', label: '财会类' },
  { value: 'marketing', label: '市场营销类' },
  { value: 'mechanical', label: '机械制造类' },
  { value: 'electronics', label: '电子信息类' },
  { value: 'medicine', label: '医学护理类' },
  { value: 'education', label: '教育类' },
  { value: 'civil_engineering', label: '土木建筑类' },
  { value: 'other_major', label: '其它专业' }
];

const resumeSalaryOptions = [
  { value: '1k_2k', label: '1000-2000' },
  { value: '2k_3k', label: '2000-3000' },
  { value: '3k_5k', label: '3000-5000' },
  { value: '5k_8k', label: '5000-8000' },
  { value: '8k_12k', label: '8000-12000' },
  { value: '12k_15k', label: '12000-15000' },
  { value: '15k_plus', label: '15000以上' }
];

const resumeUpdatedWithinOptions = [
  { value: '3', label: '最近3天' },
  { value: '7', label: '最近7天' },
  { value: '30', label: '最近30天' },
  { value: '90', label: '最近90天' }
];

const resumeReportReasonOptions = [
  { value: 'false_information', label: '信息不实' },
  { value: 'inappropriate_content', label: '内容不当' },
  { value: 'duplicate_snapshot', label: '重复简历' },
  { value: 'wrong_category', label: '分类不准确' },
  { value: 'privacy_concern', label: '隐私风险' },
  { value: 'other', label: '其它原因' }
];

const resumeAccessRequestLabels: Record<CompanyResumeAccessRequestType, string> = {
  download_resume: '申请下载简历',
  contact_access: '申请查看联系方式',
  chat_request: '申请聊一聊',
  interview_invite_request: '申请面试邀请'
};

const resumeExperienceFilters = [
  { value: '', label: '经验不限' },
  { value: '0_1', label: '1年以下', min: 0, max: 1 },
  { value: '1_3', label: '1～3年', min: 1, max: 3 },
  { value: '3_5', label: '3～5年', min: 3, max: 5 },
  { value: '5_10', label: '5～10年', min: 5, max: 10 },
  { value: '10_plus', label: '10年以上', min: 10 }
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
  if (tab === 'deleted') {
    return Boolean(job.deleted_at);
  }
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

function salaryPresetFromRange(min: number | null, max: number | null, negotiable: boolean): string {
  if (negotiable) {
    return 'custom';
  }
  if (min == null || max == null) {
    return 'custom';
  }
  const preset = `${min}-${max}`;
  return jobSalaryPresetOptions.some((option) => option.value === preset) ? preset : 'custom';
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
  const [jobSalaryPreset, setJobSalaryPreset] = useState('custom');
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
  const [editingJobId, setEditingJobId] = useState<number | null>(null);

  const [workbenchMode, setWorkbenchMode] = useState<WorkbenchMode>('unknown');
  const [overview, setOverview] = useState<CompanyWorkbenchOverview | null>(null);
  const [companyStatus, setCompanyStatus] = useState<CompanyStatus | null>(null);
  const [jobs, setJobs] = useState<CompanyJob[]>([]);
  const [deletedJobs, setDeletedJobs] = useState<CompanyJob[]>([]);
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
  const [jobOperationNotice, setJobOperationNotice] = useState('');
  const [resumeSearchKeyword, setResumeSearchKeyword] = useState('');
  const [resumeSearchCityCode, setResumeSearchCityCode] = useState('');
  const [resumeSearchPositions, setResumeSearchPositions] = useState<string[]>([]);
  const [resumeSearchCategoryCode, setResumeSearchCategoryCode] = useState('');
  const [resumeSearchEducationCode, setResumeSearchEducationCode] = useState('');
  const [resumeSearchExperience, setResumeSearchExperience] = useState('');
  const [resumeSearchGender, setResumeSearchGender] = useState('');
  const [resumeSearchTag, setResumeSearchTag] = useState('');
  const [resumeSearchIndustryCode, setResumeSearchIndustryCode] = useState('');
  const [resumeSearchMajor, setResumeSearchMajor] = useState('');
  const [resumeSearchWorkNature, setResumeSearchWorkNature] = useState('');
  const [resumeSearchSalaryCode, setResumeSearchSalaryCode] = useState('');
  const [resumeSearchUpdatedWithin, setResumeSearchUpdatedWithin] = useState('');
  const [resumeSearchPage, setResumeSearchPage] = useState<CompanyResumeSearchPage>({
    snapshot_list: [],
    total: 0,
    page: 1,
    size: 20
  });
  const [resumeSearchStatus, setResumeSearchStatus] = useState<ResumeSearchStatus>('idle');
  const [resumeSearchMessage, setResumeSearchMessage] = useState('');
  const [resumeSearchTraceId, setResumeSearchTraceId] = useState('');
  const [resumeSearchNotice, setResumeSearchNotice] = useState('');
  const [resumeDetailStatus, setResumeDetailStatus] = useState<ResumeDetailStatus>('idle');
  const [resumeDetail, setResumeDetail] = useState<CompanyResumeSearchDetail | null>(null);
  const [resumeDetailMessage, setResumeDetailMessage] = useState('');
  const [resumeReportOpen, setResumeReportOpen] = useState(false);
  const [resumeReportReason, setResumeReportReason] = useState('false_information');
  const [resumeReportRemark, setResumeReportRemark] = useState('');
  const [resumeReportMessage, setResumeReportMessage] = useState('');
  const [resumeReportSubmitting, setResumeReportSubmitting] = useState(false);
  const [resumeAccessRequestType, setResumeAccessRequestType] = useState<CompanyResumeAccessRequestType | null>(null);
  const [resumeAccessReason, setResumeAccessReason] = useState('');
  const [resumeAccessMessage, setResumeAccessMessage] = useState('');
  const [resumeAccessSubmitting, setResumeAccessSubmitting] = useState(false);

  const workbenchEnabled = workbenchMode === 'enabled';
  const applicationRows = workbenchEnabled ? workbenchApplications : legacyApplications;
  const profile = overview?.profile ?? null;
  const missingProfileFields = workbenchEnabled ? getMissingCompanyProfileFields(profile) : [];
  const basicProfileComplete = !workbenchEnabled || missingProfileFields.length === 0;
  const gateLocked = workbenchEnabled && Boolean(overview) && !basicProfileComplete;

  async function loadLegacy() {
    const [jobResult, applicationResult] = await Promise.all([
      fetchCompanyJobs(context.token),
      fetchCompanyApplications(context.token)
    ]);
    setJobs(jobResult.data.job_list);
    setDeletedJobs([]);
    setLegacyApplications(applicationResult.data.application_list);
    setWorkbenchApplications([]);
    setInterviewSessions([]);
    setTraceId(applicationResult.traceId || jobResult.traceId);
  }

  async function loadWorkbench() {
    const overviewResult = await fetchCompanyWorkbenchOverview(context.token);
    const [jobResult, deletedJobResult, applicationResult, sessionResult] = await Promise.all([
      fetchCompanyWorkbenchJobs(context.token),
      fetchDeletedCompanyWorkbenchJobs(context.token),
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
    setDeletedJobs(deletedJobResult.data.job_list);
    setWorkbenchApplications(applicationResult.data.application_list);
    setInterviewSessions(sessionResult.data.session_list);
    setLegacyApplications([]);
    setWorkbenchMode('enabled');
    setTraceId(sessionResult.traceId || applicationResult.traceId || deletedJobResult.traceId || jobResult.traceId || overviewResult.traceId);
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
    setSelectedJobIds((current) => current.filter((jobId) => {
      const source = jobTab === 'deleted' ? deletedJobs : jobs;
      return source.some((job) => job.job_id === jobId);
    }));
  }, [deletedJobs, jobTab, jobs]);

  useEffect(() => {
    if (activeSection !== 'talentSearch' || gateLocked || resumeSearchStatus !== 'idle') {
      return;
    }
    void loadResumeSearch(1);
  }, [activeSection, gateLocked, resumeSearchStatus]);

  async function runAction(description: string, action: () => Promise<void>): Promise<boolean> {
    setStatus('retrying');
    setMessage(description);
    try {
      await action();
      await load();
      return true;
    } catch (error) {
      setStatus('error');
      setTraceId(isHttpClientError(error) ? error.traceId : undefined);
      setMessage(errorMessage(error, description));
      return false;
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

  function resumeSearchParams(page = 1, overrides: Partial<CompanyResumeSearchParams> = {}): CompanyResumeSearchParams {
    const experience = resumeExperienceFilters.find((item) => item.value === resumeSearchExperience);
    return {
      keyword: resumeSearchKeyword.trim() || undefined,
      city_code: resumeSearchCityCode || undefined,
      category_code: resumeSearchCategoryCode || undefined,
      education_code: resumeSearchEducationCode || undefined,
      experience_min: experience?.min,
      experience_max: experience?.max,
      gender: resumeSearchGender || undefined,
      resume_tag: resumeSearchTag || undefined,
      industry_code: resumeSearchIndustryCode || undefined,
      major: resumeSearchMajor || undefined,
      work_nature: resumeSearchWorkNature || undefined,
      expected_salary_code: resumeSearchSalaryCode || undefined,
      updated_within: resumeSearchUpdatedWithin ? Number(resumeSearchUpdatedWithin) : undefined,
      page,
      size: 20,
      sort: 'updated_desc',
      ...overrides
    };
  }

  async function loadResumeSearch(page = 1, overrides: Partial<CompanyResumeSearchParams> = {}) {
    setResumeSearchStatus('loading');
    setResumeSearchMessage('正在读取受控发布快照。');
    try {
      const result = await fetchCompanyResumeSearch(context.token, resumeSearchParams(page, overrides));
      setResumeSearchPage(result.data);
      setResumeSearchStatus('ready');
      setResumeSearchMessage('');
      setResumeSearchTraceId(result.traceId);
    } catch (error) {
      const trace = isHttpClientError(error) ? error.traceId ?? '' : '';
      setResumeSearchTraceId(trace);
      if (featureDisabled(error)) {
        setResumeSearchStatus('disabled');
        setResumeSearchMessage('受控搜索简历暂未开启。请先确认 phase3.company_resume_search 灰度开关。');
        return;
      }
      setResumeSearchStatus('error');
      setResumeSearchMessage(errorMessage(error, '搜索简历暂时不可用。'));
    }
  }

  async function openResumeDetail(snapshotId: number) {
    setResumeSearchNotice('');
    setResumeDetailStatus('loading');
    setResumeDetailMessage('正在读取发布快照安全摘要。');
    setResumeDetail(null);
    setResumeReportOpen(false);
    setResumeReportRemark('');
    setResumeReportMessage('');
    setResumeAccessRequestType(null);
    setResumeAccessReason('');
    setResumeAccessMessage('');
    try {
      const result = await fetchCompanyResumeSearchDetail(context.token, snapshotId);
      setResumeDetail(result.data);
      setResumeSearchTraceId(result.traceId);
      setResumeDetailStatus('ready');
      setResumeDetailMessage('');
    } catch (error) {
      setResumeDetailStatus('error');
      setResumeDetailMessage(errorMessage(error, '发布快照详情暂时不可用。'));
      setResumeSearchTraceId(isHttpClientError(error) ? error.traceId ?? '' : '');
    }
  }

  function closeResumeDetail() {
    setResumeDetail(null);
    setResumeDetailStatus('idle');
    setResumeDetailMessage('');
    setResumeReportOpen(false);
    setResumeReportRemark('');
    setResumeReportMessage('');
    setResumeAccessRequestType(null);
    setResumeAccessReason('');
    setResumeAccessMessage('');
  }

  async function submitResumeReport(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!resumeDetail) {
      return;
    }
    setResumeReportSubmitting(true);
    setResumeReportMessage('正在提交举报。');
    try {
      const result = await reportCompanyResumeSnapshot(context.token, resumeDetail.snapshot_id, {
        reason_code: resumeReportReason,
        remark: resumeReportRemark
      });
      setResumeReportMessage(`${result.data.message} trace_id: ${result.traceId}`);
      setResumeSearchTraceId(result.traceId);
      setResumeReportRemark('');
    } catch (error) {
      const trace = isHttpClientError(error) ? error.traceId : undefined;
      setResumeReportMessage(`${errorMessage(error, '举报提交失败。')}${trace ? ` trace_id: ${trace}` : ''}`);
    } finally {
      setResumeReportSubmitting(false);
    }
  }

  function openResumeAccessRequest(requestType: CompanyResumeAccessRequestType) {
    setResumeReportOpen(false);
    setResumeAccessRequestType(requestType);
    setResumeAccessReason('');
    setResumeAccessMessage('本申请只进入合规门禁记录，不会生成完整简历、展示联系方式或发送外部通知。');
  }

  async function submitResumeAccessRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!resumeDetail || !resumeAccessRequestType) {
      return;
    }
    setResumeAccessSubmitting(true);
    setResumeAccessMessage('正在提交受控访问申请。');
    try {
      const result = await requestCompanyResumeAccess(context.token, resumeDetail.snapshot_id, {
        request_type: resumeAccessRequestType,
        reason: resumeAccessReason
      });
      setResumeAccessMessage(`申请已提交，状态：${result.data.status}。trace_id: ${result.traceId}`);
      setResumeSearchTraceId(result.traceId);
      setResumeAccessReason('');
    } catch (error) {
      const trace = isHttpClientError(error) ? error.traceId : undefined;
      setResumeAccessMessage(`${errorMessage(error, '受控访问申请提交失败。')}${trace ? ` trace_id: ${trace}` : ''}`);
    } finally {
      setResumeAccessSubmitting(false);
    }
  }

  async function onSubmitResumeSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await loadResumeSearch(1);
  }

  async function onResetResumeSearch() {
    setResumeSearchKeyword('');
    setResumeSearchCityCode('');
    setResumeSearchPositions([]);
    setResumeSearchCategoryCode('');
    setResumeSearchEducationCode('');
    setResumeSearchExperience('');
    setResumeSearchGender('');
    setResumeSearchTag('');
    setResumeSearchIndustryCode('');
    setResumeSearchMajor('');
    setResumeSearchWorkNature('');
    setResumeSearchSalaryCode('');
    setResumeSearchUpdatedWithin('');
    await loadResumeSearch(1, {
      keyword: undefined,
      city_code: undefined,
      category_code: undefined,
      education_code: undefined,
      experience_min: undefined,
      experience_max: undefined,
      gender: undefined,
      resume_tag: undefined,
      industry_code: undefined,
      major: undefined,
      work_nature: undefined,
      expected_salary_code: undefined,
      updated_within: undefined
    });
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
    setEditingJobId(job.job_id);
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
    setJobSalaryPreset(salaryPresetFromRange(job.salary_min, job.salary_max, job.salary_negotiable));
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

  function resetJobDraft() {
    setEditingJobId(null);
    setJobTitle('三期招聘顾问');
    setJobNatureCode('full_time');
    setJobCategoryCode('software');
    setJobCategoryName('互联网/电子商务');
    setJobExperienceCode('1_3_years');
    setJobEducationCode('college');
    setJobRecruitCount('3');
    setJobCityCode('310000');
    setJobWorkRegionPath('上海 / 上海市 / 浦东新区');
    setJobAddress('上海市浦东新区演示大道 100 号');
    setJobSalaryMin('12000');
    setJobSalaryMax('22000');
    setJobSalaryNegotiable(false);
    setJobSalaryPreset('custom');
    setJobWelfareCodes(['five_insurance', 'weekend_double']);
    setJobDepartmentName('招聘运营部');
    setJobAgeMin('');
    setJobAgeMax('');
    setJobAgeUnlimited(true);
    setJobRecruitmentTimeCode('long_term');
    setJobContactMode('company_profile');
    setJobContactName('');
    setJobContactMobile('');
    setJobContactPhone('');
    setJobContactEmail('');
    setJobContactWechat('');
    setJobContactHidden(true);
    setJobNotifyEnabled(false);
    setJobResumeSubscriptionEnabled(false);
    setJobDesc('负责本地人才服务平台灰度试运营支持。');
  }

  async function saveJob({ submitReview }: { submitReview: boolean }) {
    let savedJobId: number | null = null;
    const succeeded = await runAction(submitReview ? '正在保存并提交职位审核。' : '正在保存企业职位草稿。', async () => {
      const result = editingJobId
        ? await updateCompanyWorkbenchJob(context.token, editingJobId, jobPayload())
        : await createCompanyWorkbenchJob(context.token, jobPayload());
      savedJobId = result.data.job_id;
      setTraceId(result.traceId);
      if (submitReview && result.data.job_id) {
        const reviewResult = await submitCompanyWorkbenchJobReview(context.token, result.data.job_id);
        savedJobId = reviewResult.data.job_id;
        setTraceId(reviewResult.traceId || result.traceId);
      }
    });
    if (!succeeded) {
      return;
    }
    if (savedJobId) {
      setEditingJobId(savedJobId);
    }
    setJobTab(submitReview ? 'reviewing' : 'reviewing');
    setJobOperationNotice(submitReview ? '职位已提交审核，审核通过后将上线。' : '职位草稿已保存，提交审核通过后才会公开展示。');
  }

  async function onSaveJobDraft(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await saveJob({ submitReview: false });
  }

  async function onSubmitJobReview(jobId: number) {
    if (!jobId) {
      return;
    }
    const succeeded = await runAction('正在提交职位审核。', async () => {
      await submitCompanyWorkbenchJobReview(context.token, jobId);
    });
    if (!succeeded) {
      return;
    }
    setJobTab('reviewing');
    setJobOperationNotice('职位已重新提交审核，审核通过后将上线。');
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

  async function onOfflineSelectedJobs(jobIds: number[]) {
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

  async function onDeleteJob(jobId: number) {
    if (!jobId) {
      return;
    }
    if (!window.confirm('确认删除该职位？删除后将不在职位列表和公开门户展示，历史投递、面试和审计记录会保留。')) {
      return;
    }
    await runAction('正在删除职位。', async () => {
      await deleteCompanyWorkbenchJob(context.token, jobId, '企业工作台软删除职位。');
    });
    setSelectedJobIds((current) => current.filter((id) => id !== jobId));
  }

  async function onDeleteSelectedJobs(jobIds: number[]) {
    if (jobIds.length === 0) {
      return;
    }
    if (!window.confirm(`确认删除已选 ${jobIds.length} 个职位？删除后默认不在企业列表和公开门户展示。`)) {
      return;
    }
    await runAction('正在批量删除所选职位。', async () => {
      for (const jobId of jobIds) {
        await deleteCompanyWorkbenchJob(context.token, jobId, '企业工作台批量软删除职位。');
      }
    });
    setSelectedJobIds([]);
  }

  async function onRestoreJobDraft(jobId: number) {
    if (!jobId) {
      return;
    }
    if (!window.confirm('确认恢复该职位为草稿？恢复后需要重新提交审核，审核通过前不会公开展示。')) {
      return;
    }
    await runAction('正在恢复职位为草稿。', async () => {
      await restoreCompanyWorkbenchJobDraft(context.token, jobId, '企业工作台从回收站恢复为草稿。');
    });
    setSelectedJobIds((current) => current.filter((id) => id !== jobId));
    setJobTab('reviewing');
  }

  async function onRestoreSelectedJobs(jobIds: number[]) {
    if (jobIds.length === 0) {
      return;
    }
    if (!window.confirm(`确认恢复已选 ${jobIds.length} 个职位为草稿？恢复后仍需重新提交审核。`)) {
      return;
    }
    await runAction('正在批量恢复职位为草稿。', async () => {
      for (const jobId of jobIds) {
        await restoreCompanyWorkbenchJobDraft(context.token, jobId, '企业工作台批量从回收站恢复为草稿。');
      }
    });
    setSelectedJobIds([]);
    setJobTab('reviewing');
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

  const menuItems: Array<{
    id: CompanySection;
    label: string;
    disabledWhenLocked?: boolean;
    risk?: boolean;
    children?: Array<{ id: CompanySection; label: string; disabledWhenLocked?: boolean }>;
  }> = [
    { id: 'home', label: '会员首页', disabledWhenLocked: true },
    { id: 'jobs', label: '职位管理', disabledWhenLocked: true },
    { id: 'talentSearch', label: '搜索简历', disabledWhenLocked: true },
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
      { icon: '才', label: '搜索人才', section: 'talentSearch' as CompanySection, risk: false },
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
                    <p className={styles.muted}>根据职位与投递情况展示安全摘要。搜索人才仅限受控发布快照，不开放公共简历库、联系解锁或联系方式查看。</p>
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

    const visibleJobs = jobTab === 'deleted' ? deletedJobs : jobs.filter((job) => jobMatchesTab(job, jobTab));
    const selectedVisibleIds = visibleJobs.filter((job) => selectedJobIds.includes(job.job_id)).map((job) => job.job_id);
    const allVisibleSelected = visibleJobs.length > 0 && selectedVisibleIds.length === visibleJobs.length;
    const publishedCount = jobs.filter((job) => jobMatchesTab(job, 'published')).length;
    const reviewingCount = jobs.filter((job) => jobMatchesTab(job, 'reviewing')).length;
    const offlineCount = jobs.filter((job) => jobMatchesTab(job, 'offline')).length;
    const tabCounts: Record<JobTab, number> = {
      published: publishedCount,
      reviewing: reviewingCount,
      offline: offlineCount,
      deleted: deletedJobs.length
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

    const companyCanSubmitJobReview = overview?.profile.auth_status === 2;

    function canSubmitJobReview(job: CompanyJob): boolean {
      return companyCanSubmitJobReview && !(job.status === 2 && job.audit_status === 2) && job.audit_status !== 1;
    }

    function submitReviewButtonLabel(job: CompanyJob): string {
      return job.status === 3 ? '重新提交审核' : '提交审核';
    }

    return (
      <section
        className={`${styles.card} ${styles.jobManagementCard}`}
        data-testid="company-job-management"
        data-ui-stage="ui-4-refined"
      >
        <div className={styles.jobToolbar}>
          <div>
            <p className={styles.eyebrow}>职位工作台</p>
            <h2>管理职位</h2>
            <span>发布、审核、下线状态全部由服务端返回；推广能力仅做灰度占位。</span>
          </div>
          <button
            type="button"
            className={styles.primaryButton}
            onClick={() => {
              if (showJobCreateForm) {
                setShowJobCreateForm(false);
                resetJobDraft();
                return;
              }
              resetJobDraft();
              setShowJobCreateForm(true);
            }}
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
                setJobOperationNotice('');
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

        {!companyCanSubmitJobReview ? (
          <div className={styles.noticeBar}>
            企业认证通过后才能提交职位审核。你仍可以保存草稿、编辑职位、下线或删除本企业职位。
          </div>
        ) : null}

        {jobTab === 'offline' && companyCanSubmitJobReview ? (
          <div className={styles.noticeBar}>
            下线职位需要重新提交审核，审核通过后才会重新公开展示。
          </div>
        ) : null}

        {jobOperationNotice ? (
          <div className={styles.noticeBar}>{jobOperationNotice}</div>
        ) : null}

        {showJobCreateForm ? (
          <form
            onSubmit={onSaveJobDraft}
            className={styles.jobPublishForm}
            data-testid="company-job-publish-form"
            data-ui-stage="ui-4-refined"
          >
            <div className={styles.jobPublishHeader}>
              <div>
                <p className={styles.eyebrow}>发布职位</p>
                <h3>{editingJobId ? '编辑职位' : '发布职位'}</h3>
                <span>完整字段会保存到数据库；提交审核后仍由服务端状态机决定是否公开。</span>
              </div>
              <button
                type="button"
                className={styles.plainButton}
                onClick={() => {
                  setShowJobCreateForm(false);
                  resetJobDraft();
                }}
              >
                收起
              </button>
            </div>

            <section className={styles.jobPublishSection}>
              <h4>基本信息</h4>
              <div className={styles.jobPublishGrid}>
                <Field label="职位名称 *" value={jobTitle} onChange={setJobTitle} placeholder="请输入职位名称" />
                <DictionarySelect label="职位性质 *" value={jobNatureCode} options={jobNatureOptions} onChange={setJobNatureCode} />
                <label className={styles.selectorField}>
                  <span className={styles.selectorLabel}>职位类别 *</span>
                  <ExpectedPositionPicker
                    title="职位类别"
                    placeholder="请选择职位类别"
                    searchPlaceholder="请输入职位类别关键词"
                    maxSelections={1}
                    selectedPositions={jobCategoryName ? [jobCategoryName] : []}
                    selectedCategoryCode={jobCategoryCode}
                    onSave={({ positions, categoryCode }) => {
                      setJobCategoryName(positions[0] ?? '');
                      setJobCategoryCode(categoryCode);
                    }}
                  />
                </label>
                <DictionarySelect label="经验要求 *" value={jobExperienceCode} options={jobExperienceOptions} onChange={setJobExperienceCode} />
                <DictionarySelect label="学历要求 *" value={jobEducationCode} options={jobEducationOptions} onChange={setJobEducationCode} />
                <Field label="招聘人数 *" value={jobRecruitCount} onChange={setJobRecruitCount} placeholder="请输入招聘人数" />
                <div className={styles.salaryField}>
                  <DictionarySelect
                    label="薪资待遇 *"
                    value={jobSalaryPreset}
                    options={jobSalaryPresetOptions}
                    onChange={(value) => {
                      setJobSalaryPreset(value);
                      setJobSalaryNegotiable(false);
                      if (value === 'custom') {
                        return;
                      }
                      if (value === '15000+') {
                        setJobSalaryMin('15000');
                        setJobSalaryMax('');
                        return;
                      }
                      const [min, max] = value.split('-');
                      setJobSalaryMin(min ?? '');
                      setJobSalaryMax(max ?? '');
                    }}
                  />
                  <div className={styles.salaryCustomRow}>
                    <input
                      aria-label="最低薪资"
                      value={jobSalaryMin}
                      disabled={jobSalaryNegotiable}
                      onChange={(event) => {
                        setJobSalaryPreset('custom');
                        setJobSalaryMin(event.target.value);
                      }}
                      placeholder="最低"
                    />
                    <span>-</span>
                    <input
                      aria-label="最高薪资"
                      value={jobSalaryMax}
                      disabled={jobSalaryNegotiable}
                      onChange={(event) => {
                        setJobSalaryPreset('custom');
                        setJobSalaryMax(event.target.value);
                      }}
                      placeholder="最高"
                    />
                    <CheckboxField
                      label="不限/面议"
                      checked={jobSalaryNegotiable}
                      onChange={(checked) => {
                        setJobSalaryNegotiable(checked);
                        if (checked) {
                          setJobSalaryPreset('custom');
                          setJobSalaryMin('');
                          setJobSalaryMax('');
                        }
                      }}
                    />
                  </div>
                </div>
                <RegionCascadePicker
                  mode="single"
                  label="工作地区 *"
                  value={jobCityCode}
                  onChange={(code) => {
                    setJobCityCode(code);
                    setJobWorkRegionPath(regionLabelForCode(code, ''));
                  }}
                  dialogLabel="工作地区选择"
                />
                <div className={styles.addressWithMap}>
                  <Field label="详细地址" value={jobAddress} onChange={setJobAddress} placeholder="请填写详细地址" />
                  <button type="button" className={styles.mapPlaceholderButton} disabled>标注（地图占位）</button>
                </div>
                <div className={styles.formWide}>
                  <TextAreaField label="职位描述 *" value={jobDesc} onChange={setJobDesc} rows={5} />
                  <p className={styles.jobFormTip}>禁止填写歧视、面议、虚假、收费类信息。</p>
                  <button type="button" className={styles.aiPlaceholderButton} disabled>AI一键生成（占位）</button>
                </div>
              </div>
            </section>

            <section className={styles.jobPublishSection}>
              <h4>其他信息</h4>
              <div className={styles.jobPublishGrid}>
                <div className={styles.formWide}>
                  <DictionaryMultiSelect label="岗位福利" values={jobWelfareCodes} options={companyBenefitOptions} onChange={setJobWelfareCodes} max={18} />
                </div>
                <Field label="部门" value={jobDepartmentName} onChange={setJobDepartmentName} placeholder="请填写部门" />
                <div className={styles.ageField}>
                  <span className={styles.selectorLabel}>年龄要求</span>
                  <div className={styles.ageSelectRow}>
                    <DictionarySelect label="最低年龄" value={jobAgeMin} options={jobAgeOptions} onChange={(value) => {
                      setJobAgeUnlimited(false);
                      setJobAgeMin(value);
                    }} />
                    <span>-</span>
                    <DictionarySelect label="最高年龄" value={jobAgeMax} options={jobAgeOptions} onChange={(value) => {
                      setJobAgeUnlimited(false);
                      setJobAgeMax(value);
                    }} />
                    <CheckboxField label="不限" checked={jobAgeUnlimited} onChange={(checked) => {
                      setJobAgeUnlimited(checked);
                      if (checked) {
                        setJobAgeMin('');
                        setJobAgeMax('');
                      }
                    }} />
                  </div>
                </div>
                <DictionarySelect label="招聘时间" value={jobRecruitmentTimeCode} options={jobRecruitmentTimeOptions} onChange={setJobRecruitmentTimeCode} />
              </div>
            </section>

            <section className={styles.jobPublishSection}>
              <h4>联系方式</h4>
              <div className={styles.jobPublishGrid}>
                <DictionarySelect label="联系方式" value={jobContactMode} options={jobContactModeOptions} onChange={setJobContactMode} />
                <div className={styles.formWide}>
                  <CheckboxField label="联系方式保密（不想受到骚扰）" checked={jobContactHidden} onChange={setJobContactHidden} />
                </div>
                {jobContactMode === 'custom' ? (
                  <>
                    <Field label="联系人" value={jobContactName} onChange={setJobContactName} placeholder="请输入联系人" />
                    <Field label="联系手机" value={jobContactMobile} onChange={setJobContactMobile} placeholder="请输入联系手机" />
                    <Field label="联系固话" value={jobContactPhone} onChange={setJobContactPhone} placeholder="如 021-88889999" />
                    <Field label="联系邮箱" value={jobContactEmail} onChange={setJobContactEmail} placeholder="hr@example.com" />
                    <Field label="联系微信" value={jobContactWechat} onChange={setJobContactWechat} placeholder="请输入联系微信" />
                  </>
                ) : (
                  <div className={styles.formWide}>
                    <p className={styles.jobFormTip}>
                      {jobContactMode === 'hidden'
                        ? '本职位不公开联系方式，候选人仅可通过站内流程投递。'
                        : '默认使用企业管理 > 基本资料中的联系方式；公开职位仍不展示企业内部联系方式。'}
                    </p>
                  </div>
                )}
                <div className={styles.jobSwitchPanel}>
                  <CheckboxField label="接收通知（站内配置）" checked={jobNotifyEnabled} onChange={setJobNotifyEnabled} />
                  <p>联系手机接收投递通知为占位说明，不接真实短信、微信、小程序或 App。</p>
                </div>
                <div className={styles.jobSwitchPanel}>
                  <CheckboxField label="简历订阅（站内配置）" checked={jobResumeSubscriptionEnabled} onChange={setJobResumeSubscriptionEnabled} />
                  <p>精准推送优质人才暂为站内配置，不开放搜索简历、联系解锁或公共简历库。</p>
                </div>
                <div className={styles.wechatNoticePlaceholder}>
                  <strong>公众号通知占位</strong>
                  <span>随时接收简历投递通知的真实微信/小程序/App 能力暂未接入。</span>
                </div>
              </div>
            </section>

            <div className={styles.jobPublishActions}>
              <button type="submit" className={styles.secondaryButton}>保存草稿</button>
              <button
                type="button"
                className={styles.primaryButton}
                onClick={() => void saveJob({ submitReview: true })}
                disabled={!companyCanSubmitJobReview}
                title={companyCanSubmitJobReview ? undefined : '企业认证通过后才能提交职位审核'}
              >
                发布职位（提交审核）
              </button>
              <button type="button" className={styles.plainButton} onClick={() => void onOfflineFirstJob()} disabled={jobs.length === 0}>下线首个职位</button>
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
            {jobTab === 'deleted' ? (
              <>
                <button type="button" className={styles.secondaryButton} onClick={() => void onRestoreSelectedJobs(selectedVisibleIds)} disabled={selectedVisibleIds.length === 0}>恢复为草稿</button>
                <button type="button" className={styles.plainButton} disabled>永久删除（禁止）</button>
              </>
            ) : (
              <>
                <button type="button" className={styles.plainButton} disabled>刷新职位（占位）</button>
                <button type="button" className={styles.plainButton} disabled>订阅简历（占位）</button>
                <button type="button" className={styles.secondaryButton} onClick={() => void onOfflineSelectedJobs(selectedVisibleIds)} disabled={selectedVisibleIds.length === 0}>关闭所选</button>
                <button type="button" className={styles.plainButton} onClick={() => void onDeleteSelectedJobs(selectedVisibleIds)} disabled={selectedVisibleIds.length === 0}>删除职位</button>
              </>
            )}
          </div>
        </div>

        <div className={styles.jobTableWrap} data-testid="company-job-table-wrap">
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
                      <span>{jobTab === 'deleted' ? `删除于 ${formatDateText(job.deleted_at)}` : `更新于 ${formatDateText(job.updated_at)}`} · 编号 {job.job_id}</span>
                      <em>{jobTab === 'deleted' ? (job.delete_reason || '企业已软删除，历史投递和审计记录保留') : (job.reject_reason || statusLabel('job_audit', job.audit_status))}</em>
                      {jobTab === 'deleted' ? (
                        <div className={styles.jobInlineActions}>
                          <button type="button" onClick={() => void onRestoreJobDraft(job.job_id)}>恢复为草稿</button>
                          <button type="button" disabled>永久删除（禁止）</button>
                        </div>
                      ) : (
                        <div className={styles.jobInlineActions}>
                          <button type="button" onClick={() => {
                            loadJobIntoForm(job);
                            setShowJobCreateForm(true);
                          }}>修改</button>
                          <button
                            type="button"
                            onClick={() => void onSubmitJobReview(job.job_id)}
                            disabled={!canSubmitJobReview(job)}
                            title={companyCanSubmitJobReview ? undefined : '企业认证通过后才能提交职位审核'}
                          >
                            {submitReviewButtonLabel(job)}
                          </button>
                          <button type="button" disabled>匹配（占位）</button>
                          <button type="button" onClick={() => void onOfflineJob(job.job_id)} disabled={job.status === 3}>关闭</button>
                          <button type="button" onClick={() => void onDeleteJob(job.job_id)}>删除</button>
                        </div>
                      )}
                    </td>
                    <td>
                      <div className={styles.jobMetricList}>
                        <span>被投递 <strong>{applicationCount}</strong> 次</span>
                        <span>被浏览 <strong>0</strong> 次</span>
                        {jobTab === 'deleted' ? <span>回收站不支持推广或分享</span> : <button type="button" disabled>一键分享（占位）</button>}
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

  function resumeSearchEducationLabel(item: CompanyResumeSearchItem): string {
    return item.highest_education || dictionaryOptionLabel(jobEducationOptions, item.education_code, '学历暂未填写');
  }

  function resumeSearchWorkNatureLabel(value: string): string {
    return dictionaryOptionLabel(jobNatureOptions, value, value || '求职性质不限');
  }

  function renderResumeSearchCard(item: CompanyResumeSearchItem) {
    const positions = item.expected_positions.length > 0 ? item.expected_positions.join(' / ') : '期望职位暂未填写';
    const cities = item.expected_cities.length > 0 ? item.expected_cities.join(' / ') : '期望地区不限';
    const tags = item.resume_tags.length > 0 ? item.resume_tags : ['发布快照', '字段白名单'];
    return (
      <article className={styles.resumeSearchCard} key={item.snapshot_id}>
        <div className={styles.resumeSearchAvatar} aria-hidden="true">
          {item.display_name_masked.slice(0, 1) || '才'}
        </div>
        <div className={styles.resumeSearchMain}>
          <div className={styles.resumeSearchTitleRow}>
            <div>
              <h3>{item.display_name_masked || '求职者*'}</h3>
              <p>
                {item.gender || '性别未公开'} · {item.age_band || '年龄段未公开'} · {resumeSearchEducationLabel(item)}
                {item.experience_years == null ? '' : ` · ${item.experience_years}年经验`}
              </p>
            </div>
            <span className={styles.resumeSearchUpdated}>{formatDateText(item.updated_at)} 更新</span>
          </div>
          <div className={styles.resumeSearchMetaLine}>
            <span>期望：{positions}</span>
            <span>{cities}</span>
            <span>{item.expected_salary || '面议'}</span>
            <span>{resumeSearchWorkNatureLabel(item.work_nature)}</span>
          </div>
          <dl className={styles.resumeSearchSummary}>
            <div><dt>专业方向</dt><dd>{item.major_name || '专业未公开'}</dd></div>
            <div><dt>行业类别</dt><dd>{dictionaryOptionLabel(companyIndustryOptions, item.industry_code, item.industry_code || '行业未公开')}</dd></div>
          </dl>
          <p className={styles.resumeSearchSkills}>{item.skills_summary || '候选人暂未发布技能摘要。'}</p>
          <div className={styles.resumeSearchTags}>
            {tags.slice(0, 6).map((tag) => <span key={tag}>{tag}</span>)}
          </div>
        </div>
        <div className={styles.resumeSearchActions}>
          <button type="button" onClick={() => openResumeDetail(item.snapshot_id)}>查看摘要</button>
          <button type="button" onClick={() => setResumeSearchNotice('画像分析仅为视觉占位，本轮不接 AI 或候选人画像能力。')}>画像分析（占位）</button>
          <button type="button" onClick={() => setResumeSearchNotice('下载完整简历属于高风险能力，本轮不开放。')}>下载简历（受控占位）</button>
          <button type="button" onClick={() => setResumeSearchNotice('聊一聊和联系方式查看仍需后续风险准入，本轮不开放。')}>聊一聊（占位）</button>
        </div>
      </article>
    );
  }

  function renderResumeDetailDrawer() {
    if (resumeDetailStatus === 'idle') {
      return null;
    }
    const detail = resumeDetail;
    const positions = detail && detail.expected_positions.length > 0 ? detail.expected_positions.join(' / ') : '期望职位暂未填写';
    const cities = detail && detail.expected_cities.length > 0 ? detail.expected_cities.join(' / ') : '期望地区不限';
    return (
      <div className={styles.resumeDetailBackdrop} role="dialog" aria-modal="true" aria-labelledby="resume-detail-title">
        <aside className={styles.resumeDetailDrawer}>
          <div className={styles.resumeDetailHeader}>
            <div>
              <span>发布快照安全摘要</span>
              <h3 id="resume-detail-title">简历摘要详情</h3>
            </div>
            <button type="button" aria-label="关闭简历摘要详情" onClick={closeResumeDetail}>×</button>
          </div>
          {resumeDetailStatus === 'loading' ? (
            <div className={styles.resumeSearchEmpty}>正在读取安全详情...</div>
          ) : null}
          {resumeDetailStatus === 'error' ? (
            <div className={styles.resumeSearchError}>
              <strong>详情读取失败</strong>
              <p>{resumeDetailMessage}</p>
              {resumeSearchTraceId ? <span>trace_id: {resumeSearchTraceId}</span> : null}
            </div>
          ) : null}
          {resumeDetailStatus === 'ready' && detail ? (
            <>
              <section className={styles.resumeDetailProfile}>
                <div className={styles.resumeSearchAvatar} aria-hidden="true">{detail.display_name_masked.slice(0, 1) || '才'}</div>
                <div>
                  <h4>{detail.display_name_masked || '求职者*'}</h4>
                  <p>
                    {detail.gender || '性别未公开'} · {detail.age_band || '年龄段未公开'} · {resumeSearchEducationLabel(detail)}
                    {detail.experience_years == null ? '' : ` · ${detail.experience_years}年经验`}
                  </p>
                  <span>{formatDateText(detail.updated_at)} 更新</span>
                </div>
              </section>
              <dl className={styles.resumeDetailGrid}>
                <div><dt>期望职位</dt><dd>{positions}</dd></div>
                <div><dt>期望地区</dt><dd>{cities}</dd></div>
                <div><dt>期望薪资</dt><dd>{detail.expected_salary || '面议'}</dd></div>
                <div><dt>工作性质</dt><dd>{resumeSearchWorkNatureLabel(detail.work_nature)}</dd></div>
                <div><dt>专业</dt><dd>{detail.major_name || '未公开'}</dd></div>
                <div><dt>行业</dt><dd>{dictionaryOptionLabel(companyIndustryOptions, detail.industry_code, detail.industry_code || '未公开')}</dd></div>
              </dl>
              <section className={styles.resumeDetailSection}>
                <h4>技能摘要</h4>
                <p>{detail.skills_summary || '候选人暂未发布技能摘要。'}</p>
                <div className={styles.resumeSearchTags}>
                  {(detail.resume_tags.length > 0 ? detail.resume_tags : ['发布快照', '字段白名单']).slice(0, 8).map((tag) => <span key={tag}>{tag}</span>)}
                </div>
              </section>
              <section className={styles.resumeDetailSection}>
                <h4>经历与教育摘要</h4>
                <p>{detail.experience_summary || '经历摘要未公开。'}</p>
                <p>{detail.education_summary || '教育摘要未公开。'}</p>
                <p>{detail.self_description_summary || '自我描述摘要未公开。'}</p>
              </section>
              <section className={styles.resumeDetailContact}>
                <h4>联系方式</h4>
                <p>{detail.contact_access_hint}</p>
                <button type="button" onClick={() => openResumeAccessRequest('contact_access')}>申请查看联系方式</button>
              </section>
              <div className={styles.resumeDetailActions}>
                <button type="button" onClick={() => setResumeReportOpen(true)}>举报简历</button>
                <button type="button" onClick={() => setResumeSearchNotice('画像分析仅为视觉占位，本轮不接 AI 或候选人画像能力。')}>画像分析（占位）</button>
                <button type="button" onClick={() => openResumeAccessRequest('download_resume')}>申请下载简历</button>
                <button type="button" onClick={() => openResumeAccessRequest('chat_request')}>申请聊一聊</button>
                <button type="button" onClick={() => openResumeAccessRequest('interview_invite_request')}>申请面试邀请</button>
              </div>
              {resumeAccessRequestType ? (
                <form className={styles.resumeAccessForm} onSubmit={submitResumeAccessRequest}>
                  <h4>{resumeAccessRequestLabels[resumeAccessRequestType]}</h4>
                  <p>申请只进入合规审核记录，不生成完整简历、不展示手机号/邮箱/微信、不创建真实职聊，也不会发送短信、微信、小程序或 App 通知。</p>
                  <label>
                    <span>申请说明</span>
                    <textarea
                      value={resumeAccessReason}
                      maxLength={300}
                      onChange={(event) => setResumeAccessReason(event.target.value)}
                      placeholder="请填写申请原因，最多 300 字。不要填写联系方式或敏感材料。"
                    />
                  </label>
                  {resumeAccessMessage ? <p className={styles.resumeReportMessage}>{resumeAccessMessage}</p> : null}
                  <div className={styles.actionRow}>
                    <button type="button" className={styles.plainButton} onClick={() => setResumeAccessRequestType(null)}>取消</button>
                    <button type="submit" className={styles.primaryButton} disabled={resumeAccessSubmitting}>
                      {resumeAccessSubmitting ? '提交中...' : '提交申请'}
                    </button>
                  </div>
                </form>
              ) : null}
              {resumeReportOpen ? (
                <form className={styles.resumeReportForm} onSubmit={submitResumeReport}>
                  <h4>举报简历</h4>
                  <DictionarySelect
                    label="举报原因"
                    value={resumeReportReason}
                    options={resumeReportReasonOptions}
                    onChange={setResumeReportReason}
                    placeholder="请选择举报原因"
                  />
                  <label>
                    <span>备注说明</span>
                    <textarea
                      value={resumeReportRemark}
                      maxLength={300}
                      onChange={(event) => setResumeReportRemark(event.target.value)}
                      placeholder="请描述问题，最多 300 字。不要填写联系方式或敏感材料。"
                    />
                  </label>
                  {resumeReportMessage ? <p className={styles.resumeReportMessage}>{resumeReportMessage}</p> : null}
                  <div className={styles.actionRow}>
                    <button type="button" className={styles.plainButton} onClick={() => setResumeReportOpen(false)}>取消</button>
                    <button type="submit" className={styles.primaryButton} disabled={resumeReportSubmitting}>
                      {resumeReportSubmitting ? '提交中...' : '提交举报'}
                    </button>
                  </div>
                </form>
              ) : null}
            </>
          ) : null}
        </aside>
      </div>
    );
  }

  function renderTalentSearch() {
    const totalPages = Math.max(1, Math.ceil(resumeSearchPage.total / resumeSearchPage.size));
    const currentPage = Math.min(resumeSearchPage.page || 1, totalPages);
    return (
      <section className={styles.resumeSearchPage}>
        <div className={styles.resumeSearchHero}>
          <div>
            <span>受控发布快照搜索</span>
            <h2>搜索简历</h2>
            <p>仅检索候选人授权发布的快照摘要，不展示联系方式、附件、完整简历或原始候选人数据。</p>
          </div>
          <strong>快照白名单</strong>
        </div>

        <form className={styles.resumeSearchFilters} onSubmit={onSubmitResumeSearch}>
          <div className={styles.resumeSearchTopBar}>
            <RegionCascadePicker
              mode="single"
              label="地区"
              value={resumeSearchCityCode}
              onChange={setResumeSearchCityCode}
              placeholder="不限地区"
              dialogLabel="搜索简历地区"
            />
            <ExpectedPositionPicker
              selectedPositions={resumeSearchPositions}
              selectedCategoryCode={resumeSearchCategoryCode}
              maxSelections={1}
              title="职位类别"
              placeholder="职位类型"
              searchPlaceholder="请输入职位类别关键词"
              onSave={({ positions, categoryCode }) => {
                setResumeSearchPositions(positions);
                setResumeSearchCategoryCode(categoryCode);
              }}
            />
            <label>
              <input
                aria-label="搜索简历关键词"
                value={resumeSearchKeyword}
                onChange={(event) => setResumeSearchKeyword(event.target.value)}
                placeholder="请输入关键词搜索"
              />
            </label>
            <button type="submit" className={styles.resumeSearchSubmit}>搜索简历</button>
          </div>
          <div className={styles.resumeSearchPrimaryFilters}>
            <span>学历</span>
            <button
              type="button"
              className={!resumeSearchEducationCode ? styles.resumeQuickFilterActive : styles.resumeQuickFilter}
              onClick={() => setResumeSearchEducationCode('')}
            >
              不限
            </button>
            {jobEducationOptions.map((option) => (
              <button
                key={option.value}
                type="button"
                className={resumeSearchEducationCode === option.value ? styles.resumeQuickFilterActive : styles.resumeQuickFilter}
                onClick={() => setResumeSearchEducationCode(option.value)}
              >
                {option.label}
              </button>
            ))}
          </div>
          <div className={styles.resumeSearchQuickFilters} aria-label="工作经验快捷筛选">
            <span>工作经验</span>
            {resumeExperienceFilters.map((option) => (
              <button
                key={option.value || 'all'}
                type="button"
                className={resumeSearchExperience === option.value ? styles.resumeQuickFilterActive : styles.resumeQuickFilter}
                onClick={() => setResumeSearchExperience(option.value)}
              >
                {option.label}
              </button>
            ))}
          </div>
          <div className={styles.resumeSearchMoreFilters}>
            <span>其他筛选</span>
            <DictionarySelect label="性别" value={resumeSearchGender} options={resumeSearchGenderOptions} onChange={setResumeSearchGender} placeholder="不限性别" />
            <DictionarySelect label="简历标签" value={resumeSearchTag} options={resumeTagOptions} onChange={setResumeSearchTag} placeholder="不限标签" />
            <DictionarySelect label="行业类别" value={resumeSearchIndustryCode} options={companyIndustryOptions} onChange={setResumeSearchIndustryCode} placeholder="不限行业" />
            <DictionarySelect label="所学专业" value={resumeSearchMajor} options={resumeMajorOptions} onChange={setResumeSearchMajor} placeholder="不限专业" />
            <DictionarySelect label="工作性质" value={resumeSearchWorkNature} options={jobNatureOptions} onChange={setResumeSearchWorkNature} placeholder="不限性质" />
            <DictionarySelect label="期望薪资" value={resumeSearchSalaryCode} options={resumeSalaryOptions} onChange={setResumeSearchSalaryCode} placeholder="不限薪资" />
            <DictionarySelect
              label="更新时间"
              value={resumeSearchUpdatedWithin}
              options={resumeUpdatedWithinOptions}
              onChange={setResumeSearchUpdatedWithin}
              placeholder="不限时间"
            />
            <button type="button" className={styles.resumeSearchReset} onClick={onResetResumeSearch}>清空筛选</button>
          </div>
        </form>

        {resumeSearchNotice ? <div className={styles.resumeSearchNotice}>{resumeSearchNotice}</div> : null}

        <div className={styles.resumeSearchBody}>
          <div className={styles.resumeSearchList}>
            <div className={styles.resumeSearchListHeader}>
              <div>
                <h3>简历快照列表</h3>
                <p>共 {resumeSearchPage.total} 条安全快照。</p>
              </div>
              {resumeSearchTraceId ? <span>trace {resumeSearchTraceId}</span> : null}
            </div>
            {resumeSearchStatus === 'loading' ? (
              <div className={styles.resumeSearchEmpty}>正在读取受控发布快照...</div>
            ) : null}
            {resumeSearchStatus === 'disabled' ? (
              <div className={styles.resumeSearchEmpty}>
                <strong>受控搜索简历暂未开启</strong>
                <p>{resumeSearchMessage}</p>
              </div>
            ) : null}
            {resumeSearchStatus === 'error' ? (
              <div className={styles.resumeSearchError}>
                <strong>搜索简历读取失败</strong>
                <p>{resumeSearchMessage}</p>
                {resumeSearchTraceId ? <span>trace_id: {resumeSearchTraceId}</span> : null}
              </div>
            ) : null}
            {resumeSearchStatus === 'ready' && resumeSearchPage.snapshot_list.length === 0 ? (
              <div className={styles.resumeSearchEmpty}>
                <strong>暂无符合条件的发布快照</strong>
                <p>请调整筛选条件，或等待候选人授权发布更多快照。</p>
              </div>
            ) : null}
            {resumeSearchStatus === 'ready' && resumeSearchPage.snapshot_list.length > 0 ? (
              <div className={styles.resumeSearchCards}>
                {resumeSearchPage.snapshot_list.map(renderResumeSearchCard)}
              </div>
            ) : null}
            <div className={styles.resumeSearchPagination}>
              <span>第 {currentPage} / {totalPages} 页</span>
              <button type="button" disabled={resumeSearchStatus === 'loading' || currentPage <= 1} onClick={() => loadResumeSearch(currentPage - 1)}>上一页</button>
              <button type="button" disabled={resumeSearchStatus === 'loading' || currentPage >= totalPages} onClick={() => loadResumeSearch(currentPage + 1)}>下一页</button>
            </div>
          </div>
          <aside className={styles.resumeSearchBoundary}>
            <h3>搜索简历边界</h3>
            <ul>
              <li>只读取候选人授权发布快照。</li>
              <li>不展示联系方式、附件或完整简历。</li>
              <li>画像分析、下载和职聊均为占位。</li>
              <li>撤回或下线快照不会出现在本页。</li>
            </ul>
          </aside>
        </div>
        {renderResumeDetailDrawer()}
      </section>
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
    if (section === 'talentSearch') return renderTalentSearch();
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

      <div className={styles.workspace} data-testid="company-workspace" data-layout="fluid" data-shell="refined">
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
