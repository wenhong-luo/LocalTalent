import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { ExportDownloadAction } from '@/components/backoffice/ExportDownloadAction';
import { ACCESS_TOKEN_STORAGE_KEY } from '@/pages/backoffice/session';
import { CompanyDashboard } from './CompanyDashboard';

function apiOk(data: unknown, traceId = 'trace-company'): Response {
  return new Response(
    JSON.stringify({
      code: '0',
      message: 'success',
      trace_id: traceId,
      data
    }),
    { status: 200, headers: { 'Content-Type': 'application/json' } }
  );
}

function apiError(code: string, message: string, status = 403): Response {
  return new Response(
    JSON.stringify({
      code,
      message,
      trace_id: 'trace-disabled',
      data: null
    }),
    { status, headers: { 'Content-Type': 'application/json' } }
  );
}

const legacyJobs = {
  job_list: [
    {
      job_id: 101,
      title: '后端工程师',
      status: 2,
      audit_status: 2,
      reject_reason: '',
      updated_at: '2026-04-28T10:00:00'
    }
  ],
  total: 1
};

const legacyApplications = {
  application_list: [
    {
      application_id: 201,
      job_title: '后端工程师',
      company_name: '示例企业',
      status: 2,
      apply_time: '2026-04-28T11:00:00',
      mobile: '13900001234',
      email: 'candidate@example.com',
      resume_body: 'raw resume body',
      attachment_object_key: 'resume/private.pdf',
      evidence: 'consent evidence',
      password_hash: 'bcrypt-hash'
    }
  ],
  total: 1
};

const workbenchOverview = {
  profile: {
    company_id: 21,
    company_name: '示例企业',
    industry_code: 'software',
    nature_code: 'private',
    scale_code: '50-200',
    city_code: '310000',
    address: '上海市示例园区',
    company_profile: '企业公开简介摘要',
    registered_capital_amount: '800',
    registered_capital_unit: 'cny_10k',
    website_url: 'https://company.example.local',
    benefit_codes: ['five_insurance', 'annual_leave'],
    contact_name: '王招聘',
    contact_mobile: '18877776666',
    contact_mobile_hidden: true,
    contact_wechat: 'lt-hr',
    contact_wechat_same_mobile: false,
    contact_phone: '021-88889999',
    contact_email: 'hr@example.local',
    contact_qq: '123456',
    auth_status: 2,
    reject_reason: '',
    certification_material_summary: {
      material_summary: '已核验基础材料',
      attachment_object_key: 'license/private.pdf'
    },
    updated_at: '2026-05-01T10:00:00'
  },
  stats: {
    job_total: 2,
    application_total: 3,
    pending_application_total: 1,
    interview_total: 1,
    export_total: 1
  },
  features: { company_workbench_enabled: true, company_style_upload_enabled: false, company_logo_upload_enabled: false }
};

const styleEnabledWorkbenchOverview = {
  ...workbenchOverview,
  features: { company_workbench_enabled: true, company_style_upload_enabled: true, company_logo_upload_enabled: false }
};

const logoEnabledWorkbenchOverview = {
  ...workbenchOverview,
  features: { company_workbench_enabled: true, company_style_upload_enabled: false, company_logo_upload_enabled: true }
};

const companyLogo = {
  has_logo: true,
  logo_status: 'uploaded',
  file_name: 'logo.png',
  content_type: 'image/png',
  size_bytes: 1024,
  uploaded_at: '2026-05-11T10:30:00',
  content_url: '/api/company/workbench/logo/content',
  object_key: 'company-logo-assets/21/private.png',
  presigned_url: 'http://minio.local/logo.png'
};

const emptyCompanyLogo = {
  has_logo: false,
  logo_status: 'empty',
  file_name: '',
  content_type: '',
  size_bytes: null,
  uploaded_at: '',
  content_url: ''
};

const companyStyleImages = {
  image_list: [
    {
      image_id: 901,
      file_name: 'office.webp',
      content_type: 'image/webp',
      size_bytes: 2048,
      display_order: 10,
      status: 1,
      review_status: 0,
      uploaded_at: '2026-05-11T10:00:00',
      content_url: '/api/company/workbench/style-images/901/content',
      object_key: 'company-style-images/21/private.webp',
      presigned_url: 'http://minio.local/private.webp'
    }
  ],
  total: 1
};

const emptyCompanyStyleImages = {
  image_list: [],
  total: 0
};

const fullCompanyStyleImages = {
  image_list: Array.from({ length: 6 }, (_, index) => ({
    image_id: 901 + index,
    file_name: `office-${index + 1}.webp`,
    content_type: 'image/webp',
    size_bytes: 2048 + index,
    display_order: (index + 1) * 10,
    status: 1,
    review_status: 0,
    uploaded_at: '2026-05-11T10:00:00',
    content_url: `/api/company/workbench/style-images/${901 + index}/content`
  })),
  total: 6
};

const incompleteWorkbenchOverview = {
  ...workbenchOverview,
  profile: {
    ...workbenchOverview.profile,
    company_name: '',
    industry_code: '',
    nature_code: '',
    scale_code: '',
    city_code: '',
    address: '',
    company_profile: '',
    contact_name: '',
    contact_mobile: ''
  }
};

const contactMissingWorkbenchOverview = {
  ...workbenchOverview,
  profile: {
    ...workbenchOverview.profile,
    contact_name: '',
    contact_mobile: ''
  }
};

const pendingCertificationWorkbenchOverview = {
  ...workbenchOverview,
  profile: {
    ...workbenchOverview.profile,
    auth_status: 1
  }
};

