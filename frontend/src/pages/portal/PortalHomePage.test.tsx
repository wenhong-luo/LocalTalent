import { render, screen } from '@testing-library/react';
import { PortalShell } from '@/components/portal/PortalShell';
import { PortalHomePage } from './PortalHomePage';
import { type PortalHomeSlotItem } from './portalHomeSlotApi';

const configuredHomeSlots: PortalHomeSlotItem[] = [
  {
    slot_id: 188,
    slot_code: 'home_hero_banner',
    title: '城市人才服务首屏',
    subtitle: '后台运营配置的首页 banner，只展示公开职位、认证企业和活动资讯。',
    image_url: '/api/portal/home-slots/188/image',
    image_alt: '城市人才服务首屏运营图',
    link_url: '/jobs?from=home-hero',
    display_order: 1,
    updated_at: '2026-05-15T10:00:00'
  },
  {
    slot_id: 189,
    slot_code: 'home_quick_1',
    title: '岗位直达',
    subtitle: '公开职位入口',
    image_url: '',
    image_alt: '',
    link_url: '/jobs?from=quick',
    display_order: 2,
    updated_at: '2026-05-15T10:01:00'
  }
];

function renderHome(homeSlots: PortalHomeSlotItem[] = configuredHomeSlots) {
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
        homeSlots={homeSlots}
      />
    </PortalShell>
  );
}

describe('PortalHomePage', () => {
  it('renders the high-fidelity home first screen without an inline login form', () => {
    renderHome();

    expect(screen.getByLabelText('门户顶部工具条')).toBeInTheDocument();
    expect(screen.getByLabelText('门户搜索头部')).toBeInTheDocument();
    expect(screen.getByLabelText('门户主导航')).toBeInTheDocument();
    expect(screen.getByTestId('portal-home-page')).toHaveAttribute('data-layout', 'portal-ad-rails');
    expect(screen.getByLabelText('职位分类墙')).toBeInTheDocument();
    expect(screen.getByLabelText('首页搜索增强')).toBeInTheDocument();
    expect(screen.getByLabelText('首页快捷入口')).toBeInTheDocument();
    expect(screen.getByLabelText('首页右侧运营面板')).toBeInTheDocument();
    expect(screen.getByLabelText('安全登录入口')).toBeInTheDocument();
    expect(screen.getByLabelText('公告与招聘会')).toBeInTheDocument();
    expect(screen.getByLabelText('首页公开边界提示')).toBeInTheDocument();
    expect(screen.getByLabelText('右侧浮动工具条')).toBeInTheDocument();
    expect(screen.queryByLabelText('手机号视觉占位')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('验证码视觉占位')).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '短信占位' })).not.toBeInTheDocument();
  });

  it('uses backend-configured home slots for the hero banner and quick entrances', () => {
    renderHome();

    expect(screen.getByRole('link', { name: '首页首屏广告位' })).toHaveAttribute('href', '/jobs?from=home-hero');
    expect(screen.getByText('城市人才服务首屏')).toBeInTheDocument();
    expect(screen.getByText('后台运营配置的首页 banner，只展示公开职位、认证企业和活动资讯。')).toBeInTheDocument();
    expect(screen.getByAltText('城市人才服务首屏运营图')).toHaveAttribute('src', '/api/portal/home-slots/188/image');
    expect(screen.getByRole('link', { name: /岗位直达/ })).toHaveAttribute('href', '/jobs?from=quick');
  });

  it('renders the ten-category wall with flyout position groups', () => {
    renderHome();

    expect(screen.getByRole('link', { name: /生活 \| 服务业/ })).toHaveAttribute('href', '/jobs?category=life_service');
    expect(screen.getByRole('link', { name: /建筑 \| 物业 \| 农林牧渔 \| 其他/ })).toHaveAttribute(
      'href',
      '/jobs?category=construction_property_agriculture'
    );
    expect(screen.getByText('餐饮')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '服务员' })).toHaveAttribute('href', expect.stringContaining('/jobs?category=life_service'));
    expect(screen.getByRole('link', { name: '前端工程师' })).toHaveAttribute(
      'href',
      expect.stringContaining('/jobs?category=network_communication_electronics')
    );
  });

  it('renders service entrances, public recommendations and below-fold modules', () => {
    renderHome();

    expect(screen.getByRole('link', { name: /名企直聘/ })).toHaveAttribute('href', '/jobs?channel=famous');
    expect(screen.getByRole('link', { name: /现场招聘会/ })).toHaveAttribute('href', '/job-fairs?type=offline');
    expect(screen.getByText('Java 后端工程师')).toBeInTheDocument();
    expect(screen.getAllByText('山西正力网络有限公司').length).toBeGreaterThan(0);
    expect(screen.getAllByText('认证企业').length).toBeGreaterThan(0);
    expect(screen.getByLabelText('首页招聘会模块')).toBeInTheDocument();
    expect(screen.getByLabelText('资讯公告与 HR 工具箱')).toBeInTheDocument();
    expect(screen.getByLabelText('友情链接与合作入口')).toBeInTheDocument();
  });

  it('renders the full home operation slot system as placeholders', () => {
    renderHome();

    expect(screen.getByLabelText('首页运营广告位体系')).toBeInTheDocument();
    expect(screen.getByLabelText('首页通栏广告位')).toHaveTextContent('城市人才服务季');
    expect(screen.getByAltText('城市人才服务季运营图')).toBeInTheDocument();
    expect(screen.getByLabelText('首页1/2广告位')).toBeInTheDocument();
    expect(screen.getByLabelText('首页1/3广告位')).toBeInTheDocument();
    expect(screen.getByAltText('企业发布职位运营图')).toBeInTheDocument();
    expect(screen.getByAltText('就业政策运营图')).toBeInTheDocument();
    expect(screen.getByLabelText('首页快捷广告位')).toBeInTheDocument();
    expect(screen.getByText('广告位仅为运营占位，不接真实投放、计费、支付或外部平台。')).toBeInTheDocument();
  });

  it('uses safe demo data when recommendation slots are empty', () => {
    render(
      <PortalShell>
        <PortalHomePage />
      </PortalShell>
    );

    expect(screen.getByText('前端开发工程师')).toBeInTheDocument();
    expect(screen.getByText('生产质检员')).toBeInTheDocument();
    expect(screen.getByText('LocalTalent 数字服务示范企业')).toBeInTheDocument();
    expect(screen.getAllByText('演示数据').length).toBeGreaterThan(0);
  });

  it('keeps external capabilities as placeholders', () => {
    renderHome();

    expect(screen.getByText(/首页不放置短信验证码表单/)).toBeInTheDocument();
    expect(screen.getByText('短信、微信、小程序、App 登录均为后续专项，不在首页开放。')).toBeInTheDocument();
    for (const button of screen.getAllByRole('button', { name: '地图搜索占位' })) {
      expect(button).toBeDisabled();
    }
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
      '搜索简历',
      'contact_unlock'
    ];

    for (const marker of forbiddenMarkers) {
      expect(body).not.toContain(marker);
    }
  });
});
