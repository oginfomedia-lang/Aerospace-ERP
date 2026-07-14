import React from 'react';
import { MaterialReceipt } from '../types/inventory';
import { OrganizationProfile, Supplier, PurchaseOrderResponse } from '../types/procurement';

interface Props {
  receipt: MaterialReceipt;
  org: OrganizationProfile | null;
  supplier: Supplier | undefined;
  po: PurchaseOrderResponse | undefined;
}

const formatDMY = (value?: string | null) => {
  if (!value) return '';
  const d = new Date(value);
  if (isNaN(d.getTime())) return '';
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
};

const cellLabel = 'border-r border-black px-2 py-1.5 font-bold bg-slate-100 w-1/4';
const cellValue = 'border-r border-black px-2 py-1.5 w-1/4';
const cellValueLast = 'px-2 py-1.5 w-1/4';

// A "Label | Value | Label | Value" meta row, matching the dense boxed field layout
// used throughout the reference Material Receipt Format template.
const MetaRow: React.FC<{ l1: string; v1: React.ReactNode; l2: string; v2: React.ReactNode }> = ({ l1, v1, l2, v2 }) => (
  <tr className="border-b border-black">
    <td className={cellLabel}>{l1}</td>
    <td className={cellValue}>{v1 || '—'}</td>
    <td className={cellLabel}>{l2}</td>
    <td className={cellValueLast}>{v2 || '—'}</td>
  </tr>
);

// Formal Material Receipt document matching the company's reference "Material Receipt
// Format" letterhead exactly — plain black-on-white, boxed fields (rendered inside
// ReceiptModal with `plain`, which skips its own generic branding header).
export const MaterialReceiptDocument: React.FC<Props> = ({ receipt, org, supplier, po }) => {
  return (
    <div className="text-black font-sans border-2 border-black text-xs">
      {/* Logo / Company Name / Status */}
      <div className="grid grid-cols-[1fr_2fr_1fr] border-b-2 border-black text-center">
        <div className="border-r border-black py-3 text-slate-300 font-bold uppercase text-[10px]">Logo</div>
        <div className="border-r border-black py-3 font-bold text-base">{org?.companyName || 'Company Name'}</div>
        <div className="py-3 font-bold uppercase">{receipt.status === 'GRN_FORMED' ? 'Processed' : 'Pending'}</div>
      </div>

      <div className="bg-slate-300 text-center font-bold text-sm py-2 border-b-2 border-black uppercase tracking-wide">
        Material Receipt Format
      </div>

      {/* Organization / Document meta */}
      <table className="w-full border-collapse border-b-2 border-black">
        <tbody>
          <MetaRow l1="Organization:" v1={org?.companyName} l2="Document No:" v2={receipt.mrNumber} />
          <MetaRow l1="Challan Number:" v1={receipt.challanNumber} l2="Transport:" v2={receipt.transportName} />
          <MetaRow l1="Status:" v1={receipt.status} l2="Received By:" v2={receipt.receivedBy} />
        </tbody>
      </table>

      {/* Supplier / PO meta */}
      <table className="w-full border-collapse border-b-2 border-black">
        <tbody>
          <MetaRow l1="Supplier Name" v1={receipt.supplierName} l2="Supplier Code" v2={supplier?.vendorCode} />
          <MetaRow l1="Receipt Number" v1={receipt.mrNumber} l2="PO Number" v2={receipt.poNumber} />
          <MetaRow l1="Receipt Date" v1={formatDMY(receipt.receivedAt)} l2="PO Date" v2={po ? formatDMY(po.orderDate) : undefined} />
        </tbody>
      </table>

      {/* Details */}
      <div className="bg-slate-300 text-center font-bold py-1 border-b border-black uppercase tracking-wide">Details</div>
      <table className="w-full border-collapse border-b-2 border-black">
        <thead>
          <tr className="border-b border-black bg-slate-100 font-bold">
            <th className="border-r border-black px-2 py-1.5 w-12">S No.</th>
            <th className="border-r border-black px-2 py-1.5 text-left">Name of the Material</th>
            <th className="border-r border-black px-2 py-1.5">Ordered Qty</th>
            <th className="border-r border-black px-2 py-1.5">Received Qty</th>
            <th className="px-2 py-1.5 text-left">Remarks</th>
          </tr>
        </thead>
        <tbody>
          {receipt.items.map((item, idx) => {
            const orderedLine = po?.items.find(i => i.skuCode === item.skuCode);
            return (
              <tr key={item.id} className="border-b border-black">
                <td className="border-r border-black px-2 py-1.5 text-center">{idx + 1}</td>
                <td className="border-r border-black px-2 py-1.5 font-mono font-bold">{item.skuCode}</td>
                <td className="border-r border-black px-2 py-1.5 text-center">
                  {orderedLine ? `${orderedLine.quantity} ${item.uom}` : '—'}
                </td>
                <td className="border-r border-black px-2 py-1.5 text-center">{item.quantity} {item.uom}</td>
                <td className="px-2 py-1.5">{item.grnNumber ? `GRN formed: ${item.grnNumber}` : 'GRN not yet formed'}</td>
              </tr>
            );
          })}
        </tbody>
      </table>

      {/* Signatures */}
      <table className="w-full border-collapse">
        <tbody>
          <tr className="border-b border-black">
            <td className={cellLabel + ' align-top'}>Received By:<br />(Signature)</td>
            <td className={cellValue}>&nbsp;</td>
            <td className={cellLabel + ' align-top'}>Quality Check Done by:<br />(Signature)</td>
            <td className={cellValueLast}>&nbsp;</td>
          </tr>
          <tr>
            <td className={cellLabel}>Name/ Title:</td>
            <td className={cellValue}>{receipt.receivedBy || ''}</td>
            <td className={cellLabel}>Name/ Title:</td>
            <td className={cellValueLast}>&nbsp;</td>
          </tr>
        </tbody>
      </table>
    </div>
  );
};
