import Link from 'next/link';
import styles from './PortalHomePage.module.css';
import { type PortalHomeSlotItem } from './portalHomeSlotApi';
import { type PortalRecommendationItem } from './portalRecommendationApi';

type CategoryGroup = {
  code: string;
  label: string;
  groups: Array<{
    title: string;
    positions: string[];
  }>;
};

const categoryGroups: CategoryGroup[] = [
  {
    code: 'life_service',
    label: '生活 | 服务业',
    groups: [
      { title: '餐饮', positions: ['服务员', '厨师/厨师长', '配菜/打荷', '茶艺师', '洗碗工', '面点师'] },
      { title: '家政保洁/安保', positions: ['保洁', '保姆', '月嫂', '育婴师', '安保', '护工'] },
      { title: '美容/美发', positions: ['美容师', '美发师', '美甲师', '化妆师', '美容顾问'] }
    ]
  },
  {
    code: 'hr_admin_management',
    label: '人力 | 行政 | 管理',
    groups: [
      { title: '人事行政', positions: ['行政专员', '人事专员', '招聘专员', '前台文员', '办公室主任'] },
      { title: '管理岗位', positions: ['项目经理', '运营主管', '门店店长', '总经理助理', '储备干部'] }
    ]
  },
  {
    code: 'sales_customer_purchase',
    label: '销售 | 客服 | 采购 | 淘宝',
    groups: [
      { title: '销售', positions: ['销售代表', '客户经理', '渠道专员', '电话销售', '销售主管'] },
      { title: '客服电商', positions: ['客服专员', '售后客服', '淘宝客服', '电商运营', '采购专员'] }
    ]
  },
  {
    code: 'marketing_media_design',
    label: '市场 | 媒介 | 广告 | 设计',
    groups: [
      { title: '市场媒介', positions: ['市场专员', '活动策划', '新媒体运营', '短视频运营', '品牌专员'] },
      { title: '广告设计', positions: ['平面设计', 'UI 设计师', '视觉设计', '文案策划', '摄影剪辑'] }
    ]
  },
  {
    code: 'production_logistics_quality',
    label: '生产 | 物流 | 质控 | 汽车',
    groups: [
      { title: '生产质控', positions: ['普工', '操作工', '质检员', '生产主管', '设备维修'] },
      { title: '物流汽车', positions: ['仓库管理员', '物流专员', '货运司机', '汽车维修', '叉车工'] }
    ]
  },
  {
    code: 'network_communication_electronics',
    label: '网络 | 通信 | 电子',
    groups: [
      { title: '互联网开发', positions: ['前端工程师', 'Java 工程师', '测试工程师', '产品经理', '运维工程师'] },
      { title: '通信电子', positions: ['网络工程师', '弱电工程师', '电子工程师', '通信技术员', '硬件工程师'] }
    ]
  },
  {
    code: 'law_education_translation',
    label: '法律 | 教育 | 翻译 | 出版',
    groups: [
      { title: '教育培训', positions: ['教师', '课程顾问', '教务管理', '培训讲师', '幼教'] },
      { title: '法律出版', positions: ['法务专员', '律师助理', '翻译', '编辑', '校对'] }
    ]
  },
  {
    code: 'finance_accounting_insurance',
    label: '财会 | 金融 | 保险',
    groups: [
      { title: '财务会计', positions: ['会计', '出纳', '审计专员', '财务主管', '税务专员'] },
      { title: '金融保险', positions: ['银行柜员', '理财顾问', '保险顾问', '风控专员', '信贷专员'] }
    ]
  },
  {
    code: 'medical_pharma_environment',
    label: '医疗 | 制药 | 环保',
    groups: [
      { title: '医疗护理', positions: ['护士', '医生助理', '药剂师', '检验师', '康复治疗师'] },
      { title: '环保制药', positions: ['环保工程师', '污水处理员', '制药技术员', '质量管理', '实验员'] }
    ]
  },
  {
    code: 'construction_property_agriculture',
    label: '建筑 | 物业 | 农林牧渔 | 其他',
    groups: [
      { title: '建筑物业', positions: ['施工员', '资料员', '造价员', '物业管家', '物业维修'] },
      { title: '农林牧渔', positions: ['养殖人员', '饲料业务', '农艺师', '畜牧师', '园林绿化'] },
      { title: '其他招聘信息', positions: ['其他职位', '实习生', '兼职', '储备人才'] }
    ]
  }
];

