import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { searchArticles } from "../api/articles";
import { extractFirstImageUrl, stripHtmlToText } from "../lib/articleFormat";
import type { ArticleSearchResponse } from "../types/articles";

const PAGE_SIZE = 12;

export default function NewsPage() {
  const [inputKeyword, setInputKeyword] = useState("");
  const [keyword, setKeyword] = useState("");
  const [page, setPage] = useState(0);

  const [data, setData] = useState<ArticleSearchResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    async function loadArticles() {
      try {
        setLoading(true);
        setError("");

        const result = await searchArticles({
          keyword,
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
  }, [keyword, page]);

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setKeyword(inputKeyword);
  }

  return (
    <main className="news-page">
      <header className="news-header">
        <p className="eyebrow">NEWS AI</p>
        <h1>Tin tức mới nhất</h1>
        <p>
          Các bài viết được thu thập từ nguồn RSS và lưu trong hệ thống.
        </p>

        <form className="search-form" onSubmit={handleSearch}>
          <input
            value={inputKeyword}
            onChange={(event) => setInputKeyword(event.target.value)}
            placeholder="Tìm trong tiêu đề, mô tả và nội dung..."
            aria-label="Từ khóa tìm kiếm"
          />
          <button type="submit">Tìm kiếm</button>
        </form>
      </header>

      {loading && <p className="status">Đang tải bài viết...</p>}

      {!loading && error && (
        <p className="status error">
          {error}. Hãy kiểm tra Spring Boot có đang chạy ở cổng 8080 không.
        </p>
      )}

      {!loading && !error && data?.items.length === 0 && (
        <p className="status">Không tìm thấy bài viết phù hợp.</p>
      )}

      {!loading && !error && data && data.items.length > 0 && (
        <>
          <p className="result-count">
            Tìm thấy {data.totalElements} bài viết.
          </p>

          <section className="article-grid">
            {data.items.map((article) => {
              const cardImage =
                article.imageUrl ?? extractFirstImageUrl(article.description);
              const lead =
                stripHtmlToText(article.description) ?? "Bài viết chưa có phần mô tả.";

              return (
                <article className="article-card" key={article.id}>
                  {cardImage ? (
                    <img src={cardImage} alt="" />
                  ) : (
                    <div className="image-placeholder">NEWS</div>
                  )}

                  <div className="article-body">
                    <div className="article-meta">
                      <span>{article.category?.name ?? "Chưa phân loại"}</span>
                      <span>{article.source.name}</span>
                    </div>

                    <h2>{article.title}</h2>

                    <p>{lead}</p>

                    {article.publishedAt && (
                      <time dateTime={article.publishedAt}>
                        {new Date(article.publishedAt).toLocaleDateString("vi-VN")}
                      </time>
                    )}
                  </div>
                </article>
              );
            })}
          </section>

          <nav className="pagination" aria-label="Phân trang">
            <button
              disabled={data.page === 0}
              onClick={() => setPage((current) => current - 1)}
            >
              ← Trang trước
            </button>

            <span>
              Trang {data.page + 1} / {Math.max(data.totalPages, 1)}
            </span>

            <button
              disabled={data.page + 1 >= data.totalPages}
              onClick={() => setPage((current) => current + 1)}
            >
              Trang sau →
            </button>
          </nav>
        </>
      )}
    </main>
  );
}