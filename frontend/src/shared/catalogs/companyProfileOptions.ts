export type CompanyProfileOption = {
  value: string;
  label: string;
};

export const companyNatureOptions: CompanyProfileOption[] = [
  { value: 'state_owned', label: '国企' },
  { value: 'private', label: '民营' },
  { value: 'joint_venture', label: '合资' },
  { value: 'foreign_owned', label: '外商独资' },
  { value: 'joint_stock', label: '股份制企业' },
  { value: 'listed', label: '上市公司' },
  { value: 'government', label: '国家机关' },
  { value: 'public_institution', label: '事业单位' },
  { value: 'other', label: '其它' }
];

export const companyScaleOptions: CompanyProfileOption[] = [
  { value: 'under_10', label: '10人以下' },
  { value: '10-50', label: '10～50人' },
  { value: '50-200', label: '50～200人' },
  { value: '200-500', label: '200～500人' },
  { value: '500-1000', label: '500～1000人' },
  { value: 'over_1000', label: '1000人以上' }
];

export const companyIndustryOptions: CompanyProfileOption[] = [
  { value: 'software', label: '计算机软件/硬件' },
  { value: 'computer_service', label: '计算机系统/维修' },
  { value: 'communication', label: '通信(设备/运营/服务)' },
  { value: 'internet', label: '互联网/电子商务' },
  { value: 'online_game', label: '网络游戏' },
  { value: 'electronics_semiconductor', label: '电子/半导体/集成电路' },
  { value: 'instrument_automation', label: '仪器仪表/工业自动化' },
  { value: 'accounting_audit', label: '会计/审计' },
  { value: 'finance_investment', label: '金融(投资/证券)' },
  { value: 'finance_bank_insurance', label: '金融(银行/保险)' },
  { value: 'trade_import_export', label: '贸易/进出口' },
  { value: 'wholesale_retail', label: '批发/零售' },
  { value: 'consumer_goods', label: '消费品(食/饮/烟酒)' },
  { value: 'textile_leather', label: '服装/纺织/皮革' },
  { value: 'furniture_appliance_toy', label: '家具/家电/工艺品/玩具' },
  { value: 'office_equipment', label: '办公用品及设备' },
  { value: 'machinery_heavy', label: '机械/设备/重工' },
  { value: 'auto_parts', label: '汽车/摩托车/零配件' },
  { value: 'pharma_biotech', label: '制药/生物工程' },
  { value: 'medical_beauty_health', label: '医疗/美容/保健/卫生' },
  { value: 'medical_device', label: '医疗设备/器械' },
  { value: 'advertising_marketing', label: '广告/市场推广' },
  { value: 'exhibition', label: '会展/博览' },
  { value: 'media_art_publish', label: '影视/媒体/艺术/出版' },
  { value: 'printing_packaging', label: '印刷/包装/造纸' },
  { value: 'real_estate', label: '房地产开发' },
  { value: 'construction_engineering', label: '建筑与工程' },
  { value: 'interior_design', label: '家居/室内设计/装潢' },
  { value: 'property_business_center', label: '物业管理/商业中心' },
  { value: 'agency_housekeeping', label: '中介服务/家政服务' },
  { value: 'professional_service', label: '专业服务/财会/法律' },
  { value: 'testing_certification', label: '检测/认证' },
  { value: 'education_training', label: '教育/培训' },
  { value: 'academic_research', label: '学术/科研' },
  { value: 'catering_leisure', label: '餐饮/娱乐/休闲' },
  { value: 'hotel_travel', label: '酒店/旅游' },
  { value: 'transport_logistics', label: '交通/运输/物流' },
  { value: 'aerospace_aviation', label: '航天/航空' },
  { value: 'energy_chemical_mining', label: '能源(石油/化工/矿产)' },
  { value: 'energy_metal_material', label: '能源(采掘/冶炼/原材料)' },
  { value: 'power_water_new_energy', label: '电力/水利/新能源' },
  { value: 'government_public_institution', label: '政府部门/事业单位' },
  { value: 'nonprofit_association', label: '非盈利机构/行业协会' },
  { value: 'agriculture_fishery_forestry', label: '农业/渔业/林业/牧业' },
  { value: 'other_industry', label: '其他行业' },
  { value: 'low_altitude_economy', label: '低空经济' }
];

export const companyCapitalUnitOptions: CompanyProfileOption[] = [
  { value: 'cny_10k', label: '万元人民币' },
  { value: 'usd_10k', label: '万美元' },
  { value: 'hkd_10k', label: '万港币' },
  { value: 'other', label: '其它' }
];

export const companyBenefitOptions: CompanyProfileOption[] = [
  { value: 'five_insurance', label: '五险一金' },
  { value: 'commercial_insurance', label: '商业保险' },
  { value: 'free_meals', label: '包吃' },
  { value: 'free_housing', label: '包住' },
  { value: 'weekend_double', label: '周末双休' },
  { value: 'annual_leave', label: '带薪年假' },
  { value: 'year_end_bonus', label: '年终奖' },
  { value: 'performance_bonus', label: '绩效奖金' },
  { value: 'transport_allowance', label: '交通补助' },
  { value: 'meal_allowance', label: '餐补' },
  { value: 'phone_allowance', label: '话补' },
  { value: 'housing_allowance', label: '房补' },
  { value: 'health_check', label: '定期体检' },
  { value: 'holiday_benefits', label: '节日福利' },
  { value: 'flexible_work', label: '弹性工作' },
  { value: 'training_promotion', label: '培训晋升' },
  { value: 'team_travel', label: '员工旅游' },
  { value: 'other_benefit', label: '其它福利' }
];
