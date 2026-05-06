import Link from 'next/link';
import styles from './PortalHomePage.module.css';
import { type PortalRecommendationItem } from './portalRecommendationApi';

const categories = [
  '生活 | 服务业',
  '人力 | 行政 | 管理',
  '销售 | 客服 | 采购 | 淘宝',
  '市场 | 媒介 | 广告 | 设计',
  '生产 | 物流 | 质控 | 汽车',
  '网络 | 通信 | 电子',
  '法律 | 教育 | 翻译 | 出版',
  '财会 | 金融 | 保险',
  '医疗 | 制药 | 环保'
];

const quickEntries = [
  { label: '名企直聘', href: '/jobs?channel=famous', icon: '企' },
  { label: '现场招聘会', href: '/job-fairs?type=offline', icon: '会' },
  { label: '网络招聘会', href: '/job-fairs?type=online', icon: '网' },
  { label: '普工招聘', href: '/jobs?category=general-worker', icon: '普' }
];

const notices = ['系统升级公告', '活动预告公告'];
const fairNotices = ['春风行动招聘会开放预约', '网络招聘会频道占位'];

type PortalHomePageProps = {
  hotJobs?: PortalRecommendationItem[];
  featuredCompanies?: PortalRecommendationItem[];
};

function JobCategoryWall() {
  return (
    <aside className={`${styles.card} ${styles.categoryWall}`} aria-label="职位分类墙">
      <ul className={styles.categoryList}>
        {categories.map((category) => (
          <li key={category}>
            <Link className={styles.categoryLink} href={`/jobs?category=${encodeURIComponent(category)}`}>
              {category}
              <span>›</span>
            </Link>
          </li>
        ))}
      </ul>
      <Link className={styles.allJobs} href="/jobs">
        全部职位
      </Link>
    </aside>
  );
}

function ServiceEntranceGrid() {
  return (
    <nav className={styles.quickGrid} aria-label="首页快捷入口">
      {quickEntries.map((entry) => (
        <Link key={entry.label} className={styles.quickEntry} href={entry.href}>
          <span className={styles.quickIcon}>{entry.icon}</span>
          {entry.label}
        </Link>
      ))}
    </nav>
  );
}

function LoginCard() {
  return (
    <section className={`${styles.card} ${styles.loginCard}`} aria-label="首页登录卡片">
      <div className={styles.loginTabs} role="tablist" aria-label="登录身份">
        <span className={styles.loginTabActive} role="tab" aria-selected="true">
          求职者登录
        </span>
        <span role="tab" aria-selected="false">
          企业登录
        </span>
      </div>
      <div className={styles.loginFields}>
        <input className={styles.input} aria-label="手机号视觉占位" placeholder="请输入手机号" />
        <div className={styles.codeRow}>
          <input aria-label="验证码视觉占位" placeholder="请输入验证码" />
          <button className={styles.disabledCode} type="button" aria-disabled="true" disabled>
            短信占位
          </button>
        </div>
      </div>
      <div className={styles.loginMeta}>
        <Link href="/auth/login?role=candidate&redirect=/candidate/center">密码登录</Link>
        <Link href="/auth/register?role=candidate">立即注册</Link>
      </div>
      <Link className={styles.loginButton} href="/auth/login?role=candidate&redirect=/candidate/center">
        进入登录页
      </Link>
    </section>
  );
}

function NoticeTabs() {
  return (
    <section className={`${styles.card} ${styles.noticeCard}`} aria-label="公告与招聘会">
      <div className={styles.noticeTabs} role="tablist" aria-label="公告招聘会切换">
        <span className={`${styles.noticeTab} ${styles.noticeTabActive}`} role="tab" aria-selected="true">
          网站公告
        </span>
        <span className={styles.noticeTab} role="tab" aria-selected="false">
          招聘会
        </span>
      </div>
      <ul className={styles.noticeList}>
        {[...notices, ...fairNotices].map((notice) => (
          <li key={notice}>⌁ {notice}</li>
        ))}
      </ul>
    </section>
  );
}

function AdvertisementBand() {
  return (
    <section className={styles.adBand} aria-label="首页广告位">
      <div className={styles.primaryAd}>首页自定义 通栏广告位</div>
      <div className={styles.secondaryAds}>
        <div className={styles.secondaryAd}>免费注册简历 · 占位 CTA</div>
        <div className={styles.secondaryAd}>企业发布职位 · 占位 CTA</div>
      </div>
    </section>
  );
}

