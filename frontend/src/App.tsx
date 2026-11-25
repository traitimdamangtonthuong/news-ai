import { useState } from "react";
import { BrowserRouter, Navigate, Route, Routes, useNavigate } from "react-router-dom";
import ChatbotWidget from "./components/ChatbotWidget";
import NewsPage from "./pages/NewsPage";
import ArticleDetail from "./pages/ArticleDetail";

function AppRoutes() {
  const navigate = useNavigate();
  const [isChatOpen, setIsChatOpen] = useState(false);

  return (
    <>
      <Routes>
        <Route
          path="/"
          element={
            <NewsPage
              onOpenArticle={(id) => navigate(`/article/${id}`)}
              onOpenChat={() => setIsChatOpen(true)}
            />
          }
        />
        <Route path="/article/:id" element={<ArticleDetail />} />
        <Route path="/chat" element={<Navigate replace to="/" />} />
      </Routes>
      <ChatbotWidget isOpen={isChatOpen} onOpenChange={setIsChatOpen} />
    </>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  );
}
