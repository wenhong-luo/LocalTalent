import Link from 'next/link';
import type { PortalJobPage } from '@/pages/portal/jobs/portalJobApi';
import styles from './PortalContentPages.module.css';
import type { PortalContentPage, PortalEventPage } from './portalContentApi';

type CampusLandingPageProps = {
  jobs?: PortalJobPage;
  events?: PortalEventPage;
  contents?: PortalContentPage;
};

export function CampusLandingPage({ jobs, events, contents }: CampusLandingPageProps) {
  const jobList = jobs?.job_list ?? [];
  const eventList = events?.event_list ?? [];
  const contentList = contents?.content_list ?? [];

  return (
    <main className={styles.page}>
      <section className={styles.hero} aria-label="校园招聘门户首屏">
        <div>
          <p className={styles.eyebrow}>Campus Recruiting</p>
          <h1 className={styles.title}>校园招聘</h1>
          <p className={styles.subtitle}>
            公开展示校园职位入口、校园招聘会和校园资讯。当前仅使用公开职位、公开活动和公开内容，不接真实校招报名、短信、微信或外部校园系统。
          </p>
        </div>
        <div className={styles.heroBadge}>校园公开频道</div>
      </section>

      <section className={styles.grid}>
        <section className={`${styles.card} ${styles.listCard}`} aria-label="校园招聘公开入口">
          <h2 className={styles.sectionTitle}>校园招聘入口</h2>
          <div className={styles.serviceGrid}>
            <Link className={styles.serviceCard} href="/campus/jobs">
              <strong>校园职位</strong>
              <span className={styles.muted}>复用公开职位接口，结构化校招筛选后续演进。</span>
            </Link>
            <Link className={styles.serviceCard} href="/campus/elections">
              <strong>校园宣讲 / 双选会</strong>
              <span className={styles.muted}>只展示公开活动信息，不展示报名名单。</span>
            </Link>
            <Link className={styles.serviceCard} href="/articles/news?content_type=campus">
              <strong>校园资讯</strong>
              <span className={styles.muted}>公开资讯入口，不包含个人明细。</span>
            </Link>
          </div>
        </section>
        <aside className={styles.sideBar} aria-label="校园招聘边界">
          <section className={`${styles.card} ${styles.sideCard}`}>
            <h2 className={styles.sectionTitle}>边界说明</h2>
            <ul className={styles.sideList}>
              <li>不公开学生联系方式、简历正文或附件。</li>
              <li>不接真实短信、微信、小程序、App。</li>
              <li>校园报名与外部系统同步仍在风险池。</li>
            </ul>
          </section>
        </aside>
      </section>

      <section className={`${styles.card} ${styles.listCard}`} aria-label="校园公开聚合">
        <div className={styles.sectionHeader}>
          <div>
            <h2 className={styles.sectionTitle}>校园公开信息聚合</h2>
            <p className={styles.muted}>以下内容均来自公开白名单字段。</p>
          </div>
        </div>
        <div className={styles.serviceGrid}>
          <section className={styles.serviceCard} aria-label="校园职位摘要">
            <strong>校园职位</strong>
            <span className={styles.muted}>共 {jobs?.total ?? 0} 个公开职位入口</span>
            {jobList.slice(0, 3).map((job) => (
              <Link key={job.job_id} className={styles.detailLink} href={`/jobs/${job.job_id}`}>
                {job.title || '未命名职位'}
              </Link>
            ))}
          </section>
          <section className={styles.serviceCard} aria-label="校园活动摘要">
            <strong>校园招聘会</strong>
            <span className={styles.muted}>共 {events?.total ?? 0} 场公开活动</span>
            {eventList.slice(0, 3).map((event) => (
              <Link key={event.event_id} className={styles.detailLink} href={`/job-fairs/${event.event_id}`}>
                {event.title || '未命名活动'}
              </Link>
            ))}
          </section>
          <section className={styles.serviceCard} aria-label="校园资讯摘要">
            <strong>校园资讯</strong>
            <span className={styles.muted}>共 {contents?.total ?? 0} 条公开资讯</span>
            {contentList.slice(0, 3).map((content) => (
              <Link key={content.content_id} className={styles.detailLink} href={`/articles/${content.content_id}`}>
                {content.title || '未命名资讯'}
              </Link>
            ))}
          </section>
        </div>
      </section>
    </main>
  );
}
