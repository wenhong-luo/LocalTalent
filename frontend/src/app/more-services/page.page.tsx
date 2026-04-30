import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { MoreServicesPage } from '@/pages/portal/content/PortalContentPages';

export const metadata: Metadata = {
  title: '更多服务',
  description: 'LocalTalent 更多服务页展示低风险公开入口和风险池能力占位。'
};

export default function MoreServicesRoute() {
  return (
    <PortalShell>
      <MoreServicesPage />
    </PortalShell>
  );
}
