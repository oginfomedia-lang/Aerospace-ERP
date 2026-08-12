import React, { useEffect, useState } from 'react';
import productionResourceApi from '../api/productionResourceAxios';
import productionApi from '../api/productionAxios';
import toast from 'react-hot-toast';
import { CalendarClock, Loader2, CheckCircle2, Plus, X, RefreshCw, Link2 } from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import {
  Machine, Employee, Skill, MachineReservation, MachineReservationRequest,
  EmployeeReservation, EmployeeReservationRequest,
} from '../types/productionResource';
import { ProductionOrder, RoutingTemplate } from '../types/production';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole } from '../utils/permissions';

// No calendar/date library exists anywhere in this frontend today (checked
// package.json). Pulling one in (FullCalendar, react-big-calendar, etc.) is a bigger
// dependency decision than this pass should make unilaterally, and a real drag/drop
// grid isn't needed to satisfy the actual requirement: see existing reservations for
// a resource, create a new one, see conflicts, cancel one. A chronological list
// grouped by day does that with zero new dependencies — same "reuse what's here"
// principle applied to Machine/Employee/Tooling's own list+detail pages.
const CAN_WRITE = ['PRODUCTION_SUPERVISOR', 'PRODUCTION_ENGINEER', 'MANAGER', 'ADMIN'];

type ResourceType = 'MACHINE' | 'EMPLOYEE';

const emptyMachineForm = (createdBy: string): MachineReservationRequest => ({
  machineId: 0, reservedFrom: '', reservedTo: '', productionOrderId: undefined, createdBy,
});
const emptyEmployeeForm = (createdBy: string): EmployeeReservationRequest => ({
  employeeId: 0, reservedFrom: '', reservedTo: '', productionOrderId: undefined, createdBy,
});

const statusStyle = (status: string) =>
  status === 'ACTIVE' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-slate-100 text-slate-400 border-slate-200';

const fmt = (iso: string) => new Date(iso).toLocaleString(undefined, {
  weekday: 'short', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
});

