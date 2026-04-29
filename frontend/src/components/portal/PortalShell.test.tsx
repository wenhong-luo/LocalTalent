import { render, screen, within } from '@testing-library/react';
import { PortalShell } from './PortalShell';

describe('PortalShell', () => {
  it('renders portal-wide header, navigation, footer and floating toolbar', () => {
    render(
      <PortalShell>
        <main>公开门户内容</main>
      </PortalShell>
    );

    expect(screen.getByLabelText('门户顶部工具条')).toBeInTheDocument();
    expect(screen.getByLabelText('门户搜索头部')).toBeInTheDocument();
    expect(screen.getByLabelText('门户主导航')).toBeInTheDocument();
    expect(screen.getByLabelText('门户页脚')).toBeInTheDocument();
    expect(screen.getByLabelText('右侧浮动工具条')).toBeInTheDocument();
    expect(screen.getByText('公开门户内容')).toBeInTheDocument();
  });

  it('renders fixed public navigation without public resume-library wording', () => {
    render(
      <PortalShell>
        <main />
      </PortalShell>
    );

    const nav = screen.getByLabelText('门户主导航');
    expect(within(nav).getByRole('link', { name: '首页' })).toHaveAttribute('href', '/');
    expect(within(nav).getByRole('link', { name: '找工作' })).toHaveAttribute('href', '/jobs');
    expect(within(nav).getByRole('link', { name: '找企业' })).toHaveAttribute('href', '/companies');
    expect(within(nav).getByRole('link', { name: '人才服务区' })).toHaveAttribute(
      'href',
      '/portal/talent-service-area'
    );
    expect(within(nav).getByRole('link', { name: '招聘会' })).toHaveAttribute('href', '/job-fairs');
    expect(within(nav).getByRole('link', { name: '就业政策' })).toHaveAttribute('href', '/articles/policies');
    expect(within(nav).getByRole('link', { name: 'HR 工具箱' })).toHaveAttribute('href', '/hr-tools');
    expect(within(nav).getByRole('link', { name: '管理中心' })).toHaveAttribute('href', '/candidate/center');
    expect(document.body).not.toHaveTextContent('简历库');
    expect(document.body).not.toHaveTextContent('搜索简历');
  });

  it('keeps external capabilities as disabled placeholders', () => {
    render(
      <PortalShell>
        <main />
      </PortalShell>
    );

    expect(screen.getByRole('button', { name: '地图搜索占位' })).toBeDisabled();
    expect(screen.getByRole('button', { name: /地图找工作/ })).toBeDisabled();
    expect(screen.getByRole('button', { name: /视频招聘/ })).toBeDisabled();
    expect(screen.getByRole('button', { name: /直播招聘/ })).toBeDisabled();
    expect(screen.getByRole('button', { name: '公众号占位' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '小程序占位' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'App 占位' })).toBeDisabled();
  });

  it('does not render raw candidate data markers', () => {
    render(
      <PortalShell>
        <main />
      </PortalShell>
    );

    const body = document.body.textContent ?? '';
    const forbiddenMarkers = [
      'mobile',
      'email',
      'resume_body',
      'attachment_object_key',
      'evidence',
      'password_hash',
      'base_profile_json'
    ];

    for (const marker of forbiddenMarkers) {
      expect(body).not.toContain(marker);
    }
  });
});
