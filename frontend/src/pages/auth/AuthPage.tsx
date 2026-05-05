'use client';

import Link from 'next/link';
import { FormEvent, useEffect, useMemo, useState } from 'react';
import { isHttpClientError } from '@/lib/httpClient';
import {
  type AuthMode,
  type AuthRole,
  destinationForIdentity,
  fetchOidcConfig,
  login,
  oidcLoginUrl,
  registerCandidate,
  registerCompany,
  saveAccessToken,
  type OidcConfig
} from './authApi';
import styles from './AuthPage.module.css';

type AuthPageProps = {
  mode: AuthMode;
  initialRole?: AuthRole;
  redirect?: string;
  oidcError?: string;
  oidcTraceId?: string;
  onNavigate?: (href: string) => void;
};

type FormState = {
  account: string;
  email: string;
  mobile: string;
  password: string;
  displayName: string;
  companyName: string;
  licenseNo: string;
  userName: string;
};

const initialForm: FormState = {
  account: '',
  email: '',
  mobile: '',
  password: '',
  displayName: '',
  companyName: '',
  licenseNo: '',
  userName: ''
};

const defaultOidcConfig: OidcConfig = {
  oidc_enabled: false,
  local_fallback_enabled: true,
  login_url: '/api/auth/oidc/login',
  logout_url: '/api/auth/logout'
};

function normalizeRole(role: AuthRole | undefined, mode: AuthMode): AuthRole {
  if (mode === 'login' && role === 'operator') {
    return 'operator';
  }
  return role === 'company' ? 'company' : 'candidate';
}

function defaultNavigate(href: string): void {
  window.location.assign(href);
}

function roleLabel(role: AuthRole): string {
  if (role === 'operator') {
    return '运营';
  }
  return role === 'company' ? '招聘者' : '求职者';
}

function modeLabel(mode: AuthMode): string {
  return mode === 'register' ? '注册' : '登录';
}

function loginHref(role: AuthRole): string {
  return `/auth/login?role=${role}`;
}

function registerHref(role: AuthRole): string {
  return `/auth/register?role=${role === 'operator' ? 'candidate' : role}`;
}

function ExternalLoginPlaceholders() {
  return (
    <section className={styles.placeholderBox} aria-label="外部登录占位">
      <p className={styles.placeholderTitle}>外部登录能力暂不接入</p>
      <div className={styles.placeholderGrid}>
        {['短信验证码占位', '微信扫码占位', '小程序占位', 'App 登录占位'].map((label) => (
          <button key={label} className={styles.disabledOption} type="button" disabled aria-disabled="true">
            {label}
          </button>
        ))}
      </div>
    </section>
  );
}

