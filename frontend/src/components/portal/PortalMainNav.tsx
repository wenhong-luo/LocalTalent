'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { MoreServicesMenu } from './MoreServicesMenu';
import { mainNavLinks } from './portalLinks';
import styles from './PortalShell.module.css';

function isActiveNavItem(pathname: string, href: string): boolean {
  if (href === '/') {
    return pathname === '/';
  }
  return pathname === href || pathname.startsWith(`${href}/`);
}

export function PortalMainNav() {
  const pathname = usePathname() ?? '/';

  return (
    <div className={styles.navWrap}>
      <nav className={`${styles.container} ${styles.nav}`} aria-label="门户主导航">
        {mainNavLinks.map((item) => {
          const isActive = isActiveNavItem(pathname, item.href);
          return (
            <Link
              key={item.label}
              aria-current={isActive ? 'page' : undefined}
              className={`${styles.navLink} ${isActive ? styles.navPrimary : ''}`}
              href={item.href}
            >
              {item.label}
            </Link>
          );
        })}
        <MoreServicesMenu />
        <Link className={styles.navLink} href="/auth/login?redirect=/candidate/center">
          管理中心
        </Link>
      </nav>
    </div>
  );
}
