import React from 'react';
import { X, Printer, Download } from 'lucide-react';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  documentNumber: string;
  children: React.ReactNode;
  // When true, skip the generic "AEROSPACE ERP" branding header inside the print
  // root — used by documents (like the formal GST PO) that render their own full
  // letterhead from the Organization Profile instead.
  plain?: boolean;
}

// Generic "view/print/download" wrapper for any document (PO, Material Receipt,
// GRN). Printing/downloading both just call window.print() — the #receipt-print-root
// + .no-print rules in index.css isolate this modal's content on the printed page,
// and "Download" is the same dialog with "Save as PDF" picked as the destination
// (no PDF-generation library needed).
export const ReceiptModal: React.FC<Props> = ({ isOpen, onClose, title, documentNumber, children, plain }) => {
  if (!isOpen) return null;

  const handlePrint = () => window.print();

  return (
    <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-md flex items-center justify-center p-4 z-50 animate-in fade-in zoom-in-95 duration-200">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-3xl border border-slate-200 flex flex-col max-h-[90vh]">

        <div className="bg-slate-50 px-6 py-4 border-b flex justify-between items-center shrink-0 no-print">
          <div>
            <h2 className="text-lg font-black text-slate-900">{title}</h2>
            <p className="text-xs font-bold text-slate-400 font-mono">{documentNumber}</p>
          </div>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-slate-700 hover:bg-slate-100 rounded-xl transition-colors">
            <X size={20} />
          </button>
        </div>

        <div id="receipt-print-root" className="p-8 overflow-y-auto flex-1 bg-white">
          {!plain && (
            <div className="flex items-center justify-between border-b border-slate-200 pb-4 mb-6">
              <div>
                <h1 className="text-xl font-black text-slate-900 tracking-tight">AEROSPACE ERP</h1>
                <p className="text-xs text-slate-400 font-bold uppercase tracking-widest">{title}</p>
              </div>
              <div className="text-right">
                <p className="font-mono font-black text-slate-900">{documentNumber}</p>
                <p className="text-[10px] text-slate-400">Printed: {new Date().toLocaleString()}</p>
              </div>
            </div>
          )}
          {children}
        </div>

        <div className="flex gap-3 p-6 pt-4 border-t border-slate-100 shrink-0 no-print">
          <button onClick={onClose} className="flex-1 py-2.5 text-slate-500 font-bold text-sm hover:bg-slate-50 rounded-xl transition-all">
            Close
          </button>
          <button onClick={handlePrint}
            className="flex-1 flex items-center justify-center gap-2 py-2.5 bg-slate-700 hover:bg-slate-800 text-white font-bold text-sm rounded-xl transition-all">
            <Printer size={16} /> Print
          </button>
          <button onClick={handlePrint} title='In the print dialog, choose "Save as PDF" as the destination'
            className="flex-1 flex items-center justify-center gap-2 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold text-sm rounded-xl transition-all">
            <Download size={16} /> Download
          </button>
        </div>
      </div>
    </div>
  );
};
