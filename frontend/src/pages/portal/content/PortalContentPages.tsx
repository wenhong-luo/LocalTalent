import Link from 'next/link';
import { highRiskServicePlaceholders, lowRiskServiceLinks } from '@/components/portal/portalLinks';
import styles from './PortalContentPages.module.css';
import type { PortalContent, PortalContentPage, PortalEvent, PortalEventPage } from './portalContentApi';

export type PublicChannelStatus = 'ready' | 'empty' | 'error';

export type ContentChannelConfig = {
  contentType: string;
  eyebrow: string;
  title: string;
  description: string;
  detailBasePath: string;
  badge: string;
};

export type ContentChannelInitialState = {
  status: PublicChannelStatus;
  query: {
    city_code?: string;
    page?: number;
    size?: number;
  };
  page?: PortalContentPage;
  message?: string;
  traceId?: string;
};

export type EventChannelInitialState = {
  status: PublicChannelStatus;
  query: {
    type_code?: string;
    city_code?: string;
    page?: number;
    size?: number;
  };
  page?: PortalEventPage;
  message?: string;
  traceId?: string;
};

type ContentChannelPageProps = {
  config: ContentChannelConfig;
  initialState: ContentChannelInitialState;
};

type JobFairListPageProps = {
  initialState: EventChannelInitialState;
};

type DetailProps = {
  content?: PortalContent;
  event?: PortalEvent;
  title: string;
  message?: string;
  traceId?: string;
};

const cityOptions = [
  ['310000', '上海'],
  ['110000', '北京'],
  ['320900', '盐城'],
  ['140100', '太原']
];

const eventTypeOptions = [
  ['onsite', '现场招聘会'],
  ['online', '网络招聘会'],
  ['campus', '校园招聘']
];

function cityLabel(cityCode: string): string {
  return cityOptions.find(([value]) => value === cityCode)?.[1] ?? cityCode;
}

function ContentCard({ content, detailBasePath }: { content: PortalContent; detailBasePath: string }) {
  return (
    <article className={styles.itemCard}>
      <h2 className={styles.itemTitle}>
        <Link className={styles.detailLink} href={`${detailBasePath}/${content.content_id}`}>
          {content.title || '未命名内容'}
        </Link>
      </h2>
      <p className={styles.muted}>{content.summary || '公开摘要待补充。'}</p>
      <div className={styles.tagRow}>
        <span className={styles.tag}>{content.content_type || 'content'}</span>
        <span className={styles.tag}>{content.city_code ? cityLabel(content.city_code) : '不限城市'}</span>
        <span className={styles.tag}>公开内容</span>
      </div>
    </article>
  );
}

function EventCard({ event }: { event: PortalEvent }) {
  return (
    <article className={styles.itemCard}>
      <h2 className={styles.itemTitle}>
        <Link className={styles.detailLink} href={`/job-fairs/${event.event_id}`}>
          {event.title || '未命名招聘会'}
        </Link>
      </h2>
      <p className={styles.muted}>
        {event.start_time || '时间待定'} 至 {event.end_time || '时间待定'} · {event.location || '地点待公开'}
      </p>
      <div className={styles.tagRow}>
        <span className={styles.tag}>{event.type_code || 'job-fair'}</span>
        <span className={styles.tag}>{event.city_code ? cityLabel(event.city_code) : '不限城市'}</span>
        <span className={styles.tag}>报名名单不公开</span>
      </div>
    </article>
  );
}

function PublicBoundarySideBar() {
  return (
    <aside className={styles.sideBar} aria-label="公开频道边界">
      <section className={`${styles.card} ${styles.sideCard}`}>
        <h2 className={styles.sectionTitle}>公开字段边界</h2>
        <ul className={styles.sideList}>
          <li>只展示运营公开内容与公开活动信息。</li>
          <li>不展示报名名单、参会个人明细或签到证据。</li>
          <li>不接真实地图、视频、直播、短信或微信通知。</li>
        </ul>
      </section>
      <section className={`${styles.card} ${styles.sideCard}`}>
        <h2 className={styles.sectionTitle}>风险池入口</h2>
        <div className={styles.tagRow}>
          <button className={styles.disabledButton} type="button" disabled aria-disabled="true">
            展位售卖：风险池
          </button>
          <button className={styles.disabledButton} type="button" disabled aria-disabled="true">
            真实支付：风险池
          </button>
        </div>
      </section>
    </aside>
  );
}

