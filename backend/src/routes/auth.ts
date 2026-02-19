import { Router, Request, Response } from "express";
import { CICSGateway } from "../services/cicsGateway";

const router = Router();
const cicsGateway = new CICSGateway();

router.post("/login", async (req: Request, res: Response) => {
  const { userId, password } = req.body;

  if (!userId || !password) {
    res.status(400).json({ error: "User ID and password are required" });
    return;
  }

  try {
    const result = await cicsGateway.authenticate(userId, password);

    if (result.success) {
      res.json({
        data: {
          userId: result.userId,
          token: result.token,
        },
      });
    } else {
      res.status(401).json({ error: result.errorMessage });
    }
  } catch (err) {
    res.status(500).json({ error: "Authentication service unavailable" });
  }
});

export default router;
