import {
  apiRequest,
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

export type CandidatePrivateStats = {
  favorite_count: number;
  subscription_count: number;
  unread_notification_count: number;
};

export type CandidateCenterFeatures = {
  candidate_closure_enabled: boolean;
};

export type CandidateOnboardingState = {
  onboarding_required: boolean;
  onboarding_step: 'basic' | 'detail' | 'center' | string;
  publish_status: CandidatePublishStatus;
};

export type CandidateCenterOverview = {
  resume: CandidateResumeSummary;
  applications: CandidateApplicationSummary;
  signin: CandidateSigninSummary;
  consent: CandidateConsentSummary;
  stats: CandidatePrivateStats;
  features: CandidateCenterFeatures;
  onboarding?: CandidateOnboardingState;
};

export type CandidateWorkExperience = {
  company_name: string;
  position_name: string;
  start_date: string;
  end_date: string;
  ongoing: boolean;
  responsibility: string;
};

export type CandidateEducationExperience = {
  school_name: string;
  major_name: string;
  start_date: string;
  end_date: string;
  ongoing: boolean;
  degree: string;
};

export type CandidateResume = {
  resume_id: number | null;
  resume_status: string;
  completion_percent: number;
  updated_at: string;
  resume_name: string;
  base_profile: {
    display_name: string;
    city_code: string;
    category_code: string;
    experience_years: number | null;
    summary: string;
    gender: string;
    birth_date: string;
    highest_education: string;
    start_work_date: string;
    no_experience: boolean;
    contact_phone: string;
    contact_wechat: string;
    wechat_same_as_phone: boolean;
    expected_positions: string[];
    expected_salary: string;
    expected_cities: string[];
    job_status: string;
  };
  education: string[];
  experience: string[];
  skills: string[];
  work_experience: CandidateWorkExperience[];
  education_experience: CandidateEducationExperience[];
  self_description: string;
  has_attachment: boolean;
};

export type CandidateApplicationItem = {
  application_id: number;
  job_id: number;
  job_title: string;
  company_name: string;
  application_status: number;
  status_label: string;
  apply_time: string;
};

export type CandidateFavoriteItem = {
  favorite_id: number;
  job_id: number;
  job_title: string;
  company_name: string;
  city_code: string;
  category_code: string;
  favorite_status: string;
  created_at: string;
};

export type CandidateSubscriptionItem = {
  subscription_id: number;
  subscription_name: string;
  keyword: string;
  city_code: string;
  category_code: string;
  salary_min: number | null;
  salary_max: number | null;
  subscription_status: string;
  updated_at: string;
};

export type CandidateNotificationItem = {
  notification_id: number;
  notification_type: string;
  title: string;
  content_summary: string;
  read_status: string;
  created_at: string;
};

export type CandidateClosureData = {
  resume: CandidateResume;
  preview: CandidateResume;
  applications: CandidateApplicationItem[];
  favorites: CandidateFavoriteItem[];
  subscriptions: CandidateSubscriptionItem[];
  notifications: CandidateNotificationItem[];
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

function arrayOfText(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((item): item is string => typeof item === 'string') : [];
}

function arrayOfRecord(value: unknown): RawRecord[] {
  return Array.isArray(value)
    ? value.map((item) => asRecord(item))
    : [];
}

function booleanOr(value: unknown, fallback = false): boolean {
  return typeof value === 'boolean' ? value : fallback;
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
    },
    stats: {
      favorite_count: numberOr(asRecord(payload.stats).favorite_count, 0),
      subscription_count: numberOr(asRecord(payload.stats).subscription_count, 0),
      unread_notification_count: numberOr(asRecord(payload.stats).unread_notification_count, 0)
    },
    features: {
      candidate_closure_enabled: Boolean(asRecord(payload.features).candidate_closure_enabled)
    },
    onboarding: {
      onboarding_required: Boolean(asRecord(payload.onboarding).onboarding_required),
      onboarding_step: text(asRecord(payload.onboarding).onboarding_step, 'center'),
      publish_status: publishStatus(asRecord(payload.onboarding).publish_status)
    }
  };
}

