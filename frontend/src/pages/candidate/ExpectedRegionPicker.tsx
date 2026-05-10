'use client';

import { useEffect, useMemo, useState } from 'react';
import {
  EXPECTED_REGION_PROVINCES,
  MAX_EXPECTED_REGIONS,
  normalizeExpectedRegionSelections,
  regionSelectionKey,
  summarizeExpectedRegions,
  type ExpectedRegionSelection,
  type RegionCity,
  type RegionDistrict,
  type RegionProvince
} from './expectedRegionCatalog';
import styles from './ExpectedRegionPicker.module.css';

type ExpectedRegionPickerProps = {
  selectedRegions: string[];
  selectedCityCode?: string;
  onSave: (selection: { regions: string[]; cityCode: string }) => void;
};

function selectionMatches(selection: ExpectedRegionSelection, kind: ExpectedRegionSelection['kind'], code: string) {
  return selection.kind === kind && selection.regionCode === code;
}

function countByProvince(draft: ExpectedRegionSelection[], provinceCode: string): number {
  return draft.filter((item) => item.provinceCode === provinceCode).length;
}

function countByCity(draft: ExpectedRegionSelection[], cityCode: string): number {
  return draft.filter((item) => item.cityCode === cityCode).length;
}

function filterRegions(query: string): RegionProvince[] {
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return EXPECTED_REGION_PROVINCES;
  }

  return EXPECTED_REGION_PROVINCES
    .map((province) => {
      const provinceMatched = province.name.toLowerCase().includes(normalized);
      const cities = provinceMatched
        ? province.cities
        : province.cities
          .map((city) => {
            const cityMatched = city.name.toLowerCase().includes(normalized);
            const districts = cityMatched
              ? city.districts
              : city.districts.filter((district) => district.name.toLowerCase().includes(normalized));
            return { ...city, districts };
          })
          .filter((city) => city.districts.length > 0);
      return { ...province, cities };
    })
    .filter((province) => province.cities.length > 0);
}

function makeProvinceSelection(province: RegionProvince): ExpectedRegionSelection {
  return {
    kind: 'province',
    provinceCode: province.code,
    provinceName: province.name,
    regionCode: province.code,
    regionName: `全${province.name}`
  };
}

function makeCitySelection(province: RegionProvince, city: RegionCity): ExpectedRegionSelection {
  return {
    kind: 'city',
    provinceCode: province.code,
    provinceName: province.name,
    cityCode: city.code,
    cityName: city.name,
    regionCode: city.code,
    regionName: `全${city.name}`
  };
}

