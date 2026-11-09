export interface Category {
  id: number;
  name: string;
  slug: string;
}

export interface Source {
  id: number;
  name: string;
  baseUrl: string;
}

export interface Article {
  id: number;
  title: string;
  description: string;
  content?: string;
  imageUrl: string;
  publishedAt: string;
  url: string;
  source: Source;
  category: Category;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ChatCitation {
  articleId: number;
  title: string;
  sourceName: string;
  url: string;
}

export interface ChatResponse {
  summary: string;
  citations: ChatCitation[];
}