import React, { useEffect, useRef, useState } from 'react';
import { Search } from 'lucide-react';
import { Part } from '../types/inventory';

interface Props {
  parts: Part[];
  value: number; // currently selected partId, 0 = none
  onSelect: (part: Part) => void;
  placeholder?: string;
}

// A searchable part combobox — types like a text input, but only ever resolves to a
// real Part from inventory-service, so a line item can never end up pointing at a
// part id that doesn't exist. Filters client-side against the part list the parent
// page already fetched once (passed in as a prop, so N line rows share one fetch
// instead of each firing its own).
export const PartPicker: React.FC<Props> = ({ parts, value, onSelect, placeholder }) => {
  const [query, setQuery] = useState('');
  const [open, setOpen] = useState(false);
  const wrapperRef = useRef<HTMLDivElement>(null);

  const selected = parts.find(p => p.id === value);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const filtered = (query.trim()
    ? parts.filter(p =>
        p.name.toLowerCase().includes(query.toLowerCase()) ||
        (p.serialNumber || '').toLowerCase().includes(query.toLowerCase()))
    : parts
  ).slice(0, 30);

  return (
    <div className="relative" ref={wrapperRef}>
      <div className="relative">
        <Search size={12} className="absolute left-2 top-1/2 -translate-y-1/2 text-slate-300" />
        <input
          value={open ? query : (selected ? `${selected.serialNumber ? selected.serialNumber + ' · ' : ''}${selected.name}` : '')}
          onChange={e => { setQuery(e.target.value); setOpen(true); }}
          onFocus={() => { setQuery(''); setOpen(true); }}
          placeholder={placeholder || 'Search part by name or #...'}
          className="w-full pl-6 pr-2 py-2 bg-white border border-slate-200 rounded-lg text-xs"
        />
      </div>
      {open && (
        <div className="absolute z-20 mt-1 w-72 max-h-56 overflow-y-auto bg-white border border-slate-200 rounded-xl shadow-xl">
          {filtered.length === 0 ? (
            <div className="p-3 text-[11px] text-slate-400 font-bold text-center">No parts found</div>
          ) : filtered.map(p => (
            <button type="button" key={p.id}
              onClick={() => { onSelect(p); setOpen(false); setQuery(''); }}
              className="w-full text-left px-3 py-2 hover:bg-slate-50 text-xs border-b border-slate-50 last:border-0 transition-colors">
              <div className="font-bold text-slate-800">{p.name}</div>
              <div className="text-slate-400 flex items-center justify-between mt-0.5">
                <span>{p.serialNumber || '—'}</span>
                <span className={p.quantity && p.quantity > 0 ? 'text-emerald-600 font-bold' : 'text-red-400 font-bold'}>
                  {p.quantity ?? 0} in stock
                </span>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
};
