import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50">
      <div className="text-center">
        <h1 className="text-8xl font-bold text-slate-200">404</h1>
        <p className="text-xl text-slate-600 mt-4">页面未找到</p>
        <p className="text-slate-400 mt-2">你访问的页面不存在或已被移除</p>
        <Link
          to="/"
          className="inline-block mt-6 px-6 py-3 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition-colors shadow-lg shadow-blue-600/30"
        >
          返回首页
        </Link>
      </div>
    </div>
  );
}
