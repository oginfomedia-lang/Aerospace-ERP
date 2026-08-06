import React, { useEffect, useState } from 'react';
import { Receipt, Plus, RefreshCw, ChevronDown, ChevronRight, CheckCircle2, XCircle, Banknote, AlertTriangle, Loader2, FileText } from 'lucide-react';
import toast from 'react-hot-toast';
import procurementApi from '../api/procurementAxios';
import { OrganizationProfile, PurchaseOrderResponse, VendorInvoiceResponse, VendorInvoiceRequest, VendorInvoiceLineItemDTO } from '../types/procurement';
import { ReceiptModal } from '../components/ReceiptModal';
import { VendorInvoiceDocument } from '../components/VendorInvoiceDocument';
import { useAuth } from '../context/AuthContext';
import { CAN_MANAGE_PROCUREMENT, CAN_APPROVE_PROCUREMENT, hasAnyRole } from '../utils/permissions';

const statusStyle: Record<string, string> = {
  PENDING_MATCH: 'bg-slate-100 text-slate-500',
  MATCHED: 'bg-emerald-100 text-emerald-700',
  VARIANCE_HOLD: 'bg-amber-100 text-amber-700 animate-pulse',
  APPROVED: 'bg-indigo-100 text-indigo-700',
  REJECTED: 'bg-red-100 text-red-700',
  PAID: 'bg-slate-800 text-white',
};

