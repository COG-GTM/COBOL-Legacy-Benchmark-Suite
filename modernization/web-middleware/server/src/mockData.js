/**
 * In-memory mock data store.
 *
 * Shapes mirror the COBOL copybooks:
 *   src/copybook/common/POSREC.cpy   -> positions
 *   src/copybook/common/HISTREC.cpy  -> history
 * Sample values follow documentation/operations/test-data-specs.md
 * (PORT00001..PORT00003) and documentation/technical/data-dictionary.md
 * (9-digit ACCOUNT-NO, 6-char FUND-ID, PRICE 9(5)V9999, AMOUNT S9(11)V99).
 */

const fundNames = {
  IBM001: 'GLOBAL TECHNOLOGY FUND',
  MSFT01: 'INCOME GROWTH FUND',
  AAPL01: 'BALANCED EQUITY FUND',
};

/** POSREC.cpy: POS-KEY + POS-DATA + POS-AUDIT */
const positions = {
  '100000001': {
    posPortfolioId: 'PORT0001',
    posDate: '20240320',
    posInvestmentId: 'IBM001',
    posFundName: fundNames.IBM001,
    posQuantity: 12500.0,
    posCostBasis: 12345678.99,
    posMarketValue: 13980221.45,
    posCurrency: 'SGD',
    posStatus: 'A',
    posLastMaintDate: '2024-03-20-15.30.45.123456',
    posLastMaintUser: 'BATCHUSR',
  },
  '100000002': {
    posPortfolioId: 'PORT0002',
    posDate: '20240320',
    posInvestmentId: 'MSFT01',
    posFundName: fundNames.MSFT01,
    posQuantity: 5000.0,
    posCostBasis: 98765432.1,
    posMarketValue: 97110044.02,
    posCurrency: 'USD',
    posStatus: 'A',
    posLastMaintDate: '2024-03-20-15.31.12.789012',
    posLastMaintUser: 'BATCHUSR',
  },
  '100000003': {
    posPortfolioId: 'PORT0003',
    posDate: '20240320',
    posInvestmentId: 'AAPL01',
    posFundName: fundNames.AAPL01,
    posQuantity: 7500.0,
    posCostBasis: 5555555.55,
    posMarketValue: 5121300.75,
    posCurrency: 'SGD',
    posStatus: 'P',
    posLastMaintDate: '2024-03-20-15.32.01.456789',
    posLastMaintUser: 'ONLNUSR',
  },
};

/** HISTREC.cpy: HIST-KEY + HIST-DATA (+ display fields used by HISMAP rows) */
function buildHistory(portfolioId, investmentId, count, seed) {
  const rows = [];
  for (let i = 0; i < count; i += 1) {
    const day = String(1 + (i % 28)).padStart(2, '0');
    const type = i % 3 === 0 ? 'TR' : i % 3 === 1 ? 'PS' : 'PT';
    const action = i % 3 === 0 ? 'A' : i % 3 === 1 ? 'C' : 'D';
    const units = Number((seed * (i + 1) * 1.5).toFixed(3));
    const price = Number((10 + ((seed + i) % 90) + 0.25 * (i % 4)).toFixed(4));
    rows.push({
      histPortfolioId: portfolioId,
      histDate: `202403${day}`,
      histTime: `${String(9 + (i % 8)).padStart(2, '0')}3045`,
      histSeqNo: String(i + 1).padStart(4, '0'),
      histRecordType: type,
      histActionCode: action,
      histInvestmentId: investmentId,
      histUnits: units,
      histPrice: price,
      histAmount: Number((units * price).toFixed(2)),
      histReasonCode: '0000',
      histProcessDate: `2024-03-${day}-15.30.45.123456`,
      histProcessUser: 'BATCHUSR',
    });
  }
  return rows;
}

const history = {
  '100000001': buildHistory('PORT0001', 'IBM001', 23, 12),
  '100000002': buildHistory('PORT0002', 'MSFT01', 14, 7),
  '100000003': buildHistory('PORT0003', 'AAPL01', 6, 3),
};

/** MENMAP options (src/maps/INQSET.bms) -> INQCOM-FUNCTION values */
const menuOptions = [
  { option: '1', label: 'Portfolio Position Inquiry', function: 'INQP', route: '/position' },
  { option: '2', label: 'Transaction History', function: 'INQH', route: '/history' },
  { option: '3', label: 'Exit', function: 'EXIT', route: '/exit' },
];

module.exports = { positions, history, menuOptions };
