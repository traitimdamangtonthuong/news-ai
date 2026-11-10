import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { NewsAPI } from '../api/client';
import type { Article } from '../types';
import { Clock, ExternalLink, ArrowLeft, Loader2, Sparkles, AlertCircle, Flame } from 'lucide-react';

export default function ArticleDetail() {
  const { id } = useParams<{ id: string }>();
  const [article, setArticle] = useState<Article | null>(null);
  const [topArticles, setTopArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    window.scrollTo(0, 0);
    if (id) {
      setLoading(true);
      Promise.all([
        NewsAPI.getArticle(Number(id)),
        NewsAPI.searchArticles({ page: 0, size: 6 })
      ])
        .then(([articleData, searchData]) => {
          setArticle(articleData);
          setTopArticles(searchData.items.filter((item: Article) => item.id !== Number(id)));
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
      <div className="article-detail-wrapper" style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
        <Loader2 className="w-8 h-8 animate-spin" style={{ color: '#2563eb', width: '36px', height: '36px' }} />
        <p style={{ fontSize: '14px', fontWeight: 500, color: '#64748b', marginTop: '12px' }}>Đang tải nội dung bài báo...</p>
      </div>
    );
  }

  if (error || !article) {
    return (
      <div className="article-detail-wrapper" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ maxWidth: '400px', width: '100%', padding: '32px', background: 'white', border: '1px solid #e2e8f0', borderRadius: '24px', textAlign: 'center', boxShadow: '0 4px 12px rgba(0,0,0,0.05)' }}>
          <AlertCircle style={{ width: '40px', height: '40px', margin: '0 auto 12px', color: '#ef4444' }} />
          <h3 style={{ fontSize: '18px', fontWeight: 'bold', color: '#0f172a', marginBottom: '8px' }}>Không tìm thấy bài viết</h3>
          <p style={{ fontSize: '14px', color: '#64748b', marginBottom: '24px' }}>{error || 'Bài viết không tồn tại hoặc đã bị xóa.'}</p>
          <Link to="/" className="article-back-btn" style={{ width: '100%', justifyContent: 'center', background: '#2563eb', color: 'white' }}>
            <ArrowLeft size={16} /> Quay lại bảng tin
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="article-detail-wrapper">
      <div className="article-detail-container">
        
        {/* Nút quay lại */}
        <div className="article-back-bar">
          <Link to="/" className="article-back-btn">
            <ArrowLeft size={16} /> Quay lại bảng tin
          </Link>
        </div>

        {/* Bố cục 2 cột chuẩn báo chí */}
        <div className="article-main-grid">
          
          {/* CỘT TRÁI: Nội dung chính */}
          <main className="article-content-card">
            
            <header className="article-header-section">
              <div className="article-meta-tags">
                <span className="article-cat-badge">
                  {article.category?.name || 'Tin tức'}
                </span>
                <span style={{ color: '#cbd5e1' }}>•</span>
                <span className="article-source-name">{article.source?.name}</span>
              </div>

              <h1 className="article-title-main">
                {article.title}
              </h1>

              <p className="article-desc-lead">
                {article.description}
              </p>

              <div className="article-info-bar">
                <div className="article-date-box">
                  <Clock size={16} style={{ color: '#2563eb' }} />
                  <span>Đăng ngày: {article.publishedAt ? new Date(article.publishedAt).toLocaleString('vi-VN') : 'Hôm nay'}</span>
                </div>

                {article.url && (
                  <a 
                    href={article.url} 
                    target="_blank" 
                    rel="noopener noreferrer"
                    className="article-original-link"
                  >
                    Xem bài gốc <ExternalLink size={14} />
                  </a>
                )}
              </div>
            </header>

            {/* Ảnh Cover */}
            {article.imageUrl && (
              <div className="article-cover-img-box">
                <img src={article.imageUrl} alt={article.title} className="article-cover-img" />
              </div>
            )}

            {/* Hộp AI */}
            <div className="article-ai-box">
              <div className="article-ai-icon">
                <Sparkles size={22} />
              </div>
              <div>
                <h3 style={{ fontWeight: 'bold', color: '#0f172a', fontSize: '15px', marginBottom: '2px' }}>Trợ lý AI hỗ trợ bài viết</h3>
                <p style={{ fontSize: '13px', color: '#475569', margin: 0 }}>Bạn có thể dùng khung chat AI ở góc phải màn hình để tóm tắt nội dung này.</p>
              </div>
            </div>

            {/* Nội dung chi tiết */}
            <div 
              className="article-body-html-content"
              dangerouslySetInnerHTML={{ __html: article.content || '<p>Đang cập nhật nội dung chi tiết bài viết...</p>' }}
            />
          </main>

          {/* CỘT PHẢI: Sidebar Xem nhiều */}
          <aside className="article-sidebar-card">
            <div className="sidebar-heading-row">
              <div className="sidebar-heading-icon">
                <Flame size={18} />
              </div>
              <h2 style={{ fontWeight: 800, color: '#0f172a', fontSize: '16px', textTransform: 'uppercase', letterSpacing: '0.05em', margin: 0 }}>Xem nhiều</h2>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              {topArticles.map((item, index) => (
                <Link 
                  to={`/article/${item.id}`} 
                  key={item.id}
                  className="sidebar-list-item"
                >
                  <span className="sidebar-item-num">
                    0{index + 1}
                  </span>
                  <div>
                    <span className="sidebar-item-source">
                      {item.source?.name || 'VNExpress'}
                    </span>
                    <p className="sidebar-item-title">
                      {item.title}
                    </p>
                  </div>
                </Link>
              ))}
            </div>
          </aside>

        </div>
      </div>
    </div>
  );
}