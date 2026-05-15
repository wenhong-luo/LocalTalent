import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { TalentServiceArea, type TalentServiceAreaInitialState } from './TalentServiceArea';
import type { TalentSnapshotPage } from './talentSnapshotApi';

const page: TalentSnapshotPage = {
  snapshot_list: [
    {
      snapshot_id: 101,
      display_name_masked: '张*',
      city_code: '310000',
      category_code: 'software',
      skills_summary: 'Java / Spring Boot / MySQL',
      experience_years: 5,
      updated_at: '2026-04-28T10:00:00+08:00'
    }
  ],
  total: 1,
  page: 1,
  size: 12
};

function initialState(state: Partial<TalentServiceAreaInitialState>): TalentServiceAreaInitialState {
  return {
    status: 'ready',
    query: { page: 1, size: 12 },
    page,
    traceId: 'trace-initial',
    ...state
  };
}

describe('TalentServiceArea', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('renders initial snapshot list from SSR data', () => {
    render(<TalentServiceArea initialState={initialState({})} />);

    expect(screen.getByTestId('talent-service-area-page')).toHaveAttribute('data-layout', 'portal-ad-rails');
    expect(screen.getByTestId('portal-ad-rail-frame')).toHaveAttribute('data-layout', 'portal-ad-rails');
    expect(screen.getByLabelText('人才服务区左侧广告留白')).toBeInTheDocument();
    expect(screen.getByLabelText('人才服务区右侧广告留白')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '人才服务区：只展示候选人授权发布的快照摘要' })).toBeInTheDocument();
    expect(screen.getByRole('form', { name: '人才服务区筛选' })).toBeInTheDocument();
    expect(screen.getByRole('region', { name: '发布快照列表' })).toBeInTheDocument();
    expect(screen.getByText('发布快照 #101')).toBeInTheDocument();
    expect(screen.getByText('字段白名单')).toBeInTheDocument();
    expect(screen.getByText('发布快照边界')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '详情读取：不开放' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '联系方式：不开放' })).toBeDisabled();
    expect(screen.getByText('张*')).toBeInTheDocument();
    expect(screen.getByText('Java / Spring Boot / MySQL')).toBeInTheDocument();
    expect(screen.getByText(/trace_id：trace-initial/)).toBeInTheDocument();
  });

  it.each([
    { status: 'loading', label: '正在读取人才服务区' },
    { status: 'empty', label: '暂无可展示发布快照' },
    { status: 'error', label: '人才服务区暂时不可用' },
    { status: 'unauthorized', label: '暂无权限读取人才服务区' },
    { status: 'retrying', label: '正在重新读取发布快照' }
  ] as const)('renders $status state', ({ status, label }) => {
    render(
      <TalentServiceArea
        initialState={initialState({
          status,
          page: status === 'empty' ? { ...page, snapshot_list: [], total: 0 } : undefined,
          message: '状态机测试'
        })}
      />
    );

    expect(screen.getByText(label)).toBeInTheDocument();
  });

  it('does not render raw candidate fields even when response contains extra data', () => {
    render(
      <TalentServiceArea
        initialState={initialState({
          page: {
            ...page,
            snapshot_list: [
              {
                ...page.snapshot_list[0],
                mobile: '13900001234',
                email: 'candidate@example.com',
                resume_body: 'raw resume body',
                attachment_object_key: 'resume/full.pdf',
                evidence: 'consent evidence',
                password_hash: 'bcrypt-hash',
                base_profile_json: '{"name":"raw"}',
                education_json: '{"school":"raw"}',
                experience_json: '{"company":"raw"}',
                skills_json: '{"private":"raw"}'
              } as never
            ]
          }
        })}
      />
    );

    const body = document.body.textContent ?? '';
    expect(body).not.toContain('13900001234');
    expect(body).not.toContain('candidate@example.com');
    expect(body).not.toContain('raw resume body');
    expect(body).not.toContain('resume/full.pdf');
    expect(body).not.toContain('consent evidence');
    expect(body).not.toContain('bcrypt-hash');
    expect(body).not.toContain('base_profile_json');
    expect(body).not.toContain('education_json');
    expect(body).not.toContain('experience_json');
    expect(body).not.toContain('skills_json');
  });

  it('submits stable public filter query for shareable snapshot search', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          code: '0',
          message: 'success',
          trace_id: 'trace-filter',
          data: page
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      )
    );

    render(<TalentServiceArea initialState={initialState({})} />);

    fireEvent.change(screen.getByLabelText('城市'), { target: { value: '310000' } });
    fireEvent.change(screen.getByLabelText('职类'), { target: { value: 'software' } });
    fireEvent.change(screen.getByLabelText('经验下限'), { target: { value: '3' } });
    fireEvent.change(screen.getByLabelText('经验上限'), { target: { value: '8' } });
    fireEvent.change(screen.getByLabelText('更新时间'), { target: { value: '7' } });
    fireEvent.change(screen.getByLabelText('排序'), { target: { value: 'experience_desc' } });
    fireEvent.submit(screen.getByRole('form', { name: '人才服务区筛选' }));

    await waitFor(() => {
      expect(fetchSpy).toHaveBeenCalled();
    });

    const requestUrl = String(fetchSpy.mock.calls[0][0]);
    expect(requestUrl).toContain('/api/portal/talent-snapshots?');
    expect(requestUrl).toContain('city_code=310000');
    expect(requestUrl).toContain('category_code=software');
    expect(requestUrl).toContain('experience_min=3');
    expect(requestUrl).toContain('experience_max=8');
    expect(requestUrl).toContain('updated_within=7');
    expect(requestUrl).toContain('sort=experience_desc');
    expect(requestUrl).toContain('page=1');
    expect(requestUrl).toContain('size=12');
  });

  it('retries from error state and renders returned snapshots', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          code: '0',
          message: 'success',
          trace_id: 'trace-retry',
          data: page
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      )
    );

    render(
      <TalentServiceArea
        initialState={initialState({
          status: 'error',
          page: undefined,
          message: '初始失败'
        })}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: '重新读取' }));

    expect(await screen.findByText('张*')).toBeInTheDocument();
    expect(screen.getByText(/trace_id：trace-retry/)).toBeInTheDocument();
  });

  it('maps unauthorized retry response to unauthorized state', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          code: 'AUTHZ_403',
          message: '无权限',
          trace_id: 'trace-unauthorized',
          data: null
        }),
        { status: 403, headers: { 'Content-Type': 'application/json' } }
      )
    );

    render(
      <TalentServiceArea
        initialState={initialState({
          status: 'error',
          page: undefined,
          message: '初始失败'
        })}
      />
    );

    fireEvent.click(screen.getByRole('button', { name: '重新读取' }));

    await waitFor(() => {
      expect(screen.getByText('暂无权限读取人才服务区')).toBeInTheDocument();
    });
  });
});
