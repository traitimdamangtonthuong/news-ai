import { useState, useRef, useEffect } from 'react';
import { MessageSquare, X, Send, Loader2, Sparkles } from 'lucide-react';
import { chatClient } from '../api/client';
import type { ChatResponse } from '../types';

interface Message {
  role: 'user' | 'ai';
  content: string;
  citations?: ChatResponse['citations'];
}

export default function ChatbotWidget() {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<Message[]>([
    { role: 'ai', content: 'Chào bạn, tôi là trợ lý AI. Tôi có thể giúp bạn tổng hợp tin tức và tìm kiếm thông tin từ cơ sở dữ liệu.' }
  ]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const endOfMessagesRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (endOfMessagesRef.current) endOfMessagesRef.current.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = async (e?: React.FormEvent) => {
    e?.preventDefault();
    if (!input.trim() || isLoading) return;

    const userQuery = input.trim();
    setInput('');
    setMessages(prev => [...prev, { role: 'user', content: userQuery }]);
    setIsLoading(true);

    try {
      const { data } = await chatClient.post<ChatResponse>('/api/chat', { question: userQuery });
      setMessages(prev => [...prev, { role: 'ai', content: data.summary, citations: data.citations }]);
    } catch (error) {
      setMessages(prev => [...prev, { role: 'ai', content: 'Xin lỗi, hệ thống AI hiện không phản hồi. Vui lòng kiểm tra lại kết nối.' }]);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="fixed bottom-6 right-6 z-50 flex flex-col items-end">
      {isOpen && (
        <div className="bg-white w-[380px] h-[550px] mb-4 rounded-2xl shadow-2xl border border-slate-200 flex flex-col overflow-hidden animate-in slide-in-from-bottom-5">
          <div className="bg-brand-600 text-white p-4 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Sparkles size={20} />
              <h3 className="font-medium">Trợ lý Tin tức AI</h3>
            </div>
            <button onClick={() => setIsOpen(false)} className="hover:bg-brand-700 p-1 rounded-md transition-colors" aria-label="Đóng chat">
              <X size={20} />
            </button>
          </div>
          
          <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-slate-50">
            {messages.map((msg, idx) => (
              <div key={idx} className={`flex flex-col ${msg.role === 'user' ? 'items-end' : 'items-start'}`}>
                <div className={`max-w-[85%] p-3 rounded-2xl ${msg.role === 'user' ? 'bg-brand-600 text-white rounded-br-none' : 'bg-white border border-slate-200 text-slate-800 rounded-bl-none shadow-sm'}`}>
                  <p className="text-sm leading-relaxed whitespace-pre-wrap">{msg.content}</p>
                </div>
                {msg.citations && msg.citations.length > 0 && (
                  <div className="mt-2 flex flex-wrap gap-2">
                    {msg.citations.map((cite, i) => (
                      <a key={i} href={cite.url} target="_blank" rel="noopener noreferrer" className="text-[11px] bg-slate-200 text-slate-600 px-2 py-1 rounded hover:bg-slate-300 transition-colors">
                        [{cite.articleId}] {cite.sourceName}
                      </a>
                    ))}
                  </div>
                )}
              </div>
            ))}
            {isLoading && (
              <div className="flex items-center gap-2 text-slate-500 bg-white border border-slate-200 p-3 rounded-2xl rounded-bl-none w-fit shadow-sm">
                <Loader2 size={16} className="animate-spin" />
                <span className="text-sm">AI đang suy nghĩ...</span>
              </div>
            )}
            <div ref={endOfMessagesRef} />
          </div>

          <form onSubmit={sendMessage} className="p-3 bg-white border-t border-slate-200 flex gap-2">
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Hỏi AI về tin tức..."
              className="flex-1 bg-slate-100 border-transparent rounded-lg px-3 py-2 text-sm focus:bg-white focus:border-brand-500 focus:ring-2 focus:ring-brand-200 outline-none"
            />
            <button type="submit" disabled={isLoading || !input.trim()} className="bg-brand-600 text-white p-2 rounded-lg hover:bg-brand-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors" aria-label="Gửi tin nhắn">
              <Send size={18} />
            </button>
          </form>
        </div>
      )}
      
      {!isOpen && (
        <button onClick={() => setIsOpen(true)} className="bg-brand-600 hover:bg-brand-700 text-white p-4 rounded-full shadow-xl hover:shadow-2xl hover:-translate-y-1 transition-all flex items-center justify-center" aria-label="Mở chat AI">
          <MessageSquare size={24} />
        </button>
      )}
    </div>
  );
}