import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { NewsAPI } from '../api/client';
import type { Article } from '../types';
import { Clock, ExternalLink, ArrowLeft, Loader2, Sparkles, AlertCircle } from 'lucide-react';

export default function ArticleDetail() {
  const { id } = useParams<{ id: string }>();
  const [article, setArticle] = useState<Article | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (id) {
      setLoading(true);
      NewsAPI.getArticle(Number(id))
        .then((data) => {
          setArticle(data);
          setLoading(false);
        })
        .catch((err) => {
          console.error("Lỗi lấy chi tiết bài báo:", err);
          setError('Không thể tải nội dung bài viết.');
          setLoading(false);
        });
    }
  }, [id]);

  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center py-24 gap-3 text-slate-500">
        <Loader2 className="w-8 h-8 animate-spin text-brand-600" />
        <p>Đang tải nội dung bài báo...</p>
      </div>
    );
  }

  if (error || !article) {
    return (
      <div className="max-w-md mx-auto mt-16 p-6 bg-red-50 border border-red-200 rounded-2xl text-center text-red-700">
        <AlertCircle className="w-8 h-8 mx-auto mb-2" />
        <h3 className="font-semibold mb-1">Không tìm thấy bài viết</h3>
        <p className="text-sm mb-4">{error || 'Bài viết không tồn tại hoặc đã bị xóa.'}</p>
        <Link to="/" className="inline-flex items-center gap-2 bg-red-600 text-white px-4 py-2 rounded-xl text-sm font-medium hover:bg-red-700 transition-colors">
          <ArrowLeft size={16} /> Quay lại trang chủ
        </Link>
      </div>
    );
  }

  return (
    <article className="max-w-3xl mx-auto pb-16">
      {/* Nút quay lại */}
      <div className="mb-6">
        <Link to="/" className="inline-flex items-center gap-2 text-sm font-medium text-slate-500 hover:text-slate-800 transition-colors">
          <ArrowLeft size={16} /> Quay lại bảng tin
        </Link>
      </div>

      {/* Header bài viết */}
      <header className="mb-8">
        <div className="flex items-center gap-2 mb-4">
          <span className="text-xs font-semibold uppercase tracking-wider bg-brand-50 text-brand-600 px-3 py-1 rounded-full">
            {article.category?.name || 'Tin tức'}
          </span>
          <span className="text-slate-300">•</span>
          <span className="text-sm text-slate-600 font-medium">{article.source?.name}</span>
        </div>

        <h1 className="font-serif text-3xl md:text-4xl font-bold text-slate-900 leading-tight mb-4">
          {article.title}
        </h1>

        <p className="text-lg text-slate-600 leading-relaxed mb-6 font-serif italic">
          {article.description}
        </p>

        <div className="flex items-center justify-between py-4 border-y border-slate-200 text-sm text-slate-500">
          <div className="flex items-center gap-2">
            <Clock size={16} />
            <span>Đăng ngày: {new Date(article.publishedAt).toLocaleString('vi-VN')}</span>
          </div>

          {article.url && (
            <a 
              href={article.url} 
              target="_blank" 
              rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 text-brand-600 hover:text-brand-700 font-medium transition-colors"
            >
              Xem bài gốc <ExternalLink size={14} />
            </a>
          )}
        </div>
      </header>

      {/* Ảnh Cover */}
      {article.imageUrl && (
        <div className="mb-8 aspect-video rounded-2xl overflow-hidden bg-slate-100 shadow-sm">
          <img src={article.imageUrl} alt={article.title} className="w-full h-full object-cover" />
        </div>
      )}

      {/* Hộp gợi ý hỏi AI */}
      <div className="bg-brand-50 border border-brand-100 rounded-2xl p-5 mb-8 flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="bg-brand-600 text-white p-2.5 rounded-xl">
            <Sparkles size={20} />
          </div>
          <div>
            <h3 className="font-semibold text-slate-900 text-sm">Bạn cần tóm tắt hoặc hỏi đáp về bài viết này?</h3>
            <p className="text-xs text-slate-600">Sử dụng Trợ lý AI ở góc dưới bên phải màn hình để bắt đầu.</p>
          </div>
        </div>
      </div>

      {/* Nội dung chi tiết (Render HTML an toàn từ cơ sở dữ liệu) */}
      <div 
        className="prose prose-slate lg:prose-lg max-w-none font-serif leading-relaxed space-y-4 text-slate-800"
        dangerouslySetInnerHTML={{ __html: article.content || '<p>Đang cập nhật nội dung chi tiết bài viết...</p>' }}
      />
    </article>
  );
}