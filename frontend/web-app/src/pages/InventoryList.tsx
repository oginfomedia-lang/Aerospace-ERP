import React, { useEffect, useRef, useState } from 'react';
import api from '../api/axios';
import procurementApi from '../api/procurementAxios';
import workOrderApi from '../api/workOrderAxios';
import productionApi from '../api/productionAxios';
import { Link, useLocation } from 'react-router-dom';
import {
  Edit3, Search, Loader2, Clock, MinusCircle, PlusCircle,
  Layers, MapPin, SlidersHorizontal, CalendarX2, Ban, RotateCcw, Printer, History, X, Wrench, Factory
} from 'lucide-react';
import { Part, ApiResponse, MaterialIssueRecord, Location } from '../types/inventory';
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

interface WorkOrderNeedingPart {
  workOrderId: number;
  woNumber: string;
  lineItemId: number;
  taskDescription: string;
  quantityRequired: number;
  quantityIssued: number;
}

interface ProductionOrderNeedingPart {
  productionOrderId: number;
  poNumber: string;
  materialId: number;
  partName: string;
  quantityRequired: number;
  quantityConsumed: number;
}

type IssueContext =
  | { type: 'wo'; workOrderId: number; woNumber: string; lineItemId: number }
  | { type: 'po'; productionOrderId: number; poNumber: string; materialId: number; quantityRequired: number; quantityConsumed: number }
  | { type: 'adhoc' };

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
  const routerLocation = useLocation();
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

  // Location filter (A2)
  const [locations, setLocations] = useState<Location[]>([]);
  const [locationFilter, setLocationFilter] = useState('');

  // Dashboard click-through (C)
  const [lowStockOnly, setLowStockOnly] = useState(false);
  const flaggedSectionRef = useRef<HTMLDivElement>(null);

  // "What is this issuance for?" step (B2)
  const [issueTargetPart, setIssueTargetPart] = useState<Part | null>(null);
  const [issueOptionsLoading, setIssueOptionsLoading] = useState(false);
  const [woOptions, setWoOptions] = useState<WorkOrderNeedingPart[]>([]);
  const [poOptions, setPoOptions] = useState<ProductionOrderNeedingPart[]>([]);
  const [issueContext, setIssueContext] = useState<IssueContext | null>(null);

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
    api.get<Location[]>('/inventory/locations', { params: { activeOnly: true } })
      .then(res => setLocations(res.data)).catch(() => {});
  }, []);

  // Dashboard stat cards navigate here with { filterStatus: 'REJECTED' } or
  // { filterLowStock: true } — apply whichever was requested, once.
  useEffect(() => {
    const state = routerLocation.state as { filterStatus?: string; filterLowStock?: boolean } | undefined;
    if (state?.filterStatus === 'REJECTED') {
      setTimeout(() => flaggedSectionRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' }), 300);
    }
    if (state?.filterLowStock) {
      setLowStockOnly(true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
    setIssueTargetPart(part);
    setIssueOptionsLoading(true);
    Promise.all([
      workOrderApi.get<ApiResponse<WorkOrderNeedingPart[]>>(`/work-orders/needing-part/${part.id}`).then(r => r.data.data).catch(() => []),
      productionApi.get<ApiResponse<ProductionOrderNeedingPart[]>>(`/production/needing-part/${part.id}`).then(r => r.data.data).catch(() => []),
    ]).then(([wo, po]) => {
      setWoOptions(wo);
      setPoOptions(po);
    }).finally(() => setIssueOptionsLoading(false));
  };

  // "What is this for?" step resolved — open the real picker with the right context.
  const handleSelectIssueContext = (ctx: IssueContext) => {
    setIssueContext(ctx);
    setIssuePickerPart(issueTargetPart);
    setIssueTargetPart(null);
  };

  const handleIssued = (response: Part) => {
    const issueRecordId = response.materialIssueRecordId;
    if (issueRecordId) {
      api.get(`/inventory/material-issue-records/${issueRecordId}`)
        .then(r => {
          setIssueReceipt(r.data.data);
          // Standalone Production Order material — no Kafka path exists for this (only
          // WO-linked issuances sync automatically), so record the real consumed qty on
          // the order directly, same follow-up ProductionOrderDetailModal.tsx already does.
          if (issueContext?.type === 'po') {
            const record = r.data.data;
            const totalIssued = record.totalQuantityIssued || 0;
            productionApi.post(
              `/production/${issueContext.productionOrderId}/materials/${issueContext.materialId}/consume`,
              null,
              { params: { consumed: (issueContext.quantityConsumed || 0) + totalIssued, wasted: 0 } }
            ).catch(() => toast.error('Issued from inventory, but failed to record consumption on the Production Order.'));
          }
        }).catch(() => {});
    }
    setIssueContext(null);
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
  let activeParts = parts.filter(p => p.status !== 'REJECTED' && p.itemStatus !== 'INACTIVE');
  const flaggedParts = parts.filter(p => p.status === 'REJECTED' || p.itemStatus === 'INACTIVE');

  // A part can have balances across multiple warehouse locations — filtering narrows to
  // parts that have at least one matching balance; the batch badges themselves are
  // further narrowed inside renderPartRow so only that location's batches show.
  if (locationFilter) {
    activeParts = activeParts.filter(p => (p.balances || []).some(b => b.location === locationFilter));
  }
  if (lowStockOnly) {
    activeParts = activeParts.filter(p => p.quantity !== null && p.reorderLevel !== null && (p.quantity ?? 0) <= (p.reorderLevel ?? 0));
  }

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
          {part.balances && part.balances.filter(b => b.status === 'APPROVED' && b.quantity > 0 && (!locationFilter || b.location === locationFilter)).length > 0 ? (
            <div className="mt-1.5 flex flex-wrap gap-1">
              {part.balances
                .filter(b => b.status === 'APPROVED' && b.quantity > 0 && (!locationFilter || b.location === locationFilter))
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
          <select
            value={locationFilter}
            onChange={e => setLocationFilter(e.target.value)}
            className="bg-slate-50 border border-slate-200 rounded-2xl px-4 py-3 text-sm font-bold text-slate-600 outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">All Locations</option>
            {locations.map(l => (
              <option key={l.id} value={l.code}>{l.code} — {l.name}</option>
            ))}
          </select>
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

      {lowStockOnly && (
        <div className="flex items-center justify-between bg-red-50 border border-red-200 rounded-2xl px-5 py-3">
          <span className="text-xs font-black text-red-700 uppercase tracking-widest">
            Showing Low Stock items only
          </span>
          <button onClick={() => setLowStockOnly(false)} className="text-red-600 hover:text-red-800 font-bold text-xs flex items-center gap-1">
            <X size={14} /> Clear
          </button>
        </div>
      )}

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
        <div ref={flaggedSectionRef} className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden opacity-90">
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
        onClose={() => { setIssuePickerPart(null); setIssueContext(null); }}
        part={issuePickerPart}
        workOrderId={issueContext?.type === 'wo' ? issueContext.workOrderId : undefined}
        woNumber={issueContext?.type === 'wo' ? issueContext.woNumber : undefined}
        lineItemId={issueContext?.type === 'wo' ? issueContext.lineItemId : undefined}
        suggestedQuantity={issuePickerPart?.quantity || 0}
        issuedBy={user?.username}
        onIssued={handleIssued}
      />

      {/* "What is this issuance for?" — links an ad-hoc Inventory issue to a real Work
          Order or Production Order so that record's quantities update accordingly,
          instead of only ever being a free-text Purpose with no downstream link. */}
      {issueTargetPart && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg border border-slate-200 flex flex-col max-h-[80vh]">
            <div className="bg-slate-50 px-6 py-4 border-b flex justify-between items-center shrink-0">
              <div>
                <h2 className="text-lg font-black text-slate-900">What is this issuance for?</h2>
                <p className="text-xs font-bold text-slate-400">{issueTargetPart.name}</p>
              </div>
              <button onClick={() => setIssueTargetPart(null)} className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors">
                <X size={20} />
              </button>
            </div>
            <div className="p-4 overflow-y-auto flex-1 space-y-1.5">
              {issueOptionsLoading ? (
                <div className="py-10 flex justify-center"><Loader2 className="animate-spin text-blue-500" size={24} /></div>
              ) : (
                <>
                  {woOptions.map(wo => (
                    <button
                      key={`wo-${wo.lineItemId}`}
                      onClick={() => handleSelectIssueContext({ type: 'wo', workOrderId: wo.workOrderId, woNumber: wo.woNumber, lineItemId: wo.lineItemId })}
                      className="w-full flex items-center gap-3 bg-blue-50 hover:bg-blue-100 border border-blue-200 rounded-xl px-3 py-2.5 transition text-left"
                    >
                      <Wrench size={16} className="text-blue-500 shrink-0" />
                      <div className="flex-1 min-w-0">
                        <span className="font-mono text-xs font-bold text-slate-700 block">WO {wo.woNumber}</span>
                        <span className="text-[10px] text-slate-500 truncate block">{wo.taskDescription}</span>
                      </div>
                      <span className="text-[11px] text-slate-500 text-right shrink-0">Needs {wo.quantityRequired - (wo.quantityIssued || 0)}</span>
                    </button>
                  ))}
                  {poOptions.map(po => (
                    <button
                      key={`po-${po.materialId}`}
                      onClick={() => handleSelectIssueContext({ type: 'po', productionOrderId: po.productionOrderId, poNumber: po.poNumber, materialId: po.materialId, quantityRequired: po.quantityRequired, quantityConsumed: po.quantityConsumed })}
                      className="w-full flex items-center gap-3 bg-indigo-50 hover:bg-indigo-100 border border-indigo-200 rounded-xl px-3 py-2.5 transition text-left"
                    >
                      <Factory size={16} className="text-indigo-500 shrink-0" />
                      <div className="flex-1 min-w-0">
                        <span className="font-mono text-xs font-bold text-slate-700 block">PO {po.poNumber}</span>
                        <span className="text-[10px] text-slate-500 truncate block">{po.partName}</span>
                      </div>
                      <span className="text-[11px] text-slate-500 text-right shrink-0">Needs {po.quantityRequired - (po.quantityConsumed || 0)}</span>
                    </button>
                  ))}
                  {woOptions.length === 0 && poOptions.length === 0 && (
                    <p className="text-center py-4 text-xs font-bold uppercase tracking-widest text-slate-300">
                      No open Work Orders or Production Orders need this part right now
                    </p>
                  )}
                  <button
                    onClick={() => handleSelectIssueContext({ type: 'adhoc' })}
                    className="w-full flex items-center gap-3 bg-slate-50 hover:bg-slate-100 border border-slate-200 rounded-xl px-3 py-2.5 transition text-left mt-2"
                  >
                    <MinusCircle size={16} className="text-slate-400 shrink-0" />
                    <span className="text-xs font-bold text-slate-600">Ad-hoc (no Work Order or Production Order)</span>
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}

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
