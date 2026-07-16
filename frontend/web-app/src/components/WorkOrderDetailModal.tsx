import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  X, Clock, Package, CheckCircle2, XCircle, PlayCircle,
  PauseCircle, Plane, MapPin, User, Calendar, ChevronRight,
  Loader2, History, AlertTriangle, ShieldCheck, Edit3, Trash2
} from 'lucide-react';
import workOrderApi from '../api/workOrderAxios';
import procurementApi from '../api/procurementAxios';
import productionApi from '../api/productionAxios';
import { OrganizationProfile, PurchaseOrderResponse } from '../types/procurement';
import { MaterialIssueRecord, Part as PartType } from '../types/inventory';
import { ProductionOrder } from '../types/production';
import {
  WorkOrder, WorkOrderStatus, StatusTransitionRequest,
  StatusHistoryEntry, ApiResponse, MaterialLineItem
} from '../types/workOrder';
import toast from 'react-hot-toast';
import api from '../api/axios';
import { ReceiptModal } from './ReceiptModal';
import { MaterialIssueReceiptDocument } from './MaterialIssueReceiptDocument';
import { IssueMaterialPickerModal } from './IssueMaterialPickerModal';

interface Props {
  workOrder: WorkOrder | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  onEdit: (wo: WorkOrder) => void;
  onDelete: (wo: WorkOrder) => void;
}

const statusStyle: Record<WorkOrderStatus, string> = {
  DRAFT:       'bg-slate-100  text-slate-600  border-slate-200',
  OPEN:        'bg-blue-50    text-blue-700   border-blue-200',
  IN_PROGRESS: 'bg-amber-50   text-amber-700  border-amber-200',
  ON_HOLD:     'bg-orange-50  text-orange-700 border-orange-200',
  COMPLETED:   'bg-emerald-50 text-emerald-700 border-emerald-200',
  CANCELLED:   'bg-red-50     text-red-700    border-red-200',
};

const NEXT_STATUSES: Record<WorkOrderStatus, WorkOrderStatus[]> = {
  DRAFT:       ['OPEN', 'CANCELLED'],
  OPEN:        ['IN_PROGRESS', 'ON_HOLD', 'CANCELLED'],
  IN_PROGRESS: ['ON_HOLD', 'COMPLETED', 'CANCELLED'],
  ON_HOLD:     ['IN_PROGRESS', 'CANCELLED'],
  COMPLETED:   [],
  CANCELLED:   [],
};

const transitionIcon: Record<WorkOrderStatus, React.ReactNode> = {
  OPEN:        <PlayCircle size={14} />,
  IN_PROGRESS: <PlayCircle size={14} />,
  ON_HOLD:     <PauseCircle size={14} />,
  COMPLETED:   <CheckCircle2 size={14} />,
  CANCELLED:   <XCircle size={14} />,
  DRAFT:       <Clock size={14} />,
};

const transitionColor: Record<WorkOrderStatus, string> = {
  OPEN:        'bg-blue-600 hover:bg-blue-700 text-white shadow-blue-200',
  IN_PROGRESS: 'bg-amber-500 hover:bg-amber-600 text-white shadow-amber-200',
  ON_HOLD:     'bg-orange-500 hover:bg-orange-600 text-white shadow-orange-200',
  COMPLETED:   'bg-emerald-600 hover:bg-emerald-700 text-white shadow-emerald-200',
  CANCELLED:   'bg-red-500 hover:bg-red-600 text-white shadow-red-200',
  DRAFT:       'bg-slate-600 hover:bg-slate-700 text-white shadow-slate-200',
};

const lineStatusStyle: Record<string, string> = {
  PENDING:   'bg-amber-50 text-amber-700 border-amber-200',
  PARTIALLY_ISSUED: 'bg-orange-50 text-orange-700 border-orange-200',
  ISSUED:    'bg-emerald-50 text-emerald-700 border-emerald-200',
  RETURNED:  'bg-blue-50 text-blue-700 border-blue-200',
  CANCELLED: 'bg-red-50 text-red-700 border-red-200',
};

