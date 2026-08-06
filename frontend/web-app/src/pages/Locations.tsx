import React, { useState, useEffect } from 'react';
import { MapPin, Plus, Loader2, RefreshCw, Edit3, Ban, RotateCcw } from 'lucide-react';
import toast from 'react-hot-toast';
import api from '../api/axios';
import { Location, LocationRequest } from '../types/inventory';
import { useAuth } from '../context/AuthContext';
import { CAN_MANAGE_PARTS, hasAnyRole } from '../utils/permissions';

const emptyForm: LocationRequest = { code: '', name: '', zone: '' };

const Locations = () => {
  const { user } = useAuth();
  const canManage = hasAnyRole(user?.roles, CAN_MANAGE_PARTS);
  const [locations, setLocations] = useState<Location[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editingLocation, setEditingLocation] = useState<Location | null>(null);
  const [form, setForm] = useState<LocationRequest>(emptyForm);

  const fetchLocations = async () => {
    setLoading(true);
    try {
      const res = await api.get<Location[]>('/inventory/locations');
      setLocations(res.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchLocations(); }, []);

  const handleOpenCreate = () => {
    setEditingLocation(null);
    setForm(emptyForm);
    setShowModal(true);
  };

  const handleOpenEdit = (location: Location) => {
    setEditingLocation(location);
    setForm({ code: location.code, name: location.name, zone: location.zone || '' });
    setShowModal(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const payload: LocationRequest = { code: form.code, name: form.name, zone: form.zone || undefined };

    try {
      if (editingLocation) {
        const res = await api.put<Location>(`/inventory/locations/${editingLocation.id}`, payload);
        toast.success('Location updated successfully!');
        setLocations(locations.map(l => l.id === editingLocation.id ? res.data : l));
      } else {
        const res = await api.post<Location>('/inventory/locations', payload);
        toast.success('Location created successfully!');
        setLocations([...locations, res.data]);
      }
      setShowModal(false);
      setEditingLocation(null);
      setForm(emptyForm);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Save failed.');
    }
  };

  const handleToggleActive = async (location: Location) => {
    const action = location.active ? 'deactivate' : 'reactivate';
    const confirmed = window.confirm(
      location.active
        ? `Deactivate ${location.name} (${location.code})? Existing stock/GRN records referencing it are unaffected, but it will drop out of the location dropdown until reactivated.`
        : `Reactivate ${location.name} (${location.code})?`
    );
    if (!confirmed) return;

    try {
      const res = location.active
        ? await api.delete<Location>(`/inventory/locations/${location.id}`)
        : await api.patch<Location>(`/inventory/locations/${location.id}/reactivate`);
      toast.success(`Location ${action}d.`);
      setLocations(locations.map(l => l.id === location.id ? res.data : l));
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Locations</h1>
          <p className="text-slate-500 text-sm font-medium mt-1">Manage storage locations — used as the dropdown everywhere a location is entered</p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={fetchLocations}
            className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-xl transition-all"
          >
            <RefreshCw size={20} className={loading ? 'animate-spin' : ''} />
          </button>
          {canManage && (
            <button
              onClick={handleOpenCreate}
              className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold rounded-xl transition-all shadow-lg shadow-blue-600/20"
            >
              <Plus size={18} />
              Add Location
            </button>
          )}
        </div>
      </div>

      {loading && locations.length === 0 ? (
        <div className="flex items-center justify-center p-12">
          <Loader2 className="w-8 h-8 text-blue-600 animate-spin" />
        </div>
      ) : (
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase text-xs tracking-wider">
              <tr>
                <th className="px-6 py-4">Code</th>
                <th className="px-6 py-4">Name</th>
                <th className="px-6 py-4">Zone</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {locations.map(location => (
                <tr key={location.id} className={`hover:bg-slate-50/50 transition-colors ${!location.active ? 'opacity-60' : ''}`}>
                  <td className="px-6 py-4">
                    <span className="px-2 py-1 bg-slate-100 text-slate-600 font-mono text-xs rounded-md border border-slate-200">
                      {location.code}
                    </span>
                  </td>
                  <td className="px-6 py-4 font-bold text-slate-900">{location.name}</td>
                  <td className="px-6 py-4 text-slate-600">{location.zone || '—'}</td>
                  <td className="px-6 py-4">
                    <span className={`px-2.5 py-1 text-[11px] font-black tracking-wider uppercase rounded-full ${
                      location.active ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-200 text-slate-600'
                    }`}>
                      {location.active ? 'ACTIVE' : 'INACTIVE'}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-right">
                    <div className="flex justify-end gap-1">
                      {canManage && (
                        <button
                          onClick={() => handleOpenEdit(location)}
                          title="Edit location"
                          className="p-2 text-slate-400 hover:text-blue-600 hover:bg-blue-50 rounded-xl transition"
                        >
                          <Edit3 size={16} />
                        </button>
                      )}
                      {canManage && (
                        <button
                          onClick={() => handleToggleActive(location)}
                          title={location.active ? 'Deactivate location' : 'Reactivate location'}
                          className={`p-2 rounded-xl transition ${location.active ? 'text-slate-400 hover:text-red-600 hover:bg-red-50' : 'text-slate-400 hover:text-emerald-600 hover:bg-emerald-50'}`}
                        >
                          {location.active ? <Ban size={16} /> : <RotateCcw size={16} />}
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
              {locations.length === 0 && !loading && (
                <tr>
                  <td colSpan={5} className="px-6 py-12 text-center text-slate-500">
                    <MapPin className="w-12 h-12 mx-auto text-slate-300 mb-3" />
                    <p className="font-medium">No locations found</p>
                    <p className="text-sm mt-1">Add a storage location to get started.</p>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
          </div>
        </div>
      )}

      {showModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl w-full max-w-md overflow-hidden shadow-2xl animate-in fade-in zoom-in-95 duration-200">
            <div className="p-6 border-b border-slate-100">
              <h2 className="text-xl font-bold text-slate-900">
                {editingLocation ? `Edit Location: ${editingLocation.name}` : 'Add New Location'}
              </h2>
            </div>

            <form onSubmit={handleSubmit} className="p-6 space-y-4">
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Code</label>
                <input required type="text" value={form.code} onChange={e => setForm({ ...form, code: e.target.value })}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all font-mono"
                  placeholder="e.g. WH1-RACK-2" />
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Name</label>
                <input required type="text" value={form.name} onChange={e => setForm({ ...form, name: e.target.value })}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                  placeholder="e.g. Warehouse 1 - Rack 2" />
              </div>
              <div>
                <label className="block text-sm font-semibold text-slate-700 mb-1.5">Zone (optional)</label>
                <input type="text" value={form.zone} onChange={e => setForm({ ...form, zone: e.target.value })}
                  className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                  placeholder="e.g. Warehouse 1" />
              </div>

              <div className="flex gap-3 pt-4">
                <button type="button" onClick={() => { setShowModal(false); setEditingLocation(null); }}
                  className="flex-1 px-4 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold rounded-xl transition-all">
                  Cancel
                </button>
                <button type="submit"
                  className="flex-1 px-4 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl transition-all shadow-lg shadow-blue-600/20">
                  {editingLocation ? 'Save Changes' : 'Save Location'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default Locations;
