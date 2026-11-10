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

// 1. Láº¥y danh sÃ¡ch chuyÃªn má»¥c tá»« GET /api/categories
export async function getCategories(): Promise<Category[]> {
  const response = await fetch("/api/categories");
  if (!response.ok) {
    throw new Error("KhÃ´ng thá»ƒ táº£i danh sÃ¡ch chuyÃªn má»¥c");
  }
  return response.json();
}

// 2. Láº¥y danh sÃ¡ch nguá»“n bÃ¡o tá»« GET /api/sources
export async function getSources(): Promise<Source[]> {
  const response = await fetch("/api/sources");
  if (!response.ok) {
    throw new Error("KhÃ´ng thá»ƒ táº£i danh sÃ¡ch nguá»“n bÃ¡o");
  }
  return response.json();
}

// 3. TÃ¬m kiáº¿m bÃ i viáº¿t káº¿t há»£p Ä‘a bá»™ lá»c
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
    throw new Error(`KhÃ´ng thá»ƒ táº£i bÃ i viáº¿t. HTTP ${response.status}`);
  }

  return response.json();
}
// Kiểu dữ liệu chi tiết của bài viết đầy đủ content, media, nguồn và danh mục
export type ArticleDetail = {
  id: number;
  title: string;
  description: string | null;
  content: string | null;
  url: string;
  imageUrl: string | null;
  publishedAt: string | null;
  source?: {
    id: number;
    name: string;
    baseUrl?: string;
  };
  category?: {
    id: number;
    name: string;
    slug?: string;
    parent?: {
      id: number;
      name: string;
      slug?: string;
    } | null;
  } | null;
  subCategory?: {
    id: number;
    name: string;
    slug?: string;
  } | null;
};

// 4. Láº¥y chi tiáº¿t bÃ i viáº¿t theo ID (Ä‘á»ƒ láº¥y link bÃ¡o gá»‘c url)
export async function getArticleById(id: number): Promise<ArticleDetail> {
  const response = await fetch(`/api/articles/${id}`);
  if (!response.ok) {
    throw new Error(`KhÃ´ng thá»ƒ láº¥y chi tiáº¿t bÃ i viáº¿t ID ${id}`);
  }
  return response.json();
}