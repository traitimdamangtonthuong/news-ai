import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import {
  getCategories,
  searchArticles,
  type Category,
} from "../api/articles";
import Navbar from "../components/Navbar";
import { extractFirstImageUrl, stripHtmlToText } from "../lib/articleFormat";
import type { ArticleSearchResponse } from "../types/articles";

const PAGE_SIZE = 12;

interface NewsPageProps {
  initialCategory?: string;
  onOpenArticle: (id: number) => void;
}

export default function NewsPage({
  initialCategory = "",
  onOpenArticle,
}: NewsPageProps) {
  const [inputKeyword, setInputKeyword] = useState("");
  const [keyword, setKeyword] = useState("");

  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>(initialCategory);

  const [publishedFrom, setPublishedFrom] = useState<string>("");
  const [publishedTo, setPublishedTo] = useState<string>("");

  const [page, setPage] = useState(0);
  const [data, setData] = useState<ArticleSearchResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (initialCategory) {
      setSelectedCategory(initialCategory);
    }
  }, [initialCategory]);

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

      {loading && <p className="status">Đang tải bài viết...</p>}

      {!loading && error && (
        <p className="status error">
          {error}. Hãy kiểm tra Spring Boot có đang chạy ở cổng 8080 không.
        </p>
      )}

      {!loading && !error && data?.items.length === 0 && (
        <p className="status">
          Không tìm thấy bài viết phù hợp với bộ lọc hiện tại.
        </p>
      )}

      {!loading && !error && data && data.items.length > 0 && (
        <>
          <p className="result-count">Tìm thấy {data.totalElements} bài viết.</p>

          <section className="article-grid">
            {data.items.map((article) => {
              const cardImage =
                article.imageUrl ?? extractFirstImageUrl(article.description);
              const lead =
                stripHtmlToText(article.description) ??
                "Bài viết chưa có phần mô tả.";

              return (
                <article
                  className="article-card clickable-card"
                  key={article.id}
                  onClick={() => onOpenArticle(article.id)}
                >
                  {cardImage ? (
                    <img src={cardImage} alt="" loading="lazy" />
                  ) : (
                    <div className="image-placeholder">NEWS</div>
                  )}

                  <div className="article-body">
                    <div className="article-meta">
                      <span className="source-tag">
                        {article.source.name}
                      </span>
                      {article.category && (
                        <span className="category-tag">
                          {article.category.name}
                        </span>
                      )}
                    </div>

                    <h2
                      className="article-title"
                      title="Bấm để đọc nội dung bài viết"
                    >
                      {article.title}
                    </h2>
                    <p>{lead}</p>

                    {article.publishedAt && (
                      <time dateTime={article.publishedAt}>
                        {new Date(article.publishedAt).toLocaleDateString(
                          "vi-VN",
                        )}
                      </time>
                    )}
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