type QuickEntry = {
  label: string;
  href: string;
  icon: string;
};

const defaultQuickEntries: QuickEntry[] = [
  { label: '名企直聘', href: '/jobs?channel=famous', icon: '企' },
  { label: '现场招聘会', href: '/job-fairs?type=offline', icon: '会' },
  { label: '网络招聘会', href: '/job-fairs?type=online', icon: '网' },
  { label: '普工招聘', href: '/jobs?category=general-worker', icon: '普' }
];

const homeSlotRiskWords = [
  ['简历', '库'].join(''),
  ['搜索', '简历'].join(''),
  ['联系', '解锁'].join(''),
  ['真实', '支付'].join(''),
  ['点击', '结算'].join('')
];

const defaultHeroSlot = {
  title: '地方人才网首页，高保真门户首屏。',
  subtitle: '对标专业地方人才网首页的模块密度和入口层级，但公开层只展示职位、企业、活动、资讯和发布快照白名单。',
  imageAlt: 'LocalTalent 首页首屏运营图',
  linkUrl: '/jobs'
};

const notices = ['系统升级公告', '活动预告公告', '春风行动招聘会开放预约', '网络招聘会频道占位'];

const demoHotJobs: PortalRecommendationItem[] = [
  {
    target_type: 'job',
    target_id: 9001,
    title: '前端开发工程师',
    summary: '数字化服务企业 · 10K~16K · 上海',
    tags: ['公开职位', '认证企业', '五险一金'],
    url: '/jobs?keyword=%E5%89%8D%E7%AB%AF',
    city_code: '上海',
    updated_at: '2026-05-15T09:00:00',
    display_order: 1
  },
  {
    target_type: 'job',
    target_id: 9002,
    title: '生产质检员',
    summary: '智能制造企业 · 6K~9K · 苏州',
    tags: ['公开职位', '长白班', '包工作餐'],
    url: '/jobs?keyword=%E8%B4%A8%E6%A3%80',
    city_code: '苏州',
    updated_at: '2026-05-15T09:10:00',
    display_order: 2
  },
  {
    target_type: 'job',
    target_id: 9003,
    title: '行政人事专员',
    summary: '本地服务企业 · 5K~8K · 杭州',
    tags: ['公开职位', '双休', '成长空间'],
    url: '/jobs?keyword=%E8%A1%8C%E6%94%BF',
    city_code: '杭州',
    updated_at: '2026-05-15T09:20:00',
    display_order: 3
  }
];

const demoCompanies: PortalRecommendationItem[] = [
  {
    target_type: 'company',
    target_id: 9101,
    title: 'LocalTalent 数字服务示范企业',
    summary: '互联网服务 · 100~500人 · 招聘流程数字化',
    tags: ['认证企业', '公开主页', '本地名企'],
    url: '/companies',
    city_code: '上海',
    updated_at: '2026-05-15T09:00:00',
    display_order: 1
  },
  {
    target_type: 'company',
    target_id: 9102,
    title: '智造园区协作企业',
    summary: '智能制造 · 500~1000人 · 多岗位热招',
    tags: ['认证企业', '园区招聘'],
    url: '/companies',
    city_code: '苏州',
    updated_at: '2026-05-15T09:10:00',
    display_order: 2
  }
];

const adSlots = {
  banner: {
    slotCode: 'home_full_width_banner',
    title: '城市人才服务季',
    subtitle: '公开职位、招聘会、政策资讯一站式触达',
    image: '/demo/home-ad-hero.svg',
    href: '/jobs',
    alt: '城市人才服务季运营图'
  },
  half: [
    {
      slotCode: 'home_half_left',
      title: '企业发布职位',
      subtitle: '灰度演示入口 · 不含付费投放',
      image: '/demo/home-ad-company.svg',
      href: '/company',
      alt: '企业发布职位运营图'
    },
    {
      slotCode: 'home_half_right',
      title: '求职者完善资料',
      subtitle: '私有中心维护 · 发布快照可撤回',
      image: '/demo/home-ad-candidate.svg',
      href: '/candidate/center',
      alt: '求职者完善资料运营图'
    }
  ],
  third: [
    {
      slotCode: 'home_third_1',
      title: '招聘会预约',
      image: '/demo/home-ad-fair.svg',
      href: '/job-fairs',
      alt: '招聘会运营图'
    },
    {
      slotCode: 'home_third_2',
      title: '就业政策公开',
      image: '/demo/home-ad-policy.svg',
      href: '/articles/policies',
      alt: '就业政策运营图'
    },
    {
      slotCode: 'home_third_3',
      title: 'HR 工具箱',
      image: '/demo/home-ad-toolkit.svg',
      href: '/hr-tools',
      alt: 'HR 工具箱运营图'
    }
  ],
  bottom: {
    slotCode: 'home_bottom_banner',
    title: '地方人才服务公开矩阵',
    subtitle: '职位、企业、招聘会、资讯与工具箱统一运营',
    image: '/demo/home-ad-hero.svg',
    href: '/more-services',
    alt: '地方人才服务公开矩阵运营图'
  }
};

