import React, { useState } from 'react';
import { X, Send, Check, XCircle, RotateCcw, Pencil, Plus, Trash2, Save } from 'lucide-react';
import financeApi from '../api/financeAxios';
import toast from 'react-hot-toast';
import { JournalEntry, JournalLine, Account } from '../types/finance';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole, CAN_APPROVE_FINANCE, CAN_MANAGE_FINANCE } from '../utils/permissions';

interface Props {
  journal: JournalEntry | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void; // refetch the list after any state-changing action
  accounts?: Account[]; // POSTABLE accounts, needed only while editing a DRAFT
}

const statusStyle: Record<string, string> = {
  DRAFT:            'bg-slate-100  text-slate-600  border-slate-200',
  PENDING_APPROVAL: 'bg-amber-50   text-amber-700  border-amber-200',
  REJECTED:         'bg-red-50     text-red-700    border-red-200',
  POSTED:           'bg-emerald-50 text-emerald-700 border-emerald-200',
};

const emptyLine: JournalLine = { accountCode: '', debitAmount: 0, creditAmount: 0, currency: 'INR' };

export const JournalEntryDetailModal: React.FC<Props> = ({ journal, isOpen, onClose, onSuccess, accounts = [] }) => {
  const { user } = useAuth();
  const canApprove = hasAnyRole(user?.roles, CAN_APPROVE_FINANCE);
  const canEdit = hasAnyRole(user?.roles, CAN_MANAGE_FINANCE);
  const [acting, setActing] = useState(false);
  const [comments, setComments] = useState('');
  const [reversing, setReversing] = useState(false);
  const [reason, setReason] = useState('');

  const [editing, setEditing] = useState(false);
  const [editNarration, setEditNarration] = useState('');
  const [editPostingDate, setEditPostingDate] = useState('');
  const [editLines, setEditLines] = useState<JournalLine[]>([]);

  if (!isOpen || !journal) return null;

  const totalDebit = journal.lines.reduce((s, l) => s + l.debitAmount, 0);
  const totalCredit = journal.lines.reduce((s, l) => s + l.creditAmount, 0);
  const isBalanced = Math.abs(totalDebit - totalCredit) < 0.005;

  const startEdit = () => {
    setEditNarration(journal.narration || '');
    setEditPostingDate(journal.postingDate);
    setEditLines(journal.lines.map(l => ({ ...l })));
    setEditing(true);
  };

  const updateEditLine = (idx: number, patch: Partial<JournalLine>) => {
    setEditLines(prev => prev.map((l, i) => i === idx ? { ...l, ...patch } : l));
  };
  const addEditLine = () => setEditLines(prev => [...prev, { ...emptyLine }]);
  const removeEditLine = (idx: number) => setEditLines(prev => prev.filter((_, i) => i !== idx));

  const editTotalDebit = editLines.reduce((s, l) => s + (Number(l.debitAmount) || 0), 0);
  const editTotalCredit = editLines.reduce((s, l) => s + (Number(l.creditAmount) || 0), 0);
  const editIsBalanced = Math.abs(editTotalDebit - editTotalCredit) < 0.005 && editTotalDebit > 0;

  const saveEdit = async () => {
    if (editLines.some(l => !l.accountCode)) { toast.error('Every line needs an account.'); return; }
    if (!editIsBalanced) { toast.error('Debits must equal credits before this can be saved.'); return; }
    setActing(true);
    try {
      await toast.promise(
        financeApi.put(`/finance/journals/${journal.id}`, {
          postingDate: editPostingDate,
          narration: editNarration,
          lines: editLines,
        }),
        { loading: 'Saving changes...', success: 'Journal entry updated.', error: (err) => err.response?.data?.message || 'Failed to save changes.' }
      );
      setEditing(false);
      onSuccess();
    } catch {
      // toast.promise handles display
    } finally {
      setActing(false);
    }
  };

  const runAction = async (action: () => Promise<any>, successMsg: string) => {
    setActing(true);
    try {
      await toast.promise(action(), {
        loading: 'Working...',
        success: successMsg,
        error: (err) => err.response?.data?.message || 'Action failed.',
      });
      onSuccess();
    } catch {
      // toast.promise handles display
    } finally {
      setActing(false);
    }
  };

  const submit = () => runAction(
    () => financeApi.post(`/finance/journals/${journal.id}/submit`, {}),
    'Journal entry submitted.'
  );
  const approve = () => runAction(
    () => financeApi.post(`/finance/journals/${journal.id}/approve`, { comments }),
    'Journal entry approved.'
  );
  const reject = () => runAction(
    () => financeApi.post(`/finance/journals/${journal.id}/reject`, { comments }),
    'Journal entry rejected.'
  );
  const reverse = () => {
    if (!reason.trim()) { toast.error('A reason is required to post a reversing entry.'); return; }
    runAction(
      () => financeApi.post(`/finance/journals/${journal.id}/reverse`, { reason }),
      'Reversing journal entry posted.'
    );
    setReversing(false);
    setReason('');
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">

        {/* Header */}
        <div className="px-8 py-6 bg-slate-900 text-white flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <span className="font-mono text-blue-400 font-black text-lg">{journal.journalNumber}</span>
              <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-black border ${statusStyle[journal.status] || ''}`}>
                {journal.status.replace(/_/g, ' ')}
              </span>
            </div>
            <p className="text-slate-300 text-sm font-medium">{journal.narration || '—'}</p>
            <div className="flex items-center gap-4 mt-2 flex-wrap text-[11px] text-slate-400 font-bold">
              <span>{journal.postingDate}</span>
              <span>{journal.sourceDocType || 'MANUAL'} {journal.sourceDocRef ? `· ${journal.sourceDocRef}` : ''}</span>
              <span>Created by {journal.createdBy}</span>
            </div>
          </div>
          <button onClick={() => { setEditing(false); onClose(); }} className="p-2 hover:bg-white/10 rounded-xl transition text-slate-400">
            <X size={20} />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto px-8 py-6 space-y-6">

          {!editing ? (
            <>
              {/* Lines (read-only) */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Lines</p>
                  {journal.status === 'DRAFT' && canEdit && (
                    <button onClick={startEdit}
                      className="flex items-center gap-1.5 text-[10px] font-black px-3 py-1 rounded-full bg-white border border-slate-200 text-slate-600 hover:border-blue-300 hover:text-blue-600">
                      <Pencil size={11} /> Edit
                    </button>
                  )}
                </div>
                <div className="border border-slate-100 rounded-2xl overflow-hidden">
                  <div className="divide-y divide-slate-50">
                    {journal.lines.map((l, i) => (
                      <div key={i} className="p-3 grid grid-cols-5 gap-2 text-xs items-center">
                        <div className="col-span-2 font-bold text-slate-800">{l.accountCode}</div>
                        <div className="text-right text-slate-600">{l.debitAmount ? l.debitAmount.toFixed(2) : '—'}</div>
                        <div className="text-right text-slate-600">{l.creditAmount ? l.creditAmount.toFixed(2) : '—'}</div>
                        <div className="text-right text-slate-400">{l.currency}{l.counterpartyRef ? ` · ${l.counterpartyRef}` : ''}</div>
                      </div>
                    ))}
                  </div>
                  <div className={`p-3 grid grid-cols-5 gap-2 text-xs font-black bg-slate-50 ${isBalanced ? 'text-emerald-600' : 'text-red-500'}`}>
                    <div className="col-span-2">Total</div>
                    <div className="text-right">{totalDebit.toFixed(2)}</div>
                    <div className="text-right">{totalCredit.toFixed(2)}</div>
                    <div className="text-right">{isBalanced ? 'Balanced' : 'NOT BALANCED'}</div>
                  </div>
                </div>
              </div>

              {(journal.reversalOfJournalId || journal.reversedByJournalId) && (
                <p className="text-[11px] text-amber-600 font-bold">
                  {journal.reversalOfJournalId && `This entry reverses journal #${journal.reversalOfJournalId}. `}
                  {journal.reversedByJournalId && `This entry was reversed by journal #${journal.reversedByJournalId}.`}
                </p>
              )}

              {/* Action bar */}
              <div className="border-t border-slate-100 pt-4">
                <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest block mb-3">Actions</span>

                {journal.status === 'DRAFT' && (
                  <button disabled={acting} onClick={submit}
                    className="flex items-center gap-1.5 px-4 py-1.5 rounded-xl text-xs font-black shadow-lg transition-all active:scale-95 bg-blue-600 hover:bg-blue-700 text-white shadow-blue-200">
                    <Send size={13} /> Submit for Approval
                  </button>
                )}

                {journal.status === 'PENDING_APPROVAL' && canApprove && (
                  <div className="space-y-2">
                    <textarea value={comments} onChange={e => setComments(e.target.value)}
                      placeholder="Comments (optional)" rows={2}
                      className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs resize-none" />
                    <div className="flex gap-2">
                      <button disabled={acting} onClick={approve}
                        className="flex items-center gap-1.5 px-4 py-1.5 rounded-xl text-xs font-black shadow-lg transition-all active:scale-95 bg-emerald-600 hover:bg-emerald-700 text-white shadow-emerald-200">
                        <Check size={13} /> Approve
                      </button>
                      <button disabled={acting} onClick={reject}
                        className="flex items-center gap-1.5 px-4 py-1.5 rounded-xl text-xs font-black shadow-lg transition-all active:scale-95 bg-red-500 hover:bg-red-600 text-white shadow-red-200">
                        <XCircle size={13} /> Reject
                      </button>
                    </div>
                  </div>
                )}

                {journal.status === 'POSTED' && canApprove && !journal.reversedByJournalId && (
                  reversing ? (
                    <div className="space-y-2">
                      <input value={reason} onChange={e => setReason(e.target.value)}
                        placeholder="Reason for reversal" className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs" />
                      <div className="flex gap-2">
                        <button disabled={acting} onClick={reverse}
                          className="flex items-center gap-1.5 px-4 py-1.5 rounded-xl text-xs font-black bg-slate-900 text-white hover:bg-slate-800 disabled:opacity-60">
                          <RotateCcw size={13} /> Post Reversing Entry
                        </button>
                        <button onClick={() => setReversing(false)} className="px-4 py-1.5 text-xs font-bold text-slate-500">Cancel</button>
                      </div>
                    </div>
                  ) : (
                    <button onClick={() => setReversing(true)}
                      className="flex items-center gap-1.5 px-4 py-1.5 rounded-xl text-xs font-black bg-white border border-slate-300 text-slate-700 hover:bg-slate-50">
                      <RotateCcw size={13} /> Reverse This Entry
                    </button>
                  )
                )}

                {!['DRAFT', 'PENDING_APPROVAL'].includes(journal.status) && !(journal.status === 'POSTED' && canApprove) && (
                  <span className="text-[11px] text-slate-400 font-bold">No actions available right now.</span>
                )}
              </div>
            </>
          ) : (
            <>
              {/* Edit mode — only reachable for DRAFT entries */}
              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Posting Date</label>
                  <input type="date" value={editPostingDate} onChange={e => setEditPostingDate(e.target.value)}
                    className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                </div>
                <div className="col-span-2">
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Narration</label>
                  <input value={editNarration} onChange={e => setEditNarration(e.target.value)}
                    className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                </div>
              </div>

              <div>
                <div className="flex items-center justify-between mb-2">
                  <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">Lines</label>
                  <button type="button" onClick={addEditLine}
                    className="flex items-center gap-1 text-[10px] font-black px-3 py-1 rounded-full bg-slate-900 text-white">
                    <Plus size={12} /> Add Line
                  </button>
                </div>
                <div className="space-y-2">
                  {editLines.map((l, i) => (
                    <div key={i} className="grid grid-cols-12 gap-2 items-center bg-slate-50 p-3 rounded-xl border border-slate-100">
                      <select value={l.accountCode} onChange={e => updateEditLine(i, { accountCode: e.target.value })}
                        className="col-span-5 px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs">
                        <option value="">— Account —</option>
                        {accounts.map(a => <option key={a.id} value={a.accountCode}>{a.accountCode} — {a.accountName}</option>)}
                      </select>
                      <input type="number" min={0} step="0.01" placeholder="Debit" value={l.debitAmount || ''}
                        onChange={e => updateEditLine(i, { debitAmount: Number(e.target.value), creditAmount: 0 })}
                        className="col-span-2 px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                      <input type="number" min={0} step="0.01" placeholder="Credit" value={l.creditAmount || ''}
                        onChange={e => updateEditLine(i, { creditAmount: Number(e.target.value), debitAmount: 0 })}
                        className="col-span-2 px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                      <input value={l.currency} onChange={e => updateEditLine(i, { currency: e.target.value.toUpperCase() })}
                        placeholder="Currency" maxLength={3}
                        className="col-span-1 px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                      <input value={l.counterpartyRef || ''} onChange={e => updateEditLine(i, { counterpartyRef: e.target.value })}
                        placeholder="Counterparty"
                        className="col-span-1 px-2 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                      <button type="button" onClick={() => removeEditLine(i)} disabled={editLines.length === 2}
                        className="col-span-1 flex items-center justify-center text-red-500 disabled:opacity-30">
                        <Trash2 size={14} />
                      </button>
                    </div>
                  ))}
                </div>
                <div className={`mt-3 flex items-center justify-end gap-4 text-xs font-black ${editIsBalanced ? 'text-emerald-600' : 'text-red-500'}`}>
                  <span>Debit: {editTotalDebit.toFixed(2)}</span>
                  <span>Credit: {editTotalCredit.toFixed(2)}</span>
                  <span>{editIsBalanced ? 'Balanced' : 'Not balanced'}</span>
                </div>
              </div>

              <div className="flex justify-end gap-2 border-t border-slate-100 pt-4">
                <button type="button" onClick={() => setEditing(false)} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
                <button type="button" disabled={acting || !editIsBalanced} onClick={saveEdit}
                  className="flex items-center gap-1.5 px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">
                  <Save size={14} /> Save Changes
                </button>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
