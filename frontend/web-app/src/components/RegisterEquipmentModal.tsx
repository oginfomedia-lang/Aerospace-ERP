import React, { useState } from 'react';
import toast from 'react-hot-toast';
import qaApi from '../api/qaAxios';
import { ApiResponse } from '../types/inventory';
import { Equipment } from '../types/qa';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

const equipmentTypes = ['GAUGE', 'TOOL', 'INSTRUMENT', 'FIXTURE', 'TEST_EQUIPMENT'];

export const RegisterEquipmentModal: React.FC<Props> = ({ isOpen, onClose, onSuccess }) => {
  const [assetTag, setAssetTag] = useState('');
  const [name, setName] = useState('');
  const [equipmentType, setEquipmentType] = useState('GAUGE');
  const [manufacturer, setManufacturer] = useState('');
  const [modelNumber, setModelNumber] = useState('');
  const [location, setLocation] = useState('');
  const [calibrationFrequencyDays, setCalibrationFrequencyDays] = useState(90);
  const [submitting, setSubmitting] = useState(false);

  if (!isOpen) return null;

  const reset = () => {
    setAssetTag(''); setName(''); setEquipmentType('GAUGE');
    setManufacturer(''); setModelNumber(''); setLocation('');
    setCalibrationFrequencyDays(90);
  };

  const submit = async () => {
    if (!assetTag.trim() || !name.trim()) { toast.error('Asset tag and name are required'); return; }
    setSubmitting(true);
    try {
      await toast.promise(
        qaApi.post<ApiResponse<Equipment>>('/qa/calibration/equipment', {
          assetTag, name, equipmentType, manufacturer, modelNumber, location,
          calibrationFrequencyDays,
        }),
        { loading: 'Registering equipment...', success: 'Equipment registered', error: (err) => err.response?.data?.message || 'Failed to register' }
      );
      onSuccess();
      reset();
    } catch {} finally { setSubmitting(false); }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
      <div className="bg-white rounded-3xl shadow-2xl w-full max-w-lg p-6 space-y-4">
        <h2 className="text-lg font-black text-slate-900">Register Equipment</h2>

        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Asset Tag</label>
            <input value={assetTag} onChange={e => setAssetTag(e.target.value)} placeholder="CAL-0001"
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm font-mono" />
          </div>
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Type</label>
            <select value={equipmentType} onChange={e => setEquipmentType(e.target.value)}
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm">
              {equipmentTypes.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
        </div>

        <div>
          <label className="text-xs font-black text-slate-500 uppercase block mb-1">Name</label>
          <input value={name} onChange={e => setName(e.target.value)} placeholder="Digital Caliper 0-150mm"
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
            <input value={location} onChange={e => setLocation(e.target.value)} placeholder="QC Lab"
              className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
          <div>
            <label className="text-xs font-black text-slate-500 uppercase block mb-1">Calibration Frequency (days)</label>
            <input type="number" min={1} value={calibrationFrequencyDays}
              onChange={e => setCalibrationFrequencyDays(Number(e.target.value))}
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
