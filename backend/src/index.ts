import express from "express";
import cors from "cors";
import portfolioRoutes from "./routes/portfolios";
import historyRoutes from "./routes/history";
import authRoutes from "./routes/auth";

const app = express();
const PORT = process.env.PORT || 3001;

app.use(cors());
app.use(express.json());

app.use("/api/portfolios", portfolioRoutes);
app.use("/api/history", historyRoutes);
app.use("/api/auth", authRoutes);

app.get("/api/health", (_req, res) => {
  res.json({ status: "ok", service: "portfolio-api-gateway" });
});

app.listen(PORT, () => {
  console.log(`API Gateway running on port ${PORT}`);
});

export default app;
