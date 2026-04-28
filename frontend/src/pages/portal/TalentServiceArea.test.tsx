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
