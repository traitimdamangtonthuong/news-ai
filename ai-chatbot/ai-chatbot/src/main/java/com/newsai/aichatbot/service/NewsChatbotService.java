package com.newsai.aichatbot.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.newsai.aichatbot.client.NewsApi;
import com.newsai.aichatbot.client.NewsApiException;
import com.newsai.aichatbot.client.SearchRequest;
import com.newsai.aichatbot.client.dto.ArticleDetailDto;
import com.newsai.aichatbot.client.dto.ArticleSearchItemDto;
import com.newsai.aichatbot.client.dto.ArticleSearchResponseDto;
import com.newsai.aichatbot.client.dto.NamedItemDto;
import com.newsai.aichatbot.model.ArticleContext;
import com.newsai.aichatbot.model.ChatAnswer;
import com.newsai.aichatbot.model.Citation;
import com.newsai.aichatbot.model.SearchPlan;
import com.newsai.aichatbot.text.HtmlCleaner;
import com.newsai.aichatbot.text.TextUtils;

/**
 * Luồng xử lý một câu hỏi, bám theo mô tả trong README của dự án:
 *
 *   Người dùng gửi câu hỏi
 *   → tách từ khoá (+ khoảng thời gian, tên báo, chuyên mục nếu có)         [QueryAnalyzer]
 *   → GET /api/articles/search cho từng từ khoá ứng viên                     [NewsApi]
 *   → chọn bài liên quan: xếp hạng theo tiêu đề/mô tả, tải GET /api/articles/{id},
 *     làm sạch HTML, chấm điểm lại theo toàn văn                             [RelevanceScorer]
 *   → tổng hợp (extractive mặc định, hoặc LLM nếu đã cấu hình)              [ArticleSummarizer]
 *   → trả lời kèm title, source, URL bài gốc (đánh số [1], [2]... khớp câu trả lời)
 *
 * Chỉ nói chuyện với backend qua {@link NewsApi} (REST công khai).
 */
public class NewsChatbotService {

    private static final long CATALOG_TTL_MS = 10 * 60_000L;
    private static final long CATALOG_RETRY_MS = 60_000L;
    private static final int NOISY_SCAN_PAGES = 4;
    private static final double MIN_COVERAGE = 0.7; // tỉ lệ (có trọng số) từ khoá phải xuất hiện trong bài
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private record Catalog(List<NamedItemDto> sources, List<NamedItemDto> categories, long expiresAt) {
    }

    private record Ranked(ArticleContext context, RelevanceScorer.Score score) {
    }

    private final NewsApi api;
    private final ArticleSummarizer summarizer;
    private final int topK;
    private final QueryAnalyzer analyzer;
    private volatile Catalog catalog = new Catalog(List.of(), List.of(), 0L);

    public NewsChatbotService(NewsApi api, ArticleSummarizer summarizer, int topK) {
        this(api, summarizer, topK, new QueryAnalyzer());
    }

    public NewsChatbotService(NewsApi api, ArticleSummarizer summarizer, int topK, QueryAnalyzer analyzer) {
        this.api = api;
        this.summarizer = summarizer;
        this.topK = Math.max(1, topK);
        this.analyzer = analyzer;
    }

    public ChatAnswer answer(String question) {
        Catalog cat = catalog();
        SearchPlan plan = analyzer.analyze(question, cat.sources(), cat.categories());

        List<ArticleSearchItemDto> items = retrieve(plan);
        String note = null;
        if (items.isEmpty() && plan.hasDate()) {
            // Không có bài nào trong khoảng thời gian yêu cầu -> thử lại không giới hạn ngày và báo rõ.
            SearchPlan relaxed = plan.withoutDate();
            List<ArticleSearchItemDto> retry = retrieve(relaxed);
            if (!retry.isEmpty()) {
                items = retry;
                note = "Lưu ý: không có bài nào khớp trong " + plan.dateLabel()
                        + " nên dưới đây là các bài gần nhất theo " + relaxed.describe() + ".";
                plan = relaxed;
            }
        }
        if (items.isEmpty() && !plan.categoryIds().isEmpty()) {
            // Chuyên mục có trong danh mục nhưng chưa có bài nào gắn với nó -> tìm tên chuyên mục như từ khoá.
            SearchPlan asKeyword = plan.withCategoryAsKeyword();
            items = retrieve(asKeyword);
            if (!items.isEmpty()) {
                plan = asKeyword;
            } else if (asKeyword.hasDate()) {
                SearchPlan noDate = asKeyword.withoutDate();
                items = retrieve(noDate);
                if (!items.isEmpty()) {
                    note = "Lưu ý: không có bài nào khớp trong " + asKeyword.dateLabel()
                            + " nên dưới đây là các bài gần nhất theo " + noDate.describe() + ".";
                    plan = noDate;
                }
            }
        }
        if (items.isEmpty()) {
            return notFound(plan);
        }

        List<ArticleContext> contexts = rankAndLoad(plan, items);
        if (contexts.isEmpty()) {
            return notFound(plan);
        }

        List<Citation> citations = new ArrayList<>();
        for (ArticleContext c : contexts) {
            citations.add(new Citation(c.id(), c.title(), c.sourceName(), c.url(),
                    c.publishedAt() == null ? null : c.publishedAt().format(DMY)));
        }
        String summary = summarizer.summarize(question, contexts);
        return new ChatAnswer(note == null ? summary : note + "\n\n" + summary, citations);
    }

