import React, { useEffect, useState } from 'react';
import financeApi from '../api/financeAxios';
import toast from 'react-hot-toast';
import { BarChart3, Loader2 } from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import { FinanceReportSummary } from '../types/finance';

const FinanceReportsPage = () => {
  const [summary, setSummary] = useState<FinanceReportSummary | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    financeApi.get<ApiResponse<FinanceReportSummary>>('/finance/reports/summary')
      .then(res => setSummary(res.data.data))
      .catch(() => toast.error('Failed to fetch finance summary.'))
      .finally(() => setLoading(false));
  }, []);

  const tiles = summary ? [
    { label: 'Journals Submitted', value: summary.totalJournalsSubmitted, color: 'text-slate-800' },
    { label: 'Journals Posted', value: summary.journalsPosted, color: 'text-emerald-600' },
    { label: 'Journals Rejected', value: summary.journalsRejected, color: 'text-red-500' },
    { label: 'Posting Success Rate', value: `${summary.journalPostingSuccessRatePercent.toFixed(1)}%`, color: 'text-blue-600' },
    { label: 'Open Periods', value: summary.openPeriodCount, color: 'text-emerald-600' },
    { label: 'Closed Periods', value: summary.closedPeriodCount, color: 'text-amber-600' },
    { label: 'Locked Periods', value: summary.lockedPeriodCount, color: 'text-red-500' },
    { label: 'Outstanding AP', value: summary.outstandingAP.toFixed(2), color: 'text-slate-800' },
    { label: 'Outstanding AR', value: summary.outstandingAR.toFixed(2), color: 'text-slate-800' },
    { label: 'Total Revenue Posted', value: summary.totalRevenuePosted.toFixed(2), color: 'text-emerald-600' },
    { label: 'Total Expense Posted', value: summary.totalExpensePosted.toFixed(2), color: 'text-red-500' },
    { label: 'Gross Margin (approx)', value: summary.grossMarginApprox.toFixed(2), color: 'text-blue-600' },
  ] : [];

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center gap-3">
        <div className="p-3 bg-blue-600 rounded-2xl text-white"><BarChart3 size={24} /></div>
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Finance Summary</h1>
          <p className="text-slate-500 text-sm font-medium">Is finance healthy today — one glance at posting rates, open periods, and outstanding balances.</p>
        </div>
      </div>

      {loading ? (
        <div className="bg-white rounded-3xl p-16 text-center border border-slate-100"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
      ) : !summary ? (
        <div className="bg-white rounded-3xl p-16 text-center border border-slate-100 text-slate-300">
          <BarChart3 size={48} className="mx-auto mb-3 opacity-30" />
          <p className="font-bold uppercase tracking-widest text-xs">No data available</p>
        </div>
      ) : (
        <div className="grid grid-cols-4 gap-4">
          {tiles.map(t => (
            <div key={t.label} className="bg-white rounded-2xl border border-slate-100 shadow-sm p-5">
              <div className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-2">{t.label}</div>
              <div className={`text-2xl font-black font-mono ${t.color}`}>{t.value}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default FinanceReportsPage;
