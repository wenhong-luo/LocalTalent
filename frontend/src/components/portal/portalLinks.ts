export type PortalLink = {
  label: string;
  href: string;
};

export type PortalDisabledLink = {
  label: string;
  reason: string;
};

export const mainNavLinks: PortalLink[] = [
  { label: '首页', href: '/' },
  { label: '找工作', href: '/jobs' },
  { label: '找企业', href: '/companies' },
  { label: '人才服务区', href: '/portal/talent-service-area' },
  { label: '招聘会', href: '/job-fairs' },
  { label: '就业政策', href: '/articles/policies' },
  { label: 'HR 工具箱', href: '/hr-tools' }
];

export const lowRiskServiceLinks: PortalLink[] = [
  { label: '今日招聘', href: '/daily-jobs' },
  { label: '快速招聘', href: '/jobs/emergency' },
  { label: '网络招聘会', href: '/job-fairs/online' },
  { label: '校园招聘', href: '/campus' },
  { label: '帮助中心', href: '/help' }
];

export const highRiskServicePlaceholders: PortalDisabledLink[] = [
  { label: '地图找工作', reason: '真实地图服务后续评估' },
  { label: '视频招聘', reason: '真实视频服务后续评估' },
  { label: '直播招聘', reason: '真实直播服务后续评估' },
  { label: '自由职业', reason: '交易撮合进入风险池' },
  { label: '求职登记', reason: '个人信息采集进入风险池' }
];

export const managementLinks: PortalLink[] = [
  { label: '求职者中心', href: '/auth/login?role=candidate&redirect=/candidate/center' },
  { label: '企业中心', href: '/auth/login?role=company&redirect=/company' },
  { label: '运营后台', href: '/auth/login?redirect=/admin' }
];
