import React, { useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { authApi } from '../../api/authAxios';

const SupplierActivate: React.FC = () => {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const token = params.get('token') || '';
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    try {
      await authApi.post('/suppliers/activate', { token, password });
      toast.success('Account activated — please sign in');
      navigate('/supplier/login');
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Activation failed');
    } finally { setBusy(false); }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50">
      <form onSubmit={submit} className="bg-white p-8 rounded-3xl shadow-xl w-full max-w-sm space-y-4">
        <h1 className="text-xl font-black text-slate-900">Activate Your Account</h1>
        <input value={password} onChange={e => setPassword(e.target.value)} type="password"
          placeholder="Set a password" className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
        <button disabled={busy || !token} className="w-full py-2.5 bg-indigo-600 text-white font-black rounded-xl disabled:opacity-60">
          Activate
        </button>
      </form>
    </div>
  );
};

export default SupplierActivate;