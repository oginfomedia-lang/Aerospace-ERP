import React from 'react';
import { OrganizationProfile, VendorInvoiceResponse } from '../types/procurement';

interface Props {
  invoice: VendorInvoiceResponse;
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

// Formal Vendor Invoice / Three-Way Match statement — same plain, boxed, black-on-white
// pattern as GrnReceiptDocument/MaterialReceiptDocument, since this is an internal AP audit
// record (not a fiscal document sent to the supplier).
export const VendorInvoiceDocument: React.FC<Props> = ({ invoice, org }) => {
  return (
    <div className="text-black font-sans border-2 border-black text-xs">
      {/* Logo / Company Name / Doc type */}
      <div className="grid grid-cols-[1fr_2fr_1fr] border-b-2 border-black text-center">
        <div className="border-r border-black py-3 text-slate-300 font-bold uppercase text-[10px]">Logo</div>
        <div className="border-r border-black py-3 font-bold text-base">{org?.companyName || 'Company Name'}</div>
        <div className="py-3 font-bold uppercase">Invoice</div>
      </div>

      <div className="bg-slate-300 text-center font-bold text-sm py-2 border-b-2 border-black uppercase tracking-wide">
        Vendor Invoice — Three-Way Match Statement
      </div>

      {/* Invoice / PO meta */}
      <table className="w-full border-collapse border-b-2 border-black">
        <tbody>
          <MetaRow l1="Invoice Number" v1={<span className="font-mono font-bold">{invoice.invoiceNumber}</span>} l2="Invoice Date" v2={formatDMY(invoice.invoiceDate)} />
          <MetaRow l1="Supplier Invoice Ref." v1={invoice.supplierInvoiceRef} l2="Currency" v2={invoice.currency} />
          <MetaRow l1="PO Number" v1={invoice.poNumber} l2="Supplier" v2={invoice.supplierName} />
          <tr>
            <td className={cellLabel}>Status</td>
            <td className="px-2 py-1.5" colSpan={3}>
              <span className="font-black uppercase tracking-wide">{invoice.status}</span>
            </td>
          </tr>
        </tbody>
      </table>

      {/* Line items */}
      <div className="bg-slate-300 text-center font-bold py-1 border-b border-black uppercase tracking-wide">Line Item Match Detail</div>
      <table className="w-full border-collapse text-[11px]">
        <thead>
          <tr className="border-b-2 border-black font-bold bg-slate-50">
            <th className="border-r border-black py-1.5 px-2 text-left">SKU</th>
            <th className="border-r border-black py-1.5 px-2 text-right">Invoiced<br />Qty</th>
            <th className="border-r border-black py-1.5 px-2 text-right">Accepted<br />Qty (PO)</th>
            <th className="border-r border-black py-1.5 px-2 text-right">Qty<br />Variance</th>
            <th className="border-r border-black py-1.5 px-2 text-right">Invoiced<br />Price</th>
            <th className="border-r border-black py-1.5 px-2 text-right">PO<br />Price</th>
            <th className="border-r border-black py-1.5 px-2 text-right">Price<br />Variance %</th>
            <th className="py-1.5 px-2 text-left">Match</th>
          </tr>
        </thead>
        <tbody>
          {invoice.items.map((item, idx) => (
            <tr key={idx} className="border-b border-black">
              <td className="border-r border-black py-1.5 px-2 font-mono font-bold">{item.skuCode}</td>
              <td className="border-r border-black py-1.5 px-2 text-right">{item.invoicedQuantity}</td>
              <td className="border-r border-black py-1.5 px-2 text-right">{item.matchedPoQuantity ?? '—'}</td>
              <td className="border-r border-black py-1.5 px-2 text-right">{item.quantityVariance ?? '—'}</td>
              <td className="border-r border-black py-1.5 px-2 text-right">{item.invoicedUnitPrice?.toFixed(2)}</td>
              <td className="border-r border-black py-1.5 px-2 text-right">{item.matchedPoUnitPrice?.toFixed(2) ?? '—'}</td>
              <td className="border-r border-black py-1.5 px-2 text-right">
                {item.priceVariancePercent != null ? `${item.priceVariancePercent.toFixed(2)}%` : '—'}
              </td>
              <td className="py-1.5 px-2 font-bold">{item.lineStatus === 'UNMATCHED_SKU' ? 'NOT ON PO' : item.lineStatus}</td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr className="border-t-2 border-black font-bold">
            <td className="border-r border-black py-1.5 px-2" colSpan={7}>Total Amount</td>
            <td className="py-1.5 px-2 text-right">{invoice.currency} {invoice.totalAmount?.toFixed(2)}</td>
          </tr>
        </tfoot>
      </table>

      {/* Remarks */}
      <table className="w-full border-collapse border-t-2 border-black">
        <tbody>
          <tr className="border-b border-black">
            <td className={cellLabel}>Remarks</td>
            <td className="px-2 py-1.5" colSpan={3}>{invoice.remarks || '—'}</td>
          </tr>
        </tbody>
      </table>

      {/* Signatures / decision trail */}
      <table className="w-full border-collapse">
        <tbody>
          <tr className="border-b border-black">
            <td className={cellLabel + ' align-top'}>Approved/Rejected By:<br />(Signature)</td>
            <td className={cellValue}>&nbsp;</td>
            <td className={cellLabel + ' align-top'}>Paid On:</td>
            <td className={cellValueLast}>{formatDMY(invoice.paidAt) || '—'}</td>
          </tr>
          <tr>
            <td className={cellLabel}>Name/ Title:</td>
            <td className={cellValue}>{invoice.approvedBy || ''}</td>
            <td className={cellLabel}>Decision Date:</td>
            <td className={cellValueLast}>{formatDMY(invoice.approvedAt) || '—'}</td>
          </tr>
        </tbody>
      </table>
    </div>
  );
};
