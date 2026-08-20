import React, { useEffect, useState } from 'react';
import financeApi from '../api/financeAxios';
import toast from 'react-hot-toast';
import { Workflow, Loader2, PlusCircle, Pencil, CheckCircle2, Zap } from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import { Account, PostingRule, PostingTriggerType, TaxRate } from '../types/finance';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole, CAN_APPROVE_FINANCE } from '../utils/permissions';

const TRIGGER_EVENTS: PostingTriggerType[] = [
  'PURCHASE_RECEIPT', 'SUPPLIER_INVOICE', 'VENDOR_INVOICE_APPROVED',
  'INVENTORY_ADJUSTMENT', 'MATERIAL_CONSUMPTION', 'FINISHED_GOODS_RECEIPT',
  'SALES_DISPATCH_CONFIRMED', 'CUSTOMER_INVOICE', 'MAINTENANCE_COST', 'QUALITY_COST',
];

const emptyForm = () => ({
  triggerEvent: 'VENDOR_INVOICE_APPROVED' as PostingTriggerType,
  debitAccountCode: '', creditAccountCode: '', taxRuleCode: '',
  costCenterId: '' as number | '', profitCenterId: '' as number | '', isActive: true,
});

const emptyRunForm = () => ({
  triggerEvent: 'VENDOR_INVOICE_APPROVED' as PostingTriggerType,
  amount: '', currency: 'INR', sourceDocType: 'MANUAL_TEST', sourceDocRef: '',
});

