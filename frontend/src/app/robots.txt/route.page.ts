const siteUrl = (process.env.NEXT_PUBLIC_LOCALTALENT_SITE_URL ?? 'http://localhost:3000').replace(/\/$/, '');

export function GET(): Response {
  return new Response(
    [
      'User-agent: *',
      'Allow: /',
      'Allow: /portal/talent-service-area',
      `Sitemap: ${siteUrl}/sitemap.xml`,
      ''
    ].join('\n'),
    {
      headers: {
        'content-type': 'text/plain; charset=utf-8'
      }
    }
  );
}