const workbenchJobs = {
  job_list: [
    {
      job_id: 501,
      title: '三期招聘顾问',
      job_nature_code: 'full_time',
      category_code: 'software',
      category_name: '互联网/电子商务',
      experience_code: '1_3_years',
      education_code: 'college',
      recruit_count: 3,
      city_code: '310000',
      work_region_path: '上海 / 上海市 / 浦东新区',
      address: '上海市浦东新区演示大道 100 号',
      salary_min: 12000,
      salary_max: 22000,
      salary_negotiable: false,
      welfare_codes: ['five_insurance', 'weekend_double'],
      department_name: '招聘运营部',
      age_min: null,
      age_max: null,
      age_unlimited: true,
      recruitment_time_code: 'long_term',
      contact_mode: 'company_profile',
      contact_name: '',
      contact_mobile: '',
      contact_phone: '',
      contact_email: '',
      contact_wechat: '',
      contact_hidden: true,
      notify_enabled: false,
      resume_subscription_enabled: false,
      job_desc: '负责本地人才服务平台灰度试运营支持。',
      status: 1,
      audit_status: 1,
      reject_reason: '',
      updated_at: '2026-05-01T10:10:00'
    }
  ],
  total: 1
};

const deletedWorkbenchJobs = {
  job_list: [
    {
      ...workbenchJobs.job_list[0],
      job_id: 801,
      title: '已删除职位',
      status: 3,
      audit_status: 1,
      reject_reason: '',
      updated_at: '2026-05-04T10:00:00',
      deleted_at: '2026-05-05T10:00:00',
      delete_reason: '企业工作台软删除职位。'
    }
  ],
  total: 1
};

const emptyDeletedWorkbenchJobs = {
  job_list: [],
  total: 0
};

const workbenchApplications = {
  application_list: [
    {
      application_id: 601,
      job_id: 501,
      job_title: '三期招聘顾问',
      application_status: 1,
      status_label: '待筛选',
      apply_time: '2026-05-01T11:00:00',
      display_name_masked: '张*',
      city_code: '310000',
      skills_summary: 'Java, Spring',
      experience_years: 5,
      has_resume_attachment: true,
      company_stage_note: '',
      stage_changed_at: '',
      mobile: '13900001234',
      email: 'candidate@example.com',
      resume_body: 'raw resume body',
      attachment_object_key: 'resume/private.pdf',
      evidence: 'consent evidence',
      base_profile_json: '{"name":"raw"}',
      education_json: '[]',
      experience_json: '[]',
      skills_json: '[]',
      password_hash: 'bcrypt-hash',
      审核材料: 'private audit material',
      营业执照附件: 'private license'
    }
  ],
  total: 1
};

const workbenchSessions = {
  session_list: [
    {
      session_id: 701,
      application_id: 601,
      job_id: 501,
      job_title: '三期招聘顾问',
      status: 1,
      session_name: '初试邀约',
      session_time: '2026-05-02T10:00:00',
      location: '站内面试邀约'
    }
  ],
  total: 1
};

