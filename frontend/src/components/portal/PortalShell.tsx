import type { ReactNode } from 'react';
import { FloatingToolBar } from './FloatingToolBar';
import { LogoSearchHeader } from './LogoSearchHeader';
import { PortalFooter } from './PortalFooter';
import { PortalMainNav } from './PortalMainNav';
import { TopUtilityBar } from './TopUtilityBar';
import styles from './PortalShell.module.css';

export function PortalShell({ children }: { children: ReactNode }) {
  return (
    <div className={styles.shell}>
      <TopUtilityBar />
      <LogoSearchHeader />
      <PortalMainNav />
      <div className={styles.content}>{children}</div>
      <PortalFooter />
      <FloatingToolBar />
    </div>
  );
}
