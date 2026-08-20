import React, { useEffect, useState } from 'react';
import financeApi from '../api/financeAxios';
import toast from 'react-hot-toast';
import { ListTree, Loader2, PlusCircle, PowerOff, ChevronRight } from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import { Account, AccountType, PostingPermission } from '../types/finance';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole, CAN_APPROVE_FINANCE } from '../utils/permissions';

const typeStyle: Record<AccountType, string> = {
  ASSET:     'bg-blue-50    text-blue-700    border-blue-200',
  LIABILITY: 'bg-red-50     text-red-700     border-red-200',
  EQUITY:    'bg-purple-50  text-purple-700  border-purple-200',
  REVENUE:   'bg-emerald-50 text-emerald-700 border-emerald-200',
  EXPENSE:   'bg-amber-50   text-amber-700   border-amber-200',
};

const emptyForm = () => ({
  accountCode: '', accountName: '', accountType: 'ASSET' as AccountType,
  parentAccountId: '' as number | '', postingPermission: 'POSTABLE' as PostingPermission,
});

// Flattens the tree into "code — name" options for the parent-account picker.
const flatten = (nodes: Account[], depth = 0): { account: Account; depth: number }[] =>
  nodes.flatMap(n => [{ account: n, depth }, ...flatten(n.children || [], depth + 1)]);

