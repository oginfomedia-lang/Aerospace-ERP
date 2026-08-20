// Thin fetch utility for the local customer reference mirror — mirrors
// financeSuppliers.ts exactly, kept separate from <Dropdown> so it stays
// data-source-agnostic.
import financeApi from './financeAxios';
import { ApiResponse } from '../types/inventory';
import { Customer } from '../types/finance';

export async function fetchCustomers(): Promise<Customer[]> {
  const res = await financeApi.get<ApiResponse<Customer[]>>('/finance/customers');
  return res.data.data;
}
