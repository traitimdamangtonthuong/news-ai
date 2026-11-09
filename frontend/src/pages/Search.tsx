import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { NewsAPI } from '../api/client';
import type { Article } from '../types';
import { Search as SearchIcon, Loader2, Clock, ArrowRight } from 'lucide-react';

export default function Search() {
  const [searchParams, setSearchParams] = useSearchParams();
  const keywordQuery = searchParams.get('keyword') || '';
  
  const [keyword, setKeyword] = useState(keywordQuery);
  const [articles, setArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);

  useEffect(() => {
    setKeyword(keywordQuery);
    if (keywordQuery.trim()) {
      setLoading(true);
      NewsAPI.searchArticles({ keyword: keywordQuery, page: 0, size: 10 })
        .then((data) => {
          setArticles(data.items || []);
          setTotal(data.totalElements || 0);
          setLoading(false);
        })
        .catch((err) => {
          console.error("Lỗi tìm kiếm:", err);
          setLoading(false);
        });
    } else {
      setArticles([]);
      setTotal(0);
    }
  }, [keywordQuery]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (keyword.trim()) {
      setSearchParams({ keyword: keyword.trim() });
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <div className="bg-white border border-slate-200 p-6 rounded-2xl shadow-sm">
        <h1 className="font-serif text-2xl font-bold text-slate-900 mb-4">Tìm kiếm tin tức</h1>
        <form onSubmit={handleSearchSubmit} className="flex gap-3">
          <div className="relative flex-1">
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Nhập từ khóa cần tìm (vd: AI, công nghệ, y tế...)..."
              className="w-full pl-11 pr-4 py-3 bg-slate-50 border border-slate-200 rounded-xl focus:bg-white focus:border-brand-500 focus:ring-2 focus:ring-brand-200 outline-none transition-all text-sm"
            />
            <SearchIcon className="absolute left-4 top-3.5 text-slate-400" size={18} />
          </div>
          <button type="submit" className="bg-brand-600 hover:bg-brand-700 text-white px-6 py-3 rounded-xl font-medium text-sm transition-colors shadow-sm">
            Tìm kiếm
          </button>
        </form>
      </div>

      {loading ? (
        <div className="flex flex-col items-center justify-center py-16 gap-3 text-slate-500">
          <Loader2 className="w-8 h-8 animate-spin text-brand-600" />
          <p>Đang tìm kiếm bài viết phù hợp...</p>
        </div>
      ) : (
        <div>
          {keywordQuery && (
            <p className="text-sm text-slate-500 mb-6">
              Tìm thấy <strong className="text-slate-900">{total}</strong> kết quả cho từ khóa "<strong className="text-slate-900">{keywordQuery}</strong>"
            </p>
          )}

          <div className="space-y-4">
            {articles.map((article) => (
              <article key={article.id} className="bg-white border border-slate-200 p-5 rounded-xl shadow-sm hover:shadow-md transition-all flex flex-col md:flex-row gap-6 items-start">
                {article.imageUrl && (
                  <div className="w-full md:w-56 aspect-video bg-slate-100 rounded-lg overflow-hidden flex-shrink-0">
                    <img src={article.imageUrl} alt={article.title} className="w-full h-full object-cover" />
                  </div>
                )}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-2">
                    <span className="text-[11px] font-semibold uppercase tracking-wider bg-slate-100 text-slate-600 px-2 py-0.5 rounded">
                      {article.category?.name || 'Tin tức'}
                    </span>
                    <span className="text-xs text-slate-400">•</span>
                    <span className="text-xs text-slate-500">{article.source?.name}</span>
                  </div>
                  <h2 className="font-serif font-bold text-lg text-slate-900 mb-2 leading-snug">
                    <Link to={`/article/${article.id}`} className="hover:text-brand-600 transition-colors">
                      {article.title}
                    </Link>
                  </h2>
                  <p className="text-slate-600 text-sm line-clamp-2 leading-relaxed mb-4">
                    {article.description}
                  </p>
                  <div className="flex items-center justify-between pt-3 border-t border-slate-100 text-xs text-slate-400">
                    <div className="flex items-center gap-1">
                      <Clock size={12} />
                      <span>{new Date(article.publishedAt).toLocaleDateString('vi-VN')}</span>
                    </div>
                    <Link to={`/article/${article.id}`} className="inline-flex items-center gap-1 font-semibold text-brand-600 hover:underline">
                      Đọc tiếp <ArrowRight size={14} />
                    </Link>
                  </div>
                </div>
              </article>
            ))}

            {keywordQuery && articles.length === 0 && (
              <div className="text-center py-16 bg-white border border-slate-200 rounded-2xl text-slate-500">
                <p className="text-base font-medium mb-1">Không tìm thấy bài viết nào</p>
                <p className="text-sm">Hãy thử lại với từ khóa khác tổng quát hơn.</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}