import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import { createServer } from 'http';
import { Server as SocketIOServer } from 'socket.io';
import swaggerUi from 'swagger-ui-express';
import swaggerJsdoc from 'swagger-jsdoc';

import portfolioRoutes from './routes/portfolioRoutes.js';
import positionRoutes from './routes/positionRoutes.js';
import transactionRoutes from './routes/transactionRoutes.js';
import batchRoutes from './routes/batchRoutes.js';
import reportRoutes from './routes/reportRoutes.js';
import systemRoutes from './routes/systemRoutes.js';
import authRoutes from './routes/authRoutes.js';
import { errorHandler } from './middleware/errorHandler.js';

const app = express();
const httpServer = createServer(app);

// WebSocket server — channels for real-time updates
const io = new SocketIOServer(httpServer, {
  cors: {
    origin: process.env.FRONTEND_URL || 'http://localhost:5173',
    methods: ['GET', 'POST'],
  },
});

// Store io instance for use in routes
app.set('io', io);

// WebSocket connection handler
io.on('connection', (socket) => {
  console.log(`Client connected: ${socket.id}`);

  // Join portfolio-specific rooms
  socket.on('subscribe:portfolio', (portfolioId: string) => {
    socket.join(`portfolio:${portfolioId}`);
    console.log(`${socket.id} subscribed to portfolio:${portfolioId}`);
  });

  socket.on('unsubscribe:portfolio', (portfolioId: string) => {
    socket.leave(`portfolio:${portfolioId}`);
  });

  // Join batch status room
  socket.on('subscribe:batch', () => {
    socket.join('batch:status');
  });

  // Join system alerts room
  socket.on('subscribe:system', () => {
    socket.join('system:alerts');
  });

  socket.on('disconnect', () => {
    console.log(`Client disconnected: ${socket.id}`);
  });
});

// Middleware
app.use(helmet());
app.use(cors({
  origin: process.env.FRONTEND_URL || 'http://localhost:5173',
  credentials: true,
}));
app.use(express.json());

// Swagger/OpenAPI documentation
const swaggerSpec = swaggerJsdoc({
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'Portfolio Management API',
      version: '1.0.0',
      description: 'Investment Portfolio Management System — modernized from COBOL Legacy Benchmark Suite',
    },
    servers: [
      { url: '/api', description: 'API server' },
    ],
    components: {
      securitySchemes: {
        bearerAuth: {
          type: 'http',
          scheme: 'bearer',
          bearerFormat: 'JWT',
        },
      },
    },
    security: [{ bearerAuth: [] }],
  },
  apis: [],
});

app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerSpec));

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/portfolios', portfolioRoutes);
app.use('/api/portfolios', positionRoutes);
app.use('/api/transactions', transactionRoutes);
app.use('/api/batch', batchRoutes);
app.use('/api/reports', reportRoutes);
app.use('/api/system', systemRoutes);

// Error handler (ERRHNDL.cbl equivalent)
app.use(errorHandler);

// Start server
const PORT = parseInt(process.env.PORT || '3001', 10);
httpServer.listen(PORT, () => {
  console.log(`Portfolio Management API running on port ${PORT}`);
  console.log(`API docs available at http://localhost:${PORT}/api-docs`);
  console.log(`WebSocket server ready`);
});

export { app, httpServer, io };
