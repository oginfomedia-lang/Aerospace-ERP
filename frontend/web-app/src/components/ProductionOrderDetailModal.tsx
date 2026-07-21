import React, { useEffect, useState } from 'react';
import {
  X, Factory, Settings, Activity, ClipboardCheck,
  CheckCircle2, PauseCircle, PlayCircle,
  Users, History, Package, Trash2,
  ChevronRight, Loader2, Edit3, Ban,
  Wrench, ShieldAlert, Boxes, AlertTriangle
} from 'lucide-react';
import productionApi from '../api/productionAxios';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import { IssueMaterialPickerModal } from './IssueMaterialPickerModal';
import { Part as PartType } from '../types/inventory';
import {
  ProductionOrder, ProductionOrderStatus,
  ApiResponse, ProductionStageLog, ShopFloorNcr
} from '../types/production';
import toast from 'react-hot-toast';

interface Props {
  order: ProductionOrder | null;
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  onEdit: (order: ProductionOrder) => void;
  onDelete: (order: ProductionOrder) => void;
  onCancel: (order: ProductionOrder) => void;
}

const statusStyle: Record<ProductionOrderStatus, string> = {
  PLANNED:            'bg-slate-100  text-slate-600  border-slate-200',
  RELEASED:           'bg-blue-50    text-blue-700   border-blue-200',
  IN_PRODUCTION:      'bg-amber-50   text-amber-700  border-amber-200',
  READY_FOR_ASSEMBLY: 'bg-teal-50    text-teal-700   border-teal-200',
  IN_ASSEMBLY:        'bg-violet-50  text-violet-700 border-violet-200',
  QUALITY_CHECK:      'bg-indigo-50  text-indigo-700 border-indigo-200',
  COMPLETED:          'bg-emerald-50 text-emerald-700 border-emerald-200',
  ON_HOLD:            'bg-orange-50  text-orange-700 border-orange-200',
  CANCELLED:          'bg-red-50     text-red-700    border-red-200',
};

const stageStatusStyle: Record<string, string> = {
  PENDING:     'bg-slate-100 text-slate-400 border-slate-200',
  IN_PROGRESS: 'bg-amber-100 text-amber-700 border-amber-300 animate-pulse',
  COMPLETED:   'bg-emerald-100 text-emerald-700 border-emerald-300',
  FAILED:      'bg-red-100 text-red-700 border-red-300',
};

interface StageForm {
  operatorName: string;
  quantityProduced: string;
  quantityGood: string;
  quantityRework: string;
  quantityScrap: string;
  inspectedBy: string;
  remarks: string;
}

const emptyForm: StageForm = {
  operatorName: '', quantityProduced: '', quantityGood: '', quantityRework: '', quantityScrap: '', inspectedBy: '', remarks: '',
};

