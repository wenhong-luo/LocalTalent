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
    candidate_closure_enabled: true,
    resume_attachment_upload_enabled: true,
    resume_ai_assist_enabled: false
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

const aiTaskPayload = {
  task_id: 80,
  task_status: 'generated',
  suggestion_count: 2,
  applied_count: 0,
  dismissed_count: 0,
  generated_at: '2026-05-08T11:00:00',
  items: [{
    suggestion_id: 801,
    suggestion_type: 'self_description',
    target_field: 'self_description',
    title: '补充自我描述',
    reason_summary: '自我描述较短，建议补充交付亮点。',
    before_preview: '暂无自我描述',
    suggested_value: '关注稳定交付，熟悉本地人才服务合规边界。',
    can_apply: true,
    apply_status: 'pending'
  }, {
    suggestion_id: 802,
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

async function chooseJavaEngineer(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole('button', { name: /请选择期望职位/ }));
  expect(await screen.findByRole('dialog', { name: '期望职位' })).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: '网络 | 通信 | 电子' }));
  await user.click(screen.getByLabelText('Java工程师'));
  await user.click(screen.getByRole('button', { name: '保存' }));
}

async function chooseWenxiRegion(user: ReturnType<typeof userEvent.setup>) {
  await user.click(screen.getByRole('button', { name: /请选择期望地区/ }));
  expect(await screen.findByRole('dialog', { name: '期望地区' })).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: '山西' }));
  await user.click(screen.getByRole('button', { name: '运城' }));
  await user.click(screen.getByRole('button', { name: '闻喜县' }));
  await user.click(screen.getByRole('button', { name: '保存' }));
}

function closurePayload(path: string, overview = overviewPayload): unknown {
  if (path.includes('/api/candidate/center/overview')) {
    return overview;
  }

  if (path.includes('/resume/ai-suggestions')) {
    return {
      ...aiTaskPayload,
      items: []
    };
  }

  if (path.includes('/resume/attachment')) {
    return {
      has_attachment: false,
      attachment_status: 'empty',
      file_name: '',
      content_type: '',
      size_bytes: null,
      uploaded_at: ''
    };
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

function mockResumeCreateFetch(
  extraResume: Record<string, unknown> = {},
  overview = overviewPayload
) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (input, init) => {
    const path = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;

    if (init?.method === 'POST' && path.includes('/resume/ai-suggestions/801/apply')) {
      return apiOk({
        ...aiTaskPayload,
        applied_count: 1,
        items: aiTaskPayload.items.map((item) => item.suggestion_id === 801
          ? { ...item, apply_status: 'applied' }
          : item)
      });
    }

    if (init?.method === 'POST' && path.includes('/resume/ai-suggestions/')) {
      return apiOk({
        ...aiTaskPayload,
        dismissed_count: 1,
        items: aiTaskPayload.items.map((item) => item.suggestion_id === 802
          ? { ...item, apply_status: 'dismissed' }
          : item)
      });
    }

    if (init?.method === 'POST' && path.includes('/resume/ai-suggestions')) {
      return apiOk(aiTaskPayload);
    }

    if (init?.method === 'POST' && path.includes('/resume/attachment')) {
      return apiOk({
        has_attachment: true,
        attachment_status: 'uploaded',
        file_name: 'resume.pdf',
        content_type: 'application/pdf',
        size_bytes: 18,
        uploaded_at: '2026-05-08T10:10:00'
      });
    }

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

    return apiOk(closurePayload(path, overview));
  });
}

