import { apiGet, type ApiResult } from '@/lib/httpClient';

export const ACCESS_TOKEN_STORAGE_KEY = 'localtalent_access_token';
export const ADMIN_ROLE_HINT_STORAGE_KEY = 'localtalent_admin_role_hint';

export type IdentityType = 'candidate' | 'company' | 'operator';
export type AdminRoleHint = 'operator' | 'auditor';

export type AuthIdentity = {
  identity_type: IdentityType | string;
  user_id: number;
  company_id?: number | null;
  display_name?: string;
  status?: number | null;
  role_codes?: string[];
};

export function readAccessToken(): string | null {
  if (typeof window === 'undefined') {
    return null;
  }

  const token = window.localStorage.getItem(ACCESS_TOKEN_STORAGE_KEY);
  return token && token.trim() ? token.trim() : null;
}

export function readAdminRoleHint(): AdminRoleHint | null {
  if (typeof window === 'undefined') {
    return null;
  }

  const roleHint = window.localStorage.getItem(ADMIN_ROLE_HINT_STORAGE_KEY);
  return roleHint === 'operator' || roleHint === 'auditor' ? roleHint : null;
}

export async function fetchCurrentIdentity(token: string): Promise<ApiResult<AuthIdentity>> {
  return apiGet<AuthIdentity>('/api/auth/me', { token });
}
