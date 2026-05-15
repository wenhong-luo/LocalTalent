import Link from 'next/link';
import { PortalAdRailFrame } from '@/components/portal/PortalAdRailFrame';
import styles from './JobSearchPage.module.css';
import { salaryText, type JobSearchQuery, type PortalJob, type PortalJobPage } from './portalJobApi';

export type JobSearchStatus = 'ready' | 'empty' | 'error';

export type JobSearchInitialState = {
  status: JobSearchStatus;
  query: JobSearchQuery;
  page?: PortalJobPage;
  message?: string;
  traceId?: string;
};

type JobSearchPageProps = {
  initialState: JobSearchInitialState;
  topic?: {
    eyebrow?: string;
    title?: string;
    subtitle?: string;
    badge?: string;
  };
};

const categoryOptions = [
  ['software', '互联网/软件'],
  ['sales', '销售/客服'],
  ['operations', '运营/市场'],
  ['manufacturing', '生产/物流'],
  ['general-worker', '普工招聘']
];

const cityOptions = [
  ['310000', '上海'],
  ['110000', '北京'],
  ['320900', '盐城'],
  ['140100', '太原']
];

const industryOptions = [
  ['internet', '互联网服务'],
  ['manufacturing', '制造业'],
  ['service', '生活服务'],
  ['culture', '文化传媒']
];

const scaleOptions = [
  ['1-49', '少于 50 人'],
  ['50-100', '50-100 人'],
  ['100-500', '100-500 人'],
  ['500+', '500 人以上']
];

const deferredFilters = ['经验', '学历', '企业性质'];

function queryValue(value: string | number | undefined): string {
  return typeof value === 'undefined' ? '' : String(value);
}