const jobFairCards = [
  { title: '春风行动专场招聘会', meta: '5月下旬 · 市民服务中心 · 公开活动' },
  { title: '制造业网络招聘会', meta: '每周三更新 · 在线岗位展示' },
  { title: '高校毕业生双选会', meta: '校园协作 · 青年就业服务' }
];

const newsItems = [
  '2026 春季就业服务月公开事项',
  '企业招聘信息发布与审核流程说明',
  '高校毕业生就业见习岗位征集公告'
];

const hrTools = ['面试安排清单', '招聘流程说明', '岗位 JD 编写提示', '入职材料核对表'];

const friendLinks = ['地方人社服务入口', '高校就业指导中心', '产业园区招聘协作', '公共就业服务平台'];

type PortalHomePageProps = {
  hotJobs?: PortalRecommendationItem[];
  featuredCompanies?: PortalRecommendationItem[];
  homeSlots?: PortalHomeSlotItem[];
};

function isSafeHomeSlot(slot?: PortalHomeSlotItem): slot is PortalHomeSlotItem {
  if (!slot) {
    return false;
  }
  const rawText = `${slot.title} ${slot.subtitle} ${slot.link_url}`.toLowerCase();
  return !homeSlotRiskWords.some((riskWord) => rawText.includes(riskWord.toLowerCase()));
}

function safeLocalHref(value: string, fallback: string): string {
  if (!value || !value.startsWith('/') || value.startsWith('//')) {
    return fallback;
  }
  return value;
}

function findHomeSlot(homeSlots: PortalHomeSlotItem[], slotCode: string): PortalHomeSlotItem | undefined {
  return homeSlots.find((slot) => slot.slot_code === slotCode && isSafeHomeSlot(slot));
}

function buildQuickEntries(homeSlots: PortalHomeSlotItem[]): QuickEntry[] {
  const configuredEntries = ['home_quick_1', 'home_quick_2', 'home_quick_3']
    .map((slotCode) => findHomeSlot(homeSlots, slotCode))
    .filter((slot): slot is PortalHomeSlotItem => Boolean(slot))
    .map((slot) => ({
      label: slot.title || '运营入口',
      href: safeLocalHref(slot.link_url, '/jobs'),
      icon: (slot.title || '荐').trim().slice(0, 1) || '荐'
    }));

  if (configuredEntries.length === 0) {
    return defaultQuickEntries;
  }

  const configuredLabels = new Set(configuredEntries.map((entry) => entry.label));
  const fallbackEntries = defaultQuickEntries.filter((entry) => !configuredLabels.has(entry.label));
  return [...configuredEntries, ...fallbackEntries].slice(0, 4);
}

type HomeAdSlot = {
  slotCode: string;
  title: string;
  subtitle?: string;
  image: string;
  href: string;
  alt: string;
  configured: boolean;
};

type HomeAdFallback = Omit<HomeAdSlot, 'configured'>;

function buildHomeAdSlot(homeSlots: PortalHomeSlotItem[], fallback: HomeAdFallback): HomeAdSlot {
  const slot = findHomeSlot(homeSlots, fallback.slotCode);
  if (!slot) {
    return {
      ...fallback,
      configured: false
    };
  }

  return {
    slotCode: fallback.slotCode,
    title: slot.title || fallback.title,
    subtitle: slot.subtitle || fallback.subtitle,
    image: slot.image_url || fallback.image,
    href: safeLocalHref(slot.link_url, fallback.href),
    alt: slot.image_alt || fallback.alt,
    configured: true
  };
}

