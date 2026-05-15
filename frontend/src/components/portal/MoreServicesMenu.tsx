import Link from 'next/link';
import { highRiskServicePlaceholders, lowRiskServiceLinks, managementLinks } from './portalLinks';
import styles from './PortalShell.module.css';

export function MoreServicesMenu() {
  return (
    <div className={styles.moreWrap}>
      <button className={`${styles.navButton} ${styles.moreNavButton}`} type="button" aria-haspopup="true" data-nav-button="more-services">
        更多服务
      </button>
      <div className={styles.morePanel} aria-label="更多服务菜单">
        <section>
          <h3 className={styles.moreGroupTitle}>求职找工作</h3>
          <div className={styles.moreList}>
            <Link className={styles.moreLink} href="/more-services">
              全部更多服务
            </Link>
            {lowRiskServiceLinks.map((item) => (
              <Link key={item.label} className={styles.moreLink} href={item.href}>
                {item.label}
              </Link>
            ))}
          </div>
        </section>

        <section>
          <h3 className={styles.moreGroupTitle}>管理中心</h3>
          <div className={styles.moreList}>
            {managementLinks.map((item) => (
              <Link key={item.label} className={styles.moreLink} href={item.href}>
                {item.label}
              </Link>
            ))}
          </div>
        </section>

        <section>
          <h3 className={styles.moreGroupTitle}>外部能力占位</h3>
          <div className={styles.moreList}>
            {highRiskServicePlaceholders.slice(0, 3).map((item) => (
              <button key={item.label} className={styles.moreDisabled} type="button" aria-disabled="true" disabled>
                <span>{item.label}</span>
                <span className={styles.disabledTag}>占位</span>
              </button>
            ))}
          </div>
        </section>

        <section>
          <h3 className={styles.moreGroupTitle}>风险池能力</h3>
          <div className={styles.moreList}>
            {highRiskServicePlaceholders.slice(3).map((item) => (
              <button key={item.label} className={styles.moreDisabled} type="button" aria-disabled="true" disabled>
                <span>{item.label}</span>
                <span className={styles.disabledTag}>风险池</span>
              </button>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}
