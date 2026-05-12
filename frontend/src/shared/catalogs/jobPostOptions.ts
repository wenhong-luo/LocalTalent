import type { CompanyProfileOption } from './companyProfileOptions';

export const jobNatureOptions: CompanyProfileOption[] = [
  { value: 'full_time', label: '全职' },
  { value: 'part_time', label: '兼职' },
  { value: 'internship', label: '实习' },
  { value: 'temporary', label: '临时工' }
];

export const jobExperienceOptions: CompanyProfileOption[] = [
  { value: 'none', label: '经验不限' },
  { value: '1_year', label: '1年以下' },
  { value: '1_3_years', label: '1～3年' },
  { value: '3_5_years', label: '3～5年' },
  { value: '5_10_years', label: '5～10年' },
  { value: '10_years_plus', label: '10年以上' }
];

export const jobEducationOptions: CompanyProfileOption[] = [
  { value: 'none', label: '学历不限' },
  { value: 'middle_school', label: '初中' },
  { value: 'high_school', label: '高中' },
  { value: 'technical_secondary', label: '中专/技校' },
  { value: 'college', label: '大专' },
  { value: 'bachelor', label: '本科' },
  { value: 'master', label: '硕士' },
  { value: 'doctor', label: '博士' }
];

export const jobRecruitmentTimeOptions: CompanyProfileOption[] = [
  { value: 'long_term', label: '长期招聘' },
  { value: 'one_month', label: '一个月内' },
  { value: 'three_months', label: '三个月内' },
  { value: 'urgent', label: '急招' }
];

export const jobContactModeOptions: CompanyProfileOption[] = [
  { value: 'company_profile', label: '使用企业资料联系方式' },
  { value: 'custom', label: '使用本职位联系方式' },
  { value: 'hidden', label: '不公开联系方式' }
];
