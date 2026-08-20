import React, { useEffect, useState } from 'react';
import financeApi from '../api/financeAxios';
import toast from 'react-hot-toast';
import { BookOpen, Loader2, ChevronRight, CheckCircle2, PlusCircle, Plus, Trash2 } from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import { Account, JournalEntry, JournalEntryCreatePayload, JournalLine, Supplier, Customer } from '../types/finance';
import { JournalEntryDetailModal } from '../components/JournalEntryDetailModal';
import { Dropdown } from '../components/Dropdown';
import { fetchSuppliers } from '../api/financeSuppliers';
import { fetchCustomers } from '../api/financeCustomers';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole, CAN_MANAGE_FINANCE } from '../utils/permissions';

const statusStyle: Record<string, string> = {
  DRAFT:            'bg-slate-100  text-slate-600  border-slate-200',
  PENDING_APPROVAL: 'bg-amber-50   text-amber-700  border-amber-200',
  REJECTED:         'bg-red-50     text-red-700    border-red-200',
  POSTED:           'bg-emerald-50 text-emerald-700 border-emerald-200',
};

const emptyLine: JournalLine = { accountCode: '', debitAmount: 0, creditAmount: 0, currency: 'INR' };

const emptyForm = (): JournalEntryCreatePayload => ({
  postingDate: new Date().toISOString().slice(0, 10),
  narration: '', sourceDocType: 'MANUAL', sourceDocRef: '',
  lines: [{ ...emptyLine }, { ...emptyLine }],
});

