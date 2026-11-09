import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { NewsAPI } from '../api/client';
import type { Article } from '../types';
import { Clock, ArrowRight, Loader2, AlertCircle } from 'lucide-react';

export default function Home() {
  const [articles, setArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    NewsAPI.searchArticles({ page: 0, size: 12 })
      .then((data) => {
        setArticles(data.items || []);
        setLoading(false);
      })
      .catch((err) => {
        console.error("Lỗi tải tin tức:", err);
        setError('Không thể kết nối với backend. Hãy đảm bảo Spring Boot đang chạy ở cổng 8080.');
        setLoading(false);
      });
  }, []);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-24 gap-3 text-slate-500">
        <Loader2 className="w-8 h-8 animate-spin text-brand-600" />
        <p>Đang tải dòng tin tức mới nhất...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="max-w-md mx-auto mt-16 p-6 bg-red-50 border border-red-200 rounded-2xl flex items-start gap-4 text-red-700">
        <AlertCircle className="w-6 h-6 flex-shrink-0 mt-0.5" />
        <div>
          <h3 className="font-semibold mb-1">Lỗi kết nối dữ liệu</h3>
          <p className="text-sm">{error}</p>
        </div>
      </div>
    );
  }

  if (articles.length === 0) {
    return (
      <div className="text-center py-20 text-slate-500">
        <p className="text-lg">Chưa có bài viết nào trong cơ sở dữ liệu.</p>
        <p className="text-sm mt-1">Hãy thực hiện crawl dữ liệu RSS từ backend để hiển thị tin tức.</p>
      </div>
    );
  }

  const featuredArticle = articles[0];
  const subArticles = articles.slice(1);

  return (
    <div className="space-y-12">
      {/* Tin nổi bật (Hero Section) */}
      {featuredArticle && (
        <section className="bg-white border border-slate-200 rounded-2xl overflow-hidden shadow-sm hover:shadow-md transition-shadow grid grid-cols-1 lg:grid-cols-12 gap-0">
          <div className="lg:col-span-7 relative min-h-[300px] bg-slate-100">
            {featuredArticle.imageUrl ? (
              <img 
                src={featuredArticle.imageUrl} 
                alt={featuredArticle.title} 
                className="absolute inset-0 w-full h-full object-cover"
                onError={(e)=>{ (e.target as HTMLElement).style.display = 'none'; }}
              />
            ) : (
              <div className="absolute inset-0 flex items-center justify-center text-slate-400 font-medium">News AI Image</div>
            )}
          </div>
          <div className="lg:col-span-5 p-8 flex flex-col justify-between">
            <div>
              <div className="flex items-center gap-2 mb-3">
                <span className="text-xs font-semibold uppercase tracking-wider bg-brand-50 text-brand-600 px-2.5 py-1 rounded-full">
                  {featuredArticle.category?.name || 'Tin tức'}
                </span>
                <span className="text-xs text-slate-400">•</span>
                <span className="text-xs text-slate-500 font-medium">{featuredArticle.source?.name}</span>
              </div>
              <h1 className="font-serif text-2xl lg:text-3xl font-bold text-slate-900 leading-snug mb-4">
                <Link to={`/article/${featuredArticle.id}`} className="hover:text-brand-600 transition-colors">
                  {featuredArticle.title}
                </Link>
              </h1>
              <p className="text-slate-600 text-sm line-clamp-3 leading-relaxed mb-6">
                {featuredArticle.description}
              </p>
            </div>
            <div className="flex items-center justify-between pt-4 border-t border-slate-100">
              <div className="flex items-center gap-1.5 text-xs text-slate-400">
                <Clock size={14} />
                <span>{new Date(featuredArticle.publishedAt).toLocaleDateString('vi-VN')}</span>
              </div>
              <Link 
                to={`/article/${featuredArticle.id}`}
                className="inline-flex items-center gap-1.5 text-sm font-semibold text-brand-600 hover:text-brand-700 transition-colors"
              >
                Đọc chi tiết <ArrowRight size={16} />
              </Link>
            </div>
          </div>
        </section>
      )}

      {/* Tin tức mới nhất */}
      <section>
        <h2 className="font-serif text-xl font-bold text-slate-900 mb-6 pb-2 border-b border-slate-200">
          Dòng tin mới nhất
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {subArticles.map((article) => (
            <article key={article.id} className="bg-white border border-slate-200 rounded-xl overflow-hidden shadow-sm hover:shadow-md transition-all flex flex-col justify-between">
              <div>
                <div className="relative aspect-video bg-slate-100 overflow-hidden">
                  {article.imageUrl ? (
                    <img 
                      src={article.imageUrl} 
                      alt={article.title} 
                      className="w-full h-full object-cover hover:scale-105 transition-transform duration-300"
                      onError={(e)=>{ (e.target as HTMLElement).style.display = 'none'; }}
                    />
                  ) : (
                    <div className="absolute inset-0 flex items-center justify-center text-slate-400 text-xs">Không có ảnh</div>
                  )}
                </div>
                <div className="p-5">
                  <div className="flex items-center gap-2 mb-2">
                    <span className="text-[11px] font-semibold uppercase tracking-wider bg-slate-100 text-slate-600 px-2 py-0.5 rounded">
                      {article.category?.name || 'Tổng hợp'}
                    </span>
                    <span className="text-xs text-slate-400">•</span>
                    <span className="text-xs text-slate-500">{article.source?.name}</span>
                  </div>
                  <h3 className="font-serif font-bold text-slate-900 line-clamp-2 mb-2 leading-snug">
                    <Link to={`/article/${article.id}`} className="hover:text-brand-600 transition-colors">
                      {article.title}
                    </Link>
                  </h3>
                  <p className="text-slate-600 text-xs line-clamp-2 leading-relaxed">
                    {article.description}
                  </p>
                </div>
              </div>
              <div className="px-5 pb-5 pt-0 flex items-center justify-between text-xs text-slate-400">
                <div className="flex items-center gap-1">
                  <Clock size={12} />
                  <span>{new Date(article.publishedAt).toLocaleDateString('vi-VN')}</span>
                </div>
                <Link to={`/article/${article.id}`} className="text-brand-600 font-medium hover:underline">
                  Xem ngay →
                </Link>
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}