import Link from 'next/link';
import styles from './PortalShell.module.css';

export function LogoSearchHeader() {
  return (
    <header className={styles.header} aria-label="门户搜索头部">
      <div className={`${styles.container} ${styles.headerInner}`}>
        <Link className={styles.brand} href="/" aria-label="LocalTalent 首页">
          <span className={styles.brandMark}>LT</span>
          <span className={styles.brandText}>
            <span className={styles.brandTitle}>LocalTalent</span>
            <span className={styles.brandSub}>地方人才服务平台</span>
          </span>
        </Link>

        <div className={styles.searchBlock}>
          <form className={styles.searchForm} action="/jobs" method="get" aria-label="找工作关键词搜索">
            <span className={styles.searchType}>找工作</span>
            <input className={styles.searchInput} name="keyword" placeholder="请输入搜索关键字" />
            <button className={styles.searchButton} type="submit">
              搜索
            </button>
          </form>
          <div className={styles.searchMeta}>
            <Link href="/jobs?advanced=1">高级搜索</Link>
            <button className={styles.placeholderButton} type="button" aria-disabled="true" disabled>
              地图搜索占位
            </button>
          </div>
        </div>

        <div className={styles.qrCard} aria-label="扫码关注占位">
          <span className={styles.qrPlaceholder}>QR</span>
          <span>
            扫码关注
            <br />
            公众号占位
          </span>
        </div>
      </div>
    </header>
  );
}
