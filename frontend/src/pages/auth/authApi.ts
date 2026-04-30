import { apiPost, type ApiResult } from '@/lib/httpClient';
import { ACCESS_TOKEN_STORAGE_KEY, ADMIN_ROLE_HINT_STORAGE_KEY, type AuthIdentity } from '@/pages/backoffice/session';

export type AuthRole = 'candidate' | 'company';
export type AuthMode = 'login' | 'register';

export type LoginPayload = {
  identity_type: AuthRole;
  account: string;
  password: string;
};

export type RegisterCandidatePayload = {
  identity_type: 'candidate';
  email?: string;
  mobile?: string;
  password: string;
  display_name?: string;
};

export type RegisterCompanyPayload = {
  identity_type: 'company';
  company_name: string;
  license_no: string;
  user_name: string;
  email?: string;
  mobile?: string;
  password: string;
};

export type LoginResponse = {
  access_token: string;
  token_type: string;
  expires_in: number;
  identity: AuthIdentity;
};

export type RegisterResponse = {
  identity: AuthIdentity;
  access_token?: string;
  token_type?: string;
  expires_in?: number;
};

export function login(payload: LoginPayload): Promise<ApiResult<LoginResponse>> {
  return apiPost<LoginResponse>('/api/auth/login', payload);
}

export function registerCandidate(payload: Omit<RegisterCandidatePayload, 'identity_type'>): Promise<ApiResult<RegisterResponse>> {
  return apiPost<RegisterResponse>('/api/auth/register', {
    identity_type: 'candidate',
    ...payload
  });
}

export function registerCompany(payload: Omit<RegisterCompanyPayload, 'identity_type'>): Promise<ApiResult<RegisterResponse>> {
  return apiPost<RegisterResponse>('/api/auth/register', {
    identity_type: 'company',
    ...payload
  });
}

export function saveAccessToken(token: string, identityType?: string): void {
  if (typeof window === 'undefined') {
    return;
  }

  window.localStorage.setItem(ACCESS_TOKEN_STORAGE_KEY, token);

  if (identityType === 'operator') {
    window.localStorage.setItem(ADMIN_ROLE_HINT_STORAGE_KEY, 'operator');
    return;
  }

  window.localStorage.removeItem(ADMIN_ROLE_HINT_STORAGE_KEY);
}

function isSafeInternalPath(path: string): boolean {
  return path.startsWith('/') && !path.startsWith('//') && !path.includes('://');
}

export function destinationForIdentity(identityType: string, redirect?: string): string {
  const fallback = identityType === 'company'
    ? '/company'
    : identityType === 'operator'
      ? '/admin'
      : '/candidate/center';

  if (!redirect || !isSafeInternalPath(redirect)) {
    return fallback;
  }

  if (identityType === 'company' && redirect.startsWith('/company')) {
    return redirect;
  }

  if (identityType === 'operator' && redirect.startsWith('/admin')) {
    return redirect;
  }

  if (identityType === 'candidate' && redirect.startsWith('/candidate')) {
    return redirect;
  }

  return fallback;
}