function installCompanyFetchMock({
  workbenchEnabled = true,
  overview = workbenchOverview,
  completeAfterProfileSave = false,
  styleImages = companyStyleImages,
  logo = companyLogo,
  jobPage = workbenchJobs,
  deletedJobPage = emptyDeletedWorkbenchJobs
}: {
  workbenchEnabled?: boolean;
  overview?: typeof workbenchOverview;
  completeAfterProfileSave?: boolean;
  styleImages?: { image_list: Array<Record<string, unknown>>; total: number };
  logo?: Record<string, unknown>;
  jobPage?: typeof workbenchJobs;
  deletedJobPage?: typeof workbenchJobs;
} = {}) {
  let currentOverview = overview;
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const url = String(input);
    const method = init?.method ?? 'GET';

    if (url.includes('/api/auth/me')) {
      return apiOk({
        identity_type: 'company',
        user_id: 11,
        company_id: 21,
        display_name: '示例企业/管理员'
      });
    }

    if (url.includes('/api/company/workbench/overview')) {
      return workbenchEnabled ? apiOk(currentOverview) : apiError('FEATURE_DISABLED_403', 'company workbench disabled');
    }

    if (url.endsWith('/api/company/workbench/logo') && method === 'GET') {
      return apiOk(logo, 'trace-logo-get');
    }

    if (url.endsWith('/api/company/workbench/logo') && method === 'POST') {
      return apiOk(companyLogo, 'trace-logo-upload');
    }

    if (url.endsWith('/api/company/workbench/logo/content')) {
      return new Response(new Blob(['fake logo'], { type: 'image/png' }), {
        status: 200,
        headers: { 'Content-Type': 'image/png' }
      });
    }

    if (url.endsWith('/api/company/workbench/logo') && method === 'DELETE') {
      return apiOk(emptyCompanyLogo, 'trace-logo-delete');
    }

    if (url.endsWith('/api/company/workbench/style-images') && method === 'GET') {
      return apiOk(styleImages, 'trace-style-list');
    }

    if (url.endsWith('/api/company/workbench/style-images') && method === 'POST') {
      return apiOk(companyStyleImages.image_list[0], 'trace-style-upload');
    }

    if (url.endsWith('/api/company/workbench/style-images/order') && method === 'PUT') {
      return apiOk(companyStyleImages, 'trace-style-order');
    }

    if (url.includes('/api/company/workbench/style-images/') && url.includes('/content')) {
      return new Response(new Blob(['fake image'], { type: 'image/webp' }), {
        status: 200,
        headers: { 'Content-Type': 'image/webp' }
      });
    }

    if (url.includes('/api/company/workbench/style-images/901') && method === 'DELETE') {
      return apiOk({ image_list: [], total: 0 }, 'trace-style-delete');
    }

    if (url.includes('/api/company/workbench/jobs/deleted') && method === 'GET') {
      return apiOk(deletedJobPage, 'trace-deleted-jobs');
    }

    if (url.includes('/api/company/workbench/jobs') && method === 'GET') {
      return apiOk(jobPage);
    }

    if (url.includes('/api/company/workbench/applications') && url.includes('/stage')) {
      return apiOk({
        ...workbenchApplications.application_list[0],
        application_status: 2,
        status_label: '邀约面试',
        company_stage_note: '企业已查看投递，进入面试沟通流程。'
      }, 'trace-stage');
    }

    if (url.includes('/api/company/workbench/applications') && url.includes('/interview-sessions')) {
      return apiOk(workbenchSessions.session_list[0], 'trace-interview');
    }

    if (url.includes('/api/company/workbench/applications')) {
      return apiOk(workbenchApplications);
    }

    if (url.includes('/api/company/workbench/interview-sessions')) {
      return apiOk(workbenchSessions);
    }

    if (url.endsWith('/api/company/workbench/profile') && method === 'PUT') {
      if (completeAfterProfileSave) {
        currentOverview = workbenchOverview;
      }
      return apiOk(currentOverview.profile, 'trace-profile');
    }

    if (url.endsWith('/api/company/workbench/certification') && method === 'POST') {
      return apiOk(workbenchOverview.profile, 'trace-certification');
    }

    if (url.endsWith('/api/company/workbench/jobs') && method === 'POST') {
      return apiOk(workbenchJobs.job_list[0], 'trace-job-create');
    }

    if (url.includes('/api/company/workbench/jobs/') && method === 'PUT') {
      return apiOk(workbenchJobs.job_list[0], 'trace-job-update');
    }

    if (url.includes('/api/company/workbench/jobs/') && url.includes('/restore-draft') && method === 'POST') {
      return apiOk({ ...workbenchJobs.job_list[0], status: 1, audit_status: 1 }, 'trace-job-restore');
    }

    if (url.includes('/api/company/workbench/jobs/') && method === 'POST') {
      return apiOk(workbenchJobs.job_list[0], 'trace-job-action');
    }

    if (url.endsWith('/api/company/workbench/exports') && method === 'POST') {
      return apiOk({
        export_id: 801,
        biz_type: 'application_candidate_detail',
        approve_status: 0,
        generate_status: 0,
        reason: '导出本企业投递池候选人摘要，用于招聘流程复核。',
        reject_reason: '',
        download_count: 0,
        created_at: '2026-05-01T12:00:00'
      }, 'trace-export');
    }

    if (url.includes('/api/company/jobs')) {
      return apiOk(legacyJobs);
    }

    if (url.includes('/api/company/applications')) {
      return apiOk(legacyApplications);
    }

    if (method === 'POST' && url.endsWith('/api/company/apply')) {
      return apiOk({
        company_id: 21,
        auth_status: 2,
        reject_reason: ''
      }, 'trace-company-apply');
    }

    if (method === 'POST' && url.endsWith('/api/company/exports')) {
      return apiOk({
        export_id: 301,
        biz_type: 'application_candidate_detail',
        approve_status: 0,
        generate_status: 0,
        reason: '导出本企业投递池候选人明细，用于招聘流程复核。',
        reject_reason: '',
        download_count: 0,
        created_at: '2026-04-28T12:00:00'
      });
    }

    return apiOk({});
  });
}

function bodyText(): string {
  return document.body.textContent ?? '';
}

function latestWorkbenchProfilePayload(fetchMock: ReturnType<typeof vi.spyOn>): Record<string, unknown> {
  const call = [...fetchMock.mock.calls].reverse().find(([input, init]) => {
    const url = String(input);
    return url.endsWith('/api/company/workbench/profile') && init?.method === 'PUT';
  });
  if (!call) {
    throw new Error('workbench profile payload not found');
  }
  return JSON.parse(String(call[1]?.body ?? '{}')) as Record<string, unknown>;
}

function latestWorkbenchJobPayload(fetchMock: ReturnType<typeof vi.spyOn>): Record<string, unknown> {
  const call = [...fetchMock.mock.calls].reverse().find(([input, init]) => {
    const url = String(input);
    return (
      (url.endsWith('/api/company/workbench/jobs') && init?.method === 'POST')
      || (url.includes('/api/company/workbench/jobs/') && init?.method === 'PUT')
    );
  });
  if (!call) {
    throw new Error('workbench job payload not found');
  }
  return JSON.parse(String(call[1]?.body ?? '{}')) as Record<string, unknown>;
}

function expectNoSensitiveCandidateOrCompanyFields() {
  const body = bodyText();
  expect(body).not.toContain('13900001234');
  expect(body).not.toContain('candidate@example.com');
  expect(body).not.toContain('raw resume body');
  expect(body).not.toContain('resume/private.pdf');
  expect(body).not.toContain('consent evidence');
  expect(body).not.toContain('bcrypt-hash');
    expect(body).not.toContain('license/private.pdf');
  expect(body).not.toContain('company-logo-assets/21/private.png');
  expect(body).not.toContain('http://minio.local/logo.png');
  expect(body).not.toContain('private audit material');
  expect(body).not.toContain('mobile');
  expect(body).not.toContain('email');
  expect(body).not.toContain('resume_body');
  expect(body).not.toContain('attachment_object_key');
  expect(body).not.toContain('evidence');
  expect(body).not.toContain('password_hash');
  expect(body).not.toContain('base_profile_json');
  expect(body).not.toContain('education_json');
  expect(body).not.toContain('experience_json');
  expect(body).not.toContain('skills_json');
  expect(body).not.toContain('营业执照附件');
  expect(body).not.toContain('审核材料');
  expect(body).not.toContain('presigned_url');
  expect(body).not.toContain('object_key');
}

