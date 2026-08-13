import React, { useEffect, useState } from 'react';
import salesApi from '../api/salesAxios';
import inventoryApi from '../api/axios';
import toast from 'react-hot-toast';
import { RotateCcw, Loader2, CheckCircle2, PackageCheck, Check, X, Wrench, Repeat, Receipt } from 'lucide-react';
import { ApiResponse, Location } from '../types/inventory';
import { ReturnAuthorization } from '../types/sales';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole, CAN_FULFILL_ORDERS, CAN_INSPECT_RETURNS } from '../utils/permissions';

const dispositionStyle = (d: string) => {
  if (d === 'ACCEPTED') return 'bg-emerald-50 text-emerald-700 border-emerald-200';
  if (d === 'REJECTED') return 'bg-red-50 text-red-700 border-red-200';
  if (d === 'PENDING_INSPECTION') return 'bg-amber-50 text-amber-700 border-amber-200';
  return 'bg-slate-100 text-slate-500 border-slate-200'; // REPAIR, REPLACE, CREDIT_RECOMMENDED
};

const dispositionOptions = [
  { value: 'ACCEPTED', label: 'Accept', icon: <Check size={12} /> },
  { value: 'REJECTED', label: 'Reject', icon: <X size={12} /> },
  { value: 'REPAIR', label: 'Repair', icon: <Wrench size={12} /> },
  { value: 'REPLACE', label: 'Replace', icon: <Repeat size={12} /> },
  { value: 'CREDIT_RECOMMENDED', label: 'Credit', icon: <Receipt size={12} /> },
];

