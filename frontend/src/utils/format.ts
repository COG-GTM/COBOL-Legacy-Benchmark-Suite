export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

export function formatShares(value: number): string {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 3,
    maximumFractionDigits: 3,
  }).format(value);
}

export function formatPrice(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 4,
    maximumFractionDigits: 4,
  }).format(value);
}

export function formatDate(dateStr: string): string {
  if (!dateStr) return '';
  if (/^\d{8}$/.test(dateStr)) {
    const y = dateStr.substring(0, 4);
    const m = dateStr.substring(4, 6);
    const d = dateStr.substring(6, 8);
    return `${y}-${m}-${d}`;
  }
  return dateStr;
}

export function formatTime(timeStr: string): string {
  if (!timeStr || timeStr.length < 6) return timeStr;
  const padded = timeStr.padEnd(8, '0');
  return `${padded.substring(0, 2)}:${padded.substring(2, 4)}:${padded.substring(4, 6)}`;
}
