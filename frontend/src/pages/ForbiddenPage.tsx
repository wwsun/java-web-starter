export default function ForbiddenPage() {
  return (
    <div className="flex flex-col items-center justify-center h-full text-center py-20">
      <p className="text-6xl font-bold text-slate-200">403</p>
      <h1 className="text-xl font-semibold text-slate-700 mt-4">无权限访问</h1>
      <p className="text-slate-500 mt-2">你没有权限查看此页面，请联系管理员。</p>
    </div>
  );
}
