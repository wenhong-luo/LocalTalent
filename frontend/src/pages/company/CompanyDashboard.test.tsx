import { fireEvent, render, screen, waitFor } from '@testing-library/react';
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

const workbenchJobs = {
  job_list: [
    {
      job_id: 501,
      title: '三期招聘顾问',
      status: 1,
      audit_status: 1,
      reject_reason: '',
      updated_at: '2026-05-01T10:10:00'
    }
  ],
  total: 1
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
  logo = companyLogo
}: {
  workbenchEnabled?: boolean;
  overview?: typeof workbenchOverview;
  completeAfterProfileSave?: boolean;
  styleImages?: { image_list: Array<Record<string, unknown>>; total: number };
  logo?: Record<string, unknown>;
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

    if (url.includes('/api/company/workbench/jobs') && method === 'GET') {
      return apiOk(workbenchJobs);
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
