import { Router } from 'express';
import jwt from 'jsonwebtoken';
import bcrypt from 'bcryptjs';

const router = Router();
const JWT_SECRET = process.env.JWT_SECRET || 'clbs-dev-jwt-secret-change-in-production';

// In-memory user store for development
const users = [
  {
    id: 'user-001',
    username: 'admin',
    passwordHash: bcrypt.hashSync('admin123', 10),
  },
];

router.post('/login', async (req, res) => {
  const { username, password } = req.body;

  const user = users.find((u) => u.username === username);
  if (!user) {
    res.status(401).json({ code: 'E010', message: 'Invalid credentials' });
    return;
  }

  const valid = await bcrypt.compare(password, user.passwordHash);
  if (!valid) {
    res.status(401).json({ code: 'E010', message: 'Invalid credentials' });
    return;
  }

  const token = jwt.sign({ id: user.id, username: user.username }, JWT_SECRET, {
    expiresIn: '8h',
  });

  res.json({ token, user: { id: user.id, username: user.username } });
});

export default router;
