export type ExpectedPositionGroup = {
  name: string;
  positions: string[];
};

export type ExpectedPositionCategory = {
  code: string;
  name: string;
  groups: ExpectedPositionGroup[];
};

export type ExpectedPositionSelection = {
  categoryCode: string;
  categoryName: string;
  groupName: string;
  positionName: string;
};

export const MAX_EXPECTED_POSITIONS = 6;

export const EXPECTED_POSITION_CATEGORIES: ExpectedPositionCategory[] = [
  {
    code: 'life_service',
    name: '生活 | 服务业',
    groups: [
      { name: '餐饮', positions: ['不限', '服务员', '送餐员', '厨师/厨师长', '后厨', '传菜员', '配菜/打荷', '洗碗工', '面点师', '茶艺师', '迎宾/接待', '大堂经理/领班', '餐饮管理', '学徒', '杂工', '咖啡师', '预订员'] },
      { name: '家政保洁/安保', positions: ['不限', '保洁', '保姆', '月嫂', '育婴师/保育员', '洗衣工', '钟点工', '保安', '护工', '送水工', '家电清洗', '家政经纪人'] },
      { name: '美容/美发', positions: ['不限', '发型师', '美发助理/学徒', '洗头工', '美容导师', '美容师', '美容助理/学徒', '化妆师', '美甲师', '美睫师', '纹绣师'] },
      { name: '娱乐/休闲', positions: ['不限', 'KTV服务员', '酒吧服务员', '礼仪/迎宾', '主持人', '调酒师', '按摩师', '足疗师', '采耳师', '棋牌室服务员'] },
      { name: '运动健身', positions: ['不限', '健身教练', '瑜伽教练', '舞蹈老师', '游泳教练', '球类教练', '运动康复师', '救生员'] },
      { name: '旅游', positions: ['不限', '导游', '计调', '签证专员', '旅游顾问', '票务', '酒店前台', '客房服务员', '民宿管家'] }
    ]
  },
  {
    code: 'hr_admin_management',
    name: '人力 | 行政 | 管理',
    groups: [
      { name: '人事/行政/后勤', positions: ['不限', '文员', '前台/总机/接待', '人事专员/助理', '人事经理/主管', '行政专员/助理', '行政经理/主管', '经理助理/秘书', '薪酬/绩效/员工关系', '后勤', '培训专员/助理', '招聘专员/助理', '招聘经理/主管'] },
      { name: '高级管理', positions: ['不限', 'CEO/总裁/总经理', '副总裁/副总经理', '总监', '分公司经理', '办事处首席代表', '合伙人', '总经理助理'] },
      { name: '项目/运营管理', positions: ['不限', '项目经理', '项目主管', '项目专员', '运营经理', '运营主管', '运营专员', '流程管理', '数据运营', '门店经理'] },
      { name: '猎头/咨询', positions: ['不限', '猎头顾问', '人才发展顾问', '企业管理咨询', '培训讲师', '组织发展顾问'] }
    ]
  },
  {
    code: 'sales_customer_purchase',
    name: '销售 | 客服 | 采购 | 淘宝',
    groups: [
      { name: '销售', positions: ['不限', '销售代表', '销售助理', '销售经理/主管', '销售总监', '电话销售', '销售支持', '汽车销售', '医药代表', '医疗器械销售', '网络销售', '区域销售', '渠道专员', '渠道经理/总监', '客户经理/主管', '大客户经理', '团购业务员/经理'] },
      { name: '客服', positions: ['不限', '客服专员/助理', '客服经理/主管', '售前客服', '售后客服', '电话客服', '在线客服', '客户关系管理', '投诉专员', '呼叫中心客服'] },
      { name: '采购/贸易', positions: ['不限', '采购员', '采购助理', '采购经理/总监', '供应商开发', '买手', '外贸专员', '外贸经理', '报关员', '跟单员', '商务专员'] },
      { name: '淘宝/电商', positions: ['不限', '淘宝客服', '淘宝美工', '淘宝运营', '电商运营', '拼多多运营', '直播运营', '网店店长', '电商仓储', '商品专员'] }
    ]
  },
  {
    code: 'marketing_media_design',
    name: '市场 | 媒介 | 广告 | 设计',
    groups: [
      { name: '市场/媒介/公关', positions: ['不限', '市场专员/助理', '市场经理/总监', '市场拓展', '市场调研', '品牌专员', '品牌经理', '活动策划', '媒介专员', '媒介经理', '公关专员', '会务会展', '企划经理'] },
      { name: '广告/会展/咨询', positions: ['不限', '广告创意', '广告文案', '广告设计', '广告客户经理', '会展策划', '咨询顾问', '婚礼策划师'] },
      { name: '美术/设计/创意', positions: ['不限', '平面设计', 'UI设计师', '视觉设计师', '网页设计', '包装设计', '室内设计', '家具设计', '服装设计', '工业设计', 'CAD设计/制图', '美术指导', '动画设计', '插画师'] },
      { name: '影视/媒体/内容', positions: ['不限', '新媒体运营', '短视频运营', '内容运营', '文案策划', '摄影师', '摄像师', '剪辑师', '编导', '主播', '主播运营', '后期制作'] }
    ]
  },
  {
    code: 'production_logistics_quality_auto',
    name: '生产 | 物流 | 质控 | 汽车',
    groups: [
      { name: '普工/技工', positions: ['不限', '普工', '操作工', '包装工', '装配工', '钳工', '电工', '焊工', '车工/铣工', '铲车/叉车工', '油漆工', '切割/焊工', '综合维修工', '制冷/水暖工'] },
      { name: '生产管理/研发', positions: ['不限', '生产主管/组长', '生产计划', '车间主任', '生产总监', '工艺设计', '工业工程师', '材料工程师', '技术工程师', '研发工程师'] },
      { name: '物流/仓储', positions: ['不限', '物流专员/助理', '物流经理/主管', '仓库管理员', '仓库经理/主管', '调度员', '快递员', '装卸/搬运工', '供应链管理', '货运代理', '单证员'] },
      { name: '质控/安防', positions: ['不限', '质检员', '质量工程师', '质量管理/测试经理', '认证工程师', '安全消防', '安全管理', '化验/检验', '可靠度工程师'] },
      { name: '汽车制造/服务', positions: ['不限', '汽车销售', '汽车维修', '汽车美容', '钣金工', '喷漆工', '汽车设计工程师', '汽车电子工程师', '二手车评估师', '洗车工'] },
      { name: '机械/仪器仪表', positions: ['不限', '机械工程师', '机电工程师', '模具工程师', '仪器仪表', '自动化工程师', '设备维修', '设备工程师', '机械制图员'] }
    ]
  },
  {
    code: 'network_communication_electronics',
    name: '网络 | 通信 | 电子',
    groups: [
      { name: '计算机/互联网/通信', positions: ['不限', '技术总监/经理', '技术支持/维护', '技术专员/助理', '软件工程师', '程序员', '前端工程师', '后端工程师', 'Java工程师', 'Python工程师', 'PHP工程师', '测试工程师', '运维工程师', '系统架构师', '数据库管理/DBA', '游戏设计/开发', '网页设计/制作', '语音/视频/图形', '项目经理/主管', '产品经理', '产品经理/专员', '网站运营', '网站编辑', '网络管理员', '网络与信息安全工程师', '实施工程师', '通信技术工程师', '网站策划'] },
      { name: '电子/电气', positions: ['不限', '自动化工程师', '电子/电气工程师', '电路工程师/技术员', '无线电工程师', '测试/可靠性工程师', '产品工艺/规划工程师', '音频/视频工程师', '嵌入式工程师', '硬件工程师', '电子维修', '集成电路IC设计'] },
      { name: '数据/算法/智能', positions: ['不限', '数据分析师', '数据开发工程师', '算法工程师', '机器学习工程师', 'BI工程师', '数据产品经理', '数据标注', '人工智能训练师'] }
    ]
  },
  {
    code: 'legal_education_translation_publish',
    name: '法律 | 教育 | 翻译 | 出版',
    groups: [
      { name: '法律', positions: ['不限', '律师', '律师助理', '法务专员/主管', '合规专员', '知识产权/专利顾问', '法律顾问', '书记员'] },
      { name: '教育培训', positions: ['不限', '教师/助教', '幼教/早教', '小学教师', '初中教师', '高中教师', '职业培训讲师', '课程顾问', '教务管理', '培训策划', '招生顾问', '家教', '特教老师'] },
      { name: '翻译', positions: ['不限', '英语翻译', '日语翻译', '韩语翻译', '法语翻译', '德语翻译', '俄语翻译', '西班牙语翻译', '小语种翻译', '同声传译'] },
      { name: '出版/印刷', positions: ['不限', '编辑/撰稿', '记者/采编', '校对/录入', '出版专员', '图书策划', '排版设计', '印刷操作', '装订工'] }
    ]
  },
  {
    code: 'finance_accounting_insurance',
    name: '财会 | 金融 | 保险',
    groups: [
      { name: '财务/审计/统计', positions: ['不限', '会计', '出纳', '财务助理', '财务主管/经理', '财务总监', '审计专员', '审计经理', '税务专员', '成本会计', '统计员', '预算分析', '资产/资金管理'] },
      { name: '金融/银行/证券', positions: ['不限', '金融顾问', '证券经纪人', '银行柜员', '客户经理', '信贷专员', '风控专员', '投资顾问', '基金销售', '融资专员', '理财顾问', '资产评估'] },
      { name: '保险', positions: ['不限', '保险顾问', '保险经纪人', '保险理赔', '保险内勤', '保险培训师', '核保理赔', '车险专员'] }
    ]
  },
  {
    code: 'medical_pharma_environment',
    name: '医疗 | 制药 | 环保',
    groups: [
      { name: '医院/医疗/护理', positions: ['不限', '医生', '护士', '导医', '药剂师', '检验师', '康复治疗师', '口腔医生', '心理咨询师', '影像/放射', '验光师', '健康管理师', '营养师', '宠物医生'] },
      { name: '制药/生物工程', positions: ['不限', '医药代表', '医疗器械销售', '制药工程师', '质量研究员', '注册专员', '临床研究员', '药品生产', '生物工程/生物制药', '药品研发'] },
      { name: '环保', positions: ['不限', '环保工程师', '环境检测员', '污水处理员', 'EHS专员', '环保技术', '水质检测员', '固废处理工程师', '环评工程师'] }
    ]
  },
  {
    code: 'construction_property_agriculture_other',
    name: '建筑 | 物业 | 农林牧渔 | 其他',
    groups: [
      { name: '建筑', positions: ['不限', '施工员', '资料员', '安全员', '造价员', '预算员', '土建工程师', '结构工程师', '建筑设计师', '给排水/制冷/暖通', '工程监理', '项目经理', '测绘员', '市政工程师'] },
      { name: '物业管理', positions: ['不限', '物业管家', '物业经理', '维修工', '水电工', '客服管家', '绿化工', '保洁主管', '秩序维护员', '停车管理员'] },
      { name: '农林牧渔', positions: ['不限', '农艺师', '养殖技术员', '饲养员', '园艺师', '兽医', '畜牧师', '水产养殖', '饲料业务', '农业技术员'] },
      { name: '其他', positions: ['不限', '科研人员', '储备干部', '实习生', '志愿者', '其他职位'] }
    ]
  }
];

