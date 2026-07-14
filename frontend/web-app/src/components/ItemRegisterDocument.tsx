import React from 'react';
import { Part } from '../types/inventory';
import { OrganizationProfile } from '../types/procurement';

interface Props {
  parts: Part[];
  org: OrganizationProfile | null;
}

// A formal printable register of the entire item master — same plain boxed
// black-on-white document style as POReceiptDocument/MaterialReceiptDocument/
// GrnReceiptDocument (rendered inside ReceiptModal with `plain`).
export const ItemRegisterDocument: React.FC<Props> = ({ parts, org }) => {
  return (
    <div className="text-black font-sans border-2 border-black text-xs">
      <div className="text-center border-b-2 border-black py-3">
        <div className="font-bold text-base">{org?.companyName || 'Company Name'}</div>
        {org?.addressLine && <div className="text-[10px]">{org.addressLine}</div>}
      </div>

      <div className="bg-slate-300 text-center font-bold text-sm py-2 border-b-2 border-black uppercase tracking-wide">
        Item Register
      </div>

      <div className="px-3 py-1.5 border-b border-black text-[10px] font-bold flex justify-between">
        <span>Total Items: {parts.length}</span>
        <span>Printed: {new Date().toLocaleDateString()}</span>
      </div>

      <table className="w-full border-collapse">
        <thead>
          <tr className="border-b-2 border-black bg-slate-100 font-bold">
            <th className="border-r border-black px-2 py-1.5 w-10">Sl.</th>
            <th className="border-r border-black px-2 py-1.5 text-left">SKU</th>
            <th className="border-r border-black px-2 py-1.5 text-left">Name</th>
            <th className="border-r border-black px-2 py-1.5 text-left">Description</th>
            <th className="border-r border-black px-2 py-1.5">HSN Code</th>
            <th className="border-r border-black px-2 py-1.5">Tax %</th>
            <th className="border-r border-black px-2 py-1.5 text-left">Part Type</th>
            <th className="border-r border-black px-2 py-1.5">Criticality</th>
            <th className="border-r border-black px-2 py-1.5">Reorder Lvl</th>
            <th className="border-r border-black px-2 py-1.5">QC Req.</th>
            <th className="px-2 py-1.5">Status</th>
          </tr>
        </thead>
        <tbody>
          {parts.map((part, idx) => (
            <tr key={part.id} className="border-b border-black">
              <td className="border-r border-black px-2 py-1.5 text-center">{idx + 1}</td>
              <td className="border-r border-black px-2 py-1.5 font-mono font-bold">{part.serialNumber}</td>
              <td className="border-r border-black px-2 py-1.5">{part.name}</td>
              <td className="border-r border-black px-2 py-1.5">{part.description || '—'}</td>
              <td className="border-r border-black px-2 py-1.5 text-center">{part.hsnCode || '—'}</td>
              <td className="border-r border-black px-2 py-1.5 text-center">{part.taxRatePercent != null ? part.taxRatePercent : '—'}</td>
              <td className="border-r border-black px-2 py-1.5">{part.partType || '—'}</td>
              <td className="border-r border-black px-2 py-1.5 text-center">{part.criticality || '—'}</td>
              <td className="border-r border-black px-2 py-1.5 text-center">{part.reorderLevel ?? '—'}</td>
              <td className="border-r border-black px-2 py-1.5 text-center">{part.qcRequired ? 'Y' : 'N'}</td>
              <td className="px-2 py-1.5 text-center">{part.itemStatus || '—'}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};
