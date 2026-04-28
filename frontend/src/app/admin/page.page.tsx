import type { Metadata } from 'next';
import { AdminDashboard } from '@/pages/admin/AdminDashboard';

export const metadata: Metadata = {
  title: '运营后台',
  description: '企业审核、职位审核、导出审批与审计中心入口。'
};

export default function AdminPage() {
  return <AdminDashboard />;
}
