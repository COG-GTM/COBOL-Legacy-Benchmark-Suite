import { Router, Request, Response } from "express";
import { CICSGateway } from "../services/cicsGateway";

const router = Router();
const cicsGateway = new CICSGateway();

router.get("/", async (req: Request, res: Response) => {
  const portfolio = req.query.portfolio as string;
  const page = parseInt(req.query.page as string, 10) || 1;

  if (!portfolio || portfolio.trim().length === 0) {
    res.status(400).json({ error: "Portfolio account number is required" });
    return;
  }

  try {
    const history = await cicsGateway.getTransactionHistory(portfolio, page);
    res.json({ data: history });
  } catch (err) {
    res.status(500).json({ error: "Error accessing transaction history" });
  }
});

export default router;