function JobCategoryWall() {
  return (
    <aside className={`${styles.card} ${styles.categoryWall}`} aria-label="职位分类墙">
      <ul className={styles.categoryList}>
        {categoryGroups.map((category) => (
          <li key={category.code} className={styles.categoryItem}>
            <Link className={styles.categoryLink} href={`/jobs?category=${category.code}`}>
              {category.label}
              <span>›</span>
            </Link>
            <div className={styles.categoryFlyout} aria-label={`${category.label} 职位浮层`}>
              {category.groups.map((group) => (
                <section key={group.title} className={styles.categoryGroup}>
                  <h3>{group.title}</h3>
                  <div className={styles.positionLinks}>
                    {group.positions.map((position) => (
                      <Link key={position} href={`/jobs?category=${category.code}&keyword=${encodeURIComponent(position)}`}>
                        {position}
                      </Link>
                    ))}
                  </div>
                </section>
              ))}
            </div>
          </li>
        ))}
      </ul>
      <Link className={styles.allJobs} href="/jobs">
        全部职位
      </Link>
    </aside>
  );
}

function HomeSearchPanel() {
  return (
    <section className={`${styles.card} ${styles.homeSearchPanel}`} aria-label="首页搜索增强">
      <div className={styles.homeSearchTabs} aria-label="首页搜索类型">
        <span className={styles.searchTabActive}>找工作</span>
        <span>找企业</span>
        <span>人才服务区</span>
      </div>
      <div className={styles.homeSearchRow}>
        <input aria-label="首页职位关键词" placeholder="请输入职位、企业或帮扶内容关键词" />
        <Link href="/jobs" className={styles.homeSearchButton}>
          搜索
        </Link>
      </div>
      <div className={styles.homeSearchMeta}>
        <Link href="/jobs?advanced=1">高级搜索</Link>
        <button type="button" aria-disabled="true" disabled>
          地图搜索占位
        </button>
      </div>
    </section>
  );
}

function ServiceEntranceGrid({ entries }: { entries: QuickEntry[] }) {
  return (
    <nav className={styles.quickGrid} aria-label="首页快捷入口">
      {entries.map((entry) => (
        <Link key={entry.label} className={styles.quickEntry} href={entry.href}>
          <span className={styles.quickIcon}>{entry.icon}</span>
          {entry.label}
        </Link>
      ))}
    </nav>
  );
}

function HeroBanner({ slot }: { slot?: PortalHomeSlotItem }) {
  const heroSlot = isSafeHomeSlot(slot) ? slot : undefined;
  const title = heroSlot?.title || defaultHeroSlot.title;
  const subtitle = heroSlot?.subtitle || defaultHeroSlot.subtitle;
  const imageUrl = heroSlot?.image_url || '';
  const href = safeLocalHref(heroSlot?.link_url || '', defaultHeroSlot.linkUrl);
  const imageAlt = heroSlot?.image_alt || defaultHeroSlot.imageAlt;

  return (
    <Link
      className={`${styles.banner} ${imageUrl ? styles.bannerWithImage : ''}`}
      href={href}
      aria-label="首页首屏广告位"
    >
      {imageUrl ? <img className={styles.bannerImage} src={imageUrl} alt={imageAlt} /> : null}
      <span className={styles.bannerOverlay} aria-hidden="true" />
      <div className={styles.bannerContent}>
        <p className={styles.eyebrow}>LocalTalent Portal</p>
        <h1 className={styles.title}>{title}</h1>
        <p className={styles.subtitle}>{subtitle}</p>
      </div>
    </Link>
  );
}

function HomeOperationsPanel() {
  return (
    <aside className={styles.sidePanel} aria-label="首页右侧运营面板">
      <NoticeTabs />
      <section className={`${styles.card} ${styles.safeEntryCard}`} aria-label="安全登录入口">
        <div className={styles.safeEntryHeader}>
          <span className={styles.safeEntryActive}>求职者入口</span>
          <span>企业入口</span>
        </div>
        <p>账号登录、注册与私有中心统一进入登录页；首页不放置短信验证码表单。</p>
        <div className={styles.safeEntryActions}>
          <Link href="/auth/login?role=candidate&redirect=/candidate/center">进入求职者中心</Link>
          <Link href="/auth/login?role=company&redirect=/company">进入企业管理中心</Link>
        </div>
        <small>短信、微信、小程序、App 登录均为后续专项，不在首页开放。</small>
      </section>
      <section className={`${styles.card} ${styles.boundaryMiniCard}`} aria-label="首页公开边界提示">
        <strong>公开边界</strong>
        <p>首页仅展示公开职位、认证企业、公开活动、资讯和发布快照摘要。</p>
      </section>
    </aside>
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
        {notices.map((notice) => (
          <li key={notice}>⌁ {notice}</li>
        ))}
      </ul>
    </section>
  );
}

