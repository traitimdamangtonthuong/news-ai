import type { FormEvent } from "react";
import { useEffect, useRef, useState } from "react";
import { askChatbot } from "../api/chatbot";
import type { Citation } from "../types/chatbot";

type ChatbotWidgetProps = {
  isOpen: boolean;
  onOpenChange: (isOpen: boolean) => void;
};

type ChatMessage = {
  id: string;
  role: "assistant" | "user";
  content: string;
  citations?: Citation[];
};

const WELCOME_MESSAGE: ChatMessage = {
  id: "welcome",
  role: "assistant",
  content: "Xin chào! Bạn muốn tìm hiểu điều gì trong các tin tức mới nhất?",
};

export default function ChatbotWidget({ isOpen, onOpenChange }: ChatbotWidgetProps) {
  const [messages, setMessages] = useState<ChatMessage[]>([WELCOME_MESSAGE]);
  const [question, setQuestion] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const messagesRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (isOpen) {
      inputRef.current?.focus();
    }
  }, [isOpen]);

  useEffect(() => {
    messagesRef.current?.scrollTo({
      top: messagesRef.current.scrollHeight,
      behavior: "smooth",
    });
  }, [isLoading, messages]);

  useEffect(() => {
    function closeOnEscape(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onOpenChange(false);
      }
    }

    window.addEventListener("keydown", closeOnEscape);
    return () => window.removeEventListener("keydown", closeOnEscape);
  }, [onOpenChange]);

  async function submitQuestion(rawQuestion: string) {
    const trimmedQuestion = rawQuestion.trim();
    if (!trimmedQuestion || isLoading) {
      return;
    }

    setMessages((current) => [
      ...current,
      { id: crypto.randomUUID(), role: "user", content: trimmedQuestion },
    ]);
    setQuestion("");
    setIsLoading(true);

    try {
      const match = window.location.pathname.match(/^\/article\/(\d+)/);
      const currentArticleId = match ? Number(match[1]) : null;
      
      const answer = await askChatbot(trimmedQuestion, currentArticleId);
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
      setIsLoading(false);
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void submitQuestion(question);
  }

  return (
    <aside className="chatbot-widget" aria-label="Trợ lý tin tức AI">
      <section
        aria-hidden={!isOpen}
        className={`chatbot-popup ${isOpen ? "is-open" : ""}`}
        id="chatbot-popup"
      >
        <header className="chatbot-header">
          <div className="chatbot-ai-mark" aria-hidden="true">AI</div>
          <div>
            <h2>Trợ lý Tin tức</h2>
            <p>Hỏi về các bài báo đã crawl</p>
          </div>
          <span className="chatbot-online-status">Trực tuyến</span>
        </header>

        <div className="chatbot-messages" aria-live="polite" ref={messagesRef}>
          {messages.map((message) => (
            <article className={`chatbot-message ${message.role}`} key={message.id}>
              <div className="chatbot-message-avatar" aria-hidden="true">
                {message.role === "assistant" ? "AI" : "Bạn"}
              </div>
              <div className="chatbot-message-body">
                <p>{message.content}</p>
                {message.citations && message.citations.length > 0 && (
                  <div className="chatbot-sources">
                    <span>Nguồn tham khảo</span>
                    {message.citations.map((citation) => (
                      <a
                        href={citation.url}
                        key={citation.articleId}
                        rel="noreferrer"
                        target="_blank"
                      >
                        {citation.sourceName} · {citation.title} ↗
                      </a>
                    ))}
                  </div>
                )}
              </div>
            </article>
          ))}

          {isLoading && (
            <article className="chatbot-message assistant">
              <div className="chatbot-message-avatar" aria-hidden="true">AI</div>
              <div className="chatbot-message-body chatbot-typing" aria-label="AI đang trả lời">
                <i /> <i /> <i />
              </div>
            </article>
          )}
        </div>

        <form className="chatbot-composer" onSubmit={handleSubmit}>
          <label className="sr-only" htmlFor="chatbot-question">Câu hỏi cho chatbot</label>
          <input
            autoComplete="off"
            disabled={isLoading}
            id="chatbot-question"
            onChange={(event) => setQuestion(event.target.value)}
            placeholder="Hỏi AI về tin tức..."
            ref={inputRef}
            value={question}
          />
          <button aria-label="Gửi câu hỏi" disabled={isLoading || !question.trim()} type="submit">↑</button>
        </form>
      </section>

      <button
        aria-controls="chatbot-popup"
        aria-expanded={isOpen}
        className="chatbot-toggle"
        onClick={() => onOpenChange(!isOpen)}
        type="button"
      >
        <span className="chatbot-toggle-icon" aria-hidden="true">{isOpen ? "×" : "✦"}</span>
        {!isOpen && <span>Hỏi AI</span>}
      </button>
    </aside>
  );
}
