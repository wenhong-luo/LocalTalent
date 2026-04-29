import Link from 'next/link';
import styles from './PortalShell.module.css';

export function TopUtilityBar() {
  return (
    <div className={styles.utilityBar} aria-label="门户顶部工具条">
      <div className={`${styles.container} ${styles.utilityInner}`}>
        <div className={styles.utilityLeft}>
          <span>总站</span>
          <span className={styles.utilityMuted}>[切换城市]</span>
        </div>
        <nav className={styles.utilityLinks} aria-label="会员与帮助入口">
          <Link className={styles.utilityLink} href="/candidate/center">
            会员登录
          </Link>
          <Link className={styles.utilityLink} href="/candidate/center">
            会员注册
          </Link>
          <span className={styles.utilityMuted}>职聊占位</span>
          <span aria-disabled="true" className={styles.utilityMuted}>
            手机端占位
          </span>
          <Link className={styles.utilityLink} href="/help">
            使用帮助
          </Link>
          <span className={styles.utilityMuted}>网站导航</span>
        </nav>
      </div>
    </div>
  );
}
