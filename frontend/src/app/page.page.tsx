import { PortalShell } from '@/components/portal/PortalShell';
import { PortalHomePage } from '@/pages/portal/PortalHomePage';

export default function HomePage() {
  return (
    <PortalShell>
      <PortalHomePage />
    </PortalShell>
  );
}