describe('CompanyDashboard', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
    Object.defineProperty(URL, 'createObjectURL', { value: vi.fn(() => 'blob:http://localhost/style-image'), configurable: true });
    Object.defineProperty(URL, 'revokeObjectURL', { value: vi.fn(), configurable: true });
  });

  it('falls back to phase two summary when company workbench is disabled', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    installCompanyFetchMock({ workbenchEnabled: false });

    render(<CompanyDashboard />);

    expect(await screen.findByText('三期企业工作台未开启')).toBeInTheDocument();
    expect(screen.getAllByText('后端工程师').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('投递池最小列表')).toBeInTheDocument();
    expectNoSensitiveCandidateOrCompanyFields();
  });

  it('renders company workbench modules without leaking sensitive fields', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    installCompanyFetchMock();

    render(<CompanyDashboard />);

    expect(await screen.findByText('企业会员首页')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /^职位管理$/ }));
    expect(await screen.findByText('职位工作台')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /^基本资料$/ }));
    expect(screen.getByText('企业资料')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /^简历管理$/ }));
    expect(screen.getByText('投递池')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '创建站内面试邀约' })).toBeInTheDocument();
    expect(screen.getByText('张* / 310000')).toBeInTheDocument();
    expect(screen.getByText('Java, Spring')).toBeInTheDocument();
    expectNoSensitiveCandidateOrCompanyFields();
  });

  it('renders the employer member home dashboard with safe placeholders', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    installCompanyFetchMock();

    render(<CompanyDashboard />);

    expect(await screen.findByText('我的公司')).toBeInTheDocument();
    expect(screen.getByText('我的人才')).toBeInTheDocument();
    expect(screen.getByText('我的机会')).toBeInTheDocument();
    expect(screen.getByText('招聘岗位')).toBeInTheDocument();
    expect(screen.getAllByText('我的套餐').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('人才推荐')).toBeInTheDocument();
    expect(screen.getByText('我的信息')).toBeInTheDocument();
    expect(screen.getByText('我的会员')).toBeInTheDocument();
    expect(screen.getByText('快捷菜单')).toBeInTheDocument();
    expect(screen.getByText('专属客服')).toBeInTheDocument();
    expect(screen.getByText(/会员商业化、真实支付、发票与订单暂未开放/)).toBeInTheDocument();
    expect(screen.getByText(/不开放公共简历库、联系解锁或候选人搜索/)).toBeInTheDocument();

    fireEvent.click(screen.getAllByRole('button', { name: '编辑资料' })[0]);
    expect(await screen.findByText('企业资料')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /^会员首页$/ }));
    expect(await screen.findByText('我的公司')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /^发布职位$/ }));
    expect(await screen.findByText('职位工作台')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /^会员首页$/ }));
    expect(await screen.findByText('我的公司')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /收到投递/ }));
    expect(await screen.findByText('投递池')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /^会员首页$/ }));
    expect(await screen.findByText('我的公司')).toBeInTheDocument();
    fireEvent.click(screen.getAllByRole('button', { name: /^搜索人才$/ })[0]);
    expect(await screen.findByText('搜索简历：风险池占位')).toBeInTheDocument();
    expect(bodyText()).not.toContain('支付链接');
    expect(bodyText()).not.toContain('联系解锁入口');
    expectNoSensitiveCandidateOrCompanyFields();
  });

  it('renders high fidelity job management tabs with safe promotion placeholders', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    const fetchMock = installCompanyFetchMock();

    render(<CompanyDashboard />);

    expect(await screen.findByText('企业会员首页')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /^职位管理$/ }));

    expect(screen.getByTestId('company-workspace')).toHaveAttribute('data-layout', 'fluid');
    expect(await screen.findByText('管理职位')).toBeInTheDocument();
    expect(screen.getByText('职位工作台')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /\+ 发布职位/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /发布中/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /审核中/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /已下线/ })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /已删除/ })).toBeInTheDocument();
    expect(screen.getByText('招聘情况')).toBeInTheDocument();
    expect(screen.getByText('收到简历')).toBeInTheDocument();
    expect(screen.getByText('简历订阅')).toBeInTheDocument();
    expect(screen.getByText('职位推广')).toBeInTheDocument();
    expect(screen.getByText(/不产生真实套餐权益/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: /审核中/ }));
    expect(await screen.findByText('三期招聘顾问')).toBeInTheDocument();
    expect(screen.getByText((_, element) => element?.textContent === '被投递 1 次')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '提交审核' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '职位置顶' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '智能刷新' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '紧急招聘' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '一键分享（占位）' })).toBeDisabled();

    fireEvent.click(screen.getByLabelText('选择职位 三期招聘顾问'));
    expect(screen.getByRole('button', { name: '关闭所选' })).not.toBeDisabled();
    expect(screen.getByRole('button', { name: '刷新职位（占位）' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '删除职位' })).not.toBeDisabled();

    fireEvent.click(screen.getByRole('button', { name: /\+ 发布职位/ }));
    expect(screen.getByText('基本信息')).toBeInTheDocument();
    expect(screen.getByText('其他信息')).toBeInTheDocument();
    expect(screen.getAllByText('联系方式').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByRole('button', { name: /职位性质/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /经验要求/ })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /学历要求/ })).toBeInTheDocument();
    expect(screen.getByText('岗位福利')).toBeInTheDocument();
    expect(screen.getByText('接收通知（站内配置）')).toBeInTheDocument();
    expect(screen.getByText(/真实微信\/小程序\/App 能力暂未接入/)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText('职位名称 *'), { target: { value: '全量字段测试工程师' } });

    fireEvent.click(screen.getByRole('button', { name: /职位类别/ }));
    const positionDialog = screen.getByRole('dialog', { name: '职位类别' });
    fireEvent.click(within(positionDialog).getByRole('button', { name: '网络 | 通信 | 电子' }));
    fireEvent.click(within(positionDialog).getByLabelText('前端工程师'));
    fireEvent.click(within(positionDialog).getByRole('button', { name: '保存' }));

    fireEvent.click(screen.getByRole('button', { name: /工作地区/ }));
    fireEvent.click(screen.getByRole('button', { name: '山西' }));
    fireEvent.click(screen.getByRole('button', { name: '大同' }));
    fireEvent.click(screen.getByRole('button', { name: '云冈区' }));

    fireEvent.click(screen.getByRole('button', { name: /联系方式 使用企业资料联系方式/ }));
    fireEvent.click(screen.getByRole('option', { name: '使用本职位联系方式' }));
    fireEvent.change(screen.getByLabelText('联系手机'), { target: { value: '18877776666' } });
    fireEvent.change(screen.getByLabelText('联系邮箱'), { target: { value: 'hr-job@example.local' } });
    fireEvent.click(screen.getByRole('button', { name: '保存草稿' }));
    await waitFor(() => {
      expect(latestWorkbenchJobPayload(fetchMock).title).toBe('全量字段测试工程师');
    });
    const jobPayload = latestWorkbenchJobPayload(fetchMock);
    expect(jobPayload).toMatchObject({
      job_nature_code: 'full_time',
      category_code: 'network_communication_electronics',
      category_name: '前端工程师',
      experience_code: '1_3_years',
      education_code: 'college',
      recruit_count: 3,
      city_code: '140214',
      work_region_path: '山西 / 大同 / 云冈区',
      address: '上海市浦东新区演示大道 100 号',
      salary_min: 12000,
      salary_max: 22000,
      salary_negotiable: false,
      department_name: '招聘运营部',
      age_unlimited: true,
      recruitment_time_code: 'long_term',
      contact_mode: 'custom',
      contact_mobile: '18877776666',
      contact_email: 'hr-job@example.local',
      contact_hidden: true,
      notify_enabled: false,
      resume_subscription_enabled: false
    });
    expect(jobPayload.welfare_codes).toEqual(['five_insurance', 'weekend_double']);
    fireEvent.click(screen.getByRole('button', { name: /发布职位（提交审核）/ }));
    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([input]) => String(input).includes('/submit-review'))).toBe(true);
    });

    fireEvent.click(screen.getByRole('button', { name: '修改' }));
    expect(await screen.findByText('编辑职位')).toBeInTheDocument();
    expect(screen.getByLabelText('职位名称 *')).toHaveValue('三期招聘顾问');
    expect(screen.getByLabelText('招聘人数 *')).toHaveValue('3');
    expect(screen.getByLabelText('最低薪资')).toHaveValue('12000');
    expect(screen.getByLabelText('最高薪资')).toHaveValue('22000');
    fireEvent.change(screen.getByLabelText('职位名称 *'), { target: { value: '编辑后的职位' } });
    fireEvent.click(screen.getByRole('button', { name: '保存草稿' }));
    await waitFor(() => {
      const updated = [...fetchMock.mock.calls].some(([input, init]) =>
        String(input).includes('/api/company/workbench/jobs/501') && init?.method === 'PUT'
      );
      expect(updated).toBe(true);
    });
    expect(latestWorkbenchJobPayload(fetchMock).title).toBe('编辑后的职位');
    expect(bodyText()).not.toContain('支付链接');
    expect(bodyText()).not.toContain('联系解锁入口');
    expectNoSensitiveCandidateOrCompanyFields();
  });

  it('supports safe submit-review and bulk offline operations from job list', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    const actionableJobs = {
      job_list: [
        {
          ...workbenchJobs.job_list[0],
          job_id: 701,
          title: '可重新提交职位',
          status: 3,
          audit_status: 2,
          reject_reason: '',
          updated_at: '2026-05-02T10:10:00'
        },
        {
          ...workbenchJobs.job_list[0],
          job_id: 702,
          title: '已通过职位',
          status: 2,
          audit_status: 2,
          reject_reason: '',
          updated_at: '2026-05-02T11:10:00'
        }
      ],
      total: 2
    };
    const fetchMock = installCompanyFetchMock({ jobPage: actionableJobs });

    render(<CompanyDashboard />);

    fireEvent.click(await screen.findByRole('button', { name: /^职位管理$/ }));
    fireEvent.click(screen.getByRole('tab', { name: /已下线/ }));
    expect(await screen.findByText('可重新提交职位')).toBeInTheDocument();
    expect(screen.getByText('下线职位需要重新提交审核，审核通过后才会重新公开展示。')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '重新提交审核' }));
    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([input]) => String(input).includes('/api/company/workbench/jobs/701/submit-review'))).toBe(true);
    });
    expect(await screen.findByText('职位已重新提交审核，审核通过后将上线。')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: /发布中/ }));
    expect(await screen.findByText('已通过职位')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '提交审核' })).toBeDisabled();
    fireEvent.click(screen.getByLabelText('选择职位 已通过职位'));
    fireEvent.click(screen.getByRole('button', { name: '关闭所选' }));

    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([input]) => String(input).includes('/api/company/workbench/jobs/702/offline'))).toBe(true);
    });
    fireEvent.click(screen.getByLabelText('选择职位 可重新提交职位'));
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '删除职位' })).not.toBeDisabled();
    });
    fireEvent.click(screen.getByRole('button', { name: '删除职位' }));
    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([input]) => String(input).includes('/api/company/workbench/jobs/701/delete'))).toBe(true);
    });
    const actionCalls = fetchMock.mock.calls.filter(([input], index) => {
      const url = String(input);
      return index >= 0 && (url.includes('/submit-review') || url.includes('/offline') || url.includes('/delete')) && !url.includes('/jobs/deleted');
    });
    expect(actionCalls.every(([, init]) => init?.method === 'POST')).toBe(true);
    expect(bodyText()).not.toContain('支付链接');
    expect(bodyText()).not.toContain('联系解锁入口');
    expectNoSensitiveCandidateOrCompanyFields();
  });

  it('blocks submit-review actions in the UI until company certification is approved', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    const actionableJobs = {
      job_list: [
        {
          ...workbenchJobs.job_list[0],
          job_id: 711,
          title: '认证前下线职位',
          status: 3,
          audit_status: 2,
          reject_reason: '',
          updated_at: '2026-05-02T10:10:00'
        }
      ],
      total: 1
    };
    const fetchMock = installCompanyFetchMock({
      overview: pendingCertificationWorkbenchOverview,
      jobPage: actionableJobs
    });

    render(<CompanyDashboard />);

    fireEvent.click(await screen.findByRole('button', { name: /^职位管理$/ }));
    fireEvent.click(screen.getByRole('tab', { name: /已下线/ }));

    expect(await screen.findByText('认证前下线职位')).toBeInTheDocument();
    expect(screen.getByText('企业认证通过后才能提交职位审核。你仍可以保存草稿、编辑职位、下线或删除本企业职位。')).toBeInTheDocument();
    const submitButton = screen.getByRole('button', { name: '重新提交审核' });
    expect(submitButton).toBeDisabled();

    fireEvent.click(submitButton);
    expect(fetchMock.mock.calls.some(([input]) => String(input).includes('/api/company/workbench/jobs/711/submit-review'))).toBe(false);
    expectNoSensitiveCandidateOrCompanyFields();
  });

  it('shows the recycle bin and restores deleted jobs as drafts without unsafe actions', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    const fetchMock = installCompanyFetchMock({ deletedJobPage: deletedWorkbenchJobs });

    render(<CompanyDashboard />);

    fireEvent.click(await screen.findByRole('button', { name: /^职位管理$/ }));
    fireEvent.click(screen.getByRole('tab', { name: /已删除/ }));

    expect(await screen.findByText('已删除职位')).toBeInTheDocument();
    expect(screen.getByText('企业工作台软删除职位。')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: '永久删除（禁止）' }).every((button) => button.hasAttribute('disabled'))).toBe(true);
    expect(screen.queryByRole('button', { name: '修改' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '提交审核' })).not.toBeInTheDocument();

    fireEvent.click(screen.getByLabelText('选择职位 已删除职位'));
    fireEvent.click(screen.getAllByRole('button', { name: '恢复为草稿' })[0]);

    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([input, init]) => {
        const url = String(input);
        const headers = init?.headers as Headers | undefined;
        return url.includes('/api/company/workbench/jobs/801/restore-draft')
          && init?.method === 'POST'
          && headers?.get('Authorization') === 'Bearer company-token'
          && Boolean(headers?.get('X-Trace-Id'))
          && Boolean(headers?.get('X-Idempotency-Key'));
      })).toBe(true);
    });
    expect(screen.getByRole('tab', { name: /审核中/ })).toHaveAttribute('aria-selected', 'true');

    fireEvent.click(screen.getByRole('tab', { name: /已删除/ }));
    await screen.findByText('已删除职位');
    fireEvent.click(screen.getAllByRole('button', { name: '恢复为草稿' })[1]);
    await waitFor(() => {
      const restoreCalls = fetchMock.mock.calls.filter(([input]) => String(input).includes('/api/company/workbench/jobs/801/restore-draft'));
      expect(restoreCalls.length).toBeGreaterThanOrEqual(2);
    });

    expect(bodyText()).not.toContain('支付链接');
    expect(bodyText()).not.toContain('联系解锁入口');
    expect(bodyText()).not.toContain('永久删除成功');
    expectNoSensitiveCandidateOrCompanyFields();
  });

  it('gates company center tabs until basic profile is complete', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    installCompanyFetchMock({ overview: incompleteWorkbenchOverview, completeAfterProfileSave: true });

    render(<CompanyDashboard />);

    expect(await screen.findByText('请先完善企业管理中的基本资料。其它功能会在服务端确认资料完整后解锁。')).toBeInTheDocument();
    const jobMenu = screen.getAllByRole('button', { name: /职位管理/ }).find((button) => button.getAttribute('aria-disabled') === 'true');
    expect(jobMenu).toBeDefined();
    if (!jobMenu) {
      throw new Error('locked job menu not found');
    }
    expect(jobMenu).toHaveAttribute('aria-disabled', 'true');

    fireEvent.click(jobMenu);
    expect(await screen.findByRole('dialog', { name: '系统提示' })).toBeInTheDocument();
    expect(screen.getByText('根据相关法律法规要求，需要您完善企业认证信息。')).toBeInTheDocument();
    expect(screen.getByText(/请先完善：/)).toHaveTextContent('企业名称');
    expect(screen.getByText(/请先完善：/)).toHaveTextContent('联系人');
    fireEvent.click(screen.getByRole('button', { name: '去完善' }));

    expect(await screen.findByText('企业资料')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '保存企业资料' }));

    await waitFor(() => {
      const unlockedJobMenu = screen.getAllByRole('button', { name: /^职位管理$/ })
        .find((button) => button.getAttribute('aria-disabled') === 'false');
      expect(unlockedJobMenu).toBeDefined();
    });
  });

  it('keeps tabs locked when contact required fields are missing but still allows company style', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    installCompanyFetchMock({ overview: contactMissingWorkbenchOverview });

    render(<CompanyDashboard />);

    expect(await screen.findByText('请先完善企业管理中的基本资料。其它功能会在服务端确认资料完整后解锁。')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /^企业风采$/ }));
    expect((await screen.findAllByText(/最多可上传 6 张企业风采图片/)).length).toBeGreaterThanOrEqual(1);

    const jobMenu = screen.getAllByRole('button', { name: /职位管理/ }).find((button) => button.getAttribute('aria-disabled') === 'true');
    expect(jobMenu).toBeDefined();
    if (!jobMenu) {
      throw new Error('locked job menu not found');
    }
    fireEvent.click(jobMenu);

    expect(await screen.findByRole('dialog', { name: '系统提示' })).toBeInTheDocument();
    expect(screen.getByText('请先完善：联系人、联系电话')).toBeInTheDocument();
    expect(await screen.findByText('企业资料')).toBeInTheDocument();
  });

  it('renders a single disabled company style upload entry when feature is closed', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    installCompanyFetchMock({ overview: incompleteWorkbenchOverview });

    render(<CompanyDashboard />);

    expect(await screen.findByText('企业资料')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /^企业风采$/ }));

    expect((await screen.findAllByText(/最多可上传 6 张企业风采图片/)).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('+ 上传风采')).toHaveLength(1);
    expect(screen.getByText('上传暂未开放')).toBeInTheDocument();
    expect(screen.getByText('暂未开放')).toBeInTheDocument();
    expectNoSensitiveCandidateOrCompanyFields();
  });

  it('uploads, previews and deletes private company logo when enabled', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    const fetchMock = installCompanyFetchMock({ overview: logoEnabledWorkbenchOverview });

    render(<CompanyDashboard />);

    expect(await screen.findByText('企业会员首页')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /^基本资料$/ }));

    expect(await screen.findByAltText('企业 Logo')).toHaveAttribute('src', 'blob:http://localhost/style-image');
    expect(screen.getByText('替换 logo')).toBeInTheDocument();
    expect(screen.getByText('生成 LOGO（占位）')).toBeDisabled();
    expect(screen.getByText('扫码上传（占位）')).toBeDisabled();
    expectNoSensitiveCandidateOrCompanyFields();

    const file = new File(['logo'], 'logo.png', { type: 'image/png' });
    const uploadInput = document.querySelector('input[accept="image/jpeg,image/png,image/webp"]') as HTMLInputElement;
    fireEvent.change(uploadInput, { target: { files: [file] } });

    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([input, init]) => {
        const url = String(input);
        const headers = init?.headers as Headers | undefined;
        return url.endsWith('/api/company/workbench/logo')
          && init?.method === 'POST'
          && headers?.get('Authorization') === 'Bearer company-token'
          && Boolean(headers?.get('X-Trace-Id'))
          && Boolean(headers?.get('X-Idempotency-Key'));
      })).toBe(true);
    });

    fireEvent.click(screen.getByRole('button', { name: '删除 Logo' }));
    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([input, init]) => {
        const url = String(input);
        const headers = init?.headers as Headers | undefined;
        return url.endsWith('/api/company/workbench/logo')
          && init?.method === 'DELETE'
          && headers?.get('Authorization') === 'Bearer company-token'
          && Boolean(headers?.get('X-Trace-Id'))
          && Boolean(headers?.get('X-Idempotency-Key'));
      })).toBe(true);
    });
  });

  it('uploads, previews, reorders and deletes private company style images when enabled', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    const fetchMock = installCompanyFetchMock({ overview: styleEnabledWorkbenchOverview });

    render(<CompanyDashboard />);

    expect(await screen.findByText('企业会员首页')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /^企业风采$/ }));

    expect(await screen.findByText('私有上传已开启')).toBeInTheDocument();
    expect(screen.getByText('office.webp')).toBeInTheDocument();
    expect(screen.getByAltText('企业风采 1')).toHaveAttribute('src', 'blob:http://localhost/style-image');
    expect(screen.getAllByText('+ 上传风采')).toHaveLength(1);
    expectNoSensitiveCandidateOrCompanyFields();
    expect(bodyText()).not.toContain('company-style-images/21/private.webp');
    expect(bodyText()).not.toContain('http://minio.local/private.webp');

    const file = new File(['image'], 'new-office.webp', { type: 'image/webp' });
    const uploadInput = document.querySelector('input[type="file"]') as HTMLInputElement;
    fireEvent.change(uploadInput, { target: { files: [file] } });

    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([input, init]) => {
        const url = String(input);
        const headers = init?.headers as Headers | undefined;
        return url.endsWith('/api/company/workbench/style-images')
          && init?.method === 'POST'
          && headers?.get('Authorization') === 'Bearer company-token'
          && Boolean(headers?.get('X-Trace-Id'))
          && Boolean(headers?.get('X-Idempotency-Key'));
      })).toBe(true);
    });

    fireEvent.click(screen.getByRole('button', { name: '删除' }));
    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([input, init]) => {
        const url = String(input);
        const headers = init?.headers as Headers | undefined;
        return url.includes('/api/company/workbench/style-images/901')
          && init?.method === 'DELETE'
          && headers?.get('Authorization') === 'Bearer company-token'
          && Boolean(headers?.get('X-Trace-Id'))
          && Boolean(headers?.get('X-Idempotency-Key'));
      })).toBe(true);
    });
  });

  it('hides the company style upload entry after six private images exist', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    installCompanyFetchMock({
      overview: styleEnabledWorkbenchOverview,
      styleImages: fullCompanyStyleImages
    });

    render(<CompanyDashboard />);

    expect(await screen.findByText('企业会员首页')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /^企业风采$/ }));

    expect(await screen.findByText('office-1.webp')).toBeInTheDocument();
    expect(screen.getAllByAltText(/企业风采/)).toHaveLength(6);
    expect(screen.queryByText('+ 上传风采')).not.toBeInTheDocument();
    expect(screen.getByText(/已上传 6 张企业风采图片/)).toBeInTheDocument();
    expectNoSensitiveCandidateOrCompanyFields();
  });

  it('sends trace, authorization and idempotency headers for workbench writes', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    const fetchMock = installCompanyFetchMock();

    render(<CompanyDashboard />);

    expect(await screen.findByText('企业会员首页')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /^基本资料$/ }));
    fireEvent.click(screen.getByRole('button', { name: '保存企业资料' }));

    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([input, init]) => {
        const url = String(input);
        const headers = init?.headers as Headers | undefined;
        return url.endsWith('/api/company/workbench/profile')
          && init?.method === 'PUT'
          && headers?.get('Authorization') === 'Bearer company-token'
          && Boolean(headers?.get('X-Trace-Id'))
          && Boolean(headers?.get('X-Idempotency-Key'));
      })).toBe(true);
    });

    fireEvent.click(screen.getByRole('button', { name: /^简历管理$/ }));
    fireEvent.click(screen.getByRole('button', { name: '邀约首个投递' }));

    await waitFor(() => {
      expect(fetchMock.mock.calls.some(([input, init]) => {
        const url = String(input);
        const headers = init?.headers as Headers | undefined;
        return url.includes('/api/company/workbench/applications/601/stage')
          && init?.method === 'POST'
          && headers?.get('Authorization') === 'Bearer company-token'
          && Boolean(headers?.get('X-Trace-Id'))
          && Boolean(headers?.get('X-Idempotency-Key'));
      })).toBe(true);
    });
  });

  it('uses dropdowns and a three-level region picker for company basic profile', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    const fetchMock = installCompanyFetchMock();

    render(<CompanyDashboard />);

    expect(await screen.findByText('企业会员首页')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /^基本资料$/ }));

    fireEvent.click(screen.getByRole('button', { name: /民营/ }));
    expect(await screen.findByRole('option', { name: '国企' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: '其它' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('option', { name: '国企' }));
    expect(screen.getByRole('button', { name: /国企/ })).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: /50～200人|50-200/ }));
    expect(await screen.findByRole('option', { name: '10人以下' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: '1000人以上' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('option', { name: '50～200人' }));

    fireEvent.click(screen.getByRole('button', { name: /计算机软件\/硬件|software/ }));
    expect(await screen.findByRole('option', { name: '互联网/电子商务' })).toBeInTheDocument();
    expect(screen.getByRole('option', { name: '低空经济' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('option', { name: '互联网/电子商务' }));

    fireEvent.change(screen.getByLabelText('注册资金'), { target: { value: '1200' } });
    expect(screen.queryByText('注册资金单位')).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /注册资金单位/ }));
    expect(await screen.findByRole('option', { name: '万美元' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('option', { name: '万美元' }));

    fireEvent.click(screen.getByRole('button', { name: /企业福利/ }));
    expect(await screen.findByRole('option', { name: /五险一金/ })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('option', { name: /周末双休/ }));

    fireEvent.click(screen.getByRole('button', { name: /上海/ }));
    expect(await screen.findByRole('dialog', { name: '所在地选择' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: /河北/ }));
    fireEvent.click(screen.getByRole('button', { name: /唐山/ }));
    fireEvent.click(screen.getByRole('button', { name: '路南区' }));
    expect(screen.getByRole('button', { name: /路南区/ })).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText('详细地址 *'), { target: { value: '唐山市路南区新华道 88 号' } });
    fireEvent.change(screen.getByLabelText('企业网址'), { target: { value: 'https://new.example.local' } });
    fireEvent.change(screen.getByLabelText('联系人 *'), { target: { value: '李招聘' } });
    fireEvent.change(screen.getByLabelText('联系电话 *'), { target: { value: '18888889999' } });
    fireEvent.change(screen.getByLabelText('联系微信'), { target: { value: 'new-hr' } });
    fireEvent.change(screen.getByLabelText('联系邮箱'), { target: { value: 'new-hr@example.local' } });
    fireEvent.change(screen.getByLabelText('联系 QQ'), { target: { value: '987654' } });
    fireEvent.click(screen.getByRole('button', { name: '保存企业资料' }));

    await waitFor(() => {
      const payload = latestWorkbenchProfilePayload(fetchMock);
      expect(payload.nature_code).toBe('state_owned');
      expect(payload.scale_code).toBe('50-200');
      expect(payload.industry_code).toBe('internet');
      expect(payload.city_code).toBe('130202');
      expect(payload.address).toBe('唐山市路南区新华道 88 号');
      expect(payload.registered_capital_amount).toBe('1200');
      expect(payload.registered_capital_unit).toBe('usd_10k');
      expect(payload.benefit_codes).toEqual(['five_insurance', 'annual_leave', 'weekend_double']);
      expect(payload.website_url).toBe('https://new.example.local');
      expect(payload.contact_name).toBe('李招聘');
      expect(payload.contact_mobile).toBe('18888889999');
      expect(payload.contact_mobile_hidden).toBe(true);
      expect(payload.contact_wechat).toBe('new-hr');
      expect(payload.contact_email).toBe('new-hr@example.local');
      expect(payload.contact_qq).toBe('987654');
    });
    expectNoSensitiveCandidateOrCompanyFields();
  });

  it('only shows export download action after approval and generation are complete', () => {
    const onIssue = vi.fn().mockResolvedValue('http://minio.local/presigned.csv');

    const cases = [
      { export_id: 1, approve_status: 0, generate_status: 0, download_count: 0 },
      { export_id: 2, approve_status: 2, generate_status: 0, download_count: 0 },
      { export_id: 3, approve_status: 1, generate_status: 1, download_count: 0 },
      { export_id: 4, approve_status: 1, generate_status: 2, download_count: 1 }
    ];

    for (const state of cases) {
      const { unmount } = render(<ExportDownloadAction exportApply={state} onIssueDownloadUrl={onIssue} />);
      expect(screen.queryByRole('button', { name: '获取下载链接' })).not.toBeInTheDocument();
      unmount();
    }

    render(
      <ExportDownloadAction
        exportApply={{ export_id: 5, approve_status: 1, generate_status: 2, download_count: 0 }}
        onIssueDownloadUrl={onIssue}
      />
    );

    expect(screen.getByRole('button', { name: '获取下载链接' })).toBeInTheDocument();
  });
});
