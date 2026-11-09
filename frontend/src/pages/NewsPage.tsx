import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import {
  getArticleById, //chuyển hàm bên articles qua
  getCategories,
  getSources,
  searchArticles,
  type Category,
  type Source,
} from "../api/articles";
import { extractFirstImageUrl, stripHtmlToText } from "../lib/articleFormat";
import type { ArticleSearchResponse } from "../types/articles";

const PAGE_SIZE = 12;

export default function NewsPage() {
  const [inputKeyword, setInputKeyword] = useState("");
  const [keyword, setKeyword] = useState("");

  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>("");

  const [sources, setSources] = useState<Source[]>([]);
  const [selectedSource, setSelectedSource] = useState<string>("");

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

    getSources()
      .then(setSources)
      .catch((err) => console.error("Lỗi tải nguồn báo:", err));
  }, []);

  useEffect(() => {
    async function loadArticles() {
      try {
        setLoading(true);
        setError("");

        const result = await searchArticles({
          keyword,
          categoryId: selectedCategory ? Number(selectedCategory) : undefined,
          sourceId: selectedSource ? Number(selectedSource) : undefined,
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
  }, [keyword, selectedCategory, selectedSource, publishedFrom, publishedTo, page]);

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setKeyword(inputKeyword);
  }

  function handleResetFilters() {
    setInputKeyword("");
    setKeyword("");
    setSelectedCategory("");
    setSelectedSource("");
    setPublishedFrom("");
    setPublishedTo("");
    setPage(0);
  }
  // LẤY ID CỦA BÀI BÁO ĐI XIN LINK, RỒI BẢO TRÌNH DUYỆT BẬT TAB MỚI
    // Hàm xử lý: Khi bấm vào bài nào, lấy link gốc và mở sang tab mới
  async function handleOpenArticle(id: number) {
    try {
      // 1. Gọi hàm bên articles.ts để lấy chi tiết bài viết
      const detail = await getArticleById(id);

      // 2. Nếu có đường link gốc thì mở tab mới
      if (detail.url) {
        window.open(detail.url, "_blank", "noopener,noreferrer"); //lệnh mở web mới
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
        <p className="eyebrow">NEWS AI</p>
        <h1>Tin tức mới nhất</h1>
        <p>Các bài viết được thu thập từ nguồn RSS và lưu trong hệ thống.</p>

        <section className="filter-panel">
          <form className="search-form" onSubmit={handleSearch}>
            <input
              value={inputKeyword}
              onChange={(e) => setInputKeyword(e.target.value)}
              placeholder="Tìm kiếm từ khóa trong tiêu đề, mô tả, nội dung..."
              aria-label="Từ khóa tìm kiếm"
            />
            <button type="submit">Tìm kiếm</button>
          </form>

          <div className="filter-row">
            <div className="filter-item">
              <label htmlFor="cat-select">Chuyên mục:</label>
              <select
                id="cat-select"
                value={selectedCategory}
                onChange={(e) => {
                  setSelectedCategory(e.target.value);
                  setPage(0);
                }}
              >
                <option value="">-- Tất cả chuyên mục --</option>
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="filter-item">
              <label htmlFor="src-select">Nguồn báo:</label>
              <select
                id="src-select"
                value={selectedSource}
                onChange={(e) => {
                  setSelectedSource(e.target.value);
                  setPage(0);
                }}
              >
                <option value="">-- Tất cả nguồn báo --</option>
                {sources.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </div>

            <div className="filter-item">
              <label htmlFor="from-date">Từ ngày:</label>
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

            <div className="filter-item">
              <label htmlFor="to-date">Đến ngày:</label>
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

            {(keyword ||
              selectedCategory ||
              selectedSource ||
              publishedFrom ||
              publishedTo) && (
              <button
                type="button"
                className="reset-btn"
                onClick={handleResetFilters}
              >
                Đặt lại
              </button>
            )}
          </div>
        </section>
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
                      onClick={() => handleOpenArticle(article.id)} //KHI CLICK VÀO ĐÂY THÌ HÃY
                                                                    // CHẠY HÀM handleOpenArticle 
                                                                    // VỚI MÃ SỐ CỦA BÀI BÁO NÀY
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
