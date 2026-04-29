import Link from 'next/link';
import styles from './PortalShell.module.css';

export function PortalFooter() {
  return (
    <footer className={styles.footer} aria-label="门户页脚">
      <div className={`${styles.container} ${styles.footerGrid}`}>
        <section>
          <h2 className={styles.footerTitle}>LocalTalent</h2>
          <p className={styles.footerText}>
            面向地方就业服务的可信招聘门户。公开层只展示职位、企业、活动、资讯和发布快照白名单。
          </p>
        </section>
        <section>
          <h3 className={styles.footerTitle}>求职服务</h3>
          <nav className={styles.footerLinks} aria-label="求职服务">
            <Link href="/jobs">找工作</Link>
            <Link href="/portal/talent-service-area">人才服务区</Link>
            <Link href="/candidate/center">求职者中心</Link>
          </nav>
        </section>
        <section>
          <h3 className={styles.footerTitle}>招聘服务</h3>
          <nav className={styles.footerLinks} aria-label="招聘服务">
            <Link href="/companies">找企业</Link>
            <Link href="/company">企业中心</Link>
            <Link href="/job-fairs">招聘会</Link>
          </nav>
        </section>
        <section>
          <h3 className={styles.footerTitle}>内容服务</h3>
          <nav className={styles.footerLinks} aria-label="内容服务">
            <Link href="/articles/policies">就业政策</Link>
            <Link href="/hr-tools">HR 工具箱</Link>
            <Link href="/help">帮助中心</Link>
          </nav>
        </section>
      </div>
      <div className={`${styles.container} ${styles.footerBottom}`}>
        © LocalTalent Phase II. 对接接口仅 stub，高风险能力进入风险池后再评估。
      </div>
    </footer>
  );
}
