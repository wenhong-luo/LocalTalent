import { type CSSProperties } from 'react';
import { StateView } from './components/StateView';

const shellStyle: CSSProperties = {
  minHeight: '100vh',
  padding: '32px 20px',
  background: 'linear-gradient(180deg, #f5f7fb 0%, #eef3ff 100%)',
  color: '#1f2937',
  fontFamily: '"Segoe UI", "PingFang SC", sans-serif'
};

const panelStyle: CSSProperties = {
  maxWidth: '960px',
  margin: '0 auto',
  background: '#ffffff',
  border: '1px solid #d9e1f2',
  borderRadius: '24px',
  padding: '28px',
  boxShadow: '0 20px 60px rgba(37, 99, 235, 0.08)'
};

const gridStyle: CSSProperties = {
  display: 'grid',
  gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
  gap: '16px',
  marginTop: '24px'
};

export default function App() {
  return (
    <main style={shellStyle}>
      <section style={panelStyle}>
        <p style={{ margin: 0, color: '#1d4ed8', fontWeight: 700 }}>
          LocalTalent Phase-1
        </p>
        <h1 style={{ margin: '12px 0 8px', fontSize: '2rem' }}>
          P0 Frontend Scaffold
        </h1>
        <p style={{ margin: 0, color: '#4b5563', lineHeight: 1.7 }}>
          当前只提供前端空启动与状态占位，不渲染任何业务数据，不接职位、
          同意、撤回、发布快照或对接接口。
        </p>

        <div style={gridStyle}>
          <StateView
            variant="loading"
            title="加载中"
            description="前端骨架已就绪，业务数据源后续阶段接入。"
          />
          <StateView
            variant="error"
            title="加载失败"
            description="统一错误状态占位，后续接入接口后复用。"
            retryLabel="稍后重试"
          />
          <StateView
            variant="forbidden"
            title="无权限查看"
            description="公开层不展示原始候选人数据，服务端权限后续补齐。"
          />
          <StateView
            variant="retrying"
            title="重试中"
            description="对接任务与异步流程暂未实现，这里只保留状态口径。"
          />
        </div>
      </section>
    </main>
  );
}
