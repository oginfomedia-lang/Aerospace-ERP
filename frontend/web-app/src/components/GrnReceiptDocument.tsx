import React from 'react';
import { MaterialReceipt, MaterialReceiptItem, Part } from '../types/inventory';
import { OrganizationProfile, Supplier, PurchaseOrderResponse } from '../types/procurement';

interface Props {
  item: MaterialReceiptItem;
  receipt: MaterialReceipt;
  org: OrganizationProfile | null;
  supplier: Supplier | undefined;
  po: PurchaseOrderResponse | undefined;
  parts: Part[];
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

const MetaRow: React.FC<{ l1: string; v1: React.ReactNode; l2: string; v2: React.ReactNode }> = ({ l1, v1, l2, v2 }) => (
  <tr className="border-b border-black">
    <td className={cellLabel}>{l1}</td>
    <td className={cellValue}>{v1 || '—'}</td>
    <td className={cellLabel}>{l2}</td>
    <td className={cellValueLast}>{v2 || '—'}</td>
  </tr>
);

// Formal Goods Received Note — the last of the PO/MR/GRN document trio to be rebuilt as
// a plain, boxed, black-on-white document (see POReceiptDocument.tsx / MaterialReceiptDocument.tsx
// for the same pattern). One GRN document covers a single MaterialReceiptItem, since each
// line item gets its own system-generated GRN number (GRN-FR-001).
export const GrnReceiptDocument: React.FC<Props> = ({ item, receipt, org, supplier, po, parts }) => {
  const part = parts.find(p => p.serialNumber === item.skuCode);
  const orderedLine = po?.items.find(i => i.skuCode === item.skuCode);
  const accepted = orderedLine?.acceptedQuantity ?? 0;
  const rejected = orderedLine?.rejectedQuantity ?? 0;
  const qcOutcome = accepted === 0 && rejected === 0
    ? 'Not yet inspected'
    : [accepted > 0 ? `Accepted: ${accepted}` : null, rejected > 0 ? `Rejected: ${rejected}${orderedLine?.ncrNumber ? ` (${orderedLine.ncrNumber})` : ''}` : null]
        .filter(Boolean).join(' · ');

  return (
    <div className="text-black font-sans border-2 border-black text-xs">
      {/* Logo / Company Name / Status */}
      <div className="grid grid-cols-[1fr_2fr_1fr] border-b-2 border-black text-center">
        <div className="border-r border-black py-3 text-slate-300 font-bold uppercase text-[10px]">Logo</div>
        <div className="border-r border-black py-3 font-bold text-base">{org?.companyName || 'Company Name'}</div>
        <div className="py-3 font-bold uppercase">GRN</div>
      </div>

      <div className="bg-slate-300 text-center font-bold text-sm py-2 border-b-2 border-black uppercase tracking-wide">
        Goods Received Note
      </div>

      {/* GRN / MR / PO meta */}
      <table className="w-full border-collapse border-b-2 border-black">
        <tbody>
          <MetaRow l1="GRN Number" v1={item.grnNumber} l2="GRN Date" v2={formatDMY(receipt.grnFormedAt)} />
          <MetaRow l1="Material Receipt" v1={receipt.mrNumber} l2="PO Number" v2={receipt.poNumber} />
          <MetaRow l1="PO Date" v1={po ? formatDMY(po.orderDate) : undefined} l2="Challan Number" v2={receipt.challanNumber} />
        </tbody>
      </table>

      {/* Supplier meta */}
      <table className="w-full border-collapse border-b-2 border-black">
        <tbody>
          <MetaRow l1="Supplier Name" v1={receipt.supplierName} l2="Supplier Code" v2={supplier?.vendorCode} />
          <MetaRow l1="Transport" v1={receipt.transportName} l2="Received By" v2={receipt.receivedBy} />
        </tbody>
      </table>

      {/* Item detail */}
      <div className="bg-slate-300 text-center font-bold py-1 border-b border-black uppercase tracking-wide">Item Detail</div>
      <table className="w-full border-collapse border-b-2 border-black">
        <tbody>
          <MetaRow l1="Item" v1={<span className="font-mono font-bold">{item.skuCode}</span>} l2="Description" v2={part?.name} />
          <MetaRow l1="Ordered Qty" v1={orderedLine ? `${orderedLine.quantity} ${item.uom}` : undefined} l2="Received Qty" v2={`${item.quantity} ${item.uom}`} />
          <MetaRow l1="Batch Number" v1={item.batchNumber} l2="Storage Location" v2={item.location} />
          <MetaRow l1="Expiry Date" v1={formatDMY(item.expiryDate)} l2="Inspection Qty" v2={item.inspectionQty != null ? `${item.inspectionQty} ${item.uom}` : undefined} />
          <tr>
            <td className={cellLabel}>QC Outcome</td>
            <td className="px-2 py-1.5" colSpan={3}>{qcOutcome}</td>
          </tr>
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
