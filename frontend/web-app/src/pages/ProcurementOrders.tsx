import React, { useState, useEffect } from 'react';
import { ShoppingCart, Plus, Loader2, RefreshCw, Trash2 } from 'lucide-react';
import toast from 'react-hot-toast';
import procurementApi from '../api/procurementAxios';
import { Supplier, PurchaseOrderResponse, PurchaseOrderRequest, POLineItemDTO } from '../types/procurement';

const ProcurementOrders = () => {
  const [orders, setOrders] = useState<PurchaseOrderResponse[]>([]);
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);

  // Form State
  const [supplierId, setSupplierId] = useState<number | ''>('');
  const [deliveryDate, setDeliveryDate] = useState('');
  const [currency, setCurrency] = useState('USD');
  const [remarks, setRemarks] = useState('');
  const [items, setItems] = useState<POLineItemDTO[]>([
    { skuCode: '', quantity: 1, unitPrice: 0, uom: 'PCS' }
  ]);

  const fetchData = async () => {
    setLoading(true);
    try {
      // Assuming a GET /purchase-orders endpoint exists, otherwise we just handle creation for now
      // Since we didn't explicitly implement GET /purchase-orders in backend, we'll gracefully handle 404s
      const [suppRes, orderRes] = await Promise.allSettled([
        procurementApi.get('/suppliers'),
        procurementApi.get('/purchase-orders')
      ]);

      if (suppRes.status === 'fulfilled') setSuppliers(suppRes.value.data);
      if (orderRes.status === 'fulfilled') setOrders(orderRes.value.data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleAddItem = () => {
    setItems([...items, { skuCode: '', quantity: 1, unitPrice: 0, uom: 'PCS' }]);
  };

  const handleRemoveItem = (index: number) => {
    setItems(items.filter((_, i) => i !== index));
  };

  const handleItemChange = (index: number, field: keyof POLineItemDTO, value: string | number) => {
    const newItems = [...items];
    newItems[index] = { ...newItems[index], [field]: value };
    setItems(newItems);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!supplierId) {
      toast.error('Please select a supplier');
      return;
    }

    const payload: PurchaseOrderRequest = {
      supplierId: Number(supplierId),
      deliveryDate: deliveryDate ? new Date(deliveryDate).toISOString() : new Date().toISOString(),
      currency,
      remarks,
      items: items.map(item => ({
        ...item,
        quantity: Number(item.quantity),
        unitPrice: Number(item.unitPrice)
      }))
    };

    try {
      const res = await procurementApi.post('/purchase-orders', payload);
      toast.success('Purchase Order created successfully!');
      setOrders([res.data, ...orders]);
      setShowModal(false);
      // Reset form
      setSupplierId('');
      setRemarks('');
      setItems([{ skuCode: '', quantity: 1, unitPrice: 0, uom: 'PCS' }]);
    } catch (err) {
      console.error(err);
    }
  };

  const handleUpdateStatus = async (id: number, status: string) => {
    try {
      const res = await procurementApi.put(`/purchase-orders/${id}/status`, null, {
        params: { status }
      });
      toast.success(`PO status updated to ${status}!`);
      setOrders(orders.map(o => o.id === id ? res.data : o));
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-black text-slate-900 tracking-tight">Purchase Orders</h1>
          <p className="text-slate-500 text-sm font-medium mt-1">Draft and track supplier orders</p>
        </div>
        <div className="flex gap-3">
          <button
            onClick={fetchData}
            className="p-2 text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-xl transition-all"
          >
            <RefreshCw size={20} className={loading ? 'animate-spin' : ''} />
          </button>
          <button
            onClick={() => setShowModal(true)}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-bold rounded-xl transition-all shadow-lg shadow-blue-600/20"
          >
            <Plus size={18} />
            Draft PO
          </button>
        </div>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
        <table className="w-full text-left text-sm">
          <thead className="bg-slate-50 border-b border-slate-200 text-slate-500 font-semibold uppercase text-xs tracking-wider">
            <tr>
              <th className="px-6 py-4">PO Number</th>
              <th className="px-6 py-4">Supplier</th>
              <th className="px-6 py-4">Status</th>
              <th className="px-6 py-4">Delivery</th>
              <th className="px-6 py-4 text-right">Total Amount</th>
              <th className="px-6 py-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {orders.map(order => (
              <tr key={order.id} className="hover:bg-slate-50/50 transition-colors">
                <td className="px-6 py-4">
                  <div className="font-bold text-slate-900 font-mono">{order.poNumber}</div>
                </td>
                <td className="px-6 py-4 font-medium text-slate-700">
                  {order.supplierName}
                </td>
                <td className="px-6 py-4">
                  <span className={`px-2.5 py-1 text-[11px] font-black tracking-wider uppercase rounded-full ${
                    order.status === 'SENT_TO_VENDOR' ? 'bg-indigo-100 text-indigo-700' :
                    order.status === 'RECEIVED' ? 'bg-emerald-100 text-emerald-700' :
                    order.status === 'CANCELLED' ? 'bg-red-100 text-red-700' :
                    'bg-amber-100 text-amber-700'
                  }`}>
                    {order.status || 'DRAFT'}
                  </span>
                </td>
                <td className="px-6 py-4 text-slate-600">
                  {new Date(order.deliveryDate).toLocaleDateString()}
                </td>
                <td className="px-6 py-4 text-right font-bold text-slate-900">
                  {order.currency} {order.totalAmount?.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </td>
                <td className="px-6 py-4 text-right">
                  {order.status === 'DRAFT' && (
                    <button
                      onClick={() => handleUpdateStatus(order.id, 'SENT_TO_VENDOR')}
                      className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold rounded-xl transition-all shadow-sm"
                    >
                      Send to Vendor
                    </button>
                  )}
                  {order.status === 'SENT_TO_VENDOR' && (
                    <div className="flex gap-2 justify-end">
                      <button
                        onClick={() => handleUpdateStatus(order.id, 'RECEIVED')}
                        className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold rounded-xl transition-all shadow-sm"
                      >
                        Receive
                      </button>
                      <button
                        onClick={() => handleUpdateStatus(order.id, 'CANCELLED')}
                        className="px-3 py-1.5 bg-red-50 hover:bg-red-100 text-red-600 text-xs font-bold rounded-xl transition-all"
                      >
                        Cancel
                      </button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
            {orders.length === 0 && !loading && (
              <tr>
                <td colSpan={6} className="px-6 py-12 text-center text-slate-500">
                  <ShoppingCart className="w-12 h-12 mx-auto text-slate-300 mb-3" />
                  <p className="font-medium">No purchase orders found</p>
                  <p className="text-sm mt-1">Draft a new PO to get started.</p>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl w-full max-w-2xl overflow-hidden shadow-2xl animate-in fade-in zoom-in-95 duration-200 flex flex-col max-h-[90vh]">
            <div className="p-6 border-b border-slate-100 shrink-0">
              <h2 className="text-xl font-bold text-slate-900">Draft Purchase Order</h2>
            </div>
            
            <div className="p-6 overflow-y-auto flex-1">
              <form id="po-form" onSubmit={handleSubmit} className="space-y-6">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-1.5">Supplier</label>
                    <select required value={supplierId} onChange={e => setSupplierId(Number(e.target.value))}
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all">
                      <option value="" disabled>Select a supplier...</option>
                      {suppliers.map(s => (
                        <option key={s.id} value={s.id}>{s.name} ({s.vendorCode})</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-1.5">Expected Delivery</label>
                    <input required type="datetime-local" value={deliveryDate} onChange={e => setDeliveryDate(e.target.value)} 
                      className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all" />
                  </div>
                </div>

                <div>
                  <label className="block text-sm font-semibold text-slate-700 mb-1.5">Remarks / Notes</label>
                  <input type="text" value={remarks} onChange={e => setRemarks(e.target.value)} 
                    className="w-full px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 transition-all"
                    placeholder="Urgent delivery instructions..." />
                </div>

                {/* Line Items */}
                <div className="border-t border-slate-100 pt-6">
                  <div className="flex justify-between items-center mb-4">
                    <h3 className="text-sm font-bold text-slate-900">Line Items</h3>
                    <button type="button" onClick={handleAddItem}
                      className="text-sm font-bold text-blue-600 hover:text-blue-700 flex items-center gap-1">
                      <Plus size={16} /> Add Item
                    </button>
                  </div>

                  <div className="space-y-3">
                    {items.map((item, idx) => (
                      <div key={idx} className="flex gap-3 items-start p-3 bg-slate-50 rounded-xl border border-slate-200">
                        <div className="flex-1">
                          <input required type="text" placeholder="SKU Code (e.g. TB-9912)" value={item.skuCode}
                            onChange={e => handleItemChange(idx, 'skuCode', e.target.value)}
                            className="w-full px-3 py-2 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:border-blue-500 font-mono" />
                        </div>
                        <div className="w-24">
                          <input required type="number" min="1" placeholder="Qty" value={item.quantity}
                            onChange={e => handleItemChange(idx, 'quantity', e.target.value)}
                            className="w-full px-3 py-2 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:border-blue-500" />
                        </div>
                        <div className="w-32">
                          <input required type="number" min="0" step="0.01" placeholder="Price" value={item.unitPrice}
                            onChange={e => handleItemChange(idx, 'unitPrice', e.target.value)}
                            className="w-full px-3 py-2 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:border-blue-500" />
                        </div>
                        <div className="w-24">
                          <select value={item.uom} onChange={e => handleItemChange(idx, 'uom', e.target.value)}
                            className="w-full px-3 py-2 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:border-blue-500">
                            <option value="PCS">PCS</option>
                            <option value="KG">KG</option>
                            <option value="L">L</option>
                          </select>
                        </div>
                        <button type="button" onClick={() => handleRemoveItem(idx)} disabled={items.length === 1}
                          className="p-2 text-slate-400 hover:text-red-600 disabled:opacity-50">
                          <Trash2 size={18} />
                        </button>
                      </div>
                    ))}
                  </div>
                </div>
              </form>
            </div>

            <div className="p-6 border-t border-slate-100 shrink-0 bg-slate-50 flex justify-between items-center">
              <div className="text-sm text-slate-500 font-medium">
                Total: <span className="text-slate-900 font-bold text-lg">${items.reduce((acc, item) => acc + (item.quantity * item.unitPrice), 0).toLocaleString(undefined, { minimumFractionDigits: 2 })}</span>
              </div>
              <div className="flex gap-3">
                <button type="button" onClick={() => setShowModal(false)}
                  className="px-4 py-2 bg-white border border-slate-200 hover:bg-slate-100 text-slate-700 font-bold rounded-xl transition-all">
                  Cancel
                </button>
                <button type="submit" form="po-form"
                  className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl transition-all shadow-lg shadow-blue-600/20">
                  Submit PO
                </button>
              </div>
            </div>

          </div>
        </div>
      )}
    </div>
  );
};

export default ProcurementOrders;
