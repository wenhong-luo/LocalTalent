const siteUrl = (process.env.NEXT_PUBLIC_LOCALTALENT_SITE_URL ?? 'http://localhost:3000').replace(/\/$/, '');

export function GET(): Response {
  const generatedAt = new Date().toISOString();
  const urls = [
    { loc: `${siteUrl}/`, priority: '0.8', changefreq: 'weekly' },
    { loc: `${siteUrl}/jobs`, priority: '0.8', changefreq: 'daily' },
    { loc: `${siteUrl}/companies`, priority: '0.8', changefreq: 'daily' },
    { loc: `${siteUrl}/portal/talent-service-area`, priority: '0.9', changefreq: 'daily' }
  ];
  const body = [
    '<?xml version="1.0" encoding="UTF-8"?>',
    '<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">',
    ...urls.map((url) => (
      `  <url><loc>${url.loc}</loc><lastmod>${generatedAt}</lastmod>`
      + `<changefreq>${url.changefreq}</changefreq><priority>${url.priority}</priority></url>`
    )),
    '</urlset>',
    ''
  ].join('\n');

  return new Response(body, {
    headers: {
      'content-type': 'application/xml; charset=utf-8'
    }
  });
}
