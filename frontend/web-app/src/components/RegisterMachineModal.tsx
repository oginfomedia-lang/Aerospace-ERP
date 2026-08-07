import React, { useState } from 'react';
import toast from 'react-hot-toast';
import productionResourceApi from '../api/productionResourceAxios';
import { ApiResponse } from '../types/inventory';
import { Machine } from '../types/productionResource';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

const machineTypes = [
  'CNC_MILL', 'CNC_LATHE', 'DRILL_PRESS', 'WELDING_STATION',
  'GRINDER', 'HEAT_TREATMENT', 'ASSEMBLY_STATION', 'OTHER',
];

export const RegisterMachineModal: React.FC<Props> = ({ isOpen, onClose, onSuccess }) => {
  const [machineCode, setMachineCode] = useState('');
  const [name, setName] = useState('');
  const [machineType, setMachineType] = useState('CNC_MILL');
  const [manufacturer, setManufacturer] = useState('');
  const [modelNumber, setModelNumber] = useState('');
  const [location, setLocation] = useState('');
  const [capacity, setCapacity] = useState('');
  const [maintenanceFrequencyDays, setMaintenanceFrequencyDays] = useState(90);
  const [submitting, setSubmitting] = useState(false);

  if (!isOpen) return null;

  const reset = () => {
    setMachineCode(''); setName(''); setMachineType('CNC_MILL');
    setManufacturer(''); setModelNumber(''); setLocation(''); setCapacity('');
    setMaintenanceFrequencyDays(90);
  };

  const submit = async () => {
    if (!machineCode.trim() || !name.trim()) { toast.error('Machine code and name are required'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        productionResourceApi.post<ApiResponse<Machine>>('/resources/machines', {
          machineCode, name, machineType, manufacturer, modelNumber, location, capacity,
          maintenanceFrequencyDays,
        }),
        { loading: 'Registering machine...', success: 'Machine registered', error: (err) => err.response?.data?.message || 'Failed to register' }
      );
      onSuccess();
      reset();
    } catch {} finally { setSubmitting(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-lg p-6 space-y-4">
        <h2 className="text-lg font-black text-slate-900">Register Machine</h2>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Machine Code</label>
            <input value={machineCode} onChange={e => setMachineCode(e.target.value)} placeholder="MCH-0001"
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-mono" />
          </div>
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Type</label>
            <select value={machineType} onChange={e => setMachineType(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm">
              {machineTypes.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
        </div>

        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Name</label>
          <input value={name} onChange={e => setName(e.target.value)} placeholder="CNC Mill #3"
            className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Manufacturer</label>
            <input value={manufacturer} onChange={e => setManufacturer(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Model Number</label>
            <input value={modelNumber} onChange={e => setModelNumber(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Location</label>
            <input value={location} onChange={e => setLocation(e.target.value)} placeholder="Shop Floor A"
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Capacity</label>
            <input value={capacity} onChange={e => setCapacity(e.target.value)} placeholder="500x300x400mm"
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
        </div>

        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Maintenance Frequency (days)</label>
          <input type="number" min={1} value={maintenanceFrequencyDays}
            onChange={e => setMaintenanceFrequencyDays(Number(e.target.value))}
            className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
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
