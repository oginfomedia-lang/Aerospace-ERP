import React, { useEffect, useState } from 'react';
import { Route, RefreshCw, PlusCircle, Edit3, Trash2, Search, History, X, Eye } from 'lucide-react';
import toast from 'react-hot-toast';
import productionApi from '../api/productionAxios';
import { ApiResponse, RoutingTemplate } from '../types/production';
import { RoutingTemplateModal } from '../components/RoutingTemplateModal';

const RoutingTemplates = () => {
  const [templates, setTemplates] = useState<RoutingTemplate[]>([]);
  const [loading, setLoading] = useState(false);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<RoutingTemplate | null>(null);
  const [historyFor, setHistoryFor] = useState<RoutingTemplate | null>(null);
  const [history, setHistory] = useState<RoutingTemplate[]>([]);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [viewingRevision, setViewingRevision] = useState<RoutingTemplate | null>(null);

  const openHistory = async (t: RoutingTemplate) => {
    setHistoryFor(t);
    setLoadingHistory(true);
    try {
      const res = await productionApi.get<ApiResponse<RoutingTemplate[]>>('/production/routing-templates/history', {
        params: { partNumber: t.partNumber },
      });
      setHistory(res.data.data);
    } catch {
      toast.error('Failed to load revision history.');
    } finally {
      setLoadingHistory(false);
    }
  };

  const fetchData = async () => {
    setLoading(true);
    try {
      const res = await productionApi.get<ApiResponse<RoutingTemplate[]>>('/production/routing-templates');
      setTemplates(res.data.data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, []);

  const handleDelete = async (t: RoutingTemplate) => {
    if (!window.confirm(`Delete routing template "${t.name}" (${t.partNumber})? Production orders already using this routing keep their existing stage history.`)) return;
    try {
      await productionApi.delete(`/production/routing-templates/${t.id}`);
      toast.success('Routing template deleted.');
      fetchData();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to delete.');
    }
  };

  const filtered = templates.filter(t =>
    t.partNumber.toLowerCase().includes(search.toLowerCase()) ||
    t.name.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Routing Templates</h1>
          <p className="text-slate-500 text-sm font-medium mt-1">
            The ordered manufacturing operations (cutting, drilling, ...) each part follows on the shop floor.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={fetchData} className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-xl transition-all">
            <RefreshCw size={20} className={loading ? 'animate-spin' : ''} />
          </button>
          <button onClick={() => { setEditing(null); setModalOpen(true); }}
            className="flex items-center gap-2 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-bold rounded-xl transition-all shadow-sm">
            <PlusCircle size={16} /> New Template
          </button>
        </div>
      </div>

      <div className="relative max-w-sm">
        <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
        <input
          placeholder="Search by part number or name..."
          value={search}
          onChange={e => setSearch(e.target.value)}
          className="w-full pl-9 pr-3 py-2.5 bg-white border border-slate-200 rounded-xl text-sm outline-none focus:ring-2 focus:ring-indigo-500"
        />
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase text-xs tracking-wider">
            <tr>
              <th className="px-6 py-4">Part Number</th>
              <th className="px-6 py-4">Name</th>
              <th className="px-6 py-4">Operations</th>
              <th className="px-6 py-4">Status</th>
              <th className="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {filtered.map(t => (
              <tr key={t.id} className="hover:bg-slate-50/50 transition-colors">
                <td className="px-6 py-4 font-mono font-bold text-slate-900">
                  {t.partNumber}
                  <span className="ml-2 px-1.5 py-0.5 text-[9px] font-black tracking-wider uppercase rounded bg-indigo-50 text-indigo-600 border border-indigo-100">
                    Rev {t.revisionNumber}
                  </span>
                </td>
                <td className="px-6 py-4 font-medium text-slate-700">{t.name}</td>
                <td className="px-6 py-4 text-slate-500">
                  {t.operations.slice().sort((a, b) => a.sequenceNumber - b.sequenceNumber).map(o => o.operationName).join(' → ') || '—'}
                </td>
                <td className="px-6 py-4">
                  <span className={`px-2.5 py-1 text-[11px] font-black tracking-wider uppercase rounded-full ${
                    t.isActive ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-100 text-slate-500'
                  }`}>
                    {t.isActive ? 'Active' : 'Inactive'}
                  </span>
                </td>
                <td className="px-6 py-4 text-right">
                  <div className="flex justify-end gap-1">
                    <button onClick={() => openHistory(t)} title="Revision history"
                      className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-lg transition-all">
                      <History size={15} />
                    </button>
                    <button onClick={() => { setEditing(t); setModalOpen(true); }}
                      className="p-2 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-all">
                      <Edit3 size={15} />
                    </button>
                    <button onClick={() => handleDelete(t)}
                      className="p-2 text-slate-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-all">
                      <Trash2 size={15} />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {filtered.length === 0 && !loading && (
              <tr>
                <td colSpan={5} className="px-6 py-12 text-center text-slate-500">
                  <Route className="w-12 h-12 mx-auto text-slate-300 mb-3" />
                  <p className="font-medium">No routing templates yet</p>
                  <p className="text-sm mt-1">Create one per part so its BOM line knows which operations to run through.</p>
                </td>
              </tr>
            )}
          </tbody>
        </table>
        </div>
      </div>

      <RoutingTemplateModal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        onSuccess={fetchData}
        template={editing}
      />

      {historyFor && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg border border-slate-200 flex flex-col max-h-[80vh]">
            <div className="bg-slate-50 px-6 py-4 border-b flex justify-between items-center shrink-0">
              <div>
                <h2 className="text-lg font-black text-slate-900">Revision History</h2>
                <p className="text-xs text-slate-500 font-mono">{historyFor.partNumber} — {historyFor.name}</p>
              </div>
              <button onClick={() => setHistoryFor(null)} className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors">
                <X size={20} />
              </button>
            </div>
            <div className="p-4 overflow-y-auto flex-1 space-y-2">
              {loadingHistory ? (
                <p className="text-center text-sm text-slate-400 py-6">Loading...</p>
              ) : history.length === 0 ? (
                <p className="text-center text-sm text-slate-400 py-6">No revisions found.</p>
              ) : (
                history.map(rev => (
                  <div key={rev.id} className={`flex items-center justify-between p-3 rounded-xl border ${
                    rev.isCurrentRevision ? 'bg-emerald-50 border-emerald-200' : 'bg-slate-50 border-slate-200'
                  }`}>
                    <div>
                      <p className="text-sm font-black text-slate-800">
                        Rev {rev.revisionNumber} {rev.isCurrentRevision && <span className="ml-1 text-[9px] font-black uppercase text-emerald-600">Current</span>}
                      </p>
                      <p className="text-[11px] text-slate-400 font-bold">
                        {new Date(rev.createdAt).toLocaleString()} · {rev.operations.length} operation{rev.operations.length === 1 ? '' : 's'}
                      </p>
                    </div>
                    <button onClick={() => setViewingRevision(rev)}
                      className="flex items-center gap-1 px-2.5 py-1.5 text-[11px] font-bold text-indigo-600 hover:bg-indigo-50 rounded-lg transition-all">
                      <Eye size={13} /> View
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}

      {viewingRevision && (
        <RoutingTemplateModal
          isOpen={true}
          onClose={() => setViewingRevision(null)}
          onSuccess={() => {}}
          template={viewingRevision}
          readOnly
        />
      )}
    </div>
  );
};

export default RoutingTemplates;
