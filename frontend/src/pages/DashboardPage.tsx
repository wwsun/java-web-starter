import { usePermission } from '@/hooks/usePermission';

export default function DashboardPage() {
  const isAdmin = usePermission('ADMIN');
  return (
    <div className="space-y-6">
      {/* Welcome Section */}
      <div>
        <h1 className="text-2xl font-bold text-slate-800">仪表盘</h1>
        <p className="text-slate-500 mt-1">欢迎使用 Web Starter 管理后台</p>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        {[
          { label: '总用户数', value: '1,234', icon: '👥', color: 'blue' },
          { label: '今日访问', value: '567', icon: '📊', color: 'green' },
          { label: '系统消息', value: '23', icon: '💬', color: 'amber' },
          { label: '运行天数', value: '89', icon: '⏱️', color: 'purple' },
        ].map((stat) => (
          <div
            key={stat.label}
            className="bg-white rounded-xl p-6 shadow-sm border border-slate-100 hover:shadow-md transition-shadow"
          >
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm text-slate-500">{stat.label}</p>
                <p className="text-2xl font-bold text-slate-800 mt-1">{stat.value}</p>
              </div>
              <span className="text-3xl">{stat.icon}</span>
            </div>
          </div>
        ))}
      </div>

      {/* Placeholder Content */}
      <div className="bg-white rounded-xl p-8 shadow-sm border border-slate-100">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">快速开始</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {[
            { title: '添加业务模块', desc: '在 module/ 目录下创建新的业务模块', icon: '📦' },
            { title: '配置路由', desc: '在 router/index.tsx 中添加新页面路由', icon: '🔗' },
            { title: '查看 API 文档', desc: '访问 /api/doc.html 查看后端接口', icon: '📖' },
          ].map((item) => (
            <div
              key={item.title}
              className="p-4 rounded-lg bg-slate-50 hover:bg-blue-50 transition-colors cursor-pointer group"
            >
              <span className="text-2xl">{item.icon}</span>
              <h3 className="font-medium text-slate-700 mt-2 group-hover:text-blue-600 transition-colors">
                {item.title}
              </h3>
              <p className="text-sm text-slate-500 mt-1">{item.desc}</p>
            </div>
          ))}
          {isAdmin && (
            <div className="p-4 rounded-lg bg-blue-50 border border-blue-100 hover:bg-blue-100 transition-colors cursor-pointer group">
              <span className="text-2xl">👤</span>
              <h3 className="font-medium text-blue-700 mt-2">用户管理</h3>
              <p className="text-sm text-blue-500 mt-1">管理系统用户，分配角色（仅管理员可见）</p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
