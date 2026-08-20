import React, { useEffect, useState } from 'react';
import financeApi from '../api/financeAxios';
import toast from 'react-hot-toast';
import { HandCoins, Loader2 } from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import { LedgerView, AgingBucket } from '../types/finance';

type TabKey = 'ap' | 'ar';

const ApArPage = () => {
  const [tab, setTab] = useState<TabKey>('ap');
  const [ledger, setLedger] = useState<LedgerView[]>([]);
  const [aging, setAging] = useState<AgingBucket[]>([]);
  const [asOfDate, setAsOfDate] = useState(new Date().toISOString().slice(0, 10));
  const [loading, setLoading] = useState(true);

  const fetchData = async (t: TabKey, date: string) => {
    setLoading(true);
    const prefix = t === 'ap' ? '/finance/ap' : '/finance/ar';
    try {
      const [ledgerRes, agingRes] = await Promise.all([
        financeApi.get<ApiResponse<LedgerView[]>>(`${prefix}/ledger`),
        financeApi.get<ApiResponse<AgingBucket[]>>(`${prefix}/aging`, { params: { asOfDate: date } }),
      ]);
      setLedger(ledgerRes.data.data);
      setAging(agingRes.data.data);
    } catch {
      toast.error(`Failed to fetch ${t === 'ap' ? 'Accounts Payable' : 'Accounts Receivable'} data.`);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(tab, asOfDate); }, [tab]); // eslint-disable-line react-hooks/exhaustive-deps

  const bucketColor = (label: string) =>
    label === '0-30' ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
    : label === '31-60' ? 'bg-amber-50 text-amber-700 border-amber-200'
    : label === '61-90' ? 'bg-orange-50 text-orange-700 border-orange-200'
    : 'bg-red-50 text-red-700 border-red-200';

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between flex-wrap gap-3">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white"><HandCoins size={24} /></div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Accounts Payable / Receivable</h1>
            <p className="text-slate-500 text-sm font-medium">Who owes whom — read directly off the General Ledger, grouped by counterparty.</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">As of</label>
          <input type="date" value={asOfDate} onChange={e => { setAsOfDate(e.target.value); fetchData(tab, e.target.value); }}
            className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
        </div>
      </div>

      <div className="flex gap-2">
        <button onClick={() => setTab('ap')} className={`px-4 py-2 rounded-xl text-xs font-black transition ${tab === 'ap' ? 'bg-slate-900 text-white' : 'bg-white text-slate-500 border border-slate-200'}`}>Accounts Payable</button>
        <button onClick={() => setTab('ar')} className={`px-4 py-2 rounded-xl text-xs font-black transition ${tab === 'ar' ? 'bg-slate-900 text-white' : 'bg-white text-slate-500 border border-slate-200'}`}>Accounts Receivable</button>
      </div>

      {loading ? (
        <div className="bg-white rounded-3xl p-16 text-center border border-slate-100"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
      ) : (
        <>
          <div className="grid grid-cols-4 gap-3">
            {aging.map(b => (
              <div key={b.bucketLabel} className={`rounded-2xl border p-4 ${bucketColor(b.bucketLabel)}`}>
                <div className="text-[10px] font-black uppercase tracking-widest">{b.bucketLabel} days</div>
                <div className="text-xl font-black mt-1">{b.totalAmount.toFixed(2)}</div>
                <div className="text-[11px] font-bold mt-1 opacity-70">{Object.keys(b.byCounterparty).length} counterpart{Object.keys(b.byCounterparty).length === 1 ? 'y' : 'ies'}</div>
              </div>
            ))}
          </div>

          <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
            <div className="p-5 border-b border-slate-100">
              <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">
                {tab === 'ap' ? 'Supplier' : 'Customer'} Ledger
              </h2>
            </div>
            {ledger.length === 0 ? (
              <div className="p-16 text-center text-slate-300">
                <HandCoins size={48} className="mx-auto mb-3 opacity-30" />
                <p className="font-bold uppercase tracking-widest text-xs">No open balances</p>
              </div>
            ) : (
              <div className="divide-y divide-slate-50">
                {ledger.map(l => (
                  <div key={l.counterpartyRef} className="p-4 px-5 flex items-center justify-between text-sm">
                    <span className="font-mono font-bold text-slate-700">{l.counterpartyRef}</span>
                    <div className="flex items-center gap-4">
                      <span className="text-[11px] text-slate-400">{l.transactionCount} transaction{l.transactionCount === 1 ? '' : 's'}</span>
                      <span className="font-black text-slate-800 font-mono">{l.openBalance.toFixed(2)}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </>
      )}
    </div>
  );
};

export default ApArPage;
