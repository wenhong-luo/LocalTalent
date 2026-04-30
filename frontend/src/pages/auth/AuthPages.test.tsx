import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ADMIN_ROLE_HINT_STORAGE_KEY, ACCESS_TOKEN_STORAGE_KEY } from '@/pages/backoffice/session';
import { AuthPage } from './AuthPage';

function apiOk(data: unknown, traceId = 'trace-auth'): Response {
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

function apiError(code = 'AUTH_401', message = '账号或密码错误', traceId = 'trace-auth-error'): Response {
  return new Response(
    JSON.stringify({
      code,
      message,
      trace_id: traceId,
      data: null
    }),
    { status: 401, headers: { 'Content-Type': 'application/json', 'X-Trace-Id': traceId } }
  );
}

function loginResponse(identityType: string, token = `${identityType}-token`) {
  return {
    access_token: token,
    token_type: 'Bearer',
    expires_in: 3600,
    identity: {
      identity_type: identityType,
      user_id: identityType === 'company' ? 20 : 10,
      company_id: identityType === 'company' ? 30 : null,
      display_name: `${identityType} user`
    }
  };
}

describe('AuthPage', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
  });

  it('renders login UI and keeps external login capabilities disabled', () => {
    render(<AuthPage mode="login" initialRole="candidate" onNavigate={vi.fn()} />);

    expect(screen.getByLabelText('求职者登录')).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '求职者登录' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: '招聘者登录' })).toHaveAttribute('aria-selected', 'false');
    expect(screen.getByLabelText('账号')).toBeInTheDocument();
    expect(screen.getByLabelText('密码')).toBeInTheDocument();

    const external = screen.getByLabelText('外部登录占位');
    expect(within(external).getByRole('button', { name: '短信验证码占位' })).toBeDisabled();
    expect(within(external).getByRole('button', { name: '微信扫码占位' })).toBeDisabled();
    expect(within(external).getByRole('button', { name: '小程序占位' })).toBeDisabled();
    expect(within(external).getByRole('button', { name: 'App 登录占位' })).toBeDisabled();
  });

  it('switches register UI between candidate and company forms', () => {
    render(<AuthPage mode="register" initialRole="candidate" onNavigate={vi.fn()} />);

    expect(screen.getByRole('tab', { name: '求职者注册' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByLabelText('邮箱')).toBeInTheDocument();
    expect(screen.getByLabelText('昵称')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: '招聘者注册' }));

    expect(screen.getByRole('tab', { name: '招聘者注册' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByLabelText('企业名称')).toBeInTheDocument();
    expect(screen.getByLabelText('统一信用代码占位')).toBeInTheDocument();
    expect(screen.getByLabelText('联系人姓名')).toBeInTheDocument();
  });

  it('logs in candidate, stores token and navigates to candidate center', async () => {
    const user = userEvent.setup();
    const navigate = vi.fn();
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(apiOk(loginResponse('candidate')));

    render(<AuthPage mode="login" initialRole="candidate" onNavigate={navigate} />);

    await user.type(screen.getByLabelText('账号'), 'candidate@example.com');
    await user.type(screen.getByLabelText('密码'), 'Candidate@123456');
    await user.click(screen.getByRole('button', { name: '立即求职者登录' }));

    await waitFor(() => expect(navigate).toHaveBeenCalledWith('/candidate/center'));
    expect(window.localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY)).toBe('candidate-token');

    const [url, init] = fetchMock.mock.calls[0];
    expect(String(url)).toBe('/api/auth/login');
    expect(JSON.parse(String(init?.body))).toEqual({
      identity_type: 'candidate',
      account: 'candidate@example.com',
      password: 'Candidate@123456'
    });
  });

  it('logs in company and operator identities to their target centers', async () => {
    const user = userEvent.setup();
    const navigate = vi.fn();
    const fetchMock = vi.spyOn(globalThis, 'fetch');

    fetchMock.mockResolvedValueOnce(apiOk(loginResponse('company')));
    render(<AuthPage mode="login" initialRole="company" onNavigate={navigate} />);

    await user.type(screen.getByLabelText('账号'), 'company@example.com');
    await user.type(screen.getByLabelText('密码'), 'Company@123456');
    await user.click(screen.getByRole('button', { name: '立即招聘者登录' }));

    await waitFor(() => expect(navigate).toHaveBeenCalledWith('/company'));

    cleanup();
    window.localStorage.clear();
    navigate.mockClear();
    fetchMock.mockResolvedValueOnce(apiOk(loginResponse('operator', 'operator-token')));

    render(<AuthPage mode="login" initialRole="candidate" onNavigate={navigate} />);
    await user.type(screen.getByLabelText('账号'), 'operator');
    await user.type(screen.getByLabelText('密码'), 'LocalTalent@123456');
    await user.click(screen.getByRole('button', { name: '立即求职者登录' }));

    await waitFor(() => expect(navigate).toHaveBeenCalledWith('/admin'));
    expect(window.localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY)).toBe('operator-token');
    expect(window.localStorage.getItem(ADMIN_ROLE_HINT_STORAGE_KEY)).toBe('operator');
  });

  it('submits candidate and company register payloads and guides users back to login when no token is returned', async () => {
    const user = userEvent.setup();
    const fetchMock = vi.spyOn(globalThis, 'fetch');
    fetchMock.mockResolvedValueOnce(apiOk({
      identity: {
        identity_type: 'candidate',
        user_id: 99,
        display_name: '求职者'
      }
    }, 'trace-candidate-register'));

    render(<AuthPage mode="register" initialRole="candidate" onNavigate={vi.fn()} />);
    await user.type(screen.getByLabelText('邮箱'), 'candidate@example.com');
    await user.type(screen.getByLabelText('昵称'), '求职者');
    await user.type(screen.getByLabelText('密码'), 'Candidate@123456');
    await user.click(screen.getByRole('button', { name: '立即求职者注册' }));

    expect(await screen.findByText(/求职者注册成功/)).toBeInTheDocument();
    expect(JSON.parse(String(fetchMock.mock.calls[0][1]?.body))).toEqual({
      identity_type: 'candidate',
      email: 'candidate@example.com',
      password: 'Candidate@123456',
      display_name: '求职者'
    });

    fetchMock.mockResolvedValueOnce(apiOk({
      identity: {
        identity_type: 'company',
        user_id: 100,
        company_id: 200,
        display_name: '示例企业/管理员'
      }
    }, 'trace-company-register'));

    fireEvent.click(screen.getByRole('tab', { name: '招聘者注册' }));
    await user.type(screen.getByLabelText('企业名称'), '示例企业');
    await user.type(screen.getByLabelText('统一信用代码占位'), 'LIC-20260430');
    await user.type(screen.getByLabelText('联系人姓名'), '企业管理员');
    await user.clear(screen.getByLabelText('邮箱'));
    await user.type(screen.getByLabelText('邮箱'), 'company@example.com');
    await user.clear(screen.getByLabelText('密码'));
    await user.type(screen.getByLabelText('密码'), 'Company@123456');
    await user.click(screen.getByRole('button', { name: '立即招聘者注册' }));

    expect(await screen.findByText(/招聘者注册成功/)).toBeInTheDocument();
    expect(JSON.parse(String(fetchMock.mock.calls[1][1]?.body))).toMatchObject({
      identity_type: 'company',
      company_name: '示例企业',
      license_no: 'LIC-20260430',
      user_name: '企业管理员',
      email: 'company@example.com',
      password: 'Company@123456'
    });
  });

  it('shows unified auth error with trace id without bypassing permissions', async () => {
    const user = userEvent.setup();
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(apiError());

    render(<AuthPage mode="login" initialRole="candidate" onNavigate={vi.fn()} />);
    await user.type(screen.getByLabelText('账号'), 'candidate@example.com');
    await user.type(screen.getByLabelText('密码'), 'Wrong@123456');
    await user.click(screen.getByRole('button', { name: '立即求职者登录' }));

    expect(await screen.findByText(/账号或密码错误 trace_id：trace-auth-error/)).toBeInTheDocument();
    expect(window.localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY)).toBeNull();
  });
});
