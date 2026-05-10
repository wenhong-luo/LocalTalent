'use client';

export const MAX_EXPECTED_REGIONS = 6;

export type RegionDistrict = {
  code: string;
  name: string;
};

export type RegionCity = {
  code: string;
  name: string;
  districts: RegionDistrict[];
};

export type RegionProvince = {
  code: string;
  name: string;
  cities: RegionCity[];
};

export type ExpectedRegionSelection = {
  kind: 'province' | 'city' | 'district';
  provinceCode: string;
  provinceName: string;
  cityCode?: string;
  cityName?: string;
  districtCode?: string;
  districtName?: string;
  regionCode: string;
  regionName: string;
};

function districts(items: Array<[string, string]>): RegionDistrict[] {
  return items.map(([code, name]) => ({ code, name }));
}

export const EXPECTED_REGION_PROVINCES: RegionProvince[] = [
  {
    code: '110000',
    name: '北京',
    cities: [
      { code: '110100', name: '北京', districts: districts([
        ['110101', '东城区'], ['110102', '西城区'], ['110105', '朝阳区'], ['110106', '丰台区'],
        ['110107', '石景山区'], ['110108', '海淀区'], ['110109', '门头沟区'], ['110111', '房山区'],
        ['110112', '通州区'], ['110113', '顺义区'], ['110114', '昌平区'], ['110115', '大兴区'],
        ['110116', '怀柔区'], ['110117', '平谷区']
      ]) }
    ]
  },
  {
    code: '120000',
    name: '天津',
    cities: [
      { code: '120100', name: '天津', districts: districts([
        ['120101', '和平区'], ['120102', '河东区'], ['120103', '河西区'], ['120104', '南开区'],
        ['120105', '河北区'], ['120106', '红桥区'], ['120110', '东丽区'], ['120111', '西青区'],
        ['120112', '津南区'], ['120113', '北辰区'], ['120114', '武清区'], ['120116', '滨海新区']
      ]) }
    ]
  },
  {
    code: '130000',
    name: '河北',
    cities: [
      { code: '130100', name: '石家庄', districts: districts([['130102', '长安区'], ['130104', '桥西区'], ['130108', '裕华区'], ['130110', '鹿泉区'], ['130181', '辛集市']]) },
      { code: '130200', name: '唐山', districts: districts([['130202', '路南区'], ['130203', '路北区'], ['130204', '古冶区'], ['130207', '丰南区'], ['130208', '丰润区']]) },
      { code: '130300', name: '秦皇岛', districts: districts([['130302', '海港区'], ['130303', '山海关区'], ['130304', '北戴河区'], ['130306', '抚宁区']]) },
      { code: '130600', name: '保定', districts: districts([['130602', '竞秀区'], ['130606', '莲池区'], ['130607', '满城区'], ['130681', '涿州市']]) }
    ]
  },
  {
    code: '140000',
    name: '山西',
    cities: [
      { code: '140100', name: '太原', districts: districts([['140105', '小店区'], ['140106', '迎泽区'], ['140107', '杏花岭区'], ['140108', '尖草坪区'], ['140109', '万柏林区'], ['140110', '晋源区']]) },
      { code: '140200', name: '大同', districts: districts([['140213', '平城区'], ['140214', '云冈区'], ['140215', '云州区'], ['140221', '阳高县'], ['140222', '天镇县']]) },
      { code: '140700', name: '晋中', districts: districts([['140702', '榆次区'], ['140703', '太谷区'], ['140721', '榆社县'], ['140781', '介休市']]) },
      { code: '140800', name: '运城', districts: districts([
        ['140802', '盐湖区'], ['140821', '临猗县'], ['140822', '万荣县'], ['140823', '闻喜县'],
        ['140824', '稷山县'], ['140825', '新绛县'], ['140826', '绛县'], ['140827', '垣曲县'],
        ['140828', '夏县'], ['140829', '平陆县'], ['140830', '芮城县'], ['140881', '永济市'], ['140882', '河津市']
      ]) }
    ]
  },
  {
    code: '150000',
    name: '内蒙',
    cities: [
      { code: '150100', name: '呼和浩特', districts: districts([['150102', '新城区'], ['150103', '回民区'], ['150104', '玉泉区'], ['150105', '赛罕区']]) },
      { code: '150200', name: '包头', districts: districts([['150202', '东河区'], ['150203', '昆都仑区'], ['150204', '青山区'], ['150207', '九原区']]) },
      { code: '150600', name: '鄂尔多斯', districts: districts([['150602', '东胜区'], ['150603', '康巴什区'], ['150621', '达拉特旗'], ['150627', '伊金霍洛旗']]) }
    ]
  },
  {
    code: '210000',
    name: '辽宁',
    cities: [
      { code: '210100', name: '沈阳', districts: districts([['210102', '和平区'], ['210103', '沈河区'], ['210104', '大东区'], ['210105', '皇姑区'], ['210106', '铁西区'], ['210112', '浑南区']]) },
      { code: '210200', name: '大连', districts: districts([['210202', '中山区'], ['210203', '西岗区'], ['210204', '沙河口区'], ['210211', '甘井子区'], ['210213', '金州区']]) }
    ]
  },
  {
    code: '220000',
    name: '吉林',
    cities: [
      { code: '220100', name: '长春', districts: districts([['220102', '南关区'], ['220103', '宽城区'], ['220104', '朝阳区'], ['220105', '二道区'], ['220106', '绿园区']]) },
      { code: '220200', name: '吉林', districts: districts([['220202', '昌邑区'], ['220203', '龙潭区'], ['220204', '船营区'], ['220211', '丰满区']]) }
    ]
  },
  {
    code: '230000',
    name: '黑龙江',
    cities: [
      { code: '230100', name: '哈尔滨', districts: districts([['230102', '道里区'], ['230103', '南岗区'], ['230104', '道外区'], ['230108', '平房区'], ['230109', '松北区'], ['230110', '香坊区']]) },
      { code: '230200', name: '齐齐哈尔', districts: districts([['230202', '龙沙区'], ['230203', '建华区'], ['230204', '铁锋区'], ['230206', '富拉尔基区']]) }
    ]
  },
  {
    code: '310000',
    name: '上海',
    cities: [
      { code: '310100', name: '上海', districts: districts([
        ['310101', '黄浦区'], ['310104', '徐汇区'], ['310105', '长宁区'], ['310106', '静安区'],
        ['310107', '普陀区'], ['310109', '虹口区'], ['310110', '杨浦区'], ['310112', '闵行区'],
        ['310113', '宝山区'], ['310114', '嘉定区'], ['310115', '浦东新区'], ['310116', '金山区'],
        ['310117', '松江区'], ['310118', '青浦区'], ['310120', '奉贤区'], ['310151', '崇明区']
      ]) }
    ]
  },
  {
    code: '320000',
    name: '江苏',
    cities: [
      { code: '320100', name: '南京', districts: districts([['320102', '玄武区'], ['320104', '秦淮区'], ['320105', '建邺区'], ['320106', '鼓楼区'], ['320111', '浦口区'], ['320115', '江宁区']]) },
      { code: '320500', name: '苏州', districts: districts([['320505', '虎丘区'], ['320506', '吴中区'], ['320507', '相城区'], ['320508', '姑苏区'], ['320509', '吴江区'], ['320581', '常熟市'], ['320582', '张家港市'], ['320583', '昆山市']]) },
      { code: '320200', name: '无锡', districts: districts([['320205', '锡山区'], ['320206', '惠山区'], ['320211', '滨湖区'], ['320213', '梁溪区'], ['320214', '新吴区']]) }
    ]
  },
  {
    code: '330000',
    name: '浙江',
    cities: [
      { code: '330100', name: '杭州', districts: districts([['330102', '上城区'], ['330105', '拱墅区'], ['330106', '西湖区'], ['330108', '滨江区'], ['330109', '萧山区'], ['330110', '余杭区'], ['330114', '钱塘区']]) },
      { code: '330200', name: '宁波', districts: districts([['330203', '海曙区'], ['330205', '江北区'], ['330206', '北仑区'], ['330211', '镇海区'], ['330212', '鄞州区']]) },
      { code: '330300', name: '温州', districts: districts([['330302', '鹿城区'], ['330303', '龙湾区'], ['330304', '瓯海区'], ['330305', '洞头区'], ['330381', '瑞安市']]) }
    ]
  },
  {
    code: '340000',
    name: '安徽',
    cities: [
      { code: '340100', name: '合肥', districts: districts([['340102', '瑶海区'], ['340103', '庐阳区'], ['340104', '蜀山区'], ['340111', '包河区'], ['340121', '长丰县']]) },
      { code: '340200', name: '芜湖', districts: districts([['340202', '镜湖区'], ['340207', '鸠江区'], ['340209', '弋江区'], ['340210', '湾沚区']]) }
    ]
  },
  {
    code: '350000',
    name: '福建',
    cities: [
      { code: '350100', name: '福州', districts: districts([['350102', '鼓楼区'], ['350103', '台江区'], ['350104', '仓山区'], ['350111', '晋安区'], ['350112', '长乐区']]) },
      { code: '350200', name: '厦门', districts: districts([['350203', '思明区'], ['350205', '海沧区'], ['350206', '湖里区'], ['350211', '集美区'], ['350212', '同安区'], ['350213', '翔安区']]) }
    ]
  },
  {
    code: '360000',
    name: '江西',
    cities: [
      { code: '360100', name: '南昌', districts: districts([['360102', '东湖区'], ['360103', '西湖区'], ['360104', '青云谱区'], ['360111', '青山湖区'], ['360112', '新建区']]) },
      { code: '360700', name: '赣州', districts: districts([['360702', '章贡区'], ['360703', '南康区'], ['360704', '赣县区'], ['360781', '瑞金市']]) }
    ]
  },
  {
    code: '370000',
    name: '山东',
    cities: [
      { code: '370100', name: '济南', districts: districts([['370102', '历下区'], ['370103', '市中区'], ['370104', '槐荫区'], ['370105', '天桥区'], ['370112', '历城区']]) },
      { code: '370200', name: '青岛', districts: districts([['370202', '市南区'], ['370203', '市北区'], ['370211', '黄岛区'], ['370212', '崂山区'], ['370214', '城阳区']]) },
      { code: '370600', name: '烟台', districts: districts([['370602', '芝罘区'], ['370611', '福山区'], ['370612', '牟平区'], ['370613', '莱山区']]) }
    ]
  },
  {
    code: '410000',
    name: '河南',
    cities: [
      { code: '410100', name: '郑州', districts: districts([['410102', '中原区'], ['410103', '二七区'], ['410104', '管城回族区'], ['410105', '金水区'], ['410108', '惠济区']]) },
      { code: '410300', name: '洛阳', districts: districts([['410302', '老城区'], ['410303', '西工区'], ['410305', '涧西区'], ['410311', '洛龙区']]) }
    ]
  },
  {
    code: '420000',
    name: '湖北',
    cities: [
      { code: '420100', name: '武汉', districts: districts([['420102', '江岸区'], ['420103', '江汉区'], ['420104', '硚口区'], ['420105', '汉阳区'], ['420106', '武昌区'], ['420111', '洪山区'], ['420112', '东西湖区']]) },
      { code: '420500', name: '宜昌', districts: districts([['420502', '西陵区'], ['420503', '伍家岗区'], ['420504', '点军区'], ['420506', '夷陵区']]) }
    ]
  },
  {
    code: '430000',
    name: '湖南',
    cities: [
      { code: '430100', name: '长沙', districts: districts([['430102', '芙蓉区'], ['430103', '天心区'], ['430104', '岳麓区'], ['430105', '开福区'], ['430111', '雨花区'], ['430112', '望城区']]) },
      { code: '430200', name: '株洲', districts: districts([['430202', '荷塘区'], ['430203', '芦淞区'], ['430204', '石峰区'], ['430211', '天元区']]) }
    ]
  },
  {
    code: '440000',
    name: '广东',
    cities: [
      { code: '440100', name: '广州', districts: districts([['440103', '荔湾区'], ['440104', '越秀区'], ['440105', '海珠区'], ['440106', '天河区'], ['440111', '白云区'], ['440112', '黄埔区'], ['440113', '番禺区'], ['440114', '花都区']]) },
      { code: '440300', name: '深圳', districts: districts([['440303', '罗湖区'], ['440304', '福田区'], ['440305', '南山区'], ['440306', '宝安区'], ['440307', '龙岗区'], ['440308', '盐田区'], ['440309', '龙华区'], ['440310', '坪山区']]) },
      { code: '441900', name: '东莞', districts: districts([['441901', '莞城区'], ['441902', '南城区'], ['441903', '东城区'], ['441904', '万江区'], ['441905', '虎门镇'], ['441906', '长安镇']]) },
      { code: '440600', name: '佛山', districts: districts([['440604', '禅城区'], ['440605', '南海区'], ['440606', '顺德区'], ['440607', '三水区'], ['440608', '高明区']]) }
    ]
  },
  {
    code: '450000',
    name: '广西',
    cities: [
      { code: '450100', name: '南宁', districts: districts([['450102', '兴宁区'], ['450103', '青秀区'], ['450105', '江南区'], ['450107', '西乡塘区'], ['450108', '良庆区']]) },
      { code: '450300', name: '桂林', districts: districts([['450302', '秀峰区'], ['450303', '叠彩区'], ['450304', '象山区'], ['450305', '七星区']]) }
    ]
  },
  {
    code: '460000',
    name: '海南',
    cities: [
      { code: '460100', name: '海口', districts: districts([['460105', '秀英区'], ['460106', '龙华区'], ['460107', '琼山区'], ['460108', '美兰区']]) },
      { code: '460200', name: '三亚', districts: districts([['460202', '海棠区'], ['460203', '吉阳区'], ['460204', '天涯区'], ['460205', '崖州区']]) }
    ]
  },
  {
    code: '500000',
    name: '重庆',
    cities: [
      { code: '500100', name: '重庆', districts: districts([
        ['500103', '渝中区'], ['500104', '大渡口区'], ['500105', '江北区'], ['500106', '沙坪坝区'],
        ['500107', '九龙坡区'], ['500108', '南岸区'], ['500109', '北碚区'], ['500112', '渝北区'],
        ['500113', '巴南区'], ['500115', '长寿区'], ['500116', '江津区'], ['500117', '合川区']
      ]) }
    ]
  },
  {
    code: '510000',
    name: '四川',
    cities: [
      { code: '510100', name: '成都', districts: districts([['510104', '锦江区'], ['510105', '青羊区'], ['510106', '金牛区'], ['510107', '武侯区'], ['510108', '成华区'], ['510112', '龙泉驿区'], ['510116', '双流区']]) },
      { code: '510700', name: '绵阳', districts: districts([['510703', '涪城区'], ['510704', '游仙区'], ['510705', '安州区'], ['510781', '江油市']]) }
    ]
  },
  {
    code: '520000',
    name: '贵州',
    cities: [
      { code: '520100', name: '贵阳', districts: districts([['520102', '南明区'], ['520103', '云岩区'], ['520111', '花溪区'], ['520112', '乌当区'], ['520115', '观山湖区']]) },
      { code: '520300', name: '遵义', districts: districts([['520302', '红花岗区'], ['520303', '汇川区'], ['520304', '播州区'], ['520381', '赤水市']]) }
    ]
  },
  {
    code: '530000',
    name: '云南',
    cities: [
      { code: '530100', name: '昆明', districts: districts([['530102', '五华区'], ['530103', '盘龙区'], ['530111', '官渡区'], ['530112', '西山区'], ['530114', '呈贡区']]) },
      { code: '532900', name: '大理', districts: districts([['532901', '大理市'], ['532922', '漾濞县'], ['532923', '祥云县'], ['532924', '宾川县']]) }
    ]
  },
  {
    code: '540000',
    name: '西藏',
    cities: [
      { code: '540100', name: '拉萨', districts: districts([['540102', '城关区'], ['540103', '堆龙德庆区'], ['540104', '达孜区'], ['540121', '林周县']]) },
      { code: '540200', name: '日喀则', districts: districts([['540202', '桑珠孜区'], ['540221', '南木林县'], ['540222', '江孜县']]) }
    ]
  },
  {
    code: '610000',
    name: '陕西',
    cities: [
      { code: '610100', name: '西安', districts: districts([['610102', '新城区'], ['610103', '碑林区'], ['610104', '莲湖区'], ['610111', '灞桥区'], ['610112', '未央区'], ['610113', '雁塔区'], ['610116', '长安区']]) },
      { code: '610400', name: '咸阳', districts: districts([['610402', '秦都区'], ['610403', '杨陵区'], ['610404', '渭城区'], ['610481', '兴平市']]) }
    ]
  },
  {
    code: '620000',
    name: '甘肃',
    cities: [
      { code: '620100', name: '兰州', districts: districts([['620102', '城关区'], ['620103', '七里河区'], ['620104', '西固区'], ['620105', '安宁区']]) },
      { code: '620700', name: '张掖', districts: districts([['620702', '甘州区'], ['620721', '肃南县'], ['620722', '民乐县']]) }
    ]
  },
  {
    code: '630000',
    name: '青海',
    cities: [
      { code: '630100', name: '西宁', districts: districts([['630102', '城东区'], ['630103', '城中区'], ['630104', '城西区'], ['630105', '城北区']]) },
      { code: '632800', name: '海西', districts: districts([['632801', '格尔木市'], ['632802', '德令哈市'], ['632821', '乌兰县']]) }
    ]
  },
  {
    code: '640000',
    name: '宁夏',
    cities: [
      { code: '640100', name: '银川', districts: districts([['640104', '兴庆区'], ['640105', '西夏区'], ['640106', '金凤区'], ['640121', '永宁县']]) },
      { code: '640200', name: '石嘴山', districts: districts([['640202', '大武口区'], ['640205', '惠农区'], ['640221', '平罗县']]) }
    ]
  },
  {
    code: '650000',
    name: '新疆',
    cities: [
      { code: '650100', name: '乌鲁木齐', districts: districts([['650102', '天山区'], ['650103', '沙依巴克区'], ['650104', '新市区'], ['650105', '水磨沟区'], ['650106', '头屯河区']]) },
      { code: '650200', name: '克拉玛依', districts: districts([['650202', '独山子区'], ['650203', '克拉玛依区'], ['650204', '白碱滩区'], ['650205', '乌尔禾区']]) }
    ]
  },
  {
    code: '710000',
    name: '台湾',
    cities: [
      { code: '710100', name: '台北', districts: districts([['710101', '中正区'], ['710102', '大同区'], ['710103', '中山区'], ['710104', '松山区']]) },
      { code: '710200', name: '高雄', districts: districts([['710201', '新兴区'], ['710202', '前金区'], ['710203', '苓雅区'], ['710204', '盐埕区']]) }
    ]
  },
  {
    code: '810000',
    name: '香港',
    cities: [
      { code: '810100', name: '香港', districts: districts([['810101', '中西区'], ['810102', '湾仔区'], ['810103', '东区'], ['810104', '南区'], ['810105', '油尖旺区'], ['810106', '深水埗区']]) }
    ]
  },
  {
    code: '820000',
    name: '澳门',
    cities: [
      { code: '820100', name: '澳门', districts: districts([['820101', '花地玛堂区'], ['820102', '花王堂区'], ['820103', '望德堂区'], ['820104', '大堂区'], ['820105', '风顺堂区']]) }
    ]
  },
  {
    code: '900000',
    name: '海外',
    cities: [
      { code: '900100', name: '亚洲', districts: districts([['900101', '新加坡'], ['900102', '日本'], ['900103', '韩国'], ['900104', '马来西亚'], ['900105', '泰国']]) },
      { code: '900200', name: '欧洲', districts: districts([['900201', '英国'], ['900202', '德国'], ['900203', '法国'], ['900204', '荷兰'], ['900205', '意大利']]) },
      { code: '900300', name: '美洲', districts: districts([['900301', '美国'], ['900302', '加拿大'], ['900303', '墨西哥'], ['900304', '巴西']]) },
      { code: '900400', name: '大洋洲', districts: districts([['900401', '澳大利亚'], ['900402', '新西兰']]) }
    ]
  }
];

