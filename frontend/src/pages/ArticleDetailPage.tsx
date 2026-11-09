import { useEffect, useState } from "react";
import {
  getArticleById,
  searchArticles,
  type ArticleDetail,
} from "../api/articles";
import { extractFirstImageUrl, stripHtmlToText } from "../lib/articleFormat";
import type { ArticleSearchItem } from "../types/articles";

interface ArticleDetailPageProps {
  articleId: number;
  onBack: () => void;
  onSelectArticle: (id: number) => void;
  onSelectCategory?: (categoryId: string) => void;
}

export default function ArticleDetailPage({
  articleId,
  onBack,
  onSelectArticle,
  onSelectCategory,
}: ArticleDetailPageProps) {
  const [article, setArticle] = useState<ArticleDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [relatedArticles, setRelatedArticles] = useState<ArticleSearchItem[]>([]);
  const [copied, setCopied] = useState(false);

  // Cuộn lên đầu trang khi bài viết thay đổi
  useEffect(() => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  }, [articleId]);

  // Tải chi tiết bài viết
  useEffect(() => {
    async function loadDetail() {
      try {
        setLoading(true);
        setError("");
        const data = await getArticleById(articleId);
        setArticle(data);

        // Tải các bài viết liên quan cùng chuyên mục
        if (data.category?.id) {
          const res = await searchArticles({
            categoryId: data.category.id,
            size: 4,
          });
          // Lọc bỏ bài hiện tại
          setRelatedArticles(res.items.filter((item) => item.id !== articleId));
        } else {
          // Nếu không có category, lấy tin mới nhất
          const res = await searchArticles({ size: 4 });
          setRelatedArticles(res.items.filter((item) => item.id !== articleId));
        }
      } catch (err) {
        setError(
          err instanceof Error ? err.message : "Không thể tải bài viết.",
        );
      } finally {
        setLoading(false);
      }
    }

    loadDetail();
  }, [articleId]);

  const handleCopyLink = () => {
    navigator.clipboard.writeText(window.location.href);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  if (loading) {
    return (
      <main className="article-detail-page">
        <div className="detail-loading-box">
          <div className="spinner" />
          <p>Đang tải nội dung bài viết...</p>
        </div>
      </main>
    );
  }

  if (error || !article) {
    return (
      <main className="article-detail-page">
        <div className="detail-error-box">
          <h2>Không tìm thấy bài viết</h2>
          <p>{error || "Bài viết không tồn tại hoặc đã bị xóa."}</p>
          <button type="button" className="back-btn" onClick={onBack}>
            ← Quay lại danh sách tin
          </button>
        </div>
      </main>
    );
  }

  // Tính thời gian đọc ước lượng (trung bình 200 từ / phút)
  const plainText = article.content ? stripHtmlToText(article.content) : (article.description || "");
  const wordCount = plainText ? plainText.split(/\s+/).length : 0;
  const readTimeMinutes = Math.max(1, Math.ceil(wordCount / 200));

  // Kiểm tra danh mục cha và con
  const categoryName = article.category?.name;
  const parentCategoryName = article.category?.parent?.name;

  return (
    <main className="article-detail-page">
      {/* Thanh điều hướng quay lại & Breadcrumbs */}
      <div className="detail-top-bar">
        <button type="button" className="back-btn" onClick={onBack}>
          ← Quay lại danh sách
        </button>

        <nav className="detail-breadcrumbs" aria-label="Breadcrumb">
          <span className="crumb-link" onClick={onBack}>
            Trang chủ
          </span>
          {parentCategoryName && (
            <>
              <span className="crumb-sep">/</span>
              <span
                className="crumb-link"
                onClick={() => {
                  if (onSelectCategory && article.category?.parent?.id) {
                    onSelectCategory(String(article.category.parent.id));
                    onBack();
                  }
                }}
              >
                {parentCategoryName}
              </span>
            </>
          )}
          {categoryName && (
            <>
              <span className="crumb-sep">/</span>
              <span
                className="crumb-link current"
                onClick={() => {
                  if (onSelectCategory && article.category?.id) {
                    onSelectCategory(String(article.category.id));
                    onBack();
                  }
                }}
              >
                {categoryName}
              </span>
            </>
          )}
        </nav>
      </div>

      <div className="detail-layout">
        {/* Cột chính: Nội dung bài báo */}
        <article className="detail-article">
          {/* Header bài báo */}
          <header className="detail-header">
            <div className="detail-meta-tags">
              {article.source?.name && (
                <span className="detail-source-tag">
                  {article.source.name}
                </span>
              )}
              {categoryName && (
                <span className="detail-category-tag">
                  {categoryName}
                </span>
              )}
            </div>

            <h1 className="detail-title">{article.title}</h1>

            <div className="detail-info-bar">
              <div className="detail-info-left">
                {article.publishedAt && (
                  <time className="detail-time" dateTime={article.publishedAt}>
                    📅{" "}
                    {new Date(article.publishedAt).toLocaleDateString("vi-VN", {
                      weekday: "long",
                      year: "numeric",
                      month: "long",
                      day: "numeric",
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </time>
                )}
                <span className="detail-read-time">
                  ⏱ {readTimeMinutes} phút đọc
                </span>
              </div>

              <div className="detail-actions">
                <button
                  type="button"
                  className="action-btn"
                  onClick={handleCopyLink}
                  title="Sao chép liên kết"
                >
                  {copied ? "✓ Đã chép" : "🔗 Chia sẻ"}
                </button>
                {article.url && (
                  <a
                    href={article.url}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="action-btn original-link"
                    title="Mở bài viết gốc tại trang báo"
                  >
                    Nguồn gốc ↗
                  </a>
                )}
              </div>
            </div>
          </header>

          {/* Sapo / Tóm tắt mở đầu */}
          {article.description && (
            <div className="detail-sapo">
              <p>{stripHtmlToText(article.description)}</p>
            </div>
          )}

          {/* Nội dung bài viết chi tiết */}
          {article.content ? (
            <div
              className="article-content-body"
              dangerouslySetInnerHTML={{ __html: article.content }}
            />
          ) : (
            <div className="content-fallback-box">
              {article.imageUrl && (
                <img
                  src={article.imageUrl}
                  alt={article.title}
                  className="fallback-image"
                />
              )}
              <p className="fallback-note">
                Nội dung chi tiết của bài viết này được lưu dạng tóm tắt. Bạn có thể mở liên kết gốc để xem toàn bộ bài viết từ tòa soạn.
              </p>
              {article.url && (
                <a
                  href={article.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="open-original-btn"
                >
                  Xem toàn bộ bài viết tại {article.source?.name || "trang gốc"} ↗
                </a>
              )}
            </div>
          )}

          {/* Footer bài báo */}
          <footer className="detail-footer">
            <div className="detail-attribution">
              <span>Theo <strong>{article.source?.name || "Báo điện tử"}</strong></span>
              {article.url && (
                <a
                  href={article.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="source-ext-link"
                >
                  Xem link bài gốc ↗
                </a>
              )}
            </div>

            <div className="footer-actions">
              <button
                type="button"
                className="scroll-top-btn"
                onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
              >
                ↑ Lên đầu trang
              </button>
              <button type="button" className="back-btn-secondary" onClick={onBack}>
                ← Quay lại danh sách tin
              </button>
            </div>
          </footer>
        </article>

        {/* Cột bên: Tin tức liên quan / đọc thêm */}
        {relatedArticles.length > 0 && (
          <aside className="detail-sidebar">
            <h3 className="sidebar-title">Tin cùng chuyên mục</h3>
            <div className="related-list">
              {relatedArticles.map((item) => {
                const img = item.imageUrl ?? extractFirstImageUrl(item.description);
                return (
                  <div
                    key={item.id}
                    className="related-item"
                    onClick={() => onSelectArticle(item.id)}
                  >
                    {img && (
                      <img src={img} alt="" className="related-thumb" />
                    )}
                    <div className="related-info">
                      <span className="related-source">{item.source.name}</span>
                      <h4 className="related-title">{item.title}</h4>
                      {item.publishedAt && (
                        <time className="related-time">
                          {new Date(item.publishedAt).toLocaleDateString("vi-VN")}
                        </time>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          </aside>
        )}
      </div>
    </main>
  );
}
