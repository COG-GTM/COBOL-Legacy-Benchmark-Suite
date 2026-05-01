import { Router, Request, Response, NextFunction } from 'express';
import { PrismaClient } from '@prisma/client';
import { loginSchema } from '../utils/validation.js';
import { generateToken } from '../middleware/auth.js';
import { AuthError } from '../utils/errors.js';

const prisma = new PrismaClient();
const router = Router();

// POST /api/auth/login
router.post('/login', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { username, password } = loginSchema.parse(req.body);

    const user = await prisma.user.findUnique({ where: { username } });

    if (!user || user.password !== password) {
      throw new AuthError('Invalid username or password');
    }

    const token = generateToken({
      userId: user.userId,
      username: user.username,
      role: user.role,
    });

    res.json({
      success: true,
      data: {
        token,
        user: {
          userId: user.userId,
          username: user.username,
          role: user.role,
        },
      },
    });
  } catch (err) {
    next(err);
  }
});

// POST /api/auth/register (for demo purposes)
router.post('/register', async (req: Request, res: Response, next: NextFunction) => {
  try {
    const { username, password } = loginSchema.parse(req.body);

    const existing = await prisma.user.findUnique({ where: { username } });
    if (existing) {
      throw new AuthError('Username already exists');
    }

    const userId = username.substring(0, 8).toUpperCase().padEnd(8, '0');
    const user = await prisma.user.create({
      data: {
        userId,
        username,
        password,
        role: 'user',
      },
    });

    const token = generateToken({
      userId: user.userId,
      username: user.username,
      role: user.role,
    });

    res.status(201).json({
      success: true,
      data: {
        token,
        user: {
          userId: user.userId,
          username: user.username,
          role: user.role,
        },
      },
    });
  } catch (err) {
    next(err);
  }
});

export default router;
