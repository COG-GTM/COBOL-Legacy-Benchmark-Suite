import express from 'express';
import cors from 'cors';
import { PrismaClient } from '@prisma/client';
import { authRouter } from './routes/auth';
import { portfolioRouter } from './routes/portfolios';
import { positionRouter } from './routes/positions';
import { transactionRouter } from './routes/transactions';
import { reportRouter } from './routes/reports';
import { jobRouter } from './routes/jobs';
import { errorHandler } from './middleware/errorHandler';
import { auditMiddleware } from './middleware/audit';

export const prisma = new PrismaClient();

const app = express();
const PORT = process.env.PORT || 3001;

app.use(cors());
app.use(express.json());
app.use(auditMiddleware);

// Routes
app.use('/api/auth', authRouter);
app.use('/api/portfolios', portfolioRouter);
app.use('/api/positions', positionRouter);
app.use('/api/transactions', transactionRouter);
app.use('/api/reports', reportRouter);
app.use('/api/jobs', jobRouter);

// Health check
app.get('/api/health', (_req, res) => {
  res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Error handling
app.use(errorHandler);

async function main() {
  try {
    await prisma.$connect();
    console.log('Database connected');

    app.listen(PORT, () => {
      console.log(`Server running on port ${PORT}`);
    });
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
}

main();

export default app;
