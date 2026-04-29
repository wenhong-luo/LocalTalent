import Link from 'next/link';
import styles from './JobSearchPage.module.css';
import { salaryText, type PortalJob } from './portalJobApi';

type JobDetailPageProps = {
  job?: PortalJob;
  message?: string;
  traceId?: string;
};

export function JobDetailPage({ job, message, traceId }: JobDetailPageProps) {
  if (!job) {
    return (
      <main className={styles.detailPage}>
        <section className={styles.detailHero}>
          <p className={styles.eyebrow}>Job Detail</p>
          <h1 className={styles.title}>职位暂时不可用</h1>
          <p className={styles.muted}>{message ?? '该职位可能已下线，或所属企业尚未完成认证。'}</p>
          {traceId ? <span className={styles.tag}>trace {traceId}</span> : null}
          <Link className={styles.detailLink} href="/jobs">
            返回职位列表
          </Link>
        </section>
      </main>
    );
  }

  return (
    <main className={styles.detailPage}>
      <article className={styles.detailHero} aria-label="职位详情 SEO 入口">
        <p className={styles.eyebrow}>公开职位详情</p>
        <h1 className={styles.title}>{job.title || '未命名职位'}</h1>
        <p className={styles.subtitle}>
          {job.company_name || '认证企业'} · {job.city_code || '不限地区'} · {job.category_code || '未分类'}
        </p>
        <div className={styles.tagRow}>
          <span className={styles.tag}>在线职位</span>
          <span className={styles.tag}>认证企业</span>
          <span className={styles.tag}>SEO 公开入口</span>
        </div>
        <strong className={styles.salary}>{salaryText(job)}</strong>
        <section>
          <h2 className={styles.sectionTitle}>职位描述</h2>
          <p className={styles.description}>{job.job_desc || '职位描述待补充。'}</p>
        </section>
        <div className={styles.actions}>
          <button className={styles.disabledAction} type="button" disabled aria-disabled="true">
            投递占位
          </button>
          <button className={styles.disabledAction} type="button" disabled aria-disabled="true">
            收藏占位
          </button>
          <Link className={styles.detailLink} href="/jobs">
            返回职位列表
          </Link>
        </div>
        {traceId ? <span className={styles.tag}>trace {traceId}</span> : null}
      </article>
    </main>
  );
}
