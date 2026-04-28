export type ApiResponse<T> = {
  code: string;
  message: string;
  trace_id?: string;
  data: T;
};

export type ApiResult<T> = {
  data: T;
  traceId: string;
};

export type HttpClientErrorKind = 'unauthorized' | 'error';

export class HttpClientError extends Error {
  readonly kind: HttpClientErrorKind;
  readonly status: number;
  readonly code: string;
  readonly traceId?: string;

  constructor(message: string, options: { kind: HttpClientErrorKind; status: number; code: string; traceId?: string }) {
    super(message);
    this.name = 'HttpClientError';
    this.kind = options.kind;
    this.status = options.status;
    this.code = options.code;
    this.traceId = options.traceId;
  }
}

export function isHttpClientError(error: unknown): error is HttpClientError {
  return error instanceof HttpClientError;
}

export function createTraceId(): string {
  if (globalThis.crypto && 'randomUUID' in globalThis.crypto) {
    return globalThis.crypto.randomUUID();
  }

  return `trace-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function resolveUrl(path: string): string {
  if (typeof window !== 'undefined') {
    return path;
  }

  const apiBaseUrl = (process.env.LOCALTALENT_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '');
  return `${apiBaseUrl}${path}`;
}

function errorKind(status: number): HttpClientErrorKind {
  return status === 401 || status === 403 ? 'unauthorized' : 'error';
}

async function parseJson<T>(response: Response): Promise<ApiResponse<T> | null> {
  const text = await response.text();
  if (!text) {
    return null;
  }

  return JSON.parse(text) as ApiResponse<T>;
}

export async function apiGet<T>(path: string, init?: { traceId?: string; signal?: AbortSignal }): Promise<ApiResult<T>> {
  const traceId = init?.traceId ?? createTraceId();
  const headers = new Headers();
  headers.set('X-Trace-Id', traceId);
  headers.set('Accept', 'application/json');

  let response: Response;

  try {
    response = await fetch(resolveUrl(path), {
      method: 'GET',
      headers,
      signal: init?.signal,
      cache: 'no-store'
    });
  } catch (error) {
    throw new HttpClientError(error instanceof Error ? error.message : '网络请求失败', {
      kind: 'error',
      status: 0,
      code: 'NETWORK_ERROR',
      traceId
    });
  }

  const payload = await parseJson<T>(response);
  const responseTraceId = payload?.trace_id ?? response.headers.get('X-Trace-Id') ?? traceId;

  if (!response.ok || !payload || payload.code !== '0') {
    throw new HttpClientError(payload?.message ?? '请求失败，请稍后重试。', {
      kind: errorKind(response.status),
      status: response.status,
      code: payload?.code ?? `HTTP_${response.status}`,
      traceId: responseTraceId
    });
  }

  return {
    data: payload.data,
    traceId: responseTraceId
  };
}
