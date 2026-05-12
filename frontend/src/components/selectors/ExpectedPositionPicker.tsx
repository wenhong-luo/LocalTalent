'use client';

import { useEffect, useMemo, useState } from 'react';
import {
  EXPECTED_POSITION_CATEGORIES,
  MAX_EXPECTED_POSITIONS,
  normalizeExpectedPositionSelections,
  positionNameForStorage,
  type ExpectedPositionSelection
} from '@/shared/catalogs/positionCatalog';
import styles from './ExpectedPositionPicker.module.css';

type ExpectedPositionPickerProps = {
  selectedPositions: string[];
  selectedCategoryCode?: string;
  onSave: (selection: { positions: string[]; categoryCode: string }) => void;
};

function selectionKey(selection: ExpectedPositionSelection): string {
  return `${selection.categoryCode}::${selection.groupName}::${selection.positionName}`;
}

function summarize(positions: string[]): string {
  return positions.length > 0 ? positions.join('，') : '请选择期望职位，最多6个';
}

export function ExpectedPositionPicker({
  selectedPositions,
  selectedCategoryCode = '',
  onSave
}: ExpectedPositionPickerProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState('');
  const [activeCategoryCode, setActiveCategoryCode] = useState(EXPECTED_POSITION_CATEGORIES[0].code);
  const selectedPositionsKey = selectedPositions.join('|');
  const triggerLabel = selectedPositions.length > 0
    ? `期望职位：${summarize(selectedPositions)}`
    : '请选择期望职位，最多6个';
  const [draft, setDraft] = useState<ExpectedPositionSelection[]>(() =>
    normalizeExpectedPositionSelections(selectedPositions, selectedCategoryCode)
  );

  useEffect(() => {
    if (!open) {
      setDraft(normalizeExpectedPositionSelections(selectedPositions, selectedCategoryCode));
    }
  }, [open, selectedCategoryCode, selectedPositionsKey]);

  const normalizedQuery = query.trim().toLowerCase();
  const visibleCategories = useMemo(() => {
    if (!normalizedQuery) {
      return EXPECTED_POSITION_CATEGORIES;
    }
    return EXPECTED_POSITION_CATEGORIES
      .map((category) => ({
        ...category,
        groups: category.name.toLowerCase().includes(normalizedQuery)
          ? category.groups
          : category.groups
            .map((group) => ({
              ...group,
              positions: group.name.toLowerCase().includes(normalizedQuery)
                ? group.positions
                : group.positions.filter((position) => position.toLowerCase().includes(normalizedQuery))
            }))
            .filter((group) => group.positions.length > 0)
      }))
      .filter((category) => category.groups.length > 0);
  }, [normalizedQuery]);

  const activeCategory = visibleCategories.find((category) => category.code === activeCategoryCode)
    ?? visibleCategories[0]
    ?? null;

  function togglePosition(categoryCode: string, groupName: string, positionLabel: string, checked: boolean) {
    const category = EXPECTED_POSITION_CATEGORIES.find((item) => item.code === categoryCode);
    if (!category) {
      return;
    }
    const group = category.groups.find((item) => item.name === groupName);
    if (!group) {
      return;
    }
    const positionName = positionNameForStorage(group, positionLabel);
    const currentSelection: ExpectedPositionSelection = {
      categoryCode: category.code,
      categoryName: category.name,
      groupName: group.name,
      positionName
    };

    setDraft((current) => {
      const currentKey = selectionKey(currentSelection);
      if (!checked) {
        return current.filter((item) => selectionKey(item) !== currentKey);
      }

      const withoutSameCategoryUnlimited = positionLabel === '不限'
        ? current.filter((item) => !(item.categoryCode === category.code && item.groupName === group.name))
        : current.filter((item) => selectionKey(item) !== `${category.code}::${group.name}::${group.name}不限`);

      if (withoutSameCategoryUnlimited.some((item) => selectionKey(item) === currentKey)) {
        return withoutSameCategoryUnlimited;
      }

      if (withoutSameCategoryUnlimited.length >= MAX_EXPECTED_POSITIONS) {
        return withoutSameCategoryUnlimited;
      }

      return [...withoutSameCategoryUnlimited, currentSelection];
    });
  }

  function removeSelection(key: string) {
    setDraft((current) => current.filter((item) => selectionKey(item) !== key));
  }

  function closeAndReset() {
    setDraft(normalizeExpectedPositionSelections(selectedPositions, selectedCategoryCode));
    setQuery('');
    setOpen(false);
  }

  function saveSelection() {
    onSave({
      positions: draft.map((item) => item.positionName),
      categoryCode: draft[0]?.categoryCode ?? ''
    });
    setQuery('');
    setOpen(false);
  }

  return (
    <>
      <button
        type="button"
        className={styles.trigger}
        aria-label={triggerLabel}
        aria-haspopup="dialog"
        onClick={() => {
          setDraft(normalizeExpectedPositionSelections(selectedPositions, selectedCategoryCode));
          setOpen(true);
        }}
      >
        <span className={selectedPositions.length > 0 ? styles.summary : styles.placeholder}>
          {summarize(selectedPositions)}
        </span>
        <span className={styles.chevron} aria-hidden="true">⌕</span>
      </button>
      {open ? (
        <div
          className={styles.backdrop}
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeAndReset();
            }
          }}
        >
          <section className={styles.dialog} role="dialog" aria-modal="true" aria-labelledby="expected-position-title">
            <header className={styles.header}>
              <div>
                <h2 id="expected-position-title" className={styles.title}>期望职位</h2>
                <p className={styles.hint}>最多选择{MAX_EXPECTED_POSITIONS}个</p>
              </div>
              <button className={styles.close} type="button" aria-label="关闭期望职位选择" onClick={closeAndReset}>×</button>
            </header>
            <div className={styles.body}>
              <div className={styles.selectedInput} aria-label="已选期望职位输入框" aria-live="polite">
                <span className={draft.length > 0 ? styles.selectedInputText : styles.selectedInputPlaceholder}>
                  {summarize(draft.map((item) => item.positionName))}
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
                  placeholder="请输入职位类别关键词"
                  aria-label="搜索期望职位"
                />
                <span className={styles.searchIcon} aria-hidden="true">⌕</span>
              </div>
              <div className={styles.selector}>
                <nav className={styles.categories} aria-label="职位大类">
                  {visibleCategories.map((category) => (
                    <button
                      key={category.code}
                      type="button"
                      className={activeCategory?.code === category.code ? styles.categoryButtonActive : styles.categoryButton}
                      onClick={() => setActiveCategoryCode(category.code)}
                    >
                      {category.name}
                    </button>
                  ))}
                </nav>
                <div className={styles.positions}>
                  {activeCategory ? (
                    <>
                      {activeCategory.groups.map((group) => (
                        <section className={styles.positionGroup} key={`${activeCategory.code}::${group.name}`}>
                          <h3 className={styles.positionGroupTitle}>{group.name}</h3>
                          <div className={styles.positionGrid}>
                            {group.positions.map((position) => {
                              const storedName = positionNameForStorage(group, position);
                              const key = `${activeCategory.code}::${group.name}::${storedName}`;
                              const checked = draft.some((item) => selectionKey(item) === key);
                              const disabled = !checked && draft.length >= MAX_EXPECTED_POSITIONS;
                              return (
                                <label
                                  key={key}
                                  className={`${styles.checkboxLabel} ${disabled ? styles.checkboxLabelDisabled : ''}`}
                                >
                                  <input
                                    type="checkbox"
                                    checked={checked}
                                    disabled={disabled}
                                    onChange={(event) =>
                                      togglePosition(activeCategory.code, group.name, position, event.target.checked)
                                    }
                                  />
                                  {position}
                                </label>
                              );
                            })}
                          </div>
                        </section>
                      ))}
                    </>
                  ) : (
                    <p className={styles.empty}>没有找到匹配的职位类别，请换个关键词试试。</p>
                  )}
                </div>
              </div>
              <div className={styles.selectedBar} aria-label="已选期望职位">
                <span>已选 {draft.length}/{MAX_EXPECTED_POSITIONS}</span>
                {draft.map((item) => (
                  <span className={styles.chip} key={selectionKey(item)}>
                    {item.positionName}
                    <button type="button" aria-label={`移除${item.positionName}`} onClick={() => removeSelection(selectionKey(item))}>×</button>
                  </span>
                ))}
              </div>
            </div>
            <footer className={styles.footer}>
              <button className={styles.save} type="button" onClick={saveSelection}>保存</button>
              <button className={styles.cancel} type="button" onClick={closeAndReset}>取消</button>
            </footer>
          </section>
        </div>
      ) : null}
    </>
  );
}
