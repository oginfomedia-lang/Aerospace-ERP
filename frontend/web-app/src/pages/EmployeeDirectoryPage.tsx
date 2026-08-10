import React, { useEffect, useState } from 'react';
import productionResourceApi from '../api/productionResourceAxios';
import toast from 'react-hot-toast';
import { HardHat, Loader2, ChevronRight, CheckCircle2, PlusCircle, Plus } from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import { Employee, Skill } from '../types/productionResource';
import { RegisterEmployeeModal } from '../components/RegisterEmployeeModal';

const EmployeeDirectoryPage = () => {
  const [employees, setEmployees] = useState<Employee[]>([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState<Employee | null>(null);
  const [activeOnly, setActiveOnly] = useState(false);
  const [showRegister, setShowRegister] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [allSkills, setAllSkills] = useState<Skill[]>([]);
  const [newSkillName, setNewSkillName] = useState('');
  const [addingSkill, setAddingSkill] = useState(false);

  const [form, setForm] = useState({
    name: '', designation: '', department: '', status: 'ACTIVE', skillIds: [] as number[],
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

  useEffect(() => { fetchEmployees(); }, [activeOnly]); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(() => {
    fetchSkills();
  }, []);

  const handleSelect = (e: Employee) => {
    setSelected(e);
    setForm({
      name: e.name, designation: e.designation || '', department: e.department || '',
      status: e.status, skillIds: e.skills.map(s => s.id),
    });
  };

  const toggleSkill = (id: number) => {
    setForm(prev => ({
      ...prev,
      skillIds: prev.skillIds.includes(id) ? prev.skillIds.filter(s => s !== id) : [...prev.skillIds, id],
    }));
  };

  const addSkill = async () => {
    if(!newSkillName.trim()) return;
    setAddingSkill(true);
    try{
      const res = await productionResourceApi.post<ApiResponse<Skill>>('/resources/skills', {name: newSkillName });
      const created = res.data.data;
      setAllSkills(prev => [...prev, created]);
      setForm(prev => ({ ...prev, skillIds: [...prev.skillIds, created.id] })); //auto select it for this employee
      setNewSkillName('');
    } catch(err: any) {
      toast.error(err.response?.data?.message || 'Failed to add skill.');
    } finally {
      setAddingSkill(false);
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selected) return;
    setSubmitting(true);
    try {
      await toast.promise(
        productionResourceApi.put(`/resources/employees/${selected.id}`, form),
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
              Employee roster with skills, for staffing routing template steps.
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
                    {e.skills.map(s => (
                      <span key={s.id} className="text-[10px] font-black px-2.5 py-1 rounded-full border bg-indigo-50 text-indigo-700 border-indigo-200">
                        {s.name}
                      </span>
                    ))}
                  </div>
                </button>
              ))}
            </div>
          )}
        </div>

        {/* RIGHT — Edit employee */}
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
                <div className="font-black text-slate-900">{selected.employeeCode}</div>
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

              <div>
                <label className="text-xs font-bold text-slate-500 uppercase mb-2 block tracking-wider">Skills</label>
                <div className="flex flex-wrap gap-2">
                  {allSkills.map(s => (
                    <button key={s.id} type="button" onClick={() => toggleSkill(s.id)}
                      className={`px-3 py-1.5 rounded-full text-xs font-bold border transition ${
                        form.skillIds.includes(s.id) ? 'bg-indigo-600 text-white border-indigo-600' : 'bg-white text-slate-500 border-slate-200'
                      }`}>
                      {s.name}
                    </button>
                  ))}
                </div>
                <div className="flex gap-2">
                  <input value={newSkillName} onChange={e => setNewSkillName(e.target.value)} placeholder="Add a new skill..."
                    className="flex-1 px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
                  <button type="button" onClick={addSkill} disabled={addingSkill || !newSkillName.trim()}
                    className={`px-4 py-2.5 rounded-xl font-black text-white bg-blue-600 hover:bg-blue-700 transition-all active:scale-95 flex items-center gap-2 shadow-lg disabled:opacity-60`}>
                    {addingSkill ? <Loader2 size={16} className="animate-spin" /> : <Plus size={16} />}
                    Add Skill
                  </button>
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
      </div>

      <RegisterEmployeeModal isOpen={showRegister} onClose={() => setShowRegister(false)}
        onSuccess={() => { setShowRegister(false); fetchEmployees(); fetchSkills(); }} />
    </div>
  );
};

export default EmployeeDirectoryPage;
