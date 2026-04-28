import { fireEvent, render, screen } from '@testing-library/react';
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
