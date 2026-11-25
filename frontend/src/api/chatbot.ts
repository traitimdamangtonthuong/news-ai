import type { ChatAnswer } from "../types/chatbot";

// In local development, Vite proxies this path to ai-chatbot on port 8081.
// Set VITE_CHATBOT_API_URL to the deployed chatbot endpoint in production.
const chatbotApiUrl =
  import.meta.env.VITE_CHATBOT_API_URL ?? "/chatbot-api/chat";

export async function askChatbot(
  question: string,
  articleId?: number | null
): Promise<ChatAnswer> {
  const response = await fetch(chatbotApiUrl, {
    method: "POST",
    headers: { "Content-Type": "application/json; charset=utf-8" },
    body: JSON.stringify({
      question,
      articleId: articleId ?? null,
    }),
  });

  if (!response.ok) {
    throw new Error(`Chatbot không phản hồi. HTTP ${response.status}`);
  }

  const answer = (await response.json()) as ChatAnswer;
  if (!answer.summary) {
    throw new Error("Chatbot trả về phản hồi không hợp lệ.");
  }
  return answer;
}