import React from 'react';
import { PurchaseOrderResponse, Supplier, OrganizationProfile } from '../types/procurement';
import { Part } from '../types/inventory';
import { computeGst } from '../utils/gst';
import { amountToWords } from '../utils/numberToWords';

interface Props {
  po: PurchaseOrderResponse;
  supplier: Supplier | undefined;
  org: OrganizationProfile | null;
  parts: Part[];
}

const formatDMY = (value?: string | null) => {
  if (!value) return '';
  const d = new Date(value);
  if (isNaN(d.getTime())) return '';
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
};

// A single "Label : value" line, matching the dense plain-text field layout used
// throughout the reference PO (Purchase Order / Order Date / Range / ... block).
const Row: React.FC<{ label: string; value?: React.ReactNode; labelWidth?: string; extra?: React.ReactNode }> = ({ label, value, labelWidth = 'w-32', extra }) => (
  <div className="flex text-[11px] leading-snug">
    <span className={`${labelWidth} shrink-0`}>{label}</span>
    <span className="shrink-0 mr-1">:</span>
    <span>{value || ''}</span>
    {extra && <span className="ml-10">{extra}</span>}
  </div>
);

// Formal GST Purchase Order document, laid out to match the company's real
// reference letterhead exactly (plain black-on-white, no rounded cards/colors —
// this is rendered inside ReceiptModal with `plain`, which skips its own header).
export const POReceiptDocument: React.FC<Props> = ({ po, supplier, org, parts }) => {
  const gst = computeGst(org?.stateCode, supplier?.stateCode, po.items.map(i => ({
    taxRatePercent: i.taxRatePercent,
    quantity: i.quantity,
    unitPrice: i.unitPrice,
  })));
  const netTotal = gst.taxableTotal + gst.totalTax;
  const combinedRatePercent = gst.taxableTotal > 0 ? (gst.totalTax / gst.taxableTotal) * 100 : 0;
  const halfRatePercent = combinedRatePercent / 2;

  const shipToName = po.shipToName || org?.companyName;
  const shipToAddress = po.shipToAddress || org?.addressLine;
  const shipToState = po.shipToState || org?.state;
  const shipToStateCode = po.shipToStateCode || org?.stateCode;

  const paymentTerms = po.paymentTerms || org?.defaultPaymentTerms;
  const payment = po.payment || org?.defaultPayment;
  const deliveryTerms = po.deliveryTerms || org?.defaultDeliveryTerms;
  const exciseDuty = po.exciseDuty || org?.defaultExciseDuty;
  const termsText = po.termsAndConditions || org?.termsAndConditions;
  const termsList = (termsText || '').split('\n').map(t => t.trim()).filter(Boolean);

  return (
    <div className="text-black font-sans">
      {/* Letterhead */}
      <div className="text-center mb-2">
        <h1 className="text-xl font-bold">{org?.companyName}</h1>
        <p className="text-xs">
          {[org?.addressLine, org?.city].filter(Boolean).join(', ')}{org?.pincode ? `, Pincode : ${org.pincode}` : ''}
        </p>
        <p className="text-xs">{[org?.state?.toUpperCase(), org?.country?.toUpperCase()].filter(Boolean).join(' ')}</p>
        <p className="text-xs">
          Phone : {org?.phone || '—'} &nbsp;&nbsp; Telefax : {org?.telefax || '—'} &nbsp;&nbsp; Email : {org?.email || '—'}
        </p>
      </div>
      <hr className="border-t border-black" />
      <h2 className="text-center text-lg font-bold my-3">Purchase Order</h2>

      {/* Shipment Address / PO Meta */}
      <div className="grid grid-cols-2 gap-4 border-b border-black pb-2 mb-2">
        <div className="text-[11px] leading-snug">
          <p className="font-bold uppercase mb-1">Shipment Address</p>
          <p>{shipToName}</p>
          <p>{shipToAddress}</p>
          <div className="flex gap-6">
            <span>State : {shipToState}</span>
            <span>State Code : {shipToStateCode}</span>
          </div>
          <p>Phone : {org?.phone || '—'}</p>
        </div>
        <div className="space-y-0.5">
          <Row label="Purchase Order" value={po.poNumber} />
          <Row label="Order Date" value={formatDMY(po.orderDate)} />
          <Row label="Range" value={org?.range} />
          <Row label="Division" value={org?.division} />
          <Row label="GSTIN No." value={org?.gstin} />
        </div>
      </div>

      {(po.sourceWoNumber || po.remarks) && (
        <div className="mb-2 text-[11px]">
          {po.sourceWoNumber && (
            <p><span className="font-bold">Raised For :</span> Work Order shortage — {po.sourceWoNumber}</p>
          )}
          {po.remarks && (
            <p><span className="font-bold">Remarks :</span> {po.remarks}</p>
          )}
        </div>
      )}

      {/* Vendor Address */}
      <div className="grid grid-cols-2 gap-4 border-b border-black pb-2 mb-2">
        <div className="text-[11px] leading-snug">
          <p className="font-bold uppercase mb-1">Vendor Address</p>
          <p>Supplier {supplier?.name || po.supplierName}</p>
          <p>{[supplier?.city, supplier?.state, supplier?.country].filter(Boolean).join(',')}</p>
          <Row label="Phone No." value={supplier?.phone} labelWidth="w-20" />
          <Row label="Fax" value="" labelWidth="w-20" />
          <Row label="Email" value={supplier?.email} labelWidth="w-20" />
          <Row label="Cont. Person" value={supplier?.contactPerson} labelWidth="w-20" />
        </div>
        <div className="space-y-0.5">
          <Row label="State Name" value={supplier?.state} extra={supplier?.stateCode ? `State Code: ${supplier.stateCode}` : undefined} />
          <Row label="PAN No." value={supplier?.pan} />
          <Row label="GSTIN No." value={supplier?.gstin} />
          <Row label="GSTIN Reg. Type" value={supplier?.gstinRegType} />
        </div>
      </div>

      {/* Line items */}
      <table className="w-full text-[11px] mb-2 border-collapse">
        <thead>
          <tr className="border-b-2 border-black font-bold">
            <th className="py-1 text-left align-bottom">Sl.<br />No.</th>
            <th className="py-1 text-left align-bottom">Item Description</th>
            <th className="py-1 text-left align-bottom">HSN<br />Code</th>
            <th className="py-1 text-right align-bottom">Quantity</th>
            <th className="py-1 text-left align-bottom">UOM</th>
            <th className="py-1 text-left align-bottom">Delivery<br />Date</th>
            <th className="py-1 text-right align-bottom">Rate/<br />Unit</th>
            <th className="py-1 text-right align-bottom">Charges</th>
            <th className="py-1 text-right align-bottom">Value</th>
          </tr>
        </thead>
        <tbody>
          {po.items.map((item, idx) => {
            const part = parts.find(p => p.serialNumber === item.skuCode);
            const accepted = item.acceptedQuantity ?? 0;
            const rejected = item.rejectedQuantity ?? 0;
            return (
              <tr key={idx} className="align-top border-b border-slate-200">
                <td className="py-1.5">{idx + 1}</td>
                <td className="py-1.5">
                  <div className="font-bold">{item.skuCode}</div>
                  {part?.name && <div>{part.name}</div>}
                  {accepted === 0 && rejected === 0 ? (
                    <div className="text-slate-400 text-[10px]">Not yet inspected</div>
                  ) : (
                    <div className="text-[10px]">
                      {accepted > 0 && <span>Accepted: {accepted} </span>}
                      {rejected > 0 && <span>Rejected: {rejected}{item.ncrNumber ? ` (${item.ncrNumber})` : ''}</span>}
                    </div>
                  )}
                </td>
                <td className="py-1.5">{item.hsnCode || ''}</td>
                <td className="py-1.5 text-right">{item.quantity.toFixed(3)}</td>
                <td className="py-1.5">{item.uom}</td>
                <td className="py-1.5">{formatDMY(po.deliveryDate)}</td>
                <td className="py-1.5 text-right">{item.unitPrice.toFixed(2)}</td>
                <td className="py-1.5 text-right"></td>
                <td className="py-1.5 text-right">{(item.quantity * item.unitPrice).toFixed(2)}</td>
              </tr>
            );
          })}
        </tbody>
      </table>

      <div className="flex justify-end mb-2">
        <table className="text-[11px] w-72">
          <tbody>
            <tr className="border-t border-black">
              <td className="py-1 font-bold" colSpan={2}>Total</td>
              <td className="py-1 text-right font-bold">{gst.taxableTotal.toFixed(2)}</td>
            </tr>
            {gst.sameState ? (
              <>
                <tr>
                  <td className="py-1">CGST @ {halfRatePercent.toFixed(0)}%</td>
                  <td className="py-1 text-right">{halfRatePercent.toFixed(3)} (%)</td>
                  <td className="py-1 text-right">{gst.cgst.toFixed(2)}</td>
                </tr>
                <tr>
                  <td className="py-1">SGST @ {halfRatePercent.toFixed(0)}%</td>
                  <td className="py-1 text-right">{halfRatePercent.toFixed(3)} (%)</td>
                  <td className="py-1 text-right">{gst.sgst.toFixed(2)}</td>
                </tr>
              </>
            ) : (
              <tr>
                <td className="py-1">IGST @ {combinedRatePercent.toFixed(0)}%</td>
                <td className="py-1 text-right">{combinedRatePercent.toFixed(3)} (%)</td>
                <td className="py-1 text-right">{gst.igst.toFixed(2)}</td>
              </tr>
            )}
            <tr className="border-t border-black font-bold">
              <td className="py-1" colSpan={2}>Net PO Amt</td>
              <td className="py-1 text-right">{netTotal.toFixed(2)}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <p className="border-t border-black pt-1 mb-2 text-[11px]">
        <span className="font-bold">Amounts In Words :</span> {amountToWords(netTotal)}
      </p>

      <div className="space-y-0.5 mb-2">
        {paymentTerms && <Row label="Payment Terms" value={<span className="whitespace-pre-line">{paymentTerms}</span>} />}
        {deliveryTerms && <Row label="Delivery" value={deliveryTerms} />}
        {payment && <Row label="Payment" value={payment} />}
        {exciseDuty && <Row label="EXCISE DUTY" value={exciseDuty} />}
      </div>

      {termsList.length > 0 && (
        <div className="text-[11px]">
          <p className="font-bold uppercase mb-1">Special Terms and Conditions</p>
          <ol className="list-decimal list-inside space-y-0.5">
            {termsList.map((t, i) => <li key={i}>{t}</li>)}
          </ol>
        </div>
      )}
    </div>
  );
};
