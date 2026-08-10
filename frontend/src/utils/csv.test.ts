import { describe, expect, it } from 'vitest';
import { toCsv } from './csv';

describe('toCsv', () => {
  it('joins the header and data rows with CRLF', () => {
    expect(toCsv(['a', 'b'], [['1', '2'], ['3', '4']])).toBe(
      'a,b\r\n1,2\r\n3,4',
    );
  });

  it('quotes fields containing a comma, quote, or newline', () => {
    expect(
      toCsv(['x'], [['a,b'], ['say "hi"'], ['line1\nline2'], ['plain']]),
    ).toBe('x\r\n"a,b"\r\n"say ""hi"""\r\n"line1\nline2"\r\nplain');
  });

  it('emits a header-only file for an empty result set', () => {
    expect(toCsv(['a', 'b'], [])).toBe('a,b');
  });
});
