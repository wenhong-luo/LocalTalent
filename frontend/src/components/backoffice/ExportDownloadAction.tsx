import { useState } from 'react';
import { StatusBadge } from './StatusBadge';

export type ExportDownloadState = {
  export_id: number;
  approve_status: number;
  generate_status: number;
  download_count: number;
};

export function canShowExportDownload(exportApply: ExportDownloadState): boolean {
  return (
    exportApply.approve_status === 1
    && exportApply.generate_status === 2
    && exportApply.download_count === 0
  );
}

export function ExportDownloadAction({
  exportApply,
  onIssueDownloadUrl
}: {
  exportApply: ExportDownloadState;
  onIssueDownloadUrl: (exportId: number) => Promise<string>;
}) {
  const [downloadUrl, setDownloadUrl] = useState<string>();
  const [error, setError] = useState<string>();
  const [loading, setLoading] = useState(false);

  async function issue() {
    setLoading(true);
    setError(undefined);
    try {
      setDownloadUrl(await onIssueDownloadUrl(exportApply.export_id));
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : '获取下载链接失败');
    } finally {
      setLoading(false);
    }
  }

  if (!canShowExportDownload(exportApply)) {
    return (
      <span style={{ display: 'inline-flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
        <StatusBadge kind="export_approve" value={exportApply.approve_status} />
        <StatusBadge kind="export_generate" value={exportApply.generate_status} />
        <span style={{ color: 'var(--lt-ink-muted)' }}>暂不显示下载入口</span>
      </span>
    );
  }

  return (
    <span style={{ display: 'inline-flex', gap: '8px', alignItems: 'center', flexWrap: 'wrap' }}>
      <button type="button" style={buttonStyle} disabled={loading} onClick={issue}>
        获取下载链接
      </button>
      {downloadUrl ? <a href={downloadUrl}>短期下载链接</a> : null}
      {error ? <span style={{ color: 'var(--lt-danger)' }}>{error}</span> : null}
    </span>
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
