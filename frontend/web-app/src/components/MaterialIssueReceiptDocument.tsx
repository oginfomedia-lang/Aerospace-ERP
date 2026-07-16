import React from 'react';
import { MaterialIssueRecord } from '../types/inventory';
import { OrganizationProfile } from '../types/procurement';

interface Props {
  record: MaterialIssueRecord;
  org: OrganizationProfile | null;
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

// Outbound counterpart to GrnReceiptDocument/MaterialReceiptDocument — a printable
// record of exactly which batches (and how much from each) fed a single
// issue-to-production call, since a request can be split across multiple batches
// (see PartServiceImpl.issueToProduction's FIFO loop).
export const MaterialIssueReceiptDocument: React.FC<Props> = ({ record, org }) => {
  return (
    <div className="text-black font-sans border-2 border-black text-xs">
      {/* Logo / Company Name / Status */}
      <div className="grid grid-cols-[1fr_2fr_1fr] border-b-2 border-black text-center">
        <div className="border-r border-black py-3 text-slate-300 font-bold uppercase text-[10px]">Logo</div>
        <div className="border-r border-black py-3 font-bold text-base">{org?.companyName || 'Company Name'}</div>
        <div className="py-3 font-bold uppercase">Issue</div>
      </div>

      <div className="bg-slate-300 text-center font-bold text-sm py-2 border-b-2 border-black uppercase tracking-wide">
        Material Issue Receipt
      </div>

      {/* Issue meta */}
      <table className="w-full border-collapse border-b-2 border-black">
        <tbody>
          <MetaRow l1="Issue Number" v1={record.issueNumber} l2="Issue Date" v2={formatDMY(record.issuedAt)} />
          <MetaRow l1="Work Order" v1={record.woNumber} l2="Issued To" v2={record.issuedTo} />
          <MetaRow l1="Issued By" v1={record.issuedBy} l2="Total Quantity" v2={record.totalQuantityIssued} />
        </tbody>
      </table>

      {/* Item detail */}
      <div className="bg-slate-300 text-center font-bold py-1 border-b border-black uppercase tracking-wide">Item Detail</div>
      <table className="w-full border-collapse border-b-2 border-black">
        <tbody>
          <MetaRow l1="Part" v1={<span className="font-mono font-bold">{record.partId}</span>} l2="Description" v2={record.partName} />
        </tbody>
      </table>

      {/* Batch split */}
      <div className="bg-slate-300 text-center font-bold py-1 border-b border-black uppercase tracking-wide">Batch Split</div>
      <table className="w-full border-collapse border-b-2 border-black">
        <thead>
          <tr className="border-b border-black bg-slate-50">
            <th className="border-r border-black px-2 py-1.5 text-left">Batch Number</th>
            <th className="border-r border-black px-2 py-1.5 text-right">Quantity Taken</th>
            <th className="border-r border-black px-2 py-1.5 text-left">Location</th>
            <th className="px-2 py-1.5 text-left">Expiry Date</th>
          </tr>
        </thead>
        <tbody>
          {record.lines.map(line => (
            <tr key={line.id} className="border-b border-black last:border-b-0">
              <td className="border-r border-black px-2 py-1.5 font-mono">{line.batchNumber}</td>
              <td className="border-r border-black px-2 py-1.5 text-right">{line.quantityTaken}</td>
              <td className="border-r border-black px-2 py-1.5">{line.location || '—'}</td>
              <td className="px-2 py-1.5">{formatDMY(line.expiryDate) || '—'}</td>
            </tr>
          ))}
          <tr className="font-bold bg-slate-50">
            <td className="border-r border-black px-2 py-1.5">Total</td>
            <td className="border-r border-black px-2 py-1.5 text-right">{record.totalQuantityIssued}</td>
            <td className="border-r border-black px-2 py-1.5" colSpan={2}></td>
          </tr>
        </tbody>
      </table>

      {/* Signatures */}
      <table className="w-full border-collapse">
        <tbody>
          <tr className="border-b border-black">
            <td className={cellLabel + ' align-top'}>Issued By:<br />(Signature)</td>
            <td className={cellValue}>&nbsp;</td>
            <td className={cellLabel + ' align-top'}>Received By:<br />(Signature)</td>
            <td className={cellValueLast}>&nbsp;</td>
          </tr>
          <tr>
            <td className={cellLabel}>Name/ Title:</td>
            <td className={cellValue}>{record.issuedBy || ''}</td>
            <td className={cellLabel}>Name/ Title:</td>
            <td className={cellValueLast}>&nbsp;</td>
          </tr>
        </tbody>
      </table>
    </div>
  );
};
