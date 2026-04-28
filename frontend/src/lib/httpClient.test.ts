import { apiGet, HttpClientError } from './httpClient';

describe('httpClient', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('injects X-Trace-Id and parses unified response body', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          code: '0',
          message: 'success',
          trace_id: 'trace-from-backend',
          data: { ok: true }
        }),
        { status: 200, headers: { 'Content-Type': 'application/json' } }
      )
    );

    const result = await apiGet<{ ok: boolean }>('/api/portal/talent-snapshots?page=1');

    expect(result).toEqual({ data: { ok: true }, traceId: 'trace-from-backend' });
    const headers = fetchMock.mock.calls[0][1]?.headers as Headers;
    expect(headers.get('X-Trace-Id')).toMatch(/^trace-|^[0-9a-f-]{36}$/);
  });

  it.each([
    { status: 401, code: 'AUTH_401' },
    { status: 403, code: 'AUTHZ_403' }
  ])('maps $status to unauthorized state', async ({ status, code }) => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(
        JSON.stringify({
          code,
          message: '无权限',
          trace_id: 'trace-denied',
          data: null
        }),
        { status, headers: { 'Content-Type': 'application/json' } }
      )
    );

    await expect(apiGet('/api/portal/talent-snapshots')).rejects.toMatchObject({
      kind: 'unauthorized',
      status,
      code,
      traceId: 'trace-denied'
    });
  });

  it('maps network failure to error state', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('connection refused'));

    await expect(apiGet('/api/portal/talent-snapshots')).rejects.toBeInstanceOf(HttpClientError);
    await expect(apiGet('/api/portal/talent-snapshots')).rejects.toMatchObject({
      kind: 'error',
      status: 0,
      code: 'NETWORK_ERROR'
    });
  });
});
