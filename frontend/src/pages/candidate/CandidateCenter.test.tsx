import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { CandidateCenter } from './CandidateCenter';
import {
  CANDIDATE_TOKEN_STORAGE_KEY,
  type CandidateCenterOverview
} from './candidateCenterApi';

const consentedOverview: CandidateCenterOverview = {
  resume: {
    completion_percent: 86,
    updated_at: '2026-04-28T10:00:00+08:00',
    skills_summary: 'Java / Spring Boot / MySQL'
  },
  applications: {
    total: 3,
    latest_status: '邀约面试',
    latest_job_title: '后端工程师'
  },
  signin: {
    latest_status: '已签到',
    latest_time: '2026-04-28T11:00:00+08:00'
  },
  consent: {
    consent_id: 77,
    publish_status: 'consented',
    publishable_flag: 1,
    status_label: '服务端确认已同意',
    reason: '',
    updated_at: '2026-04-28T12:00:00+08:00'
  },
  stats: {
    favorite_count: 0,
    subscription_count: 0,
    unread_notification_count: 0
  },
  features: {
    candidate_closure_enabled: false,
    resume_attachment_upload_enabled: false,
    resume_ai_assist_enabled: false
  }
};

function overview(overrides: Partial<CandidateCenterOverview['consent']> = {}): CandidateCenterOverview {
  return {
    ...consentedOverview,
    consent: {
      ...consentedOverview.consent,
      ...overrides
    }
  };
}

