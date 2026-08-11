import React, { useEffect, useState } from 'react';
import productionResourceApi from '../api/productionResourceAxios';
import toast from 'react-hot-toast';
import { Hammer, Loader2, ChevronRight, CheckCircle2, PlusCircle, Unlock, Trash2 } from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import { Tooling, ToolingRequest } from '../types/productionResource';
import { RegisterToolingModal } from '../components/RegisterToolingModal';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole } from '../utils/permissions';

const CAN_WRITE = ['PRODUCTION_SUPERVISOR', 'MANAGER', 'ADMIN'];

const statusStyle = (status: string) => {
  if (status === 'ACTIVE') return 'bg-emerald-50 text-emerald-700 border-emerald-200';
  if (status === 'WORN') return 'bg-amber-50 text-amber-700 border-amber-200';
  return 'bg-slate-100 text-slate-500 border-slate-200'; // RETIRED
};

const toolingTypes = ['CUTTING_TOOL', 'FIXTURE', 'JIG', 'MEASURING_EQUIPMENT', 'OTHER'];

const ToolingRegistryPage = () => {
  const { user } = useAuth();
  const canWrite = hasAnyRole(user?.roles, CAN_WRITE);
  const [tools, setTools] = useState<Tooling[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Tooling | null>(null);
  const [typeFilter, setTypeFilter] = useState('');
  const [showRegister, setShowRegister] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [form, setForm] = useState<ToolingRequest>({ name: '', toolingType: 'CUTTING_TOOL', location: '', status: 'ACTIVE' });

  const fetchTools = async () => {
    setLoading(true);
    try {
      const res = await productionResourceApi.get<ApiResponse<Tooling[]>>('/resources/tooling', {
        params: typeFilter ? { toolingType: typeFilter } : {},
      });
      setTools(res.data.data);
    } catch {
      toast.error('Failed to fetch tooling.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchTools(); }, [typeFilter]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSelect = (t: Tooling) => {
    setSelected(t);
    setForm({ name: t.name, toolingType: t.toolingType, location: t.location || '', status: t.status });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    setSubmitting(true);
    try {
      await toast.promise(
        productionResourceApi.put(`/resources/tooling/${selected.id}`, form),
        {
          loading: 'Updating tool...',
          success: `Tool ${selected.toolCode} updated.`,
          error: (err) => err.response?.data?.message || 'Failed to update tool.',
        }
      );
      await fetchTools();
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  const handleForceRelease = async (t: Tooling) => {
    try {
      await toast.promise(
        productionResourceApi.post(`/resources/tooling/${t.id}/force-release`),
        { loading: `Releasing ${t.toolCode}...`, success: `${t.toolCode} released`, error: 'Failed to release tool' }
      );
      await fetchTools();
    } catch {}
  };

  const handleDelete = async (t: Tooling) => {
    try {
      await toast.promise(
        productionResourceApi.delete(`/resources/tooling/${t.id}`),
        {
          loading: `Deleting ${t.toolCode}...`,
          success: `${t.toolCode} deleted`,
          error: (err) => err.response?.data?.message || 'Failed to delete tool',
        }
      );
      if (selected?.id === t.id) setSelected(null);
      await fetchTools();
    } catch {}
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white">
            <Hammer size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Tooling & Fixtures</h1>
            <p className="text-slate-500 text-sm font-medium">
              Cutting tools, fixtures, jigs & measuring equipment, for staffing routing template steps.
            </p>
          </div>
        </div>
        {canWrite && (
          <button onClick={() => setShowRegister(true)}
            className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-sm font-black rounded-xl hover:bg-blue-700 transition shadow-lg shadow-blue-200">
            <PlusCircle size={16} /> Register Tool
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* LEFT — Tooling list */}
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
            <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Tool Register</h2>
            <select value={typeFilter} onChange={e => setTypeFilter(e.target.value)}
              className="text-[10px] font-black px-2 py-1 rounded-full bg-slate-100 text-slate-600 border-0">
              <option value="">ALL TYPES</option>
              {toolingTypes.map(t => <option key={t} value={t}>{t.replace('_', ' ')}</option>)}
            </select>
          </div>

          {loading ? (
            <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
          ) : tools.length === 0 ? (
            <div className="p-16 text-center text-slate-300">
              <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
              <p className="font-bold uppercase tracking-widest text-xs">No tooling registered yet</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-50">
              {tools.map(t => (
                <button key={t.id} onClick={() => handleSelect(t)}
                  className={`w-full text-left p-5 hover:bg-slate-50 transition-all group ${
                    selected?.id === t.id ? 'bg-blue-50 border-l-4 border-blue-600' : ''
                  }`}>
                  <div className="flex justify-between items-start">
                    <div>
                      <div className="font-black text-slate-900 group-hover:text-blue-600 transition-colors">{t.name}</div>
                      <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">
                        {t.toolCode} · {t.toolingType.replace('_', ' ')}{t.location ? ` · ${t.location}` : ''}
                      </div>
                    </div>
                    <ChevronRight size={16} className="text-slate-300 group-hover:text-blue-500 transition-colors" />
                  </div>
                  <div className="flex flex-wrap gap-1 mt-2">
                    <span className={`text-[10px] font-black px-2.5 py-1 rounded-full border ${statusStyle(t.status)}`}>{t.status}</span>
                    {t.occupancy === 'IN_OPERATION' && (
                      <span className="text-[10px] font-black px-2.5 py-1 rounded-full border bg-orange-50 text-orange-700 border-orange-200">IN OPERATION</span>
                    )}
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* RIGHT — Edit tool */}
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center gap-2">
            <Hammer size={16} className="text-blue-500" />
            <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Edit Tool</h2>
          </div>

          {!selected ? (
            <div className="p-16 text-center text-slate-300">
              <Hammer size={48} className="mx-auto mb-3 opacity-20" />
              <p className="font-bold uppercase tracking-widest text-xs">Select a tool to edit</p>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="p-5 space-y-4">
              <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100">
                <div className="flex items-center justify-between">
                  <div className="font-black text-slate-900">{selected.toolCode}</div>
                  <div className="flex items-center gap-2">
                    {selected.occupancy === 'IN_OPERATION' && canWrite && (
                      <button type="button" onClick={() => handleForceRelease(selected)}
                        className="flex items-center gap-1 text-[10px] font-black px-2.5 py-1 rounded-full bg-orange-500 text-white hover:bg-orange-600">
                        <Unlock size={10} /> Force Release
                      </button>
                    )}
                    {canWrite && (
                      <button type="button" onClick={() => handleDelete(selected)}
                        className="flex items-center gap-1 text-[10px] font-black px-2.5 py-1 rounded-full bg-red-500 text-white hover:bg-red-600">
                        <Trash2 size={10} /> Delete
                      </button>
                    )}
                  </div>
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Name</label>
                <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Type</label>
                  <select value={form.toolingType} onChange={e => setForm({ ...form, toolingType: e.target.value })}
                    className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
                    {toolingTypes.map(t => <option key={t} value={t}>{t.replace('_', ' ')}</option>)}
                  </select>
                </div>
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Location</label>
                  <input value={form.location} onChange={e => setForm({ ...form, location: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                </div>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-2 block tracking-wider">
                  Status {selected.status === 'RETIRED' && <span className="text-slate-400 font-normal normal-case">(retired tools cannot be reactivated)</span>}
                </label>
                <div className="grid grid-cols-3 gap-2">
                  {['ACTIVE', 'WORN', 'RETIRED'].map(s => (
                    <button key={s} type="button" disabled={selected.status === 'RETIRED' && s !== 'RETIRED'}
                      onClick={() => setForm({ ...form, status: s })}
                      className={`py-2.5 rounded-xl font-black text-xs border-2 transition-all disabled:opacity-30 disabled:cursor-not-allowed ${
                        form.status === s ? 'bg-slate-900 text-white border-slate-900' : 'bg-white text-slate-500 border-slate-200 hover:border-slate-400'
                      }`}>
                      {s}
                    </button>
                  ))}
                </div>
              </div>

              {canWrite && (
                <button type="submit" disabled={submitting}
                  className="w-full py-3 rounded-xl font-black text-white bg-blue-600 hover:bg-blue-700 transition-all active:scale-95 flex items-center justify-center gap-2 shadow-lg disabled:opacity-60">
                  {submitting ? <Loader2 size={16} className="animate-spin" /> : <CheckCircle2 size={16} />}
                  Save Changes
                </button>
              )}
            </form>
          )}
        </div>
      </div>

      <RegisterToolingModal isOpen={showRegister} onClose={() => setShowRegister(false)}
        onSuccess={() => { setShowRegister(false); fetchTools(); }} />
    </div>
  );
};

export default ToolingRegistryPage;