export function ContentChannelPage({ config, initialState }: ContentChannelPageProps) {
  const contents = initialState.page?.content_list ?? [];
  return (
    <main className={styles.page}>
      <section className={styles.hero} aria-label={`${config.title}频道首屏`}>
        <div>
          <p className={styles.eyebrow}>{config.eyebrow}</p>
          <h1 className={styles.title}>{config.title}</h1>
          <p className={styles.subtitle}>{config.description}</p>
        </div>
        <div className={styles.heroBadge}>{config.badge}</div>
      </section>

      <form className={`${styles.card} ${styles.filters}`} action={config.detailBasePath} method="get" aria-label={`${config.title}筛选`}>
        <label className={styles.filterLabel}>
          城市
          <select className={styles.select} name="city_code" defaultValue={initialState.query.city_code ?? ''}>
            <option value="">不限城市</option>
            {cityOptions.map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <input type="hidden" name="page" value="1" />
        <input type="hidden" name="size" value={initialState.query.size ?? 12} />
        <button className={styles.searchButton} type="submit">
          查询公开内容
        </button>
      </form>

      <section className={styles.grid}>
        <section className={`${styles.card} ${styles.listCard}`} aria-label={`${config.title}列表`}>
          <div className={styles.sectionHeader}>
            <div>
              <h2 className={styles.sectionTitle}>{config.title}列表</h2>
              <p className={styles.muted}>共 {initialState.page?.total ?? 0} 条公开内容。</p>
            </div>
            {initialState.traceId ? <span className={styles.tag}>trace {initialState.traceId}</span> : null}
          </div>
          {initialState.status === 'error' ? <div className={styles.empty}>{initialState.message ?? '公开内容暂时不可用。'}</div> : null}
          {initialState.status === 'empty' ? <div className={styles.empty}>暂无公开内容。</div> : null}
          {contents.length > 0 ? (
            <div className={styles.list}>
              {contents.map((content) => (
                <ContentCard key={content.content_id} content={content} detailBasePath={config.detailBasePath} />
              ))}
            </div>
          ) : null}
        </section>
        <PublicBoundarySideBar />
      </section>
    </main>
  );
}

export function JobFairListPage({ initialState }: JobFairListPageProps) {
  const events = initialState.page?.event_list ?? [];
  return (
    <main className={styles.page}>
      <section className={styles.hero} aria-label="招聘会频道首屏">
        <div>
          <p className={styles.eyebrow}>LocalTalent Job Fairs</p>
          <h1 className={styles.title}>招聘会公开展示</h1>
          <p className={styles.subtitle}>展示现场招聘会、网络招聘会和校园招聘公开信息。报名名单、参会个人明细、签到证据均不公开。</p>
        </div>
        <div className={styles.heroBadge}>招聘会公开频道</div>
      </section>

      <form className={`${styles.card} ${styles.filters}`} action="/job-fairs" method="get" aria-label="招聘会筛选">
        <label className={styles.filterLabel}>
          类型
          <select className={styles.select} name="type_code" defaultValue={initialState.query.type_code ?? ''}>
            <option value="">不限类型</option>
            {eventTypeOptions.map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <label className={styles.filterLabel}>
          城市
          <select className={styles.select} name="city_code" defaultValue={initialState.query.city_code ?? ''}>
            <option value="">不限城市</option>
            {cityOptions.map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </select>
        </label>
        <input type="hidden" name="page" value="1" />
        <input type="hidden" name="size" value={initialState.query.size ?? 12} />
        <button className={styles.searchButton} type="submit">
          查询招聘会
        </button>
      </form>

      <section className={styles.grid}>
        <section className={`${styles.card} ${styles.listCard}`} aria-label="招聘会列表">
          <div className={styles.sectionHeader}>
            <div>
              <h2 className={styles.sectionTitle}>招聘会列表</h2>
              <p className={styles.muted}>共 {initialState.page?.total ?? 0} 场公开招聘会。</p>
            </div>
            {initialState.traceId ? <span className={styles.tag}>trace {initialState.traceId}</span> : null}
          </div>
          {initialState.status === 'error' ? <div className={styles.empty}>{initialState.message ?? '招聘会暂时不可用。'}</div> : null}
          {initialState.status === 'empty' ? <div className={styles.empty}>暂无公开招聘会。</div> : null}
          {events.length > 0 ? (
            <div className={styles.list}>
              {events.map((event) => (
                <EventCard key={event.event_id} event={event} />
              ))}
            </div>
          ) : null}
        </section>
        <PublicBoundarySideBar />
      </section>
    </main>
  );
}

export function PortalContentDetailPage({ content, title, message, traceId }: DetailProps) {
  if (!content) {
    return (
      <main className={styles.detailPage}>
        <section className={`${styles.card} ${styles.detailHero}`}>
          <p className={styles.eyebrow}>Public Content</p>
          <h1 className={styles.title}>{title}暂时不可用</h1>
          <p className={styles.muted}>{message ?? '内容可能已下线。'}</p>
          {traceId ? <span className={styles.tag}>trace {traceId}</span> : null}
          <Link className={styles.detailLink} href="/">
            返回首页
          </Link>
        </section>
      </main>
    );
  }

  return (
    <main className={styles.detailPage}>
      <article className={`${styles.card} ${styles.detailHero}`} aria-label={`${title}详情`}>
        <p className={styles.eyebrow}>{content.content_type || 'content'}</p>
        <h1 className={styles.title}>{content.title || '未命名内容'}</h1>
        <p className={styles.muted}>{content.summary || '公开摘要待补充。'}</p>
        <div className={styles.tagRow}>
          <span className={styles.tag}>公开内容</span>
          <span className={styles.tag}>{content.city_code ? cityLabel(content.city_code) : '不限城市'}</span>
        </div>
        <section>
          <h2 className={styles.sectionTitle}>正文</h2>
          <p className={styles.bodyText}>{content.body_text || '正文待补充。'}</p>
        </section>
        {traceId ? <span className={styles.tag}>trace {traceId}</span> : null}
      </article>
    </main>
  );
}

export function JobFairDetailPage({ event, message, traceId }: DetailProps) {
  if (!event) {
    return (
      <main className={styles.detailPage}>
        <section className={`${styles.card} ${styles.detailHero}`}>
          <p className={styles.eyebrow}>Job Fair</p>
          <h1 className={styles.title}>招聘会暂时不可用</h1>
          <p className={styles.muted}>{message ?? '招聘会可能已下线。'}</p>
          {traceId ? <span className={styles.tag}>trace {traceId}</span> : null}
          <Link className={styles.detailLink} href="/job-fairs">
            返回招聘会列表
          </Link>
        </section>
      </main>
    );
  }

  return (
    <main className={styles.detailPage}>
      <article className={`${styles.card} ${styles.detailHero}`} aria-label="招聘会详情">
        <p className={styles.eyebrow}>{event.type_code || 'job-fair'}</p>
        <h1 className={styles.title}>{event.title || '未命名招聘会'}</h1>
        <p className={styles.subtitle}>
          {event.start_time || '时间待定'} 至 {event.end_time || '时间待定'} · {event.location || '地点待公开'}
        </p>
        <div className={styles.tagRow}>
          <span className={styles.tag}>公开招聘会</span>
          <span className={styles.tag}>报名名单不公开</span>
          <span className={styles.tag}>签到证据不公开</span>
        </div>
        <p className={styles.bodyText}>本页仅展示招聘会公开信息。参会企业名单、报名名单、签到证据和联系方式不在公开层展示。</p>
        {traceId ? <span className={styles.tag}>trace {traceId}</span> : null}
      </article>
    </main>
  );
}

export function MoreServicesPage() {
  return (
    <main className={styles.page}>
      <section className={styles.hero} aria-label="更多服务频道首屏">
        <div>
          <p className={styles.eyebrow}>More Services</p>
          <h1 className={styles.title}>更多服务与风险池入口</h1>
          <p className={styles.subtitle}>低风险服务提供稳定入口；地图、视频、直播、自由职业、求职登记、短信/微信通知等能力保持禁用占位。</p>
        </div>
        <div className={styles.heroBadge}>低风险优先</div>
      </section>

      <section className={`${styles.card} ${styles.listCard}`} aria-label="低风险更多服务">
        <h2 className={styles.sectionTitle}>低风险服务入口</h2>
        <div className={styles.serviceGrid}>
          {lowRiskServiceLinks.map((item) => (
            <Link key={item.label} className={styles.serviceCard} href={item.href}>
              <strong>{item.label}</strong>
              <span className={styles.muted}>公开频道或稳定筛选入口</span>
            </Link>
          ))}
          <Link className={styles.serviceCard} href="/articles/news">
            <strong>职场资讯</strong>
            <span className={styles.muted}>公开资讯频道</span>
          </Link>
          <Link className={styles.serviceCard} href="/articles/notices">
            <strong>网站公告</strong>
            <span className={styles.muted}>公开公告频道</span>
          </Link>
        </div>
      </section>

      <section className={`${styles.card} ${styles.listCard}`} aria-label="高风险更多服务占位">
        <h2 className={styles.sectionTitle}>高风险能力占位</h2>
        <div className={styles.serviceGrid}>
          {[
            ...highRiskServicePlaceholders,
            { label: '短信通知', reason: '真实短信通知后续评估' },
            { label: '微信通知', reason: '真实微信通知后续评估' }
          ].map((item) => (
            <div key={item.label} className={styles.disabledService}>
              <strong>{item.label}</strong>
              <span>{item.reason}</span>
              <button className={styles.disabledButton} type="button" disabled aria-disabled="true">
                禁用占位
              </button>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
