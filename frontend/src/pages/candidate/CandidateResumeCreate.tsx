'use client';

import Link from 'next/link';
import { type ChangeEvent, type FormEvent, useEffect, useMemo, useState } from 'react';
import { StateView } from '@/components/StateView';
import { isHttpClientError } from '@/lib/httpClient';
import {
  applyCandidateResumeAiSuggestion,
  dismissCandidateResumeAiSuggestion,
  fetchCandidateCenterOverview,
  fetchCandidateClosureData,
  fetchCandidateResumeAiSuggestions,
  fetchCandidateResumeAttachment,
  generateCandidateResumeAiSuggestions,
  readCandidateToken,
  saveCandidateResume,
  uploadCandidateResumeAttachment,
  type CandidateResumeAiSuggestionTask,
  type CandidateResumeAttachment,
  type CandidateEducationExperience,
  type CandidateResume,
  type CandidateWorkExperience
} from './candidateCenterApi';
import styles from './CandidateResumeCreate.module.css';

type Step = 'basic' | 'detail' | 'done';
type Status = 'loading' | 'ready' | 'unauthorized' | 'disabled' | 'error';

type BasicForm = {
  displayName: string;
  gender: string;
  birthDate: string;
  highestEducation: string;
  startWorkDate: string;
  noExperience: boolean;
  contactPhone: string;
  contactWechat: string;
  wechatSameAsPhone: boolean;
  expectedPositions: string;
  expectedSalary: string;
  expectedCities: string;
  jobStatus: string;
};

type DetailForm = {
  companyName: string;
  positionName: string;
  workStartDate: string;
  workEndDate: string;
  workOngoing: boolean;
  responsibility: string;
  schoolName: string;
  majorName: string;
  educationStartDate: string;
  educationEndDate: string;
  educationOngoing: boolean;
  degree: string;
  selfDescription: string;
};

const emptyBasic: BasicForm = {
  displayName: '',
  gender: '男',
  birthDate: '',
  highestEducation: '',
  startWorkDate: '',
  noExperience: false,
  contactPhone: '',
  contactWechat: '',
  wechatSameAsPhone: false,
  expectedPositions: '',
  expectedSalary: '',
  expectedCities: '',
  jobStatus: '我目前已离职，可快速到岗'
};

const emptyDetail: DetailForm = {
  companyName: '',
  positionName: '',
  workStartDate: '',
  workEndDate: '',
  workOngoing: false,
  responsibility: '',
  schoolName: '',
  majorName: '',
  educationStartDate: '',
  educationEndDate: '',
  educationOngoing: false,
  degree: '',
  selfDescription: ''
};

const tokenStorageKey = 'localtalent_access_token';