export function regionSelectionKey(selection: ExpectedRegionSelection): string {
  return `${selection.kind}::${selection.regionCode}`;
}

export function summarizeExpectedRegions(regions: string[]): string {
  return regions.length > 0 ? regions.join('，') : '请选择期望地区，最多6个';
}

export function cityCodeForExpectedRegion(regionName: string): string {
  const normalized = regionName.trim();
  if (!normalized) {
    return '';
  }
  for (const province of EXPECTED_REGION_PROVINCES) {
    if (province.name === normalized || `全${province.name}` === normalized) {
      return province.code;
    }
    for (const city of province.cities) {
      if (city.name === normalized || `全${city.name}` === normalized) {
        return city.code;
      }
      const district = city.districts.find((item) => item.name === normalized);
      if (district) {
        return district.code;
      }
    }
  }
  return '';
}

function selectionFromProvince(province: RegionProvince, regionName?: string): ExpectedRegionSelection {
  return {
    kind: 'province',
    provinceCode: province.code,
    provinceName: province.name,
    regionCode: province.code,
    regionName: regionName ?? `全${province.name}`
  };
}

function selectionFromCity(province: RegionProvince, city: RegionCity, regionName?: string): ExpectedRegionSelection {
  return {
    kind: 'city',
    provinceCode: province.code,
    provinceName: province.name,
    cityCode: city.code,
    cityName: city.name,
    regionCode: city.code,
    regionName: regionName ?? `全${city.name}`
  };
}