const PostingRulesPage = () => {
  const { user } = useAuth();
  const canManage = hasAnyRole(user?.roles, CAN_APPROVE_FINANCE);
  const [rules, setRules] = useState<PostingRule[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [taxRates, setTaxRates] = useState<TaxRate[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState(emptyForm());
  const [submitting, setSubmitting] = useState(false);
  const [showRunForm, setShowRunForm] = useState(false);
  const [runForm, setRunForm] = useState(emptyRunForm());
  const [running, setRunning] = useState(false);

  const fetchAll = async () => {
    setLoading(true);
    try {
      const [rulesRes, accountsRes, taxRes] = await Promise.all([
        financeApi.get<ApiResponse<PostingRule[]>>('/finance/posting/rules'),
        financeApi.get<ApiResponse<Account[]>>('/finance/accounts'),
        financeApi.get<ApiResponse<TaxRate[]>>('/finance/tax-rates').catch(() => ({ data: { data: [] as TaxRate[] } })),
      ]);
      setRules(rulesRes.data.data);
      setAccounts(accountsRes.data.data.filter(a => a.postingPermission === 'POSTABLE'));
      setTaxRates(taxRes.data.data);
    } catch {
      toast.error('Failed to fetch posting rules.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchAll(); }, []);

  const openCreate = () => { setEditingId(null); setForm(emptyForm()); setShowForm(true); };
  const openEdit = (rule: PostingRule) => {
    setEditingId(rule.id);
    setForm({
      triggerEvent: rule.triggerEvent,
      debitAccountCode: rule.debitAccountCode,
      creditAccountCode: rule.creditAccountCode,
      taxRuleCode: rule.taxRuleCode || '',
      costCenterId: rule.costCenterId ?? '',
      profitCenterId: rule.profitCenterId ?? '',
      isActive: rule.isActive,
    });
    setShowForm(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.debitAccountCode || !form.creditAccountCode) { toast.error('Debit and credit accounts are required.'); return; }
    setSubmitting(true);
    const payload = {
      triggerEvent: form.triggerEvent,
      debitAccountCode: form.debitAccountCode,
      creditAccountCode: form.creditAccountCode,
      taxRuleCode: form.taxRuleCode || null,
      costCenterId: form.costCenterId || null,
      profitCenterId: form.profitCenterId || null,
      isActive: form.isActive,
    };
    try {
      await toast.promise(
        editingId
          ? financeApi.put(`/finance/posting/rules/${editingId}`, payload)
          : financeApi.post('/finance/posting/rules', payload),
        {
          loading: editingId ? 'Updating posting rule...' : 'Creating posting rule...',
          success: editingId ? 'Posting rule updated.' : 'Posting rule created.',
          error: (err) => err.response?.data?.message || 'Failed to save posting rule.',
        }
      );
      setShowForm(false);
      setEditingId(null);
      setForm(emptyForm());
      await fetchAll();
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  const handleRun = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!runForm.triggerEvent || !runForm.amount || !runForm.currency) { toast.error('Trigger event, amount, and currency are required.'); return; }
    setRunning(true);
    try {
      await toast.promise(
        financeApi.post('/finance/posting/run', { ...runForm, amount: Number(runForm.amount) }),
        {
          loading: 'Running posting engine...',
          success: 'Posting run complete — check Journal Entries for the new entry.',
          error: (err) => err.response?.data?.message || 'Posting run failed — is there an active rule for this trigger event?',
        }
      );
      setShowRunForm(false);
      setRunForm(emptyRunForm());
    } catch {
      // toast.promise handles display
    } finally {
      setRunning(false);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white">
            <Workflow size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Posting Rules</h1>
            <p className="text-slate-500 text-sm font-medium">
              "When this event happens, debit A and credit B" — configured here, not hardcoded.
            </p>
          </div>
        </div>
        {canManage && (
          <div className="flex items-center gap-2">
            <button onClick={() => { setShowRunForm(!showRunForm); setShowForm(false); }}
              className="flex items-center gap-2 px-4 py-2.5 bg-white border border-slate-200 text-slate-700 text-sm font-black rounded-xl hover:border-blue-300 hover:text-blue-600 transition">
              <Zap size={16} /> Run Posting (Test)
            </button>
            <button onClick={openCreate}
              className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-sm font-black rounded-xl hover:bg-blue-700 transition shadow-lg shadow-blue-200">
              <PlusCircle size={16} /> New Posting Rule
            </button>
          </div>
        )}
      </div>

      {showRunForm && (
        <form onSubmit={handleRun} className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Run Manual Posting</h2>
          <p className="text-xs text-slate-500">
            Simulates a business event without needing the real source system — looks up the active rule for this trigger and posts a journal entry from it. Fails loudly if no active rule is configured for the chosen trigger.
          </p>
          <div className="grid grid-cols-4 gap-3">
            <select value={runForm.triggerEvent} onChange={e => setRunForm({ ...runForm, triggerEvent: e.target.value as PostingTriggerType })}
              className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
              {TRIGGER_EVENTS.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
            <input type="number" step="0.01" value={runForm.amount} onChange={e => setRunForm({ ...runForm, amount: e.target.value })}
              placeholder="Amount" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <input value={runForm.currency} onChange={e => setRunForm({ ...runForm, currency: e.target.value.toUpperCase() })}
              placeholder="Currency" maxLength={3} className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <input value={runForm.sourceDocRef} onChange={e => setRunForm({ ...runForm, sourceDocRef: e.target.value })}
              placeholder="Source Doc Ref (optional)" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => setShowRunForm(false)} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={running}
              className="flex items-center gap-1.5 px-5 py-2 bg-slate-900 text-white text-sm font-black rounded-xl disabled:opacity-60">
              <Zap size={14} /> Run
            </button>
          </div>
        </form>
      )}

      {showForm && (
        <form onSubmit={handleSubmit} className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">{editingId ? 'Edit Posting Rule' : 'New Posting Rule'}</h2>
          <div className="grid grid-cols-3 gap-3">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Trigger Event</label>
              <select value={form.triggerEvent} onChange={e => setForm({ ...form, triggerEvent: e.target.value as PostingTriggerType })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
                {TRIGGER_EVENTS.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Debit Account</label>
              <select value={form.debitAccountCode} onChange={e => setForm({ ...form, debitAccountCode: e.target.value })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
                <option value="">— Select —</option>
                {accounts.map(a => <option key={a.id} value={a.accountCode}>{a.accountCode} — {a.accountName}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Credit Account</label>
              <select value={form.creditAccountCode} onChange={e => setForm({ ...form, creditAccountCode: e.target.value })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
                <option value="">— Select —</option>
                {accounts.map(a => <option key={a.id} value={a.accountCode}>{a.accountCode} — {a.accountName}</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Tax Rule (optional)</label>
              <select value={form.taxRuleCode} onChange={e => setForm({ ...form, taxRuleCode: e.target.value })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
                <option value="">— None —</option>
                {taxRates.map(t => <option key={t.id} value={t.taxRuleCode}>{t.taxRuleCode} ({t.ratePercent}%)</option>)}
              </select>
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Cost Center Id (optional)</label>
              <input type="number" value={form.costCenterId} onChange={e => setForm({ ...form, costCenterId: e.target.value ? Number(e.target.value) : '' })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Profit Center Id (optional)</label>
              <input type="number" value={form.profitCenterId} onChange={e => setForm({ ...form, profitCenterId: e.target.value ? Number(e.target.value) : '' })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
          </div>
          <label className="flex items-center gap-2 text-xs font-bold text-slate-600">
            <input type="checkbox" checked={form.isActive} onChange={e => setForm({ ...form, isActive: e.target.checked })} />
            Active (only one active rule per trigger event is expected)
          </label>
          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => setShowForm(false)} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting}
              className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">
              {editingId ? 'Save Changes' : 'Create Posting Rule'}
            </button>
          </div>
        </form>
      )}

      <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="p-5 border-b border-slate-100">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Configured Rules</h2>
        </div>

        {loading ? (
          <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
        ) : rules.length === 0 ? (
          <div className="p-16 text-center text-slate-300">
            <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
            <p className="font-bold uppercase tracking-widest text-xs">No posting rules yet</p>
          </div>
        ) : (
          <div className="divide-y divide-slate-50">
            {rules.map(r => (
              <div key={r.id} className="p-5 flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-black text-slate-900 font-mono text-xs">{r.triggerEvent}</span>
                    <span className={`text-[10px] font-black px-2 py-0.5 rounded-full border ${r.isActive ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-100 text-slate-400 border-slate-200'}`}>
                      {r.isActive ? 'ACTIVE' : 'INACTIVE'}
                    </span>
                  </div>
                  <div className="text-[11px] text-slate-500 font-bold mt-0.5">
                    Debit <span className="font-mono">{r.debitAccountCode}</span> &rarr; Credit <span className="font-mono">{r.creditAccountCode}</span>
                    {r.taxRuleCode && <span> &middot; Tax: <span className="font-mono">{r.taxRuleCode}</span></span>}
                  </div>
                </div>
                {canManage && (
                  <button onClick={() => openEdit(r)}
                    className="flex items-center gap-1.5 text-[11px] font-black px-3 py-1.5 rounded-lg bg-white border border-slate-200 text-slate-600 hover:border-blue-300 hover:text-blue-600">
                    <Pencil size={12} /> Edit
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default PostingRulesPage;
