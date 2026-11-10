import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import NewsPage from "./pages/NewsPage";
import ArticleDetail from "./pages/ArticleDetail";

export default function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<NewsPage />} />
        <Route path="/article/:id" element={<ArticleDetail />} />
      </Routes>
    </Router>
  );
}