function splitList(value: string): string[] {
  return value
    .split(/[，,\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 5);
}

function joinList(value: string[]): string {
  return value.join('，');
}

function firstWorkExperience(resume?: CandidateResume): CandidateWorkExperience | undefined {
  return resume?.work_experience?.[0];
}

function firstEducationExperience(resume?: CandidateResume): CandidateEducationExperience | undefined {
  return resume?.education_experience?.[0];
}

function basicFromResume(resume?: CandidateResume): BasicForm {
  if (!resume) {
    return emptyBasic;
  }
  return {
    displayName: resume.base_profile.display_name,
    gender: resume.base_profile.gender || '男',
    birthDate: resume.base_profile.birth_date,
    highestEducation: resume.base_profile.highest_education,
    startWorkDate: resume.base_profile.start_work_date,
    noExperience: resume.base_profile.no_experience,
    contactPhone: resume.base_profile.contact_phone,
    contactWechat: resume.base_profile.contact_wechat,
    wechatSameAsPhone: resume.base_profile.wechat_same_as_phone,
    expectedPositions: joinList(resume.base_profile.expected_positions),
    expectedSalary: resume.base_profile.expected_salary,
    expectedCities: joinList(resume.base_profile.expected_cities),
    jobStatus: resume.base_profile.job_status || emptyBasic.jobStatus
  };
}

function detailFromResume(resume?: CandidateResume): DetailForm {
  const work = firstWorkExperience(resume);
  const education = firstEducationExperience(resume);
  return {
    companyName: work?.company_name ?? '',
    positionName: work?.position_name ?? '',
    workStartDate: work?.start_date ?? '',
    workEndDate: work?.end_date ?? '',
    workOngoing: work?.ongoing ?? false,
    responsibility: work?.responsibility ?? '',
    schoolName: education?.school_name ?? '',
    majorName: education?.major_name ?? '',
    educationStartDate: education?.start_date ?? '',
    educationEndDate: education?.end_date ?? '',
    educationOngoing: education?.ongoing ?? false,
    degree: education?.degree ?? '',
    selfDescription: resume?.self_description || resume?.base_profile.summary || ''
  };
}

function Progress({ step }: { step: Step }) {
  const current = step === 'basic' ? 1 : step === 'detail' ? 2 : 3;
  return (
    <ol className={styles.progress} aria-label="简历创建进度">
      {['基本信息', '完善简历', '创建完成'].map((label, index) => {
        const order = index + 1;
        const className = order < current
          ? styles.progressItemDone
          : order === current
            ? styles.progressItemActive
            : styles.progressItem;
        return (
          <li key={label} className={className}>
            <span className={styles.progressNumber}>{order}</span>
            <span>{label}</span>
          </li>
        );
      })}
    </ol>
  );
}

function PageChrome({ step, children }: { step: Step; children: React.ReactNode }) {
  return (
    <main className={styles.page}>
      <div className={styles.topbar}>
        <div className={styles.topbarInner}>
          <span>LocalTalent 求职者</span>
          <span>真实短信、微信、小程序、App 均为占位</span>
        </div>
      </div>
      <header className={styles.header}>
        <Link className={styles.brand} href="/">
          <span className={styles.brandMark}>LT</span>
          <span>LocalTalent</span>
        </Link>
        <Progress step={step} />
      </header>
      <div className={styles.main}>{children}</div>
      <footer className={styles.footer}>
        <div className={styles.footerInner}>
          <span>关于我们</span>
          <span>帮助中心</span>
          <span>隐私边界</span>
          <span>人才服务区仅展示发布快照</span>
        </div>
      </footer>
    </main>
  );
}

function buildResumePayload(basic: BasicForm, detail: DetailForm, previous?: CandidateResume) {
  const expectedPositions = splitList(basic.expectedPositions);
  const expectedCities = splitList(basic.expectedCities);
  const workExperience: CandidateWorkExperience[] = detail.companyName || detail.positionName || detail.responsibility
    ? [{
      company_name: detail.companyName,
      position_name: detail.positionName,
      start_date: detail.workStartDate,
      end_date: detail.workOngoing ? '' : detail.workEndDate,
      ongoing: detail.workOngoing,
      responsibility: detail.responsibility
    }]
    : [];
  const educationExperience: CandidateEducationExperience[] = detail.schoolName || detail.majorName || detail.degree
    ? [{
      school_name: detail.schoolName,
      major_name: detail.majorName,
      start_date: detail.educationStartDate,
      end_date: detail.educationOngoing ? '' : detail.educationEndDate,
      ongoing: detail.educationOngoing,
      degree: detail.degree
    }]
    : [];

  return {
    resume_name: previous?.resume_name || `${basic.displayName || '我的'}的简历`,
    base_profile: {
      display_name: basic.displayName,
      city_code: expectedCities[0] ?? previous?.base_profile.city_code ?? '',
      category_code: expectedPositions[0] ?? previous?.base_profile.category_code ?? '',
      experience_years: previous?.base_profile.experience_years ?? null,
      summary: detail.selfDescription,
      gender: basic.gender,
      birth_date: basic.birthDate,
      highest_education: basic.highestEducation,
      start_work_date: basic.startWorkDate,
      no_experience: basic.noExperience,
      contact_phone: basic.contactPhone,
      contact_wechat: basic.wechatSameAsPhone ? basic.contactPhone : basic.contactWechat,
      wechat_same_as_phone: basic.wechatSameAsPhone,
      expected_positions: expectedPositions,
      expected_salary: basic.expectedSalary,
      expected_cities: expectedCities,
      job_status: basic.jobStatus
    },
    education: educationExperience.map((item) => [item.school_name, item.major_name, item.degree].filter(Boolean).join(' / ')),
    experience: workExperience.map((item) => [item.company_name, item.position_name].filter(Boolean).join(' / ')),
    skills: previous?.skills ?? [],
    work_experience: workExperience,
    education_experience: educationExperience,
    self_description: detail.selfDescription
  };
}

export function CandidateResumeCreate() {
  const [status, setStatus] = useState<Status>('loading');
  const [step, setStep] = useState<Step>('basic');
  const [token, setToken] = useState<string | null>(null);
  const [resume, setResume] = useState<CandidateResume | undefined>();
  const [basic, setBasic] = useState<BasicForm>(emptyBasic);
  const [detail, setDetail] = useState<DetailForm>(emptyDetail);
  const [message, setMessage] = useState<string>();
  const [error, setError] = useState<string>();
  const [saving, setSaving] = useState(false);
  const [attachmentEnabled, setAttachmentEnabled] = useState(false);
  const [attachment, setAttachment] = useState<CandidateResumeAttachment>();
  const [attachmentBusy, setAttachmentBusy] = useState(false);
  const [aiEnabled, setAiEnabled] = useState(false);
  const [aiTask, setAiTask] = useState<CandidateResumeAiSuggestionTask>();
  const [aiBusy, setAiBusy] = useState(false);

  useEffect(() => {
    const activeToken = readCandidateToken();
    if (!activeToken) {
      setStatus('unauthorized');
      return;
    }
    const tokenForLoad = activeToken;
    setToken(tokenForLoad);
    let mounted = true;
    async function load() {
      try {
        const overview = await fetchCandidateCenterOverview(tokenForLoad);
        if (!overview.data.features.candidate_closure_enabled) {
          if (mounted) {
            setStatus('disabled');
          }
          return;
        }
        const uploadEnabled = overview.data.features.resume_attachment_upload_enabled;
        const aiAssistEnabled = overview.data.features.resume_ai_assist_enabled;
        const closure = await fetchCandidateClosureData(tokenForLoad);
        const attachmentResult = uploadEnabled
          ? await fetchCandidateResumeAttachment(tokenForLoad)
          : undefined;
        const aiResult = aiAssistEnabled
          ? await fetchCandidateResumeAiSuggestions(tokenForLoad).catch(() => undefined)
          : undefined;
        if (!mounted) {
          return;
        }
        setAttachmentEnabled(uploadEnabled);
        setAiEnabled(aiAssistEnabled);
        setResume(closure.resume);
        setAttachment(attachmentResult?.data);
        setAiTask(aiResult?.data);
        setBasic(basicFromResume(closure.resume));
        setDetail(detailFromResume(closure.resume));
        setStep(overview.data.onboarding?.onboarding_step === 'detail' ? 'detail' : 'basic');
        setStatus('ready');
      } catch (errorValue) {
        if (!mounted) {
          return;
        }
        if (isHttpClientError(errorValue) && errorValue.kind === 'unauthorized') {
          setStatus('unauthorized');
        } else {
          setError(errorValue instanceof Error ? errorValue.message : '读取简历创建流程失败。');
          setStatus('error');
        }
      }
    }
    void load();
    return () => {
      mounted = false;
    };
  }, []);

  const contactWechatValue = useMemo(
    () => (basic.wechatSameAsPhone ? basic.contactPhone : basic.contactWechat),
    [basic.contactPhone, basic.contactWechat, basic.wechatSameAsPhone]
  );

  function updateBasic(field: keyof BasicForm, value: string | boolean) {
    setBasic((current) => ({ ...current, [field]: value }));
  }

  function updateDetail(field: keyof DetailForm, value: string | boolean) {
    setDetail((current) => ({ ...current, [field]: value }));
  }

  function handlePhoneChange(event: ChangeEvent<HTMLInputElement>) {
    const value = event.target.value;
    setBasic((current) => ({
      ...current,
      contactPhone: value,
      contactWechat: current.wechatSameAsPhone ? value : current.contactWechat
    }));
  }

  async function submitBasic(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage(undefined);
    if (!basic.displayName.trim() || !basic.contactPhone.trim() || !basic.expectedPositions.trim() || !basic.expectedCities.trim()) {
      setMessage('请先补充姓名、联系电话、期望职位和期望地区。');
      return;
    }
    if (!token) {
      setStatus('unauthorized');
      return;
    }
    setSaving(true);
    setError(undefined);
    try {
      const result = await saveCandidateResume(token, buildResumePayload(basic, emptyDetail, resume));
      setResume(result.data);
      setStep('detail');
    } catch (errorValue) {
      setError(errorValue instanceof Error ? errorValue.message : '保存基本信息失败，请稍后重试。');
    } finally {
      setSaving(false);
    }
  }

  async function submitDetail(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) {
      setStatus('unauthorized');
      return;
    }
    setSaving(true);
    setMessage(undefined);
    setError(undefined);
    try {
      const result = await saveCandidateResume(token, buildResumePayload(basic, detail, resume));
      setResume(result.data);
      setStep('done');
    } catch (errorValue) {
      setError(errorValue instanceof Error ? errorValue.message : '保存简历失败，请稍后重试。');
    } finally {
      setSaving(false);
    }
  }

  async function uploadAttachment(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) {
      return;
    }
    setMessage(undefined);
    setError(undefined);
    if (!token) {
      setStatus('unauthorized');
      return;
    }
    if (!basic.displayName.trim() || !basic.contactPhone.trim() || !basic.expectedPositions.trim() || !basic.expectedCities.trim()) {
      setMessage('请先补充姓名、联系电话、期望职位和期望地区，再上传附件简历。');
      return;
    }

    setAttachmentBusy(true);
    try {
      let currentResume = resume;
      if (!currentResume?.resume_id) {
        const saved = await saveCandidateResume(token, buildResumePayload(basic, emptyDetail, resume));
        currentResume = saved.data;
        setResume(saved.data);
      }
      const result = await uploadCandidateResumeAttachment(token, file);
      setAttachment(result.data);
      setResume(currentResume ? { ...currentResume, has_attachment: result.data.has_attachment } : currentResume);
      setMessage('附件简历已上传到本人私有域，不会进入公开人才服务区。');
    } catch (errorValue) {
      setError(errorValue instanceof Error ? errorValue.message : '上传附件失败，请稍后重试。');
    } finally {
      setAttachmentBusy(false);
    }
  }

  async function refreshResumeState(activeToken: string) {
    const closure = await fetchCandidateClosureData(activeToken);
    setResume(closure.resume);
    setBasic(basicFromResume(closure.resume));
    setDetail(detailFromResume(closure.resume));
    return closure.resume;
  }

  async function generateAiSuggestions() {
    if (!token) {
      setStatus('unauthorized');
      return;
    }
    if (!aiEnabled) {
      setMessage('AI 优化建议当前未开启，继续展示安全占位。');
      return;
    }
    setAiBusy(true);
    setMessage(undefined);
    setError(undefined);
    try {
      const saved = await saveCandidateResume(token, buildResumePayload(basic, detail, resume));
      setResume(saved.data);
      const result = await generateCandidateResumeAiSuggestions(token);
      setAiTask(result.data);
      setMessage('已生成安全规则版优化建议。建议只在本人私有域展示，需手动逐条应用。');
    } catch (errorValue) {
      setError(errorValue instanceof Error ? errorValue.message : '生成优化建议失败，请稍后重试。');
    } finally {
      setAiBusy(false);
    }
  }

  async function applyAiSuggestion(suggestionId: number) {
    if (!token) {
      setStatus('unauthorized');
      return;
    }
    setAiBusy(true);
    setMessage(undefined);
    setError(undefined);
    try {
      const result = await applyCandidateResumeAiSuggestion(token, suggestionId);
      setAiTask(result.data);
      await refreshResumeState(token);
      setMessage('已手动应用该条优化建议，并重新读取服务端简历状态。');
    } catch (errorValue) {
      setError(errorValue instanceof Error ? errorValue.message : '应用优化建议失败，请稍后重试。');
    } finally {
      setAiBusy(false);
    }
  }

  async function dismissAiSuggestion(suggestionId: number) {
    if (!token) {
      setStatus('unauthorized');
      return;
    }
    setAiBusy(true);
    setMessage(undefined);
    setError(undefined);
    try {
      const result = await dismissCandidateResumeAiSuggestion(token, suggestionId);
      setAiTask(result.data);
      setMessage('已忽略该条优化建议，服务端已记录状态。');
    } catch (errorValue) {
      setError(errorValue instanceof Error ? errorValue.message : '忽略优化建议失败，请稍后重试。');
    } finally {
      setAiBusy(false);
    }
  }

  function renderAiSuggestions() {
    const items = aiTask?.items ?? [];
    return (
      <section className={`${styles.section} ${styles.aiSection}`} aria-label="安全规则版简历优化建议">
        <div className={styles.aiHeader}>
          <div>
            <h2 className={styles.sectionTitle}>智能优化建议</h2>
            <p>
              本功能为本地规则建议，不调用外部模型、不上传原始候选人数据；建议需本人手动逐条应用。
            </p>
          </div>
          {aiEnabled ? (
            <button className={styles.aiButton} type="button" disabled={aiBusy} onClick={generateAiSuggestions}>
              {aiBusy ? '正在处理' : '生成优化建议'}
            </button>
          ) : (
            <button className={styles.aiButtonDisabled} type="button" disabled aria-disabled="true">
              AI 优化暂未开放
            </button>
          )}
        </div>
        {items.length > 0 ? (
          <div className={styles.aiList}>
            {items.map((item) => (
              <article className={styles.aiCard} key={item.suggestion_id}>
                <div className={styles.aiCardTop}>
                  <strong>{item.title}</strong>
                  <span>{item.apply_status}</span>
                </div>
                <p>{item.reason_summary}</p>
                {item.before_preview ? <p className={styles.aiPreview}>当前：{item.before_preview}</p> : null}
                {item.suggested_value ? <p className={styles.aiPreview}>建议：{item.suggested_value}</p> : null}
                <div className={styles.aiActions}>
                  <button
                    className={styles.secondary}
                    type="button"
                    disabled={!item.can_apply || item.apply_status !== 'pending' || aiBusy}
                    onClick={() => applyAiSuggestion(item.suggestion_id)}
                  >
                    手动应用
                  </button>
                  <button
                    className={styles.aiGhost}
                    type="button"
                    disabled={item.apply_status !== 'pending' || aiBusy}
                    onClick={() => dismissAiSuggestion(item.suggestion_id)}
                  >
                    忽略
                  </button>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <p className={styles.aiEmpty}>
            {aiEnabled ? '暂无优化建议，可先补充工作经历和自我描述后生成。' : '当前环境默认关闭 AI 优化建议，继续展示安全占位。'}
          </p>
        )}
      </section>
    );
  }

  if (status === 'loading') {
    return (
      <PageChrome step="basic">
        <StateView variant="loading" title="正在读取简历创建流程" description="状态由服务端返回，前端不会自行推断。" />
      </PageChrome>
    );
  }

  if (status === 'unauthorized') {
    return (
      <PageChrome step="basic">
        <StateView variant="unauthorized" title="请先登录求职者账号" description="完善简历属于本人私有域，需要求职者身份。" />
      </PageChrome>
    );
  }

  if (status === 'disabled') {
    return (
      <PageChrome step="basic">
        <StateView variant="empty" title="三期求职者闭环未开启" description="当前环境未开启 phase3.candidate_closure，暂不进入简历创建流程。" />
      </PageChrome>
    );
  }

  if (status === 'error') {
    return (
      <PageChrome step="basic">
        <StateView variant="error" title="简历创建流程暂时不可用" description={error ?? '请稍后重试。'} />
      </PageChrome>
    );
  }

  if (step === 'done') {
    return (
      <PageChrome step="done">
        <section className={`${styles.panel} ${styles.successPanel}`} aria-label="简历创建完成">
          <div className={styles.successIcon} aria-hidden="true">✓</div>
          <h1>恭喜您，简历创建完成</h1>
          <p>
            您的简历已保存到本人私有域。公开人才服务区仍只展示服务端生成的发布快照，
            原始简历、联系电话、附件和证据不会进入公开门户。
          </p>
          <p className={styles.successMeta}>
            附件状态：{attachment?.has_attachment ? `${attachment.file_name || '已上传附件'} · ${attachment.size_bytes ?? 0} bytes` : '暂无附件'}
          </p>
          <div className={styles.actions}>
            <button className={styles.primary} type="button" onClick={() => setStep('detail')}>
              继续完善简历
            </button>
            <Link className={styles.secondary} href="/candidate/center">
              进入会员中心
            </Link>
          </div>
          <div className={styles.qrCard}>
            <span className={styles.fakeQr} aria-hidden="true" />
            <p>微信扫码关注公众号为占位展示；真实微信、小程序和 App 能力暂不接入。</p>
          </div>
        </section>
      </PageChrome>
    );
  }

  return (
    <PageChrome step={step}>
      {step === 'basic' ? (
        <>
          <section className={styles.importBanner} aria-label="附件简历导入占位">
            <div>
              <h1>导入附件简历，一键完成在线简历填写</h1>
              <p>
                附件只保存在求职者本人私有域，不进入公开门户、人才服务区、搜索、sitemap 或导出旁路。
              </p>
              {attachment?.has_attachment ? (
                <p className={styles.attachmentMeta}>
                  已上传：{attachment.file_name || '附件简历'} · {attachment.size_bytes ?? 0} bytes
                </p>
              ) : null}
            </div>
            {attachmentEnabled ? (
              <label className={attachmentBusy ? styles.disabledOrange : styles.uploadButton}>
                {attachmentBusy ? '正在上传' : attachment?.has_attachment ? '替换附件' : '上传附件'}
                <input
                  aria-label="上传附件简历"
                  className={styles.hiddenFile}
                  type="file"
                  accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                  disabled={attachmentBusy}
                  onChange={uploadAttachment}
                />
              </label>
            ) : (
              <button className={styles.disabledOrange} type="button" disabled aria-disabled="true">
                附件上传未开启
              </button>
            )}
          </section>
          <form className={styles.panel} aria-label="完善简历基本信息表单" onSubmit={submitBasic}>
            {message ? <p className={styles.notice}>{message}</p> : null}
            {error ? <p className={styles.notice}>{error}</p> : null}
            <section className={styles.section}>
              <h2 className={styles.sectionTitle}>基本信息</h2>
              <div className={styles.formGrid}>
                <label className={styles.field}>
                  <span><span className={styles.required}>*</span> 姓名</span>
                  <input value={basic.displayName} onChange={(event) => updateBasic('displayName', event.target.value)} placeholder="请填写姓名" />
                </label>
                <div className={styles.field}>
                  <span><span className={styles.required}>*</span> 性别</span>
                  <div className={styles.segmented}>
                    {['男', '女'].map((gender) => (
                      <button
                        key={gender}
                        className={basic.gender === gender ? styles.pillActive : styles.pill}
                        type="button"
                        onClick={() => updateBasic('gender', gender)}
                      >
                        {gender}
                      </button>
                    ))}
                  </div>
                </div>
                <label className={styles.field}>
                  <span>出生日期</span>
                  <input type="date" value={basic.birthDate} onChange={(event) => updateBasic('birthDate', event.target.value)} />
                </label>
                <label className={styles.field}>
                  <span>最高学历</span>
                  <select value={basic.highestEducation} onChange={(event) => updateBasic('highestEducation', event.target.value)}>
                    <option value="">请选择最高学历</option>
                    <option value="高中">高中</option>
                    <option value="大专">大专</option>
                    <option value="本科">本科</option>
                    <option value="硕士">硕士</option>
                    <option value="博士">博士</option>
                  </select>
                </label>
                <label className={styles.field}>
                  <span>开始工作时间</span>
                  <input type="date" value={basic.startWorkDate} disabled={basic.noExperience} onChange={(event) => updateBasic('startWorkDate', event.target.value)} />
                  <span className={styles.checkLine}>
                    <input type="checkbox" checked={basic.noExperience} onChange={(event) => updateBasic('noExperience', event.target.checked)} />
                    应届生/无经验
                  </span>
                </label>
                <label className={styles.field}>
                  <span><span className={styles.required}>*</span> 联系电话</span>
                  <input value={basic.contactPhone} onChange={handlePhoneChange} placeholder="请填写联系电话" inputMode="tel" />
                </label>
                <label className={styles.field}>
                  <span>联系微信</span>
                  <input value={contactWechatValue} disabled={basic.wechatSameAsPhone} onChange={(event) => updateBasic('contactWechat', event.target.value)} placeholder="请填写联系微信" />
                  <span className={styles.checkLine}>
                    <input
                      type="checkbox"
                      checked={basic.wechatSameAsPhone}
                      onChange={(event) => updateBasic('wechatSameAsPhone', event.target.checked)}
                    />
                    同手机
                  </span>
                </label>
              </div>
            </section>
            <section className={styles.section}>
              <h2 className={styles.sectionTitle}>求职意向</h2>
              <div className={styles.formGrid}>
                <label className={styles.field}>
                  <span><span className={styles.required}>*</span> 期望职位</span>
                  <input value={basic.expectedPositions} onChange={(event) => updateBasic('expectedPositions', event.target.value)} placeholder="请输入期望职位，最多 5 个，用逗号分隔" />
                </label>
                <label className={styles.field}>
                  <span>期望薪资</span>
                  <select value={basic.expectedSalary} onChange={(event) => updateBasic('expectedSalary', event.target.value)}>
                    <option value="">请选择期望薪资</option>
                    <option value="5k-8k">5k-8k</option>
                    <option value="8k-12k">8k-12k</option>
                    <option value="12k-20k">12k-20k</option>
                    <option value="20k以上">20k以上</option>
                  </select>
                </label>
                <label className={styles.field}>
                  <span><span className={styles.required}>*</span> 期望地区</span>
                  <input value={basic.expectedCities} onChange={(event) => updateBasic('expectedCities', event.target.value)} placeholder="请输入期望地区，最多 5 个，用逗号分隔" />
                </label>
                <label className={styles.field}>
                  <span>求职状态</span>
                  <select value={basic.jobStatus} onChange={(event) => updateBasic('jobStatus', event.target.value)}>
                    <option value="我目前已离职，可快速到岗">我目前已离职，可快速到岗</option>
                    <option value="我目前在职，正考虑换个新环境">我目前在职，正考虑换个新环境</option>
                    <option value="观望有好机会再考虑">观望有好机会再考虑</option>
                    <option value="应届毕业生">应届毕业生</option>
                  </select>
                </label>
              </div>
            </section>
            <div className={styles.actions}>
              <button className={styles.primary} type="submit" disabled={saving}>{saving ? '正在保存' : '下一步'}</button>
            </div>
          </form>
        </>
      ) : (
        <form className={styles.panel} aria-label="完善简历详情表单" onSubmit={submitDetail}>
          {message ? <p className={styles.notice}>{message}</p> : null}
          {error ? <p className={styles.notice}>{error}</p> : null}
          <section className={styles.section}>
            <h2 className={styles.sectionTitle}>工作经历</h2>
            <div className={styles.formGrid}>
              <label className={styles.field}>
                <span>公司名称</span>
                <input value={detail.companyName} onChange={(event) => updateDetail('companyName', event.target.value)} placeholder="请填写公司名称" />
              </label>
              <label className={styles.field}>
                <span>职位名称</span>
                <input value={detail.positionName} onChange={(event) => updateDetail('positionName', event.target.value)} placeholder="请填写职位名称" />
              </label>
              <label className={styles.field}>
                <span>入职时间</span>
                <input type="date" value={detail.workStartDate} onChange={(event) => updateDetail('workStartDate', event.target.value)} />
              </label>
              <label className={styles.field}>
                <span>离职时间</span>
                <input type="date" value={detail.workEndDate} disabled={detail.workOngoing} onChange={(event) => updateDetail('workEndDate', event.target.value)} />
                <span className={styles.checkLine}>
                  <input type="checkbox" checked={detail.workOngoing} onChange={(event) => updateDetail('workOngoing', event.target.checked)} />
                  至今
                </span>
              </label>
              <label className={`${styles.field} ${styles.fullWidth}`}>
                <span>工作职责</span>
                <textarea value={detail.responsibility} onChange={(event) => updateDetail('responsibility', event.target.value)} placeholder="请描述你的工作内容和项目经验" />
              </label>
            </div>
          </section>
          <section className={styles.section}>
            <h2 className={styles.sectionTitle}>教育经历</h2>
            <div className={styles.formGrid}>
              <label className={styles.field}>
                <span>学校名称</span>
                <input value={detail.schoolName} onChange={(event) => updateDetail('schoolName', event.target.value)} placeholder="请填写学校名称" />
              </label>
              <label className={styles.field}>
                <span>专业名称</span>
                <input value={detail.majorName} onChange={(event) => updateDetail('majorName', event.target.value)} placeholder="请填写专业名称" />
              </label>
              <label className={styles.field}>
                <span>入学时间</span>
                <input type="date" value={detail.educationStartDate} onChange={(event) => updateDetail('educationStartDate', event.target.value)} />
              </label>
              <label className={styles.field}>
                <span>毕业时间</span>
                <input type="date" value={detail.educationEndDate} disabled={detail.educationOngoing} onChange={(event) => updateDetail('educationEndDate', event.target.value)} />
                <span className={styles.checkLine}>
                  <input type="checkbox" checked={detail.educationOngoing} onChange={(event) => updateDetail('educationOngoing', event.target.checked)} />
                  至今
                </span>
              </label>
              <label className={styles.field}>
                <span>取得学历</span>
                <select value={detail.degree} onChange={(event) => updateDetail('degree', event.target.value)}>
                  <option value="">请选择取得学历</option>
                  <option value="大专">大专</option>
                  <option value="本科">本科</option>
                  <option value="硕士">硕士</option>
                  <option value="博士">博士</option>
                </select>
              </label>
            </div>
          </section>
          <section className={styles.section}>
            <h2 className={styles.sectionTitle}>自我描述</h2>
            <div className={styles.formGrid}>
              <label className={`${styles.field} ${styles.fullWidth}`}>
                <span>自我描述</span>
                <textarea value={detail.selfDescription} onChange={(event) => updateDetail('selfDescription', event.target.value)} placeholder="请用一段话介绍你的优势、求职方向和亮点" />
              </label>
            </div>
          </section>
          {renderAiSuggestions()}
          <div className={styles.actions}>
            <button className={styles.secondary} type="button" onClick={() => setStep('basic')}>上一步</button>
            <button className={styles.primary} type="submit" disabled={saving}>{saving ? '正在保存' : '完成'}</button>
          </div>
        </form>
      )}
    </PageChrome>
  );
}