const VendorInvoices = () => {
  const { user } = useAuth();
  const canManage = hasAnyRole(user?.roles, CAN_MANAGE_PROCUREMENT);
  const canApprove = hasAnyRole(user?.roles, CAN_APPROVE_PROCUREMENT);
  const [invoices, setInvoices] = useState<VendorInvoiceResponse[]>([]);
  const [purchaseOrders, setPurchaseOrders] = useState<PurchaseOrderResponse[]>([]);
  const [orgProfile, setOrgProfile] = useState<OrganizationProfile | null>(null);
  const [loading, setLoading] = useState(false);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [viewInvoice, setViewInvoice] = useState<VendorInvoiceResponse | null>(null);

  const [showRecordModal, setShowRecordModal] = useState(false);
  const [purchaseOrderId, setPurchaseOrderId] = useState<number | ''>('');
  const [supplierInvoiceRef, setSupplierInvoiceRef] = useState('');
  const [invoiceDate, setInvoiceDate] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [items, setItems] = useState<VendorInvoiceLineItemDTO[]>([]);
  const [submitting, setSubmitting] = useState(false);

  const [actionTarget, setActionTarget] = useState<{ invoice: VendorInvoiceResponse; action: 'approve' | 'reject' } | null>(null);
  const [actionRemarks, setActionRemarks] = useState('');
  const [acting, setActing] = useState(false);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [invRes, poRes, orgRes] = await Promise.allSettled([
        procurementApi.get<VendorInvoiceResponse[]>('/vendor-invoices'),
        procurementApi.get<PurchaseOrderResponse[]>('/purchase-orders'),
        procurementApi.get<OrganizationProfile>('/organization-profile'),
      ]);
      if (invRes.status === 'fulfilled') setInvoices(invRes.value.data);
      if (poRes.status === 'fulfilled') setPurchaseOrders(poRes.value.data);
      if (orgRes.status === 'fulfilled') setOrgProfile(orgRes.value.data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const resetForm = () => {
    setPurchaseOrderId('');
    setSupplierInvoiceRef('');
    setInvoiceDate('');
    setCurrency('USD');
    setItems([]);
  };

  const handleOpenRecord = () => {
    resetForm();
    setShowRecordModal(true);
  };

  // Pre-fill line items from the PO's own lines — quantity defaults to what QA actually
  // accepted (the "received" side of the three-way match), price defaults to the PO's
  // negotiated rate. Both stay editable to reflect what the vendor actually billed.
  const handleSelectPO = (id: number) => {
    setPurchaseOrderId(id);
    const po = purchaseOrders.find(o => o.id === id);
    if (!po) return;
    setCurrency(po.currency || 'USD');
    setItems(po.items.map(i => ({
      skuCode: i.skuCode,
      invoicedQuantity: i.acceptedQuantity ?? 0,
      invoicedUnitPrice: i.unitPrice,
    })));
  };

  const handleItemChange = (index: number, field: 'invoicedQuantity' | 'invoicedUnitPrice', value: string) => {
    const newItems = [...items];
    newItems[index] = { ...newItems[index], [field]: Number(value) };
    setItems(newItems);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!purchaseOrderId) {
      toast.error('Please select a Purchase Order.');
      return;
    }
    const payload: VendorInvoiceRequest = {
      purchaseOrderId: Number(purchaseOrderId),
      supplierInvoiceRef: supplierInvoiceRef || undefined,
      invoiceDate: invoiceDate ? new Date(invoiceDate).toISOString() : undefined,
      currency,
      items,
    };
    setSubmitting(true);
    try {
      const res = await procurementApi.post<VendorInvoiceResponse>('/vendor-invoices', payload);
      setInvoices([res.data, ...invoices]);
      setExpandedId(res.data.id);
      setShowRecordModal(false);
      resetForm();
      toast[res.data.status === 'VARIANCE_HOLD' ? 'error' : 'success'](
        res.data.status === 'VARIANCE_HOLD'
          ? `⚠️ ${res.data.invoiceNumber} matched with variance — needs an override to approve.`
          : `✅ ${res.data.invoiceNumber} matched cleanly against ${res.data.poNumber}.`
      );
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to record invoice.');
    } finally {
      setSubmitting(false);
    }
  };

  const openAction = (invoice: VendorInvoiceResponse, action: 'approve' | 'reject') => {
    setActionTarget({ invoice, action });
    setActionRemarks('');
  };

  const submitAction = async () => {
    if (!actionTarget) return;
    const { invoice, action } = actionTarget;
    const needsRemarks = action === 'reject' || (action === 'approve' && invoice.status === 'VARIANCE_HOLD');
    if (needsRemarks && !actionRemarks.trim()) {
      toast.error(action === 'reject'
        ? 'Rejection remarks are mandatory.'
        : 'An override reason is required to approve an invoice on variance hold.');
      return;
    }
    setActing(true);
    try {
      const res = await procurementApi.post<VendorInvoiceResponse>(
        `/vendor-invoices/${invoice.id}/${action}`, { remarks: actionRemarks }
      );
      setInvoices(invoices.map(i => i.id === invoice.id ? res.data : i));
      toast.success(action === 'approve' ? `${res.data.invoiceNumber} approved.` : `${res.data.invoiceNumber} rejected.`);
      setActionTarget(null);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Action failed.');
    } finally {
      setActing(false);
    }
  };

  const handleMarkPaid = async (invoice: VendorInvoiceResponse) => {
    if (!window.confirm(`Mark ${invoice.invoiceNumber} as paid? This simulates AP settlement — no real payment is made.`)) return;
    try {
      const res = await procurementApi.post<VendorInvoiceResponse>(`/vendor-invoices/${invoice.id}/mark-paid`);
      setInvoices(invoices.map(i => i.id === invoice.id ? res.data : i));
      toast.success(`${res.data.invoiceNumber} marked paid.`);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to mark paid.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Vendor Invoices</h1>
          <p className="text-slate-500 text-sm font-medium mt-1">
            Three-way match — invoiced qty/price vs. the PO's rate and QA-accepted quantity.
          </p>
        </div>
        <div className="flex gap-3">
          <button onClick={fetchData} className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-xl transition-all">
            <RefreshCw size={20} className={loading ? 'animate-spin' : ''} />
          </button>
          {canManage && (
            <button onClick={handleOpenRecord}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold rounded-xl transition-all shadow-lg shadow-blue-600/20">
              <Plus size={18} /> Record Vendor Invoice
            </button>
          )}
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase text-xs tracking-wider">
            <tr>
              <th className="px-6 py-4">Invoice Number</th>
              <th className="px-6 py-4">PO Number</th>
              <th className="px-6 py-4">Supplier</th>
              <th className="px-6 py-4">Invoice Date</th>
              <th className="px-6 py-4 text-right">Total Amount</th>
              <th className="px-6 py-4">Status</th>
              <th className="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {invoices.map(invoice => {
              const isExpanded = expandedId === invoice.id;
              return (
                <React.Fragment key={invoice.id}>
                  <tr className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <button onClick={() => setExpandedId(isExpanded ? null : invoice.id)}
                          className="p-0.5 text-slate-400 hover:text-slate-700">
                          {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                        </button>
                        <span className="font-bold text-slate-900 font-mono">{invoice.invoiceNumber}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-slate-600 font-mono">{invoice.poNumber}</td>
                    <td className="px-6 py-4 font-medium text-slate-700">{invoice.supplierName}</td>
                    <td className="px-6 py-4 text-slate-600">{new Date(invoice.invoiceDate).toLocaleDateString()}</td>
                    <td className="px-6 py-4 text-right font-bold text-slate-900">
                      {invoice.currency} {invoice.totalAmount?.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                    </td>
                    <td className="px-6 py-4">
                      <span className={`px-2.5 py-1 text-[11px] font-black tracking-wider uppercase rounded-full ${statusStyle[invoice.status] || statusStyle.PENDING_MATCH}`}>
                        {invoice.status}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <div className="flex gap-2 justify-end">
                        <button onClick={() => setViewInvoice(invoice)}
                          className="p-1.5 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-all" title="View / Print">
                          <FileText size={16} />
                        </button>
                        {(invoice.status === 'MATCHED' || invoice.status === 'VARIANCE_HOLD') && canApprove && (
                          <>
                            <button onClick={() => openAction(invoice, 'reject')}
                              className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-xl transition-all" title="Reject">
                              <XCircle size={16} />
                            </button>
                            <button onClick={() => openAction(invoice, 'approve')}
                              className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl transition-all shadow-sm">
                              Approve
                            </button>
                          </>
                        )}
                        {invoice.status === 'APPROVED' && canApprove && (
                          <button onClick={() => handleMarkPaid(invoice)}
                            className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-800 hover:bg-slate-900 text-white text-xs font-bold rounded-xl transition-all shadow-sm">
                            <Banknote size={14} /> Mark Paid
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                  {isExpanded && (
                    <tr className="bg-slate-50/70">
                      <td colSpan={7} className="px-6 py-4">
                        {invoice.status === 'VARIANCE_HOLD' && (
                          <div className="mb-3 flex items-center gap-2 px-3 py-2 bg-amber-50 border border-amber-200 rounded-xl text-amber-800 text-xs font-bold">
                            <AlertTriangle size={14} className="shrink-0" />
                            One or more lines varied beyond tolerance — approving requires an override reason.
                          </div>
                        )}
                        <table className="w-full text-xs">
                          <thead className="text-slate-400 uppercase tracking-wider">
                            <tr>
                              <th className="text-left pb-2">SKU</th>
                              <th className="text-right pb-2">Invoiced Qty</th>
                              <th className="text-right pb-2">Accepted Qty (PO)</th>
                              <th className="text-right pb-2">Qty Variance</th>
                              <th className="text-right pb-2">Invoiced Price</th>
                              <th className="text-right pb-2">PO Price</th>
                              <th className="text-right pb-2">Price Variance %</th>
                              <th className="text-left pb-2 pl-4">Match</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-slate-100">
                            {invoice.items.map((item, idx) => (
                              <tr key={idx}>
                                <td className="py-2 font-mono font-bold text-slate-700">{item.skuCode}</td>
                                <td className="py-2 text-right text-slate-600">{item.invoicedQuantity}</td>
                                <td className="py-2 text-right text-slate-600">{item.matchedPoQuantity ?? '—'}</td>
                                <td className={`py-2 text-right font-bold ${item.quantityVariance ? 'text-red-600' : 'text-slate-400'}`}>
                                  {item.quantityVariance ?? '—'}
                                </td>
                                <td className="py-2 text-right text-slate-600">{item.invoicedUnitPrice?.toFixed(2)}</td>
                                <td className="py-2 text-right text-slate-600">{item.matchedPoUnitPrice?.toFixed(2) ?? '—'}</td>
                                <td className={`py-2 text-right font-bold ${item.priceVariancePercent ? 'text-red-600' : 'text-slate-400'}`}>
                                  {item.priceVariancePercent != null ? `${item.priceVariancePercent.toFixed(2)}%` : '—'}
                                </td>
                                <td className="py-2 pl-4">
                                  <span className={`px-1.5 py-0.5 font-black rounded ${
                                    item.lineStatus === 'MATCHED' ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'
                                  }`}>
                                    {item.lineStatus === 'UNMATCHED_SKU' ? 'NOT ON PO' : item.lineStatus}
                                  </span>
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                        {invoice.remarks && (
                          <p className="mt-3 text-xs text-slate-500 italic">"{invoice.remarks}"{invoice.approvedBy ? ` — ${invoice.approvedBy}` : ''}</p>
                        )}
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              );
            })}
            {invoices.length === 0 && !loading && (
              <tr>
                <td colSpan={7} className="px-6 py-12 text-center text-slate-500">
                  <Receipt className="w-12 h-12 mx-auto text-slate-300 mb-3" />
                  <p className="font-medium">No vendor invoices recorded yet</p>
                  <p className="text-sm mt-1">Record one against a Purchase Order to run the three-way match.</p>
                </td>
              </tr>
            )}
          </tbody>
        </table>
        </div>
      </div>

      {/* Record Invoice Modal */}
      {showRecordModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl w-full max-w-2xl overflow-hidden shadow-2xl flex flex-col max-h-[90vh]">
            <div className="p-6 border-b border-slate-100 shrink-0">
              <h2 className="text-xl font-bold text-slate-900">Record Vendor Invoice</h2>
              <p className="text-xs text-slate-400 font-medium mt-1">Matched instantly against the PO's rate and QA-accepted quantity.</p>
            </div>

            <div className="p-6 overflow-y-auto flex-1">
              <form id="invoice-form" onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">Purchase Order</label>
                  <select required value={purchaseOrderId} onChange={e => handleSelectPO(Number(e.target.value))}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all">
                    <option value="" disabled>Select a Purchase Order...</option>
                    {purchaseOrders.map(po => (
                      <option key={po.id} value={po.id}>{po.poNumber} — {po.supplierName} ({po.status})</option>
                    ))}
                  </select>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-1.5">Supplier Invoice Ref.</label>
                    <input type="text" value={supplierInvoiceRef} onChange={e => setSupplierInvoiceRef(e.target.value)}
                      placeholder="Vendor's own invoice number"
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all" />
                  </div>
                  <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-1.5">Invoice Date</label>
                    <input required type="date" value={invoiceDate} onChange={e => setInvoiceDate(e.target.value)}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all" />
                  </div>
                </div>

                {items.length > 0 && (
                  <div className="border-t border-slate-100 pt-4">
                    <h3 className="text-sm font-bold text-slate-900 mb-3">Invoiced Line Items</h3>
                    <p className="text-[10px] text-slate-400 -mt-2 mb-3">
                      Pre-filled from the PO — edit to reflect what the vendor actually billed.
                    </p>
                    <div className="space-y-2">
                      {items.map((item, idx) => (
                        <div key={idx} className="flex gap-3 items-center p-3 bg-slate-50 rounded-xl border border-slate-200">
                          <div className="flex-1 font-mono font-bold text-sm text-slate-700">{item.skuCode}</div>
                          <div className="w-28">
                            <label className="block text-[9px] font-bold text-slate-400 uppercase mb-0.5">Invoiced Qty</label>
                            <input required type="number" min="0" value={item.invoicedQuantity}
                              onChange={e => handleItemChange(idx, 'invoicedQuantity', e.target.value)}
                              className="w-full px-3 py-1.5 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:border-blue-500" />
                          </div>
                          <div className="w-32">
                            <label className="block text-[9px] font-bold text-slate-400 uppercase mb-0.5">Invoiced Price</label>
                            <input required type="number" min="0" step="0.01" value={item.invoicedUnitPrice}
                              onChange={e => handleItemChange(idx, 'invoicedUnitPrice', e.target.value)}
                              className="w-full px-3 py-1.5 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:border-blue-500" />
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </form>
            </div>

            <div className="p-6 border-t border-slate-100 shrink-0 bg-slate-50 flex justify-end gap-3">
              <button type="button" onClick={() => setShowRecordModal(false)}
                className="px-4 py-2 bg-white border border-slate-200 hover:bg-slate-100 text-slate-700 font-bold rounded-xl transition-all">
                Cancel
              </button>
              <button type="submit" form="invoice-form" disabled={submitting}
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl transition-all shadow-lg shadow-blue-600/20 disabled:opacity-50 flex items-center gap-2">
                {submitting && <Loader2 size={16} className="animate-spin" />} Record &amp; Match
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Approve / Reject Modal */}
      {actionTarget && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl w-full max-w-md overflow-hidden shadow-2xl">
            <div className="p-6 border-b border-slate-100">
              <h2 className="text-lg font-black text-slate-900">
                {actionTarget.action === 'approve' ? 'Approve Invoice' : 'Reject Invoice'}
              </h2>
              <p className="text-xs font-bold text-slate-400 mt-1">{actionTarget.invoice.invoiceNumber}</p>
            </div>
            <div className="p-6 space-y-3">
              <label className="block text-xs font-bold text-slate-500 uppercase tracking-widest">
                {actionTarget.action === 'reject'
                  ? 'Rejection remarks (required)'
                  : actionTarget.invoice.status === 'VARIANCE_HOLD'
                  ? 'Override reason (required — invoice is on variance hold)'
                  : 'Remarks (optional)'}
              </label>
              <textarea value={actionRemarks} onChange={e => setActionRemarks(e.target.value)} rows={3}
                placeholder="Reason / notes for this decision..."
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm focus:ring-2 focus:ring-blue-500 outline-none" />
            </div>
            <div className="p-6 pt-0 flex justify-end gap-3">
              <button onClick={() => setActionTarget(null)} disabled={acting}
                className="px-4 py-2 text-slate-500 font-bold text-sm hover:bg-slate-50 rounded-xl transition-all">
                Cancel
              </button>
              <button onClick={submitAction} disabled={acting}
                className={`px-4 py-2 text-white font-bold text-sm rounded-xl transition-all shadow-lg disabled:opacity-50 flex items-center gap-2 ${
                  actionTarget.action === 'approve' ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-red-600 hover:bg-red-700'
                }`}>
                {acting ? <Loader2 size={16} className="animate-spin" /> : actionTarget.action === 'approve' ? <CheckCircle2 size={16} /> : <XCircle size={16} />}
                {actionTarget.action === 'approve' ? 'Approve' : 'Reject'}
              </button>
            </div>
          </div>
        </div>
      )}

      {viewInvoice && (
        <ReceiptModal
          isOpen={!!viewInvoice}
          onClose={() => setViewInvoice(null)}
          title="Vendor Invoice"
          documentNumber={viewInvoice.invoiceNumber}
          plain
        >
          <VendorInvoiceDocument invoice={viewInvoice} org={orgProfile} />
        </ReceiptModal>
      )}
    </div>
  );
};

export default VendorInvoices;
