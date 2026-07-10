import React from 'react';
import { AlertTriangle, X, ShoppingCart } from 'lucide-react';
import { Part } from '../types/inventory';
import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';

interface Props {
  parts: Part[];
}

// A part is "low stock" when its approved quantity is below or equal to its reorder level
// Both values come from PartResponseDTO — quantity (live from StockBalance) and reorderLevel (from Part master)

export const LowStockAlert = ({ parts }: Props) => {
  const [dismissed, setDismissed] = useState(false);
  const navigate = useNavigate();

  // Suggested reorder qty: cover the current shortfall plus one more reorder cycle's worth
  const suggestedQty = (p: Part) => Math.max((p.reorderLevel ?? 0) - (p.quantity ?? 0), 1) * 2;

  const handleReorder = (p: Part, e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    navigate('/purchase-orders', {
      state: {
        prefillItems: [{ skuCode: p.serialNumber, quantity: suggestedQty(p) }],
      },
    });
  };

  const lowStockParts = parts.filter(p =>
    p.reorderLevel !== null &&
    p.quantity !== null &&
    p.quantity <= p.reorderLevel &&
    p.status === 'APPROVED' // only alert on approved stock — pending/rejected are separate concerns
  );

  if (lowStockParts.length === 0 || dismissed) return null;

  return (
    <div className="bg-amber-50 border border-amber-200 rounded-2xl p-5 animate-in slide-in-from-top-2 duration-500">
      <div className="flex items-start justify-between gap-4">
        <div className="flex items-start gap-3">
          <div className="p-2 bg-amber-100 rounded-xl mt-0.5">
            <AlertTriangle size={18} className="text-amber-600" />
          </div>
          <div>
            <p className="text-sm font-black text-amber-800 uppercase tracking-wide">
              {lowStockParts.length} Material{lowStockParts.length > 1 ? 's' : ''} Below Reorder Level
            </p>
            <p className="text-xs text-amber-600 font-medium mt-0.5">
              Raise procurement requests to avoid production stoppage.
            </p>
            <div className="flex flex-wrap gap-2 mt-3">
              {lowStockParts.map(p => (
                <div
                  key={p.id}
                  className="flex items-center gap-2 bg-white border border-amber-200 pl-3 pr-1.5 py-1.5 rounded-xl group"
                >
                  <Link to={`/inventory/${p.id}/transactions`} className="flex items-center gap-2 hover:opacity-70 transition-opacity">
                    <span className="text-[10px] font-black text-slate-600 uppercase tracking-wider">{p.name}</span>
                    <span className="text-[10px] font-mono text-red-500 font-black">
                      {p.quantity} / {p.reorderLevel}
                    </span>
                  </Link>
                  <button
                    onClick={(e) => handleReorder(p, e)}
                    title={`Raise a PO for ${suggestedQty(p)} units`}
                    className="flex items-center gap-1 bg-amber-500 hover:bg-amber-600 text-white text-[10px] font-black uppercase px-2 py-1 rounded-lg transition-colors"
                  >
                    <ShoppingCart size={10} /> Reorder
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
        <button
          onClick={() => setDismissed(true)}
          className="text-amber-400 hover:text-amber-700 transition-colors mt-0.5 shrink-0"
          title="Dismiss"
        >
          <X size={16} />
        </button>
      </div>
    </div>
  );
};
