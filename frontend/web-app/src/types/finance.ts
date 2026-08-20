// types/finance.ts
// Mirrors finance-service's DTOs field-for-field — same convention as types/sales.ts.

export type JournalStatus = 'DRAFT' | 'PENDING_APPROVAL' | 'REJECTED' | 'POSTED';
export type AccountType = 'ASSET' | 'LIABILITY' | 'EQUITY' | 'REVENUE' | 'EXPENSE';
export type PostingPermission = 'POSTABLE' | 'HEADER_ONLY';
export type PeriodStatus = 'OPEN' | 'CLOSED' | 'LOCKED';
export type TaxDirection = 'INPUT' | 'OUTPUT';
export type PostingTriggerType =
  | 'PURCHASE_RECEIPT' | 'SUPPLIER_INVOICE' | 'VENDOR_INVOICE_APPROVED'
  | 'INVENTORY_ADJUSTMENT' | 'MATERIAL_CONSUMPTION' | 'FINISHED_GOODS_RECEIPT'
  | 'SALES_DISPATCH_CONFIRMED' | 'CUSTOMER_INVOICE' | 'MAINTENANCE_COST' | 'QUALITY_COST';

// ── Master data ────────────────────────────────────────────────────────────

export interface Organization {
  id: number;
  orgCode: string;
  orgName: string;
  baseCurrency?: string | null;
  activeStatus: boolean;
}

export interface Plant {
  id: number;
  plantCode: string;
  plantName: string;
  organizationId?: number | null;
  activeStatus: boolean;
}

export interface CostCenter {
  id: number;
  costCenterCode: string;
  costCenterName: string;
  description?: string | null;
  plantId?: number | null;
  activeStatus: boolean;
}

export interface ProfitCenter {
  id: number;
  profitCenterCode: string;
  profitCenterName: string;
  plantId?: number | null;
  activeStatus: boolean;
}

// ── Chart of Accounts ─────────────────────────────────────────────────────

export interface Account {
  id: number;
  accountCode: string;
  accountName: string;
  accountType: AccountType;
  parentAccountId?: number | null;
  parentAccountCode?: string | null;
  financialCategory?: string | null;
  activeStatus: boolean;
  currencyRestrictions?: string | null;
  postingPermission: PostingPermission;
  children?: Account[];
}

// ── Financial periods ─────────────────────────────────────────────────────

export interface FiscalYear {
  id: number;
  fiscalYearCode: string;
  startDate: string;
  endDate: string;
  status: PeriodStatus;
}

export interface AccountingPeriod {
  id: number;
  fiscalYearCode: string;
  periodNumber: number;
  periodLabel: string;
  startDate: string;
  endDate: string;
  status: PeriodStatus;
  closedAt?: string | null;
  closedBy?: string | null;
}

// ── Journal entries ────────────────────────────────────────────────────────

export interface JournalLine {
  id?: number;
  accountCode: string;
  debitAmount: number;
  creditAmount: number;
  currency: string;
  counterpartyRef?: string;
  supplierId?: number;
  costCenterId?: number;
  profitCenterId?: number;
  description?: string;
}

export interface JournalEntry {
  id: number;
  journalNumber: string;
  postingDate: string;
  narration?: string | null;
  sourceDocType?: string | null;
  sourceDocRef?: string | null;
  status: JournalStatus;
  createdBy: string;
  approvedBy?: string | null;
  approvedAt?: string | null;
  postedAt?: string | null;
  reversalOfJournalId?: number | null;
  reversedByJournalId?: number | null;
  lines: JournalLine[];
}

export interface JournalEntryCreatePayload {
  postingDate: string;
  narration?: string;
  sourceDocType?: string;
  sourceDocRef?: string;
  costCenterId?: number;
  profitCenterId?: number;
  organizationId?: number;
  plantId?: number;
  lines: JournalLine[];
}

// ── Multi-currency ─────────────────────────────────────────────────────────

export interface Currency {
  currencyCode: string;
  currencyName: string;
  isBaseCurrency?: boolean;
  activeStatus?: boolean;
}

export interface ExchangeRate {
  id: number;
  fromCurrency: string;
  toCurrency: string;
  rate: number;
  effectiveDate: string;
}

// ── Tax ──────────────────────────────────────────────────────────────────

export interface TaxCategory {
  id: number;
  categoryCode: string;
  categoryName: string;
  activeStatus: boolean;
}

export interface TaxJurisdiction {
  id: number;
  jurisdictionCode: string;
  jurisdictionName: string;
  activeStatus: boolean;
}

export interface TaxRate {
  id: number;
  taxRuleCode: string;
  taxCategoryId?: number | null;
  taxJurisdictionId?: number | null;
  ratePercent: number;
  direction: TaxDirection;
  reverseCharge: boolean;
  taxAccountCode: string;
  effectiveFrom?: string | null;
  effectiveTo?: string | null;
  activeStatus: boolean;
}

// ── Posting rules ───────────────────────────────────────────────────────────

export interface PostingRule {
  id: number;
  triggerEvent: PostingTriggerType;
  debitAccountCode: string;
  creditAccountCode: string;
  taxRuleCode?: string | null;
  costCenterId?: number | null;
  profitCenterId?: number | null;
  isActive: boolean;
}

// ── AP/AR ────────────────────────────────────────────────────────────────

export interface LedgerView {
  counterpartyRef: string;
  openBalance: number;
  transactionCount: number;
}

export interface AgingBucket {
  bucketLabel: string;
  totalAmount: number;
  byCounterparty: Record<string, number>;
}

// ── Suppliers (local read-only mirror, synced from procurement-service via Kafka) ─

export interface Supplier {
  id: number;
  supplierName: string;
  vendorCode: string;
}

// ── Supplier payments ──────────────────────────────────────────────────────

export type PaymentMethod = 'BANK_TRANSFER' | 'CHEQUE' | 'CASH' | 'ONLINE';
export type PaymentStatus = 'DRAFT' | 'POSTED' | 'CANCELLED';

export interface SupplierPayment {
  id: number;
  paymentNumber: string;
  invoiceNumber: string;
  supplierId: number;
  supplierName?: string;
  /** @deprecated auto-derived server-side, kept for backward-compat display only */
  supplierRef?: string;
  paymentDate: string;
  amount: number;
  currency: string;
  paymentMethod: PaymentMethod;
  status: PaymentStatus;
  journalEntryRef?: string;
  remarks?: string;
  createdBy: string;
  createdAt: string;
}

export interface SupplierPaymentCreatePayload {
  invoiceNumber: string;
  supplierId: number;
  paymentDate: string;
  amount: number;
  currency: string;
  paymentMethod: PaymentMethod;
  remarks?: string;
  costCenterId?: number;
}

// ── Reports ─────────────────────────────────────────────────────────────

export interface FinanceReportSummary {
  totalJournalsSubmitted: number;
  journalsPosted: number;
  journalsRejected: number;
  journalPostingSuccessRatePercent: number;
  openPeriodCount: number;
  closedPeriodCount: number;
  lockedPeriodCount: number;
  outstandingAP: number;
  outstandingAR: number;
  totalRevenuePosted: number;
  totalExpensePosted: number;
  grossMarginApprox: number;
}
