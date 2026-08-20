import React, { useEffect, useState } from 'react';
import financeApi from '../api/financeAxios';
import toast from 'react-hot-toast';
import { Landmark, Loader2, PlusCircle, PowerOff, Pencil } from 'lucide-react';
import { ApiResponse } from '../types/inventory';
import { Organization, Plant, CostCenter, ProfitCenter } from '../types/finance';
import { useAuth } from '../context/AuthContext';
import { hasAnyRole, CAN_APPROVE_FINANCE } from '../utils/permissions';

type TabKey = 'organizations' | 'plants' | 'costCenters' | 'profitCenters';

const TABS: { key: TabKey; label: string; url: string }[] = [
  { key: 'organizations', label: 'Organizations', url: '/finance/organizations' },
  { key: 'plants', label: 'Plants', url: '/finance/plants' },
  { key: 'costCenters', label: 'Cost Centers', url: '/finance/cost-centers' },
  { key: 'profitCenters', label: 'Profit Centers', url: '/finance/profit-centers' },
];

const MasterDataPage = () => {
  const { user } = useAuth();
  const canManage = hasAnyRole(user?.roles, CAN_APPROVE_FINANCE);
  const [tab, setTab] = useState<TabKey>('organizations');

  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [plants, setPlants] = useState<Plant[]>([]);
  const [costCenters, setCostCenters] = useState<CostCenter[]>([]);
  const [profitCenters, setProfitCenters] = useState<ProfitCenter[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);

  const [orgForm, setOrgForm] = useState({ orgCode: '', orgName: '', baseCurrency: '' });
  const [plantForm, setPlantForm] = useState({ plantCode: '', plantName: '', organizationId: '' as number | '' });
  const [ccForm, setCcForm] = useState({ costCenterCode: '', costCenterName: '', description: '', plantId: '' as number | '' });
  const [pcForm, setPcForm] = useState({ profitCenterCode: '', profitCenterName: '', plantId: '' as number | '' });
  const [submitting, setSubmitting] = useState(false);

  const fetchAll = async () => {
    setLoading(true);
    try {
      const [oRes, pRes, ccRes, pcRes] = await Promise.all([
        financeApi.get<ApiResponse<Organization[]>>('/finance/organizations'),
        financeApi.get<ApiResponse<Plant[]>>('/finance/plants'),
        financeApi.get<ApiResponse<CostCenter[]>>('/finance/cost-centers'),
        financeApi.get<ApiResponse<ProfitCenter[]>>('/finance/profit-centers'),
      ]);
      setOrganizations(oRes.data.data);
      setPlants(pRes.data.data);
      setCostCenters(ccRes.data.data);
      setProfitCenters(pcRes.data.data);
    } catch {
      toast.error('Failed to fetch master data.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchAll(); }, []);

  const save = async (url: string, body: any, label: string, id?: number) => {
    setSubmitting(true);
    try {
      await toast.promise(
        id ? financeApi.put(`${url}/${id}`, body) : financeApi.post(url, body),
        {
          loading: id ? `Updating ${label}...` : `Creating ${label}...`,
          success: id ? `${label} updated.` : `${label} created.`,
          error: (err) => err.response?.data?.message || `Failed to save ${label}.`,
        }
      );
      setShowForm(false);
      setEditingId(null);
      await fetchAll();
    } catch {
      // toast.promise handles display
    } finally {
      setSubmitting(false);
    }
  };

  const openCreate = () => { setEditingId(null); setShowForm(true); };

  const deactivate = async (url: string, code: string) => {
    try {
      await toast.promise(
        financeApi.patch(`${url}/deactivate`, {}),
        { loading: `Deactivating ${code}...`, success: `${code} deactivated.`, error: (err) => err.response?.data?.message || 'Could not deactivate.' }
      );
      await fetchAll();
    } catch {
      // toast.promise handles display
    }
  };

  const currentTabMeta = TABS.find(t => t.key === tab)!;

  const renderTable = (rows: { id: number; code: string; name: string; active: boolean; baseUrl: string; onEdit: () => void }[]) => (
    <div className="bg-white rounded-3xl shadow-sm border border-slate-100 overflow-hidden">
      {loading ? (
        <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>
      ) : rows.length === 0 ? (
        <div className="p-16 text-center text-slate-300">
          <Landmark size={48} className="mx-auto mb-3 opacity-30" />
          <p className="font-bold uppercase tracking-widest text-xs">No records yet</p>
        </div>
      ) : (
        <div className="divide-y divide-slate-50">
          {rows.map(r => (
            <div key={r.id} className="p-4 px-5 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="font-mono text-xs font-black text-slate-800">{r.code}</span>
                <span className="text-sm text-slate-600 font-medium">{r.name}</span>
                <span className={`text-[10px] font-black ${r.active ? 'text-emerald-600' : 'text-slate-300'}`}>{r.active ? 'ACTIVE' : 'INACTIVE'}</span>
              </div>
              {canManage && (
                <div className="flex items-center gap-2">
                  <button onClick={r.onEdit}
                    className="flex items-center gap-1 text-[10px] font-black px-2.5 py-1 rounded-full bg-white border border-slate-200 text-slate-500 hover:border-blue-300 hover:text-blue-600 transition">
                    <Pencil size={11} /> Edit
                  </button>
                  {r.active && (
                    <button onClick={() => deactivate(r.baseUrl, r.code)}
                      className="flex items-center gap-1 text-[10px] font-black px-2.5 py-1 rounded-full bg-white border border-slate-200 text-slate-500 hover:border-red-300 hover:text-red-500 transition">
                      <PowerOff size={11} /> Deactivate
                    </button>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );

  const startEditOrg = (o: Organization) => { setEditingId(o.id); setOrgForm({ orgCode: o.orgCode, orgName: o.orgName, baseCurrency: o.baseCurrency || '' }); setTab('organizations'); setShowForm(true); };
  const startEditPlant = (p: Plant) => { setEditingId(p.id); setPlantForm({ plantCode: p.plantCode, plantName: p.plantName, organizationId: p.organizationId ?? '' }); setTab('plants'); setShowForm(true); };
  const startEditCc = (c: CostCenter) => { setEditingId(c.id); setCcForm({ costCenterCode: c.costCenterCode, costCenterName: c.costCenterName, description: c.description || '', plantId: c.plantId ?? '' }); setTab('costCenters'); setShowForm(true); };
  const startEditPc = (p: ProfitCenter) => { setEditingId(p.id); setPcForm({ profitCenterCode: p.profitCenterCode, profitCenterName: p.profitCenterName, plantId: p.plantId ?? '' }); setTab('profitCenters'); setShowForm(true); };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white">
            <Landmark size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Master Data</h1>
            <p className="text-slate-500 text-sm font-medium">Organizations, Plants, Cost Centers, Profit Centers — where money moved.</p>
          </div>
        </div>
        {canManage && (
          <button onClick={() => { if (showForm) { setShowForm(false); } else { openCreate(); } }}
            className="flex items-center gap-2 px-4 py-2.5 bg-blue-600 text-white text-sm font-black rounded-xl hover:bg-blue-700 transition shadow-lg shadow-blue-200">
            <PlusCircle size={16} /> New {currentTabMeta.label.replace(/s$/, '')}
          </button>
        )}
      </div>

      <div className="flex gap-2">
        {TABS.map(t => (
          <button key={t.key} onClick={() => { setTab(t.key); setShowForm(false); setEditingId(null); }}
            className={`px-4 py-2 rounded-xl text-xs font-black transition ${tab === t.key ? 'bg-slate-900 text-white' : 'bg-white text-slate-500 border border-slate-200'}`}>
            {t.label}
          </button>
        ))}
      </div>

      {showForm && tab === 'organizations' && (
        <form onSubmit={e => { e.preventDefault(); if (!orgForm.orgCode || !orgForm.orgName) { toast.error('Code and name are required.'); return; } save('/finance/organizations', orgForm, 'organization', editingId || undefined); }}
          className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">{editingId ? 'Edit Organization' : 'New Organization'}</h2>
          <div className="grid grid-cols-3 gap-3">
            <input value={orgForm.orgCode} onChange={e => setOrgForm({ ...orgForm, orgCode: e.target.value })} placeholder="Org Code" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <input value={orgForm.orgName} onChange={e => setOrgForm({ ...orgForm, orgName: e.target.value })} placeholder="Org Name" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <input value={orgForm.baseCurrency} onChange={e => setOrgForm({ ...orgForm, baseCurrency: e.target.value.toUpperCase() })} placeholder="Base Currency (e.g. INR)" maxLength={3} className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
          </div>
          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => { setShowForm(false); setEditingId(null); }} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting} className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">{editingId ? 'Save Changes' : 'Create'}</button>
          </div>
        </form>
      )}

      {showForm && tab === 'plants' && (
        <form onSubmit={e => { e.preventDefault(); if (!plantForm.plantCode || !plantForm.plantName) { toast.error('Code and name are required.'); return; } save('/finance/plants', { ...plantForm, organizationId: plantForm.organizationId || null }, 'plant', editingId || undefined); }}
          className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">{editingId ? 'Edit Plant' : 'New Plant'}</h2>
          <div className="grid grid-cols-3 gap-3">
            <input value={plantForm.plantCode} onChange={e => setPlantForm({ ...plantForm, plantCode: e.target.value })} placeholder="Plant Code" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <input value={plantForm.plantName} onChange={e => setPlantForm({ ...plantForm, plantName: e.target.value })} placeholder="Plant Name" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <select value={plantForm.organizationId} onChange={e => setPlantForm({ ...plantForm, organizationId: e.target.value ? Number(e.target.value) : '' })} className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
              <option value="">— Organization (optional) —</option>
              {organizations.map(o => <option key={o.id} value={o.id}>{o.orgCode} — {o.orgName}</option>)}
            </select>
          </div>
          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => { setShowForm(false); setEditingId(null); }} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting} className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">{editingId ? 'Save Changes' : 'Create'}</button>
          </div>
        </form>
      )}

      {showForm && tab === 'costCenters' && (
        <form onSubmit={e => { e.preventDefault(); if (!ccForm.costCenterCode || !ccForm.costCenterName) { toast.error('Code and name are required.'); return; } save('/finance/cost-centers', { ...ccForm, plantId: ccForm.plantId || null }, 'cost center', editingId || undefined); }}
          className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">{editingId ? 'Edit Cost Center' : 'New Cost Center'}</h2>
          <div className="grid grid-cols-2 gap-3">
            <input value={ccForm.costCenterCode} onChange={e => setCcForm({ ...ccForm, costCenterCode: e.target.value })} placeholder="Cost Center Code" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <input value={ccForm.costCenterName} onChange={e => setCcForm({ ...ccForm, costCenterName: e.target.value })} placeholder="Cost Center Name" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <input value={ccForm.description} onChange={e => setCcForm({ ...ccForm, description: e.target.value })} placeholder="Description (optional)" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <select value={ccForm.plantId} onChange={e => setCcForm({ ...ccForm, plantId: e.target.value ? Number(e.target.value) : '' })} className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
              <option value="">— Plant (optional) —</option>
              {plants.map(p => <option key={p.id} value={p.id}>{p.plantCode} — {p.plantName}</option>)}
            </select>
          </div>
          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => { setShowForm(false); setEditingId(null); }} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting} className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">{editingId ? 'Save Changes' : 'Create'}</button>
          </div>
        </form>
      )}

      {showForm && tab === 'profitCenters' && (
        <form onSubmit={e => { e.preventDefault(); if (!pcForm.profitCenterCode || !pcForm.profitCenterName) { toast.error('Code and name are required.'); return; } save('/finance/profit-centers', { ...pcForm, plantId: pcForm.plantId || null }, 'profit center', editingId || undefined); }}
          className="bg-white rounded-3xl shadow-sm border border-slate-100 p-6 space-y-4">
          <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">{editingId ? 'Edit Profit Center' : 'New Profit Center'}</h2>
          <div className="grid grid-cols-3 gap-3">
            <input value={pcForm.profitCenterCode} onChange={e => setPcForm({ ...pcForm, profitCenterCode: e.target.value })} placeholder="Profit Center Code" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <input value={pcForm.profitCenterName} onChange={e => setPcForm({ ...pcForm, profitCenterName: e.target.value })} placeholder="Profit Center Name" className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm" />
            <select value={pcForm.plantId} onChange={e => setPcForm({ ...pcForm, plantId: e.target.value ? Number(e.target.value) : '' })} className="px-3 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-sm">
              <option value="">— Plant (optional) —</option>
              {plants.map(p => <option key={p.id} value={p.id}>{p.plantCode} — {p.plantName}</option>)}
            </select>
          </div>
          <div className="flex justify-end gap-2">
            <button type="button" onClick={() => { setShowForm(false); setEditingId(null); }} className="px-4 py-2 text-sm font-bold text-slate-500">Cancel</button>
            <button type="submit" disabled={submitting} className="px-5 py-2 bg-blue-600 text-white text-sm font-black rounded-xl disabled:opacity-60">{editingId ? 'Save Changes' : 'Create'}</button>
          </div>
        </form>
      )}

      {tab === 'organizations' && renderTable(organizations.map(o => ({ id: o.id, code: o.orgCode, name: o.orgName, active: o.activeStatus, baseUrl: `/finance/organizations/${o.id}`, onEdit: () => startEditOrg(o) })))}
      {tab === 'plants' && renderTable(plants.map(p => ({ id: p.id, code: p.plantCode, name: p.plantName, active: p.activeStatus, baseUrl: `/finance/plants/${p.id}`, onEdit: () => startEditPlant(p) })))}
      {tab === 'costCenters' && renderTable(costCenters.map(c => ({ id: c.id, code: c.costCenterCode, name: c.costCenterName, active: c.activeStatus, baseUrl: `/finance/cost-centers/${c.id}`, onEdit: () => startEditCc(c) })))}
      {tab === 'profitCenters' && renderTable(profitCenters.map(p => ({ id: p.id, code: p.profitCenterCode, name: p.profitCenterName, active: p.activeStatus, baseUrl: `/finance/profit-centers/${p.id}`, onEdit: () => startEditPc(p) })))}
    </div>
  );
};

export default MasterDataPage;
