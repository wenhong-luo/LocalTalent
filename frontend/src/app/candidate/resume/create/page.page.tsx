import type { Metadata } from 'next';
import { CandidateResumeCreate } from '@/pages/candidate/CandidateResumeCreate';

export const metadata: Metadata = {
  title: '完善简历 | LocalTalent',
  description: '求职者首次登录后的本人私有简历创建流程。',
  robots: {
    index: false,
    follow: false
  }
};

export default function CandidateResumeCreatePage() {
  return <CandidateResumeCreate />;
}
