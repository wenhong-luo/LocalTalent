import Link from 'next/link';
import styles from './PortalHomePage.module.css';

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

const featuredCompanies = [
  { name: '山西正力网络有限公司', city: '太原', industry: '互联网服务', jobs: 3 },
  { name: '唐文文化传媒有限公司', city: '北京', industry: '文化传媒', jobs: 5 },
  { name: '盐城市数据公司', city: '盐城', industry: '数据服务', jobs: 1 },
  { name: '北京渤星科技有限公司', city: '北京', industry: '科技服务', jobs: 2 }
];

const hotJobs = [
  { title: 'Java 后端工程师', city: '上海', salary: '12K~18K', exp: '3-5年', edu: '本科', company: '山西正力网络有限公司' },
  { title: '运营专员', city: '太原', salary: '5K~8K', exp: '1-3年', edu: '大专', company: '唐文文化传媒有限公司' },
  { title: '客服主管', city: '青岛', salary: '6K~9K', exp: '经验不限', edu: '学历不限', company: '胶州市彩霞超市店' },
  { title: 'UI 设计师', city: '北京', salary: '9K~14K', exp: '2年', edu: '本科', company: '北京渤星科技有限公司' }
];

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
        <span>密码登录占位</span>
        <span>忘记密码？</span>
      </div>
      <button className={styles.loginButton} type="button">
        进入登录页占位
      </button>
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

function RecommendedCompanies() {
  return (
    <section className={`${styles.card} ${styles.sectionCard}`} aria-label="推荐企业">
      <div className={styles.sectionHeader}>
        <h2 className={styles.sectionTitle}>明星企业</h2>
        <Link className={styles.moreLink} href="/companies">
          查看更多
        </Link>
      </div>
      <div className={styles.companyList}>
        {featuredCompanies.map((company) => (
          <article key={company.name} className={styles.companyCard}>
            <div className={styles.companyTop}>
              <div>
                <h3 className={styles.companyName}>{company.name}</h3>
                <p className={styles.muted}>
                  {company.city} · {company.industry}
                </p>
              </div>
              <span className={styles.tag}>认证企业</span>
            </div>
            <p className={styles.muted}>该企业共有 {company.jobs} 个职位热招</p>
          </article>
        ))}
      </div>
    </section>
  );
}

function HotJobs() {
  return (
    <section className={`${styles.card} ${styles.sectionCard}`} aria-label="热招职位">
      <div className={styles.sectionHeader}>
        <h2 className={styles.sectionTitle}>热招职位</h2>
        <Link className={styles.moreLink} href="/jobs">
          查看更多
        </Link>
      </div>
      <div className={styles.jobGrid}>
        {hotJobs.map((job) => (
          <article key={`${job.company}-${job.title}`} className={styles.jobCard}>
            <div className={styles.jobTop}>
              <h3 className={styles.jobTitle}>{job.title}</h3>
              <span className={styles.salary}>{job.salary}</span>
            </div>
            <p className={styles.muted}>
              {job.city} · {job.exp} · {job.edu}
            </p>
            <p className={styles.muted}>{job.company}</p>
            <div className={styles.tagRow}>
              <span className={styles.tag}>在线职位</span>
              <span className={styles.tag}>公开字段</span>
            </div>
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

export function PortalHomePage() {
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
        <HotJobs />
        <RecommendedCompanies />
      </section>
    </main>
  );
}