export const WorkOrderDetailModal: React.FC<Props> = ({ workOrder, isOpen, onClose, onSuccess, onEdit, onDelete }) => {
  const navigate = useNavigate();
  const [history, setHistory] = useState<StatusHistoryEntry[]>([]);
  const [activeTab, setActiveTab] = useState<'details' | 'material' | 'history'>('details');
  const [transitioning, setTransitioning] = useState(false);
  const [transitionRemarks, setTransitionRemarks] = useState('');
  const [inventoryLevels, setInventoryLevels] = useState<{ [partId: number]: { qty: number; batch: string; skuCode: string; pendingQa: number } }>({});
  const [partsByPartId, setPartsByPartId] = useState<Record<number, PartType>>({});
  const [issuePickerLine, setIssuePickerLine] = useState<MaterialLineItem | null>(null);
  const [linkedPOs, setLinkedPOs] = useState<PurchaseOrderResponse[]>([]);
  const [orgProfile, setOrgProfile] = useState<OrganizationProfile | null>(null);
  const [issueReceipt, setIssueReceipt] = useState<MaterialIssueRecord | null>(null);
  const [linkedProductionOrders, setLinkedProductionOrders] = useState<ProductionOrder[]>([]);
  const [pastIssuesByLine, setPastIssuesByLine] = useState<Record<number, MaterialIssueRecord[]>>({});

  useEffect(() => {
    if (isOpen) {
      procurementApi.get<OrganizationProfile>('/organization-profile')
        .then(res => setOrgProfile(res.data)).catch(() => setOrgProfile(null));
    }
  }, [isOpen]);

  useEffect(() => {
    if (workOrder && isOpen) {
      productionApi.get(`/production/by-work-order/${workOrder.id}`)
        .then(res => setLinkedProductionOrders(res.data.data || []))
        .catch(() => setLinkedProductionOrders([]));
    }
  }, [workOrder, isOpen]);

  useEffect(() => {
    if (workOrder && isOpen) {
      workOrderApi.get<ApiResponse<StatusHistoryEntry[]>>(
        `/work-orders/${workOrder.id}/history`
      ).then(r => setHistory(r.data.data)).catch(() => setHistory([]));
    }
  }, [workOrder, isOpen]);

  useEffect(() => {
    if (workOrder && activeTab === 'material') {
      workOrder.materialLineItems.forEach(line => {
        if (line.partId) {
          api.get(`/inventory/${line.partId}`).then(res => {
            const data = res.data.data;
            setInventoryLevels(prev => ({
              ...prev,
              [line.partId]: {
                qty: data.quantity, batch: data.batchNumber, skuCode: data.serialNumber,
                pendingQa: data.pendingQaQuantity || 0,
              }
            }));
            setPartsByPartId(prev => ({ ...prev, [line.partId]: data }));
            // Batch field is intentionally left blank — issuing with no batch triggers
            // FIFO (oldest APPROVED batch first, split across as many as needed) on
            // inventory-service. Pre-filling it here would silently pin every issue
            // to a single batch and defeat that.
          }).catch(() => {});
        }
      });
    }
  }, [workOrder, activeTab]);

  useEffect(() => {
    if (workOrder && activeTab === 'material') {
      // Lets a past issue's printable receipt be reopened after its initial popup is
      // closed — previously there was no way back to it once dismissed.
      workOrder.materialLineItems.forEach(line => {
        api.get<ApiResponse<MaterialIssueRecord[]>>('/inventory/material-issue-records', { params: { lineItemId: line.id } })
          .then(res => setPastIssuesByLine(prev => ({ ...prev, [line.id]: res.data.data })))
          .catch(() => {});
      });
    }
  }, [workOrder, activeTab]);

  useEffect(() => {
    if (workOrder && activeTab === 'material') {
      // No dedicated "POs for this WO" endpoint exists — the PO list is small enough
      // that filtering client-side (same approach the PO page itself uses for prefill)
      // is simpler than adding a new backend query param for this.
      procurementApi.get<PurchaseOrderResponse[]>('/purchase-orders')
        .then(res => setLinkedPOs(res.data.filter(po => po.sourceWoNumber === workOrder.woNumber)))
        .catch(() => setLinkedPOs([]));
    }
  }, [workOrder, activeTab]);

  if (!isOpen || !workOrder) return null;

  const wo = workOrder;
  const nextStatuses = NEXT_STATUSES[wo.status];

  const handleTransition = async (toStatus: WorkOrderStatus) => {
    setTransitioning(true);
    try {
      await toast.promise(
        workOrderApi.post<ApiResponse<WorkOrder>>(`/work-orders/${wo.id}/transition`, {
          newStatus: toStatus,
          changedBy: 'Technician',
          remarks: transitionRemarks || `Moved to ${toStatus}`,
        } as StatusTransitionRequest),
        {
          loading: `Transitioning to ${toStatus}...`,
          success: `Work order moved to ${toStatus}`,
          error: (e) => e.response?.data?.message || 'Transition failed',
        }
      );
      setTransitionRemarks('');
      onSuccess();
    } catch {}
    finally { setTransitioning(false); }
  };

  // Called once the IssueMaterialPickerModal successfully deducts stock. Passing
  // workOrderId/lineItemId (via the modal's props) lets inventory-service publish a
  // MaterialIssuedEvent carrying this WO context — order-service consumes that event
  // and updates this line's quantityIssued/status itself, so the work order stays in
  // sync even if this browser tab closes right after.
  const handleMaterialIssued = (response: PartType) => {
    const issueRecordId = response.materialIssueRecordId;
    toast.success(`Issued from batch(es) ${response.batchNumber || 'Unknown'} — deducted from inventory. Work order will update momentarily.`);

    // Pop the printable Issue Receipt immediately — the caller shouldn't have to
    // hunt for it later to know exactly which batches this issue came from.
    if (issueRecordId) {
      api.get(`/inventory/material-issue-records/${issueRecordId}`)
        .then(r => {
          const record = r.data.data;
          setIssueReceipt(record);
          // Also add it to "Past Issues" for its line, so it's reachable again
          // without waiting for the next material-tab refetch.
          if (record.lineItemId) {
            setPastIssuesByLine(prev => ({
              ...prev,
              [record.lineItemId!]: [record, ...(prev[record.lineItemId!] || [])],
            }));
          }
        }).catch(() => {});
    }

    // Small delay so the async Kafka round-trip to order-service has time to
    // land before we refetch — the WO line update isn't part of this request.
    setTimeout(onSuccess, 800);
  };

  const isOverdue = wo.dueDate && new Date(wo.dueDate) < new Date()
    && wo.status !== 'COMPLETED' && wo.status !== 'CANCELLED';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-3xl max-h-[90vh] overflow-hidden flex flex-col">

        {/* Header */}
        <div className="px-8 py-6 bg-slate-900 text-white flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <span className="font-mono text-blue-400 font-black text-lg">{wo.woNumber}</span>
              <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-black border ${statusStyle[wo.status]}`}>
                {wo.status.replace('_', ' ')}
              </span>
              {isOverdue && (
                <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black bg-red-500 text-white border border-red-400 flex items-center gap-1">
                  <AlertTriangle size={9} /> OVERDUE
                </span>
              )}
              {linkedProductionOrders.map(po => (
                <button key={po.id} onClick={() => navigate('/production')}
                  className="px-2.5 py-0.5 rounded-full text-[10px] font-black bg-indigo-500/20 text-indigo-300 border border-indigo-400/40 hover:bg-indigo-500/30 transition-colors">
                  Production: {po.poNumber} · {po.status.replace(/_/g, ' ')}
                </button>
              ))}
            </div>
            <p className="text-slate-300 text-sm font-medium">{wo.taskDescription}</p>
            <div className="flex items-center gap-4 mt-2 flex-wrap">
              <span className="flex items-center gap-1 text-[11px] text-slate-400 font-bold">
                <Plane size={11} /> {wo.aircraftId} {wo.aircraftType ? `· ${wo.aircraftType}` : ''}
              </span>
              {wo.location && (
                <span className="flex items-center gap-1 text-[11px] text-slate-400 font-bold">
                  <MapPin size={11} /> {wo.location}
                </span>
              )}
              {wo.assignedTo && (
                <span className="flex items-center gap-1 text-[11px] text-slate-400 font-bold">
                  <User size={11} /> {wo.assignedTo}
                </span>
              )}
              {wo.dueDate && (
                <span className={`flex items-center gap-1 text-[11px] font-bold ${isOverdue ? 'text-red-400' : 'text-slate-400'}`}>
                  <Calendar size={11} /> Due: {new Date(wo.dueDate).toLocaleDateString()}
                </span>
              )}
            </div>
          </div>
          <div className="flex items-center gap-1">
            {wo.status !== 'COMPLETED' && wo.status !== 'CANCELLED' && (
              <button onClick={() => onEdit(wo)} title="Edit work order"
                className="p-2 hover:bg-white/10 rounded-xl transition text-slate-400 hover:text-white">
                <Edit3 size={18} />
              </button>
            )}
            {wo.status === 'DRAFT' && (
              <button onClick={() => onDelete(wo)} title="Delete work order"
                className="p-2 hover:bg-white/10 rounded-xl transition text-slate-400 hover:text-red-400">
                <Trash2 size={18} />
              </button>
            )}
            <button onClick={onClose} className="p-2 hover:bg-white/10 rounded-xl transition text-slate-400">
              <X size={20} />
            </button>
          </div>
        </div>

        {/* Status Transition Bar */}
        {nextStatuses.length > 0 && (
          <div className="px-8 py-4 bg-slate-50 border-b border-slate-100 flex items-center gap-3 flex-wrap">
            <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Transition:</span>
            {nextStatuses.map(s => (
              <button
                key={s}
                disabled={transitioning}
                onClick={() => handleTransition(s)}
                className={`flex items-center gap-1.5 px-4 py-1.5 rounded-xl text-xs font-black shadow-lg transition-all active:scale-95 ${transitionColor[s]}`}
              >
                {transitionIcon[s]}
                {s.replace('_', ' ')}
              </button>
            ))}
            <input
              type="text"
              placeholder="Transition remarks (optional)"
              value={transitionRemarks}
              onChange={e => setTransitionRemarks(e.target.value)}
              className="ml-auto px-3 py-1.5 bg-white border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-blue-500 outline-none flex-1 min-w-[180px] max-w-xs"
            />
          </div>
        )}

        {/* Tabs */}
        <div className="flex border-b border-slate-100 px-8">
          {(['details', 'material', 'history'] as const).map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-4 py-3.5 text-xs font-black uppercase tracking-wider transition border-b-2 -mb-px ${
                activeTab === tab
                  ? 'border-blue-600 text-blue-600'
                  : 'border-transparent text-slate-400 hover:text-slate-600'
              }`}
            >
              {tab === 'material' ? `Material (${wo.materialLineItems.length})` :
               tab === 'history'  ? `History (${history.length})` : tab}
            </button>
          ))}
        </div>

        {/* Tab Content */}
        <div className="flex-1 overflow-y-auto px-8 py-6">

          {/* Details Tab */}
          {activeTab === 'details' && (
            <div className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                {[
                  { label: 'WO Number',    value: wo.woNumber },
                  { label: 'Priority',     value: wo.priority },
                  { label: 'Task Code',    value: wo.taskCode || '—' },
                  { label: 'Created By',   value: wo.createdBy || '—' },
                  { label: 'Started At',   value: wo.startedAt ? new Date(wo.startedAt).toLocaleString() : '—' },
                  { label: 'Completed At', value: wo.completedAt ? new Date(wo.completedAt).toLocaleString() : '—' },
                  { label: 'Created At',   value: new Date(wo.createdAt).toLocaleString() },
                  { label: 'Updated At',   value: wo.updatedAt ? new Date(wo.updatedAt).toLocaleString() : '—' },
                ].map(row => (
                  <div key={row.label} className="bg-slate-50 rounded-2xl p-4 border border-slate-100">
                    <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">{row.label}</p>
                    <p className="text-sm font-bold text-slate-800">{row.value}</p>
                  </div>
                ))}
              </div>
              {wo.remarks && (
                <div className="bg-amber-50 rounded-2xl p-4 border border-amber-100">
                  <p className="text-[10px] font-black text-amber-600 uppercase tracking-widest mb-1">Remarks</p>
                  <p className="text-sm text-amber-800 font-medium">{wo.remarks}</p>
                </div>
              )}
              {wo.closureNotes && (
                <div className="bg-emerald-50 rounded-2xl p-4 border border-emerald-100">
                  <p className="text-[10px] font-black text-emerald-600 uppercase tracking-widest mb-1">Closure Notes</p>
                  <p className="text-sm text-emerald-800 font-medium">{wo.closureNotes}</p>
                </div>
              )}
              {/* Material summary */}
              <div className="bg-blue-50 rounded-2xl p-4 border border-blue-100 flex items-center gap-6">
                <div>
                  <p className="text-[10px] font-black text-blue-500 uppercase tracking-widest">Parts Required</p>
                  <p className="text-2xl font-black text-blue-900">{wo.totalPartsRequired}</p>
                </div>
                <div>
                  <p className="text-[10px] font-black text-blue-500 uppercase tracking-widest">Parts Issued</p>
                  <p className="text-2xl font-black text-blue-900">{wo.totalPartsIssued}</p>
                </div>
                <div className="ml-auto">
                  {wo.allMaterialReady ? (
                    <span className="flex items-center gap-1 text-xs font-black text-emerald-600 bg-emerald-50 border border-emerald-200 px-3 py-1 rounded-full">
                      <ShieldCheck size={12} /> All Material Ready
                    </span>
                  ) : (
                    <span className="flex items-center gap-1 text-xs font-black text-amber-600 bg-amber-50 border border-amber-200 px-3 py-1 rounded-full">
                      <Package size={12} /> Material Pending
                    </span>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Material Tab */}
          {activeTab === 'material' && (
            <div className="space-y-3">
              {wo.materialLineItems.length === 0 ? (
                <div className="text-center py-12 text-slate-300">
                  <Package size={40} className="mx-auto mb-2 opacity-20" />
                  <p className="text-xs font-bold uppercase tracking-widest">No material line items</p>
                </div>
              ) : (
                wo.materialLineItems.map(line => (
                  <div key={line.id} className="bg-slate-50 rounded-2xl p-4 border border-slate-100">
                    <div className="flex items-start justify-between mb-3">
                      <div>
                        <p className="font-black text-slate-900 text-sm">{line.partName || `Part #${line.partId}`}</p>
                        <div className="flex items-center gap-2 mt-0.5 flex-wrap">
                          <span className="text-[10px] text-slate-400 font-bold">Part ID: {line.partId}</span>
                          {line.partNumber && <span className="text-[10px] text-slate-400 font-bold">SKU: {line.partNumber}</span>}
                          {line.batchNumber && (
                            <span className="font-mono text-[10px] font-bold text-blue-600 bg-blue-50 px-2 py-0.5 rounded border border-blue-100">
                              {line.batchNumber}
                            </span>
                          )}
                        </div>
                      </div>
                      <span className={`px-2.5 py-1 rounded-full text-[10px] font-black border ${lineStatusStyle[line.status] || 'bg-slate-50 text-slate-500 border-slate-200'}`}>
                        {line.status}
                      </span>
                    </div>

                    <div className="flex items-center gap-4 mb-3">
                      <div className="text-center">
                        <p className="text-[10px] text-slate-400 font-bold uppercase">Required</p>
                        <p className="text-lg font-black text-slate-900">{line.quantityRequired}</p>
                      </div>
                      <ChevronRight size={14} className="text-slate-300" />
                      <div className="text-center">
                        <p className="text-[10px] text-slate-400 font-bold uppercase">Issued</p>
                        <p className={`text-lg font-black ${line.quantityIssued >= line.quantityRequired ? 'text-emerald-600' : 'text-amber-600'}`}>
                          {line.quantityIssued}
                        </p>
                      </div>
                    </div>

                    {/* NEW: Inventory Stock Check */}
                    <div className="flex items-center gap-4 pt-3 border-t border-slate-200 mt-3 mb-3">
                      {inventoryLevels[line.partId] ? (
                        <>
                          <div className="flex-1">
                            <span className="text-[10px] text-slate-400 font-bold uppercase tracking-widest block mb-1">Stock Check</span>
                            <div className="flex items-center gap-2">
                              <span className="text-xs font-black text-emerald-600 bg-emerald-50 px-2 py-1 rounded-lg">
                                {inventoryLevels[line.partId].qty} APPROVED in store
                              </span>
                              {inventoryLevels[line.partId].qty < (line.quantityRequired - line.quantityIssued) && (
                                <span className="text-xs font-black text-red-600 bg-red-50 px-2 py-1 rounded-lg flex items-center gap-1">
                                  <AlertTriangle size={10} /> Shortage: {(line.quantityRequired - line.quantityIssued) - inventoryLevels[line.partId].qty}
                                </span>
                              )}
                              {inventoryLevels[line.partId].qty >= (line.quantityRequired - line.quantityIssued) && (line.quantityRequired - line.quantityIssued) > 0 && (
                                <span className="text-xs font-black text-emerald-600 bg-emerald-50 border border-emerald-200 px-2 py-1 rounded-lg flex items-center gap-1">
                                  <CheckCircle2 size={10} /> Ready to Issue
                                </span>
                              )}
                              {inventoryLevels[line.partId].pendingQa > 0 && (
                                <span className="text-xs font-black text-amber-600 bg-amber-50 border border-amber-200 px-2 py-1 rounded-lg flex items-center gap-1">
                                  <Clock size={10} /> {inventoryLevels[line.partId].pendingQa} pending QA inspection
                                </span>
                              )}
                            </div>
                            {inventoryLevels[line.partId].pendingQa > 0 && (
                              <p className="text-[10px] text-slate-400 mt-1">
                                Material has arrived but isn't usable yet — it won't count toward the shortage above until QA approves it.
                              </p>
                            )}
                          </div>
                          {inventoryLevels[line.partId].qty < (line.quantityRequired - line.quantityIssued) && line.status !== 'ISSUED' && (
                            <button
                              onClick={() => {
                                const shortage = (line.quantityRequired - line.quantityIssued) - inventoryLevels[line.partId].qty;
                                // Use the real inventory SKU (serialNumber), not the WO line's cached
                                // partNumber — that can drift from the item master and breaks GRN
                                // part-ID resolution later.
                                const skuCode = inventoryLevels[line.partId].skuCode || line.partNumber || String(line.partId);
                                navigate('/purchase-orders', {
                                  state: { prefillItems: [{ skuCode, quantity: shortage }], woNumber: wo.woNumber }
                                });
                              }}
                              className="px-4 py-1.5 bg-indigo-600 text-white text-xs font-black rounded-xl hover:bg-indigo-700 transition shadow-lg shadow-indigo-200 whitespace-nowrap"
                            >
                              Raise PO ↗
                            </button>
                          )}
                        </>
                      ) : (
                        <div className="text-xs text-slate-400 italic flex items-center gap-1"><Loader2 size={10} className="animate-spin" /> Checking inventory...</div>
                      )}
                    </div>
                  {/* Partial Issue Banner — shown when some qty is issued but not all */}
                  {line.status === 'PARTIALLY_ISSUED' && (
                    <div className="flex items-center gap-2 px-3 py-2 bg-orange-50 border border-orange-200 rounded-xl mb-2">
                      <AlertTriangle size={13} className="text-orange-500 shrink-0" />
                      <span className="text-xs font-bold text-orange-700">
                        Partially Issued: {line.quantityIssued} / {line.quantityRequired}
                        &nbsp;—&nbsp;Remaining: {line.quantityRequired - line.quantityIssued}
                      </span>
                    </div>
                  )}

                  {/* Past Issues — reopen a previously-closed printable receipt */}
                  {(pastIssuesByLine[line.id]?.length || 0) > 0 && (
                    <div className="pt-3 border-t border-slate-200">
                      <span className="text-[10px] text-slate-400 font-bold uppercase tracking-widest block mb-1.5">
                        Past Issues
                      </span>
                      <div className="space-y-1">
                        {pastIssuesByLine[line.id].map(record => (
                          <button
                            key={record.id}
                            onClick={() => setIssueReceipt(record)}
                            className="w-full flex items-center justify-between gap-2 bg-white hover:bg-slate-50 border border-slate-200 rounded-xl px-3 py-1.5 transition text-left"
                          >
                            <span className="font-mono text-[11px] font-bold text-slate-700">{record.issueNumber}</span>
                            <span className="text-[11px] text-slate-500">{record.totalQuantityIssued} units · {new Date(record.issuedAt).toLocaleDateString()}</span>
                          </button>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* Issue / Issue-Remaining — opens the batch picker showing real availability */}
                  {(line.status === 'PENDING' || line.status === 'PARTIALLY_ISSUED') &&
                    wo.status !== 'COMPLETED' && wo.status !== 'CANCELLED' && (
                    <div className="flex items-center justify-end pt-3 border-t border-slate-200">
                      <button
                        onClick={() => setIssuePickerLine(line)}
                        disabled={!line.partId}
                        className="px-4 py-1.5 bg-blue-600 text-white text-xs font-black rounded-xl hover:bg-blue-700 transition shadow-lg shadow-blue-200 whitespace-nowrap disabled:opacity-50 flex items-center gap-1.5"
                      >
                        {line.status === 'PARTIALLY_ISSUED' ? 'Issue Remaining' : 'Issue Material'}
                      </button>
                    </div>
                  )}
                  </div>
                ))
              )}

              {linkedPOs.length > 0 && (
                <div className="pt-2">
                  <span className="text-[10px] text-slate-400 font-bold uppercase tracking-widest block mb-2">
                    Linked Purchase Orders (raised for this Work Order's shortage)
                  </span>
                  <div className="space-y-2">
                    {linkedPOs.map(po => {
                      const rejected = po.items?.reduce((sum, i) => sum + (i.rejectedQuantity ?? 0), 0) || 0;
                      const accepted = po.items?.reduce((sum, i) => sum + (i.acceptedQuantity ?? 0), 0) || 0;
                      return (
                        <div key={po.id} className="flex items-center justify-between gap-3 bg-slate-50 rounded-xl border border-slate-100 px-3 py-2">
                          <div className="flex items-center gap-2">
                            <span className="font-mono font-black text-slate-800 text-xs">{po.poNumber}</span>
                            <span className="px-2 py-0.5 text-[9px] font-black uppercase rounded-full bg-slate-200 text-slate-600">
                              {po.status}
                            </span>
                          </div>
                          <div className="flex items-center gap-1.5">
                            {accepted > 0 && (
                              <span className="px-1.5 py-0.5 text-[9px] font-black rounded bg-emerald-100 text-emerald-700">
                                ✓ {accepted} accepted
                              </span>
                            )}
                            {rejected > 0 && (
                              <span className="px-1.5 py-0.5 text-[9px] font-black rounded bg-red-100 text-red-700">
                                ✕ {rejected} rejected
                              </span>
                            )}
                            {accepted === 0 && rejected === 0 && (
                              <span className="text-[9px] text-slate-400 font-bold">Awaiting receipt/QA</span>
                            )}
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* History Tab */}
          {activeTab === 'history' && (
            <div className="space-y-2">
              {history.length === 0 ? (
                <div className="text-center py-12 text-slate-300">
                  <History size={40} className="mx-auto mb-2 opacity-20" />
                  <p className="text-xs font-bold uppercase tracking-widest">No history yet</p>
                </div>
              ) : (
                history.map((h, i) => (
                  <div key={h.id} className="flex items-start gap-4 p-4 bg-slate-50 rounded-2xl border border-slate-100">
                    <div className="flex flex-col items-center">
                      <div className="w-7 h-7 rounded-full bg-slate-200 flex items-center justify-center text-[10px] font-black text-slate-600">
                        {history.length - i}
                      </div>
                      {i < history.length - 1 && <div className="w-0.5 h-4 bg-slate-200 mt-1" />}
                    </div>
                    <div className="flex-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        {h.fromStatus && (
                          <>
                            <span className={`px-2 py-0.5 rounded-full text-[10px] font-black border ${statusStyle[h.fromStatus]}`}>
                              {h.fromStatus.replace('_', ' ')}
                            </span>
                            <ChevronRight size={12} className="text-slate-400" />
                          </>
                        )}
                        <span className={`px-2 py-0.5 rounded-full text-[10px] font-black border ${statusStyle[h.toStatus]}`}>
                          {h.toStatus.replace('_', ' ')}
                        </span>
                      </div>
                      {h.remarks && <p className="text-xs text-slate-500 italic mt-1">{h.remarks}</p>}
                      <div className="flex items-center gap-3 mt-1">
                        {h.changedBy && <span className="text-[10px] text-slate-400 font-bold">👤 {h.changedBy}</span>}
                        <span className="text-[10px] text-slate-400 font-bold">
                          {new Date(h.changedAt).toLocaleString()}
                        </span>
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      </div>

      {issueReceipt && (
        <ReceiptModal
          isOpen={!!issueReceipt}
          onClose={() => setIssueReceipt(null)}
          title="Material Issue Receipt"
          documentNumber={issueReceipt.issueNumber}
        >
          <MaterialIssueReceiptDocument record={issueReceipt} org={orgProfile} />
        </ReceiptModal>
      )}

      <IssueMaterialPickerModal
        isOpen={!!issuePickerLine}
        onClose={() => setIssuePickerLine(null)}
        part={issuePickerLine ? partsByPartId[issuePickerLine.partId] || null : null}
        workOrderId={wo.id}
        woNumber={wo.woNumber}
        lineItemId={issuePickerLine?.id}
        suggestedQuantity={issuePickerLine ? issuePickerLine.quantityRequired - issuePickerLine.quantityIssued : 0}
        onIssued={(response) => { handleMaterialIssued(response); setIssuePickerLine(null); }}
      />
    </div>
  );
};
