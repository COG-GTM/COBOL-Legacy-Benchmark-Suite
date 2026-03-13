import { useState } from 'react';
import type { FormEvent } from 'react';
import { useNavigate, Navigate } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { useToast } from '@/context/ToastContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Building2, Lock, User } from 'lucide-react';

export function LoginPage() {
  const { isAuthenticated, login } = useAuth();
  const { addToast } = useToast();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  if (isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError('');

    if (!username.trim() || !password.trim()) {
      setError('Please enter both username and password');
      return;
    }

    const success = login(username, password);
    if (success) {
      addToast('Login successful. Welcome back!', 'success');
      navigate('/');
    } else {
      setError('Invalid username or password');
      addToast('Login failed. Please check your credentials.', 'error');
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[#0F172A] p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <div className="mx-auto mb-4 flex h-16 w-16 items-center justify-center rounded-full bg-[#22D3EE]/10">
            <Building2 className="h-8 w-8 text-[#22D3EE]" />
          </div>
          <CardTitle className="text-2xl">CLBS Portfolio Management</CardTitle>
          <CardDescription>Sign in to access the portfolio management system</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="username" className="mb-2 block text-sm font-medium text-[#CBD5E1]">
                Username
              </label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#94A3B8]" />
                <Input
                  id="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="Enter username"
                  className="pl-10"
                  autoComplete="username"
                />
              </div>
            </div>
            <div>
              <label htmlFor="password" className="mb-2 block text-sm font-medium text-[#CBD5E1]">
                Password
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#94A3B8]" />
                <Input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter password"
                  className="pl-10"
                  autoComplete="current-password"
                />
              </div>
            </div>
            {error && (
              <div className="rounded-md bg-[#F87171]/10 px-3 py-2 text-sm text-[#F87171]">
                {error}
              </div>
            )}
            <Button type="submit" className="w-full">
              Sign In
            </Button>
            <p className="text-center text-xs text-[#94A3B8]">
              Demo: use admin / admin to sign in
            </p>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
