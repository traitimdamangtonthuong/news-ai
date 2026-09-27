# AI Chatbot (module độc lập)

Chatbot Java tổng hợp lại các bài báo đã crawl trong dự án `news-ai`, dùng cho
phần AI Chatbot của bài tập lớn.

## Vì sao tách riêng khỏi backend?

Theo yêu cầu của nhóm, module này:

- **Không** phải sub-module Maven của backend (`news-ai/pom.xml` không đổi).
- **Không** import bất kỳ class Java nào trong `src/main/java/com/newsai/news_ai/...`
  (không đụng tới code của các thành viên khác).
- Chỉ nói chuyện với backend qua **REST API HTTP công khai** đã có sẵn
  (`/api/articles/search`, `/api/articles/{id}`) — giống hệt cách frontend React
  hoặc Postman gọi vào backend.

Nhờ vậy: build/chạy/test module này hoàn toàn tách biệt, không thể vô tình
làm hỏng backend, và ai trong nhóm cũng có thể chạy chatbot mà không cần đọc
hiểu code Spring Boot.

## Cấu trúc

```text
ai-chatbot/                     # module Maven riêng, KHÔNG nằm trong news-ai/src
├── pom.xml
├── src/main/resources/static/index.html   # giao diện chat (HTML/CSS/JS thuần)
└── src/main/java/com/newsai/aichatbot/
    ├── Main.java                          # entry point: mặc định mở web chat, có chế độ --cli
    ├── config/ChatbotConfig.java          # đọc cấu hình từ biến môi trường
    ├── client/NewsApiClient.java          # gọi REST API backend qua HTTP
    ├── client/NewsApiException.java
    ├── client/dto/                        # DTO khớp JSON response của backend
    ├── model/                             # ArticleContext, Citation, ChatAnswer
    ├── web/
    │   ├── ChatHttpServer.java            # HTTP server tối giản (com.sun.net.httpserver)
    │   └── ChatRequestDto.java            # body JSON của POST /api/chat
    └── service/
        ├── KeywordExtractor.java          # tách 1 keyword từ câu hỏi
        ├── ArticleSummarizer.java         # interface tổng hợp (dễ hoán đổi)
        ├── ExtractiveArticleSummarizer.java  # mặc định, không cần API key
        ├── LlmArticleSummarizer.java      # stub để cắm LLM thật sau này
        └── NewsChatbotService.java        # điều phối toàn bộ luồng
```

## Cách chạy

1. Chạy backend Spring Boot trước (xem README gốc của `news-ai`), mặc định ở
   `http://localhost:8080`.
2. Từ thư mục `ai-chatbot/`, dùng **Maven Wrapper** đi kèm (không cần cài Maven
   riêng, giống cách backend đã dùng `mvnw.cmd`):

   ```powershell
   .\mvnw.cmd package
   java -jar target\ai-chatbot-0.1.0.jar
   ```

   Trên macOS/Linux:

   ```bash
   ./mvnw package
   java -jar target/ai-chatbot-0.1.0.jar
   ```

   > Nếu máy bạn đã cài Maven riêng (lệnh `mvn` chạy được), dùng `mvn package`
   > cũng được — hai cách tương đương.

3. Mặc định chatbot mở **giao diện web chat** — mở trình duyệt vào:

   ```text
   http://localhost:8081
   ```

   Gõ câu hỏi vào ô chat (ví dụ: `AI có gì mới`) rồi bấm Gửi. Nhấn `Ctrl+C`
   trong terminal để dừng server.

   Nếu muốn dùng chế độ chat qua terminal (không cần trình duyệt) như bản cũ:

   ```powershell
   java -jar target\ai-chatbot-0.1.0.jar --cli
   ```

   > Lần đầu chạy, Windows Defender Firewall có thể hỏi cho phép Java nhận
   > kết nối mạng — chọn **Allow access** (chỉ áp dụng cho mạng nội bộ
   > máy bạn, không mở ra internet).

## Cấu hình (biến môi trường, không hard-code)