    // ------------------------------------------------------------------ retrieval

    /**
     * Gọi search cho lần lượt từng truy vấn ứng viên (cụm → cặp từ → từ đơn) và gộp kết quả.
     * Dừng sớm khi đã đủ ứng viên.
     */
    private List<ArticleSearchItemDto> retrieve(SearchPlan plan) {
        Map<Long, ArticleSearchItemDto> pool = new LinkedHashMap<>();
        RelevanceScorer scorer = new RelevanceScorer(plan.terms(), plan.matchFolded());
        int pageSize = Math.min(50, Math.max(30, topK * 6));
        int poolTarget = topK * 3;
        List<String> queries = plan.queries().isEmpty() ? Collections.singletonList(null) : plan.queries();
        List<Long> categories = plan.categoryIds().isEmpty() ? Collections.singletonList(null) : plan.categoryIds();

        boolean anySuccess = false;
        NewsApiException lastError = null;
        outer:
        for (String query : queries) {
            for (Long categoryId : categories) {
                try {
                    if (isNoisy(query)) {
                        scanNoisy(query, categoryId, plan, scorer, pool, poolTarget);
                    } else {
                        ArticleSearchResponseDto res = api.searchArticles(new SearchRequest(
                                query, null, categoryId, plan.from(), plan.to(), 0, pageSize));
                        addItems(pool, res, plan, null);
                    }
                    anySuccess = true;
                } catch (NewsApiException e) {
                    lastError = e;
                }
                if (pool.size() >= poolTarget) {
                    break outer;
                }
            }
        }
        if (!anySuccess && lastError != null) {
            throw lastError;
        }

        if (pool.isEmpty() && plan.hasKeywords()) {
            // Dự phòng: backend khớp theo ký tự CÓ DẤU nên câu hỏi gõ không dấu sẽ không ra kết quả.
            // Quét các bài mới nhất rồi lọc theo nguyên từ (đã bỏ dấu) phía chatbot.
            for (Long categoryId : categories) {
                try {
                    ArticleSearchResponseDto res = api.searchArticles(new SearchRequest(
                            null, null, categoryId, plan.from(), plan.to(), 0, 50));
                    addItems(pool, res, plan, scorer);
                } catch (NewsApiException ignored) {
                    // đã có lỗi/kết quả rỗng ở bước trước; không làm hỏng câu trả lời
                }
            }
        }
        return new ArrayList<>(pool.values());
    }

    /**
     * Từ khoá ngắn (≤ 3 ký tự như "AI", "xe") khớp chuỗi con trong gần như mọi bài ("hai", "bài", "xem"...)
     * nên chỉ giữ các bài có từ khoá ở tiêu đề/mô tả, và quét thêm vài trang để không bỏ sót bài cũ hơn.
     */
    private void scanNoisy(String query, Long categoryId, SearchPlan plan, RelevanceScorer scorer,
                           Map<Long, ArticleSearchItemDto> pool, int poolTarget) {
        for (int page = 0; page < NOISY_SCAN_PAGES; page++) {
            ArticleSearchResponseDto res = api.searchArticles(new SearchRequest(
                    query, null, categoryId, plan.from(), plan.to(), page, 50));
            addItems(pool, res, plan, scorer);
            if (pool.size() >= poolTarget || res == null || res.items() == null
                    || res.items().size() < 50 || page + 1 >= res.totalPages()) {
                return;
            }
        }
    }

    private static boolean isNoisy(String query) {
        if (query == null || query.contains(" ")) {
            return false;
        }
        String folded = TextUtils.fold(query);
        return folded.length() <= 3 && !folded.chars().allMatch(Character::isDigit);
    }

    /** Thêm kết quả vào pool: lọc theo tên báo; nếu có {@code headerFilter} thì chỉ giữ bài có từ khoá ở tiêu đề/mô tả. */
    private void addItems(Map<Long, ArticleSearchItemDto> pool, ArticleSearchResponseDto res,
                          SearchPlan plan, RelevanceScorer headerFilter) {
        if (res == null || res.items() == null) {
            return;
        }
        for (ArticleSearchItemDto item : res.items()) {
            if (item == null || item.id() == null || pool.containsKey(item.id())) {
                continue;
            }
            if (!plan.outlets().isEmpty() && !matchesOutlet(item, plan.outlets())) {
                continue;
            }
            if (headerFilter != null
                    && headerFilter.scoreHeader(item.title(), item.description()).value() <= 0) {
                continue;
            }
            pool.put(item.id(), item);
        }
    }

