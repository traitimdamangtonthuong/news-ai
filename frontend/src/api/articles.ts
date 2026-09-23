import type { ArticleSearchResponse } from "../types/article";

type SearchParams = {
  keyword?: string;
  page?: number;
  size?: number;
};

export async function searchArticles(
  params: SearchParams,
): Promise<ArticleSearchResponse> {
  const query = new URLSearchParams();

  if (params.keyword?.trim()) {
    query.set("keyword", params.keyword.trim());
  }

  query.set("page", String(params.page ?? 0));
  query.set("size", String(params.size ?? 12));

  const response = await fetch(`/api/articles/search?${query.toString()}`);

  if (!response.ok) {
    throw new Error(`Không thể tải bài viết. HTTP ${response.status}`);
  }

  return response.json();
}