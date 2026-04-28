import type { NextConfig } from 'next';

const apiBaseUrl = (process.env.LOCALTALENT_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '');

const nextConfig: NextConfig = {
  output: 'standalone',
  pageExtensions: ['page.tsx', 'page.ts', 'page.jsx', 'page.js'],
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${apiBaseUrl}/api/:path*`
      }
    ];
  }
};

export default nextConfig;
