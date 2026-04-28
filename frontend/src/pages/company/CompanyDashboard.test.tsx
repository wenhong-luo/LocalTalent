import { fireEvent, render, screen } from '@testing-library/react';
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

function installCompanyFetchMock() {
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

    if (url.includes('/api/company/jobs')) {
      return apiOk({
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
      });
    }

    if (url.includes('/api/company/applications')) {
      return apiOk({
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
      });
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

describe('CompanyDashboard', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
  });

  it('submits company certification and renders returned auth status', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    installCompanyFetchMock();

    render(<CompanyDashboard />);

    expect((await screen.findAllByText('后端工程师')).length).toBeGreaterThanOrEqual(1);
    fireEvent.click(screen.getByRole('button', { name: '提交认证资料' }));

    expect(await screen.findByText('已通过')).toBeInTheDocument();
    expect(screen.getByText(/trace_id：trace-company-apply/)).toBeInTheDocument();
  });

  it('renders job and application status without raw candidate fields', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    installCompanyFetchMock();

    render(<CompanyDashboard />);

    expect((await screen.findAllByText('后端工程师')).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('在线').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('邀约面试')).toBeInTheDocument();

    const body = document.body.textContent ?? '';
    expect(body).not.toContain('13900001234');
    expect(body).not.toContain('candidate@example.com');
    expect(body).not.toContain('raw resume body');
    expect(body).not.toContain('resume/private.pdf');
    expect(body).not.toContain('consent evidence');
    expect(body).not.toContain('bcrypt-hash');
    expect(body).not.toContain('mobile');
    expect(body).not.toContain('email');
    expect(body).not.toContain('resume_body');
    expect(body).not.toContain('attachment_object_key');
    expect(body).not.toContain('evidence');
    expect(body).not.toContain('password_hash');
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
