import React, { useState } from 'react';
import toast from 'react-hot-toast';
import productionResourceApi from '../api/productionResourceAxios';
import { ApiResponse } from '../types/inventory';
import { Tooling } from '../types/productionResource';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

const toolingTypes = ['CUTTING_TOOL', 'FIXTURE', 'JIG', 'MEASURING_EQUIPMENT', 'OTHER'];

// toolCode is server-generated (DocumentSequenceService "TOOL") — same reason
// RegisterEmployeeModal doesn't ask for employeeCode. Never typed here.
export const RegisterToolingModal: React.FC<Props> = ({ isOpen, onClose, onSuccess }) => {
  const [name, setName] = useState('');
  const [toolingType, setToolingType] = useState('CUTTING_TOOL');
  const [location, setLocation] = useState('');
  const [submitting, setSubmitting] = useState(false);

  if (!isOpen) return null;

  const reset = () => {
    setName(''); setToolingType('CUTTING_TOOL'); setLocation('');
  };

  const submit = async () => {
    if (!name.trim()) { toast.error('Name is required'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        productionResourceApi.post<ApiResponse<Tooling>>('/resources/tooling', {
          name, toolingType, location,
        }),
        { loading: 'Registering tool...', success: 'Tool registered', error: (err) => err.response?.data?.message || 'Failed to register' }
      );
      onSuccess();
      reset();
    } catch {} finally { setSubmitting(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-lg p-6 space-y-4">
        <h2 className="text-lg font-black text-slate-900">Register Tool / Fixture</h2>

        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Name</label>
          <input value={name} onChange={e => setName(e.target.value)} placeholder="Welding Fixture #3"
            className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Type</label>
            <select value={toolingType} onChange={e => setToolingType(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm">
              {toolingTypes.map(t => <option key={t} value={t}>{t.replace('_', ' ')}</option>)}
            </select>
          </div>
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Location</label>
            <input value={location} onChange={e => setLocation(e.target.value)} placeholder="Shop Floor A"
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
