import { PortalShell } from '@/components/portal/PortalShell';
import { PortalHomePage } from '@/pages/portal/PortalHomePage';
import { fetchPortalHomeSlots } from '@/pages/portal/portalHomeSlotApi';
import { fetchPortalRecommendations } from '@/pages/portal/portalRecommendationApi';

const HOME_SLOT_CODES = [
  'home_hero_banner',
  'home_quick_1',
  'home_quick_2',
  'home_quick_3',
  'home_full_width_banner',
  'home_half_left',
  'home_half_right',
  'home_third_1',
  'home_third_2',
  'home_third_3',
  'home_bottom_banner'
];

export default async function HomePage() {
  const [hotJobs, featuredCompanies, homeSlots] = await Promise.all([
    fetchPortalRecommendations('home_hot_jobs', 8).catch(() => []),
    fetchPortalRecommendations('home_featured_companies', 8).catch(() => []),
    fetchPortalHomeSlots(HOME_SLOT_CODES).catch(() => [])
  ]);

  return (
    <PortalShell>
      <PortalHomePage hotJobs={hotJobs} featuredCompanies={featuredCompanies} homeSlots={homeSlots} />
    </PortalShell>
  );
}
