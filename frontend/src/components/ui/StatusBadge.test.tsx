import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import {
  StatusBadge,
  getPortfolioStatusVariant,
  getPortfolioStatusLabel,
  getTransactionStatusVariant,
  getTransactionStatusLabel,
  getBatchStatusVariant,
  getBatchStatusLabel,
  getAuditStatusVariant,
  getSeverityVariant,
  getPositionStatusVariant,
  getPositionStatusLabel,
  getTransTypeLabel,
} from './StatusBadge';

describe('StatusBadge', () => {
  it('renders the label text', () => {
    render(<StatusBadge label="Active" />);
    expect(screen.getByText('Active')).toBeInTheDocument();
  });

  it('applies default neutral variant', () => {
    const { container } = render(<StatusBadge label="X" />);
    expect(container.firstChild).toHaveClass('bg-slate-50');
  });

  it('applies success variant styles', () => {
    const { container } = render(<StatusBadge label="OK" variant="success" />);
    expect(container.firstChild).toHaveClass('bg-emerald-50');
  });

  it('applies error variant styles', () => {
    const { container } = render(<StatusBadge label="Err" variant="error" />);
    expect(container.firstChild).toHaveClass('bg-red-50');
  });
});

describe('getPortfolioStatusVariant', () => {
  it('returns success for A', () => expect(getPortfolioStatusVariant('A')).toBe('success'));
  it('returns warning for I', () => expect(getPortfolioStatusVariant('I')).toBe('warning'));
  it('returns error for C', () => expect(getPortfolioStatusVariant('C')).toBe('error'));
  it('returns neutral for unknown', () => expect(getPortfolioStatusVariant('Z')).toBe('neutral'));
});

describe('getPortfolioStatusLabel', () => {
  it('returns Active for A', () => expect(getPortfolioStatusLabel('A')).toBe('Active'));
  it('returns Inactive for I', () => expect(getPortfolioStatusLabel('I')).toBe('Inactive'));
  it('returns Closed for C', () => expect(getPortfolioStatusLabel('C')).toBe('Closed'));
  it('returns raw value for unknown', () => expect(getPortfolioStatusLabel('Z')).toBe('Z'));
});

describe('getTransactionStatusVariant', () => {
  it('returns success for C', () => expect(getTransactionStatusVariant('C')).toBe('success'));
  it('returns warning for P', () => expect(getTransactionStatusVariant('P')).toBe('warning'));
  it('returns error for E', () => expect(getTransactionStatusVariant('E')).toBe('error'));
  it('returns neutral for unknown', () => expect(getTransactionStatusVariant('X')).toBe('neutral'));
});

describe('getTransactionStatusLabel', () => {
  it('returns Completed for C', () => expect(getTransactionStatusLabel('C')).toBe('Completed'));
  it('returns Pending for P', () => expect(getTransactionStatusLabel('P')).toBe('Pending'));
  it('returns Error for E', () => expect(getTransactionStatusLabel('E')).toBe('Error'));
  it('returns raw value for unknown', () => expect(getTransactionStatusLabel('X')).toBe('X'));
});

describe('getBatchStatusVariant', () => {
  it('returns success for C', () => expect(getBatchStatusVariant('C')).toBe('success'));
  it('returns info for P', () => expect(getBatchStatusVariant('P')).toBe('info'));
  it('returns neutral for W', () => expect(getBatchStatusVariant('W')).toBe('neutral'));
  it('returns error for E', () => expect(getBatchStatusVariant('E')).toBe('error'));
  it('returns neutral for unknown', () => expect(getBatchStatusVariant('?')).toBe('neutral'));
});

describe('getBatchStatusLabel', () => {
  it('returns Completed for C', () => expect(getBatchStatusLabel('C')).toBe('Completed'));
  it('returns Processing for P', () => expect(getBatchStatusLabel('P')).toBe('Processing'));
  it('returns Waiting for W', () => expect(getBatchStatusLabel('W')).toBe('Waiting'));
  it('returns Error for E', () => expect(getBatchStatusLabel('E')).toBe('Error'));
  it('returns raw value for unknown', () => expect(getBatchStatusLabel('?')).toBe('?'));
});

describe('getAuditStatusVariant', () => {
  it('returns success for SUCC', () => expect(getAuditStatusVariant('SUCC')).toBe('success'));
  it('returns error for FAIL', () => expect(getAuditStatusVariant('FAIL')).toBe('error'));
  it('returns neutral for unknown', () => expect(getAuditStatusVariant('X')).toBe('neutral'));
});

describe('getSeverityVariant', () => {
  it('returns error for Error', () => expect(getSeverityVariant('Error')).toBe('error'));
  it('returns warning for Warning', () => expect(getSeverityVariant('Warning')).toBe('warning'));
  it('returns neutral for unknown', () => expect(getSeverityVariant('?')).toBe('neutral'));
});

describe('getPositionStatusVariant', () => {
  it('returns success for A', () => expect(getPositionStatusVariant('A')).toBe('success'));
  it('returns error for C', () => expect(getPositionStatusVariant('C')).toBe('error'));
  it('returns neutral for unknown', () => expect(getPositionStatusVariant('Z')).toBe('neutral'));
});

describe('getPositionStatusLabel', () => {
  it('returns Active for A', () => expect(getPositionStatusLabel('A')).toBe('Active'));
  it('returns Closed for C', () => expect(getPositionStatusLabel('C')).toBe('Closed'));
  it('returns raw value for unknown', () => expect(getPositionStatusLabel('Z')).toBe('Z'));
});

describe('getTransTypeLabel', () => {
  it('returns Buy for BY', () => expect(getTransTypeLabel('BY')).toBe('Buy'));
  it('returns Sell for SL', () => expect(getTransTypeLabel('SL')).toBe('Sell'));
  it('returns Fee for FE', () => expect(getTransTypeLabel('FE')).toBe('Fee'));
  it('returns raw value for unknown', () => expect(getTransTypeLabel('XX')).toBe('XX'));
});