function apiOk(data: unknown, traceId = 'trace-candidate-center'): Response {
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

function apiError(status: number, code: string, message: string): Response {
  return new Response(
    JSON.stringify({
      code,
      message,
      trace_id: 'trace-denied',
      data: null
    }),
    { status, headers: { 'Content-Type': 'application/json' } }
  );
}

function setToken() {
  window.localStorage.setItem(CANDIDATE_TOKEN_STORAGE_KEY, 'candidate-token');
}

function closureOverview(overrides: Partial<CandidateCenterOverview> = {}): CandidateCenterOverview {
  return {
    ...consentedOverview,
    ...overrides,
    stats: {
      favorite_count: 1,
      subscription_count: 1,
      unread_notification_count: 1,
      ...overrides.stats
    },
    features: {
      candidate_closure_enabled: true,
      resume_attachment_upload_enabled: true,
      resume_ai_assist_enabled: true,
      ...overrides.features
    }
  };
}

const aiTaskPayload = {
  task_id: 90,
  task_status: 'generated',
  suggestion_count: 2,
  applied_count: 0,
  dismissed_count: 0,
  generated_at: '2026-05-08T11:00:00',
  items: [{
    suggestion_id: 901,
    suggestion_type: 'self_description',
    target_field: 'self_description',
    title: '补充自我描述',
    reason_summary: '自我描述较短，建议补充交付亮点。',
    before_preview: '暂无自我描述',
    suggested_value: '关注稳定交付，熟悉本地人才服务合规边界。',
    can_apply: true,
    apply_status: 'pending'
  }, {
    suggestion_id: 902,
    suggestion_type: 'guidance',
    target_field: 'work_experience',
    title: '完善工作职责',
    reason_summary: '工作职责较短，可以补充项目规模和结果。',
    before_preview: '暂无工作职责',
    suggested_value: '建议补充项目职责、技术栈和结果指标。',
    can_apply: false,
    apply_status: 'pending'
  }]
};

function closurePayload(path: string): unknown {
  if (path.includes('/resume/ai-suggestions')) {
    return aiTaskPayload;
  }

  if (path.includes('/resume/attachment')) {
    return {
      has_attachment: true,
      attachment_status: 'uploaded',
      file_name: 'resume.pdf',
      content_type: 'application/pdf',
      size_bytes: 18,
      uploaded_at: '2026-05-08T10:10:00'
    };
  }

  if (path.includes('/resume/preview')) {
    return {
      resume_id: 5,
      resume_status: 'complete',
      completion_percent: 100,
      updated_at: '2026-05-01T09:00:00+08:00',
      resume_name: '三期试运营简历',
      base_profile: {
        display_name: '林同学',
        city_code: '310000',
        category_code: 'network_communication_electronics',
        experience_years: 5,
        summary: '服务端返回的预览摘要',
        expected_positions: ['Java工程师'],
        expected_cities: ['浦东新区']
      },
      education: ['本科'],
      experience: ['后端服务建设'],
      skills: ['Java', 'Spring'],
      has_attachment: true
    };
  }

  if (path.includes('/resume')) {
    return {
      resume_id: 5,
      resume_status: 'complete',
      completion_percent: 100,
      updated_at: '2026-05-01T09:00:00+08:00',
      resume_name: '三期试运营简历',
      base_profile: {
        display_name: '林同学',
        city_code: '310000',
        category_code: 'network_communication_electronics',
        experience_years: 5,
        summary: '只在本人私有域展示',
        expected_positions: ['Java工程师'],
        expected_cities: ['浦东新区']
      },
      education: ['本科'],
      experience: ['后端服务建设'],
      skills: ['Java', 'Spring'],
      has_attachment: true
    };
  }

  if (path.includes('/applications')) {
    return {
      application_list: [{
        application_id: 9,
        job_id: 18,
        job_title: 'Java 工程师',
        company_name: '认证科技公司',
        application_status: 2,
        status_label: '邀约面试',
        apply_time: '2026-05-01T10:00:00'
      }],
      total: 1
    };
  }

  if (path.includes('/favorites')) {
    return {
      favorite_list: [{
        favorite_id: 3,
        job_id: 18,
        job_title: 'Java 工程师',
        company_name: '认证科技公司',
        city_code: '310000',
        category_code: 'software',
        favorite_status: 'active',
        created_at: '2026-05-01T10:00:00'
      }],
      total: 1
    };
  }

  if (path.includes('/subscriptions')) {
    return {
      subscription_list: [{
        subscription_id: 4,
        subscription_name: '后端岗位订阅',
        keyword: 'Java',
        city_code: '310000',
        category_code: 'software',
        salary_min: 15000,
        salary_max: 30000,
        subscription_status: 'active',
        updated_at: '2026-05-01T10:00:00'
      }],
      total: 1
    };
  }

  if (path.includes('/notifications')) {
    return {
      notification_list: [{
        notification_id: 6,
        notification_type: 'system',
        title: '订阅已创建',
        content_summary: '仅站内通知',
        read_status: 'unread',
        created_at: '2026-05-01T10:00:00'
      }],
      total: 1
    };
  }

  return {};
}

function mockCandidateClosureFetch(extraResume: Record<string, unknown> = {}) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const path = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;

    if (path.includes('/api/candidate/center/overview')) {
      return apiOk(closureOverview());
    }

    if (init?.method === 'DELETE' && path.includes('/resume/attachment')) {
      return apiOk({
        has_attachment: false,
        attachment_status: 'empty',
        file_name: '',
        content_type: '',
        size_bytes: null,
        uploaded_at: ''
      });
    }

    if (init?.method === 'POST' && path.includes('/resume/ai-suggestions/901/apply')) {
      return apiOk({
        ...aiTaskPayload,
        applied_count: 1,
        items: aiTaskPayload.items.map((item) => item.suggestion_id === 901
          ? { ...item, apply_status: 'applied' }
          : item)
      });
    }

    if (init?.method === 'POST' && path.includes('/resume/ai-suggestions/902/dismiss')) {
      return apiOk({
        ...aiTaskPayload,
        dismissed_count: 1,
        items: aiTaskPayload.items.map((item) => item.suggestion_id === 902
          ? { ...item, apply_status: 'dismissed' }
          : item)
      });
    }

    if (init?.method === 'POST' && path.includes('/resume/ai-suggestions')) {
      return apiOk(aiTaskPayload);
    }

    if (init?.method === 'PUT' || init?.method === 'POST') {
      if (path.includes('/resume/attachment')) {
        return apiOk({
          has_attachment: true,
          attachment_status: 'uploaded',
          file_name: 'resume-v2.pdf',
          content_type: 'application/pdf',
          size_bytes: 24,
          uploaded_at: '2026-05-08T10:12:00'
        });
      }
      return apiOk({});
    }

    const payload = closurePayload(path);
    if (path.includes('/resume') && !path.includes('/preview')) {
      return apiOk({
        ...(payload as Record<string, unknown>),
        ...extraResume
      });
    }
    return apiOk(payload);
  });
}