function toWorkExperienceList(raw: unknown): CandidateWorkExperience[] {
  return arrayOfRecord(raw).map((item) => ({
    company_name: text(item.company_name),
    position_name: text(item.position_name),
    start_date: text(item.start_date),
    end_date: text(item.end_date),
    ongoing: booleanOr(item.ongoing),
    responsibility: text(item.responsibility)
  }));
}

function toEducationExperienceList(raw: unknown): CandidateEducationExperience[] {
  return arrayOfRecord(raw).map((item) => ({
    school_name: text(item.school_name),
    major_name: text(item.major_name),
    start_date: text(item.start_date),
    end_date: text(item.end_date),
    ongoing: booleanOr(item.ongoing),
    degree: text(item.degree)
  }));
}

function toCandidateResume(raw: unknown): CandidateResume {
  const payload = asRecord(raw);
  const baseProfile = asRecord(payload.base_profile);

  return {
    resume_id: nullableNumber(payload.resume_id),
    resume_status: text(payload.resume_status, 'draft'),
    completion_percent: numberOr(payload.completion_percent, 0),
    updated_at: text(payload.updated_at),
    resume_name: text(payload.resume_name, '我的简历'),
    base_profile: {
      display_name: text(baseProfile.display_name),
      city_code: text(baseProfile.city_code),
      category_code: text(baseProfile.category_code),
      experience_years: nullableNumber(baseProfile.experience_years),
      summary: text(baseProfile.summary),
      gender: text(baseProfile.gender),
      birth_date: text(baseProfile.birth_date),
      highest_education: text(baseProfile.highest_education),
      start_work_date: text(baseProfile.start_work_date),
      no_experience: booleanOr(baseProfile.no_experience),
      contact_phone: text(baseProfile.contact_phone),
      contact_wechat: text(baseProfile.contact_wechat),
      wechat_same_as_phone: booleanOr(baseProfile.wechat_same_as_phone),
      expected_positions: arrayOfText(baseProfile.expected_positions),
      expected_salary: text(baseProfile.expected_salary),
      expected_cities: arrayOfText(baseProfile.expected_cities),
      job_status: text(baseProfile.job_status)
    },
    education: arrayOfText(payload.education),
    experience: arrayOfText(payload.experience),
    skills: arrayOfText(payload.skills),
    work_experience: toWorkExperienceList(payload.work_experience),
    education_experience: toEducationExperienceList(payload.education_experience),
    self_description: text(payload.self_description),
    has_attachment: Boolean(payload.has_attachment)
  };
}

function toApplicationList(raw: unknown): CandidateApplicationItem[] {
  return arrayOfRecord(asRecord(raw).application_list).map((item) => ({
    application_id: numberOr(item.application_id, 0),
    job_id: numberOr(item.job_id, 0),
    job_title: text(item.job_title, '职位'),
    company_name: text(item.company_name, '认证企业'),
    application_status: numberOr(item.application_status, 0),
    status_label: text(item.status_label, '已投递'),
    apply_time: text(item.apply_time)
  }));
}

function toFavoriteList(raw: unknown): CandidateFavoriteItem[] {
  return arrayOfRecord(asRecord(raw).favorite_list).map((item) => ({
    favorite_id: numberOr(item.favorite_id, 0),
    job_id: numberOr(item.job_id, 0),
    job_title: text(item.job_title, '职位'),
    company_name: text(item.company_name, '认证企业'),
    city_code: text(item.city_code),
    category_code: text(item.category_code),
    favorite_status: text(item.favorite_status, 'active'),
    created_at: text(item.created_at)
  }));
}

function toSubscriptionList(raw: unknown): CandidateSubscriptionItem[] {
  return arrayOfRecord(asRecord(raw).subscription_list).map((item) => ({
    subscription_id: numberOr(item.subscription_id, 0),
    subscription_name: text(item.subscription_name, '职位订阅'),
    keyword: text(item.keyword),
    city_code: text(item.city_code),
    category_code: text(item.category_code),
    salary_min: nullableNumber(item.salary_min),
    salary_max: nullableNumber(item.salary_max),
    subscription_status: text(item.subscription_status, 'active'),
    updated_at: text(item.updated_at)
  }));
}

