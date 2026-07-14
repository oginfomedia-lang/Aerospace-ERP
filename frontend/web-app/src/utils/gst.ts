// Purchase Order tax computation — CGST+SGST if buyer and supplier are in the
// same state, IGST if they're in different states. Pure display computation:
// the amount is never persisted, just recomputed from each line's snapshotted
// taxRatePercent + a live state comparison whenever the receipt renders.

export interface GstLine {
  taxRatePercent?: number | null;
  quantity: number;
  unitPrice: number;
}

export interface GstResult {
  taxableTotal: number;
  cgst: number;
  sgst: number;
  igst: number;
  totalTax: number;
  sameState: boolean;
}

export function computeGst(
  buyerStateCode: string | null | undefined,
  supplierStateCode: string | null | undefined,
  lines: GstLine[]
): GstResult {
  const taxableTotal = lines.reduce((sum, l) => sum + l.quantity * l.unitPrice, 0);
  const totalTax = lines.reduce(
    (sum, l) => sum + l.quantity * l.unitPrice * ((l.taxRatePercent || 0) / 100),
    0
  );

  const sameState =
    !!buyerStateCode && !!supplierStateCode &&
    buyerStateCode.trim().toLowerCase() === supplierStateCode.trim().toLowerCase();

  if (sameState) {
    return { taxableTotal, cgst: totalTax / 2, sgst: totalTax / 2, igst: 0, totalTax, sameState };
  }
  return { taxableTotal, cgst: 0, sgst: 0, igst: totalTax, totalTax, sameState };
}