export const ProductionOrderDetailModal: React.FC<Props> = ({ order, isOpen, onClose, onSuccess, onEdit, onDelete, onCancel }) => {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState<'details' | 'materials' | 'stages'>('details');
  const [loading, setLoading] = useState(false);
  const [actionRemarks, setActionRemarks] = useState('');
  const [qaProducedQty, setQaProducedQty] = useState('');
  const [qaRejectedQty, setQaRejectedQty] = useState('0');
  const [ncrs, setNcrs] = useState<ShopFloorNcr[]>([]);
  const [openStageId, setOpenStageId] = useState<number | null>(null);
  const [forms, setForms] = useState<Record<number, StageForm>>({});
  const [stageBusy, setStageBusy] = useState<number | null>(null);
  const [partsByPartId, setPartsByPartId] = useState<Record<number, PartType>>({});
  const [issueMaterialId, setIssueMaterialId] = useState<number | null>(null);

  useEffect(() => {
    if (order && isOpen) {
      productionApi.get<ApiResponse<ShopFloorNcr[]>>(`/production/${order.id}/ncrs`)
        .then(res => setNcrs(res.data.data)).catch(() => setNcrs([]));
    }
  }, [order, isOpen]);
 
  useEffect(() => {
    if(order && isOpen) {
      setQaProducedQty(String(order.producedQuantity ?? ''));
      setQaRejectedQty('0');
    }
  }, [order, isOpen]);

  // Fetch each BOM line's full Part (with live balances) for the Issue Material picker —
  // only meaningful for standalone orders, whose materials never got real stock issued.
  useEffect(() => {
    if (order && activeTab === 'materials' && !order.workOrderId) {
      order.materials.forEach(m => {
        if (m.partId) {
          api.get(`/inventory/${m.partId}`).then(res => {
            setPartsByPartId(prev => ({ ...prev, [m.partId]: res.data.data }));
          }).catch(() => {});
        }
      });
    }
  }, [order, activeTab]);

  if (!isOpen || !order) return null;

  const issueMaterialLine = order.materials.find(m => m.id === issueMaterialId) || null;

  // Called once IssueMaterialPickerModal successfully deducts real stock from
  // inventory-service. It only returns the updated Part, not the issued quantity, so
  // look up the MaterialIssueRecord it just created to get the real total — then record
  // that consumption on this BOM line via production-service's own (previously unwired)
  // consume endpoint.
  const handleMaterialIssued = async (response: PartType) => {
    const currentLine = issueMaterialLine;
    setIssueMaterialId(null);
    if (!currentLine) return;
    try {
      let consumedQty = currentLine.quantityRequired - (currentLine.quantityConsumed ?? 0);
      if (response.materialIssueRecordId) {
        const rec = await api.get(`/inventory/material-issue-records/${response.materialIssueRecordId}`);
        consumedQty = rec.data.data.totalQuantityIssued ?? consumedQty;
      }
      await productionApi.post(
        `/production/${order.id}/materials/${currentLine.id}/consume`,
        null,
        { params: { consumed: (currentLine.quantityConsumed ?? 0) + consumedQty, wasted: currentLine.quantityWasted ?? 0 } }
      );
      toast.success(`Recorded ${consumedQty} units consumed for ${currentLine.partName}.`);
      onSuccess();
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Issued from inventory, but failed to record consumption on the order.');
    }
  };

  const handleAction = async (action: string, endpoint: string, params: Record<string, any> = {}) => {
    setLoading(true);
    try {
      await toast.promise(
        productionApi.post<ApiResponse<ProductionOrder>>(`/production/${order.id}/${endpoint}`, null, { params }),
        {
          loading: `${action} in progress...`,
          success: `Order ${order.poNumber} ${action} successfully`,
          error: (err) => err.response?.data?.message || `Failed to ${action}`,
        }
      );
      onSuccess();
    } catch {
    } finally {
      setLoading(false);
    }
  };

  const getForm = (stageId: number): StageForm => forms[stageId] || emptyForm;
  const setForm = (stageId: number, patch: Partial<StageForm>) =>
    setForms(prev => ({ ...prev, [stageId]: { ...getForm(stageId), ...patch } }));

  const handleStartStage = async (materialId: number, stage: ProductionStageLog) => {
    const f = getForm(stage.id);
    setStageBusy(stage.id);
    try {
      await toast.promise(
        productionApi.post(`/production/${order.id}/materials/${materialId}/stages/${stage.id}/start`, {
          operatorName: f.operatorName || 'Operator',
        }),
        {
          loading: `Starting ${stage.stageName}...`,
          success: `${stage.stageName} started`,
          error: (err) => err.response?.data?.message || `Failed to start ${stage.stageName}`,
        }
      );
      setOpenStageId(null);
      onSuccess();
    } catch {
    } finally {
      setStageBusy(null);
    }
  };

  // PRODUCTION stages just report quantity produced — no accept/reject decision;
  // that's the following QA_INSPECTION stage's job.
  const handleCompleteProductionStage = async (materialId: number, stage: ProductionStageLog) => {
    const f = getForm(stage.id);
    const produced = Number(f.quantityProduced || 0);
    if (produced <= 0 || produced > (stage.quantityIn || 0)) {
      toast.error(`Quantity produced must be between 1 and quantity in (${stage.quantityIn}).`);
      return;
    }
    setStageBusy(stage.id);
    try {
      await toast.promise(
        productionApi.post(`/production/${order.id}/materials/${materialId}/stages/${stage.id}/complete-production`, {
          quantityProduced: produced, operatorName: f.operatorName, remarks: f.remarks,
        }),
        {
          loading: `Completing ${stage.stageName}...`,
          success: `${stage.stageName} completed`,
          error: (err) => err.response?.data?.message || `Failed to complete ${stage.stageName}`,
        }
      );
      setOpenStageId(null);
      onSuccess();
    } catch {
    } finally {
      setStageBusy(null);
    }
  };

  // QA_INSPECTION stages — the good/rework/scrap decision. Role-gated server-side to
  // QA_INSPECTOR/MANAGER/ADMIN.
  const handleCompleteInspectionStage = async (materialId: number, stage: ProductionStageLog) => {
    const f = getForm(stage.id);
    const good = Number(f.quantityGood || 0);
    const rework = Number(f.quantityRework || 0);
    const scrap = Number(f.quantityScrap || 0);
    if (good + rework + scrap !== (stage.quantityIn || 0)) {
      toast.error(`Good + Rework + Scrap must equal quantity in (${stage.quantityIn}).`);
      return;
    }
    setStageBusy(stage.id);
    try {
      await toast.promise(
        productionApi.post(`/production/${order.id}/materials/${materialId}/stages/${stage.id}/complete-inspection`, {
          quantityGood: good, quantityRework: rework, quantityScrap: scrap,
          inspectedBy: f.inspectedBy, remarks: f.remarks,
        }),
        {
          loading: `Completing ${stage.stageName}...`,
          success: `${stage.stageName} completed`,
          error: (err) => err.response?.data?.message || `Failed to complete ${stage.stageName}`,
        }
      );
      setOpenStageId(null);
      onSuccess();
    } catch {
    } finally {
      setStageBusy(null);
    }
  };

  const renderStatusActions = () => {
    switch (order.status) {
      case ProductionOrderStatus.PLANNED:
        return (
          <button
            onClick={() => handleAction('Released', 'release')}
            disabled={loading}
            className="flex items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-blue-700 transition shadow-lg shadow-blue-200 disabled:opacity-60"
          >
            <PlayCircle size={14} /> Release Order
          </button>
        );
      case ProductionOrderStatus.RELEASED:
        return (
          <button
            onClick={() => handleAction('Started', 'start')}
            disabled={loading}
            className="flex items-center gap-2 bg-amber-500 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-amber-600 transition shadow-lg shadow-amber-200 disabled:opacity-60"
          >
            <Activity size={14} /> Start Production
          </button>
        );
        case ProductionOrderStatus.IN_PRODUCTION:
        return (
          <div className="flex gap-2 items-center flex-wrap">
            <input type="number" min={0} value={qaProducedQty} onChange={e => setQaProducedQty(e.target.value)}
              placeholder="Produced qty"
              className="w-28 px-2 py-2 bg-white border border-slate-200 rounded-xl text-xs outline-none focus:ring-2 focus:ring-indigo-500" />
            <input type="number" min={0} value={qaRejectedQty} onChange={e => setQaRejectedQty(e.target.value)}
              placeholder="Rejected qty"
              className="w-28 px-2 py-2 bg-white border border-slate-200 rounded-xl text-xs outline-none focus:ring-2 focus:ring-indigo-500" />
            <button
              onClick={() => handleAction('Moved to QA', 'quality-check', {
                producedQty: Number(qaProducedQty) || 0,
                rejectedQty: Number(qaRejectedQty) || 0,
              })}
              disabled={loading}
              className="flex items-center gap-2 bg-indigo-600 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-indigo-700 transition shadow-lg shadow-indigo-200 disabled:opacity-60"
              title="For orders with no BOM routing — skips the per-item operations flow"
            >
              <ClipboardCheck size={14} /> Send to QA (no routing)
            </button>
            <button
              onClick={() => handleAction('Put on Hold', 'hold', { reason: actionRemarks })}
              disabled={loading}
              className="flex items-center gap-2 bg-orange-500 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-orange-600 transition shadow-lg shadow-orange-200 disabled:opacity-60"
            >
              <PauseCircle size={14} /> Put on Hold
            </button>
          </div>
        );
        case ProductionOrderStatus.READY_FOR_ASSEMBLY:
        return (
          <button
            onClick={() => handleAction('Assembly Started', 'start-assembly')}
            disabled={loading}
            className="flex items-center gap-2 bg-teal-600 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-teal-700 transition shadow-lg shadow-teal-200 disabled:opacity-60"
          >
            <Boxes size={14} /> Start Final Assembly
          </button>
        );
      case ProductionOrderStatus.IN_ASSEMBLY:
        return (
          <div className="flex gap-2 items-center flex-wrap">
            <input type="number" min={0} value={qaProducedQty} onChange={e => setQaProducedQty(e.target.value)}
              placeholder="Produced qty"
              className="w-28 px-2 py-2 bg-white border border-slate-200 rounded-xl text-xs outline-none focus:ring-2 focus:ring-indigo-500" />
            <input type="number" min={0} value={qaRejectedQty} onChange={e => setQaRejectedQty(e.target.value)}
              placeholder="Rejected qty"
              className="w-28 px-2 py-2 bg-white border border-slate-200 rounded-xl text-xs outline-none focus:ring-2 focus:ring-indigo-500" />
            <button
              onClick={() => handleAction('Moved to QA', 'quality-check', {
                producedQty: Number(qaProducedQty) || 0,
                rejectedQty: Number(qaRejectedQty) || 0,
              })}
              disabled={loading}
              className="flex items-center gap-2 bg-indigo-600 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-indigo-700 transition shadow-lg shadow-indigo-200 disabled:opacity-60"
            >
              <ClipboardCheck size={14} /> Send to QA
            </button>
          </div>
        );
      case ProductionOrderStatus.QUALITY_CHECK:
        return order.outputPartId ? (
          <button
            onClick={() => handleAction('Completed', 'complete', { qualityPassed: true })}
            disabled={loading}
            className="flex items-center gap-2 bg-emerald-600 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-emerald-700 transition shadow-lg shadow-emerald-200 disabled:opacity-60"
          >
            <CheckCircle2 size={14} /> Pass QA & Complete
          </button>
        ) : (
          <div className="flex items-center gap-2 text-xs font-bold text-amber-700 bg-amber-50 border border-amber-200 rounded-xl px-3 py-2">
            <AlertTriangle size={14} />
            No Output Part set — close this and click Edit on the order to select the finished-goods
            Part before completing.
          </div>
        );
      case ProductionOrderStatus.ON_HOLD:
        return (
          <button
            onClick={() => handleAction('Resumed', 'start')}
            disabled={loading}
            className="flex items-center gap-2 bg-amber-500 text-white px-4 py-2 rounded-xl text-xs font-bold hover:bg-amber-600 transition shadow-lg shadow-amber-200 disabled:opacity-60"
          >
            <PlayCircle size={14} /> Resume Production
          </button>
        );
      default:
        return null;
    }
  };

  // Order-level stages only (productionMaterialId null) — Final Assembly, plus any
  // legacy free-text entries added via the old generic "addStageLog" endpoint.
  const orderLevelStages = (order.stageLogs || []).filter(l => l.productionMaterialId == null);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-4xl max-h-[90vh] overflow-hidden flex flex-col">

        {/* Header */}
        <div className="px-8 py-6 bg-slate-900 text-white flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3 mb-1">
              <span className="font-mono text-indigo-400 font-black text-lg">{order.poNumber}</span>
              <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-black border ${statusStyle[order.status]}`}>
                {order.status.replace(/_/g, ' ')}
              </span>
            </div>
            <h2 className="text-xl font-black">{order.productName}</h2>
            <div className="flex items-center gap-4 mt-2 flex-wrap">
              <span className="flex items-center gap-1 text-[11px] text-slate-400 font-bold">
                <Settings size={11} /> {order.productCode || 'N/A'}
              </span>
              <span className="flex items-center gap-1 text-[11px] text-slate-400 font-bold">
                <Users size={11} /> {order.assignedTeam || 'No Team Assigned'}
              </span>
              {order.woNumber && (
                <span className="flex items-center gap-1 text-[11px] text-slate-400 font-bold">
                  <Factory size={11} /> WO {order.woNumber}
                </span>
              )}
            </div>
          </div>
          <div className="flex items-center gap-1">
            {order.status !== ProductionOrderStatus.COMPLETED && order.status !== ProductionOrderStatus.CANCELLED && (
              <button onClick={() => onEdit(order)} title="Edit production order"
                className="p-2 hover:bg-white/10 rounded-xl transition text-slate-400 hover:text-white">
                <Edit3 size={18} />
              </button>
            )}
            {order.status !== ProductionOrderStatus.COMPLETED && order.status !== ProductionOrderStatus.CANCELLED && (
              <button onClick={() => onCancel(order)} title="Cancel production order"
                className="p-2 hover:bg-white/10 rounded-xl transition text-slate-400 hover:text-orange-400">
                <Ban size={18} />
              </button>
            )}
            {order.status !== ProductionOrderStatus.IN_PRODUCTION && (
              <button onClick={() => onDelete(order)} title="Delete production order"
                className="p-2 hover:bg-white/10 rounded-xl transition text-slate-400 hover:text-red-400">
                <Trash2 size={18} />
              </button>
            )}
            <button onClick={onClose} className="p-2 hover:bg-white/10 rounded-xl transition text-slate-400">
              <X size={20} />
            </button>
          </div>
        </div>

        {/* Ready for Final Assembly banner */}
        {order.status === ProductionOrderStatus.READY_FOR_ASSEMBLY && (
          <div className="px-8 py-3 bg-teal-50 border-b border-teal-100 flex items-center gap-2 text-teal-700">
            <Boxes size={16} />
            <p className="text-xs font-bold">Every BOM item has finished its routing — ready for final assembly.</p>
          </div>
        )}

        {/* Action Bar */}
        {(order.status !== ProductionOrderStatus.COMPLETED && order.status !== ProductionOrderStatus.CANCELLED) && (
          <div className="px-8 py-4 bg-slate-50 border-b border-slate-100 flex items-center gap-3 flex-wrap">
            <span className="text-[10px] font-black text-slate-400 uppercase tracking-widest">Available Actions:</span>
            {renderStatusActions()}
            <input
              type="text"
              placeholder="Action notes..."
              className="ml-auto px-3 py-1.5 bg-white border border-slate-200 rounded-xl text-xs outline-none focus:ring-2 focus:ring-indigo-500 flex-1 max-w-[200px]"
              value={actionRemarks}
              onChange={e => setActionRemarks(e.target.value)}
            />
          </div>
        )}

        {/* Tabs */}
        <div className="flex border-b border-slate-100 px-8">
          {(['details', 'materials', 'stages'] as const).map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={`px-4 py-3.5 text-xs font-black uppercase tracking-wider transition border-b-2 -mb-px ${
                activeTab === tab
                  ? 'border-indigo-600 text-indigo-600'
                  : 'border-transparent text-slate-400 hover:text-slate-600'
              }`}
            >
              {tab === 'materials' ? 'BOM & Routing' : tab} {tab === 'materials' && `(${order.materials?.length || 0})`}
            </button>
          ))}
        </div>

        {/* Body */}
        <div className="flex-1 overflow-y-auto px-8 py-6">
          {activeTab === 'details' && (
            <div className="grid grid-cols-2 gap-4">
              {[
                { label: 'Planned Qty', value: order.plannedQuantity },
                { label: 'Produced Qty', value: order.producedQuantity },
                { label: 'Rejected Qty', value: order.rejectedQuantity },
                { label: 'Priority', value: order.priority },
                { label: 'Supervisor', value: order.supervisorName || '—' },
                { label: 'Production Line', value: order.productionLine || '—' },
                { label: 'Started At', value: order.actualStartDate ? new Date(order.actualStartDate).toLocaleString() : '—' },
                { label: 'Completed At', value: order.actualEndDate ? new Date(order.actualEndDate).toLocaleString() : '—' },
              ].map(item => (
                <div key={item.label} className="bg-slate-50 p-4 rounded-2xl border border-slate-100">
                  <p className="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-1">{item.label}</p>
                  <p className="text-sm font-bold text-slate-800">{item.value}</p>
                </div>
              ))}
            </div>
          )}

          {activeTab === 'materials' && (
            <div className="space-y-5">
              {(order.materials?.length || 0) === 0 ? (
                <div className="text-center py-10 text-slate-300">
                  <Package size={40} className="mx-auto mb-2 opacity-20" />
                  <p className="text-xs font-bold uppercase tracking-widest">No materials linked</p>
                </div>
              ) : (
                order.materials.map(m => {
                  // Latest attempt per sequence, in sequence order — the real routing
                  // timeline for this one BOM line.
                  const allForMaterial = (order.stageLogs || [])
                    .filter(l => l.productionMaterialId === m.id)
                    .sort((a, b) => (a.sequenceNumber || 0) - (b.sequenceNumber || 0) || (a.attemptNumber || 0) - (b.attemptNumber || 0));
                  const bySequence = new Map<number, ProductionStageLog[]>();
                  allForMaterial.forEach(l => {
                    const seq = l.sequenceNumber || 0;
                    if (!bySequence.has(seq)) bySequence.set(seq, []);
                    bySequence.get(seq)!.push(l);
                  });
                  const sequences = Array.from(bySequence.keys()).sort((a, b) => a - b);
                  const materialNcrs = ncrs.filter(n => n.productionMaterialId === m.id);

                  // The one actionable stage right now: the latest attempt of the
                  // first non-COMPLETED sequence.
                  let actionable: ProductionStageLog | undefined;
                  for (const seq of sequences) {
                    const attempts = bySequence.get(seq)!;
                    const latest = attempts[attempts.length - 1];
                    if (latest.status !== 'COMPLETED') { actionable = latest; break; }
                  }

                  return (
                    <div key={m.id} className="bg-slate-50 rounded-2xl border border-slate-100 p-4">
                      <div className="flex items-center justify-between mb-3">
                        <div>
                          <p className="font-bold text-slate-800 text-sm flex items-center gap-2">
                            {m.partName}
                            {m.readyForAssembly && (
                              <span className="text-[9px] font-black uppercase text-emerald-600 bg-emerald-50 border border-emerald-200 px-1.5 py-0.5 rounded-full flex items-center gap-1">
                                <CheckCircle2 size={10} /> Ready for assembly
                              </span>
                            )}
                          </p>
                          <p className="text-[10px] text-slate-400 font-bold">
                            {m.partNumber ? `${m.partNumber} · ` : ''}Req: {m.quantityRequired} · Consumed: {m.quantityConsumed ?? 0} · Wasted: {m.quantityWasted ?? 0}
                          </p>
                        </div>
                        <span className="text-[10px] font-black px-2 py-1 bg-white border rounded-lg text-slate-600">{m.status}</span>
                      </div>

                      {/* Real inventory issuance — only for standalone orders (no Work Order),
                          which never get their materials physically issued anywhere else. */}
                      {!order.workOrderId && (m.quantityRequired - (m.quantityConsumed ?? 0)) > 0 && (
                        <div className="flex justify-end mb-3">
                          <button
                            onClick={() => setIssueMaterialId(m.id)}
                            disabled={!m.partId}
                            className="px-3 py-1.5 bg-blue-600 text-white text-[11px] font-black rounded-lg hover:bg-blue-700 transition shadow-lg shadow-blue-200 disabled:opacity-50"
                          >
                            Issue Material
                          </button>
                        </div>
                      )}

                      {sequences.length === 0 ? (
                        <p className="text-[11px] text-slate-400 italic">No routing generated yet — click "Start Production" to expand this BOM line's operations.</p>
                      ) : (
                        <div className="flex items-start gap-1 overflow-x-auto pb-1">
                          {sequences.map((seq, idx) => {
                            const attempts = bySequence.get(seq)!;
                            const latest = attempts[attempts.length - 1];
                            return (
                              <React.Fragment key={seq}>
                                <div className="flex flex-col items-center gap-1 min-w-[84px]">
                                  <div className={`w-full text-center px-1.5 py-1 rounded-lg border text-[9px] font-black uppercase flex items-center justify-center gap-1 ${stageStatusStyle[latest.status]}`}>
                                    {latest.operationType === 'QA_INSPECTION' && <ShieldAlert size={10} />}
                                    {latest.stageName}
                                  </div>
                                  <span className="text-[8px] text-slate-400 font-bold">
                                    {latest.status}{attempts.length > 1 ? ` · attempt ${latest.attemptNumber}` : ''}
                                  </span>
                                </div>
                                {idx < sequences.length - 1 && <ChevronRight size={14} className="text-slate-300 mt-2 shrink-0" />}
                              </React.Fragment>
                            );
                          })}
                        </div>
                      )}

                      {materialNcrs.length > 0 && (
                        <div className="mt-2 flex flex-wrap gap-1.5">
                          {materialNcrs.map(n => (
                            <span key={n.id}
                              className={`text-[9px] font-black uppercase px-2 py-1 rounded-lg border flex items-center gap-1 ${
                                n.disposition === 'SCRAP' ? 'bg-red-50 text-red-600 border-red-200' : 'bg-amber-50 text-amber-700 border-amber-200'
                              }`}>
                              <ShieldAlert size={10} /> {n.disposition}: {n.rejectedQuantity} ({n.stageName})
                            </span>
                          ))}
                        </div>
                      )}

                      {/* Inline action for the current stage */}
                      {actionable && (
                        <div className="mt-3 pt-3 border-t border-slate-200">
                          {actionable.status === 'PENDING' && (
                            openStageId === actionable.id ? (
                              <div className="flex items-center gap-2">
                                <input placeholder="Operator name" className="px-2 py-1.5 bg-white border border-slate-200 rounded-lg text-xs flex-1"
                                  value={getForm(actionable.id).operatorName}
                                  onChange={e => setForm(actionable!.id, { operatorName: e.target.value })} />
                                <button disabled={stageBusy === actionable.id}
                                  onClick={() => handleStartStage(m.id, actionable!)}
                                  className="flex items-center gap-1 bg-amber-500 hover:bg-amber-600 text-white px-3 py-1.5 rounded-lg text-[11px] font-bold">
                                  {stageBusy === actionable.id ? <Loader2 size={12} className="animate-spin" /> : <PlayCircle size={12} />} Start
                                </button>
                                <button onClick={() => setOpenStageId(null)} className="text-slate-400 text-[11px] font-bold px-2">Cancel</button>
                              </div>
                            ) : (
                              <button onClick={() => setOpenStageId(actionable!.id)}
                                className="flex items-center gap-1.5 bg-amber-500 hover:bg-amber-600 text-white px-3 py-1.5 rounded-lg text-[11px] font-bold">
                                <PlayCircle size={12} /> Start {actionable.stageName}
                              </button>
                            )
                          )}
                          {actionable.status === 'IN_PROGRESS' && actionable.operationType === 'QA_INSPECTION' && (
                            openStageId === actionable.id ? (
                              <div className="space-y-2 bg-white p-3 rounded-xl border border-slate-200">
                                <p className="text-[10px] font-black text-indigo-500 uppercase flex items-center gap-1">
                                  <ShieldAlert size={11} /> QA Inspection · Quantity In: {actionable.quantityIn}
                                </p>
                                <div className="grid grid-cols-3 gap-2">
                                  <div>
                                    <label className="text-[9px] font-bold text-slate-400 uppercase">Good</label>
                                    <input type="number" className="w-full px-2 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-xs"
                                      value={getForm(actionable.id).quantityGood}
                                      onChange={e => setForm(actionable!.id, { quantityGood: e.target.value })} />
                                  </div>
                                  <div>
                                    <label className="text-[9px] font-bold text-amber-500 uppercase">Rework</label>
                                    <input type="number" className="w-full px-2 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-xs"
                                      value={getForm(actionable.id).quantityRework}
                                      onChange={e => setForm(actionable!.id, { quantityRework: e.target.value })} />
                                  </div>
                                  <div>
                                    <label className="text-[9px] font-bold text-red-500 uppercase">Scrap</label>
                                    <input type="number" className="w-full px-2 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-xs"
                                      value={getForm(actionable.id).quantityScrap}
                                      onChange={e => setForm(actionable!.id, { quantityScrap: e.target.value })} />
                                  </div>
                                </div>
                                <input placeholder="Inspected by" className="w-full px-2 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-xs"
                                  value={getForm(actionable.id).inspectedBy}
                                  onChange={e => setForm(actionable!.id, { inspectedBy: e.target.value })} />
                                <input placeholder="Remarks (reason for rework/scrap, if any)" className="w-full px-2 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-xs"
                                  value={getForm(actionable.id).remarks}
                                  onChange={e => setForm(actionable!.id, { remarks: e.target.value })} />
                                <div className="flex gap-2">
                                  <button disabled={stageBusy === actionable.id}
                                    onClick={() => handleCompleteInspectionStage(m.id, actionable!)}
                                    className="flex items-center gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white px-3 py-1.5 rounded-lg text-[11px] font-bold">
                                    {stageBusy === actionable.id ? <Loader2 size={12} className="animate-spin" /> : <CheckCircle2 size={12} />} Complete Inspection
                                  </button>
                                  <button onClick={() => setOpenStageId(null)} className="text-slate-400 text-[11px] font-bold px-2">Cancel</button>
                                </div>
                              </div>
                            ) : (
                              <button onClick={() => setOpenStageId(actionable!.id)}
                                className="flex items-center gap-1.5 bg-indigo-600 hover:bg-indigo-700 text-white px-3 py-1.5 rounded-lg text-[11px] font-bold">
                                <ShieldAlert size={12} /> Complete Inspection: {actionable.stageName}
                              </button>
                            )
                          )}
                          {actionable.status === 'IN_PROGRESS' && actionable.operationType !== 'QA_INSPECTION' && (
                            openStageId === actionable.id ? (
                              <div className="space-y-2 bg-white p-3 rounded-xl border border-slate-200">
                                <p className="text-[10px] font-black text-slate-500 uppercase">Quantity In: {actionable.quantityIn}</p>
                                <div>
                                  <label className="text-[9px] font-bold text-slate-400 uppercase">Quantity Produced</label>
                                  <input type="number" className="w-full px-2 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-xs"
                                    value={getForm(actionable.id).quantityProduced}
                                    onChange={e => setForm(actionable!.id, { quantityProduced: e.target.value })} />
                                </div>
                                <input placeholder="Remarks" className="w-full px-2 py-1.5 bg-slate-50 border border-slate-200 rounded-lg text-xs"
                                  value={getForm(actionable.id).remarks}
                                  onChange={e => setForm(actionable!.id, { remarks: e.target.value })} />
                                <div className="flex gap-2">
                                  <button disabled={stageBusy === actionable.id}
                                    onClick={() => handleCompleteProductionStage(m.id, actionable!)}
                                    className="flex items-center gap-1.5 bg-emerald-600 hover:bg-emerald-700 text-white px-3 py-1.5 rounded-lg text-[11px] font-bold">
                                    {stageBusy === actionable.id ? <Loader2 size={12} className="animate-spin" /> : <CheckCircle2 size={12} />} Complete Stage
                                  </button>
                                  <button onClick={() => setOpenStageId(null)} className="text-slate-400 text-[11px] font-bold px-2">Cancel</button>
                                </div>
                              </div>
                            ) : (
                              <button onClick={() => setOpenStageId(actionable!.id)}
                                className="flex items-center gap-1.5 bg-indigo-600 hover:bg-indigo-700 text-white px-3 py-1.5 rounded-lg text-[11px] font-bold">
                                <Wrench size={12} /> Complete {actionable.stageName}
                              </button>
                            )
                          )}
                        </div>
                      )}
                    </div>
                  );
                })
              )}
            </div>
          )}

          {activeTab === 'stages' && (
            <div className="space-y-4">
              {orderLevelStages.length === 0 ? (
                <div className="text-center py-10 text-slate-300">
                  <History size={40} className="mx-auto mb-2 opacity-20" />
                  <p className="text-xs font-bold uppercase tracking-widest">No order-level stage logs recorded</p>
                  <p className="text-[10px] mt-1">Per-BOM-line routing stages are on the "BOM & Routing" tab.</p>
                </div>
              ) : (
                orderLevelStages.map((log, i) => (
                  <div key={log.id} className="flex gap-4">
                    <div className="flex flex-col items-center">
                      <div className="w-6 h-6 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center text-[10px] font-black">
                        {i + 1}
                      </div>
                      {i < orderLevelStages.length - 1 && <div className="w-px flex-1 bg-slate-100 my-1" />}
                    </div>
                    <div className="flex-1 bg-slate-50 p-3 rounded-2xl border border-slate-100">
                      <div className="flex items-center justify-between mb-1">
                        <p className="text-xs font-black text-slate-800">{log.stageName}</p>
                        <span className="text-[10px] font-bold text-slate-400">
                          {log.startedAt ? new Date(log.startedAt).toLocaleString() : new Date(log.loggedAt).toLocaleString()}
                        </span>
                      </div>
                      <p className="text-[10px] text-slate-500">{log.remarks || 'No remarks'}</p>
                      <div className="mt-1 flex gap-2">
                         <span className="text-[9px] font-black uppercase text-indigo-500 bg-indigo-50 px-1.5 py-0.5 rounded">Status: {log.status}</span>
                         {log.operatorName && <span className="text-[9px] font-black uppercase text-slate-400">By: {log.operatorName}</span>}
                      </div>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-8 py-5 border-t border-slate-100 flex justify-end">
           <button
            onClick={onClose}
            className="px-8 py-2.5 bg-slate-900 text-white text-sm font-black rounded-2xl hover:bg-slate-800 transition shadow-lg"
          >
            Close
          </button>
        </div>
      </div>

      <IssueMaterialPickerModal
        isOpen={!!issueMaterialId}
        onClose={() => setIssueMaterialId(null)}
        part={issueMaterialLine ? partsByPartId[issueMaterialLine.partId] || null : null}
        suggestedQuantity={issueMaterialLine ? issueMaterialLine.quantityRequired - (issueMaterialLine.quantityConsumed ?? 0) : 0}
        issuedBy={user?.username}
        onIssued={handleMaterialIssued}
      />
    </div>
  );
};
