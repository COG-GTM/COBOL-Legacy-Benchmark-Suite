import { describe, expect, it } from 'vitest';
import { AUDIT_FIXTURE } from '../data/audit.fixture';
import { MockReportService } from './mockReportService';

const service = new MockReportService();

describe('MockReportService', () => {
  it('reports every fixture portfolio holding positions', async () => {
    const report = await service.getPositionReport();
    expect(report.rows.length).toBeGreaterThan(0);
    expect(report.totals.holdings).toBeGreaterThan(0);
  });

  it('applies the audit query to the AUDITLOG fixture', async () => {
    const events = await service.listAuditEvents({ status: 'FAIL' });
    expect(events.length).toBeGreaterThan(0);
    expect(events.every((event) => event.status === 'FAIL')).toBe(true);
    expect(events.length).toBeLessThan(AUDIT_FIXTURE.length);
  });

  it('offers the portfolios and users present in the report data', async () => {
    const options = await service.getFilterOptions();
    expect(options.portfolioIds).toEqual([...options.portfolioIds].sort());
    expect(options.portfolioIds).toContain('PORT0001');
    expect(options.userIds).toContain('SYSOPER');
    expect(options.portfolioIds).not.toContain('');
  });

  it('produces statistics and a return analysis for the fixture period', async () => {
    const stats = await service.getSystemStatistics();
    expect(stats.daily.length).toBeGreaterThan(1);
    expect(stats.db2.calls).toBeGreaterThan(0);

    const analysis = await service.getReturnAnalysis();
    expect(analysis.rows.length).toBeGreaterThan(0);
    expect(analysis.totals.total).toBeGreaterThan(0);
  });
});