export function AuthPage({
  mode,
  initialRole,
  redirect,
  oidcError,
  oidcTraceId,
  onNavigate = defaultNavigate
}: AuthPageProps) {
  const [role, setRole] = useState<AuthRole>(normalizeRole(initialRole, mode));
  const [form, setForm] = useState<FormState>(initialForm);
  const [oidcConfig, setOidcConfig] = useState<OidcConfig>(defaultOidcConfig);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<string>();
  const [error, setError] = useState<string | undefined>(
    oidcError ? `SSO 登录失败：${oidcError}${oidcTraceId ? ` trace_id：${oidcTraceId}` : ''}` : undefined
  );

  const title = useMemo(() => `${roleLabel(role)}${modeLabel(mode)}`, [mode, role]);

  useEffect(() => {
    let mounted = true;
    fetchOidcConfig()
      .then((result) => {
        if (mounted) {
          setOidcConfig(result.data);
        }
      })
      .catch(() => {
        if (mounted) {
          setOidcConfig(defaultOidcConfig);
        }
      });
    return () => {
      mounted = false;
    };
  }, []);

  function updateField(field: keyof FormState, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function switchRole(nextRole: AuthRole) {
    setRole(normalizeRole(nextRole, mode));
    setMessage(undefined);
    setError(undefined);
  }

  function describeError(errorValue: unknown): string {
    if (isHttpClientError(errorValue)) {
      return `${errorValue.message}${errorValue.traceId ? ` trace_id：${errorValue.traceId}` : ''}`;
    }

    return errorValue instanceof Error ? errorValue.message : '请求失败，请稍后重试。';
  }

  async function submitLogin() {
    const result = await login({
      identity_type: role,
      account: form.account.trim(),
      password: form.password
    });
    const identityType = result.data.identity.identity_type;
    saveAccessToken(result.data.access_token, identityType, result.data.identity.role_codes);
    onNavigate(destinationForIdentity(identityType, redirect));
  }

  async function submitRegister() {
    const result = role === 'candidate'
      ? await registerCandidate({
        email: form.email.trim() || undefined,
        mobile: form.mobile.trim() || undefined,
        password: form.password,
        display_name: form.displayName.trim() || undefined
      })
      : await registerCompany({
        company_name: form.companyName.trim(),
        license_no: form.licenseNo.trim(),
        user_name: form.userName.trim(),
        email: form.email.trim() || undefined,
        mobile: form.mobile.trim() || undefined,
        password: form.password
      });

    if (result.data.access_token) {
      const identityType = result.data.identity.identity_type;
      saveAccessToken(result.data.access_token, identityType, result.data.identity.role_codes);
      onNavigate(destinationForIdentity(identityType, redirect));
      return;
    }

    setMessage(`${roleLabel(role)}注册成功，请继续登录。trace_id：${result.traceId}`);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (mode === 'login' && !oidcConfig.local_fallback_enabled) {
      setError('本地账号 fallback 当前已关闭，请使用 SSO 登录。');
      return;
    }
    setSubmitting(true);
    setMessage(undefined);
    setError(undefined);

    try {
      if (mode === 'login') {
        await submitLogin();
      } else {
        await submitRegister();
      }
    } catch (errorValue) {
      setError(describeError(errorValue));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main className={styles.page}>
      <div className={styles.shell}>
        <section className={styles.panel} aria-label="登录注册说明">
          <p className={styles.eyebrow}>LocalTalent Account</p>
          <h1 className={styles.title}>登录注册只是入口，权限仍由服务端强制。</h1>
          <p className={styles.description}>
            三期支持标准 OIDC/SSO 认证入口；本地账号与 JWT 仅作为 dev/test fallback。
            真实短信、微信扫码、小程序和 App 登录均保持禁用占位，不接外部服务。
          </p>
          <div className={styles.featureGrid}>
            <article className={styles.featureCard}>
              <span className={styles.featureIcon}>求</span>
              求职者进入求职者中心。
            </article>
            <article className={styles.featureCard}>
              <span className={styles.featureIcon}>企</span>
              招聘者进入企业中心。
            </article>
            <article className={styles.featureCard}>
              <span className={styles.featureIcon}>权</span>
              无权限态继续由后端校验。
            </article>
          </div>
        </section>

        <section className={styles.formCard} aria-label={title}>
          <div className={styles.tabs} role="tablist" aria-label="账号身份">
            <button
              className={role === 'candidate' ? styles.tabActive : styles.tab}
              type="button"
              role="tab"
              aria-selected={role === 'candidate'}
              onClick={() => switchRole('candidate')}
            >
              求职者{modeLabel(mode)}
            </button>
            <button
              className={role === 'company' ? styles.tabActive : styles.tab}
              type="button"
              role="tab"
              aria-selected={role === 'company'}
              onClick={() => switchRole('company')}
            >
              招聘者{modeLabel(mode)}
            </button>
            {mode === 'login' ? (
              <button
                className={role === 'operator' ? styles.tabActive : styles.tab}
                type="button"
                role="tab"
                aria-selected={role === 'operator'}
                onClick={() => switchRole('operator')}
              >
                运营登录
              </button>
            ) : null}
          </div>

          {mode === 'login' && oidcConfig.oidc_enabled ? (
            <button
              className={styles.ssoButton}
              type="button"
              onClick={() => onNavigate(oidcLoginUrl(role, redirect))}
            >
              使用 SSO 登录{roleLabel(role)}入口
            </button>
          ) : null}

          {mode === 'login' && !oidcConfig.local_fallback_enabled ? (
            <div className={styles.fallbackNotice}>
              本地账号 fallback 当前已关闭。gray/prod 环境必须使用 SSO，白名单例外仍由后端控制。
            </div>
          ) : null}

          <form className={styles.form} onSubmit={handleSubmit}>
            {mode === 'login' ? (
              <>
                <label className={styles.field}>
                  账号
                  <input
                    className={styles.input}
                    name="account"
                    value={form.account}
                    onChange={(event) => updateField('account', event.target.value)}
                    placeholder="请输入邮箱、手机号或账号"
                    required
                  />
                </label>
              </>
            ) : role === 'candidate' ? (
              <>
                <label className={styles.field}>
                  邮箱
                  <input
                    className={styles.input}
                    name="email"
                    type="email"
                    value={form.email}
                    onChange={(event) => updateField('email', event.target.value)}
                    placeholder="candidate@example.com"
                    required
                  />
                </label>
                <label className={styles.field}>
                  昵称
                  <input
                    className={styles.input}
                    name="display_name"
                    value={form.displayName}
                    onChange={(event) => updateField('displayName', event.target.value)}
                    placeholder="用于求职者中心展示"
                  />
                </label>
              </>
            ) : (
              <>
                <label className={styles.field}>
                  企业名称
                  <input
                    className={styles.input}
                    name="company_name"
                    value={form.companyName}
                    onChange={(event) => updateField('companyName', event.target.value)}
                    required
                  />
                </label>
                <label className={styles.field}>
                  统一信用代码占位
                  <input
                    className={styles.input}
                    name="license_no"
                    value={form.licenseNo}
                    onChange={(event) => updateField('licenseNo', event.target.value)}
                    required
                  />
                </label>
                <label className={styles.field}>
                  联系人姓名
                  <input
                    className={styles.input}
                    name="user_name"
                    value={form.userName}
                    onChange={(event) => updateField('userName', event.target.value)}
                    required
                  />
                </label>
                <label className={styles.field}>
                  邮箱
                  <input
                    className={styles.input}
                    name="email"
                    type="email"
                    value={form.email}
                    onChange={(event) => updateField('email', event.target.value)}
                    required
                  />
                </label>
              </>
            )}

            <label className={styles.field}>
              密码
              <input
                className={styles.input}
                name="password"
                type="password"
                value={form.password}
                onChange={(event) => updateField('password', event.target.value)}
                placeholder="请输入本地账号密码"
                required
              />
            </label>

            {message ? <div className={styles.message}>{message}</div> : null}
            {error ? <div className={styles.error}>{error}</div> : null}

            <button className={styles.submit} type="submit" disabled={submitting}>
              {submitting ? '提交中...' : `立即${title}`}
            </button>
          </form>

          <p className={styles.switchLine}>
            {mode === 'login' ? '还没有账号？' : '已有账号？'}
            {' '}
            <Link href={mode === 'login' ? registerHref(role) : loginHref(role)}>
              {mode === 'login' ? '立即注册' : '去登录'}
            </Link>
          </p>

          <ExternalLoginPlaceholders />
        </section>
      </div>
    </main>
  );
}