function RecommendationEmpty({ label }: { label: string }) {
  return (
    <article className={styles.companyCard}>
      <h3 className={styles.companyName}>{label}</h3>
      <p className={styles.muted}>运营位待配置或目标已失效；公开门户不会回退到敏感数据或假推荐。</p>
      <span className={styles.tag}>公开白名单</span>
    </article>
  );
}

function RecommendedCompanies({ items }: { items: PortalRecommendationItem[] }) {
  return (
    <section className={`${styles.card} ${styles.sectionCard}`} aria-label="推荐企业">
      <div className={styles.sectionHeader}>
        <h2 className={styles.sectionTitle}>明星企业</h2>
        <Link className={styles.moreLink} href="/companies">
          查看更多
        </Link>
      </div>
      <div className={styles.companyList}>
        {items.length === 0 ? <RecommendationEmpty label="明星企业待配置" /> : items.map((company) => (
          <article key={`${company.target_type}-${company.target_id}`} className={styles.companyCard}>
            <div className={styles.companyTop}>
              <div>
                <h3 className={styles.companyName}>{company.title}</h3>
                <p className={styles.muted}>
                  {company.city_code || '城市待配置'} · {company.summary || '公开企业摘要'}
                </p>
              </div>
              <span className={styles.tag}>认证企业</span>
            </div>
            <Link className={styles.moreLink} href={company.url}>进入企业公开页</Link>
          </article>
        ))}
      </div>
    </section>
  );
}

function HotJobs({ items }: { items: PortalRecommendationItem[] }) {
  return (
    <section className={`${styles.card} ${styles.sectionCard}`} aria-label="热招职位">
      <div className={styles.sectionHeader}>
        <h2 className={styles.sectionTitle}>热招职位</h2>
        <Link className={styles.moreLink} href="/jobs">
          查看更多
        </Link>
      </div>
      <div className={styles.jobGrid}>
        {items.length === 0 ? <RecommendationEmpty label="热招职位待配置" /> : items.map((job) => (
          <article key={`${job.target_type}-${job.target_id}`} className={styles.jobCard}>
            <div className={styles.jobTop}>
              <h3 className={styles.jobTitle}>{job.title}</h3>
              <span className={styles.salary}>公开推荐</span>
            </div>
            <p className={styles.muted}>
              {job.city_code || '城市待配置'} · {job.summary || '在线职位公开摘要'}
            </p>
            <div className={styles.tagRow}>
              {(job.tags.length === 0 ? ['公开字段'] : job.tags).map((tag) => <span key={tag} className={styles.tag}>{tag}</span>)}
            </div>
            <Link className={styles.moreLink} href={job.url}>查看公开详情</Link>
          </article>
        ))}
      </div>
    </section>
  );
}

function ScanCta() {
  return (
    <section className={styles.ctaStrip} aria-label="扫码 CTA">
      <div>
        <p className={styles.eyebrow}>LocalTalent</p>
        <h2 className={styles.sectionTitle}>海量职位任您选，可信发布快照为边界。</h2>
        <p className={styles.footerText}>公众号、小程序和 App 均为占位，不接真实外部能力。</p>
      </div>
      <span className={styles.qrMini}>QR</span>
      <span>扫码占位 · 求职更简单</span>
    </section>
  );
}

export function PortalHomePage({ hotJobs = [], featuredCompanies = [] }: PortalHomePageProps) {
  return (
    <main className={styles.home}>
      <section className={styles.heroGrid} aria-label="首页高保真首屏">
        <JobCategoryWall />
        <div className={styles.centerStage}>
          <section className={styles.banner} aria-label="首页首屏广告位">
            <p className={styles.eyebrow}>LocalTalent Portal</p>
            <h1 className={styles.title}>地方人才网首页，高保真门户首屏。</h1>
            <p className={styles.subtitle}>
              对标 demo 的模块密度和入口层级，但公开层只展示职位、企业、活动、资讯和发布快照白名单。
            </p>
          </section>
          <ServiceEntranceGrid />
        </div>
        <div className={styles.sidePanel}>
          <LoginCard />
          <NoticeTabs />
        </div>
      </section>

      <AdvertisementBand />
      <ScanCta />
      <section className={styles.recommendGrid} aria-label="公开推荐模块">
        <HotJobs items={hotJobs} />
        <RecommendedCompanies items={featuredCompanies} />
      </section>
    </main>
  );
}
