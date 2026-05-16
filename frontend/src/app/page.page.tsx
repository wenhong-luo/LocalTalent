import { PortalShell } from '@/components/portal/PortalShell';
import { PortalHomePage } from '@/pages/portal/PortalHomePage';
import { fetchPortalHomeSlots } from '@/pages/portal/portalHomeSlotApi';
import { fetchPortalRecommendations } from '@/pages/portal/portalRecommendationApi';

const HOME_FIRST_SCREEN_SLOTS = ['home_hero_banner', 'home_quick_1', 'home_quick_2', 'home_quick_3'];

export default async function HomePage() {
  const [hotJobs, featuredCompanies, homeSlots] = await Promise.all([
    fetchPortalRecommendations('home_hot_jobs', 8).catch(() => []),
    fetchPortalRecommendations('home_featured_companies', 8).catch(() => []),
    fetchPortalHomeSlots(HOME_FIRST_SCREEN_SLOTS).catch(() => [])
  ]);

  return (
    <PortalShell>
      <PortalHomePage hotJobs={hotJobs} featuredCompanies={featuredCompanies} homeSlots={homeSlots} />
    </PortalShell>
  );
}