export function positionNameForStorage(group: ExpectedPositionGroup, positionName: string): string {
  return positionName === '不限' ? `${group.name}不限` : positionName;
}

export function categoryCodeForExpectedPosition(positionName: string): string {
  for (const category of EXPECTED_POSITION_CATEGORIES) {
    for (const group of category.groups) {
      if (positionName === `${group.name}不限` || group.positions.includes(positionName)) {
        return category.code;
      }
    }
  }
  return '';
}

export function normalizeExpectedPositionSelections(
  positions: string[],
  fallbackCategoryCode = ''
): ExpectedPositionSelection[] {
  const fallbackCategory = EXPECTED_POSITION_CATEGORIES.find((category) => category.code === fallbackCategoryCode);
  return positions
    .map((positionName) => positionName.trim())
    .filter(Boolean)
    .slice(0, MAX_EXPECTED_POSITIONS)
    .map((positionName) => {
      const matched = findCategoryAndGroupByPosition(positionName);
      const category = matched?.category ?? fallbackCategory ?? EXPECTED_POSITION_CATEGORIES[0];
      const group = matched?.group ?? category.groups[0];
      return {
        categoryCode: category.code,
        categoryName: category.name,
        groupName: group.name,
        positionName
      };
    });
}

function findCategoryAndGroupByPosition(positionName: string): {
  category: ExpectedPositionCategory;
  group: ExpectedPositionGroup;
} | null {
  for (const category of EXPECTED_POSITION_CATEGORIES) {
    for (const group of category.groups) {
      if (positionName === `${group.name}不限` || group.positions.includes(positionName)) {
        return { category, group };
      }
    }
  }
  return null;
}
