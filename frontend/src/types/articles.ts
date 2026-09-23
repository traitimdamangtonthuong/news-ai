export type ArticleReference = {
  id: number;
  name: string;
};

export type ArticleSearchItem = {
  id: number;
  title: string;
  description: string | null;
  imageUrl: string | null;
  publishedAt: string | null;
  source: ArticleReference;
  category: ArticleReference | null;
};

export type ArticleSearchResponse = {
  items: ArticleSearchItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};