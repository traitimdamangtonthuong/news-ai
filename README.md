# News AI

News AI là dự án tổng hợp tin tức phục vụ bài tập lớn. Hệ thống thu thập bài viết từ RSS/Atom feed, chuẩn hóa và lưu dữ liệu vào PostgreSQL, cung cấp REST API cho giao diện đọc tin và tạo nền dữ liệu cho chatbot/RAG trong giai đoạn sau.

> Trạng thái hiện tại: backend, database, RSS crawler, article search và News page React cơ bản đã có. AI chatbot/RAG chưa được triển khai.

## Mục lục

- [Tổng quan](#tổng-quan)
- [Kiến trúc và phạm vi hiện tại](#kiến-trúc-và-phạm-vi-hiện-tại)
- [Yêu cầu phần mềm](#yêu-cầu-phần-mềm)
- [Bắt đầu sau khi pull code](#bắt-đầu-sau-khi-pull-code)
- [Chạy backend và database](#chạy-backend-và-database)
- [Import dữ liệu seed vào PostgreSQL local](#import-dữ-liệu-seed-vào-postgresql-local)
- [Chạy frontend](#chạy-frontend)
- [API và dữ liệu crawl](#api-và-dữ-liệu-crawl)
- [Kiểm thử](#kiểm-thử)
- [Cấu trúc mã nguồn](#cấu-trúc-mã-nguồn)
- [Hướng dẫn theo vai trò](#hướng-dẫn-theo-vai-trò)
- [Quy ước làm việc nhóm](#quy-ước-làm-việc-nhóm)

## Tổng quan

| Thành phần | Công nghệ | Trách nhiệm |
| --- | --- | --- |
| Backend | Java 25, Spring Boot 4, Spring Data JPA, Hibernate | REST API, nghiệp vụ, validation, crawler, xử lý lỗi |
| Database | PostgreSQL | Lưu `articles`, `sources`, `categories` |
| Frontend | React, TypeScript, Vite | News page, search, phân trang, article detail trong tương lai |
| Crawler | ROME (RSS 2.0/Atom) | Tải feed, parse, chuẩn hóa URL, upsert article |
| AI chatbot | Chưa triển khai | Sẽ dùng dữ liệu article đã crawl để retrieval/RAG |

Mục tiêu trước mắt không phải crawl càng nhiều càng tốt, mà là đảm bảo dữ liệu crawl về có cấu trúc, truy vấn được, hiển thị được và sẵn sàng làm ngữ cảnh cho chatbot sau này.

## Kiến trúc và phạm vi hiện tại

```text
React / Postman
      │
      ▼
Spring Boot REST API
      │
Controller → Service → Repository → JPA/Hibernate → PostgreSQL
      ▲
      │
RSS/Atom → RssFeedFetcher → FeedItemMapper → RssCrawlerService
```

Các chức năng đã có:

- CRUD `Article`, `Source`, `Category`.
- Crawl thủ công RSS/Atom cho một source hoặc toàn bộ source có `rssUrl`.
- Chống trùng bằng `canonicalUrl`; URL gốc của nhà xuất bản vẫn được giữ riêng.
- Upsert crawler với trạng thái `created`, `updated`, `unchanged`, `failed`.
- Search phân trang theo keyword, source, category và khoảng ngày.
- JSON error thống nhất cho lỗi `400`, `404`, `409`, `500`.
- React News page cơ bản dùng endpoint search.

Chưa có: HTML scraping, xác thực/JWT, semantic search, embeddings, pgvector, LLM, RAG chatbot, admin dashboard.

## Yêu cầu phần mềm

### Chung cho mọi thành viên

- Git.
- IDE: IntelliJ IDEA Community/Ultimate hoặc VS Code.
- PostgreSQL và pgAdmin 4 (hoặc `psql`).
- JDK 25, vì `pom.xml` khai báo Java 25.

Kiểm tra nhanh:

```powershell
git --version
java -version
```

### Thành viên Backend / Crawler

- JDK 25.
- PostgreSQL chạy local.
- Không cần cài Maven riêng: dự án đã có Maven Wrapper.
- Postman hoặc Bruno/Insomnia để gọi API.

### Thành viên UI/UX / Frontend

- Node.js LTS và npm.
- VS Code khuyến nghị, extension: ESLint/Oxlint, Prettier, React/TypeScript snippets (tùy chọn).
- Trình duyệt Chrome/Edge và DevTools.

Kiểm tra:

```powershell
node --version
npm --version
```

Frontend chỉ gọi API qua `/api`; Vite proxy sẽ chuyển sang backend `http://localhost:8080` khi phát triển local.

### Thành viên AI Chatbot

Chatbot **không bắt buộc dùng Python**. Vì backend hiện là Spring Boot, hướng mặc định và đơn giản nhất cho dự án là phát triển chatbot trong Java, đặt trong package `ai/` hoặc `chat/` và gọi dữ liệu từ `ArticleService`/repository.

Yêu cầu tối thiểu cho hướng Java là JDK 25, PostgreSQL và Postman/Bruno để kiểm tra API. Khi bắt đầu tích hợp LLM, nhóm chỉ cần thêm thư viện Java phù hợp (ví dụ Spring AI) sau khi đã thống nhất model/provider.

Python 3.11+, `venv`/Conda và Jupyter chỉ là **tùy chọn** cho thành viên muốn thử nghiệm độc lập embedding, NLP hoặc retrieval trong notebook. Nếu chọn Python service riêng, cần thống nhất contract HTTP với backend và chấp nhận vận hành thêm một service. Không tự thêm API key vào `application.properties`, source code hoặc Git.

## Bắt đầu sau khi pull code

### 1. Clone hoặc pull nhánh mới nhất

Clone lần đầu:

```powershell
git clone <REPOSITORY_URL>
cd news-ai
```

Nếu đã có repository:

```powershell
git status
git pull
```

Nếu `git status` hiển thị file đang sửa, commit hoặc stash chúng trước khi pull để không ghi đè công việc cá nhân.

### 2. Kiểm tra cấu trúc dự án

```text
news-ai/
├── src/                         # Spring Boot backend
├── frontend/                    # React + TypeScript + Vite
├── database/manual-migrations/  # SQL chạy thủ công khi nâng cấp schema cũ
├── postman/                     # API collection/environment
├── design-note/overview-current.md
├── pom.xml
├── mvnw.cmd
└── README.md
```

### 3. Cài dependency cho đúng phần việc

Backend: Maven Wrapper sẽ tự tải dependency khi chạy/test lần đầu.

Frontend:

```powershell
cd D:\PRJ\news-ai\frontend
npm install
```

> Không chạy `npm install` hoặc `npm run dev` ở `D:\PRJ\news-ai`: thư mục gốc là Maven backend, không có `package.json`. Lỗi `ENOENT ... news-ai\package.json` nghĩa là đang đứng sai thư mục.

## Chạy backend và database

### 1. Tạo database

Trong pgAdmin Query Tool hoặc `psql`, tạo database nếu chưa có:

```sql
CREATE DATABASE news_ai;
```

Cấu hình hiện tại nằm ở `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5728/news_ai
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
```

Repository mặc định dùng PostgreSQL cổng **5728**, không phải cổng mặc định 5432. Nếu máy bạn chạy PostgreSQL ở 5432, đổi port trong cấu hình local cho phù hợp và không commit thay đổi cấu hình cá nhân nếu nhóm chưa thống nhất.

### 2. Cấp mật khẩu bằng biến môi trường

Trong PowerShell chạy backend:

```powershell
$env:DB_PASSWORD="YOUR_POSTGRES_PASSWORD"
```

Biến này chỉ có hiệu lực cho cửa sổ PowerShell hiện tại. Không ghi mật khẩu trực tiếp vào Git.

### 3. Chạy Spring Boot

Tại thư mục gốc:

```powershell
cd D:\PRJ\news-ai
.\mvnw.cmd spring-boot:run
```

Lần đầu Hibernate tự tạo/cập nhật bảng theo entity (`ddl-auto=update`). Kiểm tra backend bằng browser hoặc Postman:

```text
GET http://localhost:8080/api/status
```

### 4. Database cũ có `content` kiểu `bytea`

Entity hiện quy định `articles.content` là `TEXT`. Nếu search keyword báo `function lower(bytea) does not exist`, kiểm tra schema:

```sql
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'articles' AND column_name = 'content';
```

Chỉ khi kết quả là `bytea`, chạy một lần file [001-articles-content-bytea-to-text.sql](database/manual-migrations/001-articles-content-bytea-to-text.sql) trong pgAdmin, rồi restart backend. Không chạy migration này trên cột đã là `text`.

## Import dữ liệu seed vào PostgreSQL local

Mục này dành cho thành viên muốn chạy backend với cùng dataset đã crawl của nhóm. File seed là dữ liệu mẫu được commit trong Git tại:

```text
database/seeds/news_ai_seed.sql
```

Nếu file này chưa xuất hiện sau khi pull, nghĩa là nhóm chưa commit dataset seed; hãy dùng dữ liệu local của riêng bạn hoặc liên hệ người phụ trách crawler để bổ sung file.

### Điều kiện trước khi import

Bạn cần có:

- PostgreSQL và pgAdmin đang chạy.
- Database `news_ai` trống.
- Backend đã chạy thành công **một lần** để Hibernate tạo các bảng.
- File `database/seeds/news_ai_seed.sql` đã có trong working tree.

Không import seed vào database đang có dữ liệu làm việc riêng, vì source/category/article có unique constraint và có thể phát sinh lỗi trùng dữ liệu. Cách an toàn nhất là tạo một database local mới, chỉ dùng cho project.

### Bước 1: Mở PostgreSQL trong pgAdmin

1. Mở **pgAdmin 4**.
2. Trong thanh trái, mở `Servers` rồi chọn PostgreSQL server local của bạn.
3. Nếu pgAdmin hỏi mật khẩu, dùng mật khẩu user PostgreSQL đã tạo khi cài đặt (thường là user `postgres`).
4. Kiểm tra server đang chạy và xem đúng port. Project mặc định dùng port `5728`; nếu PostgreSQL của bạn dùng `5432`, cấu hình backend phải dùng đúng port đó.

### Bước 2: Tạo database trống

Trong pgAdmin, chuột phải **Databases** → **Create** → **Database...**:

- Database: `news_ai`
- Owner: `postgres` (hoặc user PostgreSQL local của bạn)

Hoặc mở Query Tool tại database `postgres` và chạy:

```sql
CREATE DATABASE news_ai;
```

Sau đó refresh mục `Databases`; phải thấy database `news_ai`.

### Bước 3: Để Spring Boot tạo schema

Mở PowerShell tại root project và đặt mật khẩu database:

```powershell
cd D:\PRJ\news-ai
$env:DB_PASSWORD="YOUR_POSTGRES_PASSWORD"
.\mvnw.cmd spring-boot:run
```

Chờ log có thông báo ứng dụng khởi động thành công. `spring.jpa.hibernate.ddl-auto=update` sẽ tạo các bảng cần thiết như `articles`, `sources`, `categories`.

Kiểm tra nhanh:

```text
http://localhost:8080/api/status
```

Sau đó dừng backend bằng `Ctrl+C`. Dừng app trước khi import giúp tránh nhầm lẫn khi kiểm tra dữ liệu; không bắt buộc về mặt kỹ thuật.

### Bước 4: Mở file seed trong pgAdmin

1. Trong pgAdmin, chọn database `news_ai`.
2. Chuột phải → **Query Tool**.
3. Trong cửa sổ Query Tool, bấm biểu tượng thư mục **Open File** hoặc nhấn `Ctrl + O`.
4. Chọn file:

```text
D:\PRJ\news-ai\database\seeds\news_ai_seed.sql
```

5. Xác nhận nội dung là các lệnh `INSERT INTO`, không phải file backup nhị phân.
6. Bấm **Execute** hoặc nhấn `F5`.

> Double-click file `.sql` trong File Explorer chỉ mở file bằng editor; nó không import dữ liệu vào PostgreSQL. File phải được chạy trong Query Tool hoặc bằng `psql`.

### Bước 5: Xác minh import

Trong Query Tool, chạy:

```sql
SELECT COUNT(*) AS sources FROM sources;
SELECT COUNT(*) AS categories FROM categories;
SELECT COUNT(*) AS articles FROM articles;

SELECT id, title, published_at
FROM articles
ORDER BY published_at DESC NULLS LAST, id DESC
LIMIT 5;
```

Các số đếm phải lớn hơn `0` nếu seed có data. Sau đó chạy lại backend:

```powershell
$env:DB_PASSWORD="YOUR_POSTGRES_PASSWORD"
.\mvnw.cmd spring-boot:run
```

Và gọi endpoint để kiểm tra full flow:

```text
GET http://localhost:8080/api/articles/search?page=0&size=20
```

Nếu response có `items` thì backend đã đọc được data local thành công.

### Import bằng `psql` thay cho pgAdmin (tùy chọn)

Nếu đã cài PostgreSQL command-line tools, có thể import từ PowerShell:

```powershell
psql -h localhost -p 5728 -U postgres -d news_ai -f database\seeds\news_ai_seed.sql
```

Thay `5728` bằng port PostgreSQL local thực tế. Lệnh sẽ yêu cầu mật khẩu user PostgreSQL.

### Lỗi thường gặp

| Hiện tượng | Nguyên nhân thường gặp | Cách xử lý |
| --- | --- | --- |
| `database "news_ai" does not exist` | Chưa tạo database | Tạo database ở Bước 2 |
| `relation "articles" does not exist` | Chưa để Spring Boot tạo bảng | Chạy backend một lần ở Bước 3 |
| `duplicate key value violates unique constraint` | Đã import seed trước đó hoặc DB có data riêng | Dùng database trống mới; không chạy lại seed trên DB đã import |
| `password authentication failed` | `DB_PASSWORD` hoặc password pgAdmin sai | Kiểm tra password PostgreSQL, đặt lại biến môi trường trong terminal |
| Search lỗi `lower(bytea)` | Cột `articles.content` của DB cũ là `bytea` | Chạy manual migration được nêu ở phần backend |

### UI/UX và AI có cần import seed không?

- **Không cần**, nếu họ chỉ gọi backend đang chạy trên máy một thành viên khác hoặc server chung.
- **Cần**, nếu họ muốn chạy toàn bộ backend local và dùng dataset giống nhóm.
- React không đọc file `.sql` trực tiếp; React luôn đọc qua REST API.
- Postman không bắt buộc; nó chỉ hỗ trợ test API sau khi import.

## Chạy frontend

Mở terminal thứ hai, giữ backend đang chạy:

```powershell
cd D:\PRJ\news-ai\frontend
npm install
npm run dev
```

Mở URL Vite in trong terminal, thường là `http://localhost:5173`.

`frontend/vite.config.ts` đã proxy request `/api` đến `http://localhost:8080`. Trình tự debug chuẩn:

1. Kiểm tra `GET http://localhost:8080/api/status` thành công.
2. Kiểm tra `GET http://localhost:8080/api/articles/search?page=0&size=20` thành công.
3. Mở frontend, xem DevTools > Network nếu News page không tải được.

## API và dữ liệu crawl

Base URL local: `http://localhost:8080`.

### Endpoint chính

| Method | Endpoint | Mô tả |
| --- | --- | --- |
| GET | `/api/status` | Kiểm tra backend |
| GET/POST | `/api/articles` | List tất cả / tạo article |
| GET/PUT/DELETE | `/api/articles/{id}` | Chi tiết / cập nhật / xóa article |
| GET | `/api/articles/search` | Search có phân trang |
| GET/POST | `/api/sources` | List / tạo source |
| GET/PUT/DELETE | `/api/sources/{id}` | CRUD source theo ID |
| GET/POST | `/api/categories` | List / tạo category |
| GET/PUT/DELETE | `/api/categories/{id}` | CRUD category theo ID |
| POST | `/api/crawl` | Crawl tất cả source có RSS URL |
| POST | `/api/crawl/sources/{id}` | Crawl một source |

### Tạo source và crawl RSS

Tạo một source có URL thật, không dùng Markdown URL:

```json
POST /api/sources
{
  "name": "VnExpress",
  "baseUrl": "https://vnexpress.net",
  "rssUrl": "https://vnexpress.net/rss/tin-moi-nhat.rss"
}
```

Sau khi source được tạo (ví dụ ID `1`), crawl:

```powershell
Invoke-RestMethod -Method Post http://localhost:8080/api/crawl/sources/1
```

Kết quả crawl có các trường:

| Trường | Ý nghĩa |
| --- | --- |
| `fetched` | Số item hợp lệ đọc từ feed |
| `created` | Bài mới được thêm |
| `updated` | Bài đã có nhưng nội dung crawler quản lý thay đổi |
| `unchanged` | Bài đã có và không đổi |
| `failed` | Item không lưu được |
| `errors` | Chi tiết lỗi theo item/source |

Crawler mặc định chỉ chạy bằng API; scheduler tắt trong `application.properties`:

```properties
crawler.scheduling.enabled=false
```

### Search article

```text
GET /api/articles/search?keyword=AI&sourceId=1&categoryId=2&publishedFrom=2026-09-01&publishedTo=2026-09-30&page=0&size=20
```

Tất cả filter là tùy chọn:

| Parameter | Ý nghĩa |
| --- | --- |
| `keyword` | Tìm không phân biệt hoa/thường trong title, description, content |
| `sourceId`, `categoryId` | Lọc theo source/category |
| `publishedFrom`, `publishedTo` | `yyyy-MM-dd` hoặc ISO date-time |
| `page` | Bắt đầu từ `0`; mặc định `0` |
| `size` | 1–50; mặc định `20` |

Ví dụ lấy bài có sẵn, không filter:

```text
GET http://localhost:8080/api/articles/search?page=0&size=20
```

Kết quả trả `items`, `page`, `size`, `totalElements`, `totalPages`; được sắp xếp mới nhất trước. Search response không trả toàn bộ `content`; khi làm trang detail, gọi `GET /api/articles/{id}`.

## Kiểm thử

### Test tự động backend

```powershell
cd D:\PRJ\news-ai
.\mvnw.cmd test
```

Test phạm vi hẹp khi chỉ sửa search:

```powershell
.\mvnw.cmd "-Dtest=ArticleServiceSearchTest" test
```

Các nhóm test hiện có kiểm tra search, crawler upsert, chuẩn hóa URL, parse feed, crawler service và controller crawl.

### Smoke test sau khi chạy local

1. `GET /api/status` trả `200`.
2. Tạo source với RSS URL thật và crawl source đó.
3. Kiểm tra `fetched = created + updated + unchanged + failed`.
4. Crawl lại cùng feed; phần lớn item phải là `unchanged`.
5. `GET /api/articles/search?page=0&size=20` trả `200`.
6. Search keyword có kết quả hoặc mảng rỗng hợp lệ, không được `500`.
7. Mở News page và kiểm tra search/phân trang.

## Cấu trúc mã nguồn

```text
src/main/java/com/newsai/news_ai/
├── article/     # Entity Article, CRUD, search, crawl upsert
├── source/      # Source CRUD
├── category/    # Category CRUD
├── crawler/     # RSS fetch, parse, URL normalization, crawl trigger/schedule
├── error/       # ApiError và GlobalExceptionHandler
└── NewsAiApplication.java
```

Backend dùng feature-package style. Khi thêm chatbot, tạo package riêng như `ai/` hoặc `chat/`; không nhét toàn bộ code mới vào `article/`.

Tài liệu phân tích chi tiết backend hiện có: [design-note/overview-current.md](design-note/overview-current.md).

## Hướng dẫn theo vai trò

### UI/UX và Frontend

- Dùng `/api/articles/search`, không dùng `GET /api/articles` cho News page vì endpoint list cũ chưa phân trang.
- Hiển thị loading, empty state và error state; không giả định lúc nào database cũng có bài.
- Card search chỉ có summary. Muốn hiện full content hoặc link gốc, gọi detail endpoint theo `id`.
- Không hard-code bài viết trong UI; dùng dữ liệu crawled thực tế.
- Khi thay đổi fields frontend cần, trao đổi trước với backend để cập nhật DTO/API contract và test.

Lộ trình UI hợp lý: News list/search → article detail → filter source/category → giao diện chat → admin crawl tối giản.

### AI Chatbot

Nguồn dữ liệu đầu vào là `Article`: `title`, `description`, `content`, `publishedAt`, `source`, `category`, `url`. Trước khi làm LLM UI, cần kiểm tra bài crawl có `content` thực tế hay chỉ có description; chất lượng RSS feed khác nhau.

Hướng mặc định: **Java/Spring Boot**, không cần Python, embeddings hay pgvector ở phiên bản đầu. Luồng tối giản:

```text
Người dùng gửi câu hỏi
→ Backend tách keyword đơn giản
→ GET/search Article trong PostgreSQL
→ Chọn vài bài liên quan làm ngữ cảnh
→ Gọi LLM (nếu đã tích hợp)
→ Trả lời kèm title, source và URL bài gốc
```

Lộ trình khuyến nghị:

1. Dùng `/api/articles/search` làm retrieval keyword cơ bản và tạo endpoint chat chỉ đọc dữ liệu đã crawl.
2. Trả về citation cho mỗi câu trả lời: ít nhất `articleId`, title, source và URL.
3. Thêm LLM qua Java/Spring AI khi retrieval đã ổn định; API key đặt trong environment variable/local secret file đã ignore.
4. Chỉ cân nhắc embeddings + vector store/pgvector khi keyword search không còn đáp ứng yêu cầu.
5. Python service là phương án thay thế cho nhóm muốn nghiên cứu AI riêng; không phải dependency bắt buộc của project.

## Quy ước làm việc nhóm

1. Luôn chạy `git status` trước khi pull, commit hoặc đổi nhánh.
2. Không commit mật khẩu PostgreSQL, API key, `.env` cá nhân, `node_modules/`, `target/`, log hoặc database local.
3. Sửa API phải cập nhật controller, service, test và Postman/README nếu contract đổi.
4. Sửa entity/schema phải ghi migration rõ ràng. `ddl-auto=update` không luôn chuyển kiểu dữ liệu cũ an toàn.
5. Không để crawler ghi đè category `MANUAL`; không thay `url` gốc bằng `canonicalUrl`.
6. Chạy test phù hợp trước PR và ghi kết quả trong mô tả PR.
7. Commit nhỏ, rõ nghĩa: `feat: add article detail page`, `fix: handle search date filter`, `test: cover RSS category fallback`.

## Hướng phát triển tiếp theo

Ưu tiên thực tế cho bài tập lớn:

1. Hoàn thiện frontend News list/detail với dữ liệu crawl thật.
2. Bổ sung endpoint/DTO cần thiết cho UI mà không trả thừa content ở list.
3. Làm chatbot retrieval cơ bản trên dữ liệu `Article` và citations.
4. Chỉ sau đó mới cân nhắc embeddings, pgvector và scheduler crawl.
