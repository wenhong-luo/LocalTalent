import { existsSync } from 'node:fs';
import { render, screen, within } from '@testing-library/react';
import { PortalShell } from '@/components/portal/PortalShell';
import { JobSearchPage } from '@/pages/portal/jobs/JobSearchPage';
import type { PortalJob } from '@/pages/portal/jobs/portalJobApi';
import { CampusLandingPage } from './CampusLandingPage';
import { JobFairListPage, MoreServicesPage, PortalContentDetailPage } from './PortalContentPages';
import type { PortalContent, PortalEvent } from './portalContentApi';

const publicJob = {
  job_id: 501,
  title: '校园 Java 工程师',
  company_name: 'LocalTalent 认证企业',
  category_code: 'software',
  city_code: '310000',
  salary_min: 12000,
  salary_max: 18000,
  job_desc: '公开职位描述',
  updated_at: '2026-04-30T10:00:00',
  mobile: '13900000000',
  email: 'job-secret@example.com',
  attachment_object_key: 'private/job.pdf',
  evidence: 'private evidence',
  password_hash: '$2a$secret',
  审核材料: 'internal audit'
} as unknown as PortalJob;

const publicEvent = {
  event_id: 601,
  title: '网络招聘会公开场',
  type_code: 'online',
  city_code: '310000',
  start_time: '2026-05-01T09:00:00',
  end_time: '2026-05-01T12:00:00',
  location: '线上会场占位',
  status: 1,
  updated_at: '2026-04-30T10:00:00',
  mobile: '13900000001',
  email: 'event-secret@example.com',
  报名名单: 'hidden registration list',
  签到证据: 'hidden sign evidence'
} as unknown as PortalEvent;

const publicContent = {
  content_id: 701,
  content_type: 'help',
  title: '公开服务说明',
  cover_url: '',
  summary: '公开摘要',
  body_text: '公开正文',
  city_code: '310000',
  publish_time: '2026-04-30T10:00:00',
  updated_at: '2026-04-30T10:00:00',
  mobile: '13900000002',
  email: 'content-secret@example.com',
  resume_body: 'raw resume',
  base_profile_json: '{"name":"raw"}',
  审核材料: 'hidden material'
} as unknown as PortalContent;

function expectNoSensitiveLeak() {
  const body = document.body.textContent ?? '';
  for (const marker of [
    '13900000000',
    '13900000001',
    '13900000002',
    'job-secret@example.com',
    'event-secret@example.com',
    'content-secret@example.com',
    'private/job.pdf',
    'private evidence',
    '$2a$secret',
    'hidden registration list',
    'hidden sign evidence',
    'raw resume',
    'base_profile_json',
    'mobile',
    'email',
    'attachment_object_key',
    'evidence',
    'password_hash',
    '报名名单：hidden registration list',
    '签到证据：hidden sign evidence',
    '审核材料'
  ]) {
    expect(body).not.toContain(marker);
  }
}

describe('Phase II supplemental public routes', () => {
  it('keeps the low-risk P1 template route files present', () => {
    for (const route of [
      'src/app/jobs/famous/page.page.tsx',
      'src/app/jobs/emergency/page.page.tsx',
      'src/app/daily-jobs/page.page.tsx',
      'src/app/job-fairs/online/page.page.tsx',
      'src/app/job-fairs/campus/page.page.tsx',
      'src/app/campus/page.page.tsx',
      'src/app/campus/jobs/page.page.tsx',
      'src/app/campus/elections/page.page.tsx',
      'src/app/explain/page.page.tsx',
      'src/app/explain/[id]/page.page.tsx'
    ]) {
      expect(existsSync(route)).toBe(true);
    }
  });

  it('renders famous, emergency and daily job topics without sensitive fields', () => {
    render(
      <PortalShell>
        <JobSearchPage
          initialState={{
            status: 'ready',
            query: { famous: '1', updated_within: '7', sort: 'updated_desc' },
            page: { job_list: [publicJob], total: 1 }
          }}
          topic={{
            title: '急聘 / 快速招聘',
            subtitle: '不接会员置顶、付费推广、真实地图或外部招聘服务。',
            badge: '快速入口占位'
          }}
        />
      </PortalShell>
    );

    expect(screen.getByRole('heading', { name: '急聘 / 快速招聘' })).toBeInTheDocument();
    expect(screen.getByText('校园 Java 工程师')).toBeInTheDocument();
    expect(screen.getByText('不接会员置顶、付费推广、真实地图或外部招聘服务。')).toBeInTheDocument();
    expectNoSensitiveLeak();
  });

  it('renders online and campus job fair topics without registration details', () => {
    render(
      <PortalShell>
        <JobFairListPage
          initialState={{
            status: 'ready',
            query: { type_code: 'online', page: 1, size: 12 },
            page: { event_list: [publicEvent], total: 1 }
          }}
          topic={{
            title: '网络招聘会',
            description: '直播、视频、在线互动、报名名单和联系方式均不在公开层实现。',
            badge: '网络招聘会公开频道'
          }}
        />
      </PortalShell>
    );

    expect(screen.getByRole('heading', { name: '网络招聘会' })).toBeInTheDocument();
    expect(screen.getByText('网络招聘会公开场')).toBeInTheDocument();
    expect(screen.getByText('报名名单不公开')).toBeInTheDocument();
    expectNoSensitiveLeak();
  });

  it('renders campus landing and explain detail as public-only content', () => {
    render(
      <PortalShell>
        <CampusLandingPage
          jobs={{ job_list: [publicJob], total: 1 }}
          events={{ event_list: [publicEvent], total: 1 }}
          contents={{ content_list: [publicContent], total: 1 }}
        />
      </PortalShell>
    );

    expect(screen.getByLabelText('校园招聘门户首屏')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /校园职位/ })).toHaveAttribute('href', '/campus/jobs');
    expect(screen.getByRole('link', { name: /校园宣讲/ })).toHaveAttribute('href', '/campus/elections');
    expectNoSensitiveLeak();

    render(<PortalContentDetailPage content={publicContent} title="公开说明" traceId="trace-explain" />);
    expect(screen.getByLabelText('公开说明详情')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '公开服务说明' })).toBeInTheDocument();
    expectNoSensitiveLeak();
  });

  it('keeps high-risk more-service entries disabled after adding stable low-risk routes', () => {
    render(
      <PortalShell>
        <MoreServicesPage />
      </PortalShell>
    );

    const lowRiskRegion = screen.getByLabelText('低风险更多服务');
    expect(within(lowRiskRegion).getByRole('link', { name: /今日招聘/ })).toHaveAttribute('href', '/daily-jobs');
    expect(within(lowRiskRegion).getByRole('link', { name: /快速招聘/ })).toHaveAttribute('href', '/jobs/emergency');
    expect(within(lowRiskRegion).getByRole('link', { name: /网络招聘会/ })).toHaveAttribute('href', '/job-fairs/online');
    expect(within(lowRiskRegion).getByRole('link', { name: /校园招聘/ })).toHaveAttribute('href', '/campus');

    const highRiskRegion = screen.getByLabelText('高风险更多服务占位');
    for (const label of ['地图找工作', '视频招聘', '直播招聘', '自由职业', '求职登记', '短信通知', '微信通知']) {
      expect(within(highRiskRegion).getByText(label)).toBeInTheDocument();
    }
    expect(within(highRiskRegion).getAllByRole('button', { name: '禁用占位' }).length).toBeGreaterThanOrEqual(1);
  });
});
