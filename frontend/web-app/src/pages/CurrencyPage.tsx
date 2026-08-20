import React, { useEffect, useState } from 'react';
import financeApi from '../api/financeAxios';
import toast from 'react-hot-toast';
import { Coins, Loader2, PlusCircle, RefreshCw } from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import { Currency, ExchangeRate, Account } from '../types/finance';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole, CAN_APPROVE_FINANCE } from '../utils/permissions';

type TabKey = 'currencies' | 'rates' | 'revalue';

const emptyCurrencyForm = () => ({ currencyCode: '', currencyName: '', isBaseCurrency: false });
const emptyRateForm = () => ({ fromCurrency: '', toCurrency: '', rate: '', effectiveDate: new Date().toISOString().slice(0, 10) });
const emptyRevalueForm = () => ({ controlAccountCode: '', foreignCurrency: '', asOfDate: new Date().toISOString().slice(0, 10) });

const CurrencyPage = () => {
  const { user } = useAuth();
  const canManage = hasAnyRole(user?.roles, CAN_APPROVE_FINANCE);
  const [tab, setTab] = useState<TabKey>('currencies');
  const [currencies, setCurrencies] = useState<Currency[]>([]);
  const [rates, setRates] = useState<ExchangeRate[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [currencyForm, setCurrencyForm] = useState(emptyCurrencyForm());
  const [rateForm, setRateForm] = useState(emptyRateForm());
  const [revalueForm, setRevalueForm] = useState(emptyRevalueForm());
  const [submitting, setSubmitting] = useState(false);

  const fetchAll = async () => {
    setLoading(true);
    try {
      const [cRes, rRes, aRes] = await Promise.all([
        financeApi.get<ApiResponse<Currency[]>>('/finance/currency/currencies'),
        financeApi.get<ApiResponse<ExchangeRate[]>>('/finance/currency/exchange-rates'),
        financeApi.get<ApiResponse<Account[]>>('/finance/accounts'),
      ]);
      setCurrencies(cRes.data.data);
      setRates(rRes.data.data);
      setAccounts(aRes.data.data.filter(a => a.postingPermission === 'POSTABLE'));
    } catch {
      toast.error('Failed to fetch currency data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchAll(); }, []);

  const createCurrency = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!currencyForm.currencyCode || !currencyForm.currencyName) { toast.error('Currency code and name are required.'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        financeApi.post('/finance/currency/currencies', currencyForm),
        { loading: 'Creating currency...', success: 'Currency created.', error: (err) => err.response?.data?.message || 'Failed to create currency.' }
      );
      setShowForm(false);
      setCurrencyForm(emptyCurrencyForm());
      await fetchAll();
    } catch { /* toast.promise handles display */ } finally { setSubmitting(false); }
  };

  const createRate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rateForm.fromCurrency || !rateForm.toCurrency || !rateForm.rate) { toast.error('From/To currency and rate are required.'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        financeApi.post('/finance/currency/exchange-rates', { ...rateForm, rate: Number(rateForm.rate) }),
        { loading: 'Creating exchange rate...', success: 'Exchange rate created.', error: (err) => err.response?.data?.message || 'Failed to create exchange rate.' }
      );
      setShowForm(false);
      setRateForm(emptyRateForm());
      await fetchAll();
    } catch { /* toast.promise handles display */ } finally { setSubmitting(false); }
  };

  const runRevalue = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!revalueForm.controlAccountCode || !revalueForm.foreignCurrency) { toast.error('Control account and foreign currency are required.'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        financeApi.post(`/finance/currency/revalue?controlAccountCode=${revalueForm.controlAccountCode}&foreignCurrency=${revalueForm.foreignCurrency}&asOfDate=${revalueForm.asOfDate}`, {}),
        { loading: 'Running revaluation...', success: 'Revaluation complete — check Journal Entries for a new FX entry.', error: (err) => err.response?.data?.message || 'Revaluation failed.' }
      );
    } catch { /* toast.promise handles display */ } finally { setSubmitting(false); }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white"><Coins size={24} /></div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Multi-Currency</h1>
            <p className="text-slate-500 text-sm font-medium">Currencies, daily exchange rates, and FX revaluation.</p>
          </div>
        </div>
        {canManage && tab !== 'revalue' && (
          <button onClick={() => setShowForm(!showForm)}
            className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-sm font-black rounded-xl hover:bg-blue-700 transition shadow-lg shadow-blue-200">
            <PlusCircle size={16} /> New {tab === 'currencies' ? 'Currency' : 'Exchange Rate'}
          </button>
        )}
      </div>

      <div className="flex gap-2">
        {(['currencies', 'rates', 'revalue'] as TabKey[]).map(t => (
          <button key={t} onClick={() => { setTab(t); setShowForm(false); }}
            className={`px-4 py-2 rounded-xl text-xs font-black transition ${tab === t ? 'bg-slate-900 text-white' : 'bg-white text-slate-500 border border-slate-200'}`}>
            {t === 'currencies' ? 'Currencies' : t === 'rates' ? 'Exchange Rates' : 'Run Revaluation'}
          </button>
        ))}
      </div>

      {showForm && tab === 'currencies' && (
        <form onSubmit={createCurrency} className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">New Currency</h2>
          <div className="grid grid-cols-3 gap-3 items-center">
            <input value={currencyForm.currencyCode} onChange={e => setCurrencyForm({ ...currencyForm, currencyCode: e.target.value.toUpperCase() })} maxLength={3} placeholder="Code (e.g. USD)" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <input value={currencyForm.currencyName} onChange={e => setCurrencyForm({ ...currencyForm, currencyName: e.target.value })} placeholder="Name (e.g. US Dollar)" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <label className="flex items-center gap-2 text-xs font-bold text-slate-600">
              <input type="checkbox" checked={currencyForm.isBaseCurrency} onChange={e => setCurrencyForm({ ...currencyForm, isBaseCurrency: e.target.checked })} /> Base currency
            </label>
          </div>
          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => setShowForm(false)} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting} className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">Create</button>
          </div>
        </form>
      )}

      {showForm && tab === 'rates' && (
        <form onSubmit={createRate} className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">New Exchange Rate</h2>
          <div className="grid grid-cols-4 gap-3">
            <input value={rateForm.fromCurrency} onChange={e => setRateForm({ ...rateForm, fromCurrency: e.target.value.toUpperCase() })} maxLength={3} placeholder="From (e.g. USD)" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <input value={rateForm.toCurrency} onChange={e => setRateForm({ ...rateForm, toCurrency: e.target.value.toUpperCase() })} maxLength={3} placeholder="To (e.g. INR)" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <input type="number" step="0.00000001" value={rateForm.rate} onChange={e => setRateForm({ ...rateForm, rate: e.target.value })} placeholder="Rate" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <input type="date" value={rateForm.effectiveDate} onChange={e => setRateForm({ ...rateForm, effectiveDate: e.target.value })} className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => setShowForm(false)} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting} className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">Create</button>
          </div>
        </form>
      )}

      {tab === 'revalue' && canManage && (
        <form onSubmit={runRevalue} className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Run FX Revaluation</h2>
          <p className="text-xs text-slate-500">Recalculates the base-currency value of open foreign-currency balances on a control account and posts the gain/loss delta.</p>
          <div className="grid grid-cols-3 gap-3">
            <select value={revalueForm.controlAccountCode} onChange={e => setRevalueForm({ ...revalueForm, controlAccountCode: e.target.value })} className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
              <option value="">— Control Account —</option>
              {accounts.map(a => <option key={a.id} value={a.accountCode}>{a.accountCode} — {a.accountName}</option>)}
            </select>
            <select value={revalueForm.foreignCurrency} onChange={e => setRevalueForm({ ...revalueForm, foreignCurrency: e.target.value })} className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
              <option value="">— Foreign Currency —</option>
              {currencies.map(c => <option key={c.currencyCode} value={c.currencyCode}>{c.currencyCode} — {c.currencyName}</option>)}
            </select>
            <input type="date" value={revalueForm.asOfDate} onChange={e => setRevalueForm({ ...revalueForm, asOfDate: e.target.value })} className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
          <div className="flex justify-end">
            <button type="submit" disabled={submitting} className="flex items-center gap-2 px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">
              <RefreshCw size={14} /> Run Revaluation
            </button>
          </div>
        </form>
      )}
      {tab === 'revalue' && !canManage && (
        <div className="bg-white rounded-3xl p-10 text-center border border-slate-100 text-slate-400 text-sm font-bold">
          Only a Finance Manager, Manager, or Admin can run revaluation.
        </div>
      )}

      {tab === 'currencies' && (
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          {loading ? <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div> : currencies.length === 0 ? (
            <div className="p-16 text-center text-slate-300"><Coins size={48} className="mx-auto mb-3 opacity-30" /><p className="font-bold uppercase tracking-widest text-xs">No currencies yet</p></div>
          ) : (
            <div className="divide-y divide-slate-50">
              {currencies.map(c => (
                <div key={c.currencyCode} className="p-4 px-5 flex items-center gap-3">
                  <span className="font-mono text-xs font-black text-slate-800">{c.currencyCode}</span>
                  <span className="text-sm text-slate-600 font-medium">{c.currencyName}</span>
                  {c.isBaseCurrency && <span className="text-[10px] font-black px-2 py-0.5 rounded-full border bg-blue-50 text-blue-700 border-blue-200">BASE</span>}
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {tab === 'rates' && (
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          {loading ? <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div> : rates.length === 0 ? (
            <div className="p-16 text-center text-slate-300"><Coins size={48} className="mx-auto mb-3 opacity-30" /><p className="font-bold uppercase tracking-widest text-xs">No exchange rates yet</p></div>
          ) : (
            <div className="divide-y divide-slate-50">
              {rates.map(r => (
                <div key={r.id} className="p-4 px-5 flex items-center gap-3 text-sm">
                  <span className="font-mono font-bold">{r.fromCurrency} &rarr; {r.toCurrency}</span>
                  <span className="text-slate-600">1 {r.fromCurrency} = {r.rate} {r.toCurrency}</span>
                  <span className="text-[11px] text-slate-400 ml-auto">as of {r.effectiveDate}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default CurrencyPage;
