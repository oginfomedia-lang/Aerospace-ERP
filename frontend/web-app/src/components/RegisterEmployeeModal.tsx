import React, { useState } from 'react';
import toast from 'react-hot-toast';
import productionResourceApi from '../api/productionResourceAxios';
import { ApiResponse } from '../types/inventory';
import { Employee } from '../types/productionResource';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

// Skills are recorded AFTER registration, from the Edit panel's "Record Skill
// Certification" section — a level/certificate/expiry form doesn't fit here the
// way a simple on/off toggle used to.
export const RegisterEmployeeModal: React.FC<Props> = ({ isOpen, onClose, onSuccess }) => {
  const [name, setName] = useState('');
  const [designation, setDesignation] = useState('');
  const [department, setDepartment] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (!isOpen) return null;

  const reset = () => {
    setName(''); setDesignation(''); setDepartment('');
  };

  const submit = async () => {
    if (!name.trim()) { toast.error('Name is required'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        productionResourceApi.post<ApiResponse<Employee>>('/resources/employees', {
          name, designation, department,
        }),
        { loading: 'Registering employee...', success: 'Employee registered', error: (err) => err.response?.data?.message || 'Failed to register' }
      );
      onSuccess();
      reset();
    } catch {} finally { setSubmitting(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-lg p-6 space-y-4">
        <h2 className="text-lg font-black text-slate-900">Register Employee</h2>

        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Name</label>
          <input value={name} onChange={e => setName(e.target.value)} placeholder="Jane Doe"
            className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Designation</label>
            <input value={designation} onChange={e => setDesignation(e.target.value)} placeholder="CNC Operator"
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Department</label>
            <input value={department} onChange={e => setDepartment(e.target.value)} placeholder="Machining"
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
        </div>

        <div className="flex justify-end gap-2 pt-2">
          <button onClick={() => { onClose(); reset(); }} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
          <button onClick={submit} disabled={submitting}
            className="px-5 py-2 bg-indigo-600 text-white text-sm font-black rounded-xl disabled:opacity-60">
            Register
          </button>
        </div>
      </div>
    </div>
  );
};
