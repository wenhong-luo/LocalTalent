import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { ACCESS_TOKEN_STORAGE_KEY, ADMIN_ROLE_HINT_STORAGE_KEY } from '@/pages/backoffice/session';
import { AdminDashboard } from './AdminDashboard';

function apiOk(data: unknown, traceId = 'trace-admin'): Response {
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

function installAdminFetchMock() {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const url = String(input);
    const method = init?.method ?? 'GET';

    if (url.includes('/api/auth/me')) {
      return apiOk({
        identity_type: 'operator',
        user_id: 1,
        display_name: '运营账号'
      });
    }

    if (url.includes('/api/admin/companies/review') && method === 'GET') {
      return apiOk({
        company_list: [
          {
            company_id: 10,
            company_name: '星河科技',
            license_no: '91310000MA1',
            city_code: '310000',
            auth_status: 1,
            submitted_at: '2026-04-28T10:00:00'
          }
        ],
        total: 1
      });
    }

    if (url.includes('/api/admin/jobs/review') && method === 'GET') {
      return apiOk({
        job_list: [
          {
            job_id: 20,
            title: '前端工程师',
            company_id: 10,
            status: 1,
            audit_status: 1,
            updated_at: '2026-04-28T11:00:00'
          }
        ],
        total: 1
      });
    }

    if (url.includes('/api/admin/exports/review') && method === 'GET') {
      return apiOk({
        export_list: [
          {
            export_id: 30,
            biz_type: 'application_candidate_detail',
            approve_status: 0,
            generate_status: 0,
            reason: '招聘复核',
            reject_reason: '',
            download_count: 0,
            created_at: '2026-04-28T12:00:00'
          }
        ],
        total: 1
      });
    }

    if (url.includes('/api/admin/audit-traces/')) {
      return apiOk({
        trace_id: 'trace-demo',
        audit_log_list: [{ id: 1 }],
        access_log_list: [{ id: 2 }],
        open_api_log_list: [{ id: 3 }]
      });
    }

    if (method === 'POST') {
      return apiOk({ ok: true }, 'trace-admin-review');
    }

    return apiOk({});
  });
}

describe('AdminDashboard', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
  });

  it('renders company, job, export review queues and audit trace entry', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'operator-token');
    window.localStorage.setItem(ADMIN_ROLE_HINT_STORAGE_KEY, 'operator');
    installAdminFetchMock();

    render(<AdminDashboard />);

    expect(await screen.findByText('星河科技')).toBeInTheDocument();
    expect(screen.getByText('前端工程师')).toBeInTheDocument();
    expect(screen.getByText('application_candidate_detail')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '查询审计链路' })).toBeInTheDocument();
  });

  it('shows write actions for operator hint and submits review body', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'operator-token');
    window.localStorage.setItem(ADMIN_ROLE_HINT_STORAGE_KEY, 'operator');
    const fetchMock = installAdminFetchMock();

    render(<AdminDashboard />);

    expect(await screen.findByText('星河科技')).toBeInTheDocument();
    fireEvent.click(screen.getAllByRole('button', { name: '审批' })[0]);
    fireEvent.click(screen.getByRole('button', { name: '通过' }));

    await waitFor(() => {
      const postCall = fetchMock.mock.calls.find(([input, init]) => (
        String(input).includes('/api/admin/companies/review') && init?.method === 'POST'
      ));
      expect(postCall).toBeTruthy();
      expect(postCall?.[1]?.body).toBe(JSON.stringify({
        company_id: 10,
        audit_status: 2,
        memo: ''
      }));
    });
  });

  it('hides write actions for auditor hint', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'auditor-token');
    window.localStorage.setItem(ADMIN_ROLE_HINT_STORAGE_KEY, 'auditor');
    installAdminFetchMock();

    render(<AdminDashboard />);

    expect(await screen.findByText('星河科技')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '审批' })).not.toBeInTheDocument();
    expect(screen.getAllByText('只读').length).toBeGreaterThanOrEqual(3);
  });

  it('does not submit reject action without memo', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'operator-token');
    window.localStorage.setItem(ADMIN_ROLE_HINT_STORAGE_KEY, 'operator');
    const fetchMock = installAdminFetchMock();

    render(<AdminDashboard />);

    expect(await screen.findByText('星河科技')).toBeInTheDocument();
    fireEvent.click(screen.getAllByRole('button', { name: '审批' })[0]);
    fireEvent.click(screen.getByRole('button', { name: '驳回' }));

    expect(await screen.findByText('驳回原因必填')).toBeInTheDocument();
    expect(fetchMock.mock.calls.some(([input, init]) => (
      String(input).includes('/api/admin/companies/review') && init?.method === 'POST'
    ))).toBe(false);
  });
});