const ReturnsPage = () => {
  const { user } = useAuth();
  const canReceive = hasAnyRole(user?.roles, CAN_FULFILL_ORDERS);
  const canInspect = hasAnyRole(user?.roles, CAN_INSPECT_RETURNS);

  const [returns, setReturns] = useState<ReturnAuthorization[]>([]);
  const [locations, setLocations] = useState<Location[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<string>('');
  const [locationByRma, setLocationByRma] = useState<Record<number, string>>({});
  const [notesByRma, setNotesByRma] = useState<Record<number, string>>({});
  const [acting, setActing] = useState<number | null>(null);

  useEffect(() => {
    inventoryApi.get<Location[]>('/inventory/locations', { params: { activeOnly: true } })
      .then(res => setLocations(res.data)).catch(() => {});
  }, []);

  const fetchReturns = async () => {
    setLoading(true);
    try {
      const res = await salesApi.get<ApiResponse<ReturnAuthorization[]>>('/sales/rma', { params: filter ? { disposition: filter } : {} });
      setReturns(res.data.data);
    } catch {
      toast.error('Failed to fetch returns.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchReturns(); }, [filter]); // eslint-disable-line react-hooks/exhaustive-deps

  const receive = async (rma: ReturnAuthorization) => {
    const location = locationByRma[rma.id];
    if (!location || !location.trim()) { toast.error('Select a warehouse location first.'); return; }
    setActing(rma.id);
    try {
      await toast.promise(
        salesApi.post(`/sales/rma/${rma.id}/receive`, null, { params: { location } }),
        { loading: 'Receiving return...', success: 'Return received into inventory.', error: (err) => err.response?.data?.message || 'Failed to receive return.' }
      );
      await fetchReturns();
    } catch {
      // toast.promise handles display
    } finally {
      setActing(null);
    }
  };

  const inspect = async (rma: ReturnAuthorization, disposition: string) => {
    setActing(rma.id);
    try {
      await toast.promise(
        salesApi.post(`/sales/rma/${rma.id}/inspect`, { disposition, notes: notesByRma[rma.id] || '' }),
        { loading: 'Recording inspection...', success: 'Return inspected.', error: (err) => err.response?.data?.message || 'Failed to inspect return.' }
      );
      await fetchReturns();
    } catch {
      // toast.promise handles display
    } finally {
      setActing(null);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white">
            <RotateCcw size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Customer Returns</h1>
            <p className="text-slate-500 text-sm font-medium">
              Authorize → receive → inspect. Authorizing a new return happens from a Sales Order's Returns tab.
            </p>
          </div>
        </div>
        <select value={filter} onChange={e => setFilter(e.target.value)}
          className="px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm font-bold">
          <option value="">All statuses</option>
          <option value="PENDING_INSPECTION">Pending Inspection</option>
          <option value="ACCEPTED">Accepted</option>
          <option value="REJECTED">Rejected</option>
          <option value="REPAIR">Repair</option>
          <option value="REPLACE">Replace</option>
          <option value="CREDIT_RECOMMENDED">Credit Recommended</option>
        </select>
      </div>

      <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
        {loading ? (
          <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
        ) : returns.length === 0 ? (
          <div className="p-16 text-center text-slate-300">
            <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
            <p className="font-bold uppercase tracking-widest text-xs">No returns yet</p>
          </div>
        ) : (
          <div className="divide-y divide-slate-50">
            {returns.map(r => (
              <div key={r.id} className="p-5">
                <div className="flex items-center justify-between">
                  <div>
                    <div className="font-mono font-black text-slate-900">{r.rmaNumber}</div>
                    <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">
                      Order {r.orderNumber} · {r.partNumber} × {r.quantity}
                    </div>
                  </div>
                  <span className={`text-[10px] font-black px-2.5 py-1 rounded-full border ${dispositionStyle(r.disposition)}`}>
                    {r.disposition.replace('_', ' ')}
                  </span>
                </div>
                {r.reasonDescription && <p className="text-xs text-slate-500 font-medium mt-2 italic">{r.reasonDescription}</p>}

                {r.disposition === 'PENDING_INSPECTION' && !r.receivedBatchNumber && canReceive && (
                  <div className="flex items-center gap-2 mt-3">
                    <select value={locationByRma[r.id] || ''} onChange={e => setLocationByRma({ ...locationByRma, [r.id]: e.target.value })}
                      className="flex-1 px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs">
                      <option value="" disabled>Select a location...</option>
                      {locations.map(l => (
                        <option key={l.id} value={l.code}>{l.code} — {l.name}</option>
                      ))}
                    </select>
                    <button onClick={() => receive(r)} disabled={acting === r.id}
                      className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-black bg-slate-900 text-white hover:bg-slate-800 disabled:opacity-60">
                      <PackageCheck size={13} /> Receive
                    </button>
                  </div>
                )}

                {r.disposition === 'PENDING_INSPECTION' && r.receivedBatchNumber && canInspect && (
                  <div className="mt-3 space-y-2">
                    <div className="text-[10px] text-slate-400 font-bold">Received into batch {r.receivedBatchNumber}</div>
                    <input value={notesByRma[r.id] || ''} onChange={e => setNotesByRma({ ...notesByRma, [r.id]: e.target.value })}
                      placeholder="Inspection notes" className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs" />
                    <div className="flex flex-wrap gap-2">
                      {dispositionOptions.map(opt => (
                        <button key={opt.value} onClick={() => inspect(r, opt.value)} disabled={acting === r.id}
                          className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-[11px] font-black bg-slate-100 text-slate-600 hover:bg-slate-200 disabled:opacity-60">
                          {opt.icon} {opt.label}
                        </button>
                      ))}
                    </div>
                  </div>
                )}

                {r.disposition === 'PENDING_INSPECTION' && r.receivedBatchNumber && !canInspect && (
                  <div className="text-[10px] text-slate-400 font-bold mt-2">Received into batch {r.receivedBatchNumber} — awaiting QA inspection</div>
                )}

                {r.inspectedBy && (
                  <div className="text-[10px] text-slate-400 font-bold mt-2">
                    Inspected by {r.inspectedBy} on {r.inspectedAt ? new Date(r.inspectedAt).toLocaleString() : ''}
                    {r.inspectionNotes ? ` — ${r.inspectionNotes}` : ''}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default ReturnsPage;
