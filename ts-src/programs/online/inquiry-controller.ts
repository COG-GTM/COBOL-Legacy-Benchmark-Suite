/**
 * Inquiry Controller (Main Router).
 * Migrated from: src/programs/online/INQONLN.cbl
 *
 * The pseudo-conversational CICS controller becomes a stateless Express router
 * with endpoints for menu, portfolio inquiry, and history inquiry.
 */

import { Router, Request, Response } from 'express';
import { InquiryPortfolio } from './inquiry-portfolio';
import { InquiryHistory } from './inquiry-history';
import { MenuResponse } from './api-schemas';
import { Knex } from 'knex';

export function createInquiryRouter(db: Knex): Router {
  const router = Router();
  const portfolioInquiry = new InquiryPortfolio(db);
  const historyInquiry = new InquiryHistory(db);

  /** GET /api/menu – mirrors COBOL 1000-PROCESS-MENU. */
  router.get('/menu', (_req: Request, res: Response) => {
    const menu: MenuResponse = {
      title: 'Portfolio Management System – Main Menu',
      options: [
        { code: 'INQP', description: 'Portfolio Position Inquiry' },
        { code: 'INQH', description: 'Transaction History Inquiry' },
        { code: 'EXIT', description: 'Exit' },
      ],
    };
    res.json(menu);
  });

  /** GET /api/portfolio?accountNo=... – mirrors COBOL 2000-PROCESS-INQUIRY. */
  router.get('/portfolio', async (req: Request, res: Response) => {
    const accountNo = req.query.accountNo as string;
    if (!accountNo) {
      res.status(400).json({ error: 'accountNo is required' });
      return;
    }

    const { rc, data } = await portfolioInquiry.inquire(accountNo);
    if (rc === 0) {
      res.json(data);
    } else {
      res.status(rc >= 8 ? 500 : 404).json(data);
    }
  });

  /** GET /api/history?accountNo=...&startDate=...&endDate=...&limit=... */
  router.get('/history', async (req: Request, res: Response) => {
    const accountNo = req.query.accountNo as string;
    if (!accountNo) {
      res.status(400).json({ error: 'accountNo is required' });
      return;
    }

    const startDate = req.query.startDate as string | undefined;
    const endDate = req.query.endDate as string | undefined;
    const limit = req.query.limit ? Number(req.query.limit) : 50;

    const { rc, data } = await historyInquiry.inquire(accountNo, startDate, endDate, limit);
    if (rc === 0) {
      res.json(data);
    } else {
      res.status(rc >= 8 ? 500 : 404).json(data);
    }
  });

  return router;
}
