import React, { useEffect, useState } from 'react';
import financeApi from '../api/financeAxios';
import toast from 'react-hot-toast';
import { Percent, Loader2, PlusCircle } from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import { TaxCategory, TaxJurisdiction, TaxRate, TaxDirection, Account } from '../types/finance';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole, CAN_APPROVE_FINANCE } from '../utils/permissions';

type TabKey = 'categories' | 'jurisdictions' | 'rates';

const emptyCategoryForm = () => ({ categoryCode: '', categoryName: '' });
const emptyJurisdictionForm = () => ({ jurisdictionCode: '', jurisdictionName: '' });
const emptyRateForm = () => ({
  taxRuleCode: '', taxCategoryId: '' as number | '', taxJurisdictionId: '' as number | '',
  ratePercent: '', direction: 'OUTPUT' as TaxDirection, reverseCharge: false, taxAccountCode: '',
});

const TaxPage = () => {
  const { user } = useAuth();
  const canManage = hasAnyRole(user?.roles, CAN_APPROVE_FINANCE);
  const [tab, setTab] = useState<TabKey>('rates');
  const [categories, setCategories] = useState<TaxCategory[]>([]);
  const [jurisdictions, setJurisdictions] = useState<TaxJurisdiction[]>([]);
  const [rates, setRates] = useState<TaxRate[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [categoryForm, setCategoryForm] = useState(emptyCategoryForm());
  const [jurisdictionForm, setJurisdictionForm] = useState(emptyJurisdictionForm());
  const [rateForm, setRateForm] = useState(emptyRateForm());
  const [submitting, setSubmitting] = useState(false);

  const fetchAll = async () => {
    setLoading(true);
    try {
      const [catRes, jurRes, rateRes, accRes] = await Promise.all([
        financeApi.get<ApiResponse<TaxCategory[]>>('/finance/tax-categories'),
        financeApi.get<ApiResponse<TaxJurisdiction[]>>('/finance/tax-jurisdictions'),
        financeApi.get<ApiResponse<TaxRate[]>>('/finance/tax-rates'),
        financeApi.get<ApiResponse<Account[]>>('/finance/accounts'),
      ]);
      setCategories(catRes.data.data);
      setJurisdictions(jurRes.data.data);
      setRates(rateRes.data.data);
      setAccounts(accRes.data.data.filter(a => a.postingPermission === 'POSTABLE'));
    } catch {
      toast.error('Failed to fetch tax configuration.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchAll(); }, []);

  const createCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!categoryForm.categoryCode || !categoryForm.categoryName) { toast.error('Code and name are required.'); return; }
    setSubmitting(true);
    try {
      await toast.promise(financeApi.post('/finance/tax-categories', categoryForm),
        { loading: 'Creating category...', success: 'Tax category created.', error: (err) => err.response?.data?.message || 'Failed to create.' });
      setShowForm(false); setCategoryForm(emptyCategoryForm()); await fetchAll();
    } catch { /* toast.promise handles display */ } finally { setSubmitting(false); }
  };

  const createJurisdiction = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!jurisdictionForm.jurisdictionCode || !jurisdictionForm.jurisdictionName) { toast.error('Code and name are required.'); return; }
    setSubmitting(true);
    try {
      await toast.promise(financeApi.post('/finance/tax-jurisdictions', jurisdictionForm),
        { loading: 'Creating jurisdiction...', success: 'Tax jurisdiction created.', error: (err) => err.response?.data?.message || 'Failed to create.' });
      setShowForm(false); setJurisdictionForm(emptyJurisdictionForm()); await fetchAll();
    } catch { /* toast.promise handles display */ } finally { setSubmitting(false); }
  };

  const createRate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!rateForm.taxRuleCode || !rateForm.ratePercent || !rateForm.taxAccountCode) { toast.error('Rule code, rate percent, and tax account are required.'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        financeApi.post('/finance/tax-rates', {
          ...rateForm,
          ratePercent: Number(rateForm.ratePercent),
          taxCategoryId: rateForm.taxCategoryId || null,
          taxJurisdictionId: rateForm.taxJurisdictionId || null,
        }),
        { loading: 'Creating tax rate...', success: 'Tax rate created.', error: (err) => err.response?.data?.message || 'Failed to create tax rate.' }
      );
      setShowForm(false); setRateForm(emptyRateForm()); await fetchAll();
    } catch { /* toast.promise handles display */ } finally { setSubmitting(false); }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white"><Percent size={24} /></div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Tax</h1>
            <p className="text-slate-500 text-sm font-medium">Tax categories, jurisdictions, and rates — including reverse charge.</p>
          </div>
        </div>
        {canManage && (
          <button onClick={() => setShowForm(!showForm)}
            className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-sm font-black rounded-xl hover:bg-blue-700 transition shadow-lg shadow-blue-200">
            <PlusCircle size={16} /> New {tab === 'categories' ? 'Category' : tab === 'jurisdictions' ? 'Jurisdiction' : 'Tax Rate'}
          </button>
        )}
      </div>

      <div className="flex gap-2">
        {(['rates', 'categories', 'jurisdictions'] as TabKey[]).map(t => (
          <button key={t} onClick={() => { setTab(t); setShowForm(false); }}
            className={`px-4 py-2 rounded-xl text-xs font-black transition ${tab === t ? 'bg-slate-900 text-white' : 'bg-white text-slate-500 border border-slate-200'}`}>
            {t === 'rates' ? 'Tax Rates' : t === 'categories' ? 'Categories' : 'Jurisdictions'}
          </button>
        ))}
      </div>

      {showForm && tab === 'categories' && (
        <form onSubmit={createCategory} className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">New Tax Category</h2>
          <div className="grid grid-cols-2 gap-3">
            <input value={categoryForm.categoryCode} onChange={e => setCategoryForm({ ...categoryForm, categoryCode: e.target.value })} placeholder="Category Code (e.g. GST_STANDARD)" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <input value={categoryForm.categoryName} onChange={e => setCategoryForm({ ...categoryForm, categoryName: e.target.value })} placeholder="Category Name" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => setShowForm(false)} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting} className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">Create</button>
          </div>
        </form>
      )}

      {showForm && tab === 'jurisdictions' && (
        <form onSubmit={createJurisdiction} className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">New Tax Jurisdiction</h2>
          <div className="grid grid-cols-2 gap-3">
            <input value={jurisdictionForm.jurisdictionCode} onChange={e => setJurisdictionForm({ ...jurisdictionForm, jurisdictionCode: e.target.value })} placeholder="Jurisdiction Code (e.g. IN-KA)" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <input value={jurisdictionForm.jurisdictionName} onChange={e => setJurisdictionForm({ ...jurisdictionForm, jurisdictionName: e.target.value })} placeholder="Jurisdiction Name" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => setShowForm(false)} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting} className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">Create</button>
          </div>
        </form>
      )}

      {showForm && tab === 'rates' && (
        <form onSubmit={createRate} className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">New Tax Rate</h2>
          <div className="grid grid-cols-3 gap-3">
            <input value={rateForm.taxRuleCode} onChange={e => setRateForm({ ...rateForm, taxRuleCode: e.target.value })} placeholder="Tax Rule Code (e.g. GST_18_OUTPUT)" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <input type="number" step="0.001" value={rateForm.ratePercent} onChange={e => setRateForm({ ...rateForm, ratePercent: e.target.value })} placeholder="Rate % (e.g. 18)" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <select value={rateForm.direction} onChange={e => setRateForm({ ...rateForm, direction: e.target.value as TaxDirection })} className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
              <option value="OUTPUT">OUTPUT — collected from customers</option>
              <option value="INPUT">INPUT — paid to suppliers</option>
            </select>
            <select value={rateForm.taxAccountCode} onChange={e => setRateForm({ ...rateForm, taxAccountCode: e.target.value })} className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
              <option value="">— Tax Account —</option>
              {accounts.map(a => <option key={a.id} value={a.accountCode}>{a.accountCode} — {a.accountName}</option>)}
            </select>
            <select value={rateForm.taxCategoryId} onChange={e => setRateForm({ ...rateForm, taxCategoryId: e.target.value ? Number(e.target.value) : '' })} className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
              <option value="">— Category (optional) —</option>
              {categories.map(c => <option key={c.id} value={c.id}>{c.categoryCode}</option>)}
            </select>
            <select value={rateForm.taxJurisdictionId} onChange={e => setRateForm({ ...rateForm, taxJurisdictionId: e.target.value ? Number(e.target.value) : '' })} className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
              <option value="">— Jurisdiction (optional) —</option>
              {jurisdictions.map(j => <option key={j.id} value={j.id}>{j.jurisdictionCode}</option>)}
            </select>
          </div>
          <label className="flex items-center gap-2 text-xs font-bold text-slate-600">
            <input type="checkbox" checked={rateForm.reverseCharge} onChange={e => setRateForm({ ...rateForm, reverseCharge: e.target.checked })} />
            Reverse charge — buyer self-assesses both input and output tax
          </label>
          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => setShowForm(false)} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting} className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">Create</button>
          </div>
        </form>
      )}

      {tab === 'rates' && (
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          {loading ? <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div> : rates.length === 0 ? (
            <div className="p-16 text-center text-slate-300"><Percent size={48} className="mx-auto mb-3 opacity-30" /><p className="font-bold uppercase tracking-widest text-xs">No tax rates yet</p></div>
          ) : (
            <div className="divide-y divide-slate-50">
              {rates.map(r => (
                <div key={r.id} className="p-4 px-5 flex items-center gap-3 text-sm flex-wrap">
                  <span className="font-mono font-black text-xs">{r.taxRuleCode}</span>
                  <span className="text-slate-600">{r.ratePercent}%</span>
                  <span className={`text-[10px] font-black px-2 py-0.5 rounded-full border ${r.direction === 'OUTPUT' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-blue-50 text-blue-700 border-blue-200'}`}>{r.direction}</span>
                  {r.reverseCharge && <span className="text-[10px] font-black px-2 py-0.5 rounded-full border bg-amber-50 text-amber-700 border-amber-200">REVERSE CHARGE</span>}
                  <span className="text-[11px] text-slate-400">&rarr; {r.taxAccountCode}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {tab === 'categories' && (
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          {loading ? <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div> : categories.length === 0 ? (
            <div className="p-16 text-center text-slate-300"><Percent size={48} className="mx-auto mb-3 opacity-30" /><p className="font-bold uppercase tracking-widest text-xs">No categories yet</p></div>
          ) : (
            <div className="divide-y divide-slate-50">
              {categories.map(c => (
                <div key={c.id} className="p-4 px-5 flex items-center gap-3 text-sm">
                  <span className="font-mono text-xs font-black">{c.categoryCode}</span>
                  <span className="text-slate-600">{c.categoryName}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {tab === 'jurisdictions' && (
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          {loading ? <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div> : jurisdictions.length === 0 ? (
            <div className="p-16 text-center text-slate-300"><Percent size={48} className="mx-auto mb-3 opacity-30" /><p className="font-bold uppercase tracking-widest text-xs">No jurisdictions yet</p></div>
          ) : (
            <div className="divide-y divide-slate-50">
              {jurisdictions.map(j => (
                <div key={j.id} className="p-4 px-5 flex items-center gap-3 text-sm">
                  <span className="font-mono text-xs font-black">{j.jurisdictionCode}</span>
                  <span className="text-slate-600">{j.jurisdictionName}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default TaxPage;
