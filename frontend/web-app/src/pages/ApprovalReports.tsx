import React, { useState, useEffect, useCallback } from 'react';
import { ClipboardList, Clock, Gauge, Loader2 } from 'lucide-react';
import procurementApi from '../api/procurementAxios';

interface PendingApprovalRow {
  moduleCode: string;
  documentId: number;
  documentVersion: number;
  approvalLevel: number;
  approverRole: string;
  pendingSince: string | null;
  hoursPending: number | null;
  escalationHours: number | null;
  alreadyEscalated: boolean;
  slaBreached: boolean;
}

interface SlaComplianceDetailRow {
  moduleCode: string;
  documentId: number;
  documentVersion: number;
  approvalLevel: number;
  approverRole: string;
  escalationHours: number | null;
  escalated: boolean;
  compliant: boolean;
}

interface SlaComplianceReport {
  moduleCode: string | null;
  periodFrom: string;
  periodTo: string;
  compliantCount: number;
  breachedCount: number;
  noSlaConfiguredCount: number;
  compliancePercentage: number | null;
  details: SlaComplianceDetailRow[];
}

interface TurnaroundRow {
  moduleCode: string;
  documentId: number;
  documentVersion: number;
  outcome: 'APPROVED' | 'REJECTED';
  startedAt: string;
  completedAt: string;
  turnaroundHours: number;
  levelsInChain: number;
}

type Tab = 'PENDING' | 'SLA' | 'TURNAROUND';

const isoDateInput = (d: Date) => d.toISOString().slice(0, 10);

