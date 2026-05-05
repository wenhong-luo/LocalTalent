import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { AuthPage } from '@/pages/auth/AuthPage';
import type { AuthRole } from '@/pages/auth/authApi';

export const metadata: Metadata = {
  title: '会员登录',
  description: 'LocalTalent 本地账号登录入口，真实短信、微信、小程序和 App 登录均为禁用占位。',
  robots: {
    index: false,
    follow: false
  }
};

type SearchParams = Record<string, string | string[] | undefined>;

function roleFromSearchParams(searchParams: SearchParams): AuthRole {
  if (searchParams.role === 'company' || searchParams.role === 'operator') {
    return searchParams.role;
  }
  return 'candidate';
}

function stringParam(value: string | string[] | undefined): string | undefined {
  return typeof value === 'string' ? value : undefined;
}

export default async function LoginPage({ searchParams }: { searchParams?: Promise<SearchParams> }) {
  const resolvedSearchParams = (await searchParams) ?? {};

  return (
    <PortalShell>
      <AuthPage
        mode="login"
        initialRole={roleFromSearchParams(resolvedSearchParams)}
        redirect={stringParam(resolvedSearchParams.redirect)}
        oidcError={stringParam(resolvedSearchParams.oidc_error)}
        oidcTraceId={stringParam(resolvedSearchParams.trace_id)}
      />
    </PortalShell>
  );
}