describe('CandidateCenter', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
  });

  it('renders consented state from overview response', async () => {
    setToken();
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(apiOk(consentedOverview));

    render(<CandidateCenter />);

    expect(await screen.findByText('已同意，发布快照可见')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '撤回同意' })).toBeInTheDocument();
    expect(screen.getByText('86% 完成')).toBeInTheDocument();
    expect(screen.getByText(/trace_id：trace-candidate-center/)).toBeInTheDocument();
    expect(screen.getByText('三期求职者闭环未开启')).toBeInTheDocument();
  });

  it('renders revoked state and does not show active consent copy', async () => {
    setToken();
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(apiOk(overview({
      consent_id: 88,
      publish_status: 'revoked',
      publishable_flag: 0,
      status_label: '服务端确认已撤回'
    })));

    render(<CandidateCenter />);

    expect(await screen.findByText('已撤回并下线')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '重新同意发布' })).toBeInTheDocument();
    expect(screen.queryByText('已同意，发布快照可见')).not.toBeInTheDocument();
  });

  it('renders not publishable state with disabled consent action', async () => {
    setToken();
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(apiOk(overview({
      publish_status: 'not_publishable',
      publishable_flag: 0,
      status_label: '资料待补充',
      reason: '请先完成实名核验'
    })));

    render(<CandidateCenter />);

    expect(await screen.findByText('暂不可发布')).toBeInTheDocument();
    expect(screen.getByText('请先完成实名核验')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '暂不可同意发布' })).toBeDisabled();
  });

  it('renders unauthorized state when token is missing', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch');

    render(<CandidateCenter />);

    expect(await screen.findByText('无权限访问求职者中心')).toBeInTheDocument();
    expect(screen.getByText('请先登录求职者账号后再访问求职者中心。')).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('maps 401 or 403 response to unauthorized state', async () => {
    setToken();
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(apiError(403, 'AUTHZ_403', '无权限'));

    render(<CandidateCenter />);

    expect(await screen.findByText('无权限访问求职者中心')).toBeInTheDocument();
    expect(screen.getAllByText('无权限').length).toBeGreaterThan(0);
  });

  it('renders error state and retries successfully', async () => {
    setToken();
    vi.spyOn(globalThis, 'fetch')
      .mockRejectedValueOnce(new Error('connection refused'))
      .mockResolvedValueOnce(apiOk(consentedOverview, 'trace-after-retry'));

    render(<CandidateCenter />);

    expect(await screen.findByText('求职者中心暂时不可用')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: '重新读取' }));

    expect(await screen.findByText('已同意，发布快照可见')).toBeInTheDocument();
    expect(screen.getByText(/trace_id：trace-after-retry/)).toBeInTheDocument();
  });

  it('shows retrying state while revoke request is in flight', async () => {
    setToken();
    const pendingRevoke = new Promise<Response>(() => undefined);
    vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(apiOk(consentedOverview))
      .mockReturnValueOnce(pendingRevoke);

    render(<CandidateCenter />);

    fireEvent.click(await screen.findByRole('button', { name: '撤回同意' }));

    expect(await screen.findByText('正在重新读取求职者中心')).toBeInTheDocument();
  });

  it('does not render raw candidate fields from overview response', async () => {
    setToken();
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(apiOk({
      ...consentedOverview,
      mobile: '13900001234',
      email: 'candidate@example.com',
      password_hash: 'bcrypt-hash',
      resume: {
        ...consentedOverview.resume,
        resume_body: 'raw resume body',
        attachment_object_key: 'resume/private.pdf'
      },
      consent: {
        ...consentedOverview.consent,
        evidence: 'consent evidence'
      }
    }));

    render(<CandidateCenter />);

    expect(await screen.findByText('已同意，发布快照可见')).toBeInTheDocument();
    const body = document.body.textContent ?? '';
    expect(body).not.toContain('13900001234');
    expect(body).not.toContain('candidate@example.com');
    expect(body).not.toContain('bcrypt-hash');
    expect(body).not.toContain('raw resume body');
    expect(body).not.toContain('resume/private.pdf');
    expect(body).not.toContain('consent evidence');
    expect(body).not.toContain('mobile');
    expect(body).not.toContain('email');
    expect(body).not.toContain('resume_body');
    expect(body).not.toContain('attachment_object_key');
    expect(body).not.toContain('evidence');
    expect(body).not.toContain('password_hash');
  });

  it('renders phase 3 candidate closure sections when feature is enabled', async () => {
    setToken();
    mockCandidateClosureFetch();

    render(<CandidateCenter />);

    expect(await screen.findByText('简历编辑')).toBeInTheDocument();
    expect(screen.getByText('简历预览')).toBeInTheDocument();
    expect(screen.getByText('投递记录')).toBeInTheDocument();
    expect(screen.getAllByText('职位收藏').length).toBeGreaterThan(0);
    expect(screen.getAllByText('搜索订阅').length).toBeGreaterThan(0);
    expect(screen.getAllByText('站内通知').length).toBeGreaterThan(0);
    expect(screen.getByText('智能优化建议（安全规则版）')).toBeInTheDocument();
    expect(screen.getByText('补充自我描述')).toBeInTheDocument();
    expect(screen.getByText(/resume.pdf/)).toBeInTheDocument();
    expect(screen.getByLabelText('求职者中心上传附件简历')).toBeInTheDocument();
    expect(screen.getByText('Java 工程师 · 认证科技公司')).toBeInTheDocument();
    expect(screen.getByText(/后端岗位订阅/)).toBeInTheDocument();
  });

  it('renders high fidelity member center layout without commercial capabilities', async () => {
    setToken();
    mockCandidateClosureFetch();

    render(<CandidateCenter />);

    expect(await screen.findByRole('region', { name: '求职者个人信息横幅' })).toBeInTheDocument();
    expect(screen.getByTestId('candidate-center-refined')).toHaveAttribute('data-ui-stage', 'ui-6-refined');
    expect(screen.getByTestId('candidate-profile-refined')).toHaveAttribute('data-ui-stage', 'ui-6-refined');
    expect(await screen.findByTestId('candidate-closure-refined')).toHaveAttribute('data-ui-stage', 'ui-6-refined');
    expect(screen.getByRole('complementary', { name: '求职者中心菜单' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: '我的简历' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: '优选服务' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: '职位推荐' })).toBeInTheDocument();
    expect(screen.getByText('会员首页')).toBeInTheDocument();
    expect(screen.getByText('我的职聊')).toBeInTheDocument();
    expect(screen.getByText('求职管理')).toBeInTheDocument();
    expect(screen.getByText('简历置顶')).toBeInTheDocument();
    expect(screen.getByText('醒目标签')).toBeInTheDocument();
    expect(screen.getByText('委托投递')).toBeInTheDocument();
    expect(screen.getByText('仅展示公开职位白名单字段')).toBeInTheDocument();

    const body = document.body.textContent ?? '';
    expect(body).not.toContain('联系解锁');
    expect(body).not.toContain('真实支付');
    expect(body).not.toContain('付费会员');
    expect(body).not.toContain('会员权益');
    expect(body).not.toContain('公共简历库');
  });

  it('sends token trace and idempotency key for candidate private writes then reloads server state', async () => {
    setToken();
    const fetchMock = mockCandidateClosureFetch();

    render(<CandidateCenter />);

    fireEvent.submit(await screen.findByRole('form', { name: '简历编辑表单' }));

    await screen.findByText('已同意，发布快照可见');
    const putCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'PUT');
    expect(putCall).toBeTruthy();
    const headers = putCall?.[1]?.headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer candidate-token');
    expect(headers.get('X-Trace-Id')).toBeTruthy();
    expect(headers.get('X-Idempotency-Key')).toMatch(/^candidate-resume-/);
    expect(JSON.parse(String(putCall?.[1]?.body))).toMatchObject({
      base_profile: {
        city_code: '310000',
        category_code: 'network_communication_electronics',
        expected_positions: ['Java工程师'],
        expected_cities: ['浦东新区']
      }
    });
  });

  it('uploads and deletes private resume attachment from candidate center', async () => {
    setToken();
    const fetchMock = mockCandidateClosureFetch();

    render(<CandidateCenter />);

    await screen.findByText('简历编辑');
    fireEvent.change(screen.getByLabelText('求职者中心上传附件简历'), {
      target: {
        files: [new File(['%PDF-1.4 LocalTalent'], 'resume-v2.pdf', { type: 'application/pdf' })]
      }
    });

    const uploadCall = await waitFor(() => fetchMock.mock.calls.find(([input, init]) => {
      const path = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
      return path.includes('/resume/attachment') && init?.method === 'POST';
    }));
    expect(uploadCall).toBeTruthy();
    const headers = uploadCall?.[1]?.headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer candidate-token');
    expect(headers.get('X-Idempotency-Key')).toMatch(/^candidate-resume-attachment-/);

    fireEvent.click(await screen.findByRole('button', { name: '删除附件' }));
    const deleteCall = await waitFor(() => fetchMock.mock.calls.find(([input, init]) => {
      const path = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
      return path.includes('/resume/attachment') && init?.method === 'DELETE';
    }));
    expect(deleteCall).toBeTruthy();
  });

  it('generates applies and dismisses private resume AI suggestions from candidate center', async () => {
    setToken();
    const fetchMock = mockCandidateClosureFetch();

    render(<CandidateCenter />);

    await screen.findByText('智能优化建议（安全规则版）');
    fireEvent.click(screen.getByRole('button', { name: '生成优化建议' }));

    const generateCall = await waitFor(() => fetchMock.mock.calls.find(([input, init]) => {
      const path = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
      return path.includes('/resume/ai-suggestions') && init?.method === 'POST' && !path.includes('/apply') && !path.includes('/dismiss');
    }));
    expect(generateCall).toBeTruthy();
    expect((generateCall?.[1]?.headers as Headers).get('X-Idempotency-Key')).toMatch(/^candidate-ai-generate-/);

    fireEvent.click(screen.getAllByRole('button', { name: '手动应用' })[0]);
    const applyCall = await waitFor(() => fetchMock.mock.calls.find(([input, init]) => {
      const path = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
      return path.includes('/resume/ai-suggestions/901/apply') && init?.method === 'POST';
    }));
    expect(applyCall).toBeTruthy();
    expect((applyCall?.[1]?.headers as Headers).get('X-Idempotency-Key')).toMatch(/^candidate-ai-apply-/);

    fireEvent.click(screen.getAllByRole('button', { name: '忽略' })[1]);
    const dismissCall = await waitFor(() => fetchMock.mock.calls.find(([input, init]) => {
      const path = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
      return path.includes('/resume/ai-suggestions/902/dismiss') && init?.method === 'POST';
    }));
    expect(dismissCall).toBeTruthy();
    expect((dismissCall?.[1]?.headers as Headers).get('X-Idempotency-Key')).toMatch(/^candidate-ai-dismiss-/);
  });

  it('does not render raw candidate fields from phase 3 private responses', async () => {
    setToken();
    mockCandidateClosureFetch({
      mobile: '13900001234',
      email: 'candidate@example.com',
      resume_body: 'raw resume body',
      attachment_object_key: 'resume/private.pdf',
      evidence: 'consent evidence',
      password_hash: 'bcrypt-hash',
      base_profile_json: '{"mobile":"13900001234"}',
      education_json: 'raw education',
      experience_json: 'raw experience',
      skills_json: 'raw skills'
    });

    render(<CandidateCenter />);

    expect(await screen.findByText('简历编辑')).toBeInTheDocument();
    const body = document.body.textContent ?? '';
    expect(body).not.toContain('13900001234');
    expect(body).not.toContain('candidate@example.com');
    expect(body).not.toContain('raw resume body');
    expect(body).not.toContain('resume/private.pdf');
    expect(body).not.toContain('consent evidence');
    expect(body).not.toContain('bcrypt-hash');
    expect(body).not.toContain('base_profile_json');
    expect(body).not.toContain('education_json');
    expect(body).not.toContain('experience_json');
    expect(body).not.toContain('skills_json');
  });

  it('reloads overview after consent post instead of inferring from post response', async () => {
    setToken();
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(apiOk(overview({
        consent_id: 91,
        publish_status: 'revoked',
        publishable_flag: 0,
        status_label: '已撤回'
      })))
      .mockResolvedValueOnce(apiOk({
        publish_status: 'consented',
        publishable_flag: 1
      }))
      .mockResolvedValueOnce(apiOk(overview({
        consent_id: 91,
        publish_status: 'not_publishable',
        publishable_flag: 0,
        status_label: '资料待补充',
        reason: '服务端重新读取后仍不可发布'
      })));

    render(<CandidateCenter />);

    fireEvent.click(await screen.findByRole('button', { name: '重新同意发布' }));

    expect(await screen.findByText('暂不可发布')).toBeInTheDocument();
    expect(screen.getByText('服务端重新读取后仍不可发布')).toBeInTheDocument();
    expect(screen.queryByText('已同意，发布快照可见')).not.toBeInTheDocument();

    const postCall = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST');
    expect(postCall).toBeTruthy();
    const headers = postCall?.[1]?.headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer candidate-token');
    expect(headers.get('X-Idempotency-Key')).toMatch(/^candidate-consent-/);
  });
});