| Biến             | Ý nghĩa                                             | Mặc định                |
| ---------------- | ---------------------------------------------------- | ------------------------ |
| `NEWS_AI_BASE_URL` | Base URL của backend                                | `http://localhost:8080` |
| `NEWS_AI_TOP_K`    | Số bài báo tối đa lấy làm ngữ cảnh mỗi câu hỏi       | `5`                      |
| `CHATBOT_PORT`     | Cổng chạy giao diện web chat                        | `8081`                   |
| `LLM_API_KEY`      | (Tuỳ chọn) API key cho LLM thật, nếu đã tích hợp     | không đặt                |

Ví dụ (PowerShell):

```powershell
$env:NEWS_AI_BASE_URL="http://localhost:8080"
$env:NEWS_AI_TOP_K="8"
java -jar target/ai-chatbot-0.1.0.jar
```

Không commit `LLM_API_KEY` hay bất kỳ key thật nào vào Git.

## Cách hoạt động (bản đầu, chưa dùng LLM)

```text
Câu hỏi
  → KeywordExtractor: chọn 1 từ khoá nổi bật nhất
  → NewsApiClient: GET /api/articles/search?keyword=...
  → NewsApiClient: GET /api/articles/{id} để lấy full content từng bài
  → ExtractiveArticleSummarizer: chấm điểm câu theo tần suất từ, chọn câu tiêu biểu
  → Trả lời kèm trích dẫn: id, tiêu đề, nguồn, URL bài gốc
```

`ExtractiveArticleSummarizer` là thuật toán tóm tắt tự viết (không gọi API
ngoài, không cần internet, không tốn phí) — đủ để chạy demo và nộp bài tập
lớn. Chất lượng tất nhiên không bằng LLM thật.

## Dùng LLM thật (Google Gemini) để tóm tắt

Mặc định (`LLM_API_KEY` chưa đặt) chatbot dùng `ExtractiveArticleSummarizer`
(trích câu có sẵn, không cần key). Để có tóm tắt do AI **tự viết lại** (không
còn trích nguyên câu):

1. Lấy API key miễn phí tại **https://aistudio.google.com/apikey** (đăng nhập
   bằng tài khoản Google, bấm "Create API key").
2. Đặt biến môi trường **trong cùng cửa sổ terminal** trước khi chạy jar:

   ```powershell
   $env:LLM_API_KEY="AIzaSy...key_cua_ban"
   java -jar target\ai-chatbot-0.1.0.jar
   ```

3. Vậy là xong — không cần sửa code. `Main.java` đã tự chọn
   `LlmArticleSummarizer` (gọi Gemini `gemini-2.5-flash` qua REST API) khi
   thấy có key, và tự động rơi về bản extractive nếu Gemini lỗi/hết quota/mất
   mạng (xem log cảnh báo màu đỏ trong terminal khi việc đó xảy ra).

**Lưu ý:**
- `$env:LLM_API_KEY` chỉ có hiệu lực trong cửa sổ PowerShell đang mở — đóng
  cửa sổ là phải set lại.
- Không commit API key vào Git. Nếu lỡ dán key vào file rồi commit, thu hồi
  key cũ trong AI Studio và tạo key mới.
- Gói miễn phí của Gemini có giới hạn số request/phút — nếu thấy lỗi 429
  trong log, đợi một chút rồi hỏi lại.

## Giới hạn đã biết (ghi rõ để nhóm không bất ngờ)

- Backend hiện tìm theo `LIKE '%keyword%'` với **một** chuỗi duy nhất, không
  phải tìm kiếm đa từ khoá thật sự — nên `KeywordExtractor` chỉ chọn 1 từ
  nổi bật nhất từ câu hỏi. Nếu câu hỏi không khớp từ nào có trong bài báo đã
  crawl, chatbot sẽ báo không tìm thấy.
- Tóm tắt extractive mặc định chỉ trích câu có sẵn trong bài báo, không tự
  diễn đạt lại như LLM thật.
- Không có bộ nhớ hội thoại nhiều lượt (mỗi câu hỏi xử lý độc lập).

Những giới hạn này khớp với lộ trình "retrieval cơ bản trước, LLM/embeddings
sau" đã ghi trong README của backend.
