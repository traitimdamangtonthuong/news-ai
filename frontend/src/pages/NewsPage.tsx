import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import {
  getArticleById,
  getCategories,
  searchArticles,
  type Category,
} from "../api/articles";
import Navbar from "../components/Navbar";
import { extractFirstImageUrl, stripHtmlToText } from "../lib/articleFormat";
import type { ArticleSearchResponse } from "../types/articles";

const PAGE_SIZE = 12;

export default function NewsPage() {
  const [inputKeyword, setInputKeyword] = useState("");
  const [keyword, setKeyword] = useState("");

  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>("");

  const [publishedFrom, setPublishedFrom] = useState<string>("");
  const [publishedTo, setPublishedTo] = useState<string>("");

  const [page, setPage] = useState(0);
  const [data, setData] = useState<ArticleSearchResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    getCategories()
      .then(setCategories)
      .catch((err) => console.error("Lỗi tải danh mục:", err));
  }, []);

  useEffect(() => {
    async function loadArticles() {
      try {
        setLoading(true);
        setError("");

        const result = await searchArticles({
          keyword,
          categoryId: selectedCategory ? Number(selectedCategory) : undefined,
          publishedFrom: publishedFrom || undefined,
          publishedTo: publishedTo || undefined,
          page,
          size: PAGE_SIZE,
        });

        setData(result);
      } catch (err) {
        setError(
          err instanceof Error ? err.message : "Đã có lỗi không xác định.",
        );
      } finally {
        setLoading(false);
      }
    }

    loadArticles();
  }, [keyword, selectedCategory, publishedFrom, publishedTo, page]);

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setKeyword(inputKeyword);
  }

  function handleSelectCategory(catId: string) {
    setSelectedCategory(catId);
    setPage(0);
  }

  // Khi click vào tiêu đề bài viết, lấy link gốc và mở tab mới
  async function handleOpenArticle(id: number) {
    try {
      const detail = await getArticleById(id);
      if (detail.url) {
        window.open(detail.url, "_blank", "noopener,noreferrer");
      } else {
        alert("Bài viết này không có liên kết gốc!");
      }
    } catch (err) {
      console.error("Lỗi khi mở bài báo:", err);
      alert("Không thể tải bài viết, vui lòng kiểm tra lại Backend.");
    }
  }

  return (
    <main className="news-page">
      <header className="news-header">
        <div className="header-top">
          <div className="brand-section">
            <p className="eyebrow">NEWS AI</p>
            <h1>Tin tức mới nhất</h1>
            <p className="subtitle">
              Tổng hợp tin tức tự động và đa chiều từ các nguồn báo uy tín.
            </p>
          </div>

          <form className="search-form" onSubmit={handleSearch}>
            <input
              value={inputKeyword}
              onChange={(e) => setInputKeyword(e.target.value)}
              placeholder="Tìm kiếm từ khóa..."
              aria-label="Từ khóa tìm kiếm"
            />
            <button type="submit">Tìm kiếm</button>
          </form>
        </div>

        {/* Navigation Bar thay thế hệ thống lọc nguồn báo cũ, hỗ trợ hover subcategory */}
        <Navbar
          categories={categories}
          selectedCategory={selectedCategory}
          onSelectCategory={handleSelectCategory}
        />

        {/* Thanh lọc phụ: Thời gian */}
        <div className="sub-filter-row">
          <div className="date-filter-group">
            <span className="filter-label">Thời gian:</span>
            <div className="date-input-wrap">
              <label htmlFor="from-date">Từ</label>
              <input
                id="from-date"
                type="date"
                value={publishedFrom}
                onChange={(e) => {
                  setPublishedFrom(e.target.value);
                  setPage(0);
                }}
              />
            </div>
            <div className="date-input-wrap">
              <label htmlFor="to-date">Đến</label>
              <input
                id="to-date"
                type="date"
                value={publishedTo}
                onChange={(e) => {
                  setPublishedTo(e.target.value);
                  setPage(0);
                }}
              />
            </div>
          </div>
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
                <article className="article-card" key={article.id}>
                  {cardImage ? (
                    <img src={cardImage} alt="" />
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
                      onClick={() => handleOpenArticle(article.id)}
                      title="Bấm để đọc bài báo gốc"
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
                </article>
              );
            })}
          </section>

          <nav className="pagination" aria-label="Phân trang">
            <button
              type="button"
              disabled={page === 0}
              onClick={() => setPage((current) => Math.max(0, current - 1))}
            >
              Trang trước
            </button>
            <span>
              Trang {data.page + 1} / {Math.max(1, data.totalPages)}
            </span>
            <button
              type="button"
              disabled={data.page + 1 >= data.totalPages}
              onClick={() => setPage((current) => current + 1)}
            >
              Trang sau
            </button>
          </nav>
        </>
      )}
    </main>
  );
}
