import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import procurementApi from '../api/procurementAxios';
import { Link } from 'react-router-dom';
import {
  Edit3, Search, Loader2, Clock, MinusCircle, PlusCircle,
  Layers, MapPin, SlidersHorizontal, CalendarX2, Ban, RotateCcw, Printer, History, X
} from 'lucide-react';
import { Part, ApiResponse, MaterialIssueRecord } from '../types/inventory';
import { OrganizationProfile } from '../types/procurement';
import { AddPartModal } from '../components/AddPartModal';
import { EditPartModal } from '../components/EditPartModal';
import { StockAdjustmentModal } from '../components/StockAdjustmentModal';
import { ReceiptModal } from '../components/ReceiptModal';
import { ItemRegisterDocument } from '../components/ItemRegisterDocument';
import { MaterialIssueReceiptDocument } from '../components/MaterialIssueReceiptDocument';
import { IssueMaterialPickerModal } from '../components/IssueMaterialPickerModal';
import { useAuth } from '../context/AuthContext';
import toast from 'react-hot-toast';

const statusStyle: Record<string, string> = {
  APPROVED:   'bg-emerald-50 text-emerald-700 border-emerald-200',
  PENDING_QA: 'bg-amber-50 text-amber-700 border-amber-200',
  REJECTED:   'bg-red-50 text-red-700 border-red-200',
  RESERVED:   'bg-violet-50 text-violet-700 border-violet-200',
  EMPTY:      'bg-slate-50 text-slate-500 border-slate-200',
};

const criticalityStyle: Record<string, string> = {
  HIGH:   'text-red-600 bg-red-50',
  MEDIUM: 'text-amber-600 bg-amber-50',
  LOW:    'text-emerald-600 bg-emerald-50',
};

// FIX: Added expiry display helper — expiryDate comes from PartResponseDTO
// which is populated by mapWithLiveStock() → primaryBalance.getExpiryDate()
// Color coding: red = expired, orange = ≤30 days, amber = ≤90 days, slate = fine
const getExpiryDisplay = (expiryDate: string | null) => {
  if (!expiryDate) return null;

  const expiry = new Date(expiryDate);
  const today = new Date();
  const daysLeft = Math.ceil((expiry.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));

  if (daysLeft < 0)   return { label: 'EXPIRED',           style: 'text-red-600 bg-red-50 border-red-200' };
  if (daysLeft === 0) return { label: 'EXPIRES TODAY',     style: 'text-red-600 bg-red-50 border-red-200' };
  if (daysLeft <= 30) return { label: `${daysLeft}d left`, style: 'text-orange-600 bg-orange-50 border-orange-200' };
  if (daysLeft <= 90) return { label: `${daysLeft}d left`, style: 'text-amber-600 bg-amber-50 border-amber-200' };
  return { label: expiry.toLocaleDateString(), style: 'text-slate-400 bg-slate-50 border-slate-200' };
};

