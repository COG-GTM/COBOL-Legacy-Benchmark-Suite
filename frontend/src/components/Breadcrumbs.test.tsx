import { describe, expect, it } from 'vitest';
import { buildCrumbs } from '../nav/breadcrumbs';

describe('buildCrumbs', () => {
  it('returns a single Dashboard crumb at the root', () => {
    expect(buildCrumbs('/')).toEqual([{ label: 'Dashboard', path: '/' }]);
  });

  it('maps known section paths to their nav labels', () => {
    expect(buildCrumbs('/transactions')).toEqual([
      { label: 'Dashboard', path: '/' },
      { label: 'Transactions', path: '/transactions' },
    ]);
  });

  it('humanizes unknown nested segments', () => {
    expect(buildCrumbs('/portfolios/new-record')).toEqual([
      { label: 'Dashboard', path: '/' },
      { label: 'Portfolios', path: '/portfolios' },
      { label: 'New Record', path: '/portfolios/new-record' },
    ]);
  });
});
