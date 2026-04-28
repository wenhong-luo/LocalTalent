'use client';

import { useState } from 'react';

export type ApprovalDecision = 'approve' | 'reject';

export function ApprovalDialog({
  subject,
  onSubmit
}: {
  subject: string;
  onSubmit: (decision: ApprovalDecision, memo: string) => Promise<void> | void;
}) {
  const [open, setOpen] = useState(false);
  const [memo, setMemo] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function submit(decision: ApprovalDecision) {
    if (decision === 'reject' && !memo.trim()) {
      setError('驳回原因必填');
      return;
    }

    setSubmitting(true);
    setError('');
    try {
      await onSubmit(decision, memo.trim());
      setOpen(false);
      setMemo('');
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '提交失败');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <button type="button" style={buttonStyle} onClick={() => setOpen(true)}>
        审批
      </button>
      {open ? (
        <section
          role="dialog"
          aria-label={`${subject}审批弹窗`}
          style={{
            marginTop: '10px',
            border: '1px solid rgba(15, 118, 110, 0.26)',
            borderRadius: '18px',
            background: '#ffffff',
            padding: '14px',
            minWidth: '220px'
          }}
        >
          <strong>{subject}</strong>
          <label style={{ display: 'block', marginTop: '10px' }}>
            <span style={{ display: 'block', color: 'var(--lt-ink-muted)', marginBottom: '6px' }}>审批备注</span>
            <textarea
              value={memo}
              onChange={(event) => setMemo(event.target.value)}
              rows={3}
              style={{
                width: '100%',
                border: '1px solid var(--lt-line)',
                borderRadius: '12px',
                padding: '10px'
              }}
            />
          </label>
          {error ? <p style={{ color: 'var(--lt-danger)', margin: '8px 0 0' }}>{error}</p> : null}
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', marginTop: '12px' }}>
            <button type="button" style={buttonStyle} disabled={submitting} onClick={() => submit('approve')}>
              通过
            </button>
            <button
              type="button"
              style={{ ...buttonStyle, background: 'var(--lt-danger)' }}
              disabled={submitting}
              onClick={() => submit('reject')}
            >
              驳回
            </button>
            <button
              type="button"
              style={{ ...buttonStyle, background: '#64748b' }}
              disabled={submitting}
              onClick={() => setOpen(false)}
            >
              取消
            </button>
          </div>
        </section>
      ) : null}
    </>
  );
}

const buttonStyle = {
  border: 'none',
  borderRadius: '999px',
  padding: '9px 14px',
  background: 'var(--lt-accent)',
  color: '#ffffff',
  cursor: 'pointer',
  fontWeight: 800
};
