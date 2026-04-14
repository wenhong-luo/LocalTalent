import { type CSSProperties } from 'react';

export type StateVariant = 'loading' | 'error' | 'forbidden' | 'retrying';

export type StateViewProps = {
  variant: StateVariant;
  title?: string;
  description?: string;
  retryLabel?: string;
  onRetry?: () => void;
};

const palette: Record<StateVariant, { accent: string; background: string; badge: string }> = {
  loading: {
    accent: '#2563eb',
    background: '#eef4ff',
    badge: '加载中'
  },
  error: {
    accent: '#dc2626',
    background: '#fff1f2',
    badge: '错误'
  },
  forbidden: {
    accent: '#7c3aed',
    background: '#f5f3ff',
    badge: '无权限'
  },
  retrying: {
    accent: '#d97706',
    background: '#fff7ed',
    badge: '重试中'
  }
};

const cardStyle: CSSProperties = {
  borderRadius: '20px',
  border: '1px solid #d9e1f2',
  padding: '20px',
  minHeight: '188px',
  display: 'flex',
  flexDirection: 'column',
  justifyContent: 'space-between'
};

const defaultCopy: Record<StateVariant, { title: string; description: string }> = {
  loading: {
    title: '加载中',
    description: '页面正在准备基础占位内容。'
  },
  error: {
    title: '加载失败',
    description: '页面未获取到可用结果，请稍后重试。'
  },
  forbidden: {
    title: '无权限查看',
    description: '当前状态下不允许访问目标内容。'
  },
  retrying: {
    title: '重试中',
    description: '系统正在按统一策略进行重试。'
  }
};

export function StateView({
  variant,
  title,
  description,
  retryLabel = '重试',
  onRetry
}: StateViewProps) {
  const styles = palette[variant];
  const copy = defaultCopy[variant];

  return (
    <section
      aria-label={`state-view-${variant}`}
      style={{
        ...cardStyle,
        background: styles.background
      }}
    >
      <div>
        <span
          style={{
            display: 'inline-flex',
            padding: '6px 10px',
            borderRadius: '999px',
            background: '#ffffff',
            color: styles.accent,
            fontSize: '0.8rem',
            fontWeight: 700
          }}
        >
          {styles.badge}
        </span>
        <h2 style={{ margin: '16px 0 8px', fontSize: '1.25rem', color: '#111827' }}>
          {title ?? copy.title}
        </h2>
        <p style={{ margin: 0, color: '#4b5563', lineHeight: 1.6 }}>
          {description ?? copy.description}
        </p>
      </div>

      {variant === 'error' && onRetry ? (
        <button
          type="button"
          onClick={onRetry}
          style={{
            marginTop: '20px',
            width: 'fit-content',
            border: 'none',
            borderRadius: '999px',
            padding: '10px 16px',
            background: styles.accent,
            color: '#ffffff',
            cursor: 'pointer',
            fontWeight: 700
          }}
        >
          {retryLabel}
        </button>
      ) : null}
    </section>
  );
}