const ApprovalReports = () => {
  const [tab, setTab] = useState<Tab>('PENDING');
  const [loading, setLoading] = useState(false);

  const [from, setFrom] = useState(isoDateInput(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000)));
  const [to, setTo] = useState(isoDateInput(new Date()));

  const [pending, setPending] = useState<PendingApprovalRow[]>([]);
  const [sla, setSla] = useState<SlaComplianceReport | null>(null);
  const [turnaround, setTurnaround] = useState<TurnaroundRow[]>([]);

  const fetchPending = useCallback(async () => {
    setLoading(true);
    try {
      const res = await procurementApi.get<PendingApprovalRow[]>('/approval-reports/pending-approvals');
      setPending(res.data);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchSla = useCallback(async () => {
    setLoading(true);
    try {
      const res = await procurementApi.get<SlaComplianceReport>('/approval-reports/sla-compliance', {
        params: { from: `${from}T00:00:00`, to: `${to}T23:59:59` },
      });
      setSla(res.data);
    } finally {
      setLoading(false);
    }
  }, [from, to]);

  const fetchTurnaround = useCallback(async () => {
    setLoading(true);
    try {
      const res = await procurementApi.get<TurnaroundRow[]>('/approval-reports/turnaround-time', {
        params: { from: `${from}T00:00:00`, to: `${to}T23:59:59` },
      });
      setTurnaround(res.data);
    } finally {
      setLoading(false);
    }
  }, [from, to]);

  useEffect(() => {
    if (tab === 'PENDING') fetchPending();
    if (tab === 'SLA') fetchSla();
    if (tab === 'TURNAROUND') fetchTurnaround();
  }, [tab, fetchPending, fetchSla, fetchTurnaround]);

  const tabs: { key: Tab; label: string; icon: React.ReactNode }[] = [
    { key: 'PENDING', label: 'Pending Approvals', icon: <ClipboardList size={14} /> },
    { key: 'SLA', label: 'SLA Compliance', icon: <Gauge size={14} /> },
    { key: 'TURNAROUND', label: 'Turnaround Time', icon: <Clock size={14} /> },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <div className="p-2.5 bg-slate-800 rounded-2xl text-white">
          <ClipboardList size={20} />
        </div>
        <div>
          <h1 className="text-xl font-black text-slate-900">Approval Reports</h1>
          <p className="text-xs text-slate-500 font-medium">Pending approvals, SLA compliance, and turnaround time across the Approval Engine</p>
        </div>
      </div>

      <div className="flex items-center justify-between gap-3 border-b border-slate-200">
        <div className="flex gap-2">
          {tabs.map(t => (
            <button key={t.key} onClick={() => setTab(t.key)}
              className={`flex items-center gap-1.5 px-4 py-2.5 text-sm font-bold border-b-2 transition-all ${
                tab === t.key ? 'border-slate-900 text-slate-900' : 'border-transparent text-slate-400 hover:text-slate-600'
              }`}>
              {t.icon} {t.label}
            </button>
          ))}
        </div>
        {tab !== 'PENDING' && (
          <div className="flex items-center gap-2 pb-2">
            <input type="date" value={from} onChange={e => setFrom(e.target.value)}
              className="px-2 py-1 text-xs border border-slate-200 rounded-lg" />
            <span className="text-xs text-slate-400">to</span>
            <input type="date" value={to} onChange={e => setTo(e.target.value)}
              className="px-2 py-1 text-xs border border-slate-200 rounded-lg" />
          </div>
        )}
      </div>

      {loading ? (
        <div className="p-16 text-center"><Loader2 className="animate-spin text-slate-400 mx-auto" size={28} /></div>
      ) : tab === 'PENDING' ? (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
          <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-[10px] uppercase tracking-widest font-black text-slate-400">
              <tr>
                <th className="px-5 py-3">Module / Document</th>
                <th className="px-5 py-3">Level</th>
                <th className="px-5 py-3">Role</th>
                <th className="px-5 py-3">Pending Since</th>
                <th className="px-5 py-3">Hours Pending</th>
                <th className="px-5 py-3">SLA</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {pending.map(r => (
                <tr key={`${r.moduleCode}-${r.documentId}-${r.approvalLevel}`}>
                  <td className="px-5 py-2.5 font-mono text-xs font-bold text-slate-700">{r.moduleCode} #{r.documentId}</td>
                  <td className="px-5 py-2.5 text-xs text-slate-600">{r.approvalLevel}</td>
                  <td className="px-5 py-2.5 text-xs text-slate-600">{r.approverRole}</td>
                  <td className="px-5 py-2.5 text-xs text-slate-500">{r.pendingSince ? new Date(r.pendingSince).toLocaleString() : '—'}</td>
                  <td className="px-5 py-2.5 text-xs text-slate-600">{r.hoursPending ?? '—'}</td>
                  <td className="px-5 py-2.5">
                    {r.alreadyEscalated ? (
                      <span className="px-2 py-0.5 text-[10px] font-black uppercase rounded bg-orange-100 text-orange-700 border border-orange-200">⚠ Escalated</span>
                    ) : r.slaBreached ? (
                      <span className="px-2 py-0.5 text-[10px] font-black uppercase rounded bg-rose-100 text-rose-700 border border-rose-200">Breached</span>
                    ) : r.escalationHours != null ? (
                      <span className="px-2 py-0.5 text-[10px] font-black uppercase rounded bg-emerald-100 text-emerald-700 border border-emerald-200">On Track</span>
                    ) : (
                      <span className="text-xs text-slate-300">—</span>
                    )}
                  </td>
                </tr>
              ))}
              {pending.length === 0 && <tr><td colSpan={6} className="px-5 py-8 text-center text-slate-400 text-xs">No pending approvals</td></tr>}
            </tbody>
          </table>
          </div>
        </div>
      ) : tab === 'SLA' ? (
        <div className="space-y-4">
          {sla && (
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
              <div className="bg-white rounded-2xl border border-slate-200 p-4">
                <div className="text-[10px] uppercase tracking-widest font-black text-slate-400">Compliance</div>
                <div className="text-2xl font-black text-slate-900">{sla.compliancePercentage != null ? `${sla.compliancePercentage.toFixed(1)}%` : '—'}</div>
              </div>
              <div className="bg-white rounded-2xl border border-slate-200 p-4">
                <div className="text-[10px] uppercase tracking-widest font-black text-slate-400">Compliant</div>
                <div className="text-2xl font-black text-emerald-600">{sla.compliantCount}</div>
              </div>
              <div className="bg-white rounded-2xl border border-slate-200 p-4">
                <div className="text-[10px] uppercase tracking-widest font-black text-slate-400">Breached</div>
                <div className="text-2xl font-black text-rose-600">{sla.breachedCount}</div>
              </div>
              <div className="bg-white rounded-2xl border border-slate-200 p-4">
                <div className="text-[10px] uppercase tracking-widest font-black text-slate-400">No SLA Configured</div>
                <div className="text-2xl font-black text-slate-400">{sla.noSlaConfiguredCount}</div>
              </div>
            </div>
          )}
          <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
            <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-[10px] uppercase tracking-widest font-black text-slate-400">
                <tr>
                  <th className="px-5 py-3">Module / Document</th>
                  <th className="px-5 py-3">Level</th>
                  <th className="px-5 py-3">Role</th>
                  <th className="px-5 py-3">SLA (hrs)</th>
                  <th className="px-5 py-3">Result</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {(sla?.details ?? []).map((d, i) => (
                  <tr key={`${d.moduleCode}-${d.documentId}-${d.approvalLevel}-${i}`}>
                    <td className="px-5 py-2.5 font-mono text-xs font-bold text-slate-700">{d.moduleCode} #{d.documentId}</td>
                    <td className="px-5 py-2.5 text-xs text-slate-600">{d.approvalLevel}</td>
                    <td className="px-5 py-2.5 text-xs text-slate-600">{d.approverRole}</td>
                    <td className="px-5 py-2.5 text-xs text-slate-600">{d.escalationHours ?? '—'}</td>
                    <td className="px-5 py-2.5">
                      {d.compliant ? (
                        <span className="px-2 py-0.5 text-[10px] font-black uppercase rounded bg-emerald-100 text-emerald-700 border border-emerald-200">Compliant</span>
                      ) : (
                        <span className="px-2 py-0.5 text-[10px] font-black uppercase rounded bg-rose-100 text-rose-700 border border-rose-200">Breached</span>
                      )}
                    </td>
                  </tr>
                ))}
                {(!sla || sla.details.length === 0) && <tr><td colSpan={5} className="px-5 py-8 text-center text-slate-400 text-xs">No completed approvals with an SLA in this period</td></tr>}
              </tbody>
            </table>
            </div>
          </div>
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden">
          <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-[10px] uppercase tracking-widest font-black text-slate-400">
              <tr>
                <th className="px-5 py-3">Module / Document</th>
                <th className="px-5 py-3">Outcome</th>
                <th className="px-5 py-3">Started</th>
                <th className="px-5 py-3">Completed</th>
                <th className="px-5 py-3">Turnaround (hrs)</th>
                <th className="px-5 py-3">Levels</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {turnaround.map((r, i) => (
                <tr key={`${r.moduleCode}-${r.documentId}-${r.documentVersion}-${i}`}>
                  <td className="px-5 py-2.5 font-mono text-xs font-bold text-slate-700">{r.moduleCode} #{r.documentId}</td>
                  <td className="px-5 py-2.5">
                    <span className={`px-2 py-0.5 text-[10px] font-black uppercase rounded border ${
                      r.outcome === 'APPROVED' ? 'bg-emerald-100 text-emerald-700 border-emerald-200' : 'bg-rose-100 text-rose-700 border-rose-200'
                    }`}>{r.outcome}</span>
                  </td>
                  <td className="px-5 py-2.5 text-xs text-slate-500">{new Date(r.startedAt).toLocaleString()}</td>
                  <td className="px-5 py-2.5 text-xs text-slate-500">{new Date(r.completedAt).toLocaleString()}</td>
                  <td className="px-5 py-2.5 text-xs font-bold text-slate-700">{r.turnaroundHours}</td>
                  <td className="px-5 py-2.5 text-xs text-slate-600">{r.levelsInChain}</td>
                </tr>
              ))}
              {turnaround.length === 0 && <tr><td colSpan={6} className="px-5 py-8 text-center text-slate-400 text-xs">No completed approvals in this period</td></tr>}
            </tbody>
          </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default ApprovalReports;
