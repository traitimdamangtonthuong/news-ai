import type { FormEvent } from "react";
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  getCategories,
  searchArticles,
  type Category,
} from "../api/articles";
import Navbar from "../components/Navbar";
import { extractFirstImageUrl, stripHtmlToText } from "../lib/articleFormat";
import type { ArticleSearchResponse } from "../types/articles";

const PAGE_SIZE = 12;

type NewsPageProps = {
  onOpenArticle: (id: number) => void;
  onOpenChat: () => void;
};

export default function NewsPage({ onOpenArticle, onOpenChat }: NewsPageProps) {
  const [searchParams, setSearchParams] = useSearchParams();
  const urlKeyword = searchParams.get("keyword") ?? "";
  const urlCategory = searchParams.get("categoryId") ?? "";
  const urlPageRaw = searchParams.get("page");
  const urlPage = urlPageRaw ? Math.max(0, parseInt(urlPageRaw, 10) - 1) : 0;

  const [inputKeyword, setInputKeyword] = useState(urlKeyword);
  const [keyword, setKeyword] = useState(urlKeyword);
  const [categories, setCategories] = useState<Category[]>([]);
  const [selectedCategory, setSelectedCategory] = useState(urlCategory);
  const [publishedFrom, setPublishedFrom] = useState(searchParams.get("from") ?? "");
  const [publishedTo, setPublishedTo] = useState(searchParams.get("to") ?? "");
  const [page, setPage] = useState(urlPage);
  const [data, setData] = useState<ArticleSearchResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    getCategories()
      .then(setCategories)
      .catch((requestError: unknown) => {
        console.error("Lỗi tải danh mục:", requestError);
      });
  }, []);

  // Đồng bộ state khi URL thay đổi (ví dụ khi nhấn Back/Forward trên trình duyệt)
  useEffect(() => {
    setInputKeyword(urlKeyword);
    setKeyword(urlKeyword);
    setSelectedCategory(urlCategory);
    setPage(urlPage);
  }, [urlCategory, urlKeyword, urlPageRaw]);

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
      } catch (requestError) {
        setError(
          requestError instanceof Error
            ? requestError.message
            : "Đã có lỗi không xác định.",
        );
      } finally {
        setLoading(false);
      }
    }

    void loadArticles();
  }, [keyword, selectedCategory, publishedFrom, publishedTo, page]);

  function updateUrl(newParams: { keyword?: string; categoryId?: string; page?: number; from?: string; to?: string }) {
    const params = new URLSearchParams();
    const kw = newParams.keyword !== undefined ? newParams.keyword : keyword;
    const cat = newParams.categoryId !== undefined ? newParams.categoryId : selectedCategory;
    const p = newParams.page !== undefined ? newParams.page : page;
    const f = newParams.from !== undefined ? newParams.from : publishedFrom;
    const t = newParams.to !== undefined ? newParams.to : publishedTo;

    if (kw.trim()) params.set("keyword", kw.trim());
    if (cat) params.set("categoryId", cat);
    if (p > 0) params.set("page", String(p + 1));
    if (f) params.set("from", f);
    if (t) params.set("to", t);

    setSearchParams(params);
  }

  function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPage(0);
    setKeyword(inputKeyword);
    updateUrl({ keyword: inputKeyword, page: 0 });
  }

  function handleSelectCategory(categoryId: string) {
    setSelectedCategory(categoryId);
    setPage(0);
    updateUrl({ categoryId, page: 0 });
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

          <button className="open-chat-button" onClick={onOpenChat} type="button">
            <span aria-hidden="true">✦</span>
            Hỏi AI
          </button>

          <form className="search-form" onSubmit={handleSearch}>
            <input
              aria-label="Từ khóa tìm kiếm"
              onChange={(event) => setInputKeyword(event.target.value)}
              placeholder="Tìm kiếm từ khóa..."
              value={inputKeyword}
            />
            <button type="submit">Tìm kiếm</button>
          </form>
        </div>

        <Navbar
          categories={categories}
          onSelectCategory={handleSelectCategory}
          selectedCategory={selectedCategory}
        />

        <div className="sub-filter-row">
          <div className="date-filter-group">
            <span className="filter-label">Thời gian:</span>
            <div className="date-input-wrap">
              <label htmlFor="from-date">Từ</label>
              <input
                id="from-date"
                onChange={(event) => {
                  const val = event.target.value;
                  setPublishedFrom(val);
                  setPage(0);
                  updateUrl({ from: val, page: 0 });
                }}
                type="date"
                value={publishedFrom}
              />
            </div>
            <div className="date-input-wrap">
              <label htmlFor="to-date">Đến</label>
              <input
                id="to-date"
                onChange={(event) => {
                  const val = event.target.value;
                  setPublishedTo(val);
                  setPage(0);
                  updateUrl({ to: val, page: 0 });
                }}
                type="date"
                value={publishedTo}
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
        <p className="status">Không tìm thấy bài viết phù hợp với bộ lọc hiện tại.</p>
      )}

      {!loading && !error && data && data.items.length > 0 && (
        <>
          <p className="result-count">Tìm thấy {data.totalElements} bài viết.</p>

          <section className="article-grid">
            {data.items.map((article) => {
              const cardImage = article.imageUrl ?? extractFirstImageUrl(article.description);
              const lead = stripHtmlToText(article.description) ?? "Bài viết chưa có phần mô tả.";

              return (
                <article
                  className="article-card clickable-card"
                  key={article.id}
                  onClick={() => onOpenArticle(article.id)}
                >
                  {cardImage ? (
                    <img alt="" loading="lazy" src={cardImage} />
                  ) : (
                    <div className="image-placeholder">NEWS</div>
                  )}

                  <div className="article-body">
                    <div className="article-meta">
                      <span className="source-tag">{article.source.name}</span>
                      {article.category && (
                        <span className="category-tag">{article.category.name}</span>
                      )}
                    </div>

                    <h2 className="article-title">{article.title}</h2>
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

          <nav aria-label="Phân trang" className="pagination">
            <button
              disabled={page === 0}
              onClick={() => {
                const newPage = Math.max(0, page - 1);
                setPage(newPage);
                updateUrl({ page: newPage });
              }}
              type="button"
            >
              Trang trước
            </button>
            <span>Trang {data.page + 1} / {Math.max(1, data.totalPages)}</span>
            <button
              disabled={data.page + 1 >= data.totalPages}
              onClick={() => {
                const newPage = page + 1;
                setPage(newPage);
                updateUrl({ page: newPage });
              }}
              type="button"
            >
              Trang sau
            </button>
          </nav>
        </>
      )}
    </main>
  );
}
