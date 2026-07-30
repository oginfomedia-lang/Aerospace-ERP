import React, { useEffect, useState, useCallback } from 'react';
import { Clock, RefreshCw, AlertOctagon, ShieldAlert } from 'lucide-react';
import api from '../api/axios';
import notificationApi from '../api/notificationAxios';
import { CapaRecord } from '../types/inventory';
import { NotificationLog } from '../types/notification';
import toast from 'react-hot-toast';

interface ApiResponse<T> { message: string; data: T; }

const SlaTracking: React.FC = () => {
  const [overdueCapas, setOverdueCapas] = useState<CapaRecord[]>([]);
  const [capaAlerts, setCapaAlerts] = useState<NotificationLog[]>([]);
  const [escalations, setEscalations] = useState<NotificationLog[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchAll = useCallback(async () => {
    setLoading(true);
    try {
      const [capaRes, capaAlertRes, escalationRes] = await Promise.allSettled([
        api.get<ApiResponse<CapaRecord[]>>('/inventory/capa'),
        notificationApi.get<NotificationLog[]>('/notifications/type/CAPA_OVERDUE'),
        notificationApi.get<NotificationLog[]>('/notifications/type/APPROVAL_ESCALATED'),
      ]);

      if (capaRes.status === 'fulfilled') {
        const now = Date.now();
        const overdue = capaRes.value.data.data.filter(c =>
          c.targetCloseDate && new Date(c.targetCloseDate).getTime() < now &&
          c.status !== 'CLOSED' && c.status !== 'REJECTED'
        );
        setOverdueCapas(overdue);
      } else {
        toast.error('CAPA data unavailable');
      }

      if (capaAlertRes.status === 'fulfilled') setCapaAlerts(capaAlertRes.value.data);
      if (escalationRes.status === 'fulfilled') setEscalations(escalationRes.value.data);
    } catch {
      toast.error('Failed to load SLA tracking data');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  const daysOverdue = (targetCloseDate: string) =>
    Math.floor((Date.now() - new Date(targetCloseDate).getTime()) / (1000 * 60 * 60 * 24));

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black flex items-center gap-2"><Clock className="text-rose-600" /> SLA & Overdue Tracking</h1>
          <p className="text-sm text-slate-500">CAPAs past target close date, and approvals that escalated past their SLA window</p>
        </div>
        <button onClick={fetchAll} className="flex items-center gap-2 px-4 py-2 rounded-xl bg-rose-600 text-white text-sm font-bold hover:bg-rose-700">
          <RefreshCw size={16} className={loading ? 'animate-spin' : ''} /> Refresh
        </button>
      </div>

      <section>
        <h2 className="text-sm font-black uppercase tracking-wider text-slate-500 mb-3 flex items-center gap-2">
          <AlertOctagon size={16} className="text-rose-500" /> Overdue CAPAs ({overdueCapas.length})
        </h2>
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-slate-200">
              <tr className="text-xs uppercase tracking-wider text-slate-500">
                <th className="text-left px-4 py-3">CAPA #</th>
                <th className="text-left px-4 py-3">NCR</th>
                <th className="text-left px-4 py-3">Status</th>
                <th className="text-left px-4 py-3">Target Close</th>
                <th className="text-left px-4 py-3">Overdue By</th>
                <th className="text-left px-4 py-3">Owner</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {overdueCapas.map(c => (
                <tr key={c.id} className="hover:bg-slate-50/50">
                  <td className="px-4 py-3 font-semibold">{c.capaNumber}</td>
                  <td className="px-4 py-3 text-slate-600">{c.ncrNumber}</td>
                  <td className="px-4 py-3">
                    <span className="px-2.5 py-1 text-[11px] font-black uppercase rounded-full bg-amber-100 text-amber-700">{c.status}</span>
                  </td>
                  <td className="px-4 py-3">{c.targetCloseDate ? new Date(c.targetCloseDate).toLocaleDateString() : '—'}</td>
                  <td className="px-4 py-3">
                    <span className="px-2 py-0.5 text-[11px] font-black uppercase rounded bg-rose-100 text-rose-700 border border-rose-200">
                      {c.targetCloseDate ? `${daysOverdue(c.targetCloseDate)}d overdue` : '—'}
                    </span>
                  </td>
                  <td className="px-4 py-3">{c.ownerName || '—'}</td>
                </tr>
              ))}
              {!loading && overdueCapas.length === 0 && (
                <tr><td colSpan={6} className="text-center py-8 text-slate-400">No overdue CAPAs.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </section>

      <section>
        <h2 className="text-sm font-black uppercase tracking-wider text-slate-500 mb-3 flex items-center gap-2">
          <ShieldAlert size={16} className="text-orange-500" /> Escalated Approvals ({escalations.length})
        </h2>
        <div className="bg-white rounded-2xl border border-slate-200 divide-y divide-slate-100 shadow-sm">
          {escalations.map(n => (
            <div key={n.id} className="px-4 py-3 flex items-center justify-between">
              <span className="text-sm text-slate-700">{n.message}</span>
              <span className="text-xs text-slate-400 whitespace-nowrap ml-4">{new Date(n.createdAt).toLocaleString()}</span>
            </div>
          ))}
          {!loading && escalations.length === 0 && (
            <div className="text-center py-8 text-slate-400">No approvals have escalated.</div>
          )}
        </div>
      </section>

      <section>
        <h2 className="text-sm font-black uppercase tracking-wider text-slate-500 mb-3">CAPA Overdue Alerts (Notification Feed) ({capaAlerts.length})</h2>
        <div className="bg-white rounded-2xl border border-slate-200 divide-y divide-slate-100 shadow-sm">
          {capaAlerts.map(n => (
            <div key={n.id} className="px-4 py-3 flex items-center justify-between">
              <span className="text-sm text-slate-700">{n.message}</span>
              <span className="text-xs text-slate-400 whitespace-nowrap ml-4">{new Date(n.createdAt).toLocaleString()}</span>
            </div>
          ))}
          {!loading && capaAlerts.length === 0 && (
            <div className="text-center py-8 text-slate-400">No overdue-CAPA alerts yet — the daily 4 AM sweep hasn't found one.</div>
          )}
        </div>
      </section>
    </div>
  );
};

export default SlaTracking;
