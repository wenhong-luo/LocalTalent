import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CandidateResumeCreate } from './CandidateResumeCreate';
import { CANDIDATE_TOKEN_STORAGE_KEY } from './candidateCenterApi';

function apiOk(data: unknown, traceId = 'trace-resume-create'): Response {
  return new Response(
    JSON.stringify({
      code: '0',
      message: 'success',
      trace_id: traceId,
      data
    }),
    { status: 200, headers: { 'Content-Type': 'application/json', 'X-Trace-Id': traceId } }
  );
}

const overviewPayload = {
  resume: {
    completion_percent: 20,
    updated_at: '',
    skills_summary: ''
  },
  applications: {
    total: 0,
    latest_status: '暂无投递',
    latest_job_title: '暂无'
  },
  signin: {
    latest_status: '暂无签到',
    latest_time: ''
  },
  consent: {
    consent_id: null,
    publish_status: 'not_publishable',
    publishable_flag: 0,
    status_label: '资料待补充',
    reason: '',
    updated_at: ''
  },
  stats: {
    favorite_count: 0,
    subscription_count: 0,
    unread_notification_count: 0
  },
  features: {
    candidate_closure_enabled: true
  },
  onboarding: {
    onboarding_required: true,
    onboarding_step: 'basic',
    publish_status: 'not_publishable'
  }
};

const resumePayload = {
  resume_id: null,
  resume_status: 'draft',
  completion_percent: 0,
  updated_at: '',
  resume_name: '我的简历',
  base_profile: {
    display_name: '',
    city_code: '',
    category_code: '',
    experience_years: null,
    summary: '',
    gender: '男',
    birth_date: '',
    highest_education: '',
    start_work_date: '',
    no_experience: false,
    contact_phone: '',
    contact_wechat: '',
    wechat_same_as_phone: false,
    expected_positions: [],
    expected_salary: '',
    expected_cities: [],
    job_status: '我目前已离职，可快速到岗'
  },
  education: [],
  experience: [],
  skills: [],
  work_experience: [],
  education_experience: [],
  self_description: '',
  has_attachment: false
};

function closurePayload(path: string): unknown {
  if (path.includes('/api/candidate/center/overview')) {
    return overviewPayload;
  }

  if (path.includes('/resume/preview') || path.includes('/resume')) {
    return resumePayload;
  }

  if (path.includes('/applications')) {
    return { application_list: [], total: 0 };
  }

  if (path.includes('/favorites')) {
    return { favorite_list: [], total: 0 };
  }

  if (path.includes('/subscriptions')) {
    return { subscription_list: [], total: 0 };
  }

  if (path.includes('/notifications')) {
    return { notification_list: [], total: 0 };
  }

  return {};
}

function mockResumeCreateFetch(extraResume: Record<string, unknown> = {}) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const path = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;

    if (init?.method === 'PUT') {
      return apiOk({
        ...resumePayload,
        ...extraResume,
        resume_id: 12,
        resume_status: 'complete',
        completion_percent: 100,
        updated_at: '2026-05-08T10:00:00'
      });
    }

    return apiOk(closurePayload(path));
  });
}