const JournalEntriesPage = () => {
  const { user } = useAuth();
  const canManage = hasAnyRole(user?.roles, CAN_MANAGE_FINANCE);
  const [journals, setJournals] = useState<JournalEntry[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [suppliersLoading, setSuppliersLoading] = useState(true);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [customersLoading, setCustomersLoading] = useState(true);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<JournalEntry | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<JournalEntryCreatePayload>(emptyForm());
  const [submitting, setSubmitting] = useState(false);

  const fetchJournals = async () => {
    setLoading(true);
    try {
      const res = await financeApi.get<ApiResponse<JournalEntry[]>>('/finance/journals');
      setJournals(res.data.data);
      if (selected) {
        const fresh = res.data.data.find(j => j.id === selected.id);
        setSelected(fresh || null);
      }
    } catch {
      toast.error('Failed to fetch journal entries.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchJournals(); }, []); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => {
    financeApi.get<ApiResponse<Account[]>>('/finance/accounts')
      .then(res => setAccounts(res.data.data.filter(a => a.postingPermission === 'POSTABLE'))).catch(() => {});
  }, []);
  useEffect(() => {
    fetchSuppliers().then(setSuppliers).catch(() => {}).finally(() => setSuppliersLoading(false));
  }, []);
  useEffect(() => {
    fetchCustomers().then(setCustomers).catch(() => {}).finally(() => setCustomersLoading(false));
  }, []);

  const updateLine = (idx: number, patch: Partial<JournalLine>) => {
    setForm(prev => ({ ...prev, lines: prev.lines.map((l, i) => i === idx ? { ...l, ...patch } : l) }));
  };
  const addLine = () => setForm(prev => ({ ...prev, lines: [...prev.lines, { ...emptyLine }] }));
  const removeLine = (idx: number) => setForm(prev => ({ ...prev, lines: prev.lines.filter((_, i) => i !== idx) }));

  const totalDebit = form.lines.reduce((s, l) => s + (Number(l.debitAmount) || 0), 0);
  const totalCredit = form.lines.reduce((s, l) => s + (Number(l.creditAmount) || 0), 0);
  const isBalanced = Math.abs(totalDebit - totalCredit) < 0.005 && totalDebit > 0;

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (form.lines.some(l => !l.accountCode)) { toast.error('Every line needs an account.'); return; }
    if (!isBalanced) { toast.error('Debits must equal credits before this can be saved.'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        financeApi.post('/finance/journals', form),
        { loading: 'Creating journal entry...', success: 'Journal entry created.', error: (err) => err.response?.data?.message || 'Failed to create journal entry.' }
      );
      setShowForm(false);
      setForm(emptyForm());
      await fetchJournals();
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white">
            <BookOpen size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Journal Entries</h1>
            <p className="text-slate-500 text-sm font-medium">
              Every posting to the General Ledger — debits must equal credits, and a POSTED entry never changes.
            </p>
          </div>
        </div>
        {canManage && (
          <button onClick={() => { setShowForm(!showForm); setForm(emptyForm()); }}
            className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-sm font-black rounded-xl hover:bg-blue-700 transition shadow-lg shadow-blue-200">
            <PlusCircle size={16} /> New Journal Entry
          </button>
        )}
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">New Journal Entry</h2>

          <div className="grid grid-cols-4 gap-3">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Posting Date</label>
              <input type="date" value={form.postingDate} onChange={e => setForm({ ...form, postingDate: e.target.value })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div className="col-span-2">
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Narration</label>
              <input value={form.narration} onChange={e => setForm({ ...form, narration: e.target.value })}
                placeholder="What is this entry for?"
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Source Doc Ref</label>
              <input value={form.sourceDocRef} onChange={e => setForm({ ...form, sourceDocRef: e.target.value })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
          </div>

          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Lines</label>
              <button type="button" onClick={addLine}
                className="flex items-center gap-1 text-[10px] font-black px-3 py-1 rounded-full bg-slate-900 text-white">
                <Plus size={12} /> Add Line
              </button>
            </div>
            <div className="space-y-2">
              {form.lines.map((l, i) => (
                <div key={i} className="space-y-2 bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <div className="grid grid-cols-12 gap-2 items-center">
                    <select value={l.accountCode} onChange={e => updateLine(i, { accountCode: e.target.value })}
                      className="col-span-4 px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs">
                      <option value="">— Account —</option>
                      {accounts.map(a => <option key={a.id} value={a.accountCode}>{a.accountCode} — {a.accountName}</option>)}
                    </select>
                    <input type="number" min={0} step="0.01" placeholder="Debit" value={l.debitAmount || ''}
                      onChange={e => updateLine(i, { debitAmount: Number(e.target.value), creditAmount: 0 })}
                      className="col-span-2 px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                    <input type="number" min={0} step="0.01" placeholder="Credit" value={l.creditAmount || ''}
                      onChange={e => updateLine(i, { creditAmount: Number(e.target.value), debitAmount: 0 })}
                      className="col-span-2 px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                    <input value={l.currency} onChange={e => updateLine(i, { currency: e.target.value.toUpperCase() })}
                      placeholder="Currency" maxLength={3}
                      className="col-span-2 px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                    <button type="button" onClick={() => removeLine(i)} disabled={form.lines.length === 2}
                      className="col-span-2 flex items-center justify-center text-red-500 disabled:opacity-30">
                      <Trash2 size={14} />
                    </button>
                  </div>
                  <div className="grid grid-cols-12 gap-2 items-center">
                    <div className="col-span-4">
                      <Dropdown<number>
                        options={suppliers.map(s => ({ value: s.id, label: s.supplierName, sublabel: s.vendorCode }))}
                        value={l.supplierId ?? ''}
                        loading={suppliersLoading}
                        placeholder="— Supplier (AP) —"
                        emptyMessage="No suppliers synced yet"
                        onChange={(supplierId) => updateLine(i, {
                          supplierId, customerId: undefined,
                          counterpartyRef: l.counterpartyRef || `SUPPLIER:${supplierId}`,
                        })}
                        className="w-full px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs"
                      />
                    </div>
                    <div className="col-span-4">
                      <Dropdown<number>
                        options={customers.map(c => ({ value: c.id, label: c.customerName, sublabel: c.customerCode }))}
                        value={l.customerId ?? ''}
                        loading={customersLoading}
                        placeholder="— Customer (AR) —"
                        emptyMessage="No customers synced yet"
                        onChange={(customerId) => updateLine(i, {
                          customerId, supplierId: undefined,
                          counterpartyRef: l.counterpartyRef || `CUSTOMER:${customerId}`,
                        })}
                        className="w-full px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs"
                      />
                    </div>
                    <input value={l.counterpartyRef || ''} onChange={e => updateLine(i, { counterpartyRef: e.target.value })}
                      placeholder="Counterparty (optional)"
                      className="col-span-4 px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                  </div>
                </div>
              ))}
            </div>
            <div className={`mt-3 flex items-center justify-end gap-4 text-xs font-black ${isBalanced ? 'text-emerald-600' : 'text-red-500'}`}>
              <span>Debit: {totalDebit.toFixed(2)}</span>
              <span>Credit: {totalCredit.toFixed(2)}</span>
              <span>{isBalanced ? 'Balanced' : 'Not balanced'}</span>
            </div>
          </div>

          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => setShowForm(false)} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting || !isBalanced}
              className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">
              Create Journal Entry
            </button>
          </div>
        </form>
      )}

      <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="p-5 border-b border-slate-100">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Journal Register</h2>
        </div>

        {loading ? (
          <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
        ) : journals.length === 0 ? (
          <div className="p-16 text-center text-slate-300">
            <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
            <p className="font-bold uppercase tracking-widest text-xs">No journal entries yet</p>
          </div>
        ) : (
          <div className="divide-y divide-slate-50">
            {journals.map(j => (
              <button key={j.id} onClick={() => setSelected(j)}
                className="w-full text-left p-5 hover:bg-slate-50 transition-all group flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-black text-slate-900 group-hover:text-blue-600 transition-colors font-mono">{j.journalNumber}</span>
                  </div>
                  <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">
                    {j.postingDate} · {j.narration || j.sourceDocType || 'MANUAL'}
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className={`text-[10px] font-black px-2.5 py-1 rounded-full border ${statusStyle[j.status] || ''}`}>{j.status.replace(/_/g, ' ')}</span>
                  <ChevronRight size={16} className="text-slate-300 group-hover:text-blue-500 transition-colors" />
                </div>
              </button>
            ))}
          </div>
        )}
      </div>

      <JournalEntryDetailModal journal={selected} isOpen={!!selected} onClose={() => setSelected(null)} onSuccess={fetchJournals} accounts={accounts} />
    </div>
  );
};

export default JournalEntriesPage;
