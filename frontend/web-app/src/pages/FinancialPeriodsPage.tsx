import React, { useEffect, useState } from 'react';
import financeApi from '../api/financeAxios';
import toast from 'react-hot-toast';
import { CalendarClock, Loader2, PlusCircle, Lock, Unlock, CalendarCheck2 } from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import { FiscalYear, AccountingPeriod, PeriodStatus } from '../types/finance';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole, CAN_APPROVE_FINANCE, CAN_LOCK_PERIOD } from '../utils/permissions';

const statusStyle: Record<PeriodStatus, string> = {
  OPEN:   'bg-emerald-50 text-emerald-700 border-emerald-200',
  CLOSED: 'bg-amber-50   text-amber-700   border-amber-200',
  LOCKED: 'bg-red-50     text-red-700     border-red-200',
};

const emptyForm = () => ({ fiscalYearCode: '', startDate: '', endDate: '' });

const FinancialPeriodsPage = () => {
  const { user } = useAuth();
  const canManage = hasAnyRole(user?.roles, CAN_APPROVE_FINANCE);
  const canLock = hasAnyRole(user?.roles, CAN_LOCK_PERIOD);
  const [fiscalYears, setFiscalYears] = useState<FiscalYear[]>([]);
  const [periods, setPeriods] = useState<AccountingPeriod[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm());
  const [submitting, setSubmitting] = useState(false);
  const [busyId, setBusyId] = useState<number | null>(null);

  const fetchAll = async () => {
    setLoading(true);
    try {
      const [fyRes, pRes] = await Promise.all([
        financeApi.get<ApiResponse<FiscalYear[]>>('/finance/fiscal-years'),
        financeApi.get<ApiResponse<AccountingPeriod[]>>('/finance/periods'),
      ]);
      setFiscalYears(fyRes.data.data);
      setPeriods(pRes.data.data);
    } catch {
      toast.error('Failed to fetch fiscal years / periods.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchAll(); }, []);

  const handleCreateFiscalYear = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.fiscalYearCode || !form.startDate || !form.endDate) { toast.error('All three fields are required.'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        financeApi.post('/finance/fiscal-years', form),
        { loading: 'Creating fiscal year...', success: 'Fiscal year created.', error: (err) => err.response?.data?.message || 'Failed to create fiscal year.' }
      );
      setShowForm(false);
      setForm(emptyForm());
      await fetchAll();
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  const generateMonthly = async (fy: FiscalYear) => {
    setBusyId(fy.id);
    try {
      await toast.promise(
        financeApi.post(`/finance/periods/fiscal-years/${fy.id}/generate-monthly`, {}),
        { loading: `Generating periods for ${fy.fiscalYearCode}...`, success: 'Monthly periods generated.', error: (err) => err.response?.data?.message || 'Failed to generate periods.' }
      );
      await fetchAll();
    } catch {
      // toast.promise handles display
    } finally {
      setBusyId(null);
    }
  };

  const runPeriodAction = async (period: AccountingPeriod, action: 'close' | 'reopen' | 'lock' | 'unlock') => {
    setBusyId(period.id);
    try {
      await toast.promise(
        financeApi.post(`/finance/periods/${period.id}/${action}`, {}),
        {
          loading: `${action.charAt(0).toUpperCase() + action.slice(1)}ing ${period.periodLabel}...`,
          success: `${period.periodLabel} ${action === 'close' ? 'closed' : action === 'reopen' ? 'reopened' : action === 'lock' ? 'locked' : 'unlocked'}.`,
          error: (err) => err.response?.data?.message || `Could not ${action} this period.`,
        }
      );
      await fetchAll();
    } catch {
      // toast.promise handles display
    } finally {
      setBusyId(null);
    }
  };

  const periodsByYear = (fiscalYearCode: string) =>
    periods.filter(p => p.fiscalYearCode === fiscalYearCode).sort((a, b) => a.periodNumber - b.periodNumber);

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white">
            <CalendarClock size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Financial Periods</h1>
            <p className="text-slate-500 text-sm font-medium">
              Locks the calendar — journal entries can only post into an OPEN period.
            </p>
          </div>
        </div>
        {canManage && (
          <button onClick={() => { setShowForm(!showForm); setForm(emptyForm()); }}
            className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-sm font-black rounded-xl hover:bg-blue-700 transition shadow-lg shadow-blue-200">
            <PlusCircle size={16} /> New Fiscal Year
          </button>
        )}
      </div>

      {showForm && (
        <form onSubmit={handleCreateFiscalYear} className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">New Fiscal Year</h2>
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Fiscal Year Code</label>
              <input value={form.fiscalYearCode} onChange={e => setForm({ ...form, fiscalYearCode: e.target.value })}
                placeholder="e.g. FY2027" className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Start Date</label>
              <input type="date" value={form.startDate} onChange={e => setForm({ ...form, startDate: e.target.value })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">End Date</label>
              <input type="date" value={form.endDate} onChange={e => setForm({ ...form, endDate: e.target.value })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => setShowForm(false)} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting}
              className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">
              Create Fiscal Year
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <div className="bg-white rounded-3xl p-16 text-center border border-slate-100"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
      ) : fiscalYears.length === 0 ? (
        <div className="bg-white rounded-3xl p-16 text-center border border-slate-100 text-slate-300">
          <CalendarClock size={48} className="mx-auto mb-3 opacity-30" />
          <p className="font-bold uppercase tracking-widest text-xs">No fiscal years yet</p>
        </div>
      ) : (
        fiscalYears.map(fy => {
          const yearPeriods = periodsByYear(fy.fiscalYearCode);
          return (
            <div key={fy.id} className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
              <div className="p-5 border-b border-slate-100 flex items-center justify-between">
                <div>
                  <h2 className="font-black text-slate-800 text-sm">{fy.fiscalYearCode}</h2>
                  <p className="text-[11px] text-slate-400 font-bold">{fy.startDate} &rarr; {fy.endDate}</p>
                </div>
                {canManage && yearPeriods.length === 0 && (
                  <button disabled={busyId === fy.id} onClick={() => generateMonthly(fy)}
                    className="flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs font-black bg-slate-900 text-white hover:bg-slate-800 disabled:opacity-60">
                    <CalendarCheck2 size={13} /> Generate Monthly Periods
                  </button>
                )}
              </div>

              {yearPeriods.length === 0 ? (
                <p className="p-5 text-xs text-slate-400 font-bold">No periods generated for this fiscal year yet.</p>
              ) : (
                <div className="divide-y divide-slate-50">
                  {yearPeriods.map(p => (
                    <div key={p.id} className="p-4 px-5 flex items-center justify-between">
                      <div className="flex items-center gap-3">
                        <span className="text-sm font-bold text-slate-700">{p.periodLabel}</span>
                        <span className="text-[11px] text-slate-400">{p.startDate} &ndash; {p.endDate}</span>
                        <span className={`text-[10px] font-black px-2.5 py-0.5 rounded-full border ${statusStyle[p.status]}`}>{p.status}</span>
                      </div>
                      <div className="flex items-center gap-2">
                        {canManage && p.status === 'OPEN' && (
                          <button disabled={busyId === p.id} onClick={() => runPeriodAction(p, 'close')}
                            className="px-3 py-1 text-[11px] font-black rounded-lg bg-white border border-slate-200 text-slate-600 hover:border-amber-300 hover:text-amber-600 disabled:opacity-60">
                            Close
                          </button>
                        )}
                        {canManage && p.status === 'CLOSED' && (
                          <button disabled={busyId === p.id} onClick={() => runPeriodAction(p, 'reopen')}
                            className="px-3 py-1 text-[11px] font-black rounded-lg bg-white border border-slate-200 text-slate-600 hover:border-emerald-300 hover:text-emerald-600 disabled:opacity-60">
                            Reopen
                          </button>
                        )}
                        {canLock && p.status === 'CLOSED' && (
                          <button disabled={busyId === p.id} onClick={() => runPeriodAction(p, 'lock')}
                            className="flex items-center gap-1 px-3 py-1 text-[11px] font-black rounded-lg bg-white border border-slate-200 text-slate-600 hover:border-red-300 hover:text-red-600 disabled:opacity-60">
                            <Lock size={11} /> Lock
                          </button>
                        )}
                        {canLock && p.status === 'LOCKED' && (
                          <button disabled={busyId === p.id} onClick={() => runPeriodAction(p, 'unlock')}
                            className="flex items-center gap-1 px-3 py-1 text-[11px] font-black rounded-lg bg-white border border-slate-200 text-slate-600 hover:border-slate-400 disabled:opacity-60">
                            <Unlock size={11} /> Unlock
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          );
        })
      )}
    </div>
  );
};

export default FinancialPeriodsPage;
