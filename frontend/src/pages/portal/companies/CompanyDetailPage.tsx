import Link from 'next/link';
import { salaryText } from '@/pages/portal/jobs/portalJobApi';
import styles from './CompanySearchPage.module.css';
import type { PortalCompany } from './portalCompanyApi';

type CompanyDetailPageProps = {
  company?: PortalCompany;
  message?: string;
  traceId?: string;
};

function initialLetter(name: string): string {
  return (name.trim().charAt(0) || '企').toUpperCase();
}

export function CompanyDetailPage({ company, message, traceId }: CompanyDetailPageProps) {
  if (!company) {
    return (
      <main className={styles.detailPage}>
        <section className={`${styles.card} ${styles.detailHero}`}>
          <p className={styles.eyebrow}>Company Detail</p>
          <h1 className={styles.title}>企业暂时不可用</h1>
          <p className={styles.muted}>{message ?? '该企业可能尚未认证，或公开主页暂时不可访问。'}</p>
          {traceId ? <span className={styles.tag}>trace {traceId}</span> : null}
          <Link className={styles.detailLink} href="/companies">
            返回企业列表
          </Link>
        </section>
      </main>
    );
  }

  return (
    <main className={styles.detailPage}>
      <article className={`${styles.card} ${styles.detailHero}`} aria-label="企业公开主页">
        <p className={styles.eyebrow}>认证企业公开主页</p>
        <div className={styles.companyCard}>
          <span className={styles.avatar} aria-hidden="true">
            {initialLetter(company.company_name)}
          </span>
          <div>
            <h1 className={styles.companyName}>{company.company_name || '认证企业'}</h1>
            <p className={styles.muted}>
              {company.city_code || '不限地区'} · {company.industry_code || '行业待补充'} · {company.nature_code || '性质待补充'} ·{' '}
              {company.scale_code || '规模待补充'}
            </p>
            <div className={styles.tagRow}>
              <span className={styles.tag}>认证企业</span>
              <span className={styles.tag}>企业公开简介</span>
              <span className={styles.tag}>不展示联系方式</span>
            </div>
          </div>
          <div className={styles.countBadge}>
            <span>{company.open_job_count}</span>
            <small>在线职位</small>
          </div>
        </div>

        <section>
          <h2 className={styles.sectionTitle}>企业简介</h2>
          <p className={styles.muted}>{company.company_profile || '企业公开简介待补充。'}</p>
        </section>

        <section>
          <h2 className={styles.sectionTitle}>在招职位</h2>
          {company.open_jobs.length > 0 ? (
            <div className={styles.jobList}>
              {company.open_jobs.map((job) => (
                <Link key={job.job_id} className={styles.jobItem} href={`/jobs/${job.job_id}`}>
                  <span>{job.title || '未命名职位'}</span>
                  <span className={styles.muted}>
                    {job.city_code || '不限地区'} · {job.category_code || '未分类'} · {salaryText(job)}
                  </span>
                </Link>
              ))}
            </div>
          ) : (
            <div className={styles.empty}>暂无在线职位。</div>
          )}
        </section>

        <div className={styles.tagRow}>
          <button className={styles.disabledAction} type="button" disabled aria-disabled="true">
            联系解锁：风险池
          </button>
          <button className={styles.disabledAction} type="button" disabled aria-disabled="true">
            受控找人才：风险池
          </button>
          <Link className={styles.detailLink} href="/companies">
            返回企业列表
          </Link>
        </div>
        {traceId ? <span className={styles.tag}>trace {traceId}</span> : null}
      </article>
    </main>
  );
}
