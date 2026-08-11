import React, { useEffect, useState } from 'react';
import productionResourceApi from '../api/productionResourceAxios';
import toast from 'react-hot-toast';
import { HardHat, Loader2, ChevronRight, CheckCircle2, PlusCircle, Plus, Unlock, History, CalendarDays, Check, X } from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import {
  Employee, Skill, EmployeeSkillCertification, EmployeeSkillCertificationRequest,
  Shift, EmployeeLeave, EmployeeLeaveRequest,
} from '../types/productionResource';
import { RegisterEmployeeModal } from '../components/RegisterEmployeeModal';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole } from '../utils/permissions';

const CAN_FORCE_RELEASE = ['PRODUCTION_SUPERVISOR', 'MANAGER', 'ADMIN'];
// Same roles the backend restricts shift/leave write endpoints to — matches
// ShiftController/EmployeeController's write-role @PreAuthorize overrides.
const CAN_MANAGE_SHIFTS_LEAVE = ['PRODUCTION_SUPERVISOR', 'MANAGER', 'ADMIN'];

const leaveStatusStyle = (status: string) => {
  if (status === 'APPROVED') return 'bg-emerald-50 text-emerald-700 border-emerald-200';
  if (status === 'REJECTED') return 'bg-red-50 text-red-700 border-red-200';
  return 'bg-amber-50 text-amber-700 border-amber-200'; // PENDING
};

const emptyLeaveForm: EmployeeLeaveRequest = { type: 'VACATION', startDate: '', endDate: '', reason: '' };

const skillBadgeStyle = (status: string) => {
  if (status === 'ACTIVE') return 'bg-indigo-50 text-indigo-700 border-indigo-200';
  if (status === 'EXPIRED') return 'bg-amber-50 text-amber-700 border-amber-200';
  return 'bg-red-50 text-red-700 border-red-200'; // REVOKED
};

const emptyCertForm: EmployeeSkillCertificationRequest = {
  skillId: 0, level: 1, certificateNumber: '', issuedDate: '', expiryDate: '',
  evaluatedBy: '', evaluationDate: '', nextEvaluationDate: '', remarks: '', status: 'ACTIVE',
};