    private static boolean matchesOutlet(ArticleSearchItemDto item, List<String> outlets) {
        if (item.source() == null || item.source().name() == null) {
            return false;
        }
        String source = TextUtils.fold(item.source().name());
        for (String outlet : outlets) {
            if (source.contains(outlet)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------ ranking + loading

    private List<ArticleContext> rankAndLoad(SearchPlan plan, List<ArticleSearchItemDto> items) {
        RelevanceScorer scorer = new RelevanceScorer(plan.terms(), plan.matchFolded());

        // 1) Xếp hạng sơ bộ theo tiêu đề + mô tả (và độ mới) để chọn bài cần tải nội dung.
        record Prelim(ArticleSearchItemDto item, double score) {
        }
        List<Prelim> prelim = new ArrayList<>();
        for (ArticleSearchItemDto item : items) {
            prelim.add(new Prelim(item, scorer.scoreHeader(item.title(), item.description()).value()));
        }
        prelim.sort(Comparator.comparingDouble(Prelim::score).reversed()
                .thenComparing((Prelim p) -> p.item().publishedAt(),
                        Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder())));

        int loadCount = plan.hasKeywords() ? Math.min(20, topK * 3) : topK;
        List<Long> ids = new ArrayList<>();
        Set<String> seenTitles = new HashSet<>();
        for (Prelim p : prelim) {
            if (ids.size() >= loadCount) {
                break;
            }
            if (seenTitles.add(TextUtils.fold(p.item().title()).strip())) {
                ids.add(p.item().id());
            }
        }

        // 2) Tải nội dung đầy đủ, làm sạch HTML, chấm điểm lại theo toàn văn.
        List<ArticleDetailDto> details = api.getArticlesByIds(ids);
        if (details.isEmpty() && !ids.isEmpty()) {
            throw new NewsApiException("Không tải được nội dung chi tiết của các bài báo từ backend.");
        }
        List<Ranked> ranked = new ArrayList<>();
        for (ArticleDetailDto d : details) {
            ArticleContext ctx = toContext(d);
            RelevanceScorer.Score score = scorer.score(
                    ctx.title(), ctx.description(), String.join(" ", ctx.paragraphs()));
            ranked.add(new Ranked(ctx, score));
        }

        // 3) Bỏ bài chỉ khớp chuỗi con (không có từ khoá nào đúng nguyên từ) và bài khớp quá ít từ khoá,
        //    để câu hỏi không có bài phù hợp trả lời "không tìm thấy" thay vì liệt kê bài không liên quan.
        if (plan.hasKeywords()) {
            ranked.removeIf(r -> r.score().value() <= 0 || r.score().coverage() < MIN_COVERAGE);
        }

        Comparator<Ranked> byDate = Comparator.comparing((Ranked r) -> r.context().publishedAt(),
                Comparator.nullsLast(Comparator.<LocalDateTime>reverseOrder()));
        Comparator<Ranked> byScore = Comparator.comparingDouble((Ranked r) -> r.score().value()).reversed();
        ranked.sort(plan.latest() ? byDate.thenComparing(byScore) : byScore.thenComparing(byDate));

        List<ArticleContext> result = new ArrayList<>();
        for (Ranked r : ranked) {
            if (result.size() >= topK) {
                break;
            }
            result.add(r.context());
        }
        return result;
    }

    private static ArticleContext toContext(ArticleDetailDto d) {
        String title = HtmlCleaner.toPlainText(d.title());
        if (title.isBlank()) {
            title = "(không có tiêu đề)";
        }
        String sourceName = d.source() != null && d.source().name() != null && !d.source().name().isBlank()
                ? d.source().name() : "Không rõ nguồn";
        return new ArticleContext(
                d.id(),
                title,
                sourceName,
                d.url(),
                d.publishedAt(),
                HtmlCleaner.toPlainText(d.description()),
                HtmlCleaner.toParagraphs(d.content(), title));
    }

    // ------------------------------------------------------------------- helpers

    private ChatAnswer notFound(SearchPlan plan) {
        return new ChatAnswer(
                "Không tìm thấy bài báo nào phù hợp (" + plan.describe() + ") trong dữ liệu đã crawl. "
                        + "Thử hỏi bằng từ khoá khác, bỏ bớt điều kiện (thời gian, nguồn), "
                        + "hoặc kiểm tra xem nguồn tin liên quan đã được crawl chưa.",
                List.of());
    }

    /** Danh sách nguồn/chuyên mục của backend, cache vài phút; lỗi thì dùng danh sách rỗng và thử lại sau. */
    private Catalog catalog() {
        Catalog c = catalog;
        long now = System.currentTimeMillis();
        if (now < c.expiresAt()) {
            return c;
        }
        synchronized (this) {
            c = catalog;
            if (now < c.expiresAt()) {
                return c;
            }
            try {
                List<NamedItemDto> sources = api.listSources();
                List<NamedItemDto> categories = api.listCategories();
                catalog = new Catalog(sources == null ? List.of() : sources,
                        categories == null ? List.of() : categories, now + CATALOG_TTL_MS);
            } catch (NewsApiException e) {
                System.err.println("[canh-bao] khong lay duoc danh sach nguon/chuyen muc: " + e.getMessage());
                catalog = new Catalog(c.sources(), c.categories(), now + CATALOG_RETRY_MS);
            }
            return catalog;
        }
    }
}