function selectionFromDistrict(
  province: RegionProvince,
  city: RegionCity,
  district: RegionDistrict
): ExpectedRegionSelection {
  return {
    kind: 'district',
    provinceCode: province.code,
    provinceName: province.name,
    cityCode: city.code,
    cityName: city.name,
    districtCode: district.code,
    districtName: district.name,
    regionCode: district.code,
    regionName: district.name
  };
}

function customSelection(regionName: string, code = ''): ExpectedRegionSelection {
  return {
    kind: 'district',
    provinceCode: 'custom',
    provinceName: '自定义',
    cityCode: code || 'custom',
    cityName: '自定义',
    districtCode: code || regionName,
    districtName: regionName,
    regionCode: code || regionName,
    regionName
  };
}

export function normalizeExpectedRegionSelections(
  regions: string[],
  selectedCityCode = ''
): ExpectedRegionSelection[] {
  return regions
    .map((regionName, index) => {
      const normalized = regionName.trim();
      if (!normalized) {
        return undefined;
      }
      const preferredCode = index === 0 ? selectedCityCode : '';
      for (const province of EXPECTED_REGION_PROVINCES) {
        if (province.code === preferredCode || province.name === normalized || `全${province.name}` === normalized) {
          return selectionFromProvince(province, normalized.startsWith('全') ? normalized : undefined);
        }
        for (const city of province.cities) {
          if (city.code === preferredCode || city.name === normalized || `全${city.name}` === normalized) {
            return selectionFromCity(province, city, normalized.startsWith('全') ? normalized : undefined);
          }
          const district = city.districts.find((item) => item.code === preferredCode || item.name === normalized);
          if (district) {
            return selectionFromDistrict(province, city, district);
          }
        }
      }
      return customSelection(normalized, preferredCode);
    })
    .filter((item): item is ExpectedRegionSelection => Boolean(item))
    .slice(0, MAX_EXPECTED_REGIONS);
}