function toNotificationList(raw: unknown): CandidateNotificationItem[] {
  return arrayOfRecord(asRecord(raw).notification_list).map((item) => ({
    notification_id: numberOr(item.notification_id, 0),
    notification_type: text(item.notification_type, 'system'),
    title: text(item.title, '站内通知'),
    content_summary: text(item.content_summary),
    read_status: text(item.read_status, 'unread'),
    created_at: text(item.created_at)
  }));
}

export async function fetchCandidateCenterOverview(token: string): Promise<ApiResult<CandidateCenterOverview>> {
  const result = await apiGet<unknown>('/api/candidate/center/overview', { token });

  return {
    data: toCandidateCenterOverview(result.data),
    traceId: result.traceId
  };
}

export async function fetchCandidateClosureData(token: string): Promise<CandidateClosureData> {
  const [resume, preview, applications, favorites, subscriptions, notifications] = await Promise.all([
    apiGet<unknown>('/api/candidate/center/resume', { token }),
    apiGet<unknown>('/api/candidate/center/resume/preview', { token }),
    apiGet<unknown>('/api/candidate/center/applications?page=1&size=20', { token }),
    apiGet<unknown>('/api/candidate/center/favorites?page=1&size=20', { token }),
    apiGet<unknown>('/api/candidate/center/subscriptions?page=1&size=20', { token }),
    apiGet<unknown>('/api/candidate/center/notifications?page=1&size=20', { token })
  ]);

  return {
    resume: toCandidateResume(resume.data),
    preview: toCandidateResume(preview.data),
    applications: toApplicationList(applications.data),
    favorites: toFavoriteList(favorites.data),
    subscriptions: toSubscriptionList(subscriptions.data),
    notifications: toNotificationList(notifications.data)
  };
}

export async function saveCandidateResume(
  token: string,
  resume: {
    resume_name: string;
    base_profile: CandidateResume['base_profile'];
    education: string[];
    experience: string[];
    skills: string[];
    work_experience?: CandidateWorkExperience[];
    education_experience?: CandidateEducationExperience[];
    self_description?: string;
  }
): Promise<ApiResult<CandidateResume>> {
  const result = await apiRequest<unknown>('/api/candidate/center/resume', {
    method: 'PUT',
    token,
    body: resume,
    idempotencyKey: createIdempotencyKey('candidate-resume')
  });

  return {
    data: toCandidateResume(result.data),
    traceId: result.traceId
  };
}

export async function createFavorite(token: string, jobId: number): Promise<ApiResult<unknown>> {
  return apiPost('/api/candidate/center/favorites', { job_id: jobId }, {
    token,
    idempotencyKey: createIdempotencyKey('candidate-favorite')
  });
}

export async function cancelFavorite(token: string, favoriteId: number): Promise<ApiResult<unknown>> {
  return apiPost(`/api/candidate/center/favorites/${favoriteId}/cancel`, {}, {
    token,
    idempotencyKey: createIdempotencyKey('candidate-favorite-cancel')
  });
}

export async function createSubscription(
  token: string,
  payload: {
    subscription_name: string;
    keyword: string;
    city_code: string;
    category_code: string;
  }
): Promise<ApiResult<unknown>> {
  return apiPost('/api/candidate/center/subscriptions', payload, {
    token,
    idempotencyKey: createIdempotencyKey('candidate-subscription')
  });
}

export async function cancelSubscription(token: string, subscriptionId: number): Promise<ApiResult<unknown>> {
  return apiPost(`/api/candidate/center/subscriptions/${subscriptionId}/cancel`, {}, {
    token,
    idempotencyKey: createIdempotencyKey('candidate-subscription-cancel')
  });
}

export async function markNotificationRead(token: string, notificationId: number): Promise<ApiResult<unknown>> {
  return apiPost(`/api/candidate/center/notifications/${notificationId}/read`, {}, {
    token,
    idempotencyKey: createIdempotencyKey('candidate-notification-read')
  });
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
