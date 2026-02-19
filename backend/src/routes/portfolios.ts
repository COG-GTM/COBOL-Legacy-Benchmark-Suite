import { Router, Request, Response } from "express";
import { CICSGateway } from "../services/cicsGateway";

const router = Router();
const cicsGateway = new CICSGateway();

router.get("/:id", async (req: Request, res: Response) => {
  const { id } = req.params;

  if (!id || id.trim().length === 0) {
    res.status(400).json({ error: "Account number is required" });
    return;
  }

  try {
    const position = await cicsGateway.getPortfolioPosition(id);

    if (!position) {
      res.status(404).json({ error: "Position not found for account" });
      return;
    }

    res.json({ data: position });
  } catch (err) {
    res.status(500).json({ error: "Error accessing position data" });
  }
});

export default router;
