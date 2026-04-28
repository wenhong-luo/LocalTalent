import { type ReactNode } from 'react';

export type ReviewColumn<T> = {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
};

export function ReviewTable<T>({
  title,
  rows,
  columns,
  emptyText = '暂无待处理记录'
}: {
  title: string;
  rows: T[];
  columns: ReviewColumn<T>[];
  emptyText?: string;
}) {
  return (
    <section
      style={{
        border: '1px solid var(--lt-line)',
        borderRadius: '24px',
        background: 'var(--lt-card)',
        padding: '20px',
        boxShadow: '0 16px 36px rgba(20, 33, 61, 0.08)',
        overflowX: 'auto'
      }}
    >
      <h2 style={{ margin: '0 0 14px', fontSize: '1.18rem' }}>{title}</h2>
      {rows.length === 0 ? (
        <p style={{ color: 'var(--lt-ink-muted)' }}>{emptyText}</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse', minWidth: '720px' }}>
          <thead>
            <tr>
              {columns.map((column) => (
                <th
                  key={column.key}
                  style={{
                    borderBottom: '1px solid var(--lt-line)',
                    padding: '10px',
                    textAlign: 'left',
                    color: 'var(--lt-ink-muted)',
                    fontSize: '0.85rem'
                  }}
                >
                  {column.header}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {rows.map((row, rowIndex) => (
              <tr key={rowIndex}>
                {columns.map((column) => (
                  <td
                    key={column.key}
                    style={{
                      borderBottom: '1px solid rgba(20, 33, 61, 0.08)',
                      padding: '11px 10px',
                      verticalAlign: 'top'
                    }}
                  >
                    {column.render(row)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
