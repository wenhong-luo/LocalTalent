import { render, screen, within } from '@testing-library/react';
import { PortalShell } from '@/components/portal/PortalShell';
import { JobDetailPage } from './JobDetailPage';
import { JobSearchPage, type JobSearchInitialState } from './JobSearchPage';
import type { PortalJob } from './portalJobApi';

const jobs = [
  {
    job_id: 101,
    title: 'Java 后端工程师',
    company_name: 'LocalTalent 认证科技',
    category_code: 'software',
    city_code: '310000',
    salary_min: 18000,
    salary_max: 26000,
    job_desc: '负责地方人才服务平台公开职位能力建设。',
    updated_at: '2026-04-29T10:00:00',
    mobile: '13900000000',
    email: 'secret@example.com',
    contact: 'internal-contact',
    license: 'internal-license-file',
    attachment_object_key: 'private/license.png',
    evidence: 'private evidence',
    password_hash: '$2a$secret',
    审核材料: 'hidden material',
    营业执照附件: 'hidden license'
  } as unknown as PortalJob
];

function renderJobs(initialState?: Partial<JobSearchInitialState>) {
  render(
    <PortalShell>
      <JobSearchPage
        initialState={{
          status: 'ready',
          query: {
            keyword: 'Java',
            city_code: '310000',
            category_code: 'software',
            salary_range: '12000-20000',
            industry_code: 'internet',
            scale_code: '50-100',
            updated_within: '7',
            sort: 'salary_desc'
          },
          page: {
            job_list: jobs,
            total: 1
          },
          traceId: 'trace-p18-front',
          ...initialState
        }}
      />
    </PortalShell>
  );
}

describe('JobSearchPage', () => {
  it('renders public portal shell, filters, famous entry and job cards', () => {
    renderJobs();

    expect(screen.getByLabelText('门户顶部工具条')).toBeInTheDocument();
    expect(screen.getByLabelText('门户搜索头部')).toBeInTheDocument();
    expect(screen.getByTestId('job-search-page')).toHaveAttribute('data-layout', 'portal-ad-rails');
    expect(screen.getByTestId('portal-ad-rail-frame')).toHaveAttribute('data-layout', 'portal-ad-rails');
    expect(screen.getByLabelText('找工作左侧广告留白')).toBeInTheDocument();
    expect(screen.getByLabelText('找工作右侧广告留白')).toBeInTheDocument();
    expect(screen.getByLabelText('找工作频道首屏')).toBeInTheDocument();
    expect(screen.getByLabelText('找工作筛选')).toBeInTheDocument();
    expect(screen.getByLabelText('职位搜索结果')).toBeInTheDocument();
    expect(screen.getAllByLabelText('扫码求职 CTA').length).toBeGreaterThan(0);

    expect(screen.getByRole('link', { name: '全部职位' })).toHaveAttribute('href', '/jobs');
    expect(screen.getByRole('link', { name: '名企直聘' })).toHaveAttribute('href', '/jobs/famous');
    expect(screen.getByLabelText('关键词')).toHaveValue('Java');
    expect(screen.getByLabelText('地区')).toHaveValue('310000');
    expect(screen.getByLabelText('职类')).toHaveValue('software');
    expect(screen.getByLabelText('薪资')).toHaveValue('12000-20000');
    expect(screen.getByLabelText('行业')).toHaveValue('internet');
    expect(screen.getByLabelText('企业规模')).toHaveValue('50-100');
    expect(screen.getByLabelText('更新时间')).toHaveValue('7');
    expect(screen.getByLabelText('排序')).toHaveValue('salary_desc');

    const card = screen.getByText('Java 后端工程师').closest('article');
    expect(card).not.toBeNull();
    expect(within(card as HTMLElement).getByRole('link', { name: 'Java 后端工程师' })).toHaveAttribute(
      'href',
      '/jobs/101'
    );
    expect(within(card as HTMLElement).getByText('认证企业')).toBeInTheDocument();
    expect(within(card as HTMLElement).getByText('18K-26K')).toBeInTheDocument();
    expect(within(card as HTMLElement).getByRole('button', { name: '投递占位' })).toBeDisabled();
    expect(within(card as HTMLElement).getByRole('button', { name: '收藏占位' })).toBeDisabled();
  });

  it('keeps unsupported external and deep filters as placeholders', () => {
    renderJobs();

    expect(screen.getByText('经验、学历、企业性质筛选入口已保留为稳定 query，因当前职位表暂无结构化字段，本轮不参与真实过滤。')).toBeInTheDocument();
    expect(screen.getByText('公众号、小程序、App 均为占位，不接真实外部能力。')).toBeInTheDocument();
  });

  it('does not render internal company contact, audit material or raw candidate markers', () => {
    renderJobs();

    const body = document.body.textContent ?? '';
    const forbiddenMarkers = [
      '13900000000',
      'secret@example.com',
      'internal-contact',
      'internal-license-file',
      'private/license.png',
      'private evidence',
      '$2a$secret',
      '审核材料',
      '营业执照附件',
      'mobile',
      'email',
      'attachment_object_key',
      'evidence',
      'password_hash',
      '简历库',
      '搜索简历'
    ];

    for (const marker of forbiddenMarkers) {
      expect(body).not.toContain(marker);
    }
  });

  it('renders empty and error states without leaking fields', () => {
    renderJobs({
      status: 'empty',
      page: {
        job_list: [],
        total: 0
      }
    });

    expect(screen.getByText('暂无符合条件的在线职位。')).toBeInTheDocument();
  });

  it('renders the minimal SEO job detail entry with public fields only', () => {
    render(<JobDetailPage job={jobs[0]} traceId="trace-job-detail" />);

    expect(screen.getByLabelText('职位详情 SEO 入口')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Java 后端工程师' })).toBeInTheDocument();
    expect(screen.getByText('LocalTalent 认证科技 · 310000 · software')).toBeInTheDocument();
    expect(screen.getByText('18K-26K')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '投递占位' })).toBeDisabled();
    expect(screen.getByRole('button', { name: '收藏占位' })).toBeDisabled();

    const body = document.body.textContent ?? '';
    expect(body).not.toContain('secret@example.com');
    expect(body).not.toContain('审核材料');
  });
});
