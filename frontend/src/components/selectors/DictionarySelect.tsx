'use client';

import { useRef, useState } from 'react';
import styles from './DictionarySelect.module.css';
import { useOutsidePointerDown } from './useOutsidePointerDown';

export type DictionarySelectOption = {
  value: string;
  label: string;
};

export function dictionaryOptionLabel(
  options: DictionarySelectOption[],
  value: string,
  placeholder = '请选择'
): string {
  if (!value) {
    return placeholder;
  }
  return options.find((option) => option.value === value)?.label ?? value;
}

export function DictionarySelect({
  label,
  value,
  options,
  onChange,
  placeholder = '请选择'
}: {
  label: string;
  value: string;
  options: DictionarySelectOption[];
  onChange: (value: string) => void;
  placeholder?: string;
}) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLLabelElement>(null);
  const displayLabel = dictionaryOptionLabel(options, value, placeholder);
  useOutsidePointerDown(rootRef, open, () => setOpen(false));

  return (
    <label className={styles.field} ref={rootRef}>
      <span className={styles.label}>{label}</span>
      <button
        type="button"
        className={open ? styles.triggerOpen : styles.trigger}
        aria-label={`${label} ${displayLabel}`}
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
      >
        <span className={value ? styles.value : styles.placeholder}>{displayLabel}</span>
        <span aria-hidden="true" className={styles.arrow}>⌃</span>
      </button>
      {open ? (
        <div className={styles.menu} role="listbox">
          {options.map((option) => (
            <button
              key={option.value}
              type="button"
              role="option"
              aria-selected={value === option.value}
              className={value === option.value ? styles.optionActive : styles.option}
              onClick={() => {
                onChange(option.value);
                setOpen(false);
              }}
            >
              {option.label}
            </button>
          ))}
        </div>
      ) : null}
    </label>
  );
}

function selectedLabels(options: DictionarySelectOption[], values: string[], placeholder: string): string {
  if (values.length === 0) {
    return placeholder;
  }
  return values.map((value) => dictionaryOptionLabel(options, value, value)).join('，');
}

export function DictionaryMultiSelect({
  label,
  values,
  options,
  onChange,
  placeholder = '请选择',
  max = 12
}: {
  label: string;
  values: string[];
  options: DictionarySelectOption[];
  onChange: (values: string[]) => void;
  placeholder?: string;
  max?: number;
}) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLLabelElement>(null);
  const displayLabel = selectedLabels(options, values, placeholder);
  useOutsidePointerDown(rootRef, open, () => setOpen(false));

  function toggle(value: string) {
    if (values.includes(value)) {
      onChange(values.filter((item) => item !== value));
      return;
    }
    if (values.length >= max) {
      return;
    }
    onChange([...values, value]);
  }

  return (
    <label className={styles.field} ref={rootRef}>
      <span className={styles.label}>{label}</span>
      <button
        type="button"
        className={open ? styles.triggerOpen : styles.trigger}
        aria-label={`${label} ${displayLabel}`}
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
      >
        <span className={values.length > 0 ? styles.value : styles.placeholder}>{displayLabel}</span>
        <span aria-hidden="true" className={styles.arrow}>⌃</span>
      </button>
      {open ? (
        <div className={styles.menu} role="listbox" aria-multiselectable="true">
          {values.length > 0 ? (
            <button type="button" className={styles.clearOption} onClick={() => onChange([])}>
              清空已选
            </button>
          ) : null}
          {options.map((option) => {
            const checked = values.includes(option.value);
            const disabled = !checked && values.length >= max;
            return (
              <button
                key={option.value}
                type="button"
                role="option"
                aria-selected={checked}
                disabled={disabled}
                className={checked ? styles.optionActive : styles.option}
                onClick={() => toggle(option.value)}
              >
                <span className={styles.optionCheck} aria-hidden="true">{checked ? '✓' : ''}</span>
                {option.label}
              </button>
            );
          })}
        </div>
      ) : null}
    </label>
  );
}
