import { render, screen } from '@testing-library/react';
import { RouteGuard } from './RouteGuard';
import { ACCESS_TOKEN_STORAGE_KEY, ADMIN_ROLE_HINT_STORAGE_KEY } from '@/pages/backoffice/session';

function apiOk(data: unknown): Response {
  return new Response(
    JSON.stringify({
      code: '0',
      message: 'success',
      trace_id: 'trace-guard',
      data
    }),
    { status: 200, headers: { 'Content-Type': 'application/json' } }
  );
}

describe('RouteGuard', () => {
  beforeEach(() => {
    window.localStorage.clear();
    vi.restoreAllMocks();
  });

  it('shows unauthorized state when token is missing', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch');

    render(
      <RouteGuard allowedIdentities={['company']} title="企业后台">
        {() => <div>企业后台内容</div>}
      </RouteGuard>
    );

    expect(await screen.findByText('无权限查看')).toBeInTheDocument();
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it('blocks candidate identity from company and admin routes', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'candidate-token');
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(apiOk({
      identity_type: 'candidate',
      user_id: 1,
      display_name: '求职者'
    }));

    render(
      <RouteGuard allowedIdentities={['company']} title="企业后台">
        {() => <div>企业后台内容</div>}
      </RouteGuard>
    );

    expect(await screen.findByText('无权限查看')).toBeInTheDocument();
    expect(screen.queryByText('企业后台内容')).not.toBeInTheDocument();
  });

  it('allows company identity to enter company route', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'company-token');
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(apiOk({
      identity_type: 'company',
      user_id: 2,
      company_id: 10,
      display_name: '示例企业/管理员'
    }));

    render(
      <RouteGuard allowedIdentities={['company']} title="企业后台">
        {() => <div>企业后台内容</div>}
      </RouteGuard>
    );

    expect(await screen.findByText('企业后台内容')).toBeInTheDocument();
  });

  it('passes admin role hint after operator identity is authorized', async () => {
    window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, 'operator-token');
    window.localStorage.setItem(ADMIN_ROLE_HINT_STORAGE_KEY, 'auditor');
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(apiOk({
      identity_type: 'operator',
      user_id: 3,
      display_name: '审计员'
    }));

    render(
      <RouteGuard allowedIdentities={['operator']} title="运营后台">
        {({ adminRoleHint }) => <div>role hint: {adminRoleHint}</div>}
      </RouteGuard>
    );

    expect(await screen.findByText('role hint: auditor')).toBeInTheDocument();
  });
});
