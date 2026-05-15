import type { ReactNode } from 'react';
import styles from './PortalShell.module.css';

type PortalAdRailFrameProps = {
  children: ReactNode;
  label: string;
};

export function PortalAdRailFrame({ children, label }: PortalAdRailFrameProps) {
  return (
    <div className={styles.adRailFrame} data-layout="portal-ad-rails" data-testid="portal-ad-rail-frame">
      <aside className={`${styles.adRail} ${styles.adRailLeft}`} aria-label={`${label}左侧广告留白`}>
        <span>广告位占位</span>
      </aside>
      <div className={styles.adRailContent}>{children}</div>
      <aside className={`${styles.adRail} ${styles.adRailRight}`} aria-label={`${label}右侧广告留白`}>
        <span>广告位占位</span>
      </aside>
    </div>
  );
}
