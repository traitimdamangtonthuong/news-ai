export type Citation = {
  articleId: number;
  title: string;
  sourceName: string;
  url: string;
};

export type ChatAnswer = {
  summary: string;
  citations: Citation[];
};

export type ChatMessage = {
  id: string;
  role: "assistant" | "user";
  content: string;
  citations?: Citation[];
};
