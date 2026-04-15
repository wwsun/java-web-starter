import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, Lock, AlertCircle, Eye, EyeOff } from 'lucide-react';
import { login as loginApi } from '@/api/auth';
import { getMe } from '@/api/user';
import { useAuthStore } from '@/stores/useAuthStore';
import { PATHS } from '@/constants/paths';
import { Logo } from '@/components/common/Logo';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader } from '@/components/ui/card';

export default function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const navigate = useNavigate();
  const setTokens = useAuthStore((s) => s.setTokens);
  const setRoles = useAuthStore((s) => s.setRoles);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      const { accessToken, refreshToken } = await loginApi({ username, password });
      setTokens(accessToken, refreshToken);

      const { roles } = await getMe();
      setRoles(roles);

      navigate(PATHS.HOME, { replace: true });
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : '登录失败');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-background vercel-grid px-4">
      {/* Background Decorative Gradient */}
      <div className="fixed inset-0 pointer-events-none opacity-40 dark:opacity-20 bg-[radial-gradient(circle_at_50%_50%,oklch(var(--foreground)/0.03)_0%,transparent_50%)]" />

      <div className="w-full max-w-[400px] space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-700">
        <div className="flex flex-col items-center gap-4 text-center">
          <Logo size="lg" className="mb-2" />
          <h1 className="text-2xl font-bold tracking-tight text-foreground">
            登录到 Web Starter
          </h1>
          <p className="text-muted-foreground text-sm">
            输入您的凭据以访问控制面板
          </p>
        </div>

        <Card className="border-border bg-card shadow-sm rounded-lg overflow-hidden">
          <CardHeader className="p-0 border-b border-border">
            <div className="flex bg-muted/30 px-6 py-3 items-center justify-between">
              <span className="text-[10px] font-mono uppercase tracking-widest text-muted-foreground font-bold">
                Authentication
              </span>
              <div className="flex gap-1">
                <div className="w-1.5 h-1.5 rounded-full bg-border" />
                <div className="w-1.5 h-1.5 rounded-full bg-border" />
              </div>
            </div>
          </CardHeader>

          <CardContent className="px-6 py-8">
            {error && (
              <div className="mb-6 p-3 rounded border border-destructive/20 bg-destructive/5 text-destructive text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4" />
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
              <div className="space-y-2">
                <Label htmlFor="username" className="text-xs font-mono uppercase tracking-wider text-muted-foreground">
                  Username
                </Label>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground/50" />
                  <Input
                    id="username"
                    type="text"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    required
                    className="pl-10 h-10 bg-transparent border-border focus-visible:ring-1 focus-visible:ring-foreground"
                    placeholder="Enter username"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="password" className="text-xs font-mono uppercase tracking-wider text-muted-foreground">
                  Password
                </Label>
                <div className="relative">
                  <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground/50" />
                  <Input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    className="pl-10 pr-10 h-10 bg-transparent border-border focus-visible:ring-1 focus-visible:ring-foreground"
                    placeholder="Enter password"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword((v) => !v)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground/50 hover:text-muted-foreground transition-colors"
                    tabIndex={-1}
                    aria-label={showPassword ? '隐藏密码' : '显示密码'}
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              <Button
                type="submit"
                disabled={loading}
                className="w-full h-10 bg-foreground text-background hover:bg-foreground/90 transition-all font-medium rounded-md"
              >
                {loading ? 'Processing...' : 'Continue to Dashboard'}
              </Button>
            </form>
          </CardContent>
        </Card>

        <p className="text-center text-xs text-muted-foreground/60 font-mono italic">
          Default Account: <span className="text-foreground">admin / admin123</span>
        </p>
      </div>
    </div>
  );
}


