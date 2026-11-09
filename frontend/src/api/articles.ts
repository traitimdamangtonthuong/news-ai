import type { ArticleSearchResponse } from "../types/articles";

export type Category = {
  id: number;
  name: string;
  slug?: string;
  parent?: {
    id: number;
    name: string;
    slug?: string;
  } | null;
};

export type Source = {
  id: number;
  name: string;
  baseUrl?: string;
  rssUrl?: string;
};

export type SearchParams = {
  keyword?: string;
  categoryId?: number;
  sourceId?: number;
  publishedFrom?: string;
  publishedTo?: string;
  page?: number;
  size?: number;
};

// 1. Lấy danh sách chuyên mục từ GET /api/categories
export async function getCategories(): Promise<Category[]> {
  const response = await fetch("/api/categories");
  if (!response.ok) {
    throw new Error("Không thể tải danh sách chuyên mục");
  }
  return response.json();
}

// 2. Lấy danh sách nguồn báo từ GET /api/sources
export async function getSources(): Promise<Source[]> {
  const response = await fetch("/api/sources");
  if (!response.ok) {
    throw new Error("Không thể tải danh sách nguồn báo");
  }
  return response.json();
}

// 3. Tìm kiếm bài viết kết hợp đa bộ lọc
export async function searchArticles(
  params: SearchParams,
): Promise<ArticleSearchResponse> {
  const query = new URLSearchParams();

  if (params.keyword?.trim()) {
    query.set("keyword", params.keyword.trim());
  }

  if (params.categoryId) {
    query.set("categoryId", String(params.categoryId));
  }

  if (params.sourceId) {
    query.set("sourceId", String(params.sourceId));
  }

  if (params.publishedFrom) {
    query.set("publishedFrom", params.publishedFrom);
  }

  if (params.publishedTo) {
    query.set("publishedTo", params.publishedTo);
  }

  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? 12));

  const response = await fetch(`/api/articles/search?${query.toString()}`);

  if (!response.ok) {
    throw new Error(`Không thể tải bài viết. HTTP ${response.status}`);
  }

  return response.json();
}
// Kiểu dữ liệu chi tiết của một bài viết (có chứa link url gốc)
export type ArticleDetail = {
  id: number;
  title: string;
  description: string | null;
  content: string | null;
  url: string;
  imageUrl: string | null;
  publishedAt: string | null;
};

// 4. Lấy chi tiết bài viết theo ID (để lấy link báo gốc url)
export async function getArticleById(id: number): Promise<ArticleDetail> {
  const response = await fetch(`/api/articles/${id}`);
  if (!response.ok) {
    throw new Error(`Không thể lấy chi tiết bài viết ID ${id}`);
  }
  return response.json();
}
