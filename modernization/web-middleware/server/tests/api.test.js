const request = require('supertest');
const { createApp } = require('../src/app');

const app = createApp();

describe('GET /api/menu (MENMAP / INQONLN P200-DISPLAY-MENU)', () => {
  it('returns the three menu options and an INQCOM command area', async () => {
    const res = await request(app).get('/api/menu').expect(200);
    expect(res.body.commarea).toMatchObject({
      inqcomFunction: 'MENU',
      inqcomResponseCode: 0,
    });
    expect(res.body.commarea.inqcomErrorMsg).toHaveLength(80);
    expect(res.body.options.map((o) => o.function)).toEqual(['INQP', 'INQH', 'EXIT']);
  });
});

describe('GET /api/position (POSMAP / INQPORT)', () => {
  it('returns a POSREC-shaped payload for a known account', async () => {
    const res = await request(app).get('/api/position?account=100000001').expect(200);
    expect(res.body.commarea.inqcomFunction).toBe('INQP');
    expect(Object.keys(res.body.position)).toEqual(
      expect.arrayContaining([
        'posPortfolioId',
        'posDate',
        'posInvestmentId',
        'posQuantity',
        'posCostBasis',
        'posMarketValue',
        'posCurrency',
        'posStatus',
        'posLastMaintDate',
        'posLastMaintUser',
      ])
    );
    expect(res.body.position.posPortfolioId).toHaveLength(8);
    expect(res.body.position.posDate).toMatch(/^\d{8}$/);
    expect(['A', 'C', 'P']).toContain(res.body.position.posStatus);
    expect(res.body.position.posCurrency).toHaveLength(3);
  });

  it('returns the P900-NOT-FOUND payload for an unknown account', async () => {
    const res = await request(app).get('/api/position?account=999999999').expect(404);
    expect(res.body.position).toBeNull();
    expect(res.body.commarea.inqcomResponseCode).not.toBe(0);
    expect(res.body.commarea.inqcomErrorMsg.trim()).toBe('Position not found for account');
  });
});

describe('GET /api/history (HISMAP / INQHIST)', () => {
  it('returns HISTREC-shaped rows paginated 10 per page (ROW1..ROW10)', async () => {
    const res = await request(app).get('/api/history?account=100000001&page=1').expect(200);
    expect(res.body.pageSize).toBe(10);
    expect(res.body.rows).toHaveLength(10);
    expect(res.body.totalPages).toBe(3);
    const row = res.body.rows[0];
    expect(Object.keys(row)).toEqual(
      expect.arrayContaining([
        'histPortfolioId',
        'histDate',
        'histTime',
        'histSeqNo',
        'histRecordType',
        'histActionCode',
      ])
    );
    expect(row.histTime).toMatch(/^\d{6}$/);
    expect(row.histSeqNo).toHaveLength(4);
    expect(['PT', 'PS', 'TR']).toContain(row.histRecordType);
    expect(['A', 'C', 'D']).toContain(row.histActionCode);
  });

  it('honours the page parameter (PF8 equivalent)', async () => {
    const res = await request(app).get('/api/history?account=100000001&page=3').expect(200);
    expect(res.body.rows).toHaveLength(3);
    expect(res.body.rows[0].histSeqNo).toBe('0021');
  });

  it('returns a not-found payload for an unknown account', async () => {
    const res = await request(app).get('/api/history?account=999999999').expect(404);
    expect(res.body.rows).toEqual([]);
    expect(res.body.commarea.inqcomErrorMsg.trim()).toBe('History not found for account');
  });
});
