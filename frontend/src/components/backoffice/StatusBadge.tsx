export type StatusKind =
  | 'company_auth'
  | 'job_status'
  | 'job_audit'
  | 'export_approve'
  | 'export_generate'
  | 'application_status';

const labelMap: Record<StatusKind, Record<number, string>> = {
  company_auth: {
    1: '待审核',
    2: '已通过',
    3: '已驳回'
  },
  job_status: {
    1: '待审核',
    2: '在线',
    3: '下线'
  },
  job_audit: {
    1: '待审核',
    2: '通过',
    3: '驳回'
  },
  export_approve: {
    0: '待审批',
    1: '审批通过',
    2: '已驳回'
  },
  export_generate: {
    0: '未生成',
    1: '生成中',
    2: '已生成',
    3: '生成失败'
  },
  application_status: {
    0: '已投递',
    1: '待筛选',
    2: '邀约面试',
    3: '已签到',
    4: '已结束',
    5: '已淘汰'
  }
};

const toneMap: Record<string, { color: string; background: string }> = {
  pending: { color: '#92400e', background: '#fef3c7' },
  success: { color: '#047857', background: '#d1fae5' },
  danger: { color: '#b42318', background: '#fee2e2' },
  neutral: { color: '#475569', background: '#e2e8f0' }
};

export function statusLabel(kind: StatusKind, value: number | null | undefined): string {
  if (typeof value !== 'number') {
    return '未知';
  }

  return labelMap[kind][value] ?? `状态${value}`;
}

function tone(kind: StatusKind, value: number | null | undefined): keyof typeof toneMap {
  if (kind === 'company_auth') {
    return value === 2 ? 'success' : value === 3 ? 'danger' : 'pending';
  }

  if (kind === 'job_status') {
    return value === 2 ? 'success' : value === 3 ? 'neutral' : 'pending';
  }

  if (kind === 'job_audit') {
    return value === 2 ? 'success' : value === 3 ? 'danger' : 'pending';
  }

  if (kind === 'export_approve') {
    return value === 1 ? 'success' : value === 2 ? 'danger' : 'pending';
  }

  if (kind === 'export_generate') {
    return value === 2 ? 'success' : value === 3 ? 'danger' : value === 1 ? 'pending' : 'neutral';
  }

  return value === 3 ? 'success' : value === 5 ? 'danger' : 'neutral';
}

export function StatusBadge({ kind, value }: { kind: StatusKind; value: number | null | undefined }) {
  const styles = toneMap[tone(kind, value)];

  return (
    <span
      style={{
        display: 'inline-flex',
        width: 'fit-content',
        borderRadius: '999px',
        padding: '5px 10px',
        color: styles.color,
        background: styles.background,
        fontSize: '0.82rem',
        fontWeight: 800
      }}
    >
      {statusLabel(kind, value)}
    </span>
  );
}
