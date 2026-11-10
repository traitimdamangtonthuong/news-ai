import axios from 'axios';

// Client gọi Spring Boot backend (đã được proxy qua Vite ở cổng 8080)
export const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

// Client gọi module AI Chatbot độc lập chạy ở cổng 8081
export const chatClient = axios.create({
  baseURL: 'http://localhost:8081',
  timeout: 30000,
});

export const NewsAPI = {
  searchArticles: async (params: { keyword?: string; categoryId?: number; page?: number; size?: number }) => {
    const { data } = await apiClient.get('/articles/search', { params });
    return data;
  },
  getArticle: async (id: number) => {
    const { data } = await apiClient.get(`/articles/${id}`);
    return data;
  },
  getCategories: async () => {
    const { data } = await apiClient.get('/categories');
    return data;
  }
};