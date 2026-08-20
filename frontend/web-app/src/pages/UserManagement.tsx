import React, { useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { UserPlus, Loader2, Users } from 'lucide-react';
import { authApi } from '../api/authAxios';

interface AdminUser {
  id: number;
  username: string;
  email: string;
  roles: string[];
  enabled: boolean;
}

const ALL_ROLES = [
  'STORE_KEEPER', 'QA_INSPECTOR', 'PRODUCTION_ENGINEER', 'PROCUREMENT_OFFICER', 'MANAGER', 'ADMIN',
  'PRODUCTION_SUPERVISOR', 'MAINTENANCE_ENGINEER', 'SALES_EXECUTIVE', 'SALES_MANAGER', 'WAREHOUSE_OPERATOR',
  'FINANCE_MANAGER', 'ACCOUNTANT',
];

const UserManagement: React.FC = () => {
  const [form, setForm] = useState({ username: '', password: '', email: '' });
  const [roles, setRoles] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loadingUsers, setLoadingUsers] = useState(true);
  const [editingUserId, setEditingUserId] = useState<number | null>(null);
  const [editRoles, setEditRoles] = useState<string[]>([]);
  const [savingRoles, setSavingRoles] = useState(false);

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

  const fetchUsers = async () => {
    setLoadingUsers(true);
    try {
      const res = await authApi.get<AdminUser[]>('/admin/users');
      setUsers(res.data);
    } catch { toast.error('Failed to load users'); }
    finally{ setLoadingUsers(false); }
  }
  useEffect(() => { fetchUsers(); }, []);

  const toggleStatus = async (u: AdminUser) => {
    await toast.promise(
      authApi.patch(`/admin/users/${u.id}/status`, { enabled: !u.enabled }),
      { loading: 'Updating...', success: `User ${u.enabled ? 'deactivated' : 'activated'}`, error: 'Failed to update status' }
    );
    fetchUsers();
  };

  const startEditRoles = (u: AdminUser) => {
    setEditingUserId(u.id);
    setEditRoles(u.roles);
  };

  const cancelEditRoles = () => {
    setEditingUserId(null);
    setEditRoles([]);
  };

  const toggleEditRole = (role: string) =>
    setEditRoles(r => r.includes(role) ? r.filter(x => x !== role) : [...r, role]);

  const saveRoles = async (u: AdminUser) => {
    if (editRoles.length === 0) { toast.error('Pick at least one role'); return; }
    setSavingRoles(true);
    try {
      await toast.promise(
        authApi.patch(`/admin/users/${u.id}/roles`, { roles: editRoles }),
        { loading: 'Updating roles...', success: 'Roles updated', error: (err) => err.response?.data?.message || 'Failed to update roles' }
      );
      setEditingUserId(null);
      setEditRoles([]);
      fetchUsers();
    } catch {} finally { setSavingRoles(false); }
  };

  return (
    <div className="p-8 max-w-4xl">
      <div className="flex items-center gap-3 mb-6">
        <div className="p-2.5 bg-indigo-600 rounded-2xl text-white shadow-lg shadow-indigo-200">
          <UserPlus size={20} />
        </div>
        <div>
          <h1 className="text-xl font-black text-slate-900">Create User</h1>
          <p className="text-xs text-slate-500 font-medium">Give someone access with the right role</p>
        </div>
      </div>

      <form onSubmit={submit} className="bg-white rounded-3xl border border-slate-100 shadow-sm p-6 space-y-4 max-w-lg">
        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Username</label>
          <input required minLength={3} value={form.username} onChange={e => setForm({ ...form, username: e.target.value })}
            className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          <p className="text-[11px] text-slate-400 mt-1">At least 3 characters</p>
        </div>
        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Email</label>
          <input required type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })}
            className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
        </div>
        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Password</label>
          <input required type="password" minLength={6} value={form.password} onChange={e => setForm({ ...form, password: e.target.value })}
            className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          <p className="text-[11px] text-slate-400 mt-1">At least 6 characters</p>
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

      <div className="mt-8 flex items-center gap-3 mb-6">
        <div className="p-2.5 bg-indigo-600 rounded-2xl text-white shadow-lg shadow-indigo-200">
          <Users size={20} />
        </div>
        <div>
          <h1 className="text-xl font-black text-slate-900">All Users</h1>
          <p className="text-xs text-slate-500 font-medium">View and manage created accounts</p>
        </div>
      </div>

      <div className="bg-white rounded-3xl border border-slate-100 shadow-sm overflow-hidden">
        {loadingUsers ? (
          <div className="p-10 text-center">
            <Loader2 className="animate-spin text-indigo-500 mx-auto" size={24} />
          </div>
        ) : users.length === 0 ? (
          <div className="p-10 text-center text-sm text-slate-400 font-medium">No users found</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-[10px] font-black text-slate-500 uppercase tracking-wider">
                <tr>
                  <th className="text-left px-5 py-3">Username</th>
                  <th className="text-left px-5 py-3">Email</th>
                  <th className="text-left px-5 py-3">Roles</th>
                  <th className="text-left px-5 py-3">Status</th>
                  <th className="text-right px-5 py-3">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-50">
                {users.map(u => (
                  <tr key={u.id}>
                    <td className="px-5 py-3 font-bold text-slate-900">{u.username}</td>
                    <td className="px-5 py-3 text-slate-500">{u.email}</td>
                    <td className="px-5 py-3">
                      {editingUserId === u.id ? (
                        <div className="flex flex-wrap gap-1 max-w-xs">
                          {ALL_ROLES.map(role => (
                            <button key={role} type="button" onClick={() => toggleEditRole(role)}
                              className={`px-2 py-0.5 rounded-full text-[10px] font-bold uppercase tracking-wider transition-all ${
                                editRoles.includes(role) ? 'bg-indigo-600 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'}`}>
                              {role}
                            </button>
                          ))}
                        </div>
                      ) : (
                        <div className="flex flex-wrap gap-1">
                          {u.roles.map(r => (
                            <span key={r} className="text-[10px] font-black px-2 py-0.5 rounded-full border bg-indigo-50 text-indigo-700 border-indigo-200">
                              {r}
                            </span>
                          ))}
                        </div>
                      )}
                    </td>
                    <td className="px-5 py-3">
                      <span className={`text-[10px] font-black px-2.5 py-1 rounded-full border ${
                        u.enabled ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : 'bg-red-50 text-red-700 border-red-200'}`}>
                        {u.enabled ? 'ACTIVE' : 'DISABLED'}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-right">
                      {editingUserId === u.id ? (
                        <div className="flex justify-end gap-2">
                          <button onClick={() => cancelEditRoles()} disabled={savingRoles}
                            className="px-3 py-1.5 rounded-xl text-xs font-black bg-slate-100 text-slate-600 hover:bg-slate-200 disabled:opacity-60">
                            Cancel
                          </button>
                          <button onClick={() => saveRoles(u)} disabled={savingRoles}
                            className="px-3 py-1.5 rounded-xl text-xs font-black bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-60">
                            Save
                          </button>
                        </div>
                      ) : (
                        <div className="flex justify-end gap-2">
                          <button onClick={() => startEditRoles(u)}
                            className="px-3 py-1.5 rounded-xl text-xs font-black bg-indigo-50 text-indigo-700 hover:bg-indigo-100">
                            Edit Roles
                          </button>
                          <button onClick={() => toggleStatus(u)}
                            className={`px-3 py-1.5 rounded-xl text-xs font-black ${
                              u.enabled ? 'bg-red-50 text-red-700 hover:bg-red-100' : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'}`}>
                            {u.enabled ? 'Deactivate' : 'Activate'}
                          </button>
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default UserManagement;
