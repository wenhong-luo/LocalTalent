import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { AuthPage } from '@/pages/auth/AuthPage';
import type { AuthRole } from '@/pages/auth/authApi';

export const metadata: Metadata = {
  title: '会员注册',
  description: 'LocalTalent 求职者与招聘者本地账号注册入口，真实短信和第三方注册保持禁用占位。',
  robots: {
    index: false,
    follow: false
  }
};

type SearchParams = Record<string, string | string[] | undefined>;

function roleFromSearchParams(searchParams: SearchParams): AuthRole {
  return searchParams.role === 'company' ? 'company' : 'candidate';
}

export default async function RegisterPage({ searchParams }: { searchParams?: Promise<SearchParams> }) {
  const resolvedSearchParams = (await searchParams) ?? {};

  return (
    <PortalShell>
      <AuthPage mode="register" initialRole={roleFromSearchParams(resolvedSearchParams)} />
    </PortalShell>
  );
}
