import type { Metadata } from 'next';
import { CompanyDashboard } from '@/pages/company/CompanyDashboard';

export const metadata: Metadata = {
  title: '企业后台',
  description: '企业认证、职位状态、投递池与导出申请入口。',
  robots: {
    index: false,
    follow: false
  }
};

export default function CompanyPage() {
  return <CompanyDashboard />;
}