const EmployeeDirectoryPage = () => {
  const { user } = useAuth();
  const canForceRelease = hasAnyRole(user?.roles, CAN_FORCE_RELEASE);
  const canManageShiftsLeave = hasAnyRole(user?.roles, CAN_MANAGE_SHIFTS_LEAVE);
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Employee | null>(null);
  const [activeOnly, setActiveOnly] = useState(false);
  const [showRegister, setShowRegister] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [allSkills, setAllSkills] = useState<Skill[]>([]);
  const [newSkillName, setNewSkillName] = useState('');
  const [addingSkill, setAddingSkill] = useState(false);
  const [certForm, setCertForm] = useState<EmployeeSkillCertificationRequest>(emptyCertForm);
  const [recordingCert, setRecordingCert] = useState(false);
  const [certHistory, setCertHistory] = useState<EmployeeSkillCertification[]>([]);
  const [showHistory, setShowHistory] = useState(false);

  const [allShifts, setAllShifts] = useState<Shift[]>([]);
  const [newShift, setNewShift] = useState({ name: '', startTime: '06:00', endTime: '14:00', daysOfWeek: 'MON,TUE,WED,THU,FRI' });
  const [addingShift, setAddingShift] = useState(false);
  const [showAddShift, setShowAddShift] = useState(false);

  const [leaveForm, setLeaveForm] = useState<EmployeeLeaveRequest>(emptyLeaveForm);
  const [requestingLeave, setRequestingLeave] = useState(false);
  const [leaveHistory, setLeaveHistory] = useState<EmployeeLeave[]>([]);
  const [decidingLeaveId, setDecidingLeaveId] = useState<number | null>(null);

  const [form, setForm] = useState({
    name: '', designation: '', department: '', status: 'ACTIVE', shiftId: 0,
  });

  const fetchEmployees = async () => {
    setLoading(true);
    try {
      const res = activeOnly
        ? await productionResourceApi.get<ApiResponse<Employee[]>>('/resources/employees', { params: { status: 'ACTIVE' } })
        : await productionResourceApi.get<ApiResponse<Employee[]>>('/resources/employees');
      setEmployees(res.data.data);
    } catch {
      toast.error('Failed to fetch employees.');
    } finally {
      setLoading(false);
    }
  };

  const fetchSkills = () => {
    productionResourceApi.get<ApiResponse<Skill[]>>('/resources/skills')
      .then(res => setAllSkills(res.data.data)).catch(() => {});
  };

  const fetchShifts = () => {
    productionResourceApi.get<ApiResponse<Shift[]>>('/resources/shifts')
      .then(res => setAllShifts(res.data.data)).catch(() => {});
  };

  useEffect(() => { fetchEmployees(); }, [activeOnly]); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => { fetchSkills(); fetchShifts(); }, []);

  const handleForceRelease = async (e: Employee) => {
    try {
      await toast.promise(
        productionResourceApi.post(`/resources/employees/${e.id}/force-release`),
        { loading: `Releasing ${e.employeeCode}...`, success: `${e.employeeCode} released`, error: 'Failed to release employee' }
      );
      await fetchEmployees();
    } catch {}
  };

  const handleSelect = (e: Employee) => {
    setSelected(e);
    setForm({ name: e.name, designation: e.designation || '', department: e.department || '', status: e.status, shiftId: e.shiftId || 0 });
    setCertForm(emptyCertForm);
    setShowHistory(false);
    setLeaveForm(emptyLeaveForm);
    productionResourceApi.get<ApiResponse<EmployeeSkillCertification[]>>(`/resources/employees/${e.id}/skills`)
      .then(res => setCertHistory(res.data.data)).catch(() => setCertHistory([]));
    productionResourceApi.get<ApiResponse<EmployeeLeave[]>>(`/resources/employees/${e.id}/leave`)
      .then(res => setLeaveHistory(res.data.data)).catch(() => setLeaveHistory([]));
  };

  const addShift = async () => {
    if (!newShift.name.trim() || !newShift.daysOfWeek.trim()) return;
    setAddingShift(true);
    try {
      const res = await productionResourceApi.post<ApiResponse<Shift>>('/resources/shifts', newShift);
      const created = res.data.data;
      setAllShifts(prev => [...prev, created]);
      setForm(prev => ({ ...prev, shiftId: created.id }));
      setNewShift({ name: '', startTime: '06:00', endTime: '14:00', daysOfWeek: 'MON,TUE,WED,THU,FRI' });
      setShowAddShift(false);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to add shift.');
    } finally {
      setAddingShift(false);
    }
  };

  const handleRequestLeave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    if (!leaveForm.startDate || !leaveForm.endDate) { toast.error('Pick a start and end date.'); return; }
    setRequestingLeave(true);
    try {
      await toast.promise(
        productionResourceApi.post(`/resources/employees/${selected.id}/leave`, leaveForm),
        {
          loading: 'Requesting leave...',
          success: 'Leave request submitted.',
          error: (err) => err.response?.data?.message || 'Failed to request leave.',
        }
      );
      setLeaveForm(emptyLeaveForm);
      const res = await productionResourceApi.get<ApiResponse<EmployeeLeave[]>>(`/resources/employees/${selected.id}/leave`);
      setLeaveHistory(res.data.data);
    } catch {
      // toast.promise handles display
    } finally {
      setRequestingLeave(false);
    }
  };

  const decideLeave = async (leave: EmployeeLeave, decision: 'approve' | 'reject') => {
    if (!selected || !user) return;
    setDecidingLeaveId(leave.id);
    try {
      await toast.promise(
        productionResourceApi.post(`/resources/employees/${selected.id}/leave/${leave.id}/${decision}`, { decidedBy: user.username || user.email }),
        {
          loading: `${decision === 'approve' ? 'Approving' : 'Rejecting'} leave...`,
          success: `Leave ${decision === 'approve' ? 'approved' : 'rejected'}.`,
          error: (err) => err.response?.data?.message || `Failed to ${decision} leave.`,
        }
      );
      const res = await productionResourceApi.get<ApiResponse<EmployeeLeave[]>>(`/resources/employees/${selected.id}/leave`);
      setLeaveHistory(res.data.data);
    } catch {
      // toast.promise handles display
    } finally {
      setDecidingLeaveId(null);
    }
  };

  const addSkill = async () => {
    if (!newSkillName.trim()) return;
    setAddingSkill(true);
    try {
      const res = await productionResourceApi.post<ApiResponse<Skill>>('/resources/skills', { name: newSkillName });
      const created = res.data.data;
      setAllSkills(prev => [...prev, created]);
      setCertForm(prev => ({ ...prev, skillId: created.id }));
      setNewSkillName('');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to add skill.');
    } finally {
      setAddingSkill(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    setSubmitting(true);
    try {
      await toast.promise(
        productionResourceApi.put(`/resources/employees/${selected.id}`, { ...form, shiftId: form.shiftId || null }),
        {
          loading: 'Updating employee...',
          success: `Employee ${selected.employeeCode} updated.`,
          error: (err) => err.response?.data?.message || 'Failed to update employee.',
        }
      );
      await fetchEmployees();
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  const handleRecordCertification = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    if (!certForm.skillId) { toast.error('Pick a skill first.'); return; }
    setRecordingCert(true);
    try {
      await toast.promise(
        productionResourceApi.post(`/resources/employees/${selected.id}/skills`, certForm),
        {
          loading: 'Recording skill certification...',
          success: 'Skill certification recorded.',
          error: (err) => err.response?.data?.message || 'Failed to record certification.',
        }
      );
      setCertForm(emptyCertForm);
      await fetchEmployees();
      const res = await productionResourceApi.get<ApiResponse<EmployeeSkillCertification[]>>(`/resources/employees/${selected.id}/skills`);
      setCertHistory(res.data.data);
    } catch {
      // toast.promise handles display
    } finally {
      setRecordingCert(false);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white">
            <HardHat size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Employee Directory</h1>
            <p className="text-slate-500 text-sm font-medium">
              Employee roster with certified skills, for staffing routing template steps.
            </p>
          </div>
        </div>
        <button onClick={() => setShowRegister(true)}
          className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-sm font-black rounded-xl hover:bg-blue-700 transition shadow-lg shadow-blue-200">
          <PlusCircle size={16} /> Register Employee
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* LEFT — Employee list */}
        <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
          <div className="p-5 border-b border-slate-100 flex items-center justify-between">
            <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Employee Register</h2>
            <button onClick={() => setActiveOnly(!activeOnly)}
              className={`text-[10px] font-black px-3 py-1 rounded-full transition ${activeOnly ? 'bg-blue-500 text-white' : 'bg-slate-100 text-slate-500'}`}>
              {activeOnly ? 'ACTIVE ONLY' : 'SHOW ALL'}
            </button>
          </div>

          {loading ? (
            <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
          ) : employees.length === 0 ? (
            <div className="p-16 text-center text-slate-300">
              <CheckCircle2 size={48} className="mx-auto mb-3 opacity-30" />
              <p className="font-bold uppercase tracking-widest text-xs">No employees registered yet</p>
            </div>
          ) : (
            <div className="divide-y divide-slate-50">
              {employees.map(e => (
                <button key={e.id} onClick={() => handleSelect(e)}
                  className={`w-full text-left p-5 hover:bg-slate-50 transition-all group ${
                    selected?.id === e.id ? 'bg-blue-50 border-l-4 border-blue-600' : ''
                  }`}>
                  <div className="flex justify-between items-start">
                    <div>
                      <div className="font-black text-slate-900 group-hover:text-blue-600 transition-colors">{e.name}</div>
                      <div className="text-[10px] text-slate-400 font-bold uppercase mt-0.5">
                        {e.employeeCode}{e.designation ? ` · ${e.designation}` : ''}{e.department ? ` · ${e.department}` : ''}
                      </div>
                    </div>
                    <ChevronRight size={16} className="text-slate-300 group-hover:text-blue-500 transition-colors" />
                  </div>
                  <div className="flex flex-wrap gap-1 mt-2">
                    {e.status === 'INACTIVE' && (
                      <span className="text-[10px] font-black px-2.5 py-1 rounded-full border bg-slate-100 text-slate-500 border-slate-200">INACTIVE</span>
                    )}
                    {e.occupancy === 'IN_OPERATION' && (
                      <span className="text-[10px] font-black px-2.5 py-1 rounded-full border bg-orange-50 text-orange-700 border-orange-200">IN OPERATION</span>
                    )}
                    {e.shiftName && (
                      <span className="text-[10px] font-black px-2.5 py-1 rounded-full border bg-sky-50 text-sky-700 border-sky-200">{e.shiftName}</span>
                    )}
                    {e.skills.map(s => (
                      <span key={s.skillId} className={`text-[10px] font-black px-2.5 py-1 rounded-full border ${skillBadgeStyle(s.status)}`}>
                        {s.skillName} · L{s.level}{s.status !== 'ACTIVE' ? ` (${s.status})` : ''}
                      </span>
                    ))}
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* RIGHT — Edit employee + skill certification */}
        <div className="space-y-6">
          <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
            <div className="p-5 border-b border-slate-100 flex items-center gap-2">
              <HardHat size={16} className="text-blue-500" />
              <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Edit Employee</h2>
            </div>

            {!selected ? (
              <div className="p-16 text-center text-slate-300">
                <HardHat size={48} className="mx-auto mb-3 opacity-20" />
                <p className="font-bold uppercase tracking-widest text-xs">Select an employee to edit</p>
              </div>
            ) : (
              <form onSubmit={handleSubmit} className="p-5 space-y-4">
                <div className="p-4 bg-slate-50 rounded-2xl border border-slate-100">
                  <div className="flex items-center justify-between">
                    <div className="font-black text-slate-900">{selected.employeeCode}</div>
                    {selected.occupancy === 'IN_OPERATION' && canForceRelease && (
                      <button type="button" onClick={() => handleForceRelease(selected)}
                        className="flex items-center gap-1 text-[10px] font-black px-2.5 py-1 rounded-full bg-orange-500 text-white hover:bg-orange-600">
                        <Unlock size={10} /> Force Release
                      </button>
                    )}
                  </div>
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Name</label>
                  <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })}
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Designation</label>
                    <input value={form.designation} onChange={e => setForm({ ...form, designation: e.target.value })}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                  </div>
                  <div>
                    <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Department</label>
                    <input value={form.department} onChange={e => setForm({ ...form, department: e.target.value })}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                  </div>
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Shift</label>
                  <div className="flex gap-2">
                    <select value={form.shiftId} onChange={e => setForm({ ...form, shiftId: Number(e.target.value) })}
                      className="flex-1 px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
                      <option value={0}>— No shift assigned —</option>
                      {allShifts.map(s => (
                        <option key={s.id} value={s.id}>{s.name} ({s.startTime.slice(0, 5)}-{s.endTime.slice(0, 5)}, {s.daysOfWeek})</option>
                      ))}
                    </select>
                    {canManageShiftsLeave && (
                      <button type="button" onClick={() => setShowAddShift(!showAddShift)}
                        className="px-3 py-2 bg-slate-900 text-white rounded-xl">
                        <Plus size={14} />
                      </button>
                    )}
                  </div>
                  {showAddShift && (
                    <div className="mt-2 p-3 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                      <input value={newShift.name} onChange={e => setNewShift({ ...newShift, name: e.target.value })}
                        placeholder="Shift name, e.g. Morning" className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                      <div className="grid grid-cols-2 gap-2">
                        <input type="time" value={newShift.startTime} onChange={e => setNewShift({ ...newShift, startTime: e.target.value })}
                          className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                        <input type="time" value={newShift.endTime} onChange={e => setNewShift({ ...newShift, endTime: e.target.value })}
                          className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                      </div>
                      <input value={newShift.daysOfWeek} onChange={e => setNewShift({ ...newShift, daysOfWeek: e.target.value })}
                        placeholder="MON,TUE,WED,THU,FRI" className="w-full px-3 py-2 bg-white border border-slate-200 rounded-lg text-xs" />
                      <button type="button" onClick={addShift} disabled={addingShift}
                        className="w-full py-2 bg-sky-600 text-white text-xs font-black rounded-lg disabled:opacity-60">
                        {addingShift ? 'Adding...' : 'Add Shift'}
                      </button>
                    </div>
                  )}
                </div>

                <div>
                  <label className="text-xs font-bold text-slate-500 uppercase mb-2 block tracking-wider">Status</label>
                  <div className="grid grid-cols-2 gap-2">
                    {['ACTIVE', 'INACTIVE'].map(s => (
                      <button key={s} type="button" onClick={() => setForm({ ...form, status: s })}
                        className={`py-2.5 rounded-xl font-black text-xs border-2 transition-all ${
                          form.status === s ? 'bg-slate-900 text-white border-slate-900' : 'bg-white text-slate-500 border-slate-200 hover:border-slate-400'
                        }`}>
                        {s}
                      </button>
                    ))}
                  </div>
                </div>

                <button type="submit" disabled={submitting}
                  className="w-full py-3 rounded-xl font-black text-white bg-blue-600 hover:bg-blue-700 transition-all active:scale-95 flex items-center justify-center gap-2 shadow-lg disabled:opacity-60">
                  {submitting ? <Loader2 size={16} className="animate-spin" /> : <CheckCircle2 size={16} />}
                  Save Changes
                </button>
              </form>
            )}
          </div>

          {selected && (
            <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
              <div className="p-5 border-b border-slate-100 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <PlusCircle size={16} className="text-blue-500" />
                  <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Record Skill Certification</h2>
                </div>
                <button onClick={() => setShowHistory(!showHistory)}
                  className="flex items-center gap-1 text-[10px] font-black px-3 py-1 rounded-full bg-slate-100 text-slate-500 hover:bg-slate-200">
                  <History size={12} /> {showHistory ? 'HIDE HISTORY' : 'SHOW HISTORY'}
                </button>
              </div>

              {!showHistory ? (
                <form onSubmit={handleRecordCertification} className="p-5 space-y-3">
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Skill</label>
                      <select value={certForm.skillId} onChange={e => setCertForm({ ...certForm, skillId: Number(e.target.value) })}
                        className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
                        <option value={0}>— Select —</option>
                        {allSkills.map(s => (
                          <option key={s.id} value={s.id}>{s.name}{s.certificationRequired ? ' *' : ''}</option>
                        ))}
                      </select>
                    </div>
                    <div>
                      <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">
                        Level (1=Beginner .. 5=Expert)
                      </label>
                      <input type="number" min={1} max={5} value={certForm.level}
                        onChange={e => setCertForm({ ...certForm, level: Number(e.target.value) })}
                        className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                    </div>
                  </div>

                  <div>
                    <label className="text-xs font-bold text-slate-500 uppercase mb-2 block tracking-wider">
                      Record As {certForm.status === 'REVOKED' && (
                        <span className="text-slate-400 font-normal normal-case">(this documents the certification is no longer valid, as a new event — not an edit of the old one)</span>
                      )}
                    </label>
                    <div className="grid grid-cols-2 gap-2">
                      {['ACTIVE', 'REVOKED'].map(s => (
                        <button key={s} type="button" onClick={() => setCertForm({ ...certForm, status: s })}
                          className={`py-2 rounded-xl font-black text-xs border-2 transition-all ${
                            (certForm.status || 'ACTIVE') === s
                              ? (s === 'REVOKED' ? 'bg-red-600 text-white border-red-600' : 'bg-slate-900 text-white border-slate-900')
                              : 'bg-white text-slate-500 border-slate-200 hover:border-slate-400'
                          }`}>
                          {s}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div className="flex gap-2">
                    <input value={newSkillName} onChange={e => setNewSkillName(e.target.value)} placeholder="Add a new skill to the catalog..."
                      className="flex-1 px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs" />
                    <button type="button" onClick={addSkill} disabled={addingSkill || !newSkillName.trim()}
                      className="px-3 py-2 bg-slate-900 text-white rounded-xl disabled:opacity-40">
                      {addingSkill ? <Loader2 size={14} className="animate-spin" /> : <Plus size={14} />}
                    </button>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Certificate Number</label>
                      <input value={certForm.certificateNumber} onChange={e => setCertForm({ ...certForm, certificateNumber: e.target.value })}
                        className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                    </div>
                    <div>
                      <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Experience (hours)</label>
                      <input type="number" min={0} value={certForm.experienceHours ?? ''}
                        onChange={e => setCertForm({ ...certForm, experienceHours: e.target.value ? Number(e.target.value) : undefined })}
                        className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Issued</label>
                      <input type="date" value={certForm.issuedDate} onChange={e => setCertForm({ ...certForm, issuedDate: e.target.value })}
                        className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                    </div>
                    <div>
                      <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Expires</label>
                      <input type="date" value={certForm.expiryDate} onChange={e => setCertForm({ ...certForm, expiryDate: e.target.value })}
                        className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Evaluated By</label>
                      <input value={certForm.evaluatedBy} onChange={e => setCertForm({ ...certForm, evaluatedBy: e.target.value })}
                        className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                    </div>
                    <div>
                      <label className="text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider">Next Evaluation</label>
                      <input type="date" value={certForm.nextEvaluationDate} onChange={e => setCertForm({ ...certForm, nextEvaluationDate: e.target.value })}
                        className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                    </div>
                  </div>

                  <textarea placeholder="Remarks" rows={2} value={certForm.remarks} onChange={e => setCertForm({ ...certForm, remarks: e.target.value })}
                    className="w-full px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm resize-none" />

                  <button type="submit" disabled={recordingCert}
                    className="w-full py-3 rounded-xl font-black text-white bg-indigo-600 hover:bg-indigo-700 transition-all active:scale-95 flex items-center justify-center gap-2 shadow-lg disabled:opacity-60">
                    {recordingCert ? <Loader2 size={16} className="animate-spin" /> : <CheckCircle2 size={16} />}
                    Record Certification
                  </button>
                </form>
              ) : (
                <div className="divide-y divide-slate-50 max-h-96 overflow-y-auto">
                  {certHistory.length === 0 ? (
                    <div className="p-10 text-center text-slate-300 text-xs font-bold uppercase tracking-widest">
                      No certification history yet
                    </div>
                  ) : certHistory.map(c => (
                    <div key={c.id} className="p-4">
                      <div className="flex items-center justify-between mb-1">
                        <span className={`px-3 py-1 rounded-full text-[10px] font-black border ${skillBadgeStyle(c.status)}`}>
                          {c.skillName} · Level {c.level} · {c.status}
                        </span>
                        <span className="text-[10px] text-slate-400 font-bold">{new Date(c.createdAt).toLocaleString()}</span>
                      </div>
                      {c.remarks && <p className="text-xs text-slate-600 font-medium italic">{c.remarks}</p>}
                      <div className="flex items-center justify-between mt-2 text-[10px] text-slate-400 font-bold">
                        <span>{c.certificateNumber || 'No cert #'} · {c.evaluatedBy || 'Unknown evaluator'}</span>
                        <span>{c.expiryDate ? `Expires ${c.expiryDate}` : 'No expiry'}</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {selected && (
            <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
              <div className="p-5 border-b border-slate-100 flex items-center gap-2">
                <CalendarDays size={16} className="text-sky-500" />
                <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Leave</h2>
              </div>

              {canManageShiftsLeave && (
                <form onSubmit={handleRequestLeave} className="p-5 space-y-3 border-b border-slate-100">
                  <div className="grid grid-cols-3 gap-2">
                    <select value={leaveForm.type} onChange={e => setLeaveForm({ ...leaveForm, type: e.target.value })}
                      className="px-2 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs">
                      <option value="VACATION">Vacation</option>
                      <option value="SICK">Sick</option>
                      <option value="OTHER">Other</option>
                    </select>
                    <input type="date" value={leaveForm.startDate} onChange={e => setLeaveForm({ ...leaveForm, startDate: e.target.value })}
                      className="px-2 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs" />
                    <input type="date" value={leaveForm.endDate} onChange={e => setLeaveForm({ ...leaveForm, endDate: e.target.value })}
                      className="px-2 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs" />
                  </div>
                  <input value={leaveForm.reason} onChange={e => setLeaveForm({ ...leaveForm, reason: e.target.value })}
                    placeholder="Reason (optional)" className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs" />
                  <button type="submit" disabled={requestingLeave}
                    className="w-full py-2.5 rounded-xl font-black text-white bg-sky-600 hover:bg-sky-700 text-xs disabled:opacity-60">
                    {requestingLeave ? 'Submitting...' : 'Request Leave'}
                  </button>
                </form>
              )}

              <div className="divide-y divide-slate-50 max-h-72 overflow-y-auto">
                {leaveHistory.length === 0 ? (
                  <div className="p-10 text-center text-slate-300 text-xs font-bold uppercase tracking-widest">
                    No leave requests yet
                  </div>
                ) : leaveHistory.map(l => (
                  <div key={l.id} className="p-4">
                    <div className="flex items-center justify-between mb-1">
                      <span className={`px-3 py-1 rounded-full text-[10px] font-black border ${leaveStatusStyle(l.status)}`}>
                        {l.type} · {l.status}
                      </span>
                      <span className="text-[10px] text-slate-400 font-bold">{l.startDate} → {l.endDate}</span>
                    </div>
                    {l.reason && <p className="text-xs text-slate-600 font-medium italic">{l.reason}</p>}
                    {l.status === 'PENDING' && canManageShiftsLeave ? (
                      <div className="flex gap-2 mt-2">
                        <button onClick={() => decideLeave(l, 'approve')} disabled={decidingLeaveId === l.id}
                          className="flex items-center gap-1 text-[10px] font-black px-2.5 py-1 rounded-full bg-emerald-500 text-white hover:bg-emerald-600 disabled:opacity-60">
                          <Check size={10} /> Approve
                        </button>
                        <button onClick={() => decideLeave(l, 'reject')} disabled={decidingLeaveId === l.id}
                          className="flex items-center gap-1 text-[10px] font-black px-2.5 py-1 rounded-full bg-red-500 text-white hover:bg-red-600 disabled:opacity-60">
                          <X size={10} /> Reject
                        </button>
                      </div>
                    ) : l.decidedBy ? (
                      <div className="text-[10px] text-slate-400 font-bold mt-2">Decided by {l.decidedBy}</div>
                    ) : null}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      <RegisterEmployeeModal isOpen={showRegister} onClose={() => setShowRegister(false)}
        onSuccess={() => { setShowRegister(false); fetchEmployees(); }} />
    </div>
  );
};

export default EmployeeDirectoryPage;