const ChartOfAccountsPage = () => {
  const { user } = useAuth();
  const canManage = hasAnyRole(user?.roles, CAN_APPROVE_FINANCE);
  const [tree, setTree] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(emptyForm());
  const [submitting, setSubmitting] = useState(false);

  const fetchTree = async () => {
    setLoading(true);
    try {
      const res = await financeApi.get<ApiResponse<Account[]>>('/finance/accounts/tree');
      setTree(res.data.data);
    } catch {
      toast.error('Failed to fetch Chart of Accounts.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchTree(); }, []);

  const flatRows = flatten(tree);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.accountCode || !form.accountName) { toast.error('Account code and name are required.'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        financeApi.post('/finance/accounts', {
          accountCode: form.accountCode,
          accountName: form.accountName,
          accountType: form.accountType,
          parentAccountId: form.parentAccountId || null,
          postingPermission: form.postingPermission,
        }),
        { loading: 'Creating account...', success: 'Account created.', error: (err) => err.response?.data?.message || 'Failed to create account.' }
      );
      setShowForm(false);
      setForm(emptyForm());
      await fetchTree();
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  const deactivate = async (account: Account) => {
    try {
      await toast.promise(
        financeApi.patch(`/finance/accounts/${account.id}/deactivate`, {}),
        {
          loading: `Deactivating ${account.accountCode}...`,
          success: `${account.accountCode} deactivated.`,
          error: (err) => err.response?.data?.message || 'Could not deactivate this account.',
        }
      );
      await fetchTree();
    } catch {
      // toast.promise handles display — most likely a "has children" or "has postings" guard
    }
  };

  const renderRow = (account: Account, depth: number): React.ReactNode => (
    <React.Fragment key={account.id}>
      <div className="grid grid-cols-12 gap-2 items-center px-5 py-3 hover:bg-slate-50 border-b border-slate-50 last:border-b-0">
        <div className="col-span-5 flex items-center gap-1.5" style={{ paddingLeft: depth * 20 }}>
          {depth > 0 && <ChevronRight size={13} className="text-slate-300 flex-shrink-0" />}
          <span className="font-mono text-xs font-black text-slate-800">{account.accountCode}</span>
          <span className="text-sm text-slate-600 font-medium truncate">{account.accountName}</span>
        </div>
        <div className="col-span-2">
          <span className={`text-[10px] font-black px-2 py-0.5 rounded-full border ${typeStyle[account.accountType]}`}>{account.accountType}</span>
        </div>
        <div className="col-span-2">
          <span className="text-[10px] font-black px-2 py-0.5 rounded-full border bg-slate-100 text-slate-600 border-slate-200">
            {account.postingPermission === 'HEADER_ONLY' ? 'HEADER ONLY' : 'POSTABLE'}
          </span>
        </div>
        <div className="col-span-1">
          <span className={`text-[10px] font-black ${account.activeStatus ? 'text-emerald-600' : 'text-slate-300'}`}>
            {account.activeStatus ? 'ACTIVE' : 'INACTIVE'}
          </span>
        </div>
        <div className="col-span-2 flex justify-end">
          {canManage && account.activeStatus && (
            <button onClick={() => deactivate(account)}
              className="flex items-center gap-1 text-[10px] font-black px-2.5 py-1 rounded-full bg-white border border-slate-200 text-slate-500 hover:border-red-300 hover:text-red-500 transition">
              <PowerOff size={11} /> Deactivate
            </button>
          )}
        </div>
      </div>
      {(account.children || []).map(child => renderRow(child, depth + 1))}
    </React.Fragment>
  );

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white">
            <ListTree size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Chart of Accounts</h1>
            <p className="text-slate-500 text-sm font-medium">
              The master list of buckets money can be recorded against — nested into a tree.
            </p>
          </div>
        </div>
        {canManage && (
          <button onClick={() => { setShowForm(!showForm); setForm(emptyForm()); }}
            className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-sm font-black rounded-xl hover:bg-blue-700 transition shadow-lg shadow-blue-200">
            <PlusCircle size={16} /> New Account
          </button>
        )}
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">New Account</h2>
          <div className="grid grid-cols-4 gap-3">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Account Code</label>
              <input value={form.accountCode} onChange={e => setForm({ ...form, accountCode: e.target.value })}
                placeholder="e.g. 6000" className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div className="col-span-2">
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Account Name</label>
              <input value={form.accountName} onChange={e => setForm({ ...form, accountName: e.target.value })}
                placeholder="e.g. Freight Expense" className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Type</label>
              <select value={form.accountType} onChange={e => setForm({ ...form, accountType: e.target.value as AccountType })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
                {(['ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE'] as AccountType[]).map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div className="col-span-2">
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Parent Account (optional)</label>
              <select value={form.parentAccountId} onChange={e => setForm({ ...form, parentAccountId: e.target.value ? Number(e.target.value) : '' })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
                <option value="">— No parent (root account) —</option>
                {flatRows.map(({ account, depth }) => (
                  <option key={account.id} value={account.id}>{'—'.repeat(depth)} {account.accountCode} — {account.accountName}</option>
                ))}
              </select>
            </div>
            <div className="col-span-2">
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Posting Permission</label>
              <select value={form.postingPermission} onChange={e => setForm({ ...form, postingPermission: e.target.value as PostingPermission })}
                className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
                <option value="POSTABLE">POSTABLE — journal lines can be posted to it</option>
                <option value="HEADER_ONLY">HEADER ONLY — group node, organizes the tree only</option>
              </select>
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => setShowForm(false)} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting}
              className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">
              Create Account
            </button>
          </div>
        </form>
      )}

      <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="p-5 border-b border-slate-100 grid grid-cols-12 gap-2">
          <span className="col-span-5 text-[10px] font-black text-slate-400 uppercase tracking-widest">Account</span>
          <span className="col-span-2 text-[10px] font-black text-slate-400 uppercase tracking-widest">Type</span>
          <span className="col-span-2 text-[10px] font-black text-slate-400 uppercase tracking-widest">Posting</span>
          <span className="col-span-1 text-[10px] font-black text-slate-400 uppercase tracking-widest">Status</span>
          <span className="col-span-2"></span>
        </div>

        {loading ? (
          <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
        ) : tree.length === 0 ? (
          <div className="p-16 text-center text-slate-300">
            <ListTree size={48} className="mx-auto mb-3 opacity-30" />
            <p className="font-bold uppercase tracking-widest text-xs">No accounts yet</p>
          </div>
        ) : (
          <div>{tree.map(a => renderRow(a, 0))}</div>
        )}
      </div>
    </div>
  );
};

export default ChartOfAccountsPage;
