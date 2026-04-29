import Link from 'next/link';
import styles from './PortalShell.module.css';

const privateLinks = [
  { label: '我的投递', href: '/candidate/center' },
  { label: '我的消息', href: '/candidate/center' },
  { label: '我的收藏', href: '/candidate/center' },
  { label: '面试邀请', href: '/candidate/center' }
];

const disabledEntries = ['公众号占位', '小程序占位', 'App 占位'];

export function FloatingToolBar() {
  return (
    <aside className={styles.floating} aria-label="右侧浮动工具条">
      {privateLinks.map((item) => (
        <Link key={item.label} className={styles.floatingLink} href={item.href}>
          {item.label}
        </Link>
      ))}
      {disabledEntries.map((label) => (
        <button key={label} className={styles.floatingDisabled} type="button" aria-disabled="true" disabled>
          {label}
        </button>
      ))}
    </aside>
  );
}
