import type { Metadata } from 'next';
import type { ReactNode } from 'react';
import './globals.css';

export const metadata: Metadata = {
  title: {
    default: 'LocalTalent 地方人才服务平台',
    template: '%s | LocalTalent'
  },
  description: 'LocalTalent 一期门户前台，人才服务区仅展示候选人发布快照。'
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="zh-CN">
      <body>{children}</body>
    </html>
  );
}
