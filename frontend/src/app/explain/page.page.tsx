import type { Metadata } from 'next';
import { PortalShell } from '@/components/portal/PortalShell';
import { ContentChannelPage } from '@/pages/portal/content/PortalContentPages';
import { loadContentChannelInitialState, type PortalRouteSearchParams } from '@/pages/portal/content/portalContentPageState';

export const dynamic = 'force-dynamic';
export const revalidate = 0;

export const metadata: Metadata = {
  title: '公开说明',
  description: 'LocalTalent 公开说明频道展示已发布的帮助、协议与服务说明。'
};

export default async function ExplainRoute({ searchParams }: { searchParams?: Promise<PortalRouteSearchParams> }) {
  const initialState = await loadContentChannelInitialState((await searchParams) ?? {}, 'help');

  return (
    <PortalShell>
      <ContentChannelPage
        config={{
          contentType: 'help',
          eyebrow: 'Public Explain',
          title: '公开说明',
          description: '展示公开帮助、协议和服务说明，不包含个人明细、审核材料或后台备注。',
          detailBasePath: '/explain',
          badge: '说明公开频道'
        }}
        initialState={initialState}
      />
    </PortalShell>
  );
}