const InventoryList = () => {
  const { user } = useAuth();
  const [parts, setParts] = useState<Part[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isAdjustModalOpen, setIsAdjustModalOpen] = useState(false);
  const [selectedPart, setSelectedPart] = useState<Part | null>(null);
  const [showRegister, setShowRegister] = useState(false);
  const [orgProfile, setOrgProfile] = useState<OrganizationProfile | null>(null);
  const [issueReceipt, setIssueReceipt] = useState<MaterialIssueRecord | null>(null);
  const [issuePickerPart, setIssuePickerPart] = useState<Part | null>(null);
  const [pastIssuesPart, setPastIssuesPart] = useState<Part | null>(null);
  const [pastIssues, setPastIssues] = useState<MaterialIssueRecord[]>([]);
  const [pastIssuesLoading, setPastIssuesLoading] = useState(false);

  const fetchParts = async (query = '') => {
    setLoading(true);
    try {
      const url = query ? `/inventory/search?name=${encodeURIComponent(query)}` : '/inventory';
      const res = await api.get<ApiResponse<Part[]>>(url);
      setParts(res.data.data);
    } catch {
      toast.error('ERP Sync Failed: Could not fetch inventory ledger.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // Cross-service lookup purely for the printed Item Register's letterhead —
    // read-only, same pattern as ProcurementOrders.tsx/MaterialReceipts.tsx.
    procurementApi.get<OrganizationProfile>('/organization-profile')
      .then(res => setOrgProfile(res.data)).catch(() => {});
  }, []);

  useEffect(() => {
    const delay = setTimeout(() => fetchParts(searchTerm), 300);
    return () => clearTimeout(delay);
  }, [searchTerm]);

  const handleIssue = (part: Part) => {
    if (!part.quantity || part.quantity <= 0) {
      toast.error('No approved stock available to issue for this part.');
      return;
    }
    setIssuePickerPart(part);
  };

  const handleIssued = (response: Part) => {
    const issueRecordId = response.materialIssueRecordId;
    if (issueRecordId) {
      api.get(`/inventory/material-issue-records/${issueRecordId}`)
        .then(r => setIssueReceipt(r.data.data)).catch(() => {});
    }
    fetchParts(searchTerm);
  };

  const handleOpenEdit = (part: Part) => { setSelectedPart(part); setIsEditModalOpen(true); };
  const handleOpenAdjust = (part: Part) => { setSelectedPart(part); setIsAdjustModalOpen(true); };

  // Reopens the exact same receipt modal shown right after issuing — the only way back
  // to a past issue once its initial popup is closed, previously there was none at all.
  const handleViewPastIssues = (part: Part) => {
    setPastIssuesPart(part);
    setPastIssuesLoading(true);
    api.get<ApiResponse<MaterialIssueRecord[]>>('/inventory/material-issue-records', { params: { partId: part.id } })
      .then(res => setPastIssues(res.data.data))
      .catch(() => setPastIssues([]))
      .finally(() => setPastIssuesLoading(false));
  };

  // Items are never hard-deleted — ledger/transaction history references them by ID, and a
  // real DELETE would silently orphan that traceability. Deactivating (ITM lifecycle status)
  // is the safe, reversible equivalent and keeps the item visible with an INACTIVE badge.
  const handleToggleActive = async (part: Part) => {
    const nextStatus = part.itemStatus === 'INACTIVE' ? 'ACTIVE' : 'INACTIVE';
    const confirmed = window.confirm(
      nextStatus === 'INACTIVE'
        ? `Deactivate "${part.name}"? It stays visible for traceability but won't be usable in new transactions.`
        : `Reactivate "${part.name}"?`
    );
    if (!confirmed) return;

    try {
      await toast.promise(
        api.put(`/inventory/${part.id}`, {
          name: part.name,
          serialNumber: part.serialNumber,
          partType: part.partType,
          criticality: part.criticality,
          reorderLevel: part.reorderLevel,
          itemStatus: nextStatus,
        }),
        {
          loading: `${nextStatus === 'INACTIVE' ? 'Deactivating' : 'Reactivating'} ${part.name}...`,
          success: `${part.name} is now ${nextStatus}.`,
          error: (err) => err.response?.data?.message || 'Update failed.',
        }
      );
      fetchParts(searchTerm);
    } catch {}
  };

  // Rejected stock and deactivated items are kept out of the main "what can I issue"
  // table and shown in their own section below instead, so they don't clutter the
  // day-to-day view while staying visible on the same page for traceability.
  const activeParts = parts.filter(p => p.status !== 'REJECTED' && p.itemStatus !== 'INACTIVE');
  const flaggedParts = parts.filter(p => p.status === 'REJECTED' || p.itemStatus === 'INACTIVE');

  const renderPartRow = (part: Part) => {
    const expiry = getExpiryDisplay(part.expiryDate);
    const isExpired = part.expiryDate && new Date(part.expiryDate) < new Date();

    return (
      <tr key={part.id} className={`hover:bg-slate-50/50 transition group ${isExpired ? 'bg-red-50/30' : ''}`}>

        {/* Material Details */}
        <td className="p-6">
          <div className="font-black text-slate-900">{part.name}</div>
          <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">
            SKU: {part.serialNumber}
          </div>
          {part.balances && part.balances.filter(b => b.status === 'APPROVED' && b.quantity > 0).length > 0 ? (
            <div className="mt-1.5 flex flex-wrap gap-1">
              {part.balances
                .filter(b => b.status === 'APPROVED' && b.quantity > 0)
                .map((b, idx) => (
                  <span key={`${b.batchNumber}-${idx}`}
                    className="inline-block bg-blue-50 text-blue-600 px-2 py-0.5 rounded text-[10px] font-mono font-bold border border-blue-100">
                    {b.batchNumber} · {b.quantity}
                    {b.poNumber && <span className="ml-1 text-slate-400 font-normal">· via {b.poNumber}</span>}
                  </span>
                ))}
            </div>
          ) : (
            <span className="mt-1.5 inline-block text-[10px] text-red-400 italic font-bold">
              No Batch — Add stock to assign
            </span>
          )}
        </td>

        {/* Classification */}
        <td className="p-6">
          <div className="text-xs text-slate-500 font-medium">{part.partType || '—'}</div>
          {part.criticality && (
            <span className={`mt-1 inline-block px-2 py-0.5 rounded text-[10px] font-black ${criticalityStyle[part.criticality] || 'text-slate-500 bg-slate-50'}`}>
              {part.criticality}
            </span>
          )}
          <div className="mt-1.5 flex flex-wrap gap-1">
            {part.qcRequired && (
              <span className="px-1.5 py-0.5 rounded text-[9px] font-black bg-purple-50 text-purple-600 border border-purple-100">QC</span>
            )}
            {part.serialRequired && (
              <span className="px-1.5 py-0.5 rounded text-[9px] font-black bg-sky-50 text-sky-600 border border-sky-100">SERIAL</span>
            )}
            {part.itemStatus && part.itemStatus !== 'ACTIVE' && (
              <span className="px-1.5 py-0.5 rounded text-[9px] font-black bg-slate-100 text-slate-500 border border-slate-200">{part.itemStatus}</span>
            )}
          </div>
        </td>

        {/* Stock Status */}
        <td className="p-6">
          <span className={`px-3 py-1 rounded-full text-[10px] font-black border ${statusStyle[part.status || 'EMPTY'] || statusStyle['EMPTY']}`}>
            {part.status || 'EMPTY'}
          </span>
        </td>

        {/* Stock Balance + Expiry */}
        <td className="p-6">
          <div className={`font-black text-lg ${
            (part.quantity ?? 0) <= (part.reorderLevel ?? 0) && part.quantity !== null
              ? 'text-red-500' : 'text-slate-900'
          }`}>
            {part.quantity ?? 0} <span className="text-xs font-bold text-slate-400">UNITS</span>
          </div>

          {part.reorderLevel !== null && (
            <div className="text-[10px] text-slate-400 font-bold">
              Reorder at: {part.reorderLevel}
            </div>
          )}

          {part.location && (
            <div className="text-[10px] text-slate-400 font-bold flex items-center gap-1 mt-0.5">
              <MapPin size={10} /> {part.location}
            </div>
          )}

          {/* FIX: Expiry badge — shows for any part that has an expiryDate set */}
          {/* Color: red=expired, orange=≤30d, amber=≤90d, slate=safe */}
          {expiry && (
            <div className={`mt-1.5 inline-flex items-center gap-1 px-2 py-0.5 rounded text-[10px] font-black border ${expiry.style}`}>
              <CalendarX2 size={10} />
              {expiry.label}
            </div>
          )}
        </td>

        {/* Operations */}
        <td className="p-6">
          <div className="flex justify-center items-center gap-1">
            <Link to={`/inventory/${part.id}/transactions`}
              className="p-2 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-xl transition"
              title="Ledger History">
              <Clock size={18} />
            </Link>
            <button
              onClick={() => handleViewPastIssues(part)}
              className="p-2 text-slate-400 hover:text-purple-600 hover:bg-purple-50 rounded-xl transition"
              title="Past Issue Receipts"
            >
              <History size={18} />
            </button>
            <button
              onClick={() => handleIssue(part)}
              className="p-2 text-slate-400 hover:text-orange-600 hover:bg-orange-50 rounded-xl transition"
              title="Issue Material (User Requirement)"
              disabled={part.status !== 'APPROVED'}
            >
              <MinusCircle size={18} className={part.status !== 'APPROVED' ? 'opacity-30' : ''} />
            </button>
            <button
              onClick={() => handleOpenAdjust(part)}
              className="p-2 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-xl transition"
              title="Stock Adjustment"
            >
              <SlidersHorizontal size={18} />
            </button>
            <button
              onClick={() => handleOpenEdit(part)}
              className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-xl transition"
              title="Edit Material Master"
            >
              <Edit3 size={18} />
            </button>
            <button
              onClick={() => handleToggleActive(part)}
              className={`p-2 rounded-xl transition ${part.itemStatus === 'INACTIVE' ? 'text-slate-400 hover:text-emerald-600 hover:bg-emerald-50' : 'text-slate-400 hover:text-red-600 hover:bg-red-50'}`}
              title={part.itemStatus === 'INACTIVE' ? 'Reactivate Item' : 'Deactivate Item'}
            >
              {part.itemStatus === 'INACTIVE' ? <RotateCcw size={18} /> : <Ban size={18} />}
            </button>
          </div>
        </td>

      </tr>
    );
  };

  return (
    <div className="space-y-6">
      <AddPartModal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} onSuccess={() => fetchParts(searchTerm)} />
      <EditPartModal isOpen={isEditModalOpen} part={selectedPart} onClose={() => setIsEditModalOpen(false)} onSuccess={() => fetchParts(searchTerm)} />
      <StockAdjustmentModal isOpen={isAdjustModalOpen} part={selectedPart} onClose={() => setIsAdjustModalOpen(false)} onSuccess={() => fetchParts(searchTerm)} />

      {/* Toolbar */}
      <div className="flex flex-col md:flex-row justify-between items-center gap-4 bg-white p-6 rounded-3xl shadow-sm border border-slate-100">
        <div className="relative w-full md:w-96">
          <Search className="absolute left-4 top-3 text-slate-400 w-5 h-5" />
          <input
            type="text"
            placeholder="Search material records..."
            className="w-full pl-12 pr-4 py-3 bg-slate-50 border-none rounded-2xl focus:ring-2 focus:ring-blue-500 font-medium outline-none"
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
          />
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setShowRegister(true)}
            className="bg-white border border-slate-200 text-slate-600 px-5 py-3 rounded-2xl hover:bg-slate-50 transition font-bold flex items-center gap-2"
          >
            <Printer size={18} /> Print Item Register
          </button>
          <button
            onClick={() => setIsModalOpen(true)}
            className="bg-blue-600 text-white px-8 py-3 rounded-2xl hover:bg-blue-700 transition font-bold flex items-center gap-2 shadow-lg shadow-blue-200"
          >
            <PlusCircle size={20} /> Initialize Material
          </button>
        </div>
      </div>

      {/* Active / usable inventory */}
      <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50 text-slate-400 text-[10px] uppercase tracking-[0.2em] font-black">
                <th className="p-6">Material Details & Batch</th>
                <th className="p-6">Classification</th>
                <th className="p-6">Stock Status</th>
                <th className="p-6">Stock Balance</th>
                <th className="p-6 text-center">ERP Operations</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50">
              {loading ? (
                <tr><td colSpan={5} className="p-20 text-center">
                  <Loader2 className="animate-spin text-blue-500 mx-auto" size={32} />
                </td></tr>
              ) : activeParts.length === 0 ? (
                <tr><td colSpan={5} className="p-20 text-center text-slate-300">
                  <div className="flex flex-col items-center gap-2">
                    <Layers size={48} className="opacity-20 mb-2" />
                    <p className="font-bold uppercase tracking-widest text-xs">
                      {parts.length === 0 ? 'No materials found' : 'All matching items are rejected or deactivated — see below'}
                    </p>
                  </div>
                </td></tr>
              ) : activeParts.map(renderPartRow)}
            </tbody>
          </table>
        </div>
      </div>

      {/* Rejected / Deactivated — kept out of the main table, still on the same page */}
      {!loading && flaggedParts.length > 0 && (
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden opacity-90">
          <div className="px-6 pt-5 pb-1 flex items-center gap-2">
            <Ban size={16} className="text-slate-400" />
            <h3 className="text-xs font-black uppercase tracking-widest text-slate-500">
              Rejected &amp; Deactivated ({flaggedParts.length})
            </h3>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-slate-50 text-slate-400 text-[10px] uppercase tracking-[0.2em] font-black">
                  <th className="p-6">Material Details & Batch</th>
                  <th className="p-6">Classification</th>
                  <th className="p-6">Stock Status</th>
                  <th className="p-6">Stock Balance</th>
                  <th className="p-6 text-center">ERP Operations</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {flaggedParts.map(renderPartRow)}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {showRegister && (
        <ReceiptModal
          isOpen={showRegister}
          onClose={() => setShowRegister(false)}
          title="Item Register"
          documentNumber={`${parts.length} items`}
          plain
        >
          <ItemRegisterDocument parts={parts} org={orgProfile} />
        </ReceiptModal>
      )}

      {issueReceipt && (
        <ReceiptModal
          isOpen={!!issueReceipt}
          onClose={() => setIssueReceipt(null)}
          title="Material Issue Receipt"
          documentNumber={issueReceipt.issueNumber}
        >
          <MaterialIssueReceiptDocument record={issueReceipt} org={orgProfile} />
        </ReceiptModal>
      )}

      <IssueMaterialPickerModal
        isOpen={!!issuePickerPart}
        onClose={() => setIssuePickerPart(null)}
        part={issuePickerPart}
        suggestedQuantity={issuePickerPart?.quantity || 0}
        issuedBy={user?.username}
        onIssued={handleIssued}
      />

      {pastIssuesPart && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg border border-slate-200 flex flex-col max-h-[80vh]">
            <div className="bg-slate-50 px-6 py-4 border-b flex justify-between items-center shrink-0">
              <div>
                <h2 className="text-lg font-black text-slate-900">Past Issues</h2>
                <p className="text-xs font-bold text-slate-400">{pastIssuesPart.name}</p>
              </div>
              <button onClick={() => setPastIssuesPart(null)} className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors">
                <X size={20} />
              </button>
            </div>
            <div className="p-4 overflow-y-auto flex-1 space-y-1.5">
              {pastIssuesLoading ? (
                <div className="py-10 flex justify-center"><Loader2 className="animate-spin text-blue-500" size={24} /></div>
              ) : pastIssues.length === 0 ? (
                <p className="text-center py-10 text-xs font-bold uppercase tracking-widest text-slate-300">No issues recorded for this part</p>
              ) : (
                pastIssues.map(record => (
                  <button
                    key={record.id}
                    onClick={() => { setIssueReceipt(record); setPastIssuesPart(null); }}
                    className="w-full flex items-center justify-between gap-2 bg-slate-50 hover:bg-slate-100 border border-slate-200 rounded-xl px-3 py-2 transition text-left"
                  >
                    <div>
                      <span className="font-mono text-xs font-bold text-slate-700 block">{record.issueNumber}</span>
                      <span className="text-[10px] text-slate-400">{record.woNumber ? `WO ${record.woNumber}` : record.issuedTo || 'Ad-hoc issue'}</span>
                    </div>
                    <span className="text-[11px] text-slate-500 text-right">{record.totalQuantityIssued} units<br />{new Date(record.issuedAt).toLocaleDateString()}</span>
                  </button>
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default InventoryList;