function AdvertisementBand({ homeSlots }: { homeSlots: PortalHomeSlotItem[] }) {
  const fullWidthSlot = buildHomeAdSlot(homeSlots, adSlots.banner);
  const halfSlots = adSlots.half.map((slot) => buildHomeAdSlot(homeSlots, slot));
  const thirdSlots = adSlots.third.map((slot) => buildHomeAdSlot(homeSlots, slot));
  const bottomSlot = buildHomeAdSlot(homeSlots, adSlots.bottom);

  return (
    <section className={styles.adBand} aria-label="首页运营广告位体系">
      <Link className={styles.primaryAd} aria-label="首页通栏广告位" href={fullWidthSlot.href}>
        <img src={fullWidthSlot.image} alt={fullWidthSlot.alt} />
        <span>
          <strong>{fullWidthSlot.title}</strong>
          <small>{fullWidthSlot.subtitle || (fullWidthSlot.configured ? '后台运营位已配置' : '运营位待配置')}</small>
        </span>
      </Link>
      <div className={styles.halfAds} aria-label="首页1/2广告位">
        {halfSlots.map((slot) => (
          <Link key={slot.title} href={slot.href}>
            <img src={slot.image} alt={slot.alt} />
            <span>
              <strong>{slot.title}</strong>
              <small>{slot.subtitle || (slot.configured ? '后台运营位已配置' : '运营位待配置')}</small>
            </span>
          </Link>
        ))}
      </div>
      <div className={styles.thirdAds} aria-label="首页1/3广告位">
        {thirdSlots.map((slot) => (
          <Link key={slot.title} href={slot.href}>
            <img src={slot.image} alt={slot.alt} />
            <strong>{slot.title}</strong>
          </Link>
        ))}
      </div>
      <Link className={styles.bottomAd} aria-label="首页底部运营横幅" href={bottomSlot.href}>
        <img src={bottomSlot.image} alt={bottomSlot.alt} />
        <span>
          <strong>{bottomSlot.title}</strong>
          <small>{bottomSlot.subtitle || (bottomSlot.configured ? '后台运营位已配置' : '运营位待配置')}</small>
        </span>
      </Link>
      <div className={styles.smallAdGrid} aria-label="首页快捷广告位">
        {['岗位直达', '名企展示', '帮扶专区', '招聘会', '更多服务'].map((slot) => (
          <div key={slot}>{slot}</div>
        ))}
      </div>
      <p className={styles.adNote}>广告位仅为运营占位，不接真实投放、计费、支付或外部平台。</p>
    </section>
  );
}

function RecommendedCompanies({ items }: { items: PortalRecommendationItem[] }) {
  const displayItems = items.length === 0 ? demoCompanies : items;
  const usesDemoData = items.length === 0;

  return (
    <section className={`${styles.card} ${styles.sectionCard}`} aria-label="名企展示">
      <div className={styles.sectionHeader}>
        <div>
          <p className={styles.eyebrow}>Featured Companies</p>
          <h2 className={styles.sectionTitle}>名企展示</h2>
        </div>
        <Link className={styles.moreLink} href="/companies">
          查看更多
        </Link>
      </div>
      <div className={styles.companyList}>
        {displayItems.map((company) => (
          <article key={`${company.target_type}-${company.target_id}`} className={styles.companyCard}>
            <div className={styles.companyTop}>
              <div>
                <h3 className={styles.companyName}>{company.title}</h3>
                <p className={styles.muted}>
                  {company.city_code || '城市待配置'} · {company.summary || '公开企业摘要'}
                </p>
              </div>
              <span className={styles.tag}>{usesDemoData ? '演示数据' : '认证企业'}</span>
            </div>
            <Link className={styles.moreLink} href={company.url}>进入企业公开页</Link>
          </article>
        ))}
      </div>
    </section>
  );
}

