// Thin fetch utility for the local supplier reference mirror — kept separate from
// <Dropdown> so the component itself stays data-source-agnostic (reusable later for
// accounts, cost centers, etc. without touching this file).
import financeApi from './financeAxios';
import { ApiResponse } from '../types/inventory';
import { Supplier } from '../types/finance';

export async function fetchSuppliers(): Promise<Supplier[]> {
  const res = await financeApi.get<ApiResponse<Supplier[]>>('/finance/suppliers');
  return res.data.data;
}
