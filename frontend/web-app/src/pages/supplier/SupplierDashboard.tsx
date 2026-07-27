import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FileText, Trophy, Clock, LogOut, Send, Truck } from 'lucide-react';
import procurementApi from '../../api/procurementAxios';
import { Rfq, Quotation, PurchaseOrderResponse } from '../../types/procurement';
import { useAuth } from '../../context/AuthContext';

const quoteStatusStyle: Record<string, string> = {
  SELECTED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  REJECTED: 'bg-red-50 text-red-500 border-red-200',
  SUBMITTED: 'bg-amber-50 text-amber-700 border-amber-200',
};

const quoteStatusLabel: Record<string, string> = {
  SELECTED: 'Won',
  REJECTED: 'Not Selected',
  SUBMITTED: 'Awaiting Decision',
};

const poStatusStyle: Record<string, string> = {
  DRAFT: 'bg-slate-100 text-slate-600 border-slate-200',
  PENDING_APPROVAL: 'bg-amber-50 text-amber-700 border-amber-200',
  APPROVED: 'bg-blue-50 text-blue-700 border-blue-200',
  REJECTED: 'bg-red-50 text-red-600 border-red-200',
  SENT_TO_VENDOR: 'bg-indigo-50 text-indigo-700 border-indigo-200',
  PARTIALLY_RECEIVED: 'bg-amber-50 text-amber-700 border-amber-200',
  RECEIVED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  CLOSED: 'bg-slate-100 text-slate-500 border-slate-200',
  CANCELLED: 'bg-red-50 text-red-500 border-red-200',
};

const poStatusLabel: Record<string, string> = {
  DRAFT: 'Being Prepared',
  PENDING_APPROVAL: 'Pending Approval',
  APPROVED: 'Approved',
  REJECTED: 'Rejected Internally',
  SENT_TO_VENDOR: 'Sent to You',
  PARTIALLY_RECEIVED: 'Partially Received',
  RECEIVED: 'Received',
  CLOSED: 'Closed',
  CANCELLED: 'Cancelled',
};

