import { usePermission } from '@/hooks/usePermission';
import {
  Users,
  BarChart3,
  MessageSquare,
  Timer,
  Box,
  ExternalLink,
  BookOpen,
  UserCheck
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';

export default function DashboardPage() {
  const isAdmin = usePermission('ADMIN');

  const stats = [
    { label: 'Total Users', value: '1,234', icon: Users, trend: '+12% month-over-month' },
    { label: 'Today\'s Visits', value: '567', icon: BarChart3, trend: '+5% since yesterday' },
    { label: 'System Messages', value: '23', icon: MessageSquare, trend: '4 urgent requiring action' },
    { label: 'Uptime (Days)', value: '89', icon: Timer, trend: '99.9% reliability score' },
  ];

  const quickLinks = [
    { title: 'Add Business Module', desc: 'Create new business modules in the module/ directory', icon: Box },
    { title: 'Configure Router', desc: 'Add new page routes in router/index.tsx', icon: ExternalLink },
    { title: 'API Documentation', desc: 'Visit /api/doc.html to view backend interfaces', icon: BookOpen },
  ];

  return (
    <div className="space-y-10">
      {/* Welcome Section */}
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-bold tracking-tight text-foreground">Overview</h1>
        <p className="text-muted-foreground text-sm font-medium tracking-tight">
          Welcome back to the Web Starter dashboard. Here's what's happening today.
        </p>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((stat) => (
          <Card key={stat.label} className="border-border bg-card shadow-none rounded-lg overflow-hidden group hover:border-foreground/20 transition-all">
            <CardContent className="p-6">
              <div className="flex items-center justify-between mb-4">
                <span className="text-[10px] font-mono font-bold uppercase tracking-widest text-muted-foreground/60">
                  {stat.label}
                </span>
                <stat.icon className="w-4 h-4 text-muted-foreground/40 group-hover:text-foreground transition-colors" />
              </div>
              <div className="space-y-1">
                <div className="text-2xl font-bold tracking-tighter text-foreground">{stat.value}</div>
                <p className="text-[10px] font-medium text-muted-foreground/50 tracking-tight">
                  {stat.trend}
                </p>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Main Content Sections */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-6">
          <div className="flex items-center justify-between border-b border-border pb-4">
            <h2 className="text-lg font-bold tracking-tight">Quick Start</h2>
            <span className="text-xs font-mono text-muted-foreground/50">Next Steps</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {quickLinks.map((link) => (
              <Card key={link.title} className="border-border bg-transparent shadow-none hover:bg-muted/50 cursor-pointer transition-all group">
                <CardContent className="p-5 space-y-3">
                  <div className="w-8 h-8 rounded border border-border flex items-center justify-center bg-background group-hover:border-foreground/30 transition-colors">
                    <link.icon className="w-4 h-4 text-muted-foreground" />
                  </div>
                  <div>
                    <h3 className="text-sm font-bold tracking-tight group-hover:underline underline-offset-4 decoration-1">
                      {link.title}
                    </h3>
                    <p className="text-[11px] text-muted-foreground leading-relaxed mt-1">
                      {link.desc}
                    </p>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>

          {isAdmin && (
            <Card className="border-border bg-foreground text-background shadow-none rounded-lg border-none">
              <CardContent className="p-6 flex items-center justify-between">
                <div className="space-y-1">
                  <h3 className="text-sm font-bold tracking-tight flex items-center gap-2">
                    <UserCheck className="w-4 h-4" />
                    Admin Management
                  </h3>
                  <p className="text-[11px] opacity-70">
                    Manage system users and assign roles. Restricted to administrators.
                  </p>
                </div>
                <Button variant="secondary" size="sm" className="h-8 text-[11px] font-bold">
                  Manage Now
                </Button>
              </CardContent>
            </Card>
          )}
        </div>

        {/* Placeholder Side Card */}
        <div className="space-y-6">
          <div className="flex items-center justify-between border-b border-border pb-4">
            <h2 className="text-lg font-bold tracking-tight">Activity</h2>
            <span className="text-xs font-mono text-muted-foreground/50">Recent</span>
          </div>
          <Card className="border-border bg-card shadow-none">
            <CardContent className="p-0">
              <div className="p-4 border-b border-border flex items-center gap-3">
                <div className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
                <span className="text-xs font-medium">System operational</span>
              </div>
              <div className="p-6 space-y-4">
                {[
                  { time: '2m ago', event: 'New user registered' },
                  { time: '1h ago', event: 'Backup completed' },
                  { time: '4h ago', event: 'Critical update applied' },
                ].map((log, i) => (
                  <div key={i} className="flex items-start justify-between gap-4">
                    <p className="text-xs font-medium text-foreground">{log.event}</p>
                    <span className="text-[10px] font-mono text-muted-foreground shrink-0">{log.time}</span>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}