describe('CandidateResumeCreate', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
  });

  it('renders first resume completion page with private attachment upload', async () => {
    window.localStorage.setItem(CANDIDATE_TOKEN_STORAGE_KEY, 'candidate-token');
    mockResumeCreateFetch();

    render(<CandidateResumeCreate />);

    expect(await screen.findByLabelText('完善简历基本信息表单')).toBeInTheDocument();
    expect(screen.getByText('导入附件简历，一键完成在线简历填写')).toBeInTheDocument();
    expect(screen.getByLabelText('上传附件简历')).toBeInTheDocument();
    expect(screen.getByLabelText('简历创建进度')).toHaveTextContent('1基本信息2完善简历3创建完成');
    expect(screen.getByPlaceholderText('请填写姓名')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /请选择期望职位，最多6个/ })).toBeInTheDocument();
  });

  it('opens professional expected position picker and limits selection to six positions', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem(CANDIDATE_TOKEN_STORAGE_KEY, 'candidate-token');
    mockResumeCreateFetch();

    render(<CandidateResumeCreate />);

    await user.click(await screen.findByRole('button', { name: /请选择期望职位/ }));
    expect(screen.getByRole('dialog', { name: '期望职位' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '生活 | 服务业' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '人力 | 行政 | 管理' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '网络 | 通信 | 电子' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '保存' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '取消' })).toBeInTheDocument();
    expect(screen.getByLabelText('已选期望职位输入框')).toHaveTextContent('请选择期望职位，最多6个');

    await user.click(screen.getByRole('button', { name: '网络 | 通信 | 电子' }));
    expect(screen.getByText('计算机/互联网/通信')).toBeInTheDocument();
    expect(screen.getByText('电子/电气')).toBeInTheDocument();
    await user.click(screen.getAllByLabelText('不限')[0]);
    expect(screen.getByLabelText('已选期望职位输入框')).toHaveTextContent('计算机/互联网/通信不限');
    expect(screen.getByLabelText('移除计算机/互联网/通信不限')).toBeInTheDocument();
    await user.click(screen.getByLabelText('Java工程师'));
    expect(screen.getByLabelText('已选期望职位输入框')).toHaveTextContent('Java工程师');
    expect(screen.queryByLabelText('移除计算机/互联网/通信不限')).not.toBeInTheDocument();

    for (const position of ['前端工程师', '后端工程师', 'Python工程师', '测试工程师', '运维工程师']) {
      await user.click(screen.getByLabelText(position));
    }
    expect(screen.getByText('已选 6/6')).toBeInTheDocument();
    expect(screen.getByLabelText('产品经理')).toBeDisabled();

    await user.type(screen.getByLabelText('搜索期望职位'), '护士');
    expect(screen.getByRole('button', { name: '医疗 | 制药 | 环保' })).toBeInTheDocument();
    await user.clear(screen.getByLabelText('搜索期望职位'));
    await user.click(screen.getByLabelText('移除Java工程师'));
    expect(screen.getByText('已选 5/6')).toBeInTheDocument();
  });

  it('shows selected positions inside the picker and persists them only after save', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem(CANDIDATE_TOKEN_STORAGE_KEY, 'candidate-token');
    mockResumeCreateFetch();

    render(<CandidateResumeCreate />);

    const trigger = await screen.findByRole('button', { name: /请选择期望职位/ });
    await user.click(trigger);
    await user.click(screen.getByRole('button', { name: '网络 | 通信 | 电子' }));
    await user.click(screen.getByLabelText('Java工程师'));

    expect(screen.getByLabelText('已选期望职位输入框')).toHaveTextContent('Java工程师');
    expect(screen.getByLabelText('移除Java工程师')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '清空' }));
    expect(screen.getByLabelText('已选期望职位输入框')).toHaveTextContent('请选择期望职位，最多6个');
    expect(screen.queryByLabelText('移除Java工程师')).not.toBeInTheDocument();

    await user.click(screen.getByLabelText('Java工程师'));
    expect(screen.getByLabelText('已选期望职位输入框')).toHaveTextContent('Java工程师');

    await user.click(screen.getByRole('button', { name: '取消' }));
    expect(screen.queryByRole('dialog', { name: '期望职位' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /请选择期望职位，最多6个/ })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /请选择期望职位/ }));
    await user.click(screen.getByRole('button', { name: '网络 | 通信 | 电子' }));
    await user.click(screen.getByLabelText('Java工程师'));
    await user.click(screen.getByRole('button', { name: '保存' }));

    expect(screen.queryByRole('dialog', { name: '期望职位' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: '期望职位：Java工程师' })).toHaveTextContent('Java工程师');
  });

  it('opens expected region picker and persists selected districts only after save', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem(CANDIDATE_TOKEN_STORAGE_KEY, 'candidate-token');
    mockResumeCreateFetch();

    render(<CandidateResumeCreate />);

    const trigger = await screen.findByRole('button', { name: /请选择期望地区/ });
    await user.click(trigger);
    expect(screen.getByRole('dialog', { name: '期望地区' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '山西' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '上海' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '保存' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '取消' })).toBeInTheDocument();
    expect(screen.getByLabelText('已选期望地区输入框')).toHaveTextContent('请选择期望地区，最多6个');

    await user.click(screen.getByRole('button', { name: '山西' }));
    await user.click(screen.getByRole('button', { name: '运城' }));
    await user.click(screen.getByRole('button', { name: '闻喜县' }));
    expect(screen.getByLabelText('已选期望地区输入框')).toHaveTextContent('闻喜县');
    expect(screen.getByLabelText('移除闻喜县')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '清空' }));
    expect(screen.getByLabelText('已选期望地区输入框')).toHaveTextContent('请选择期望地区，最多6个');
    expect(screen.queryByLabelText('移除闻喜县')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '闻喜县' }));
    await user.click(screen.getByRole('button', { name: '取消' }));
    expect(screen.queryByRole('dialog', { name: '期望地区' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /请选择期望地区，最多6个/ })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /请选择期望地区/ }));
    await user.click(screen.getByRole('button', { name: '山西' }));
    await user.click(screen.getByRole('button', { name: '运城' }));
    await user.click(screen.getByRole('button', { name: '闻喜县' }));
    await user.click(screen.getByRole('button', { name: '万荣县' }));
    expect(screen.getByLabelText('已选期望地区输入框')).toHaveTextContent('闻喜县');
    expect(screen.getByLabelText('已选期望地区输入框')).toHaveTextContent('万荣县');

    await user.click(screen.getByRole('button', { name: '选择全运城' }));
    expect(screen.getByLabelText('已选期望地区输入框')).toHaveTextContent('全运城');
    expect(screen.queryByLabelText('移除闻喜县')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '闻喜县' }));
    expect(screen.getByLabelText('已选期望地区输入框')).toHaveTextContent('闻喜县');
    expect(screen.queryByLabelText('移除全运城')).not.toBeInTheDocument();

    for (const district of ['盐湖区', '临猗县', '万荣县', '稷山县', '新绛县']) {
      await user.click(screen.getByRole('button', { name: district }));
    }
    expect(screen.getByText('已选 6/6')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '绛县' })).toBeDisabled();

    await user.click(screen.getByRole('button', { name: '保存' }));
    expect(screen.queryByRole('dialog', { name: '期望地区' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /期望地区：/ })).toHaveTextContent('闻喜县');
  });

  it('moves through step two and success page while saving with token trace and idempotency key', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem(CANDIDATE_TOKEN_STORAGE_KEY, 'candidate-token');
    const fetchMock = mockResumeCreateFetch();

    render(<CandidateResumeCreate />);

    await user.type(await screen.findByPlaceholderText('请填写姓名'), '林同学');
    await user.type(screen.getByPlaceholderText('请填写联系电话'), '13900001234');
    await chooseJavaEngineer(user);
    await chooseWenxiRegion(user);
    await user.click(screen.getByRole('button', { name: '下一步' }));

    expect(await screen.findByLabelText('完善简历详情表单')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'AI 优化暂未开放' })).toBeDisabled();

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
        city_code: '140823',
        category_code: 'network_communication_electronics',
        expected_positions: ['Java工程师'],
        expected_cities: ['闻喜县']
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
        city_code: '140823',
        category_code: 'network_communication_electronics',
        expected_positions: ['Java工程师'],
        expected_cities: ['闻喜县']
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

  it('uploads resume attachment after saving basic profile into private domain', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem(CANDIDATE_TOKEN_STORAGE_KEY, 'candidate-token');
    const fetchMock = mockResumeCreateFetch();

    render(<CandidateResumeCreate />);

    await user.type(await screen.findByPlaceholderText('请填写姓名'), '林同学');
    await user.type(screen.getByPlaceholderText('请填写联系电话'), '13900001234');
    await chooseJavaEngineer(user);
    await chooseWenxiRegion(user);
    await user.upload(screen.getByLabelText('上传附件简历'), new File(['%PDF-1.4 LocalTalent'], 'resume.pdf', { type: 'application/pdf' }));

    expect(await screen.findByText(/附件简历已上传到本人私有域/)).toBeInTheDocument();
    expect(screen.getByText(/已上传：resume.pdf/)).toBeInTheDocument();
    const uploadCall = fetchMock.mock.calls.find(([input, init]) => {
      const path = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
      return path.includes('/resume/attachment') && init?.method === 'POST';
    });
    expect(uploadCall).toBeTruthy();
    const headers = uploadCall?.[1]?.headers as Headers;
    expect(headers.get('Authorization')).toBe('Bearer candidate-token');
    expect(headers.get('X-Trace-Id')).toBeTruthy();
    expect(headers.get('X-Idempotency-Key')).toMatch(/^candidate-resume-attachment-/);
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

  it('keeps external QR and AI abilities as disabled placeholders when AI flag is off', async () => {
    window.localStorage.setItem(CANDIDATE_TOKEN_STORAGE_KEY, 'candidate-token');
    mockResumeCreateFetch();

    render(<CandidateResumeCreate />);
    await screen.findByLabelText('完善简历基本信息表单');
    expect(screen.getByLabelText('上传附件简历')).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText('请填写姓名'), { target: { value: '林同学' } });
    fireEvent.change(screen.getByPlaceholderText('请填写联系电话'), { target: { value: '13900001234' } });
    fireEvent.click(screen.getByRole('button', { name: /请选择期望职位/ }));
    fireEvent.click(await screen.findByRole('button', { name: '网络 | 通信 | 电子' }));
    fireEvent.click(screen.getByLabelText('Java工程师'));
    fireEvent.click(screen.getByRole('button', { name: '保存' }));
    fireEvent.click(screen.getByRole('button', { name: /请选择期望地区/ }));
    fireEvent.click(await screen.findByRole('button', { name: '山西' }));
    fireEvent.click(screen.getByRole('button', { name: '运城' }));
    fireEvent.click(screen.getByRole('button', { name: '闻喜县' }));
    fireEvent.click(screen.getByRole('button', { name: '保存' }));
    fireEvent.click(screen.getByRole('button', { name: '下一步' }));

    await screen.findByLabelText('完善简历详情表单');
    expect(screen.getByRole('button', { name: 'AI 优化暂未开放' })).toBeDisabled();

    fireEvent.click(screen.getByRole('button', { name: '完成' }));
    await waitFor(() => expect(screen.getByText(/微信扫码关注公众号为占位展示/)).toBeInTheDocument());
    expect(screen.queryByRole('link', { name: /微信|小程序|App/ })).not.toBeInTheDocument();
  });

  it('generates and manually applies safe rule-based suggestions when AI flag is on', async () => {
    const user = userEvent.setup();
    window.localStorage.setItem(CANDIDATE_TOKEN_STORAGE_KEY, 'candidate-token');
    const fetchMock = mockResumeCreateFetch({}, {
      ...overviewPayload,
      features: {
        ...overviewPayload.features,
        resume_ai_assist_enabled: true
      }
    });

    render(<CandidateResumeCreate />);

    await user.type(await screen.findByPlaceholderText('请填写姓名'), '林同学');
    await user.type(screen.getByPlaceholderText('请填写联系电话'), '13900001234');
    await chooseJavaEngineer(user);
    await chooseWenxiRegion(user);
    await user.click(screen.getByRole('button', { name: '下一步' }));

    expect(await screen.findByRole('button', { name: '生成优化建议' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: '生成优化建议' }));

    expect(await screen.findByText('补充自我描述')).toBeInTheDocument();
    expect(screen.getByText(/本地规则建议/)).toBeInTheDocument();
    await user.click(screen.getAllByRole('button', { name: '手动应用' })[0]);

    await waitFor(() => {
      const applyCall = fetchMock.mock.calls.find(([input, init]) => {
        const path = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url;
        return path.includes('/resume/ai-suggestions/801/apply') && init?.method === 'POST';
      });
      expect(applyCall).toBeTruthy();
      const headers = applyCall?.[1]?.headers as Headers;
      expect(headers.get('Authorization')).toBe('Bearer candidate-token');
      expect(headers.get('X-Idempotency-Key')).toMatch(/^candidate-ai-apply-/);
    });

    expect(await screen.findByText('applied')).toBeInTheDocument();
  });
});
