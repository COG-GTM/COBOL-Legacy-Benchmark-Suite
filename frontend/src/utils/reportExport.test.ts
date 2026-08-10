import { describe, expect, it } from 'vitest';
import { toCsv } from './csv';
import { toPdf, toPrintLines } from './pdf';
import { reportFileStem, type ReportDocument } from './reportDocument';

const REPORT: ReportDocument = {
  title: 'Position Report',
  meta: [
    { label: 'Program', value: 'RPTPOS00' },
    { label: 'Period', value: '2024-04-01 to 2024-04-02' },
  ],
  columns: [
    { label: 'Portfolio' },
    { label: 'Client' },
    { label: 'Market Value', align: 'right' },
  ],
  rows: [
    ['PORT0001', 'Margaret Chen', '$1,096,290.55'],
    ['PORT0002', 'Atlas Holdings, LLC', '$8,845,200.00'],
  ],
  totalsRow: ['TOTALS', '', '$9,941,490.55'],
};

describe('toCsv', () => {
  it('writes the heading, filters, columns, detail lines and totals', () => {
    expect(toCsv(REPORT)).toBe(
      [
        'Position Report',
        'Program,RPTPOS00',
        'Period,2024-04-01 to 2024-04-02',
        '',
        'Portfolio,Client,Market Value',
        'PORT0001,Margaret Chen,"$1,096,290.55"',
        'PORT0002,"Atlas Holdings, LLC","$8,845,200.00"',
        'TOTALS,,"$9,941,490.55"',
      ].join('\r\n') + '\r\n',
    );
  });

  it('escapes embedded quotes', () => {
    const csv = toCsv({
      ...REPORT,
      meta: undefined,
      rows: [['PORT0001', 'The "Whitfield" Trust', '0.00']],
      totalsRow: undefined,
    });
    expect(csv).toContain('"The ""Whitfield"" Trust"');
  });
});

describe('toPrintLines', () => {
  it('aligns columns into fixed-width print lines', () => {
    const lines = toPrintLines(REPORT);

    expect(lines[0]).toBe('Position Report');
    expect(lines[1]).toBe('Program: RPTPOS00');
    // Numeric columns are right aligned to the widest cell in the column.
    expect(lines).toContain(
      'PORT0001   Margaret Chen        $1,096,290.55',
    );
    expect(lines).toContain('TOTALS                          $9,941,490.55');
  });
});

describe('toPdf', () => {
  it('produces a PDF with a page per block of print lines', () => {
    const pdf = new TextDecoder('latin1').decode(toPdf(REPORT));

    expect(pdf.startsWith('%PDF-1.4')).toBe(true);
    expect(pdf.trimEnd().endsWith('%%EOF')).toBe(true);
    expect(pdf).toContain('/Type /Catalog');
    expect(pdf).toContain('/BaseFont /Courier');
    expect(pdf).toMatch(/\/Type \/Pages \/Count 1 /);
    expect(pdf).toContain('(Position Report) Tj');
  });

  it('paginates long reports', () => {
    const rows = Array.from({ length: 120 }, (_, index) => [
      `PORT${index}`,
      'Client',
      '1.00',
    ]);
    const pdf = new TextDecoder('latin1').decode(
      toPdf({ ...REPORT, rows, totalsRow: undefined }),
    );
    expect(pdf).toMatch(/\/Type \/Pages \/Count 3 /);
  });

  it('escapes PDF syntax and replaces unsupported characters', () => {
    const pdf = new TextDecoder('latin1').decode(
      toPdf({
        ...REPORT,
        meta: undefined,
        rows: [['PORT0001', 'Chen (Trust) \\ Co', '—']],
        totalsRow: undefined,
      }),
    );
    expect(pdf).toContain('Chen \\(Trust\\) \\\\ Co');
    expect(pdf).not.toContain('—');
  });
});

describe('reportFileStem', () => {
  it('slugifies the report title', () => {
    expect(reportFileStem('Return Code Analysis')).toBe('return-code-analysis');
    expect(reportFileStem('!!!')).toBe('report');
  });
});
