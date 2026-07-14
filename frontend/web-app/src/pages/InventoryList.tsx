import React, { useEffect, useState } from 'react';
import api from '../api/axios';
import procurementApi from '../api/procurementAxios';
import { Link } from 'react-router-dom';
import {
  Edit3, Search, Loader2, Clock, MinusCircle, PlusCircle,
  Layers, MapPin, SlidersHorizontal, CalendarX2, Ban, RotateCcw, Printer
} from 'lucide-react';
import { Part, ApiResponse } from '../types/inventory';
import { OrganizationProfile } from '../types/procurement';
import { AddPartModal } from '../components/AddPartModal';
import { EditPartModal } from '../components/EditPartModal';
import { StockAdjustmentModal } from '../components/StockAdjustmentModal';
import { ReceiptModal } from '../components/ReceiptModal';
import { ItemRegisterDocument } from '../components/ItemRegisterDocument';
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
  const [parts, setParts] = useState<Part[]>([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isAdjustModalOpen, setIsAdjustModalOpen] = useState(false);
  const [selectedPart, setSelectedPart] = useState<Part | null>(null);
  const [showRegister, setShowRegister] = useState(false);
  const [orgProfile, setOrgProfile] = useState<OrganizationProfile | null>(null);

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

  const handleIssue = async (part: Part) => {
    if (!part.quantity || part.quantity <= 0) {
      toast.error('No approved stock available to issue for this part.');
      return;
    }

    // Warn if any APPROVED batch is expired — FIFO may draw from it
    const expiredBatch = (part.balances || []).find(
      b => b.status === 'APPROVED' && b.expiryDate && new Date(b.expiryDate) < new Date()
    );
    if (expiredBatch) {
      const proceed = window.confirm(
        `⚠️ WARNING: Approved batch ${expiredBatch.batchNumber} is EXPIRED.\nAre you sure you want to issue this stock to production?`
      );
      if (!proceed) return;
    }

    const qtyStr = window.prompt(
      `Issue material based on user requirement.\nStep 5: Material will issue and stock will update (oldest batch first — FIFO).\n\nAvailable (Approved, all batches): ${part.quantity} units\n\nEnter quantity to issue:`
    );
    if (!qtyStr || isNaN(Number(qtyStr)) || Number(qtyStr) <= 0) return;

    // No batchNumber passed — backend auto-consumes the oldest APPROVED batch(es)
    // first (FIFO), splitting across as many as needed to cover the quantity.
    const params = new URLSearchParams({ qty: qtyStr });

    try {
      await toast.promise(
        api.post(`/inventory/${part.id}/issue-to-production?${params}`),
        {
          loading: `Issuing ${qtyStr} units (FIFO — oldest batch first)...`,
          success: (res) => `Material issued from batch(es) ${res.data.data.batchNumber}. Stock updated.`,
          error: (err) => err.response?.data?.message || 'Issue failed — check batch approval status.',
        }
      );
      fetchParts(searchTerm);
    } catch {}
  };

  const handleOpenEdit = (part: Part) => { setSelectedPart(part); setIsEditModalOpen(true); };
  const handleOpenAdjust = (part: Part) => { setSelectedPart(part); setIsAdjustModalOpen(true); };

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

      {/* Table */}
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
              ) : parts.length === 0 ? (
                <tr><td colSpan={5} className="p-20 text-center text-slate-300">
                  <div className="flex flex-col items-center gap-2">
                    <Layers size={48} className="opacity-20 mb-2" />
                    <p className="font-bold uppercase tracking-widest text-xs">No materials found</p>
                  </div>
                </td></tr>
              ) : parts.map(part => {
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
                      {part.batchNumber ? (
                        <span className="mt-1.5 inline-block bg-blue-50 text-blue-600 px-2 py-0.5 rounded text-[10px] font-mono font-bold border border-blue-100">
                          BATCH: {part.batchNumber}
                        </span>
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
              })}
            </tbody>
          </table>
        </div>
      </div>

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
    </div>
  );
};

export default InventoryList;
