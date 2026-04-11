import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import { LayoutDashboard, Users, LogOut, Search, Command, ChevronRight } from 'lucide-react';
import { useAuthStore } from '@/stores/useAuthStore';
import { Logo } from '@/components/common/Logo';
import { Button } from '@/components/ui/button';
import { PATHS } from '@/constants/paths';
import { cn } from '@/lib/utils';

const navItems = [
  { path: PATHS.DASHBOARD, label: 'Dashboard', icon: LayoutDashboard },
  { path: PATHS.USERS, label: 'User Management', icon: Users },
];

export default function MainLayout() {
  const navigate = useNavigate();
  const logout = useAuthStore((s) => s.logout);

  const handleLogout = () => {
    logout();
    navigate(PATHS.LOGIN, { replace: true });
  };

  return (
    <div className="flex h-screen bg-background font-sans overflow-hidden">
      {/* Sidebar - Border-only design */}
      <aside className="w-[240px] border-r border-border bg-background flex flex-col shrink-0">
        <div className="h-14 flex items-center px-6 gap-3">
          <Logo size="sm" />
          <span className="font-bold text-foreground text-sm tracking-tight font-heading">
            WebStarter
          </span>
          <div className="ml-auto flex items-center gap-1 border border-border rounded px-1.5 py-0.5 bg-muted/50">
            <span className="text-[10px] text-muted-foreground font-mono font-bold tracking-tighter">PRO</span>
          </div>
        </div>

        <nav className="flex-1 py-6 px-3 space-y-1">
          <div className="px-3 mb-2">
            <span className="text-[10px] font-mono font-bold uppercase tracking-widest text-muted-foreground/50">
              Overview
            </span>
          </div>
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              end={item.path === PATHS.HOME}
              className={({ isActive }) =>
                cn(
                  "flex items-center px-3 py-2 rounded-md text-sm font-medium transition-all group",
                  isActive
                    ? "bg-foreground text-background"
                    : "text-muted-foreground hover:text-foreground hover:bg-muted/50"
                )
              }
            >
              <item.icon className="mr-3 w-4 h-4 shrink-0" />
              {item.label}
              {item.path === PATHS.DASHBOARD && (
                <span className="ml-auto text-[10px] opacity-50 group-hover:opacity-100 transition-opacity">
                  ⌘1
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        {/* Bottom Sidebar */}
        <div className="p-3 border-t border-border mt-auto">
          <Button
            variant="ghost"
            onClick={handleLogout}
            className="w-full justify-start text-muted-foreground hover:text-destructive hover:bg-destructive/5 px-3 py-2 h-9 rounded-md transition-all group"
          >
            <LogOut className="mr-3 w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
            <span className="text-sm font-medium">Log out</span>
          </Button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0">
        <header className="h-14 bg-background border-b border-border flex items-center justify-between px-6 shrink-0 z-10 sticky top-0">
          <div className="flex items-center gap-2 overflow-hidden">
            <span className="text-muted-foreground text-xs font-mono">projects</span>
            <ChevronRight className="w-3 h-3 text-muted-foreground/30" />
            <span className="text-foreground text-sm font-bold tracking-tight truncate">starter-baseline</span>
            <div className="hidden sm:flex items-center ml-4 px-2 py-1 rounded bg-muted border border-border gap-2 group cursor-pointer transition-colors hover:border-foreground/20">
              <Search className="w-3 h-3 text-muted-foreground" />
              <span className="text-[11px] text-muted-foreground">Search...</span>
              <div className="flex items-center gap-0.5 ml-2">
                <Command className="w-2.5 h-2.5 text-muted-foreground/50" />
                <span className="text-[10px] text-muted-foreground/50 font-mono">K</span>
              </div>
            </div>
          </div>
          
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2 px-2 py-1 rounded-full border border-border hover:bg-muted transition-colors cursor-pointer pr-1">
              <div className="w-6 h-6 bg-foreground text-background rounded-full flex items-center justify-center text-[10px] font-bold">
                AD
              </div>
              <span className="text-xs font-bold mr-1">Admin</span>
            </div>
          </div>
        </header>

        <main className="flex-1 overflow-auto bg-muted/20 vercel-grid">
          <div className="max-w-[1200px] mx-auto p-8 animate-in fade-in duration-500">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}


