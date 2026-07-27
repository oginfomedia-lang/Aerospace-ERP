import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const SupplierLogin: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    const ok = await login(username, password);
    setBusy(false);
    if (ok) navigate('/supplier/dashboard');
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50">
      <form onSubmit={submit} className="bg-white p-8 rounded-3xl shadow-xl w-full max-w-sm space-y-4">
        <h1 className="text-xl font-black text-slate-900">Supplier Portal</h1>
        <p className="text-xs text-slate-500">Sign in to view RFQs and submit quotations.</p>
        <input value={username} onChange={e => setUsername(e.target.value)} placeholder="Email"
          className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
        <input value={password} onChange={e => setPassword(e.target.value)} type="password" placeholder="Password"
          className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
        <button disabled={busy} className="w-full py-2.5 bg-indigo-600 text-white font-black rounded-xl disabled:opacity-60">
          Sign In
        </button>
      </form>
    </div>
  );
};

export default SupplierLogin;