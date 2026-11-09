import { Outlet, Link, useNavigate } from 'react-router-dom';
import { Search, Menu } from 'lucide-react';
import ChatbotWidget from './ChatbotWidget.tsx';
import { useState } from 'react';

export default function Layout() {
  const [keyword, setKeyword] = useState('');
  const navigate = useNavigate();

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (keyword.trim()) navigate(`/search?keyword=${encodeURIComponent(keyword)}`);
  };

  return (
    <div className="min-h-screen flex flex-col relative">
      <header className="sticky top-0 z-40 bg-white border-b border-slate-200 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button className="lg:hidden p-2 text-slate-600 hover:bg-slate-100 rounded-md" aria-label="Menu">
              <Menu size={24} />
            </button>
            <Link to="/" className="font-serif text-2xl font-bold text-brand-900 tracking-tight">
              News AI
            </Link>
          </div>
          
          <form onSubmit={handleSearch} className="hidden md:flex flex-1 max-w-md mx-8 relative">
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Tìm kiếm tin tức..."
              className="w-full pl-10 pr-4 py-2 bg-slate-100 border-transparent rounded-full focus:bg-white focus:border-brand-500 focus:ring-2 focus:ring-brand-200 outline-none transition-all"
            />
            <Search className="absolute left-3 top-2.5 text-slate-400" size={18} />
          </form>
        </div>
      </header>

      <main className="flex-1 max-w-7xl mx-auto w-full px-4 py-8">
        <Outlet />
      </main>

      <footer className="bg-slate-900 text-slate-400 py-8 text-center text-sm">
        <p>© 2026 News AI Platform. Developed for Advanced AI News Aggregation.</p>
      </footer>

      <ChatbotWidget />
    </div>
  );
}