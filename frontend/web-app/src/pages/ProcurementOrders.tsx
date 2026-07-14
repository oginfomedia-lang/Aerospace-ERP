import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { ShoppingCart, Plus, RefreshCw, Trash2, Edit3, ChevronDown, ChevronRight, AlertTriangle } from 'lucide-react';
import toast from 'react-hot-toast';
import procurementApi from '../api/procurementAxios';
import api from '../api/axios';
import { Supplier, PurchaseOrderResponse, PurchaseOrderRequest, POLineItemDTO, OrganizationProfile } from '../types/procurement';
import { Part } from '../types/inventory';
import { MaterialReceiptFromPOModal } from '../components/MaterialReceiptFromPOModal';
import { POApprovalModal } from '../components/POApprovalModal';
import { ReceiptModal } from '../components/ReceiptModal';
import { POReceiptDocument } from '../components/POReceiptDocument';

const ProcurementOrders = () => {
  const [orders, setOrders] = useState<PurchaseOrderResponse[]>([]);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [parts, setParts] = useState<Part[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [selectedPOForGRN, setSelectedPOForGRN] = useState<PurchaseOrderResponse | null>(null);
  const [selectedPOForApproval, setSelectedPOForApproval] = useState<PurchaseOrderResponse | null>(null);
  const [editingPO, setEditingPO] = useState<PurchaseOrderResponse | null>(null);
  const [expandedId, setExpandedId] = useState<number | null>(null);
  const [selectedPOForReceipt, setSelectedPOForReceipt] = useState<PurchaseOrderResponse | null>(null);
  const [orgProfile, setOrgProfile] = useState<OrganizationProfile | null>(null);

  const location = useLocation();
  const state = location.state as { prefillItems?: any[], woNumber?: string } | null;

  // Form State
  const [supplierId, setSupplierId] = useState<number | ''>('');
  const [deliveryDate, setDeliveryDate] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [remarks, setRemarks] = useState('');
  const [items, setItems] = useState<POLineItemDTO[]>([
    { skuCode: '', quantity: 1, unitPrice: 0, uom: 'PCS' }
  ]);
  const [shipToName, setShipToName] = useState('');
  const [shipToAddress, setShipToAddress] = useState('');
  const [shipToState, setShipToState] = useState('');
  const [shipToStateCode, setShipToStateCode] = useState('');
  const [paymentTerms, setPaymentTerms] = useState('');
  const [payment, setPayment] = useState('');
  const [deliveryTerms, setDeliveryTerms] = useState('');
  const [exciseDuty, setExciseDuty] = useState('');
  const [termsAndConditions, setTermsAndConditions] = useState('');

  const fetchData = async () => {
    setLoading(true);
    try {
      // Assuming a GET /purchase-orders endpoint exists, otherwise we just handle creation for now
      // Since we didn't explicitly implement GET /purchase-orders in backend, we'll gracefully handle 404s
      const [suppRes, orderRes, partsRes, orgRes] = await Promise.allSettled([
        procurementApi.get('/suppliers'),
        procurementApi.get('/purchase-orders'),
        api.get('/inventory'),
        procurementApi.get('/organization-profile'),
      ]);

      if (suppRes.status === 'fulfilled') setSuppliers(suppRes.value.data);
      if (orderRes.status === 'fulfilled') setOrders(orderRes.value.data);
      // PO line items must reference a real inventory SKU, otherwise GRN part-ID
      // resolution fails later — this list is what the item picker is built from.
      if (partsRes.status === 'fulfilled') setParts(partsRes.value.data.data);
      if (orgRes.status === 'fulfilled') setOrgProfile(orgRes.value.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  useEffect(() => {
    if (state?.prefillItems && state.prefillItems.length > 0) {
      setItems(state.prefillItems.map(i => ({ skuCode: i.skuCode, quantity: i.quantity, unitPrice: 0, uom: 'PCS' })));
      setShowModal(true);
      if (state.woNumber) {
        setRemarks(`Material shortage for Work Order: ${state.woNumber}`);
      }
      // Clear state so it doesn't reopen on refresh
      window.history.replaceState({}, document.title);
    }
  }, [state]);

  const resetForm = () => {
    setSupplierId('');
    setDeliveryDate('');
    setCurrency('USD');
    setRemarks('');
    setItems([{ skuCode: '', quantity: 1, unitPrice: 0, uom: 'PCS' }]);
    // Ship-to defaults to our own address (goods normally arrive at our own site);
    // payment/delivery/excise/T&C default to the Organization Profile's boilerplate —
    // all editable per PO from here.
    setShipToName(orgProfile?.companyName || '');
    setShipToAddress(orgProfile?.addressLine || '');
    setShipToState(orgProfile?.state || '');
    setShipToStateCode(orgProfile?.stateCode || '');
    setPaymentTerms(orgProfile?.defaultPaymentTerms || '');
    setPayment(orgProfile?.defaultPayment || '');
    setDeliveryTerms(orgProfile?.defaultDeliveryTerms || '');
    setExciseDuty(orgProfile?.defaultExciseDuty || '');
    setTermsAndConditions(orgProfile?.termsAndConditions || '');
  };

  const handleOpenCreate = () => {
    setEditingPO(null);
    resetForm();
    setShowModal(true);
  };

  // Only DRAFT POs can be edited (enforced server-side too) — this is what fixes a PO
  // that was created with a bad SKU per the GRN "could not resolve" error.
  const handleOpenEdit = (po: PurchaseOrderResponse) => {
    setEditingPO(po);
    setSupplierId(po.supplierId);
    setDeliveryDate(po.deliveryDate ? new Date(po.deliveryDate).toISOString().slice(0, 16) : '');
    setCurrency(po.currency || 'USD');
    setRemarks(po.remarks || '');
    setItems(po.items.map(i => ({ ...i })));
    setShipToName(po.shipToName || '');
    setShipToAddress(po.shipToAddress || '');
    setShipToState(po.shipToState || '');
    setShipToStateCode(po.shipToStateCode || '');
    setPaymentTerms(po.paymentTerms || '');
    setPayment(po.payment || '');
    setDeliveryTerms(po.deliveryTerms || '');
    setExciseDuty(po.exciseDuty || '');
    setTermsAndConditions(po.termsAndConditions || '');
    setShowModal(true);
  };

  const handleDelete = async (po: PurchaseOrderResponse) => {
    if (!window.confirm(`Delete draft PO ${po.poNumber}? This cannot be undone.`)) return;
    try {
      await procurementApi.delete(`/purchase-orders/${po.id}`);
      toast.success(`${po.poNumber} deleted.`);
      setOrders(orders.filter(o => o.id !== po.id));
    } catch (err) {
      console.error(err);
    }
  };

  const handleAddItem = () => {
    setItems([...items, { skuCode: '', quantity: 1, unitPrice: 0, uom: 'PCS' }]);
  };

  const handleRemoveItem = (index: number) => {
    setItems(items.filter((_, i) => i !== index));
  };

  const handleItemChange = (index: number, field: keyof POLineItemDTO, value: string | number) => {
    const newItems = [...items];
    newItems[index] = { ...newItems[index], [field]: value };
    if (field === 'skuCode') {
      // HSN/tax rate are product-classification facts, not PO-negotiated — snapshot
      // them straight from the Part item master (unitPrice stays manually entered).
      const part = parts.find(p => p.serialNumber === value);
      newItems[index].hsnCode = part?.hsnCode || undefined;
      newItems[index].taxRatePercent = part?.taxRatePercent ?? undefined;
      newItems[index].description = part?.description || undefined;
    }
    setItems(newItems);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!supplierId) {
      toast.error('Please select a supplier');
      return;
    }

    const payload: PurchaseOrderRequest = {
      supplierId: Number(supplierId),
      deliveryDate: deliveryDate ? new Date(deliveryDate).toISOString() : new Date().toISOString(),
      currency,
      remarks,
      shipToName: shipToName || undefined,
      shipToAddress: shipToAddress || undefined,
      shipToState: shipToState || undefined,
      shipToStateCode: shipToStateCode || undefined,
      paymentTerms: paymentTerms || undefined,
      payment: payment || undefined,
      deliveryTerms: deliveryTerms || undefined,
      exciseDuty: exciseDuty || undefined,
      termsAndConditions: termsAndConditions || undefined,
      items: items.map(item => ({
        ...item,
        quantity: Number(item.quantity),
        unitPrice: Number(item.unitPrice)
      }))
    };

    try {
      if (editingPO) {
        const res = await procurementApi.put<PurchaseOrderResponse>(`/purchase-orders/${editingPO.id}`, payload);
        toast.success(`${res.data.poNumber} updated.`);
        setOrders(orders.map(o => o.id === editingPO.id ? res.data : o));
      } else {
        const res = await procurementApi.post<PurchaseOrderResponse>('/purchase-orders', payload);
        toast.success('Purchase Order created successfully!');
        if (res.data.duplicateWarning) {
          toast(res.data.duplicateWarning, { icon: '⚠️', duration: 6000 });
        }
        setOrders([res.data, ...orders]);
      }
      setShowModal(false);
      setEditingPO(null);
      resetForm();
    } catch (err) {
      console.error(err);
    }
  };

  const handleUpdateStatus = async (id: number, status: string) => {
    try {
      const res = await procurementApi.put(`/purchase-orders/${id}/status`, null, {
        params: { status }
      });
      toast.success(`PO status updated to ${status}!`);
      setOrders(orders.map(o => o.id === id ? res.data : o));
    } catch (err) {
      console.error(err);
    }
  };

  // PUR-BR-001: evaluates the approval matrix; either starts the chain or auto-approves
  const handleSubmitForApproval = async (id: number) => {
    try {
      const res = await procurementApi.post<PurchaseOrderResponse>(`/purchase-orders/${id}/submit`);
      toast.success(
        res.data.status === 'APPROVED'
          ? 'No approval required for this amount — PO approved automatically.'
          : `Submitted — awaiting Level ${res.data.currentApprovalLevel} approval.`
      );
      setOrders(orders.map(o => o.id === id ? res.data : o));
    } catch (err) {
      console.error(err);
    }
  };

  // QA rejected part of a shipment — pre-fill a fresh draft PO for just the rejected
  // quantity so the shortfall it left behind (especially one raised from a WO shortage)
  // can actually be re-ordered instead of silently staying unresolved.
  const handleRaiseReplacementPO = (order: PurchaseOrderResponse, item: POLineItemDTO) => {
    setEditingPO(null);
    setSupplierId(order.supplierId);
    setDeliveryDate('');
    setCurrency(order.currency || 'USD');
    setRemarks(
      `Replacement for ${item.rejectedQuantity} unit(s) of ${item.skuCode} rejected under NCR ${item.ncrNumber} (original PO: ${order.poNumber})` +
      (order.sourceWoNumber ? ` — Work Order: ${order.sourceWoNumber}` : '')
    );
    setItems([{
      skuCode: item.skuCode,
      quantity: item.rejectedQuantity || 0,
      unitPrice: item.unitPrice,
      uom: item.uom,
      replacementForNcrNumber: item.ncrNumber,
    }]);
    setShowModal(true);
  };

  // A replacement PO was already raised for this NCR if any (non-cancelled) order has a
  // line item tagged with it — checked against `orders`, already loaded in state, so this
  // needs no extra API call.
  const findReplacementPO = (ncrNumber: string) =>
    orders.find(o => o.status !== 'CANCELLED' &&
      o.items.some(i => i.replacementForNcrNumber === ncrNumber));

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Purchase Orders</h1>
          <p className="text-slate-500 text-sm font-medium mt-1">Draft and track supplier orders</p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={fetchData}
            className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-xl transition-all"
          >
            <RefreshCw size={20} className={loading ? 'animate-spin' : ''} />
          </button>
          <button
            onClick={handleOpenCreate}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold rounded-xl transition-all shadow-lg shadow-blue-600/20"
          >
            <Plus size={18} />
            Draft PO
          </button>
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase text-xs tracking-wider">
            <tr>
              <th className="px-6 py-4">PO Number</th>
              <th className="px-6 py-4">Supplier</th>
              <th className="px-6 py-4">Status</th>
              <th className="px-6 py-4">Delivery</th>
              <th className="px-6 py-4 text-right">Total Amount</th>
              <th className="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {orders.map(order => {
              const hasQaOutcome = order.items?.some(i => (i.acceptedQuantity ?? 0) > 0 || (i.rejectedQuantity ?? 0) > 0);
              const isExpanded = expandedId === order.id;
              return (
              <React.Fragment key={order.id}>
              <tr className="hover:bg-slate-50/50 transition-colors">
                <td className="px-6 py-4">
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => setExpandedId(isExpanded ? null : order.id)}
                      title="View line items"
                      className="p-0.5 text-slate-400 hover:text-slate-700"
                    >
                      {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                    </button>
                    <button
                      onClick={() => setSelectedPOForReceipt(order)}
                      title="View / print / download this Purchase Order"
                      className="font-bold text-slate-900 font-mono hover:text-blue-600 hover:underline transition-colors"
                    >
                      {order.poNumber}
                    </button>
                    {hasQaOutcome && (
                      <span className="px-1.5 py-0.5 text-[9px] font-black tracking-wider uppercase rounded bg-slate-100 text-slate-500 border border-slate-200">
                        QA
                      </span>
                    )}
                    {order.sourceWoNumber ? (
                      <span title={order.remarks || undefined} className="px-1.5 py-0.5 text-[9px] font-black tracking-wider uppercase rounded bg-violet-100 text-violet-700 border border-violet-200">
                        Shortage: {order.sourceWoNumber}
                      </span>
                    ) : order.remarks?.startsWith('AUTO-RAISED') && (
                      <span title={order.remarks} className="px-1.5 py-0.5 text-[9px] font-black tracking-wider uppercase rounded bg-violet-100 text-violet-700 border border-violet-200">
                        AUTO
                      </span>
                    )}
                  </div>
                </td>
                <td className="px-6 py-4 font-medium text-slate-700">
                  {order.supplierName}
                </td>
                <td className="px-6 py-4">
                  <span className={`px-2.5 py-1 text-[11px] font-black tracking-wider uppercase rounded-full ${
                    order.status === 'SENT_TO_VENDOR' ? 'bg-indigo-100 text-indigo-700' :
                    order.status === 'RECEIVED' || order.status === 'APPROVED' ? 'bg-emerald-100 text-emerald-700' :
                    order.status === 'CANCELLED' || order.status === 'REJECTED' ? 'bg-red-100 text-red-700' :
                    order.status === 'PENDING_APPROVAL' ? 'bg-amber-100 text-amber-700 animate-pulse' :
                    order.status === 'CLOSED' ? 'bg-slate-200 text-slate-600' :
                    'bg-amber-100 text-amber-700'
                  }`}>
                    {order.status || 'DRAFT'}
                    {order.status === 'PENDING_APPROVAL' && order.currentApprovalLevel != null && ` · L${order.currentApprovalLevel}`}
                  </span>
                </td>
                <td className="px-6 py-4 text-slate-600">
                  {new Date(order.deliveryDate).toLocaleDateString()}
                </td>
                <td className="px-6 py-4 text-right font-bold text-slate-900">
                  {order.currency} {order.totalAmount?.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </td>
                <td className="px-6 py-4 text-right">
                  {order.status === 'DRAFT' && (
                    <div className="flex gap-2 justify-end">
                      <button
                        onClick={() => handleOpenEdit(order)}
                        title="Edit PO"
                        className="p-1.5 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-xl transition-all"
                      >
                        <Edit3 size={16} />
                      </button>
                      <button
                        onClick={() => handleDelete(order)}
                        title="Delete PO"
                        className="p-1.5 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-xl transition-all"
                      >
                        <Trash2 size={16} />
                      </button>
                      <button
                        onClick={() => handleSubmitForApproval(order.id)}
                        className="px-3 py-1.5 bg-violet-600 hover:bg-violet-700 text-white text-xs font-bold rounded-xl transition-all shadow-sm"
                      >
                        Submit for Approval
                      </button>
                    </div>
                  )}
                  {order.status === 'PENDING_APPROVAL' && (
                    <button
                      onClick={() => setSelectedPOForApproval(order)}
                      className="px-3 py-1.5 bg-amber-500 hover:bg-amber-600 text-white text-xs font-bold rounded-xl transition-all shadow-sm"
                    >
                      Review · Level {order.currentApprovalLevel}
                    </button>
                  )}
                  {order.status === 'APPROVED' && (
                    <button
                      onClick={() => handleUpdateStatus(order.id, 'SENT_TO_VENDOR')}
                      className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl transition-all shadow-sm"
                    >
                      Send to Vendor
                    </button>
                  )}
                  {order.status === 'SENT_TO_VENDOR' && (
                    <div className="flex gap-2 justify-end">
                      <button
                        onClick={() => handleUpdateStatus(order.id, 'RECEIVED')}
                        className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl transition-all shadow-sm"
                      >
                        Receive
                      </button>
                      <button
                        onClick={() => handleUpdateStatus(order.id, 'CANCELLED')}
                        className="px-3 py-1.5 bg-red-50 hover:bg-red-100 text-red-600 text-xs font-bold rounded-xl transition-all"
                      >
                        Cancel
                      </button>
                    </div>
                  )}
                  {order.status === 'RECEIVED' && (
                    <button
                      onClick={() => setSelectedPOForGRN(order)}
                      className="px-3 py-1.5 bg-cyan-600 hover:bg-cyan-700 text-white text-xs font-bold rounded-xl transition-all shadow-sm"
                    >
                      Record Material Receipt →
                    </button>
                  )}
                </td>
              </tr>
              {isExpanded && (
                <tr className="bg-slate-50/70">
                  <td colSpan={6} className="px-6 py-4">
                    <table className="w-full text-xs">
                      <thead className="text-slate-400 uppercase tracking-wider">
                        <tr>
                          <th className="text-left pb-2">SKU</th>
                          <th className="text-right pb-2">Ordered</th>
                          <th className="text-right pb-2">Unit Price</th>
                          <th className="text-left pb-2 pl-4">QA Outcome</th>
                          <th className="text-right pb-2">Action</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-100">
                        {order.items.map((item, idx) => {
                          const accepted = item.acceptedQuantity ?? 0;
                          const rejected = item.rejectedQuantity ?? 0;
                          return (
                            <tr key={idx}>
                              <td className="py-2 font-mono font-bold text-slate-700">{item.skuCode}</td>
                              <td className="py-2 text-right text-slate-600">{item.quantity} {item.uom}</td>
                              <td className="py-2 text-right text-slate-600">{order.currency} {item.unitPrice?.toFixed(2)}</td>
                              <td className="py-2 pl-4">
                                <div className="flex flex-wrap gap-1.5">
                                  {accepted > 0 && (
                                    <span className="px-1.5 py-0.5 font-black rounded bg-emerald-100 text-emerald-700">
                                      ✓ {accepted} accepted
                                    </span>
                                  )}
                                  {rejected > 0 && (
                                    <span className="px-1.5 py-0.5 font-black rounded bg-red-100 text-red-700">
                                      ✕ {rejected} rejected{item.ncrNumber ? ` · ${item.ncrNumber}` : ''}
                                    </span>
                                  )}
                                  {accepted === 0 && rejected === 0 && (
                                    <span className="text-slate-400">Not yet inspected</span>
                                  )}
                                </div>
                              </td>
                              <td className="py-2 text-right">
                                {rejected > 0 && (() => {
                                  const replacementPO = item.ncrNumber ? findReplacementPO(item.ncrNumber) : undefined;
                                  return replacementPO ? (
                                    <span className="px-2 py-1 text-[11px] font-bold text-violet-700 bg-violet-50 border border-violet-200 rounded-lg whitespace-nowrap">
                                      ↻ Replacement raised: {replacementPO.poNumber} ({replacementPO.status})
                                    </span>
                                  ) : (
                                    <button
                                      onClick={() => handleRaiseReplacementPO(order, item)}
                                      className="px-2 py-1 bg-red-600 hover:bg-red-700 text-white font-bold rounded-lg transition-all"
                                    >
                                      Raise Replacement PO →
                                    </button>
                                  );
                                })()}
                              </td>
                            </tr>
                          );
                        })}
                      </tbody>
                    </table>
                    {order.sourceWoNumber && order.items?.some(i => (i.rejectedQuantity ?? 0) > 0) && (
                      <div className="mt-3 flex items-center gap-2 px-3 py-2 bg-amber-50 border border-amber-200 rounded-xl text-amber-800 text-xs font-bold">
                        <AlertTriangle size={14} className="shrink-0" />
                        Work Order {order.sourceWoNumber} is still short — some of this PO's quantity was rejected in QA and hasn't been replaced yet.
                      </div>
                    )}
                  </td>
                </tr>
              )}
              </React.Fragment>
              );
            })}
            {orders.length === 0 && !loading && (
              <tr>
                <td colSpan={6} className="px-6 py-12 text-center text-slate-500">
                  <ShoppingCart className="w-12 h-12 mx-auto text-slate-300 mb-3" />
                  <p className="font-medium">No purchase orders found</p>
                  <p className="text-sm mt-1">Draft a new PO to get started.</p>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl w-full max-w-2xl overflow-hidden shadow-2xl animate-in fade-in zoom-in-95 duration-200 flex flex-col max-h-[90vh]">
            <div className="p-6 border-b border-slate-100 shrink-0">
              <h2 className="text-xl font-bold text-slate-900">
                {editingPO ? `Edit Purchase Order: ${editingPO.poNumber}` : 'Draft Purchase Order'}
              </h2>
            </div>
            
            <div className="p-6 overflow-y-auto flex-1">
              <form id="po-form" onSubmit={handleSubmit} className="space-y-6">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-1.5">Supplier</label>
                    <select required value={supplierId} onChange={e => setSupplierId(Number(e.target.value))}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all">
                      <option value="" disabled>Select a supplier...</option>
                      {suppliers.map(s => (
                        <option key={s.id} value={s.id}>{s.name} ({s.vendorCode})</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-1.5">Expected Delivery</label>
                    <input required type="datetime-local" value={deliveryDate} onChange={e => setDeliveryDate(e.target.value)} 
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all" />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">Remarks / Notes</label>
                  <input type="text" value={remarks} onChange={e => setRemarks(e.target.value)}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                    placeholder="Urgent delivery instructions..." />
                </div>

                {/* Shipment & Document Terms — pre-filled from Org Settings, editable per PO */}
                <div className="border-t border-slate-100 pt-6 space-y-4">
                  <h3 className="text-sm font-bold text-slate-900">Shipment & Document Terms</h3>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5">Ship To Name</label>
                      <input type="text" value={shipToName} onChange={e => setShipToName(e.target.value)}
                        className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all" />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5">Ship To Address</label>
                      <input type="text" value={shipToAddress} onChange={e => setShipToAddress(e.target.value)}
                        className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all" />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5">Ship To State</label>
                      <input type="text" value={shipToState} onChange={e => setShipToState(e.target.value)}
                        className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all" />
                    </div>
                    <div>
                      <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5">Ship To State Code</label>
                      <input type="text" value={shipToStateCode} onChange={e => setShipToStateCode(e.target.value)}
                        className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all" />
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5">Payment Terms</label>
                    <textarea rows={2} value={paymentTerms} onChange={e => setPaymentTerms(e.target.value)}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all" />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5">Payment</label>
                    <input type="text" value={payment} onChange={e => setPayment(e.target.value)}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                      placeholder="e.g. 30% Advance alongwith order & balance against Proforma Invoice" />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5">Delivery Terms</label>
                    <textarea rows={2} value={deliveryTerms} onChange={e => setDeliveryTerms(e.target.value)}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all" />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5">Excise Duty</label>
                    <input type="text" value={exciseDuty} onChange={e => setExciseDuty(e.target.value)}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all" />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-slate-500 uppercase mb-1.5">Special Terms and Conditions</label>
                    <textarea rows={6} value={termsAndConditions} onChange={e => setTermsAndConditions(e.target.value)}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all font-mono text-xs"
                      placeholder="One clause per line — numbered automatically on the printed PO." />
                  </div>
                </div>

                {/* Line Items */}
                <div className="border-t border-slate-100 pt-6">
                  <div className="flex justify-between items-center mb-4">
                    <h3 className="text-sm font-bold text-slate-900">Line Items</h3>
                    <button type="button" onClick={handleAddItem}
                      className="text-sm font-bold text-blue-600 hover:text-blue-700 flex items-center gap-1">
                      <Plus size={16} /> Add Item
                    </button>
                  </div>

                  <div className="space-y-3">
                    {items.map((item, idx) => {
                      const knownPart = parts.find(p => p.serialNumber === item.skuCode);
                      const unresolvedSku = item.skuCode && !knownPart;
                      return (
                      <div key={idx} className="flex gap-3 items-start p-3 bg-slate-50 rounded-xl border border-slate-200">
                        <div className="flex-1">
                          <select required value={item.skuCode}
                            onChange={e => handleItemChange(idx, 'skuCode', e.target.value)}
                            className={`w-full px-3 py-2 text-sm bg-white border rounded-lg focus:outline-none focus:border-blue-500 ${unresolvedSku ? 'border-red-300' : 'border-slate-200'}`}>
                            <option value="" disabled>Select an item...</option>
                            {parts.map(p => (
                              <option key={p.id} value={p.serialNumber}>{p.name} ({p.serialNumber})</option>
                            ))}
                            {/* Keep a prefilled/legacy SKU visible even if it's not a known part, so it's obvious it needs fixing */}
                            {unresolvedSku && <option value={item.skuCode}>⚠ Unknown SKU: {item.skuCode}</option>}
                          </select>
                          {unresolvedSku && (
                            <p className="text-[10px] text-red-500 font-bold mt-1">
                              This SKU doesn't match any item in Inventory — GRN receipt will fail. Pick a real item.
                            </p>
                          )}
                          {knownPart && (
                            <p className="text-[10px] text-slate-400 mt-1 font-medium">
                              HSN: {item.hsnCode || '—'} · Tax: {item.taxRatePercent != null ? `${item.taxRatePercent}%` : '—'}
                            </p>
                          )}
                          <input type="text" placeholder="Description (auto-filled from item, editable)"
                            value={item.description || ''}
                            onChange={e => handleItemChange(idx, 'description', e.target.value)}
                            className="w-full mt-1.5 px-3 py-1.5 text-xs bg-white border border-slate-200 rounded-lg focus:outline-none focus:border-blue-500" />
                        </div>
                        <div className="w-24">
                          <input required type="number" min="1" placeholder="Qty" value={item.quantity}
                            onChange={e => handleItemChange(idx, 'quantity', e.target.value)}
                            className="w-full px-3 py-2 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:border-blue-500" />
                        </div>
                        <div className="w-32">
                          <input required type="number" min="0" step="0.01" placeholder="Price" value={item.unitPrice}
                            onChange={e => handleItemChange(idx, 'unitPrice', e.target.value)}
                            className="w-full px-3 py-2 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:border-blue-500" />
                        </div>
                        <div className="w-24">
                          <select value={item.uom} onChange={e => handleItemChange(idx, 'uom', e.target.value)}
                            className="w-full px-3 py-2 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:border-blue-500">
                            <option value="PCS">PCS</option>
                            <option value="KG">KG</option>
                            <option value="L">L</option>
                          </select>
                        </div>
                        <button type="button" onClick={() => handleRemoveItem(idx)} disabled={items.length === 1}
                          className="p-2 text-slate-400 hover:text-red-600 disabled:opacity-50">
                          <Trash2 size={18} />
                        </button>
                      </div>
                      );
                    })}
                  </div>
                </div>
              </form>
            </div>

            <div className="p-6 border-t border-slate-100 shrink-0 bg-slate-50 flex justify-between items-center">
              <div className="text-sm text-slate-500 font-medium">
                Total: <span className="text-slate-900 font-bold text-lg">${items.reduce((acc, item) => acc + (Number(item.quantity || 0) * Number(item.unitPrice || 0)), 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
              </div>
              <div className="flex gap-3">
                <button type="button" onClick={() => { setShowModal(false); setEditingPO(null); }}
                  className="px-4 py-2 bg-white border border-slate-200 hover:bg-slate-100 text-slate-700 font-bold rounded-xl transition-all">
                  Cancel
                </button>
                <button type="submit" form="po-form"
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl transition-all shadow-lg shadow-blue-600/20">
                  {editingPO ? 'Save Changes' : 'Submit PO'}
                </button>
              </div>
            </div>

          </div>
        </div>
      )}

      {selectedPOForGRN && (
        <MaterialReceiptFromPOModal
          po={selectedPOForGRN}
          onClose={() => setSelectedPOForGRN(null)}
          onSuccess={() => {
            setSelectedPOForGRN(null);
            fetchData();
          }}
        />
      )}

      {selectedPOForApproval && (
        <POApprovalModal
          po={selectedPOForApproval}
          onClose={() => setSelectedPOForApproval(null)}
          onSuccess={() => {
            setSelectedPOForApproval(null);
            fetchData();
          }}
        />
      )}

      {selectedPOForReceipt && (
        <ReceiptModal
          isOpen={!!selectedPOForReceipt}
          onClose={() => setSelectedPOForReceipt(null)}
          title="Purchase Order"
          documentNumber={selectedPOForReceipt.poNumber}
          plain
        >
          <POReceiptDocument
            po={selectedPOForReceipt}
            supplier={suppliers.find(s => s.id === selectedPOForReceipt.supplierId)}
            org={orgProfile}
            parts={parts}
          />
        </ReceiptModal>
      )}
    </div>
  );
};

export default ProcurementOrders;