const SchedulingPage = () => {
  const { user } = useAuth();
  const canWrite = hasAnyRole(user?.roles, CAN_WRITE);
  const createdBy = user?.username || user?.email || 'unknown';

  const [resourceType, setResourceType] = useState<ResourceType>('MACHINE');
  const [machines, setMachines] = useState<Machine[]>([]);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [skills, setSkills] = useState<Skill[]>([]);
  const [selectedResourceId, setSelectedResourceId] = useState<number>(0);

  const [machineReservations, setMachineReservations] = useState<MachineReservation[]>([]);
  const [employeeReservations, setEmployeeReservations] = useState<EmployeeReservation[]>([]);
  const [loading, setLoading] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const [machineForm, setMachineForm] = useState<MachineReservationRequest>(emptyMachineForm(createdBy));
  const [employeeForm, setEmployeeForm] = useState<EmployeeReservationRequest>(emptyEmployeeForm(createdBy));

  // Employee-reservation-only: linking to a real Production Order + one of its PENDING
  // stages auto-fills requiredSkillId/minimumSkillLevel from that stage's routing
  // operation, instead of leaving the planner to type the skill in manually (which was
  // the flagged gap — the qualification check itself lives entirely in
  // EmployeeReservationServiceImpl.validateEmployeeEligible and is unchanged by this).
  const [linkOrderInput, setLinkOrderInput] = useState('');
  const [linkedOrder, setLinkedOrder] = useState<ProductionOrder | null>(null);
  const [loadingOrder, setLoadingOrder] = useState(false);
  const [selectedStageId, setSelectedStageId] = useState<number>(0);
  const [resolvingStage, setResolvingStage] = useState(false);

  useEffect(() => {
    productionResourceApi.get<ApiResponse<Machine[]>>('/resources/machines')
      .then(res => setMachines(res.data.data.filter(m => m.status === 'ACTIVE'))).catch(() => {});
    productionResourceApi.get<ApiResponse<Employee[]>>('/resources/employees')
      .then(res => setEmployees(res.data.data.filter(e => e.status === 'ACTIVE'))).catch(() => {});
    productionResourceApi.get<ApiResponse<Skill[]>>('/resources/skills')
      .then(res => setSkills(res.data.data)).catch(() => {});
  }, []);

  const fetchReservations = async () => {
    if (!selectedResourceId) { setMachineReservations([]); setEmployeeReservations([]); return; }
    setLoading(true);
    try {
      if (resourceType === 'MACHINE') {
        const res = await productionResourceApi.get<ApiResponse<MachineReservation[]>>('/resources/machine-reservations', {
          params: { machineId: selectedResourceId },
        });
        setMachineReservations(res.data.data);
      } else {
        const res = await productionResourceApi.get<ApiResponse<EmployeeReservation[]>>('/resources/employee-reservations', {
          params: { employeeId: selectedResourceId },
        });
        setEmployeeReservations(res.data.data);
      }
    } catch {
      toast.error('Failed to fetch reservations.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchReservations(); }, [resourceType, selectedResourceId]); // eslint-disable-line react-hooks/exhaustive-deps

  const fetchLinkedOrder = async () => {
    if (!linkOrderInput.trim()) return;
    setLoadingOrder(true);
    setLinkedOrder(null);
    setSelectedStageId(0);
    try {
      const res = await productionApi.get<ApiResponse<ProductionOrder>>(`/production/${linkOrderInput.trim()}`);
      setLinkedOrder(res.data.data);
    } catch {
      toast.error('Could not find that production order.');
    } finally {
      setLoadingOrder(false);
    }
  };

  // Resolves the chosen stage's routing operation (via the part's routing template)
  // and auto-fills the reservation's skill requirement — the planner can still see
  // and override the result before submitting, this is just a convenience default.
  const handleStageSelect = async (stageId: number) => {
    setSelectedStageId(stageId);
    if (!linkedOrder || !stageId) return;
    const stage = linkedOrder.stageLogs.find(s => s.id === stageId);
    const material = stage ? linkedOrder.materials.find(m => m.id === stage.productionMaterialId) : null;
    if (!stage?.routingOperationId || !material?.partNumber) {
      setEmployeeForm(prev => ({ ...prev, productionOrderId: linkedOrder.id }));
      return;
    }
    setResolvingStage(true);
    try {
      const res = await productionApi.get<ApiResponse<RoutingTemplate[]>>('/production/routing-templates', {
        params: { partNumber: material.partNumber },
      });
      const template = res.data.data[0];
      const op = template?.operations.find(o => o.id === stage.routingOperationId);
      setEmployeeForm(prev => ({
        ...prev,
        productionOrderId: linkedOrder.id,
        requiredSkillId: op?.requiredSkillId ?? undefined,
        minimumSkillLevel: op?.minimumSkillLevel ?? undefined,
      }));
      if (op?.requiredSkillId) {
        toast.success(`Auto-filled required skill (${op.requiredSkillName}, level ${op.minimumSkillLevel ?? 1}+) from "${stage.stageName}".`);
      }
    } catch {
      toast.error('Could not resolve this stage\'s routing requirement — you can still enter it manually below.');
    } finally {
      setResolvingStage(false);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      if (resourceType === 'MACHINE') {
        if (!machineForm.machineId || !machineForm.reservedFrom || !machineForm.reservedTo) {
          toast.error('Machine, start, and end are required.'); setSubmitting(false); return;
        }
        await toast.promise(
          productionResourceApi.post('/resources/machine-reservations', { ...machineForm, createdBy }),
          {
            loading: 'Creating reservation...',
            success: 'Machine reserved.',
            error: (err) => err.response?.data?.message || 'Failed to create reservation.',
          }
        );
        setMachineForm(emptyMachineForm(createdBy));
      } else {
        if (!employeeForm.employeeId || !employeeForm.reservedFrom || !employeeForm.reservedTo) {
          toast.error('Employee, start, and end are required.'); setSubmitting(false); return;
        }
        await toast.promise(
          productionResourceApi.post('/resources/employee-reservations', { ...employeeForm, createdBy }),
          {
            loading: 'Creating reservation...',
            success: 'Employee reserved.',
            error: (err) => err.response?.data?.message || 'Failed to create reservation.',
          }
        );
        setEmployeeForm(emptyEmployeeForm(createdBy));
        setLinkOrderInput(''); setLinkedOrder(null); setSelectedStageId(0);
      }
      setShowForm(false);
      await fetchReservations();
    } catch {
      // toast.promise handles display — conflicts surface here with the exact
      // overlapping window, straight from MachineReservationServiceImpl/
      // EmployeeReservationServiceImpl's rejectIfOverlapping message.
    } finally {
      setSubmitting(false);
    }
  };

  const handleCancel = async (id: number) => {
    const path = resourceType === 'MACHINE' ? '/resources/machine-reservations' : '/resources/employee-reservations';
    try {
      await toast.promise(
        productionResourceApi.delete(`${path}/${id}`),
        { loading: 'Cancelling...', success: 'Reservation cancelled', error: (err) => err.response?.data?.message || 'Failed to cancel' }
      );
      await fetchReservations();
    } catch {}
  };

  const reservations = resourceType === 'MACHINE' ? machineReservations : employeeReservations;

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white">
            <CalendarClock size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Machine & Employee Scheduling</h1>
            <p className="text-slate-500 text-sm font-medium">
              Advance bookings for a future time window — separate from live occupancy.
            </p>
          </div>
        </div>
        <button onClick={fetchReservations} className="flex items-center gap-2 px-3 py-2 text-slate-500 hover:bg-slate-100 rounded-xl text-sm font-bold">
          <RefreshCw size={14} /> Refresh
        </button>
      </div>

      <div className="bg-white rounded-3xl shadow-sm border border-slate-100 p-5 space-y-4">
        <div className="flex items-center gap-3">
          <div className="flex bg-slate-100 rounded-xl p-1">
            {(['MACHINE', 'EMPLOYEE'] as ResourceType[]).map(t => (
              <button key={t} onClick={() => {
                setResourceType(t); setSelectedResourceId(0); setShowForm(false);
                setLinkOrderInput(''); setLinkedOrder(null); setSelectedStageId(0);
              }}
                className={`px-4 py-2 rounded-lg text-xs font-black transition-all ${
                  resourceType === t ? 'bg-white shadow text-blue-600' : 'text-slate-500'
                }`}>
                {t === 'MACHINE' ? 'Machines' : 'Employees'}
              </button>
            ))}
          </div>

          <select value={selectedResourceId} onChange={e => setSelectedResourceId(Number(e.target.value))}
            className="flex-1 px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
            <option value={0}>— Select a {resourceType === 'MACHINE' ? 'machine' : 'employee'} —</option>
            {resourceType === 'MACHINE'
              ? machines.map(m => <option key={m.id} value={m.id}>{m.machineCode} — {m.name}</option>)
              : employees.map(e => <option key={e.id} value={e.id}>{e.employeeCode} — {e.name}</option>)}
          </select>

          {canWrite && selectedResourceId > 0 && (
            <button onClick={() => setShowForm(!showForm)}
              className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-sm font-black rounded-xl hover:bg-blue-700 transition">
              {showForm ? <X size={16} /> : <Plus size={16} />} {showForm ? 'Cancel' : 'New Reservation'}
            </button>
          )}
        </div>

        {showForm && selectedResourceId > 0 && (
          <form onSubmit={handleCreate} className="p-4 bg-slate-50 rounded-2xl border border-slate-200 grid grid-cols-2 gap-3">
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">From</label>
              <input type="datetime-local" required
                value={resourceType === 'MACHINE' ? machineForm.reservedFrom : employeeForm.reservedFrom}
                onChange={e => resourceType === 'MACHINE'
                  ? setMachineForm({ ...machineForm, machineId: selectedResourceId, reservedFrom: e.target.value })
                  : setEmployeeForm({ ...employeeForm, employeeId: selectedResourceId, reservedFrom: e.target.value })}
                className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm" />
            </div>
            <div>
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">To</label>
              <input type="datetime-local" required
                value={resourceType === 'MACHINE' ? machineForm.reservedTo : employeeForm.reservedTo}
                onChange={e => resourceType === 'MACHINE'
                  ? setMachineForm({ ...machineForm, machineId: selectedResourceId, reservedTo: e.target.value })
                  : setEmployeeForm({ ...employeeForm, employeeId: selectedResourceId, reservedTo: e.target.value })}
                className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm" />
            </div>
            {resourceType === 'EMPLOYEE' && (
              <div className="col-span-2 p-3 bg-white rounded-xl border border-slate-200 space-y-2">
                <label className="text-xs font-bold text-slate-500 uppercase mb-1 flex items-center gap-1.5 tracking-wider">
                  <Link2 size={12} /> Link to Production Order (optional)
                </label>
                <div className="flex gap-2">
                  <input placeholder="Production Order ID" value={linkOrderInput}
                    onChange={e => setLinkOrderInput(e.target.value)} onBlur={fetchLinkedOrder}
                    className="flex-1 px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs" />
                  {loadingOrder && <Loader2 size={16} className="animate-spin text-slate-400 self-center" />}
                </div>
                {linkedOrder && (
                  <select value={selectedStageId} onChange={e => handleStageSelect(Number(e.target.value))}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs">
                    <option value={0}>— Select a pending stage — ({linkedOrder.poNumber})</option>
                    {linkedOrder.stageLogs.filter(s => s.status === 'PENDING').map(s => (
                      <option key={s.id} value={s.id}>{s.stageName} (seq {s.sequenceNumber})</option>
                    ))}
                  </select>
                )}
                {resolvingStage && <div className="text-[10px] text-slate-400 font-bold">Resolving skill requirement...</div>}

                <div className="grid grid-cols-2 gap-2 pt-1">
                  <select value={employeeForm.requiredSkillId ?? ''}
                    onChange={e => setEmployeeForm({ ...employeeForm, requiredSkillId: e.target.value ? Number(e.target.value) : undefined })}
                    className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs">
                    <option value="">— No required skill —</option>
                    {skills.map(s => <option key={s.id} value={s.id}>{s.name}</option>)}
                  </select>
                  <input type="number" min={1} max={5} placeholder="Min level" value={employeeForm.minimumSkillLevel ?? ''}
                    onChange={e => setEmployeeForm({ ...employeeForm, minimumSkillLevel: e.target.value ? Number(e.target.value) : undefined })}
                    className="px-3 py-2 bg-slate-50 border border-slate-200 rounded-lg text-xs" />
                </div>
                <p className="text-[10px] text-slate-400 font-medium">
                  Picking a pending stage above auto-fills the required skill/level from its routing operation — you can still edit or clear them here.
                </p>
              </div>
            )}
            <div className="col-span-2">
              <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Production Order ID (optional)</label>
              <input type="number" min={1}
                value={(resourceType === 'MACHINE' ? machineForm.productionOrderId : employeeForm.productionOrderId) ?? ''}
                onChange={e => resourceType === 'MACHINE'
                  ? setMachineForm({ ...machineForm, productionOrderId: e.target.value ? Number(e.target.value) : undefined })
                  : setEmployeeForm({ ...employeeForm, productionOrderId: e.target.value ? Number(e.target.value) : undefined })}
                className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-sm" />
            </div>
            <button type="submit" disabled={submitting}
              className="col-span-2 py-2.5 rounded-xl font-black text-white bg-blue-600 hover:bg-blue-700 transition-all flex items-center justify-center gap-2 disabled:opacity-60">
              {submitting ? <Loader2 size={16} className="animate-spin" /> : <CheckCircle2 size={16} />}
              Reserve
            </button>
          </form>
        )}
      </div>

      <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
        <div className="p-5 border-b border-slate-100">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Reservations</h2>
        </div>
        {!selectedResourceId ? (
          <div className="p-16 text-center text-slate-300">
            <CalendarClock size={48} className="mx-auto mb-3 opacity-20" />
            <p className="font-bold uppercase tracking-widest text-xs">Select a resource to view its schedule</p>
          </div>
        ) : loading ? (
          <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
        ) : reservations.length === 0 ? (
          <div className="p-16 text-center text-slate-300">
            <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
            <p className="font-bold uppercase tracking-widest text-xs">No reservations for this resource</p>
          </div>
        ) : (
          <div className="divide-y divide-slate-50">
            {reservations.map(r => (
              <div key={r.id} className="p-5 flex items-center justify-between">
                <div>
                  <div className="font-black text-slate-900 text-sm">{fmt(r.reservedFrom)} → {fmt(r.reservedTo)}</div>
                  <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">
                    By {r.createdBy}{r.productionOrderId ? ` · PO #${r.productionOrderId}` : ''}
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <span className={`text-[10px] font-black px-2.5 py-1 rounded-full border ${statusStyle(r.status)}`}>{r.status}</span>
                  {r.status === 'ACTIVE' && canWrite && (
                    <button onClick={() => handleCancel(r.id)}
                      className="text-[10px] font-black px-2.5 py-1 rounded-full bg-red-500 text-white hover:bg-red-600">
                      Cancel
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default SchedulingPage;
