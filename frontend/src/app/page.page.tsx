import { PortalShell } from '@/components/portal/PortalShell';
import { PortalHomePage } from '@/pages/portal/PortalHomePage';
import { fetchPortalRecommendations } from '@/pages/portal/portalRecommendationApi';

export default async function HomePage() {
  const [hotJobs, featuredCompanies] = await Promise.all([
    fetchPortalRecommendations('home_hot_jobs', 8).catch(() => []),
    fetchPortalRecommendations('home_featured_companies', 8).catch(() => [])
  ]);

  return (
    <PortalShell>
      <PortalHomePage hotJobs={hotJobs} featuredCompanies={featuredCompanies} />
    </PortalShell>
  );
}
