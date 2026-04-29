import Link from 'next/link';
import { MoreServicesMenu } from './MoreServicesMenu';
import { mainNavLinks } from './portalLinks';
import styles from './PortalShell.module.css';

export function PortalMainNav() {
  return (
    <div className={styles.navWrap}>
      <nav className={`${styles.container} ${styles.nav}`} aria-label="门户主导航">
        {mainNavLinks.map((item, index) => (
          <Link
            key={item.label}
            className={`${styles.navLink} ${index === 0 ? styles.navPrimary : ''}`}
            href={item.href}
          >
            {item.label}
          </Link>
        ))}
        <MoreServicesMenu />
        <Link className={styles.navLink} href="/candidate/center">
          管理中心
        </Link>
      </nav>
    </div>
  );
}
