import Link from 'next/link';
import { PortalAdRailFrame } from '@/components/portal/PortalAdRailFrame';
import styles from './CompanySearchPage.module.css';
import type { CompanySearchQuery, PortalCompany, PortalCompanyPage } from './portalCompanyApi';

export type CompanySearchStatus = 'ready' | 'empty' | 'error';

export type CompanySearchInitialState = {
  status: CompanySearchStatus;
  query: CompanySearchQuery;
  page?: PortalCompanyPage;
  message?: string;
  traceId?: string;
};

type CompanySearchPageProps = {
  initialState: CompanySearchInitialState;
};

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

const natureOptions = [
  ['private', '民营'],
  ['state-owned', '国企'],
  ['foreign', '外资'],
  ['joint-venture', '合资']
];

const scaleOptions = [
  ['1-49', '少于 50 人'],
  ['50-100', '50-100 人'],
  ['100-500', '100-500 人'],
  ['500+', '500 人以上']
];

function initialLetter(name: string): string {
  return (name.trim().charAt(0) || '企').toUpperCase();
}

function CompanyFilters({ query }: { query: CompanySearchQuery }) {
  return (
    <form className={`${styles.card} ${styles.filters}`} action="/companies" method="get" aria-label="找企业筛选">
      <label className={styles.filterLabel}>
        关键词
        <input className={styles.input} name="keyword" defaultValue={query.keyword ?? ''} placeholder="企业名称/公开简介" />
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
        企业性质
        <select className={styles.select} name="nature_code" defaultValue={query.nature_code ?? ''}>
          <option value="">不限性质</option>
          {natureOptions.map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </label>
      <label className={styles.filterLabel}>
        规模
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
        认证
        <select className={styles.select} name="verified" defaultValue={query.verified ?? '1'}>
          <option value="1">认证企业</option>
          <option value="0">未认证不可公开</option>
        </select>
      </label>
      <button className={styles.searchButton} type="submit">
        查询企业
      </button>
    </form>
  );
}

function CompanyCard({ company }: { company: PortalCompany }) {
  return (
    <article className={styles.companyCard}>
      <span className={styles.avatar} aria-hidden="true">
        {initialLetter(company.company_name)}
      </span>
      <div>
        <h2 className={styles.companyName}>
          <Link className={styles.detailLink} href={`/companies/${company.company_id}`}>
            {company.company_name || '认证企业'}
          </Link>
        </h2>
        <p className={styles.muted}>
          {company.city_code || '不限地区'} · {company.industry_code || '行业待补充'} · {company.nature_code || '性质待补充'} ·{' '}
          {company.scale_code || '规模待补充'}
        </p>
        <p className={styles.muted}>{company.company_profile || '企业公开简介待补充。'}</p>
        <div className={styles.tagRow}>
          <span className={styles.tag}>认证企业</span>
          <span className={styles.tag}>公开主页</span>
          <span className={styles.tag}>无联系方式公开</span>
        </div>
      </div>
      <div className={styles.countBadge}>
        <span>{company.open_job_count}</span>
        <small>在招职位</small>
      </div>
    </article>
  );
}

function SideBar() {
  return (
    <aside className={styles.sideBar} aria-label="找企业侧栏">
      <section className={`${styles.card} ${styles.sideCard}`}>
        <h2 className={styles.sectionTitle}>企业公开边界</h2>
        <ul className={styles.sideList}>
          <li>只展示认证企业。</li>
          <li>只展示公开简介与公开属性。</li>
          <li>不展示内部联系方式、资质附件或审核资料。</li>
        </ul>
      </section>
      <section className={`${styles.card} ${styles.sideCard}`}>
        <h2 className={styles.sectionTitle}>高风险能力</h2>
        <button className={styles.disabledAction} type="button" disabled aria-disabled="true">
          受控找人才：风险池
        </button>
        <button className={styles.disabledAction} type="button" disabled aria-disabled="true">
          联系解锁：风险池
        </button>
      </section>
    </aside>
  );
}

export function CompanySearchPage({ initialState }: CompanySearchPageProps) {
  const companies = initialState.page?.company_list ?? [];

  return (
    <main className={styles.page} data-layout="portal-ad-rails" data-testid="company-search-page">
      <PortalAdRailFrame label="找企业">
        <section className={styles.hero} aria-label="找企业频道首屏">
          <div>
            <p className={styles.eyebrow}>LocalTalent Companies</p>
            <h1 className={styles.title}>找企业与企业公开主页</h1>
            <p className={styles.subtitle}>
              面向公开门户展示已认证企业、公开简介和在线职位聚合。企业内部联系方式、资质附件、审核资料和内部备注不进入公开层。
            </p>
          </div>
          <div className={styles.brandCard}>
            <span className={styles.brandMark}>企</span>
            <span>名企展示占位</span>
          </div>
        </section>

        <CompanyFilters query={initialState.query} />

        <section className={styles.grid}>
          <section className={`${styles.card} ${styles.listCard}`} aria-label="企业搜索结果">
            <div className={styles.sectionHeader}>
              <div>
                <h2 className={styles.sectionTitle}>企业列表</h2>
                <p className={styles.muted}>共 {initialState.page?.total ?? 0} 家认证企业，筛选 query 可分享。</p>
              </div>
              {initialState.traceId ? <span className={styles.tag}>trace {initialState.traceId}</span> : null}
            </div>

            {initialState.status === 'error' ? (
              <div className={styles.empty}>{initialState.message ?? '企业列表暂时不可用，请稍后重试。'}</div>
            ) : null}

            {initialState.status === 'empty' ? <div className={styles.empty}>暂无符合条件的认证企业。</div> : null}

            {companies.length > 0 ? (
              <div className={styles.companyList}>
                {companies.map((company) => (
                  <CompanyCard key={company.company_id} company={company} />
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

export function normalizeCompanySearchParams(searchParams: Record<string, string | string[] | undefined>): CompanySearchQuery {
  const first = (value: string | string[] | undefined): string | undefined => (Array.isArray(value) ? value[0] : value);
  const positiveInt = (value: string | undefined, fallback: number): number => {
    const parsed = Number.parseInt(value ?? '', 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
  };

  return {
    keyword: first(searchParams.keyword),
    city_code: first(searchParams.city_code),
    industry_code: first(searchParams.industry_code),
    nature_code: first(searchParams.nature_code),
    scale_code: first(searchParams.scale_code),
    verified: first(searchParams.verified) ?? '1',
    page: positiveInt(first(searchParams.page), 1),
    size: positiveInt(first(searchParams.size), 12)
  };
}
