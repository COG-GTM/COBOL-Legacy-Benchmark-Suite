import { portfolios as basePortfolios, positions } from '@/data/mockData';
import type { Position } from '@/data/types';

/** Mirrors PORT-RECORD in src/copybook/common/PORTFLIO.cpy */
export interface PortfolioMaster extends Record<string, unknown> {
  id: string;
  accountNo: string;
  clientName: string;
  clientType: 'I' | 'C' | 'T';
  createDate: string;
  lastMaint: string;
  status: 'A' | 'C' | 'S';
  totalValue: number;
  cashBalance: number;
}

export interface Holding extends Record<string, unknown> {
  fundId: string;
  cusip: string;
  quantity: number;
  avgCost: number;
  costBasis: number;
  marketValue: number;
  gainLoss: number;
  status: Position['status'];
}

export const CLIENT_TYPE_LABELS: Record<PortfolioMaster['clientType'], string> = {
  I: 'Individual',
  C: 'Corporate',
  T: 'Trust',
};

export const STATUS_LABELS: Record<PortfolioMaster['status'], string> = {
  A: 'Active',
  C: 'Closed',
  S: 'Suspended',
};

export function getStatusVariant(status: string): 'success' | 'error' | 'warning' | 'neutral' {
  switch (status) {
    case 'A': return 'success';
    case 'C': return 'error';
    case 'S': return 'warning';
    default: return 'neutral';
  }
}

const currencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
});

export function formatCurrency(value: number): string {
  return currencyFormatter.format(value);
}

const quantityFormatter = new Intl.NumberFormat('en-US', {
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});

export function formatQuantity(value: number): string {
  return quantityFormatter.format(value);
}

/** Current market prices per fund used to derive holding market values */
const fundPrices: Record<string, number> = {
  GRWEQF: 47.10,
  BLUCDP: 122.40,
  FIXINC: 99.35,
  EMERGE: 34.15,
  TECHSF: 218.75,
  HLTHIF: 76.90,
  REITPF: 57.20,
  BALGIF: 89.10,
  SMCAPV: 30.05,
  MUNBPF: 42.00,
  ESGSUS: 64.30,
  RETINC: 106.20,
};

interface ClientInfo {
  accountNo: string;
  clientName: string;
  clientType: PortfolioMaster['clientType'];
  cashBalance: number;
}

const clientInfo: Record<string, ClientInfo> = {
  PORT0001: { accountNo: '0100000001', clientName: 'Harrison Whitfield', clientType: 'I', cashBalance: 125000.0 },
  PORT0002: { accountNo: '0100000002', clientName: 'Meridian Capital LLC', clientType: 'C', cashBalance: 340000.0 },
  PORT0003: { accountNo: '0100000003', clientName: 'Eleanor Vance Trust', clientType: 'T', cashBalance: 89500.0 },
  PORT0004: { accountNo: '0100000004', clientName: 'Global Ventures Inc', clientType: 'C', cashBalance: 215000.0 },
  PORT0005: { accountNo: '0100000005', clientName: 'Samuel Okafor', clientType: 'I', cashBalance: 56200.0 },
  PORT0006: { accountNo: '0100000006', clientName: 'Caldwell Family Trust', clientType: 'T', cashBalance: 178000.0 },
  PORT0007: { accountNo: '0100000007', clientName: 'Priya Raghavan', clientType: 'I', cashBalance: 42300.0 },
  PORT0008: { accountNo: '0100000008', clientName: 'Sterling Industries Corp', clientType: 'C', cashBalance: 510000.0 },
  PORT0009: { accountNo: '0100000009', clientName: 'Marcus Delgado', clientType: 'I', cashBalance: 31800.0 },
  PORT0010: { accountNo: '0100000010', clientName: 'Ashworth Estate Trust', clientType: 'T', cashBalance: 0.0 },
  PORT0011: { accountNo: '0100000011', clientName: 'Helena Brandt', clientType: 'I', cashBalance: 97600.0 },
  PORT0012: { accountNo: '0100000012', clientName: 'Pinnacle Retirement Group', clientType: 'C', cashBalance: 425000.0 },
};

export function seedPortfolios(): PortfolioMaster[] {
  return basePortfolios.map((p) => {
    const info = clientInfo[p.id];
    return {
      id: p.id,
      accountNo: info?.accountNo ?? '0000000000',
      clientName: info?.clientName ?? p.name,
      clientType: info?.clientType ?? 'I',
      createDate: p.createDate,
      lastMaint: p.createDate,
      status: p.status === 'I' ? 'S' : p.status,
      totalValue: p.totalValue,
      cashBalance: info?.cashBalance ?? 0,
    };
  });
}

