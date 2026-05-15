import { render, screen } from '@testing-library/react';
import { PortalShell } from '@/components/portal/PortalShell';
import { PortalHomePage } from './PortalHomePage';

function renderHome() {
  render(
    <PortalShell>
      <PortalHomePage
        hotJobs={[
          {
            target_type: 'job',
            target_id: 1,
            title: 'Java 后端工程师',
            summary: '山西正力网络有限公司 · 12K~18K',
            tags: ['在线职位', '认证企业'],
            url: '/jobs/1',
            city_code: '上海',
            updated_at: '2026-05-01T10:00:00',
            display_order: 1
          }
        ]}
        featuredCompanies={[
          {
            target_type: 'company',
            target_id: 2,
            title: '山西正力网络有限公司',
            summary: '互联网服务',
            tags: ['认证企业'],
            url: '/companies/2',
            city_code: '太原',
            updated_at: '2026-05-01T10:00:00',
            display_order: 1
          }
        ]}
      />
    </PortalShell>
  );
}

describe('PortalHomePage', () => {
  it('renders the high-fidelity home first screen structure', () => {
    renderHome();

    expect(screen.getByLabelText('门户顶部工具条')).toBeInTheDocument();
    expect(screen.getByLabelText('门户搜索头部')).toBeInTheDocument();
    expect(screen.getByLabelText('门户主导航')).toBeInTheDocument();
    expect(screen.getByTestId('portal-home-page')).toHaveAttribute('data-layout', 'portal-ad-rails');
    expect(screen.getByLabelText('职位分类墙')).toBeInTheDocument();
    expect(screen.getByLabelText('首页快捷入口')).toBeInTheDocument();
    expect(screen.getByLabelText('首页登录卡片')).toBeInTheDocument();
    expect(screen.getByLabelText('公告与招聘会')).toBeInTheDocument();
    expect(screen.getByLabelText('首页广告位')).toBeInTheDocument();
    expect(screen.getByLabelText('扫码 CTA')).toBeInTheDocument();
    expect(screen.getByLabelText('推荐企业')).toBeInTheDocument();
    expect(screen.getByLabelText('热招职位')).toBeInTheDocument();
    expect(screen.getByLabelText('右侧浮动工具条')).toBeInTheDocument();
  });

  it('renders job categories, service entrances and public recommendation data', () => {
    renderHome();

    expect(screen.getByRole('link', { name: /生活 \| 服务业/ })).toHaveAttribute('href', expect.stringContaining('/jobs?category='));
    expect(screen.getByRole('link', { name: /名企直聘/ })).toHaveAttribute('href', '/jobs?channel=famous');
    expect(screen.getByRole('link', { name: /现场招聘会/ })).toHaveAttribute('href', '/job-fairs?type=offline');
    expect(screen.getByText('Java 后端工程师')).toBeInTheDocument();
    expect(screen.getAllByText('山西正力网络有限公司').length).toBeGreaterThan(0);
    expect(screen.getAllByText('认证企业').length).toBeGreaterThan(0);
  });

  it('shows an operations placeholder when recommendation slots are empty', () => {
    render(
      <PortalShell>
        <PortalHomePage />
      </PortalShell>
    );

    expect(screen.getByText('热招职位待配置')).toBeInTheDocument();
    expect(screen.getByText('明星企业待配置')).toBeInTheDocument();
    expect(screen.getAllByText(/运营位待配置或目标已失效/).length).toBeGreaterThan(0);
  });

  it('keeps login and external capabilities as placeholders', () => {
    renderHome();

    expect(screen.getByRole('tab', { name: '求职者登录' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: '企业登录' })).toHaveAttribute('aria-selected', 'false');
    expect(screen.getByLabelText('手机号视觉占位')).toBeInTheDocument();
    expect(screen.getByLabelText('验证码视觉占位')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '短信占位' })).toBeDisabled();
    expect(screen.getByText('公众号、小程序和 App 均为占位，不接真实外部能力。')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '公众号占位' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '小程序占位' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'App 占位' })).toBeDisabled();
  });

  it('does not render raw candidate data or audit material markers', () => {
    renderHome();

    const body = document.body.textContent ?? '';
    const forbiddenMarkers = [
      'mobile',
      'email',
      'resume_body',
      'attachment_object_key',
      'evidence',
      'password_hash',
      'base_profile_json',
      '审核材料',
      '营业执照附件',
      '简历库',
      '搜索简历'
    ];

    for (const marker of forbiddenMarkers) {
      expect(body).not.toContain(marker);
    }
  });
});
