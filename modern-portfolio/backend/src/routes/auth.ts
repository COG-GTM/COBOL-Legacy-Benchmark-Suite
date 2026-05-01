// Auth routes (replaces SECMGR from src/programs/online/SECMGR.cbl)
import { Router, Request, Response, NextFunction } from 'express';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { prisma } from '../index';
import { authenticate, AuthRequest } from '../middleware/auth';

const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-change-in-production';
const JWT_EXPIRES_IN = '24h';

export const authRouter = Router();

// POST /api/auth/login
authRouter.post('/login', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { username, password } = req.body;

    if (!username || !password) {
      res.status(400).json({
        success: false,
        error: { code: 'VL01', message: 'Username and password are required', category: 'VL', severity: 8 },
      });
      return;
    }

    const user = await prisma.user.findUnique({ where: { username } });

    if (!user || !user.isActive) {
      res.status(401).json({
        success: false,
        error: { code: 'SY01', message: 'Invalid credentials', category: 'SY', severity: 8 },
      });
      return;
    }

    const validPassword = await bcrypt.compare(password, user.passwordHash);
    if (!validPassword) {
      res.status(401).json({
        success: false,
        error: { code: 'SY01', message: 'Invalid credentials', category: 'SY', severity: 8 },
      });
      return;
    }

    const token = jwt.sign({ userId: user.id, role: user.role }, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN });

    await prisma.user.update({
      where: { id: user.id },
      data: { lastLogin: new Date() },
    });

    const { passwordHash: _, ...userWithoutPassword } = user;
    res.json({ success: true, data: { token, user: userWithoutPassword } });
  } catch (error) {
    next(error);
  }
});

// POST /api/auth/register
authRouter.post('/register', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { username, email, password, role } = req.body;

    if (!username || !email || !password) {
      res.status(400).json({
        success: false,
        error: { code: 'VL01', message: 'Username, email, and password are required', category: 'VL', severity: 8 },
      });
      return;
    }

    const existing = await prisma.user.findFirst({
      where: { OR: [{ username }, { email }] },
    });

    if (existing) {
      res.status(409).json({
        success: false,
        error: { code: 'VL03', message: 'Username or email already exists', category: 'VL', severity: 4 },
      });
      return;
    }

    const passwordHash = await bcrypt.hash(password, 10);
    const user = await prisma.user.create({
      data: { username, email, passwordHash, role: role || 'READ' },
    });

    const { passwordHash: _, ...userWithoutPassword } = user;
    res.status(201).json({ success: true, data: userWithoutPassword });
  } catch (error) {
    next(error);
  }
});

// POST /api/auth/logout
authRouter.post('/logout', authenticate, (_req: Request, res: Response) => {
  res.json({ success: true, data: { message: 'Logged out successfully' } });
});

// GET /api/auth/me
authRouter.get('/me', authenticate, async (req: AuthRequest, res: Response, next: NextFunction) => {
  try {
    const user = await prisma.user.findUnique({
      where: { id: req.userId },
      select: { id: true, username: true, email: true, role: true, isActive: true, lastLogin: true, createdAt: true, updatedAt: true },
    });

    if (!user) {
      res.status(404).json({
        success: false,
        error: { code: 'VL02', message: 'User not found', category: 'VL', severity: 4 },
      });
      return;
    }

    res.json({ success: true, data: user });
  } catch (error) {
    next(error);
  }
});
