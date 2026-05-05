import type { Metadata } from 'next';
import { CandidateCenter } from '@/pages/candidate/CandidateCenter';

export const metadata: Metadata = {
  title: '求职者中心 | LocalTalent',
  description: '查看简历、投递、签到、同意与撤回状态。',
  robots: {
    index: false,
    follow: false
  }
};

export default function CandidateCenterPage() {
  return <CandidateCenter />;
}