function makeDistrictSelection(
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

export function ExpectedRegionPicker({
  selectedRegions,
  selectedCityCode = '',
  onSave
}: ExpectedRegionPickerProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [activeProvinceCode, setActiveProvinceCode] = useState(EXPECTED_REGION_PROVINCES[0].code);
  const [activeCityCode, setActiveCityCode] = useState(EXPECTED_REGION_PROVINCES[0].cities[0].code);
  const selectedRegionsKey = selectedRegions.join('|');
  const [draft, setDraft] = useState<ExpectedRegionSelection[]>(() =>
    normalizeExpectedRegionSelections(selectedRegions, selectedCityCode)
  );

  useEffect(() => {
    if (!open) {
      setDraft(normalizeExpectedRegionSelections(selectedRegions, selectedCityCode));
    }
  }, [open, selectedCityCode, selectedRegionsKey]);

  const visibleProvinces = useMemo(() => filterRegions(query), [query]);
  const activeProvince = visibleProvinces.find((province) => province.code === activeProvinceCode)
    ?? visibleProvinces[0]
    ?? null;
  const activeCity = activeProvince?.cities.find((city) => city.code === activeCityCode)
    ?? activeProvince?.cities[0]
    ?? null;

  useEffect(() => {
    if (!activeProvince) {
      return;
    }
    if (!activeProvince.cities.some((city) => city.code === activeCityCode)) {
      setActiveCityCode(activeProvince.cities[0]?.code ?? '');
    }
  }, [activeCityCode, activeProvince]);

  function resetDraftAndClose() {
    setDraft(normalizeExpectedRegionSelections(selectedRegions, selectedCityCode));
    setQuery('');
    setOpen(false);
  }

  function selectProvince(province: RegionProvince) {
    const selection = makeProvinceSelection(province);
    setDraft((current) => {
      const exists = current.some((item) => selectionMatches(item, 'province', province.code));
      if (exists) {
        return current.filter((item) => !selectionMatches(item, 'province', province.code));
      }
      const withoutChildren = current.filter((item) => item.provinceCode !== province.code);
      if (withoutChildren.length >= MAX_EXPECTED_REGIONS) {
        return withoutChildren;
      }
      return [...withoutChildren, selection];
    });
  }

  function selectCity(province: RegionProvince, city: RegionCity) {
    const selection = makeCitySelection(province, city);
    setDraft((current) => {
      const exists = current.some((item) => selectionMatches(item, 'city', city.code));
      if (exists) {
        return current.filter((item) => !selectionMatches(item, 'city', city.code));
      }
      const withoutParentAndChildren = current.filter((item) =>
        item.regionCode !== province.code && item.cityCode !== city.code
      );
      if (withoutParentAndChildren.length >= MAX_EXPECTED_REGIONS) {
        return withoutParentAndChildren;
      }
      return [...withoutParentAndChildren, selection];
    });
  }

  function selectDistrict(province: RegionProvince, city: RegionCity, district: RegionDistrict) {
    const selection = makeDistrictSelection(province, city, district);
    setDraft((current) => {
      const exists = current.some((item) => selectionMatches(item, 'district', district.code));
      if (exists) {
        return current.filter((item) => !selectionMatches(item, 'district', district.code));
      }
      const withoutParents = current.filter((item) =>
        item.regionCode !== province.code && item.regionCode !== city.code
      );
      if (withoutParents.length >= MAX_EXPECTED_REGIONS) {
        return withoutParents;
      }
      return [...withoutParents, selection];
    });
  }

  function removeSelection(key: string) {
    setDraft((current) => current.filter((item) => regionSelectionKey(item) !== key));
  }

  function saveSelection() {
    onSave({
      regions: draft.map((item) => item.regionName),
      cityCode: draft[0]?.regionCode ?? ''
    });
    setQuery('');
    setOpen(false);
  }

  const triggerLabel = selectedRegions.length > 0
    ? `期望地区：${summarizeExpectedRegions(selectedRegions)}`
    : '请选择期望地区，最多6个';

  return (
    <>
      <button
        type="button"
        className={styles.trigger}
        aria-label={triggerLabel}
        aria-haspopup="dialog"
        onClick={() => {
          setDraft(normalizeExpectedRegionSelections(selectedRegions, selectedCityCode));
          setOpen(true);
        }}
      >
        <span className={selectedRegions.length > 0 ? styles.summary : styles.placeholder}>
          {summarizeExpectedRegions(selectedRegions)}
        </span>
        <span className={styles.chevron} aria-hidden="true">⌕</span>
      </button>
      {open ? (
        <div className={styles.backdrop}>
          <section className={styles.dialog} role="dialog" aria-modal="true" aria-labelledby="expected-region-title">
            <header className={styles.header}>
              <div>
                <h2 id="expected-region-title" className={styles.title}>期望地区</h2>
                <p className={styles.hint}>最多选择{MAX_EXPECTED_REGIONS}个</p>
              </div>
              <button className={styles.close} type="button" aria-label="关闭期望地区选择" onClick={resetDraftAndClose}>×</button>
            </header>
            <div className={styles.body}>
              <div className={styles.selectedInput} aria-label="已选期望地区输入框" aria-live="polite">
                <span className={draft.length > 0 ? styles.selectedInputText : styles.selectedInputPlaceholder}>
                  {summarizeExpectedRegions(draft.map((item) => item.regionName))}
                </span>
                {draft.length > 0 ? (
                  <button className={styles.clearButton} type="button" onClick={() => setDraft([])}>
                    清空
                  </button>
                ) : null}
              </div>
              <div className={styles.searchWrap}>
                <input
                  className={styles.search}
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="请输入地区关键词"
                  aria-label="搜索期望地区"
                />
                <span className={styles.searchIcon} aria-hidden="true">⌕</span>
              </div>
              <div className={styles.selector}>
                <nav className={styles.provinces} aria-label="省份">
                  {visibleProvinces.map((province) => {
                    const count = countByProvince(draft, province.code);
                    return (
                      <button
                        key={province.code}
                        type="button"
                        aria-label={province.name}
                        className={activeProvince?.code === province.code ? styles.navButtonActive : styles.navButton}
                        onClick={() => {
                          setActiveProvinceCode(province.code);
                          setActiveCityCode(province.cities[0]?.code ?? '');
                        }}
                      >
                        <span>{province.name}</span>
                        {count > 0 ? <strong aria-hidden="true">{count}</strong> : null}
                      </button>
                    );
                  })}
                </nav>
                <nav className={styles.cities} aria-label="城市">
                  {activeProvince ? (
                    <>
                      <button
                        type="button"
                        aria-label={`选择全${activeProvince.name}`}
                        className={draft.some((item) => selectionMatches(item, 'province', activeProvince.code))
                          ? styles.choiceButtonChecked
                          : styles.choiceButton}
                        disabled={
                          !draft.some((item) => selectionMatches(item, 'province', activeProvince.code))
                          && draft.length >= MAX_EXPECTED_REGIONS
                        }
                        onClick={() => selectProvince(activeProvince)}
                      >
                        全{activeProvince.name}
                      </button>
                      {activeProvince.cities.map((city) => {
                        const count = countByCity(draft, city.code);
                        return (
                          <button
                            key={city.code}
                            type="button"
                            aria-label={city.name}
                            className={activeCity?.code === city.code ? styles.navButtonActive : styles.navButton}
                            onClick={() => setActiveCityCode(city.code)}
                          >
                            <span>{city.name}</span>
                            {count > 0 ? <strong aria-hidden="true">{count}</strong> : null}
                          </button>
                        );
                      })}
                    </>
                  ) : null}
                </nav>
                <div className={styles.districts} aria-label="区县">
                  {activeProvince && activeCity ? (
                    <>
                      <button
                        type="button"
                        aria-label={`选择全${activeCity.name}`}
                        className={draft.some((item) => selectionMatches(item, 'city', activeCity.code))
                          ? styles.choiceButtonChecked
                          : styles.choiceButton}
                        disabled={
                          !draft.some((item) => selectionMatches(item, 'city', activeCity.code))
                          && draft.length >= MAX_EXPECTED_REGIONS
                        }
                        onClick={() => selectCity(activeProvince, activeCity)}
                      >
                        全{activeCity.name}
                      </button>
                      <div className={styles.districtGrid}>
                        {activeCity.districts.map((district) => {
                          const checked = draft.some((item) => selectionMatches(item, 'district', district.code));
                          const disabled = !checked && draft.length >= MAX_EXPECTED_REGIONS;
                          return (
                            <button
                              key={district.code}
                              type="button"
                              aria-label={district.name}
                              className={checked ? styles.districtButtonChecked : styles.districtButton}
                              disabled={disabled}
                              onClick={() => selectDistrict(activeProvince, activeCity, district)}
                            >
                              <span aria-hidden="true">{checked ? '✓' : ''}</span>
                              {district.name}
                            </button>
                          );
                        })}
                      </div>
                    </>
                  ) : (
                    <p className={styles.empty}>没有找到匹配的地区，请换个关键词试试。</p>
                  )}
                </div>
              </div>
              <div className={styles.selectedBar} aria-label="已选期望地区">
                <span>已选 {draft.length}/{MAX_EXPECTED_REGIONS}</span>
                {draft.map((item) => (
                  <span className={styles.chip} key={regionSelectionKey(item)}>
                    {item.regionName}
                    <button
                      type="button"
                      aria-label={`移除${item.regionName}`}
                      onClick={() => removeSelection(regionSelectionKey(item))}
                    >
                      ×
                    </button>
                  </span>
                ))}
              </div>
            </div>
            <footer className={styles.footer}>
              <button className={styles.save} type="button" onClick={saveSelection}>保存</button>
              <button className={styles.cancel} type="button" onClick={resetDraftAndClose}>取消</button>
            </footer>
          </section>
        </div>
      ) : null}
    </>
  );
}