describe('CandidateResumeCreate', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
  });

  it('renders first resume completion page with disabled import placeholder', async () => {
    window.localStorage.setItem(CANDIDATE_TOKEN_STORAGE_KEY, 'candidate-token');
    mockResumeCreateFetch();

    render(<CandidateResumeCreate />);

    expect(await screen.findByLabelText('完善简历基本信息表单')).toBeInTheDocument();
    expect(screen.getByText('导入附件简历，一键完成在线简历填写')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '一键导入占位' })).toBeDisabled();
    expect(screen.getByLabelText('简历创建进度')).toHaveTextContent('1基本信息2完善简历3创建完成');
    expect(screen.getByPlaceholderText('请填写姓名')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('请输入期望职位，最多 5 个，用逗号分隔')).toBeInTheDocument();
  });

  it('moves through step two and success page while saving with token trace and idempotency key', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem(CANDIDATE_TOKEN_STORAGE_KEY, 'candidate-token');
    const fetchMock = mockResumeCreateFetch();

    render(<CandidateResumeCreate />);

    await user.type(await screen.findByPlaceholderText('请填写姓名'), '林同学');
    await user.type(screen.getByPlaceholderText('请填写联系电话'), '13900001234');
    await user.type(screen.getByPlaceholderText('请输入期望职位，最多 5 个，用逗号分隔'), 'Java工程师');
    await user.type(screen.getByPlaceholderText('请输入期望地区，最多 5 个，用逗号分隔'), '上海');
    await user.click(screen.getByRole('button', { name: '下一步' }));

    expect(await screen.findByLabelText('完善简历详情表单')).toBeInTheDocument();
    expect(screen.getAllByRole('button', { name: 'AI一键优化占位' })).toHaveLength(2);
    expect(screen.getAllByRole('button', { name: 'AI一键优化占位' })[0]).toBeDisabled();

    await user.type(screen.getByPlaceholderText('请填写公司名称'), 'LocalTalent 科技');
    await user.type(screen.getByPlaceholderText('请填写职位名称'), '后端工程师');
    await user.type(screen.getByPlaceholderText('请填写学校名称'), '上海大学');
    await user.type(screen.getByPlaceholderText('请填写专业名称'), '软件工程');
    await user.type(screen.getByPlaceholderText('请用一段话介绍你的优势、求职方向和亮点'), '关注稳定交付和合规边界。');
    await user.click(screen.getByRole('button', { name: '完成' }));

    expect(await screen.findByText('恭喜您，简历创建完成')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '进入会员中心' })).toHaveAttribute('href', '/candidate/center');
    expect(screen.getByRole('button', { name: '继续完善简历' })).toBeInTheDocument();

    const putCalls = fetchMock.mock.calls.filter(([, init]) => init?.method === 'PUT');
    expect(putCalls).toHaveLength(2);
    const basicBody = JSON.parse(String(putCalls[0]?.[1]?.body));
    expect(basicBody).toMatchObject({
      base_profile: {
        display_name: '林同学',
        contact_phone: '13900001234',
        expected_positions: ['Java工程师'],
        expected_cities: ['上海']
      },
      work_experience: [],
      education_experience: []
    });
    const putCall = putCalls[1];
    const headers = putCall?.[1]?.headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer candidate-token');
    expect(headers.get('X-Trace-Id')).toBeTruthy();
    expect(headers.get('X-Idempotency-Key')).toMatch(/^candidate-resume-/);
    expect(JSON.parse(String(putCall?.[1]?.body))).toMatchObject({
      base_profile: {
        display_name: '林同学',
        contact_phone: '13900001234',
        expected_positions: ['Java工程师'],
        expected_cities: ['上海']
      },
      work_experience: [{
        company_name: 'LocalTalent 科技',
        position_name: '后端工程师'
      }],
      education_experience: [{
        school_name: '上海大学',
        major_name: '软件工程'
      }],
      self_description: '关注稳定交付和合规边界。'
    });
  });

  it('does not render raw candidate field names or injected sensitive values', async () => {
    window.localStorage.setItem(CANDIDATE_TOKEN_STORAGE_KEY, 'candidate-token');
    mockResumeCreateFetch({
      base_profile_json: '{"contact_phone":"13900001234"}',
      education_json: 'raw education json',
      experience_json: 'raw experience json',
      skills_json: 'raw skills json',
      attachment_object_key: 'resume/private.pdf',
      evidence: 'private consent evidence',
      password_hash: 'bcrypt-hash'
    });

    render(<CandidateResumeCreate />);

    expect(await screen.findByLabelText('完善简历基本信息表单')).toBeInTheDocument();
    const body = document.body.textContent ?? '';
    expect(body).not.toContain('base_profile_json');
    expect(body).not.toContain('education_json');
    expect(body).not.toContain('experience_json');
    expect(body).not.toContain('skills_json');
    expect(body).not.toContain('resume/private.pdf');
    expect(body).not.toContain('private consent evidence');
    expect(body).not.toContain('bcrypt-hash');
  });

  it('shows unauthorized state without calling candidate APIs when token is missing', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch');

    render(<CandidateResumeCreate />);

    expect(await screen.findByText('请先登录求职者账号')).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('keeps external QR, AI and import abilities as disabled placeholders only', async () => {
    window.localStorage.setItem(CANDIDATE_TOKEN_STORAGE_KEY, 'candidate-token');
    mockResumeCreateFetch();

    render(<CandidateResumeCreate />);
    await screen.findByLabelText('完善简历基本信息表单');
    expect(screen.getByRole('button', { name: '一键导入占位' })).toBeDisabled();

    fireEvent.change(screen.getByPlaceholderText('请填写姓名'), { target: { value: '林同学' } });
    fireEvent.change(screen.getByPlaceholderText('请填写联系电话'), { target: { value: '13900001234' } });
    fireEvent.change(screen.getByPlaceholderText('请输入期望职位，最多 5 个，用逗号分隔'), { target: { value: 'Java工程师' } });
    fireEvent.change(screen.getByPlaceholderText('请输入期望地区，最多 5 个，用逗号分隔'), { target: { value: '上海' } });
    fireEvent.click(screen.getByRole('button', { name: '下一步' }));

    await screen.findByLabelText('完善简历详情表单');
    screen.getAllByRole('button', { name: 'AI一键优化占位' }).forEach((button) => {
      expect(button).toBeDisabled();
    });

    fireEvent.click(screen.getByRole('button', { name: '完成' }));
    await waitFor(() => expect(screen.getByText(/微信扫码关注公众号为占位展示/)).toBeInTheDocument());
    expect(screen.queryByRole('link', { name: /微信|小程序|App/ })).not.toBeInTheDocument();
  });
});
