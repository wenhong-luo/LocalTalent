import {
  apiGet,
  apiPost,
  createIdempotencyKey,
  type ApiResult
} from '@/lib/httpClient';

export const CANDIDATE_TOKEN_STORAGE_KEY = 'localtalent_access_token';

export type CandidatePublishStatus =
  | 'consented'
  | 'revoked'
  | 'not_publishable'
  | 'unauthorized'
  | 'unavailable';

export type CandidateResumeSummary = {
  completion_percent: number;
  updated_at: string;
  skills_summary: string;
};

export type CandidateApplicationSummary = {
  total: number;
  latest_status: string;
  latest_job_title: string;
};

export type CandidateSigninSummary = {
  latest_status: string;
  latest_time: string;
};

export type CandidateConsentSummary = {
  consent_id: number | null;
  publish_status: CandidatePublishStatus;
  publishable_flag: number;
  status_label: string;
  reason: string;
  updated_at: string;
};

export type CandidateCenterOverview = {
  resume: CandidateResumeSummary;
  applications: CandidateApplicationSummary;
  signin: CandidateSigninSummary;
  consent: CandidateConsentSummary;
};

const publishStatuses: CandidatePublishStatus[] = [
  'consented',
  'revoked',
  'not_publishable',
  'unauthorized',
  'unavailable'
];

type RawRecord = Record<string, unknown>;

export function readCandidateToken(): string | null {
  if (typeof window === 'undefined') {
    return null;
  }

  const token = window.localStorage.getItem(CANDIDATE_TOKEN_STORAGE_KEY);
  return token && token.trim() ? token.trim() : null;
}

function asRecord(value: unknown): RawRecord {
  return value && typeof value === 'object' ? value as RawRecord : {};
}

function text(value: unknown, fallback = ''): string {
  return typeof value === 'string' ? value : fallback;
}

function numberOr(value: unknown, fallback: number): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : fallback;
}

function nullableNumber(value: unknown): number | null {
  return typeof value === 'number' && Number.isFinite(value) ? value : null;
}

function publishStatus(value: unknown): CandidatePublishStatus {
  return publishStatuses.includes(value as CandidatePublishStatus)
    ? value as CandidatePublishStatus
    : 'unavailable';
}

export function toCandidateCenterOverview(raw: unknown): CandidateCenterOverview {
  const payload = asRecord(raw);
  const resume = asRecord(payload.resume);
  const applications = asRecord(payload.applications);
  const signin = asRecord(payload.signin);
  const consent = asRecord(payload.consent);

  return {
    resume: {
      completion_percent: numberOr(resume.completion_percent, 0),
      updated_at: text(resume.updated_at),
      skills_summary: text(resume.skills_summary)
    },
    applications: {
      total: numberOr(applications.total, 0),
      latest_status: text(applications.latest_status, '暂无投递'),
      latest_job_title: text(applications.latest_job_title, '暂无')
    },
    signin: {
      latest_status: text(signin.latest_status, '暂无签到'),
      latest_time: text(signin.latest_time)
    },
    consent: {
      consent_id: nullableNumber(consent.consent_id),
      publish_status: publishStatus(consent.publish_status),
      publishable_flag: numberOr(consent.publishable_flag, 0),
      status_label: text(consent.status_label, '状态暂不可用'),
      reason: text(consent.reason),
      updated_at: text(consent.updated_at)
    }
  };
}

export async function fetchCandidateCenterOverview(token: string): Promise<ApiResult<CandidateCenterOverview>> {
  const result = await apiGet<unknown>('/api/candidate/center/overview', { token });

  return {
    data: toCandidateCenterOverview(result.data),
    traceId: result.traceId
  };
}

export async function submitCandidateConsent(token: string): Promise<ApiResult<unknown>> {
  return apiPost(
    '/api/consents',
    {
      consent_scope: ['talent_service_area'],
      consent_version: 'phase1-v1',
      realname_verified: true,
      second_confirmed: true
    },
    {
      token,
      idempotencyKey: createIdempotencyKey('candidate-consent')
    }
  );
}

export async function revokeCandidateConsent(
  token: string,
  consentId: number,
  reason = 'candidate self revoke from center'
): Promise<ApiResult<unknown>> {
  return apiPost(
    `/api/consents/${consentId}/revoke`,
    { reason },
    {
      token,
      idempotencyKey: createIdempotencyKey('candidate-revoke')
    }
  );
}
