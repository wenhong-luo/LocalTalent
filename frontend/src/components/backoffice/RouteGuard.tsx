'use client';

import { type ReactNode, useEffect, useState } from 'react';
import { StateView, type StateVariant } from '@/components/StateView';
import { isHttpClientError } from '@/lib/httpClient';
import {
  fetchCurrentIdentity,
  readAccessToken,
  readAdminRoleHint,
  type AdminRoleHint,
  type AuthIdentity,
  type IdentityType
} from '@/pages/backoffice/session';

type GuardStatus = 'loading' | 'ready' | 'unauthorized' | 'error' | 'retrying';

export type GuardContext = {
  token: string;
  identity: AuthIdentity;
  adminRoleHint: AdminRoleHint | null;
};

export type RouteGuardProps = {
  allowedIdentities: IdentityType[];
  title: string;
  children: (context: GuardContext) => ReactNode;
};

function isAllowed(identity: AuthIdentity, allowedIdentities: IdentityType[]): boolean {
  return allowedIdentities.includes(identity.identity_type as IdentityType);
}

function stateVariant(status: GuardStatus): StateVariant {
  return status === 'ready' ? 'loading' : status;
}

export function RouteGuard({ allowedIdentities, title, children }: RouteGuardProps) {
  const [status, setStatus] = useState<GuardStatus>('loading');
  const [context, setContext] = useState<GuardContext | null>(null);
  const [message, setMessage] = useState<string>('正在校验当前账号权限。');
  const [traceId, setTraceId] = useState<string>();

  async function load(nextStatus: GuardStatus = 'retrying') {
    const token = readAccessToken();
    const adminRoleHint = readAdminRoleHint();

    if (!token) {
      setContext(null);
      setStatus('unauthorized');
      setMessage('请先登录后再访问该后台页面。');
      return;
    }

    setStatus(nextStatus);
    setMessage('正在校验当前账号权限。');

    try {
      const result = await fetchCurrentIdentity(token);
      setTraceId(result.traceId);

      if (!isAllowed(result.data, allowedIdentities)) {
        setContext(null);
        setStatus('unauthorized');
        setMessage('无权限查看：当前账号身份不允许访问该后台页面。');
        return;
      }

      setContext({ token, identity: result.data, adminRoleHint });
      setStatus('ready');
      setMessage('');
    } catch (error) {
      setTraceId(isHttpClientError(error) ? error.traceId : undefined);
      setContext(null);
      setStatus(isHttpClientError(error) && error.kind === 'unauthorized' ? 'unauthorized' : 'error');
      setMessage(error instanceof Error ? error.message : '权限校验失败，请稍后重试。');
    }
  }

  useEffect(() => {
    void load('loading');
  }, []);

  if (status === 'ready' && context) {
    return <>{children(context)}</>;
  }

  return (
    <main style={{ minHeight: '100vh', padding: '32px 18px' }}>
      <section style={{ maxWidth: '960px', margin: '0 auto' }}>
        <StateView
          variant={stateVariant(status)}
          title={status === 'unauthorized' ? '无权限查看' : title}
          description={traceId ? `${message} trace_id：${traceId}` : message}
          retryLabel="重新校验"
          onRetry={status === 'loading' || status === 'retrying' ? undefined : () => load('retrying')}
        />
      </section>
    </main>
  );
}
