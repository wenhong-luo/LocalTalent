const siteUrl = (process.env.NEXT_PUBLIC_LOCALTALENT_SITE_URL ?? 'http://localhost:3000').replace(/\/$/, '');

export function GET(): Response {
  const generatedAt = new Date().toISOString();
  const urls = [
    { loc: `${siteUrl}/`, priority: '0.8', changefreq: 'weekly' },
    { loc: `${siteUrl}/jobs`, priority: '0.8', changefreq: 'daily' },
    { loc: `${siteUrl}/jobs/famous`, priority: '0.7', changefreq: 'daily' },
    { loc: `${siteUrl}/jobs/emergency`, priority: '0.7', changefreq: 'daily' },
    { loc: `${siteUrl}/daily-jobs`, priority: '0.7', changefreq: 'daily' },
    { loc: `${siteUrl}/companies`, priority: '0.8', changefreq: 'daily' },
    { loc: `${siteUrl}/portal/talent-service-area`, priority: '0.9', changefreq: 'daily' },
    { loc: `${siteUrl}/job-fairs`, priority: '0.7', changefreq: 'weekly' },
    { loc: `${siteUrl}/job-fairs/online`, priority: '0.6', changefreq: 'weekly' },
    { loc: `${siteUrl}/job-fairs/campus`, priority: '0.6', changefreq: 'weekly' },
    { loc: `${siteUrl}/campus`, priority: '0.6', changefreq: 'weekly' },
    { loc: `${siteUrl}/campus/jobs`, priority: '0.6', changefreq: 'weekly' },
    { loc: `${siteUrl}/campus/elections`, priority: '0.6', changefreq: 'weekly' },
    { loc: `${siteUrl}/articles/policies`, priority: '0.7', changefreq: 'weekly' },
    { loc: `${siteUrl}/articles/news`, priority: '0.6', changefreq: 'weekly' },
    { loc: `${siteUrl}/articles/notices`, priority: '0.6', changefreq: 'weekly' },
    { loc: `${siteUrl}/hr-tools`, priority: '0.6', changefreq: 'weekly' },
    { loc: `${siteUrl}/help`, priority: '0.5', changefreq: 'monthly' },
    { loc: `${siteUrl}/explain`, priority: '0.5', changefreq: 'monthly' },
    { loc: `${siteUrl}/more-services`, priority: '0.5', changefreq: 'monthly' }
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
