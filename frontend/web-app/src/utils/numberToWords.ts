// Converts a currency amount into words using the Indian numbering system
// (Crore/Lakh/Thousand/Hundred), e.g. 11800 -> "Eleven Thousand Eight Hundred Only".

const ONES = [
  '', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine', 'Ten',
  'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen', 'Seventeen',
  'Eighteen', 'Nineteen',
];
const TENS = ['', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety'];

function threeDigitsToWords(n: number): string {
  let out = '';
  if (n >= 100) {
    out += `${ONES[Math.floor(n / 100)]} Hundred `;
    n %= 100;
  }
  if (n >= 20) {
    out += `${TENS[Math.floor(n / 10)]} `;
    n %= 10;
  }
  if (n > 0) {
    out += `${ONES[n]} `;
  }
  return out.trim();
}

function integerToWords(value: number): string {
  if (value === 0) return 'Zero';

  const crore = Math.floor(value / 10000000);
  value %= 10000000;
  const lakh = Math.floor(value / 100000);
  value %= 100000;
  const thousand = Math.floor(value / 1000);
  value %= 1000;
  const hundred = value;

  const parts: string[] = [];
  if (crore) parts.push(`${threeDigitsToWords(crore)} Crore`);
  if (lakh) parts.push(`${threeDigitsToWords(lakh)} Lakh`);
  if (thousand) parts.push(`${threeDigitsToWords(thousand)} Thousand`);
  if (hundred) parts.push(threeDigitsToWords(hundred));

  return parts.join(' ');
}

export function amountToWords(amount: number, currencyLabel = ''): string {
  const rounded = Math.round((amount + Number.EPSILON) * 100) / 100;
  const rupees = Math.floor(rounded);
  const paise = Math.round((rounded - rupees) * 100);

  let words = `${currencyLabel ? currencyLabel + ' ' : ''}${integerToWords(rupees)}`;
  if (paise > 0) {
    words += ` and ${integerToWords(paise)} Paise`;
  }
  return `${words} Only`;
}