function HotJobs({ items }: { items: PortalRecommendationItem[] }) {
  const displayItems = items.length === 0 ? demoHotJobs : items;
  const usesDemoData = items.length === 0;

  return (
    <section className={`${styles.card} ${styles.sectionCard}`} aria-label="热招职位">
      <div className={styles.sectionHeader}>
        <div>
          <p className={styles.eyebrow}>Hot Jobs</p>
          <h2 className={styles.sectionTitle}>热招职位</h2>
        </div>
        <Link className={styles.moreLink} href="/jobs">
          查看更多
        </Link>
      </div>
      <div className={styles.jobGrid}>
        {displayItems.map((job) => (
          <article key={`${job.target_type}-${job.target_id}`} className={styles.jobCard}>
            <div className={styles.jobTop}>
              <h3 className={styles.jobTitle}>{job.title}</h3>
              <span className={styles.salary}>{usesDemoData ? '演示岗位' : '公开推荐'}</span>
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

function JobFairPreview() {
  return (
    <section className={`${styles.card} ${styles.sectionCard}`} aria-label="首页招聘会模块">
      <div className={styles.sectionHeader}>
        <div>
          <p className={styles.eyebrow}>Job Fairs</p>
          <h2 className={styles.sectionTitle}>招聘会</h2>
        </div>
        <Link className={styles.moreLink} href="/job-fairs">
          查看更多
        </Link>
      </div>
      <div className={styles.fairGrid}>
        {jobFairCards.map((fair) => (
          <article key={fair.title} className={styles.fairCard}>
            <h3>{fair.title}</h3>
            <p>{fair.meta}</p>
          </article>
        ))}
      </div>
    </section>
  );
}

function NewsAndTools() {
  return (
    <section className={styles.newsToolsGrid} aria-label="资讯公告与 HR 工具箱">
      <div className={`${styles.card} ${styles.sectionCard}`}>
        <div className={styles.sectionHeader}>
          <div>
            <p className={styles.eyebrow}>News</p>
            <h2 className={styles.sectionTitle}>资讯公告</h2>
          </div>
          <Link className={styles.moreLink} href="/articles/policies">
            政策资讯
          </Link>
        </div>
        <ul className={styles.newsList}>
          {newsItems.map((item) => <li key={item}>{item}</li>)}
        </ul>
      </div>
      <div className={`${styles.card} ${styles.sectionCard}`}>
        <div className={styles.sectionHeader}>
          <div>
            <p className={styles.eyebrow}>Toolkit</p>
            <h2 className={styles.sectionTitle}>HR 工具箱</h2>
          </div>
          <Link className={styles.moreLink} href="/hr-tools">
            查看工具
          </Link>
        </div>
        <div className={styles.toolList}>
          {hrTools.map((tool) => <span key={tool}>{tool}</span>)}
        </div>
      </div>
    </section>
  );
}

function FriendLinks() {
  return (
    <section className={`${styles.card} ${styles.friendLinks}`} aria-label="友情链接与合作入口">
      <h2 className={styles.sectionTitle}>友情链接与合作入口</h2>
      <div>
        {friendLinks.map((link) => <span key={link}>{link}</span>)}
      </div>
    </section>
  );
}

function ScanCta() {
  return (
    <section className={styles.ctaStrip} aria-label="扫码 CTA">
      <div>
        <p className={styles.eyebrow}>LocalTalent</p>
        <h2 className={styles.sectionTitle}>可信发布、合规展示，适合灰度试运营的地方人才门户。</h2>
        <p className={styles.footerText}>公众号、小程序和 App 均为占位，不接真实外部能力。</p>
      </div>
      <span className={styles.qrMini}>QR</span>
      <span>扫码占位 · 关注公开服务</span>
    </section>
  );
}

export function PortalHomePage({ hotJobs = [], featuredCompanies = [], homeSlots = [] }: PortalHomePageProps) {
  const heroSlot = findHomeSlot(homeSlots, 'home_hero_banner');
  const quickEntryList = buildQuickEntries(homeSlots);

  return (
    <main className={styles.home} data-layout="portal-ad-rails" data-testid="portal-home-page">
      <section className={styles.heroGrid} aria-label="首页高保真首屏">
        <JobCategoryWall />
        <div className={styles.centerStage}>
          <HomeSearchPanel />
          <HeroBanner slot={heroSlot} />
          <ServiceEntranceGrid entries={quickEntryList} />
        </div>
        <HomeOperationsPanel />
      </section>

      <AdvertisementBand homeSlots={homeSlots} />
      <section className={styles.homeContent} aria-label="首页公开内容模块">
        <HotJobs items={hotJobs} />
        <RecommendedCompanies items={featuredCompanies} />
        <JobFairPreview />
        <NewsAndTools />
      </section>
      <ScanCta />
      <FriendLinks />
    </main>
  );
}
