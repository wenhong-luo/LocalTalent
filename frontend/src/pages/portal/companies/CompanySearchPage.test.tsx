import { render, screen, within } from '@testing-library/react';
import { PortalShell } from '@/components/portal/PortalShell';
import { CompanyDetailPage } from './CompanyDetailPage';
import { CompanySearchPage, type CompanySearchInitialState } from './CompanySearchPage';
import type { PortalCompany } from './portalCompanyApi';

const company = {
  company_id: 201,
  company_name: 'LocalTalent 认证科技',
  city_code: '310000',
  industry_code: 'internet',
  nature_code: 'private',
  scale_code: '50-100',
  company_verified: true,
  company_profile: '专注地方人才服务的认证企业。',
  open_job_count: 2,
  updated_at: '2026-04-30T10:00:00',
  open_jobs: [
    {
      job_id: 101,
      title: 'Java 后端工程师',
      company_name: 'LocalTalent 认证科技',
      category_code: 'software',
      city_code: '310000',
      salary_min: 18000,
      salary_max: 26000,
      job_desc: '公开职位描述。',
      updated_at: '2026-04-30T10:00:00'
    }
  ],
  license_no: 'secret-license',
  address: 'internal address',
  mobile: '13900000000',
  email: 'secret@example.com',
  auth_reject_reason: 'hidden reject',
  auth_review_user_id: 9,
  auth_submit_time: '2026-04-30T09:00:00',
  营业执照附件: 'private license',
  审核材料: 'private material',
  后台备注: 'internal memo'
} as unknown as PortalCompany;

function renderCompanies(initialState?: Partial<CompanySearchInitialState>) {
  window.history.pushState({}, '', '/companies');

  render(
    <PortalShell>
      <CompanySearchPage
        initialState={{
          status: 'ready',
          query: {
            keyword: 'LocalTalent',
            city_code: '310000',
            industry_code: 'internet',
            nature_code: 'private',
            scale_code: '50-100',
            verified: '1'
          },
          page: {
            company_list: [company],
            total: 1
          },
          traceId: 'trace-p19-front',
          ...initialState
        }}
      />
    </PortalShell>
  );
}

describe('CompanySearchPage', () => {
  afterEach(() => {
    window.history.pushState({}, '', '/');
  });

  it('renders portal shell, filters, company cards and detail link', () => {
    renderCompanies();

    expect(screen.getByLabelText('门户顶部工具条')).toBeInTheDocument();
    expect(screen.getByLabelText('门户搜索头部')).toBeInTheDocument();
    expect(screen.getByLabelText('找企业频道首屏')).toBeInTheDocument();
    expect(screen.getByLabelText('找企业筛选')).toBeInTheDocument();
    expect(screen.getByLabelText('企业搜索结果')).toBeInTheDocument();
    expect(screen.getByTestId('company-search-page')).toHaveAttribute('data-layout', 'portal-ad-rails');
    expect(within(screen.getByLabelText('门户主导航')).getByRole('link', { name: '找企业' })).toHaveAttribute(
      'aria-current',
      'page'
    );

    expect(screen.getByLabelText('关键词')).toHaveValue('LocalTalent');
    expect(screen.getByLabelText('地区')).toHaveValue('310000');
    expect(screen.getByLabelText('行业')).toHaveValue('internet');
    expect(screen.getByLabelText('企业性质')).toHaveValue('private');
    expect(screen.getByLabelText('规模')).toHaveValue('50-100');
    expect(screen.getByLabelText('认证')).toHaveValue('1');

    const card = screen.getByText('LocalTalent 认证科技').closest('article');
    expect(card).not.toBeNull();
    expect(within(card as HTMLElement).getByRole('link', { name: 'LocalTalent 认证科技' })).toHaveAttribute(
      'href',
      '/companies/201'
    );
    expect(within(card as HTMLElement).getByText('认证企业')).toBeInTheDocument();
    expect(within(card as HTMLElement).getByText('2')).toBeInTheDocument();
  });

  it('does not render contact, license, audit material or resume-library wording', () => {
    renderCompanies();

    const body = document.body.textContent ?? '';
    const forbiddenMarkers = [
      'secret-license',
      'internal address',
      '13900000000',
      'secret@example.com',
      'hidden reject',
      'private license',
      'private material',
      'internal memo',
      'license_no',
      'mobile',
      'email',
      'auth_reject_reason',
      '营业执照附件',
      '审核材料',
      '后台备注',
      '简历库',
      '搜索简历'
    ];

    for (const marker of forbiddenMarkers) {
      expect(body).not.toContain(marker);
    }
  });

  it('keeps controlled talent search and contact unlock as risk-pool placeholders', () => {
    renderCompanies();

    expect(screen.getByRole('button', { name: '受控找人才：风险池' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '联系解锁：风险池' })).toBeDisabled();
    expect(document.body).not.toHaveTextContent('会员特权');
  });

  it('renders empty state', () => {
    renderCompanies({
      status: 'empty',
      page: {
        company_list: [],
        total: 0
      }
    });

    expect(screen.getByText('暂无符合条件的认证企业。')).toBeInTheDocument();
  });

  it('renders company public detail with online job aggregation only', () => {
    render(<CompanyDetailPage company={company} traceId="trace-company-detail" />);

    expect(screen.getByLabelText('企业公开主页')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'LocalTalent 认证科技' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /Java 后端工程师/ })).toHaveAttribute('href', '/jobs/101');
    expect(document.body).toHaveTextContent('18K-26K');
    expect(screen.getByRole('button', { name: '联系解锁：风险池' })).toBeDisabled();

    const body = document.body.textContent ?? '';
    expect(body).not.toContain('secret@example.com');
    expect(body).not.toContain('营业执照附件');
  });
});
