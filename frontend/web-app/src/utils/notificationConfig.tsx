// utils/notificationConfig.tsx
//
// Single source of truth for "what icon/color/label does each notification type
// get". Both the Notifications page (the list) and the global popup import from
// here — so the next time a new notification type is added on the backend, it
// only needs to be added in ONE place on the frontend, not two.
import React from 'react';
import {
  AlertTriangle, ShieldCheck, ShieldX, Clock, CalendarX2, ArrowDownToLine,
  AlertCircle, Package, PlusSquare, Wrench, PlayCircle, ClipboardCheck, Gauge,
  Undo2, FileCheck2, Activity, AlarmClock, ArrowUpCircle, Truck, Send,
  Hourglass, UserCheck, UserX, Cog, KeyRound, PackageCheck,
} from 'lucide-react';
import { NotificationType } from '../types/notification';

export const typeConfig: Record<NotificationType, { icon: React.ReactNode; iconBg: string; label: string }> = {
  // Inventory
  LOW_STOCK:      { icon: <AlertTriangle   size={16} />, iconBg: 'bg-amber-100  text-amber-600',   label: 'Low Stock'       },
  QA_APPROVED:    { icon: <ShieldCheck     size={16} />, iconBg: 'bg-emerald-100 text-emerald-600', label: 'QA Approved'     },
  QA_REJECTED:    { icon: <ShieldX         size={16} />, iconBg: 'bg-red-100    text-red-600',      label: 'QA Rejected'     },
  QA_PENDING:     { icon: <Clock           size={16} />, iconBg: 'bg-blue-100   text-blue-600',     label: 'QA Pending'      },
  BATCH_EXPIRED:  { icon: <CalendarX2      size={16} />, iconBg: 'bg-red-100    text-red-600',      label: 'Batch Expired'   },
  STOCK_RECEIVED: { icon: <ArrowDownToLine size={16} />, iconBg: 'bg-indigo-100 text-indigo-600',   label: 'Stock Received'  },
  EXPIRY_WARNING: { icon: <AlertCircle     size={16} />, iconBg: 'bg-orange-100 text-orange-600',   label: 'Expiry Warning'  },
  MATERIAL_ISSUED:{ icon: <Package         size={16} />, iconBg: 'bg-violet-100 text-violet-600',   label: 'Material Issued' },
  PART_CREATED:   { icon: <PlusSquare      size={16} />, iconBg: 'bg-teal-100   text-teal-600',     label: 'Part Created'    },
  // Work Orders
  WORK_ORDER_CREATED:   { icon: <Wrench          size={16} />, iconBg: 'bg-blue-100    text-blue-600',    label: 'WO Created'   },
  WORK_ORDER_UPDATED:   { icon: <PlayCircle      size={16} />, iconBg: 'bg-amber-100   text-amber-600',   label: 'WO Updated'   },
  WORK_ORDER_COMPLETED: { icon: <ClipboardCheck  size={16} />, iconBg: 'bg-emerald-100 text-emerald-600', label: 'WO Completed' },
  VENDOR_RETURN:         { icon: <Undo2          size={16} />, iconBg: 'bg-orange-100  text-orange-600',  label: 'Vendor Return'},
  // QA / Compliance
  CALIBRATION_DUE:     { icon: <Gauge      size={16} />, iconBg: 'bg-orange-100 text-orange-600',  label: 'Calibration Due' },
  FAI_COMPLETED:       { icon: <FileCheck2 size={16} />, iconBg: 'bg-emerald-100 text-emerald-600', label: 'FAI Completed'  },
  CAPA_STATUS_CHANGED: { icon: <Activity   size={16} />, iconBg: 'bg-blue-100   text-blue-600',     label: 'CAPA Updated'   },
  CAPA_OVERDUE:        { icon: <AlarmClock size={16} />, iconBg: 'bg-red-100    text-red-600',      label: 'CAPA Overdue'   },
  // Approvals & Procurement
  APPROVAL_ESCALATED: { icon: <ArrowUpCircle size={16} />, iconBg: 'bg-red-100    text-red-600',    label: 'Approval Escalated' },
  PO_DELIVERY_OVERDUE:{ icon: <Truck         size={16} />, iconBg: 'bg-red-100    text-red-600',    label: 'PO Overdue'         },
  PO_SENT_TO_VENDOR:  { icon: <Send          size={16} />, iconBg: 'bg-blue-100   text-blue-600',   label: 'PO Sent'            },
  // Suppliers
  SUPPLIER_PENDING_CONFIRMATION: { icon: <Hourglass size={16} />, iconBg: 'bg-amber-100  text-amber-600',   label: 'Supplier Pending'   },
  SUPPLIER_CONFIRMED:            { icon: <UserCheck size={16} />, iconBg: 'bg-emerald-100 text-emerald-600', label: 'Supplier Confirmed' },
  SUPPLIER_REJECTED:             { icon: <UserX     size={16} />, iconBg: 'bg-red-100    text-red-600',     label: 'Supplier Rejected'  },
  // Maintenance & Account
  MAINTENANCE_DUE:          { icon: <Cog      size={16} />, iconBg: 'bg-orange-100 text-orange-600', label: 'Maintenance Due' },
  PASSWORD_RESET_REQUESTED: { icon: <KeyRound size={16} />, iconBg: 'bg-slate-100  text-slate-600',  label: 'Password Reset'  },
  // Sales
  SALES_ORDER_DISPATCHED: { icon: <PackageCheck size={16} />, iconBg: 'bg-indigo-100 text-indigo-600', label: 'Order Dispatched' },
};

export const TYPE_GROUPS: { label: string; types: NotificationType[] }[] = [
  {
    label: 'Inventory',
    types: ['LOW_STOCK', 'QA_APPROVED', 'QA_REJECTED', 'QA_PENDING',
            'BATCH_EXPIRED', 'STOCK_RECEIVED', 'EXPIRY_WARNING',
            'MATERIAL_ISSUED', 'PART_CREATED'],
  },
  {
    label: 'Work Orders',
    types: ['WORK_ORDER_CREATED', 'WORK_ORDER_UPDATED', 'WORK_ORDER_COMPLETED', 'VENDOR_RETURN'],
  },
  {
    label: 'QA & Compliance',
    types: ['CALIBRATION_DUE', 'FAI_COMPLETED', 'CAPA_STATUS_CHANGED', 'CAPA_OVERDUE'],
  },
  {
    label: 'Approvals & Procurement',
    types: ['APPROVAL_ESCALATED', 'PO_DELIVERY_OVERDUE', 'PO_SENT_TO_VENDOR'],
  },
  {
    label: 'Suppliers',
    types: ['SUPPLIER_PENDING_CONFIRMATION', 'SUPPLIER_CONFIRMED', 'SUPPLIER_REJECTED'],
  },
  {
    label: 'Sales',
    types: ['SALES_ORDER_DISPATCHED'],
  },
  {
    label: 'Maintenance & Account',
    types: ['MAINTENANCE_DUE', 'PASSWORD_RESET_REQUESTED'],
  },
];
