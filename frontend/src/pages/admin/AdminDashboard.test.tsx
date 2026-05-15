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

function installAdminFetchMock(options: { operatorOpsEnabled?: boolean } = {}) {
  const operatorOpsEnabled = Boolean(options.operatorOpsEnabled);
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

    if (url.includes('/api/admin/ops/overview') && method === 'GET') {
      if (!operatorOpsEnabled) {
        return new Response(
          JSON.stringify({
            code: 'FEATURE_DISABLED_403',
            message: 'operator portal ops disabled',
            trace_id: 'trace-admin-ops-disabled',
            data: null
          }),
          { status: 403, headers: { 'Content-Type': 'application/json' } }
        );
      }
      return apiOk({
        features: { operator_portal_ops_enabled: true },
        pending_company_count: 1,
        pending_job_count: 1,
        pending_export_count: 1,
        published_content_count: 2,
        published_event_count: 3,
        active_recommendation_count: 1,
        pending_risk_count: 1,
        recent_audit_count: 5
      }, 'trace-admin-ops');
    }

    if (url.includes('/api/admin/recommendations') && method === 'GET') {
      return apiOk({
        recommendation_list: operatorOpsEnabled ? [
          {
            recommendation_id: 88,
            slot_code: 'home_hot_jobs',
            target_type: 'job',
            target_id: 20,
            title_override: 'Prompt29 推荐位',
            summary_override: '公开推荐摘要',
            display_order: 1,
            status: 1,
            target_valid: true,
            updated_at: '2026-05-01T10:00:00'
          }
        ] : [],
        total: operatorOpsEnabled ? 1 : 0
      }, 'trace-admin-recommendations');
    }

    if (url.includes('/api/admin/home-slots/188/image/content') && method === 'GET') {
      return new Response(new Blob(['home-slot-image'], { type: 'image/webp' }), {
        status: 200,
        headers: { 'Content-Type': 'image/webp' }
      });
    }

    if (url.includes('/api/admin/home-slots') && method === 'GET') {
      return apiOk({
        slot_list: operatorOpsEnabled ? [
          {
            slot_id: 188,
            slot_code: 'home_hero_banner',
            title: '首页首屏运营位',
            subtitle: '不做广告售卖',
            image_url: '/demo/home-ad-full.svg',
            image_alt: '首页运营位',
            has_image: true,
            image_file_name: 'hero.webp',
            image_content_type: 'image/webp',
            image_size_bytes: 4096,
            image_uploaded_at: '2026-05-01T10:00:00',
            image_content_url: '/api/admin/home-slots/188/image/content',
            link_type: 'internal',
            link_url: '/jobs',
            display_order: 1,
            status: 1,
            updated_at: '2026-05-01T10:00:00'
          }
        ] : [],
        total: operatorOpsEnabled ? 1 : 0
      }, 'trace-admin-home-slots');
    }

    if (url.includes('/api/admin/risk-reviews') && method === 'GET') {
      return apiOk({
        risk_review_list: operatorOpsEnabled ? [
          {
            risk_id: 99,
            risk_type: 'content_risk',
            target_type: 'content',
            target_id: 77,
            severity: 'medium',
            status: 0,
            title: 'Prompt29 风险任务',
            summary: '仅公开摘要',
            updated_at: '2026-05-01T10:00:00'
          }
        ] : [],
        total: operatorOpsEnabled ? 1 : 0
      }, 'trace-admin-risks');
    }

    if (url.includes('/api/admin/audit-traces/')) {
      return apiOk({
        trace_id: 'trace-demo',
        audit_log_list: [{
          id: 1,
          biz_type: 'export_apply',
          biz_id: 30,
          action_type: 'export_approve',
          operator_role: 'operator',
          before_json: '{"mobile":"13900000000"}',
          after_json: '{"attachment_object_key":"private/export.csv"}',
          trace_id: 'trace-demo'
        }],
        access_log_list: [{
          id: 2,
          biz_type: 'export_application',
          biz_id: 100,
          field_name: 'mobile',
          access_type: 'MASK',
          operator_role: 'operator',
          trace_id: 'trace-demo'
        }],
        open_api_log_list: [{
          id: 3,
          source_system: 'stub_partner',
          client_code: 'localtalent_stub',
          api_code: 'open.jobs.sync',
          biz_type: 'job',
          biz_id: 20,
          http_status: 200,
          success_flag: true,
          trace_id: 'trace-demo',
          request_uri: '/api/open/v1/jobs/sync?access_token=secret-token'
        }]
      });
    }

    if (url.includes('/api/admin/recommendations') && method === 'POST') {
      return apiOk({
        recommendation_id: 89,
        slot_code: 'home_hot_jobs',
        target_type: 'job',
        target_id: 1,
        title_override: '首页推荐位',
        summary_override: '运营推荐位公开摘要',
        display_order: 1,
        status: 1,
        target_valid: true
      }, 'trace-admin-recommendation-write');
    }

    if (url.includes('/api/admin/home-slots/188/image') && method === 'POST') {
      return apiOk({
        slot_id: 188,
        slot_code: 'home_hero_banner',
        title: '首页首屏运营位',
        subtitle: '不做广告售卖',
        image_url: '/demo/home-ad-full.svg',
        image_alt: '首页运营位',
        has_image: true,
        image_file_name: 'new-hero.webp',
        image_content_type: 'image/webp',
        image_size_bytes: 2048,
        image_uploaded_at: '2026-05-01T11:00:00',
        image_content_url: '/api/admin/home-slots/188/image/content',
        link_type: 'internal',
        link_url: '/jobs',
        display_order: 1,
        status: 1
      }, 'trace-admin-home-slot-image-upload');
    }

    if (url.includes('/api/admin/home-slots/188/image') && method === 'DELETE') {
      return apiOk({
        slot_id: 188,
        slot_code: 'home_hero_banner',
        title: '首页首屏运营位',
        subtitle: '不做广告售卖',
        image_url: '/demo/home-ad-full.svg',
        image_alt: '首页运营位',
        has_image: false,
        image_file_name: '',
        image_content_type: '',
        image_size_bytes: 0,
        image_uploaded_at: '',
        image_content_url: '',
        link_type: 'internal',
        link_url: '/jobs',
        display_order: 1,
        status: 1
      }, 'trace-admin-home-slot-image-delete');
    }

    if (url.includes('/api/admin/home-slots') && method === 'POST') {
      return apiOk({
        slot_id: 189,
        slot_code: 'home_hero_banner',
        title: '首页首屏运营位',
        subtitle: '后台运营可配置；不做广告售卖、支付或外部投放。',
        image_url: '/demo/home-ad-full.svg',
        image_alt: 'LocalTalent 首页运营位',
        has_image: false,
        image_file_name: '',
        image_content_type: '',
        image_size_bytes: 0,
        image_uploaded_at: '',
        image_content_url: '',
        link_type: 'internal',
        link_url: '/jobs',
        display_order: 1,
        status: url.includes('/offline') ? 0 : 1
      }, 'trace-admin-home-slot-write');
    }

    if (url.includes('/api/admin/risk-reviews') && method === 'POST') {
      return apiOk({
        risk_id: 99,
        status: 2,
        decision: '已核查，按低风险公开对象处理。'
      }, 'trace-admin-risk-write');
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
    Object.defineProperty(URL, 'createObjectURL', { value: vi.fn(() => 'blob:http://localhost/home-slot-image'), configurable: true });
    Object.defineProperty(URL, 'revokeObjectURL', { value: vi.fn(), configurable: true });
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

  it('renders phase3 operations sections and sends idempotent writes for operator', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'operator-token');
    window.localStorage.setItem(ADMIN_ROLE_HINT_STORAGE_KEY, 'operator');
    const fetchMock = installAdminFetchMock({ operatorOpsEnabled: true });

    render(<AdminDashboard />);

    expect(await screen.findByText(/三期运营化已开启/)).toBeInTheDocument();
    expect(screen.getByText('运营首页')).toBeInTheDocument();
    expect(screen.getByLabelText('推荐位配置')).toBeInTheDocument();
    expect(screen.getByLabelText('首页运营位配置')).toBeInTheDocument();
    expect(screen.getByLabelText('风险审核')).toBeInTheDocument();
    expect(screen.getByText('Prompt29 风险任务')).toBeInTheDocument();
    expect(screen.getByDisplayValue('首页首屏运营位')).toBeInTheDocument();
    expect(screen.getByText(/首页运营位图片已上传/)).toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '保存推荐位' }));

    await waitFor(() => {
      const postCall = fetchMock.mock.calls.find(([input, init]) => (
        String(input).includes('/api/admin/recommendations') && init?.method === 'POST'
      ));
      expect(postCall).toBeTruthy();
      expect((postCall?.[1]?.headers as Headers).get('X-Idempotency-Key')).toContain('admin-recommendation');
      expect(postCall?.[1]?.body).toContain('"slot_code":"home_hot_jobs"');
    });

    fireEvent.click(screen.getByRole('button', { name: '保存首页运营位' }));

    await waitFor(() => {
      const slotCall = fetchMock.mock.calls.find(([input, init]) => (
        String(input).includes('/api/admin/home-slots') && init?.method === 'POST'
      ));
      expect(slotCall).toBeTruthy();
      expect((slotCall?.[1]?.headers as Headers).get('X-Idempotency-Key')).toContain('admin-home-slot');
      expect(slotCall?.[1]?.body).toContain('"slot_code":"home_hero_banner"');
    });

    const imageInput = screen.getByLabelText('上传 home_hero_banner 图片') as HTMLInputElement;
    fireEvent.change(imageInput, {
      target: {
        files: [new File(['hero'], 'new-hero.webp', { type: 'image/webp' })]
      }
    });

    await waitFor(() => {
      const imageCall = fetchMock.mock.calls.find(([input, init]) => (
        String(input).includes('/api/admin/home-slots/188/image') && init?.method === 'POST'
      ));
      expect(imageCall).toBeTruthy();
      expect((imageCall?.[1]?.headers as Headers).get('X-Idempotency-Key')).toContain('admin-home-slot-image');
      expect(imageCall?.[1]?.body).toBeInstanceOf(FormData);
    });

    fireEvent.click(screen.getByRole('button', { name: '删除首页运营位图片' }));

    await waitFor(() => {
      const deleteCall = fetchMock.mock.calls.find(([input, init]) => (
        String(input).includes('/api/admin/home-slots/188/image') && init?.method === 'DELETE'
      ));
      expect(deleteCall).toBeTruthy();
      expect((deleteCall?.[1]?.headers as Headers).get('X-Idempotency-Key')).toContain('admin-home-slot-image-delete');
    });

    fireEvent.click(screen.getByRole('button', { name: '处理为低风险' }));

    await waitFor(() => {
      const riskCall = fetchMock.mock.calls.find(([input, init]) => (
        String(input).includes('/api/admin/risk-reviews/99/handle') && init?.method === 'POST'
      ));
      expect(riskCall).toBeTruthy();
      expect((riskCall?.[1]?.headers as Headers).get('X-Idempotency-Key')).toContain('admin-risk-review');
    });
  });

  it('renders sanitized audit trace details without raw sensitive payloads', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'operator-token');
    window.localStorage.setItem(ADMIN_ROLE_HINT_STORAGE_KEY, 'operator');
    installAdminFetchMock({ operatorOpsEnabled: true });

    render(<AdminDashboard />);

    expect(await screen.findByText(/三期运营化已开启/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '查询审计链路' }));

    expect(await screen.findByText('audit_log 摘要')).toBeInTheDocument();
    expect(screen.getByText('field_access_log 摘要')).toBeInTheDocument();
    expect(screen.getByText('open_api_log 摘要')).toBeInTheDocument();
    expect(screen.getByText(/export_apply\/30 · export_approve/)).toBeInTheDocument();
    expect(screen.getByText(/export_application\/100 · mobile · MASK/)).toBeInTheDocument();
    expect(screen.getByText(/stub_partner\/localtalent_stub · open.jobs.sync/)).toBeInTheDocument();

    const body = document.body.textContent ?? '';
    expect(body).not.toContain('13900000000');
    expect(body).not.toContain('private/export.csv');
    expect(body).not.toContain('secret-token');
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