function JobFilters({ query }: { query: JobSearchQuery }) {
  return (
    <form className={`${styles.card} ${styles.filters}`} action="/jobs" method="get" aria-label="找工作筛选">
      <label className={styles.filterLabel}>
        关键词
        <input className={styles.input} name="keyword" defaultValue={query.keyword ?? ''} placeholder="职位/企业关键词" />
      </label>
      <label className={styles.filterLabel}>
        地区
        <select className={styles.select} name="city_code" defaultValue={query.city_code ?? ''}>
          <option value="">不限地区</option>
          {cityOptions.map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </label>
      <label className={styles.filterLabel}>
        职类
        <select className={styles.select} name="category_code" defaultValue={query.category_code ?? ''}>
          <option value="">不限职类</option>
          {categoryOptions.map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </label>
      <label className={styles.filterLabel}>
        薪资
        <select className={styles.select} name="salary_range" defaultValue={query.salary_range ?? ''}>
          <option value="">不限薪资</option>
          <option value="0-5000">5K 以下</option>
          <option value="5000-8000">5K-8K</option>
          <option value="8000-12000">8K-12K</option>
          <option value="12000-20000">12K-20K</option>
          <option value="20000-">20K 以上</option>
        </select>
      </label>
      <label className={styles.filterLabel}>
        行业
        <select className={styles.select} name="industry_code" defaultValue={query.industry_code ?? ''}>
          <option value="">不限行业</option>
          {industryOptions.map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </label>
      <label className={styles.filterLabel}>
        企业规模
        <select className={styles.select} name="scale_code" defaultValue={query.scale_code ?? ''}>
          <option value="">不限规模</option>
          {scaleOptions.map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </label>
      <label className={styles.filterLabel}>
        更新时间
        <select className={styles.select} name="updated_within" defaultValue={query.updated_within ?? ''}>
          <option value="">不限时间</option>
          <option value="3">近 3 天</option>
          <option value="7">近 7 天</option>
          <option value="30">近 30 天</option>
        </select>
      </label>
      <label className={styles.filterLabel}>
        排序
        <select className={styles.select} name="sort" defaultValue={query.sort ?? 'updated_desc'}>
          <option value="updated_desc">最新发布</option>
          <option value="salary_desc">薪资从高到低</option>
          <option value="salary_asc">薪资从低到高</option>
        </select>
      </label>
      <input type="hidden" name="experience_code" value={query.experience_code ?? ''} />
      <input type="hidden" name="education_code" value={query.education_code ?? ''} />
      <input type="hidden" name="company_nature" value={query.company_nature ?? ''} />
      <button className={styles.searchButton} type="submit">
        查询职位
      </button>
      <p className={styles.deferredNote}>
        {deferredFilters.join('、')}筛选入口已保留为稳定 query，因当前职位表暂无结构化字段，本轮不参与真实过滤。
      </p>
    </form>
  );
}

function JobCard({ job }: { job: PortalJob }) {
  return (
    <article className={styles.jobCard}>
      <div className={styles.jobTop}>
        <div>
          <h2 className={styles.jobTitle}>
            <Link className={styles.detailLink} href={`/jobs/${job.job_id}`}>
              {job.title || '未命名职位'}
            </Link>
          </h2>
          <p className={styles.muted}>
            {job.company_name || '认证企业'} · {job.city_code || '不限地区'} · {job.category_code || '未分类'}
          </p>
        </div>
        <span className={styles.salary}>{salaryText(job)}</span>
      </div>
      <p className={styles.muted}>{job.job_desc ? `${job.job_desc.slice(0, 96)}${job.job_desc.length > 96 ? '...' : ''}` : '职位描述待补充'}</p>
      <div className={styles.tagRow}>
        <span className={styles.tag}>在线职位</span>
        <span className={styles.tag}>认证企业</span>
        <span className={styles.tag}>公开字段</span>
      </div>
      <div className={styles.actions}>
        <Link className={styles.detailLink} href={`/jobs/${job.job_id}`}>
          查看职位详情
        </Link>
        <button className={styles.disabledAction} type="button" disabled aria-disabled="true">
          投递占位
        </button>
        <button className={styles.disabledAction} type="button" disabled aria-disabled="true">
          收藏占位
        </button>
      </div>
    </article>
  );
}

function SideBar() {
  return (
    <aside className={styles.sideBar} aria-label="找工作侧栏">
      <section className={`${styles.card} ${styles.sideCard}`} aria-label="扫码求职 CTA">
        <span className={styles.qr}>QR</span>
        <h2 className={styles.sectionTitle}>扫码求职占位</h2>
        <p className={styles.muted}>公众号、小程序、App 均为占位，不接真实外部能力。</p>
      </section>
      <section className={`${styles.card} ${styles.sideCard}`} aria-label="安全边界提示">
        <h2 className={styles.sectionTitle}>公开职位边界</h2>
        <ul className={styles.sideList}>
          <li>仅展示在线职位。</li>
          <li>仅展示认证企业职位。</li>
          <li>不展示企业内部材料。</li>
        </ul>
      </section>
    </aside>
  );
}

export function JobSearchPage({ initialState, topic }: JobSearchPageProps) {
  const jobs = initialState.page?.job_list ?? [];
  const activeTab = initialState.query.famous === '1' ? 'famous' : 'all';

  return (
    <main className={styles.page} data-layout="portal-ad-rails" data-testid="job-search-page">
      <PortalAdRailFrame label="找工作">
        <section className={styles.hero} aria-label="找工作频道首屏">
          <div>
            <p className={styles.eyebrow}>{topic?.eyebrow ?? 'LocalTalent Jobs'}</p>
            <h1 className={styles.title}>{topic?.title ?? '找工作与名企直聘'}</h1>
            <p className={styles.subtitle}>
              {topic?.subtitle
                ?? '面向公开门户的职位搜索页。列表只读取在线职位和认证企业职位，地图找工作、会员置顶与付费推广均不在本轮实现。'}
            </p>
          </div>
          <div className={styles.scanCard} aria-label="扫码求职 CTA">
            <span className={styles.qr}>QR</span>
            <strong>{topic?.badge ?? '求职更简单'}</strong>
            <span className={styles.muted}>扫码入口占位</span>
          </div>
        </section>

        <nav className={styles.tabs} aria-label="职位频道">
          <Link className={activeTab === 'all' ? styles.tabActive : styles.tab} href="/jobs">
            全部职位
          </Link>
          <Link className={activeTab === 'famous' ? styles.tabActive : styles.tab} href="/jobs/famous">
            名企直聘
          </Link>
        </nav>

        <JobFilters query={initialState.query} />

        <section className={styles.contentGrid}>
          <section className={`${styles.card} ${styles.listCard}`} aria-label="职位搜索结果">
            <div className={styles.sectionHeader}>
              <div>
                <h2 className={styles.sectionTitle}>职位列表</h2>
                <p className={styles.muted}>共 {initialState.page?.total ?? 0} 个公开职位，筛选 query 可分享。</p>
              </div>
              {initialState.traceId ? <span className={styles.tag}>trace {initialState.traceId}</span> : null}
            </div>

            {initialState.status === 'error' ? (
              <div className={styles.empty}>{initialState.message ?? '职位列表暂时不可用，请稍后重试。'}</div>
            ) : null}

            {initialState.status === 'empty' ? <div className={styles.empty}>暂无符合条件的在线职位。</div> : null}

            {jobs.length > 0 ? (
              <div className={styles.jobList}>
                {jobs.map((job) => (
                  <JobCard key={job.job_id} job={job} />
                ))}
              </div>
            ) : null}
          </section>
          <SideBar />
        </section>
      </PortalAdRailFrame>
    </main>
  );
}

export function normalizeJobSearchParams(searchParams: Record<string, string | string[] | undefined>): JobSearchQuery {
  const first = (value: string | string[] | undefined): string | undefined => (Array.isArray(value) ? value[0] : value);
  const positiveInt = (value: string | undefined, fallback: number): number => {
    const parsed = Number.parseInt(value ?? '', 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
  };

  return {
    keyword: first(searchParams.keyword),
    city_code: first(searchParams.city_code),
    category_code: first(searchParams.category_code),
    salary_range: first(searchParams.salary_range),
    experience_code: first(searchParams.experience_code),
    industry_code: first(searchParams.industry_code),
    scale_code: first(searchParams.scale_code),
    company_nature: first(searchParams.company_nature),
    education_code: first(searchParams.education_code),
    updated_within: first(searchParams.updated_within),
    sort: first(searchParams.sort),
    famous: first(searchParams.famous),
    page: positiveInt(first(searchParams.page), 1),
    size: positiveInt(first(searchParams.size), 12)
  };
}
