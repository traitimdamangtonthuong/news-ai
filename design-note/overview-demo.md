
**# Hệ thống tin tức tự động - News Aggregator

Hệ thống tự động thu thập tin từ nhiều nguồn báo chí, chuẩn hóa dữ liệu, phân loại, tìm kiếm và cho phép người dùng hỏi AI về những bài báo đã thu thập.

## Công nghệ:

## Flow:

Các trang báo

      ↓

Crawler / RSS

      ↓

Article Parser

      ↓

Cleaning + Deduplication

      ↓

PostgreSQL

      ↓

Search / Vector Search

      ↓

       **	** Java Backend

       ↙           **	**	 ↘

News Reader        **	**AI Chatbot**	**

## Mục tiêu hệ thống:

| Chức năng    | Mô tả                                             |
| -------------- | --------------------------------------------------- |
| Thu thập báo | Lấy bài viết từ nhiều nguồn                   |
| Chuẩn hóa    | Chuyển dữ liệu các báo về cùng format        |
| Phân loại    | Công nghệ, kinh tế, thể thao, giáo dục...     |
| Tìm kiếm     | Search theo từ khóa, ngày, nguồn                |
| AI Search      | Hỏi câu hỏi bằng ngôn ngữ tự nhiên          |
| Tổng hợp     | AI tổng hợp nhiều bài báo liên quan           |
| So sánh       | So sánh cách các báo đưa cùng một sự kiện |
| Cá nhân hóa | Gợi ý tin theo lịch sử đọc                    |
| Quản trị     | Theo dõi crawler, nguồn báo và dữ liệu        |

## Nguồn tham khảo:

https://github.com/Emesall/spring-news-app.git

**
