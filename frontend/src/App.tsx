import { useEffect, useState } from "react";
import ArticleDetailPage from "./pages/ArticleDetailPage";
import NewsPage from "./pages/NewsPage";

export default function App() {
  const [currentArticleId, setCurrentArticleId] = useState<number | null>(() => {
    // Đọc articleId từ URL query param nếu người dùng mở trực tiếp link ?articleId=...
    const params = new URLSearchParams(window.location.search);
    const id = params.get("articleId");
    return id ? Number(id) : null;
  });

  const [initialCategory, setInitialCategory] = useState<string>("");

  // Đồng bộ với nút Back/Forward của trình duyệt
  useEffect(() => {
    function handlePopState() {
      const params = new URLSearchParams(window.location.search);
      const id = params.get("articleId");
      setCurrentArticleId(id ? Number(id) : null);
    }

    window.addEventListener("popstate", handlePopState);
    return () => window.removeEventListener("popstate", handlePopState);
  }, []);

  // Mở trang chi tiết bài viết
  const handleSelectArticle = (id: number) => {
    setCurrentArticleId(id);
    const url = new URL(window.location.href);
    url.searchParams.set("articleId", String(id));
    window.history.pushState({}, "", url.toString());
  };

  // Quay lại danh sách bài viết
  const handleBackToList = () => {
    setCurrentArticleId(null);
    const url = new URL(window.location.href);
    url.searchParams.delete("articleId");
    window.history.pushState({}, "", url.toString());
  };

  // Chọn danh mục từ bài viết rồi quay lại danh sách
  const handleSelectCategory = (catId: string) => {
    setInitialCategory(catId);
    handleBackToList();
  };

  if (currentArticleId) {
    return (
      <ArticleDetailPage
        articleId={currentArticleId}
        onBack={handleBackToList}
        onSelectArticle={handleSelectArticle}
        onSelectCategory={handleSelectCategory}
      />
    );
  }

  return (
    <NewsPage
      initialCategory={initialCategory}
      onOpenArticle={handleSelectArticle}
    />
  );
}
