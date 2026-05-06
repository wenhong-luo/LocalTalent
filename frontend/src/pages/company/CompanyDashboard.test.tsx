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
    scale_code: '50-150',
    city_code: '310000',
    address: '上海市示例园区',
    company_profile: '企业公开简介摘要',
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
  features: { company_workbench_enabled: true }
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

function installCompanyFetchMock({ workbenchEnabled = true }: { workbenchEnabled?: boolean } = {}) {
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
      return workbenchEnabled ? apiOk(workbenchOverview) : apiError('FEATURE_DISABLED_403', 'company workbench disabled');
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
      return apiOk(workbenchOverview.profile, 'trace-profile');
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

function expectNoSensitiveCandidateOrCompanyFields() {
  const body = bodyText();
  expect(body).not.toContain('13900001234');
  expect(body).not.toContain('candidate@example.com');
  expect(body).not.toContain('raw resume body');
  expect(body).not.toContain('resume/private.pdf');
  expect(body).not.toContain('consent evidence');
  expect(body).not.toContain('bcrypt-hash');
  expect(body).not.toContain('license/private.pdf');
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
}

describe('CompanyDashboard', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
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

    expect(await screen.findByText('职位工作台')).toBeInTheDocument();
    expect(screen.getByText('企业资料与认证')).toBeInTheDocument();
    expect(screen.getByText('投递池')).toBeInTheDocument();
    expect(screen.getAllByText('面试邀约').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('张* / 310000')).toBeInTheDocument();
    expect(screen.getByText('Java, Spring')).toBeInTheDocument();
    expectNoSensitiveCandidateOrCompanyFields();
  });

  it('sends trace, authorization and idempotency headers for workbench writes', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    const fetchMock = installCompanyFetchMock();

    render(<CompanyDashboard />);

    expect(await screen.findByText('职位工作台')).toBeInTheDocument();
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
