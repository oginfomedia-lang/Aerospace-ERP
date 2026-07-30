import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { FileText, PlusCircle, Send, Check, X } from 'lucide-react';
import toast from 'react-hot-toast';
import procurementApi from '../api/procurementAxios';
import { PurchaseRequisition } from '../types/procurement';
import { CreateRfqModal } from '../components/CreateRfqModel';
import { CreatePurchaseRequisitionModal } from '../components/CreatePurchaseRequisitionModal';
import { Download } from 'lucide-react';
import { exportToExcel } from '../utils/excelExport';
import { useAuth } from '../context/AuthContext';
import { CAN_MANAGE_PROCUREMENT, CAN_APPROVE_PROCUREMENT, hasAnyRole } from '../utils/permissions';

const statusStyle: Record<string, string> = {
  PENDING_APPROVAL: 'bg-amber-50 text-amber-700',
  DRAFT: 'bg-slate-100 text-slate-600',
  RFQ_SENT: 'bg-blue-50 text-blue-600',
  QUOTES_RECEIVED: 'bg-amber-50 text-amber-700',
  SUPPLIER_SELECTED: 'bg-emerald-50 text-emerald-700',
  PO_RAISED: 'bg-indigo-50 text-indigo-700',
  CANCELLED: 'bg-red-50 text-red-600',
};

const PurchaseRequisitions: React.FC = () => {
  const { user } = useAuth();
  const canManage = hasAnyRole(user?.roles, CAN_MANAGE_PROCUREMENT);
  const canApprove = hasAnyRole(user?.roles, CAN_APPROVE_PROCUREMENT);
  const [prs, setPrs] = useState<PurchaseRequisition[]>([]);
  const [loading, setLoading] = useState(true);
  const [rfqTarget, setRfqTarget] = useState<PurchaseRequisition | null>(null);
  const [showCreate, setShowCreate] = useState(false);

  const load = () => {
    setLoading(true);
    procurementApi.get<PurchaseRequisition[]>('/purchase-requisitions')
      .then(res => setPrs(res.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const handleApprove = async (pr: PurchaseRequisition) => {
    try {
      await procurementApi.post(`/purchase-requisitions/${pr.id}/approve`);
      toast.success(`${pr.prNumber} approved.`);
      load();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to approve.');
    }
  };

  const handleReject = async (pr: PurchaseRequisition) => {
    const remarks = window.prompt(`Reason for rejecting ${pr.prNumber}?`);
    if (remarks === null) return;
    if (!remarks.trim()) {
      toast.error('Rejection remarks are mandatory.');
      return;
    }
    try {
      await procurementApi.post(`/purchase-requisitions/${pr.id}/reject`, { remarks });
      toast.success(`${pr.prNumber} rejected.`);
      load();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to reject.');
    }
  };

  const handleExport = () => {
    exportToExcel(
      prs.map(pr => ({
        'PR Number': pr.prNumber,
        'Part': pr.partName,
        'SKU': pr.skuCode,
        'Quantity Needed': pr.quantityNeeded,
        'Required By': pr.requiredByDate ? new Date(pr.requiredByDate).toLocaleDateString() : '',
        'Source': pr.sourceType,
        'Status': pr.status,
        'Created At': new Date(pr.createdAt).toLocaleString(),
      })),
      'Purchase_Requisition_Register'
    );
  };


  return (
    <div className="p-8">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <div className="p-2.5 bg-indigo-600 rounded-2xl text-white shadow-lg shadow-indigo-200">
            <FileText size={20} />
          </div>
          <div>
            <h1 className="text-xl font-black text-slate-900">Purchase Requisitions</h1>
            <p className="text-xs text-slate-500 font-medium">Auto-raised from shortages/low stock, or created manually</p>
          </div>
        </div>
        <div className="flex gap-2">
          <button onClick={handleExport}
            className="flex items-center gap-2 px-4 py-2.5 bg-white border border-slate-200 text-slate-600 text-sm font-black rounded-xl hover:bg-slate-50 transition">
            <Download size={16} /> Export to Excel
          </button>
          {canManage && (
            <button onClick={() => setShowCreate(true)}
              className="flex items-center gap-2 px-4 py-2.5 bg-indigo-600 text-white text-sm font-black rounded-xl hover:bg-indigo-700 transition shadow-lg shadow-indigo-200">
              <PlusCircle size={16} /> New Purchase Requisition
            </button>
          )}
        </div>
      </div>

      {loading ? (
        <p className="text-sm text-slate-400">Loading...</p>
      ) : (
        <div className="bg-white rounded-3xl border border-slate-100 shadow-sm overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 text-[10px] font-black uppercase text-slate-400 tracking-widest">
              <tr>
                <th className="text-left px-6 py-3">PR Number</th>
                <th className="text-left px-6 py-3">Part</th>
                <th className="text-left px-6 py-3">Qty</th>
                <th className="text-left px-6 py-3">Source</th>
                <th className="text-left px-6 py-3">Status</th>
                <th className="text-right px-6 py-3">Action</th>
              </tr>
            </thead>
            <tbody>
              {prs.map(pr => (
                <tr key={pr.id} className="border-t border-slate-100">
                  <td className="px-6 py-3 font-bold text-slate-800">{pr.prNumber}</td>
                  <td className="px-6 py-3">{pr.partName} <span className="text-slate-400">({pr.skuCode})</span></td>
                  <td className="px-6 py-3">{pr.quantityNeeded}</td>
                  <td className="px-6 py-3 text-xs text-slate-500">{pr.sourceType}{pr.sourceWoNumber ? ` · ${pr.sourceWoNumber}` : ''}</td>
                  <td className="px-6 py-3">
                    <span className={`text-[10px] font-black px-2 py-1 rounded-lg ${statusStyle[pr.status]}`}>{pr.status}</span>
                  </td>
                  <td className="px-6 py-3 text-right">
                    <div className="flex items-center justify-end gap-3">
                      {pr.latestRfqId && (
                        <Link to={`/rfqs/${pr.latestRfqId}`}
                          className="flex items-center gap-1 text-xs font-bold text-slate-500 hover:text-indigo-700">
                          <Send size={14} /> {pr.latestRfqNumber}
                        </Link>
                      )}
                      {pr.status === 'DRAFT' && canManage && (
                        <button onClick={() => setRfqTarget(pr)}
                          className="flex items-center gap-1 text-xs font-bold text-indigo-600 hover:text-indigo-700">
                          <PlusCircle size={14} /> Create RFQ
                        </button>
                      )}
                      {pr.status === 'PENDING_APPROVAL' && canApprove && (
                        <>
                          <button onClick={() => handleApprove(pr)}
                            className="flex items-center gap-1 text-xs font-bold text-emerald-600 hover:text-emerald-700">
                            <Check size={14} /> Approve
                          </button>
                          <button onClick={() => handleReject(pr)}
                            className="flex items-center gap-1 text-xs font-bold text-red-500 hover:text-red-600">
                            <X size={14} /> Reject
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <CreateRfqModal pr={rfqTarget} onClose={() => setRfqTarget(null)} onSuccess={() => { setRfqTarget(null); load(); }} />
      <CreatePurchaseRequisitionModal isOpen={showCreate} onClose={() => setShowCreate(false)}
        onSuccess={() => { setShowCreate(false); load(); }} />
    </div>
  );
};

export default PurchaseRequisitions;