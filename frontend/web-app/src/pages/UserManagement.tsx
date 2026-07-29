import React, { useState } from 'react';
import toast from 'react-hot-toast';
import { UserPlus } from 'lucide-react';
import { authApi } from '../api/authAxios';

const ALL_ROLES = ['STORE_KEEPER', 'QA_INSPECTOR', 'PRODUCTION_ENGINEER', 'PROCUREMENT_OFFICER', 'MANAGER', 'ADMIN'];

const UserManagement: React.FC = () => {
  const [form, setForm] = useState({ username: '', password: '', email: '' });
  const [roles, setRoles] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);

  const toggleRole = (role: string) =>
    setRoles(r => r.includes(role) ? r.filter(x => x !== role) : [...r, role]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (roles.length === 0) { toast.error('Pick at least one role'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        authApi.post('/admin/users', { ...form, roles }),
        { loading: 'Creating user...', success: 'User created', error: (err) => err.response?.data?.message || 'Failed to create user' }
      );
      setForm({ username: '', password: '', email: '' });
      setRoles([]);
    } catch {} finally { setSubmitting(false); }
  };

  return (
    <div className="p-8 max-w-lg">
      <div className="flex items-center gap-3 mb-6">
        <div className="p-2.5 bg-indigo-600 rounded-2xl text-white shadow-lg shadow-indigo-200">
          <UserPlus size={20} />
        </div>
        <div>
          <h1 className="text-xl font-black text-slate-900">Create User</h1>
          <p className="text-xs text-slate-500 font-medium">Give someone access with the right role</p>
        </div>
      </div>

      <form onSubmit={submit} className="bg-white rounded-3xl border border-slate-100 shadow-sm p-6 space-y-4">
        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Username</label>
          <input required value={form.username} onChange={e => setForm({ ...form, username: e.target.value })}
            className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
        </div>
        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Email</label>
          <input required type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })}
            className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
        </div>
        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Password</label>
          <input required type="password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })}
            className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
        </div>
        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-2">Role(s)</label>
          <div className="flex flex-wrap gap-2">
            {ALL_ROLES.map(role => (
              <button key={role} type="button" onClick={() => toggleRole(role)}
                className={`px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider transition-all ${
                  roles.includes(role) ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}>
                {role}
              </button>
            ))}
          </div>
        </div>
        <button type="submit" disabled={submitting}
          className="w-full py-2.5 bg-indigo-600 text-white font-black rounded-xl disabled:opacity-60">
          Create User
        </button>
      </form>
    </div>
  );
};

export default UserManagement;