const SupplierDashboard: React.FC = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [rfqs, setRfqs] = useState<Rfq[]>([]);
  const [quotations, setQuotations] = useState<Quotation[]>([]);
  const [purchaseOrders, setPurchaseOrders] = useState<PurchaseOrderResponse[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      procurementApi.get<Rfq[]>('/supplier-portal/rfqs'),
      procurementApi.get<Quotation[]>('/supplier-portal/quotations'),
      procurementApi.get<PurchaseOrderResponse[]>('/supplier-portal/purchase-orders'),
    ]).then(([rfqRes, quoteRes, poRes]) => {
      setRfqs(rfqRes.data);
      setQuotations(quoteRes.data);
      setPurchaseOrders(poRes.data);
    }).catch(() => {}).finally(() => setLoading(false));
  }, []);

  const quotedRfqIds = new Set(quotations.map(q => q.rfqId));
  const awaitingQuote = rfqs.filter(r => !quotedRfqIds.has(r.id));
  const wonCount = quotations.filter(q => q.status === 'SELECTED').length;

  const handleLogout = async () => {
    await logout();
    navigate('/supplier/login');
  };

  return (
    <div className="min-h-screen bg-slate-50">
      <div className="bg-slate-900 px-8 py-5 flex items-center justify-between">
        <div>
          <p className="text-[10px] font-black text-slate-500 uppercase tracking-widest">Supplier Portal</p>
          <h1 className="text-lg font-black text-white">{user?.username || 'Supplier'}</h1>
        </div>
        <button onClick={handleLogout}
          className="flex items-center gap-2 px-4 py-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-xl text-xs font-bold transition">
          <LogOut size={14} /> Logout
        </button>
      </div>

      <div className="p-8 space-y-8">
        {loading ? (
          <p className="text-sm text-slate-400">Loading...</p>
        ) : (
          <>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="bg-white p-4 rounded-2xl border border-slate-100 shadow-sm">
                <div className="flex items-center gap-2 text-slate-400 mb-1"><Send size={14} /><span className="text-[10px] font-black uppercase tracking-widest">Open RFQs</span></div>
                <p className="text-2xl font-black text-slate-900">{rfqs.length}</p>
              </div>
              <div className="bg-white p-4 rounded-2xl border border-slate-100 shadow-sm">
                <div className="flex items-center gap-2 text-amber-500 mb-1"><Clock size={14} /><span className="text-[10px] font-black uppercase tracking-widest">Awaiting Your Quote</span></div>
                <p className="text-2xl font-black text-slate-900">{awaitingQuote.length}</p>
              </div>
              <div className="bg-white p-4 rounded-2xl border border-slate-100 shadow-sm">
                <div className="flex items-center gap-2 text-slate-400 mb-1"><FileText size={14} /><span className="text-[10px] font-black uppercase tracking-widest">Quotes Submitted</span></div>
                <p className="text-2xl font-black text-slate-900">{quotations.length}</p>
              </div>
              <div className="bg-white p-4 rounded-2xl border border-slate-100 shadow-sm">
                <div className="flex items-center gap-2 text-emerald-500 mb-1"><Trophy size={14} /><span className="text-[10px] font-black uppercase tracking-widest">Won</span></div>
                <p className="text-2xl font-black text-slate-900">{wonCount}</p>
              </div>
            </div>

            <div>
              <h2 className="text-sm font-black text-slate-900 uppercase tracking-wide mb-3">Open Requests for Quotation</h2>
              <div className="space-y-3">
                {rfqs.map(r => (
                  <Link key={r.id} to={`/supplier/rfqs/${r.id}`}
                    className="flex items-center justify-between bg-white p-4 rounded-2xl border border-slate-100 shadow-sm hover:shadow-md transition">
                    <div>
                      <p className="font-bold text-slate-800">{r.rfqNumber} — {r.partName}</p>
                      <p className="text-xs text-slate-500">Qty {r.quantity} · Quote by {new Date(r.dueDate).toLocaleString()}</p>
                    </div>
                    {quotedRfqIds.has(r.id) ? (
                      <span className="text-[10px] font-black px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">Quoted</span>
                    ) : (
                      <span className="text-[10px] font-black px-2.5 py-1 rounded-full bg-amber-50 text-amber-700 border border-amber-200">Action Needed</span>
                    )}
                  </Link>
                ))}
                {rfqs.length === 0 && <p className="text-sm text-slate-400">No open RFQs right now.</p>}
              </div>
            </div>

            <div>
              <h2 className="text-sm font-black text-slate-900 uppercase tracking-wide mb-3">Quotation History</h2>
              <div className="bg-white rounded-3xl border border-slate-100 shadow-sm overflow-hidden">
                <table className="w-full text-sm">
                  <thead className="bg-slate-50 text-[10px] font-black uppercase text-slate-400 tracking-widest">
                    <tr>
                      <th className="text-left px-6 py-3">RFQ</th>
                      <th className="text-left px-6 py-3">Part</th>
                      <th className="text-left px-6 py-3">Your Price</th>
                      <th className="text-left px-6 py-3">Lead Time</th>
                      <th className="text-left px-6 py-3">Submitted</th>
                      <th className="text-left px-6 py-3">Outcome</th>
                    </tr>
                  </thead>
                  <tbody>
                    {quotations.map(q => (
                      <tr key={q.id} className="border-t border-slate-100">
                        <td className="px-6 py-3 font-bold text-slate-800">{q.rfqNumber}</td>
                        <td className="px-6 py-3">{q.partName}</td>
                        <td className="px-6 py-3">₹{q.unitPrice} <span className="text-slate-400">(₹{q.totalPrice} total)</span></td>
                        <td className="px-6 py-3">{q.leadTimeDays} days</td>
                        <td className="px-6 py-3 text-xs text-slate-500">{new Date(q.submittedAt).toLocaleString()}</td>
                        <td className="px-6 py-3">
                          <span className={`text-[10px] font-black px-2.5 py-1 rounded-full border ${quoteStatusStyle[q.status]}`}>
                            {quoteStatusLabel[q.status]}
                          </span>
                        </td>
                      </tr>
                    ))}
                    {quotations.length === 0 && (
                      <tr><td colSpan={6} className="px-6 py-8 text-center text-slate-300 text-xs font-bold uppercase">No quotations submitted yet</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            <div>
              <h2 className="text-sm font-black text-slate-900 uppercase tracking-wide mb-3 flex items-center gap-2">
                <Truck size={16} className="text-slate-400" /> Your Purchase Orders
              </h2>
              <div className="bg-white rounded-3xl border border-slate-100 shadow-sm overflow-hidden">
                <table className="w-full text-sm">
                  <thead className="bg-slate-50 text-[10px] font-black uppercase text-slate-400 tracking-widest">
                    <tr>
                      <th className="text-left px-6 py-3">PO Number</th>
                      <th className="text-left px-6 py-3">Order Date</th>
                      <th className="text-left px-6 py-3">Delivery Date</th>
                      <th className="text-left px-6 py-3">Amount</th>
                      <th className="text-left px-6 py-3">Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {purchaseOrders.map(po => (
                      <tr key={po.id} className="border-t border-slate-100">
                        <td className="px-6 py-3 font-bold text-slate-800">{po.poNumber}</td>
                        <td className="px-6 py-3 text-xs text-slate-500">{new Date(po.orderDate).toLocaleDateString()}</td>
                        <td className="px-6 py-3 text-xs text-slate-500">{po.deliveryDate ? new Date(po.deliveryDate).toLocaleDateString() : '—'}</td>
                        <td className="px-6 py-3">{po.currency} {po.totalAmount}</td>
                        <td className="px-6 py-3">
                          <span className={`text-[10px] font-black px-2.5 py-1 rounded-full border ${poStatusStyle[po.status]}`}>
                            {poStatusLabel[po.status] || po.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                    {purchaseOrders.length === 0 && (
                      <tr><td colSpan={5} className="px-6 py-8 text-center text-slate-300 text-xs font-bold uppercase">No purchase orders yet</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default SupplierDashboard;
