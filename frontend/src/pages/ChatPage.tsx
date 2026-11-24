import type { FormEvent } from "react";
import { useRef, useState } from "react";
import { askChatbot } from "../api/chatbot";
import type { ChatMessage } from "../types/chatbot";

type ChatPageProps = {
  onOpenNews: () => void;
};

const SUGGESTIONS = [
  "Tin mới nhất về AI là gì?",
  "Tóm tắt các tin tức về kinh tế hôm nay",
  "Có bài báo nào về công nghệ không?",
];

const WELCOME_MESSAGE: ChatMessage = {
  id: "welcome",
  role: "assistant",
  content:
    "Xin chào! Mình có thể tổng hợp các bài báo đã được News AI thu thập. Hãy đặt câu hỏi về một chủ đề bạn quan tâm.",
};

export default function ChatPage({ onOpenNews }: ChatPageProps) {
  const [messages, setMessages] = useState<ChatMessage[]>([WELCOME_MESSAGE]);
  const [question, setQuestion] = useState("");
  const [isSending, setIsSending] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  async function submitQuestion(rawQuestion: string) {
    const trimmedQuestion = rawQuestion.trim();
    if (!trimmedQuestion || isSending) {
      return;
    }

    const userMessage: ChatMessage = {
      id: crypto.randomUUID(),
      role: "user",
      content: trimmedQuestion,
    };
    setMessages((current) => [...current, userMessage]);
    setQuestion("");
    setIsSending(true);

    try {
      const answer = await askChatbot(trimmedQuestion);
      setMessages((current) => [
        ...current,
        {
          id: crypto.randomUUID(),
          role: "assistant",
          content: answer.summary,
          citations: answer.citations ?? [],
        },
      ]);
    } catch (error) {
      setMessages((current) => [
        ...current,
        {
          id: crypto.randomUUID(),
          role: "assistant",
          content:
            error instanceof Error
              ? `${error.message}. Hãy kiểm tra ai-chatbot đang chạy ở cổng 8081.`
              : "Không thể kết nối với chatbot. Hãy thử lại sau.",
        },
      ]);
    } finally {
      setIsSending(false);
      inputRef.current?.focus();
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void submitQuestion(question);
  }

  return (
    <main className="chat-page">
      <header className="chat-header">
        <button className="brand-button" type="button" onClick={onOpenNews}>
          <span>NEWS AI</span>
          <strong>Trang tin</strong>
        </button>
        <div className="chat-heading">
          <p className="eyebrow">TRỢ LÝ TIN TỨC</p>
          <h1>Hỏi AI về tin tức</h1>
          <p>
            Câu trả lời được tổng hợp từ các bài báo đã crawl và luôn kèm nguồn tham khảo.
          </p>
        </div>
        <span className="chat-status"><i /> Đang kết nối</span>
      </header>

      <section className="chat-shell" aria-label="Hội thoại với trợ lý tin tức">
        <div className="chat-messages" aria-live="polite">
          {messages.map((message) => (
            <article className={`chat-message ${message.role}`} key={message.id}>
              <div className="message-avatar" aria-hidden="true">
                {message.role === "assistant" ? "AI" : "Bạn"}
              </div>
              <div className="message-content">
                <p>{message.content}</p>
                {message.citations && message.citations.length > 0 && (
                  <div className="citations" aria-label="Nguồn tham khảo">
                    <span>Nguồn tham khảo</span>
                    {message.citations.map((citation) => (
                      <a
                        href={citation.url}
                        key={citation.articleId}
                        rel="noreferrer"
                        target="_blank"
                      >
                        <strong>{citation.sourceName}</strong>
                        {citation.title}
                        <b aria-hidden="true">↗</b>
                      </a>
                    ))}
                  </div>
                )}
              </div>
            </article>
          ))}
          {isSending && (
            <article className="chat-message assistant" aria-label="Chatbot đang phản hồi">
              <div className="message-avatar" aria-hidden="true">AI</div>
              <div className="message-content typing-indicator">
                <span /> <span /> <span />
              </div>
            </article>
          )}
        </div>

        {messages.length === 1 && (
          <div className="chat-suggestions">
            <span>Gợi ý câu hỏi</span>
            <div>
              {SUGGESTIONS.map((suggestion) => (
                <button
                  key={suggestion}
                  onClick={() => void submitQuestion(suggestion)}
                  type="button"
                >
                  {suggestion}
                </button>
              ))}
            </div>
          </div>
        )}

        <form className="chat-composer" onSubmit={handleSubmit}>
          <label className="sr-only" htmlFor="chat-question">Câu hỏi cho trợ lý</label>
          <input
            autoComplete="off"
            disabled={isSending}
            id="chat-question"
            onChange={(event) => setQuestion(event.target.value)}
            placeholder="Ví dụ: Tin tức mới nhất về AI là gì?"
            ref={inputRef}
            value={question}
          />
          <button disabled={!question.trim() || isSending} type="submit">
            {isSending ? "Đang gửi" : "Gửi"}
            <span aria-hidden="true">↑</span>
          </button>
        </form>
      </section>
    </main>
  );
}
