import React, { useEffect, useState } from 'react';
import { Building2, Save, Loader2 } from 'lucide-react';
import toast from 'react-hot-toast';
import procurementApi from '../api/procurementAxios';
import { OrganizationProfile, OrganizationProfileRequest } from '../types/procurement';

const emptyForm: OrganizationProfileRequest = {
  companyName: '', addressLine: '', city: '', state: '', stateCode: '', pincode: '', country: 'INDIA',
  phone: '', telefax: '', email: '',
  gstin: '', pan: '',
  range: '', division: '',
  bankName: '', bankBranch: '', bankAccountNumber: '', bankIfsc: '',
  defaultPaymentTerms: '', defaultPayment: '', defaultDeliveryTerms: '', defaultExciseDuty: '', termsAndConditions: '',
};

// Singleton "who are we" record — the letterhead, GSTIN/bank details, and default
// payment/delivery terms + terms-and-conditions boilerplate reused on every printed
// Purchase Order (see POReceiptDocument.tsx). There is exactly one row (id fixed at 1
// on the backend), so this page is a single edit form, not a CRUD list.
const OrgSettings = () => {
  const [form, setForm] = useState<OrganizationProfileRequest>(emptyForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  const fetchProfile = async () => {
    setLoading(true);
    try {
      const res = await procurementApi.get<OrganizationProfile>('/organization-profile');
      const { id, ...rest } = res.data;
      setForm({ ...emptyForm, ...rest });
    } catch {
      toast.error('Failed to load organization profile.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchProfile(); }, []);

  const set = (field: keyof OrganizationProfileRequest) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
      setForm({ ...form, [field]: e.target.value });

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      await procurementApi.put('/organization-profile', form);
      toast.success('Organization profile saved.');
    } catch {
      toast.error('Failed to save organization profile.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="p-16 text-center"><Loader2 className="animate-spin text-blue-500 mx-auto" size={28} /></div>;
  }

  const inputCls = "w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:ring-4 focus:ring-blue-500/10 focus:border-blue-500 outline-none transition-all";
  const labelCls = "text-xs font-bold text-slate-500 uppercase mb-1.5 block tracking-wider";

  return (
    <form onSubmit={handleSave} className="space-y-6 animate-in fade-in duration-500 max-w-4xl">
      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-blue-600 rounded-2xl text-white">
            <Building2 size={24} />
          </div>
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">Organization Profile</h1>
            <p className="text-slate-500 text-sm font-medium">
              Letterhead, GSTIN/bank details, and default terms shown on every printed Purchase Order.
            </p>
          </div>
        </div>
        <button type="submit" disabled={saving}
          className="flex items-center gap-2 px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold rounded-xl transition-all shadow-lg shadow-blue-600/20 disabled:opacity-50">
          {saving ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />} Save
        </button>
      </div>

      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 space-y-5">
        <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Company & Address</h2>
        <div>
          <label className={labelCls}>Company Name</label>
          <input className={inputCls} value={form.companyName || ''} onChange={set('companyName')} />
        </div>
        <div>
          <label className={labelCls}>Address Line</label>
          <input className={inputCls} value={form.addressLine || ''} onChange={set('addressLine')} />
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div>
            <label className={labelCls}>City</label>
            <input className={inputCls} value={form.city || ''} onChange={set('city')} />
          </div>
          <div>
            <label className={labelCls}>State</label>
            <input className={inputCls} value={form.state || ''} onChange={set('state')} />
          </div>
          <div>
            <label className={labelCls}>State Code</label>
            <input className={inputCls} value={form.stateCode || ''} onChange={set('stateCode')} placeholder="e.g. 22" />
          </div>
          <div>
            <label className={labelCls}>Pincode</label>
            <input className={inputCls} value={form.pincode || ''} onChange={set('pincode')} />
          </div>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div>
            <label className={labelCls}>Country</label>
            <input className={inputCls} value={form.country || ''} onChange={set('country')} />
          </div>
          <div>
            <label className={labelCls}>Phone</label>
            <input className={inputCls} value={form.phone || ''} onChange={set('phone')} />
          </div>
          <div>
            <label className={labelCls}>Telefax</label>
            <input className={inputCls} value={form.telefax || ''} onChange={set('telefax')} />
          </div>
          <div>
            <label className={labelCls}>Email</label>
            <input className={inputCls} value={form.email || ''} onChange={set('email')} />
          </div>
        </div>
      </div>

      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 space-y-5">
        <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Tax & Excise Registration</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div>
            <label className={labelCls}>GSTIN No.</label>
            <input className={inputCls} value={form.gstin || ''} onChange={set('gstin')} />
          </div>
          <div>
            <label className={labelCls}>PAN No.</label>
            <input className={inputCls} value={form.pan || ''} onChange={set('pan')} />
          </div>
          <div>
            <label className={labelCls}>Range</label>
            <input className={inputCls} value={form.range || ''} onChange={set('range')} />
          </div>
          <div>
            <label className={labelCls}>Division</label>
            <input className={inputCls} value={form.division || ''} onChange={set('division')} />
          </div>
        </div>
      </div>

      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 space-y-5">
        <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Bank Details</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div>
            <label className={labelCls}>Bank Name</label>
            <input className={inputCls} value={form.bankName || ''} onChange={set('bankName')} />
          </div>
          <div>
            <label className={labelCls}>Branch</label>
            <input className={inputCls} value={form.bankBranch || ''} onChange={set('bankBranch')} />
          </div>
          <div>
            <label className={labelCls}>Account Number</label>
            <input className={inputCls} value={form.bankAccountNumber || ''} onChange={set('bankAccountNumber')} />
          </div>
          <div>
            <label className={labelCls}>IFSC</label>
            <input className={inputCls} value={form.bankIfsc || ''} onChange={set('bankIfsc')} />
          </div>
        </div>
      </div>

      <div className="bg-white rounded-3xl p-6 shadow-sm border border-slate-100 space-y-5">
        <h2 className="font-black text-slate-800 uppercase tracking-wider text-sm">Default Document Terms</h2>
        <p className="text-xs text-slate-400 -mt-3">
          Shown on every new PO unless overridden per-order.
        </p>
        <div>
          <label className={labelCls}>Payment Terms</label>
          <textarea rows={2} className={inputCls} value={form.defaultPaymentTerms || ''} onChange={set('defaultPaymentTerms')} />
        </div>
        <div>
          <label className={labelCls}>Payment</label>
          <input className={inputCls} value={form.defaultPayment || ''} onChange={set('defaultPayment')}
            placeholder="e.g. 30% Advance alongwith order & balance against Proforma Invoice" />
        </div>
        <div>
          <label className={labelCls}>Delivery Terms</label>
          <textarea rows={2} className={inputCls} value={form.defaultDeliveryTerms || ''} onChange={set('defaultDeliveryTerms')} />
        </div>
        <div>
          <label className={labelCls}>Excise Duty</label>
          <input className={inputCls} value={form.defaultExciseDuty || ''} onChange={set('defaultExciseDuty')} />
        </div>
        <div>
          <label className={labelCls}>Special Terms and Conditions</label>
          <textarea rows={8} className={inputCls + ' font-mono text-xs'} value={form.termsAndConditions || ''} onChange={set('termsAndConditions')}
            placeholder="One clause per line — numbered automatically on the printed PO." />
        </div>
      </div>
    </form>
  );
};

export default OrgSettings;
