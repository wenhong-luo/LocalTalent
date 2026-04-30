import { render, screen, within } from '@testing-library/react';
import { PortalShell } from '@/components/portal/PortalShell';
import {
  ContentChannelPage,
  JobFairDetailPage,
  JobFairListPage,
  MoreServicesPage,
  PortalContentDetailPage
} from './PortalContentPages';
import type { PortalContent, PortalEvent } from './portalContentApi';

const content = {
  content_id: 301,
  content_type: 'policy',
  title: '就业政策公开说明',
  cover_url: '/demo/policy.png',
  summary: '政策公开摘要',
  body_text: '政策公开正文，不包含个人明细。',
  city_code: '310000',
  publish_time: '2026-04-30T10:00:00',
  updated_at: '2026-04-30T10:00:00',
  mobile: '13900000000',
  email: 'secret@example.com',
  resume_body: 'raw resume body',
  attachment_object_key: 'private/file.pdf',
  evidence: 'private evidence',
  password_hash: '$2a$secret',
  base_profile_json: '{"name":"raw"}',
  审核材料: 'private material',
  后台备注: 'internal memo'
} as unknown as PortalContent;

const event = {
  event_id: 401,
  title: '春风行动招聘会',
  type_code: 'onsite',
  city_code: '310000',
  start_time: '2026-05-10T09:00:00',
  end_time: '2026-05-10T13:00:00',
  location: 'LocalTalent 会场',
  status: 1,
  updated_at: '2026-04-30T10:00:00',
  activity_registration: [{ candidate_id: 1 }],
  sign_status: 1,
  mobile: '13900000001',
  email: 'event-secret@example.com',
  报名名单: 'hidden list',
  签到证据: 'hidden evidence'
} as unknown as PortalEvent;

function expectNoSensitiveLeak() {
  const body = document.body.textContent ?? '';
  const forbiddenMarkers = [
    '13900000000',
    '13900000001',
    'secret@example.com',
    'event-secret@example.com',
    'raw resume body',
    'private/file.pdf',
    'private evidence',
    '$2a$secret',
    'base_profile_json',
    'activity_registration',
    'candidate_id',
    'sign_status',
    'mobile',
    'email',
    'resume_body',
    'attachment_object_key',
    'evidence',
    'password_hash',
    '审核材料',
    '后台备注',
    '报名名单：hidden list',
    '签到证据：hidden evidence'
  ];

  for (const marker of forbiddenMarkers) {
    expect(body).not.toContain(marker);
  }
}

describe('Portal public content channels', () => {
  it('renders policy channel with public fields only', () => {
    render(
      <PortalShell>
        <ContentChannelPage
          config={{
            contentType: 'policy',
            eyebrow: 'Employment Policy',
            title: '就业政策',
            description: '公开政策频道',
            detailBasePath: '/articles',
            badge: '政策公开频道'
          }}
          initialState={{
            status: 'ready',
            query: { city_code: '310000', page: 1, size: 12 },
            page: { content_list: [content], total: 1 },
            traceId: 'trace-policy'
          }}
        />
      </PortalShell>
    );

    expect(screen.getByLabelText('就业政策频道首屏')).toBeInTheDocument();
    expect(screen.getByLabelText('就业政策筛选')).toBeInTheDocument();
    expect(screen.getByLabelText('就业政策列表')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '就业政策公开说明' })).toHaveAttribute('href', '/articles/301');
    expect(screen.getByText('政策公开摘要')).toBeInTheDocument();
    expectNoSensitiveLeak();
  });

  it('renders job fair list and detail without registration details', () => {
    render(
      <PortalShell>
        <JobFairListPage
          initialState={{
            status: 'ready',
            query: { type_code: 'onsite', city_code: '310000', page: 1, size: 12 },
            page: { event_list: [event], total: 1 },
            traceId: 'trace-event'
          }}
        />
      </PortalShell>
    );

    expect(screen.getByLabelText('招聘会频道首屏')).toBeInTheDocument();
    expect(screen.getByLabelText('招聘会筛选')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: '春风行动招聘会' })).toHaveAttribute('href', '/job-fairs/401');
    expect(screen.getByText('报名名单不公开')).toBeInTheDocument();
    expectNoSensitiveLeak();

    render(<JobFairDetailPage event={event} title="招聘会" traceId="trace-event-detail" />);
    expect(screen.getByLabelText('招聘会详情')).toBeInTheDocument();
    expect(screen.getAllByText('报名名单不公开').length).toBeGreaterThan(0);
    expect(screen.getAllByText('签到证据不公开').length).toBeGreaterThan(0);
    expectNoSensitiveLeak();
  });

  it('renders content detail as safe text', () => {
    render(<PortalContentDetailPage content={content} title="公开内容" traceId="trace-content-detail" />);

    expect(screen.getByLabelText('公开内容详情')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '就业政策公开说明' })).toBeInTheDocument();
    expect(screen.getByText('政策公开正文，不包含个人明细。')).toBeInTheDocument();
    expectNoSensitiveLeak();
  });

  it('renders more services with high-risk entries disabled', () => {
    render(
      <PortalShell>
        <MoreServicesPage />
      </PortalShell>
    );

    expect(screen.getByLabelText('更多服务频道首屏')).toBeInTheDocument();
    const lowRiskRegion = screen.getByLabelText('低风险更多服务');
    expect(lowRiskRegion).toBeInTheDocument();
    expect(screen.getByLabelText('高风险更多服务占位')).toBeInTheDocument();
    expect(within(lowRiskRegion).getByRole('link', { name: /今日招聘/ })).toHaveAttribute('href', '/daily-jobs');
    expect(within(lowRiskRegion).getByRole('link', { name: /网络招聘会/ })).toHaveAttribute('href', '/job-fairs/online');
    expect(within(lowRiskRegion).getByRole('link', { name: /校园招聘/ })).toHaveAttribute('href', '/campus');

    const highRiskRegion = screen.getByLabelText('高风险更多服务占位');
    for (const label of ['地图找工作', '视频招聘', '直播招聘', '自由职业', '求职登记', '短信通知', '微信通知']) {
      expect(within(highRiskRegion).getByText(label)).toBeInTheDocument();
    }
    expect(within(highRiskRegion).getAllByRole('button', { name: '禁用占位' }).length).toBeGreaterThanOrEqual(1);
    expect(document.body.textContent ?? '').not.toContain('真实支付');
  });
});