export function getHoldings(accountNo: string): Holding[] {
  return positions
    .filter((pos) => pos.accountNo.padStart(10, '0') === accountNo)
    .map((pos) => {
      const price = fundPrices[pos.fundId] ?? pos.avgCost;
      const marketValue = pos.shareBalance * price;
      return {
        fundId: pos.fundId,
        cusip: pos.cusip,
        quantity: pos.shareBalance,
        avgCost: pos.avgCost,
        costBasis: pos.costBasis,
        marketValue,
        gainLoss: marketValue - pos.costBasis,
        status: pos.status,
      };
    });
}

export interface PortfolioFormValues {
  id: string;
  accountNo: string;
  clientName: string;
  clientType: string;
  status: string;
  totalValue: string;
  cashBalance: string;
}

export type PortfolioFormErrors = Partial<Record<keyof PortfolioFormValues, string>>;

const VAL_MIN_AMOUNT = 0;
const VAL_MAX_AMOUNT = 9999999999999.99;

/** Mirrors PORTVALD 1000-VALIDATE-ID: 'PORT' prefix + 4 numeric digits */
export function validatePortfolioId(value: string): string | null {
  if (!value.trim()) return 'Portfolio ID is required';
  if (value.length !== 8 || !value.startsWith('PORT')) {
    return 'Portfolio ID must be PORT followed by 4 digits';
  }
  if (!/^\d+$/.test(value.slice(4))) {
    return 'Portfolio ID must be PORT followed by 4 digits';
  }
  return null;
}

/** Mirrors PORTVALD 2000-VALIDATE-ACCOUNT: 10 numeric digits, not all zeros */
export function validateAccountNo(value: string): string | null {
  if (!value.trim()) return 'Account number is required';
  if (value.length !== 10 || !/^\d+$/.test(value)) {
    return 'Account number must be exactly 10 digits';
  }
  if (/^0+$/.test(value)) return 'Account number cannot be all zeros';
  return null;
}

/** Mirrors PORTADD 2100-VALIDATE-AND-ADD: client name cannot be spaces */
export function validateClientName(value: string): string | null {
  if (!value.trim()) return 'Client name is required';
  if (value.length > 30) return 'Client name cannot exceed 30 characters';
  return null;
}

export function validateClientType(value: string): string | null {
  if (!value) return 'Client type is required';
  if (!['I', 'C', 'T'].includes(value)) return 'Client type must be Individual, Corporate, or Trust';
  return null;
}

export function validateStatus(value: string): string | null {
  if (!value) return 'Status is required';
  if (!['A', 'C', 'S'].includes(value)) return 'Status must be Active, Closed, or Suspended';
  return null;
}

/** Mirrors PORTVALD 4000-VALIDATE-AMOUNT: numeric within valid range */
export function validateAmount(value: string, label: string): string | null {
  if (!value.trim()) return `${label} is required`;
  if (!/^\d+(\.\d{1,2})?$/.test(value)) {
    return `${label} must be a positive number with up to 2 decimal places`;
  }
  const num = Number(value);
  if (num < VAL_MIN_AMOUNT || num > VAL_MAX_AMOUNT) {
    return `${label} must be between 0 and 9,999,999,999,999.99`;
  }
  return null;
}

export function validatePortfolioForm(
  values: PortfolioFormValues,
  options: { isNew: boolean; existingIds?: string[] }
): PortfolioFormErrors {
  const errors: PortfolioFormErrors = {};
  const idError = validatePortfolioId(values.id);
  if (idError) errors.id = idError;
  else if (options.isNew && options.existingIds?.includes(values.id)) {
    errors.id = 'Portfolio ID already exists';
  }
  const acctError = validateAccountNo(values.accountNo);
  if (acctError) errors.accountNo = acctError;
  const nameError = validateClientName(values.clientName);
  if (nameError) errors.clientName = nameError;
  const typeError = validateClientType(values.clientType);
  if (typeError) errors.clientType = typeError;
  const statusError = validateStatus(values.status);
  if (statusError) errors.status = statusError;
  const totalError = validateAmount(values.totalValue, 'Total value');
  if (totalError) errors.totalValue = totalError;
  const cashError = validateAmount(values.cashBalance, 'Cash balance');
  if (cashError) errors.cashBalance = cashError;
  return errors;
}

/** PORTDEL only processes portfolios already marked Closed or Suspended */
export function isDeletable(portfolio: PortfolioMaster): boolean {
  return portfolio.status !== 'A';
}
