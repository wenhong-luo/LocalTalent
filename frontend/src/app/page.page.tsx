import Link from 'next/link';
import { PortalShell } from '@/components/portal/PortalShell';

const shellStyle = {
  minHeight: '100vh',
  padding: '40px 20px'
};

const heroStyle = {
  maxWidth: '1120px',
  margin: '0 auto',
  padding: '56px',
  border: '1px solid var(--lt-line)',
  borderRadius: '36px',
  background: 'var(--lt-card)',
  boxShadow: 'var(--lt-shadow)',
  overflow: 'hidden'
};

export default function PortalHomePage() {
  return (
    <PortalShell>
      <main style={shellStyle}>
        <section style={heroStyle}>
          <p style={{ margin: 0, color: 'var(--lt-accent-strong)', fontWeight: 800 }}>
            LocalTalent Portal
          </p>
          <h1 style={{ maxWidth: '760px', margin: '18px 0', fontSize: 'clamp(2.5rem, 6vw, 5.6rem)', lineHeight: 0.98 }}>
            地方人才服务，从可信发布快照开始。
          </h1>
          <p style={{ maxWidth: '680px', margin: 0, color: 'var(--lt-ink-muted)', fontSize: '1.1rem', lineHeight: 1.9 }}>
            一期门户先开放人才服务区入口。这里不是公共简历库，只展示已同意、可发布、已脱敏的候选人发布快照。
          </p>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '14px', marginTop: '32px' }}>
            <Link
              href="/portal/talent-service-area"
              style={{
                display: 'inline-flex',
                borderRadius: '999px',
                padding: '14px 22px',
                background: 'var(--lt-accent)',
                color: '#ffffff',
                fontWeight: 800
              }}
            >
              进入人才服务区
            </Link>
            <span
              style={{
                display: 'inline-flex',
                borderRadius: '999px',
                padding: '14px 22px',
                background: 'rgba(20, 33, 61, 0.06)',
                color: 'var(--lt-ink-muted)'
              }}
            >
              仅展示发布快照字段白名单
            </span>
          </div>
        </section>
      </main>
    </PortalShell>
  );
}
