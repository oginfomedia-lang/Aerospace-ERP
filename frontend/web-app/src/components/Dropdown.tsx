import React from 'react';

// Generic, reusable dropdown backed by whatever list the caller already fetched —
// not supplier-specific, so the same component can back Accounts/CostCenters/etc
// later without changing its contract. Renders a plain controlled <select>, styled
// to match the existing inline selects in JournalEntriesPage.tsx/PostingRulesPage.tsx.
export interface DropdownOption<T extends string | number = string | number> {
  value: T;
  label: string;
  /** shown muted next to the label, e.g. a vendor code */
  sublabel?: string;
}

interface DropdownProps<T extends string | number = string | number> {
  options: DropdownOption<T>[];
  value: T | '';
  onChange: (value: T) => void;
  placeholder?: string;
  loading?: boolean;
  disabled?: boolean;
  /** escape hatch for grid-col-span sizing, matches call-site usage */
  className?: string;
  emptyMessage?: string;
}

export function Dropdown<T extends string | number = string | number>({
  options,
  value,
  onChange,
  placeholder = '— Select —',
  loading = false,
  disabled = false,
  className = '',
  emptyMessage = 'No options available',
}: DropdownProps<T>) {
  return (
    <select
      value={value}
      disabled={disabled || loading}
      onChange={(e) => {
        const raw = e.target.value;
        if (raw === '') return;
        // options carry either string or number values — coerce back to number
        // when the matching option's value is a number, so callers get T back,
        // not always a string (native <select> values are always strings).
        const matched = options.find((o) => String(o.value) === raw);
        if (matched) onChange(matched.value);
      }}
      className={
        className ||
        'w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm disabled:opacity-60'
      }
    >
      <option value="">{loading ? 'Loading…' : placeholder}</option>
      {!loading && options.length === 0 && (
        <option value="" disabled>{emptyMessage}</option>
      )}
      {options.map((o) => (
        <option key={String(o.value)} value={String(o.value)}>
          {o.label}{o.sublabel ? ` — ${o.sublabel}` : ''}
        </option>
      ))}
    </select>
  );
}
